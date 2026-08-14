// Package persistence 实现独立管理员安全域的 PostgreSQL 持久化适配器。
package persistence

import (
	"context"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/jackc/pgx/v5/pgtype"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/adminaccount"
	"github.com/lishangbu/avalon/internal/admin"
	platformaudit "github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	securityaccount "github.com/lishangbu/avalon/internal/security/account"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// authenticationAdapters 持久化管理员登录保护、会话与安全审计。
type authenticationAdapters struct {
	// pool 是管理员安全事务使用的共享 PostgreSQL 连接池。
	pool     *database.Pool
	sessions SessionBackend
}

// SessionBackend 是 Valkey 会话存储向管理员认证适配器提供的最小边界。
type SessionBackend interface {
	CreateSession(context.Context, authentication.SessionRecord) error
	AuthenticateSession(context.Context, []byte, time.Time) (authentication.Principal, error)
	RotateRefreshSession(context.Context, []byte, []byte, snowflake.ID, time.Time, time.Duration) (authentication.Principal, time.Time, error)
	TouchSessionActivity(context.Context, snowflake.ID, time.Time, time.Time, time.Time) error
	ListActiveSessionFamilies(context.Context, snowflake.ID, time.Time) ([]authentication.SessionFamily, error)
	RevokeSessionFamily(context.Context, snowflake.ID, time.Time) error
}

// NewAuthenticationAdapters 创建独立管理员认证持久化适配器。
func NewAuthenticationAdapters(pool *database.Pool, sessions ...SessionBackend) *authenticationAdapters {
	repository := &authenticationAdapters{pool: pool}
	if len(sessions) > 0 {
		repository.sessions = sessions[0]
	}
	return repository
}

// FindLoginAccount 读取管理员密码、状态和登录保护信息组成的登录投影。
func (s *authenticationAdapters) FindLoginAccount(
	ctx context.Context,
	usernameKey string,
) (authentication.LoginAccount, error) {
	row, err := s.pool.Client(ctx).AdminAccount.Query().Where(adminaccount.UsernameKeyEQ(usernameKey)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return authentication.LoginAccount{}, authentication.ErrLoginAccountNotFound
	}
	if err != nil {
		return authentication.LoginAccount{}, err
	}
	return authentication.LoginAccount{ID: row.ID, PasswordHash: row.PasswordHash,
		Status: securityaccount.Status(row.Status), SecurityVersion: 1,
		FailedLoginAttempts: row.FailedLoginAttempts, LockedUntil: row.LockedUntil}, nil
}

// RecordLoginFailure 原子更新管理员渐进锁定状态并写入尝试和安全审计。
func (s *authenticationAdapters) RecordLoginFailure(
	ctx context.Context,
	record authentication.LoginFailureRecord,
) error {
	usernameDigest, err := hex.DecodeString(record.UsernameDigest)
	if err != nil || len(usernameDigest) != 32 {
		return errors.New("管理员登录名摘要无效")
	}
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := s.pool.Client(transactionCtx)
		executor := database.Executor(transactionCtx, s.pool)
		failedAttempts := int32(0)
		var lockedUntil *time.Time
		var accountID *snowflake.ID
		if record.AccountID != snowflake.ID(0) {
			current, findErr := client.AdminAccount.Query().Where(adminaccount.IDEQ(record.AccountID)).Only(transactionCtx)
			if findErr != nil && !avalonent.IsNotFound(findErr) {
				return fmt.Errorf("锁定管理员登录保护状态: %w", findErr)
			}
			if findErr == nil {
				accountIDValue := record.AccountID
				accountID = &accountIDValue
				state := authentication.LoginProtectionState{
					Status: securityaccount.Status(current.Status), SecurityVersion: 1,
					FailedAttempts: current.FailedLoginAttempts, LockedUntil: current.LockedUntil,
				}
				next, changed := state.AfterFailure(record.Policy, record.OccurredAt)
				failedAttempts = next.FailedAttempts
				lockedUntil = next.LockedUntil
				if changed {
					if _, updateErr := client.AdminAccount.UpdateOne(current).SetStatus(string(next.Status)).SetFailedLoginAttempts(failedAttempts).SetNillableLockedUntil(next.LockedUntil).SetUpdatedAt(record.OccurredAt.UTC()).Save(transactionCtx); updateErr != nil {
						return fmt.Errorf("更新管理员登录保护状态: %w", updateErr)
					}
				}
			}
		}
		if _, err := client.AdminLoginAttempt.Create().SetID(record.LoginAttemptID).SetNillableAccountID(accountID).SetUsernameDigest(usernameDigest).SetSucceeded(false).SetFailureReason(string(record.Reason)).SetRequestID(record.RequestID).SetOccurredAt(record.OccurredAt.UTC()).Save(transactionCtx); err != nil {
			return fmt.Errorf("写入管理员登录失败记录: %w", err)
		}
		changes, err := json.Marshal(struct {
			FailedAttempts int32      `json:"failedAttempts"`
			LockedUntil    *time.Time `json:"lockedUntil,omitempty"`
		}{failedAttempts, lockedUntil})
		if err != nil {
			return fmt.Errorf("编码管理员登录失败审计: %w", err)
		}
		if err := platformaudit.Append(transactionCtx, executor, platformaudit.AdminLedger, platformaudit.Entry{ID: record.AuditID, ActorKind: "anonymous", ActorIdentifier: stringPointer("username_sha256:" + record.UsernameDigest), ActionCode: "admin.login.failed", ObjectType: "admin_account", ObjectID: identifierStringPointer(record.AccountID), RequestID: record.RequestID, Reason: stringPointer(string(record.Reason)), Changes: changes, CreatedAt: record.OccurredAt.UTC()}); err != nil {
			return fmt.Errorf("写入管理员登录失败审计: %w", err)
		}
		return nil
	})
}

// CreateSession 原子清除登录锁定、写入成功尝试并创建管理员持久会话。
func (s *authenticationAdapters) CreateSession(ctx context.Context, record authentication.SessionRecord) error {
	return s.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := s.pool.Client(txctx)
		executor := database.Executor(txctx, s.pool)
		account, err := client.AdminAccount.Query().Where(adminaccount.IDEQ(record.AccountID), adminaccount.PasswordHashEQ(record.ExpectedPasswordHash)).Only(txctx)
		if avalonent.IsNotFound(err) {
			return authentication.ErrInvalidCredentials
		}
		if err != nil {
			return fmt.Errorf("读取管理员登录账号: %w", err)
		}
		if _, err = client.AdminAccount.UpdateOne(account).SetStatus(string(securityaccount.StatusActive)).SetFailedLoginAttempts(0).ClearLockedUntil().SetUpdatedAt(record.CreatedAt.UTC()).Save(txctx); err != nil {
			return fmt.Errorf("重置管理员登录保护状态: %w", err)
		}
		if s.sessions != nil {
			if err := s.sessions.CreateSession(txctx, record); err != nil {
				return fmt.Errorf("写入 Valkey 会话: %w", err)
			}
		} else {
			return errors.New("Valkey Session Store 未配置")
		}
		if _, err = client.AdminLoginAttempt.Create().SetID(record.LoginAttemptID).SetAccountID(record.AccountID).SetUsernameDigest(record.UsernameDigest).SetSucceeded(true).SetRequestID(record.RequestID).SetOccurredAt(record.CreatedAt.UTC()).Save(txctx); err != nil {
			return fmt.Errorf("写入管理员登录成功记录: %w", err)
		}
		if err := platformaudit.Append(txctx, executor, platformaudit.AdminLedger, platformaudit.Entry{
			ID: record.AuditID, ActorAccountID: &record.AccountID,
			ActorKind: "admin", ActionCode: "admin.login.succeeded", ObjectType: "admin_session",
			ObjectID: stringPointer(record.FamilyID.String()), RequestID: record.RequestID,
			Changes: []byte(`{"created":true}`), CreatedAt: record.CreatedAt.UTC(),
		}); err != nil {
			return fmt.Errorf("记录管理员登录审计: %w", err)
		}
		return nil
	})
}

// AuthenticateSession 根据 Valkey 摘要读取有效管理员会话。
func (s *authenticationAdapters) AuthenticateSession(ctx context.Context, digest []byte, now time.Time) (authentication.Principal, error) {
	if s.sessions == nil {
		return authentication.Principal{}, errors.New("Valkey Session Store 未配置")
	}
	principal, err := s.sessions.AuthenticateSession(ctx, digest, now)
	if err != nil {
		return authentication.Principal{}, err
	}
	account, err := s.pool.Client(ctx).AdminAccount.Query().Where(adminaccount.IDEQ(principal.AccountID), adminaccount.StatusEQ(string(securityaccount.StatusActive))).Only(ctx)
	if err != nil || account == nil {
		return authentication.Principal{}, authentication.ErrSessionNotFound
	}
	return principal, nil
}

// RotateRefreshSession 原子消费 Valkey refresh token。
func (s *authenticationAdapters) RotateRefreshSession(ctx context.Context, digest, nextDigest []byte, nextID snowflake.ID, now time.Time, idleTTL time.Duration) (authentication.Principal, time.Time, error) {
	if s.sessions == nil {
		return authentication.Principal{}, time.Time{}, errors.New("Valkey Session Store 未配置")
	}
	principal, expires, err := s.sessions.RotateRefreshSession(ctx, digest, nextDigest, nextID, now, idleTTL)
	if err != nil {
		return authentication.Principal{}, time.Time{}, err
	}
	account, err := s.pool.Client(ctx).AdminAccount.Query().Where(adminaccount.IDEQ(principal.AccountID), adminaccount.StatusEQ(string(securityaccount.StatusActive))).Only(ctx)
	if err != nil || account == nil {
		return authentication.Principal{}, time.Time{}, authentication.ErrSessionNotFound
	}
	return principal, expires, nil
}

// TouchSessionActivity 按节流条件更新管理员会话。
func (s *authenticationAdapters) TouchSessionActivity(ctx context.Context, sessionID snowflake.ID, lastActivityAt, idleExpiresAt, writeBefore time.Time) error {
	if s.sessions == nil {
		return errors.New("Valkey Session Store 未配置")
	}
	return s.sessions.TouchSessionActivity(ctx, sessionID, lastActivityAt, idleExpiresAt, writeBefore)
}

// ListActiveSessionFamilies 返回账号下仍然有效的设备会话。
func (s *authenticationAdapters) ListActiveSessionFamilies(ctx context.Context, accountID snowflake.ID, now time.Time) ([]authentication.SessionFamily, error) {
	if s.sessions == nil {
		return nil, errors.New("Valkey Session Store 未配置")
	}
	return s.sessions.ListActiveSessionFamilies(ctx, accountID, now)
}

// WithinSessionRevocation 在单一事务中撤销自有会话并写管理员审计。
func (s *authenticationAdapters) WithinSessionRevocation(ctx context.Context, work func(authentication.SessionRevocationWriter) error) error {
	if s.sessions == nil {
		return errors.New("Valkey Session Store 未配置")
	}
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&sessionRevocationWriter{sessions: s.sessions, executor: database.Executor(transactionCtx, s.pool)})
	})
}

// RevokeSessionFamily 撤销当前管理员登录对应的整个会话族。
func (s *authenticationAdapters) RevokeSessionFamily(ctx context.Context, familyID snowflake.ID, now time.Time) error {
	if s.sessions != nil {
		return s.sessions.RevokeSessionFamily(ctx, familyID, now)
	}
	return errors.New("Valkey Session Store 未配置")
}

// FindIdentity 读取仍然有效的管理员最小身份快照。
func (s *authenticationAdapters) FindIdentity(ctx context.Context, accountID snowflake.ID) (admin.Identity, error) {
	row, err := s.pool.Client(ctx).AdminAccount.Query().Where(adminaccount.IDEQ(accountID), adminaccount.StatusEQ(string(securityaccount.StatusActive))).Only(ctx)
	if avalonent.IsNotFound(err) {
		return admin.Identity{}, admin.ErrIdentityNotFound
	}
	if err != nil {
		return admin.Identity{}, err
	}
	return admin.Identity{ID: row.ID, Username: row.Username, DisplayName: row.DisplayName}, nil
}

// sessionRevocationWriter 把管理员自有会话撤销和审计固定在同一事务。
type sessionRevocationWriter struct {
	// sessions 是独立 Valkey 会话事实存储。
	sessions SessionBackend
	// executor 是同一事务中的原生执行器，用于追加审计哈希链。
	executor database.Transaction
}

// RevokeOwnedSessionFamily 幂等撤销属于指定管理员的会话族。
func (w *sessionRevocationWriter) RevokeOwnedSessionFamily(
	ctx context.Context,
	accountID snowflake.ID,
	familyID snowflake.ID,
	now time.Time,
) (bool, error) {
	families, err := w.sessions.ListActiveSessionFamilies(ctx, accountID, now)
	if err != nil {
		return false, err
	}
	for _, family := range families {
		if family.FamilyID == familyID {
			return true, w.sessions.RevokeSessionFamily(ctx, familyID, now)
		}
	}
	return false, nil
}

// RecordSessionRevocation 写入不包含任何会话凭证明文的管理员审计。
func (w *sessionRevocationWriter) RecordSessionRevocation(
	ctx context.Context,
	audit authentication.SessionRevocationAudit,
) error {
	return platformaudit.Append(ctx, w.executor, platformaudit.AdminLedger, platformaudit.Entry{ID: audit.ID, ActorAccountID: &audit.AccountID, ActorKind: "admin", ActionCode: "admin.session.self_revoked", ObjectType: "admin_session_family", ObjectID: stringPointer(audit.FamilyID.String()), RequestID: audit.RequestID, Reason: stringPointer("self_revoked"), Changes: []byte(`{"revoked":true}`), CreatedAt: audit.OccurredAt.UTC()})
}

func stringPointer(value string) *string { return &value }
func identifierStringPointer(value snowflake.ID) *string {
	if value == snowflake.ID(0) {
		return nil
	}
	return stringPointer(value.String())
}

func databaseIdentifier(id snowflake.ID) pgtype.Int8 {
	return pgtype.Int8{Int64: int64(id), Valid: id != 0}
}

func nullableIdentifier(id snowflake.ID) pgtype.Int8 {
	return databaseIdentifier(id)
}

func textIdentifier(id snowflake.ID) pgtype.Text {
	return pgtype.Text{String: id.String(), Valid: id != snowflake.ID(0)}
}

func databaseTime(value time.Time) pgtype.Timestamptz {
	return pgtype.Timestamptz{Time: value, Valid: true}
}

func nullableTime(value pgtype.Timestamptz) *time.Time {
	if !value.Valid {
		return nil
	}
	result := value.Time
	return &result
}
