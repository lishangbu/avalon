// Package persistence 提供玩家安全域的 PostgreSQL 与 Valkey 持久化适配器。
package persistence

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	avalonent "github.com/lishangbu/avalon/ent"
	accountent "github.com/lishangbu/avalon/ent/account"
	platformaudit "github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	securityaccount "github.com/lishangbu/avalon/internal/security/account"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// authenticationAdapters 读取登录与认证投影，并协调可撤销会话持久化。
type authenticationAdapters struct {
	pool     *database.Pool
	sessions SessionBackend
}

// SessionBackend 是 Valkey 会话存储向玩家认证适配器提供的最小边界。
type SessionBackend interface {
	CreateSession(context.Context, authentication.SessionRecord) error
	AuthenticateSession(context.Context, []byte, time.Time) (authentication.Principal, error)
	RotateRefreshSession(context.Context, []byte, []byte, snowflake.ID, time.Time, time.Duration) (authentication.Principal, time.Time, error)
	TouchSessionActivity(context.Context, snowflake.ID, time.Time, time.Time, time.Time) error
	ListActiveSessionFamilies(context.Context, snowflake.ID, time.Time) ([]authentication.SessionFamily, error)
	RevokeSessionFamily(context.Context, snowflake.ID, time.Time) error
}

// NewAuthenticationAdapters 创建玩家账号与 Valkey 会话适配器。
func NewAuthenticationAdapters(pool *database.Pool, sessions SessionBackend) *authenticationAdapters {
	return &authenticationAdapters{pool: pool, sessions: sessions}
}

// FindIdentity 读取仍然有效的玩家账号最小身份快照。
func (s *authenticationAdapters) FindIdentity(
	ctx context.Context,
	accountID snowflake.ID,
) (authentication.Identity, error) {
	row, err := s.pool.Client(ctx).Account.Query().Where(accountent.IDEQ(accountID), accountent.StatusEQ(string(securityaccount.StatusActive))).Only(ctx)
	if avalonent.IsNotFound(err) {
		return authentication.Identity{}, authentication.ErrIdentityNotFound
	}
	if err != nil {
		return authentication.Identity{}, fmt.Errorf("读取玩家账号身份: %w", err)
	}
	return authentication.Identity{ID: row.ID, Username: row.Username, DisplayName: row.DisplayName}, nil
}

// FindLoginAccount 读取密码、状态、授权版本和登录保护信息组成的登录投影。
func (s *authenticationAdapters) FindLoginAccount(
	ctx context.Context,
	usernameKey string,
) (authentication.LoginAccount, error) {
	row, err := s.pool.Client(ctx).Account.Query().Where(accountent.UsernameKeyEQ(usernameKey)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return authentication.LoginAccount{}, authentication.ErrLoginAccountNotFound
	}
	if err != nil {
		return authentication.LoginAccount{}, err
	}
	return authentication.LoginAccount{ID: row.ID, PasswordHash: row.PasswordHash,
		Status: securityaccount.Status(row.Status), SecurityVersion: row.SecurityVersion,
		FailedLoginAttempts: row.FailedLoginAttempts, LockedUntil: row.LockedUntil}, nil
}

// RecordLoginFailure 原子递增连续失败次数、施加渐进锁定并写入匿名安全审计。
func (s *authenticationAdapters) RecordLoginFailure(
	ctx context.Context,
	record authentication.LoginFailureRecord,
) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		executor := database.Executor(transactionCtx, s.pool)
		client := s.pool.Client(transactionCtx)
		failedAttempts := int32(0)
		var lockedUntil *time.Time
		var objectID *string
		if record.AccountID != snowflake.ID(0) {
			current, err := client.Account.Query().Where(accountent.IDEQ(record.AccountID)).Only(ctx)
			if avalonent.IsNotFound(err) {
				current = nil
			}
			if err != nil && !avalonent.IsNotFound(err) {
				return fmt.Errorf("锁定登录防护状态: %w", err)
			}
			if current != nil {
				objectID = stringPointer(record.AccountID.String())
				state := authentication.LoginProtectionState{
					Status: securityaccount.Status(current.Status), SecurityVersion: current.SecurityVersion,
					FailedAttempts: current.FailedLoginAttempts, LockedUntil: current.LockedUntil,
				}
				next, changed := state.AfterFailure(record.Policy, record.OccurredAt)
				failedAttempts = next.FailedAttempts
				lockedUntil = next.LockedUntil
				if changed {
					_, err := client.Account.UpdateOne(current).SetStatus(string(next.Status)).SetSecurityVersion(next.SecurityVersion).SetFailedLoginAttempts(failedAttempts).SetNillableLockedUntil(next.LockedUntil).SetUpdatedAt(record.OccurredAt.UTC()).Save(ctx)
					if err != nil {
						return fmt.Errorf("更新登录防护状态: %w", err)
					}
				}
			}
		}
		changes, err := json.Marshal(struct {
			FailedAttempts int32      `json:"failedAttempts"`
			LockedUntil    *time.Time `json:"lockedUntil,omitempty"`
		}{
			FailedAttempts: failedAttempts,
			LockedUntil:    lockedUntil,
		})
		if err != nil {
			return fmt.Errorf("编码登录失败审计变更: %w", err)
		}
		if err := platformaudit.Append(ctx, executor, platformaudit.AdministrationLedger, platformaudit.Entry{
			ID: record.AuditID, ActorKind: "anonymous", ActorIdentifier: stringPointer("username_sha256:" + record.UsernameDigest),
			ActionCode: "security.login.failed", ObjectType: "account", ObjectID: objectID, RequestID: record.RequestID,
			Reason: stringPointer(string(record.Reason)), Changes: changes, CreatedAt: record.OccurredAt.UTC(),
		}); err != nil {
			return fmt.Errorf("写入登录失败审计: %w", err)
		}
		return nil
	})
}

// AuthenticateSession 根据领域隔离的会话 Token 摘要读取仍然有效的权威会话。
//
// 每次认证都会同时校验账号状态和 authorizationVersion，因此禁用账号或调整角色后，
// 已签发 会话凭证 无需等待本地缓存过期即可失效。
func (s *authenticationAdapters) AuthenticateSession(
	ctx context.Context,
	digest []byte,
	now time.Time,
) (authentication.Principal, error) {
	if s.sessions == nil {
		return authentication.Principal{}, errors.New("Valkey Session Store 未配置")
	}
	principal, err := s.sessions.AuthenticateSession(ctx, digest, now)
	if err != nil {
		return authentication.Principal{}, err
	}
	account, err := s.pool.Client(ctx).Account.Query().Where(accountent.IDEQ(principal.AccountID), accountent.StatusEQ(string(securityaccount.StatusActive)), accountent.SecurityVersionEQ(principal.SecurityVersion)).Only(ctx)
	if err != nil || account == nil {
		return authentication.Principal{}, authentication.ErrSessionNotFound
	}
	return principal, nil
}

// RotateRefreshSession 原子消费玩家 refresh token 并创建同一会话族的下一枚凭据。
// 重放已消费凭据时撤销整个会话族，避免失窃 refresh token 持续生效。
func (s *authenticationAdapters) RotateRefreshSession(
	ctx context.Context, digest []byte, nextDigest []byte, nextID snowflake.ID, now time.Time, idleTTL time.Duration,
) (authentication.Principal, time.Time, error) {
	if s.sessions == nil {
		return authentication.Principal{}, time.Time{}, errors.New("Valkey Session Store 未配置")
	}
	principal, expires, err := s.sessions.RotateRefreshSession(ctx, digest, nextDigest, nextID, now, idleTTL)
	if err != nil {
		return authentication.Principal{}, time.Time{}, err
	}
	account, err := s.pool.Client(ctx).Account.Query().Where(accountent.IDEQ(principal.AccountID), accountent.StatusEQ(string(securityaccount.StatusActive)), accountent.SecurityVersionEQ(principal.SecurityVersion)).Only(ctx)
	if err != nil || account == nil {
		return authentication.Principal{}, time.Time{}, authentication.ErrSessionNotFound
	}
	return principal, expires, nil
}

// TouchSessionActivity 最多按调用方给出的节流窗口更新一次活动时间，并受绝对期限封顶。
func (s *authenticationAdapters) TouchSessionActivity(
	ctx context.Context,
	sessionID snowflake.ID,
	lastActivityAt time.Time,
	idleExpiresAt time.Time,
	writeBefore time.Time,
) error {
	if s.sessions != nil {
		return s.sessions.TouchSessionActivity(ctx, sessionID, lastActivityAt, idleExpiresAt, writeBefore)
	}
	return errors.New("Valkey Session Store 未配置")
}

// ListActiveSessionFamilies 返回账号按最近活动时间倒序排列的有效会话族。
func (s *authenticationAdapters) ListActiveSessionFamilies(
	ctx context.Context,
	accountID snowflake.ID,
	now time.Time,
) ([]authentication.SessionFamily, error) {
	if s.sessions != nil {
		return s.sessions.ListActiveSessionFamilies(ctx, accountID, now)
	}
	return nil, errors.New("Valkey Session Store 未配置")
}

// WithinSessionRevocation 执行由 SessionManager 明确划定范围的自有会话撤销事务。
func (s *authenticationAdapters) WithinSessionRevocation(
	ctx context.Context,
	work func(authentication.SessionRevocationWriter) error,
) error {
	if s.sessions == nil {
		return errors.New("Valkey Session Store 未配置")
	}
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return work(&sessionRevocationWriter{sessions: s.sessions, executor: database.Executor(transactionCtx, s.pool)})
	})
}

type sessionRevocationWriter struct {
	sessions SessionBackend
	executor database.Transaction
}

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

func (w *sessionRevocationWriter) RecordSessionRevocation(
	ctx context.Context,
	audit authentication.SessionRevocationAudit,
) error {
	if err := platformaudit.Append(ctx, w.executor, platformaudit.AdministrationLedger, platformaudit.Entry{
		ID: audit.ID, ActorAccountID: &audit.AccountID, ActorKind: "account",
		ActionCode: "security.session.self_revoked", ObjectType: "session_family", ObjectID: stringPointer(audit.FamilyID.String()),
		RequestID: audit.RequestID, Reason: stringPointer("self_revoked"), Changes: []byte(`{"revoked":true}`), CreatedAt: audit.OccurredAt.UTC(),
	}); err != nil {
		return fmt.Errorf("插入自有会话撤销审计: %w", err)
	}
	return nil
}

// CreateSession 写入只包含 Token 摘要和服务端有效期的新会话代际。
func (s *authenticationAdapters) CreateSession(
	ctx context.Context,
	record authentication.SessionRecord,
) error {
	return s.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := s.pool.Client(txctx)
		account, err := client.Account.Query().Where(accountent.IDEQ(record.AccountID), accountent.PasswordHashEQ(record.ExpectedPasswordHash), accountent.SecurityVersionEQ(record.SecurityVersion)).Only(txctx)
		if avalonent.IsNotFound(err) {
			return authentication.ErrInvalidCredentials
		}
		if err != nil {
			return fmt.Errorf("读取登录账号: %w", err)
		}
		authorizationVersion := account.SecurityVersion + 1
		if _, err = client.Account.UpdateOne(account).SetStatus(string(securityaccount.StatusActive)).SetFailedLoginAttempts(0).ClearLockedUntil().SetSecurityVersion(authorizationVersion).SetUpdatedAt(record.CreatedAt.UTC()).Save(txctx); err != nil {
			return fmt.Errorf("重置登录防护状态: %w", err)
		}
		record.SecurityVersion = authorizationVersion
		if s.sessions != nil {
			if err := s.sessions.CreateSession(txctx, record); err != nil {
				return fmt.Errorf("写入 Valkey 会话: %w", err)
			}
		} else {
			return errors.New("Valkey Session Store 未配置")
		}
		return nil
	})
}

// stringPointer 返回审计条目使用的稳定可选字符串。
func stringPointer(value string) *string { return &value }

// RevokeSessionFamily 撤销已经通过 会话凭证 认证的整个会话族。
func (s *authenticationAdapters) RevokeSessionFamily(
	ctx context.Context,
	familyID snowflake.ID,
	now time.Time,
) error {
	if s.sessions != nil {
		return s.sessions.RevokeSessionFamily(ctx, familyID, now)
	}
	return errors.New("Valkey Session Store 未配置")
}
