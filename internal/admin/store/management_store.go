package store

import (
	"context"
	"crypto/rand"
	"encoding/json"
	"errors"
	"strings"
	"time"

	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/adminaccount"
	"github.com/lishangbu/avalon/ent/adminauditlog"
	"github.com/lishangbu/avalon/internal/admin"
	platformaudit "github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	securityaccount "github.com/lishangbu/avalon/internal/security/account"
)

// ManagementStore 持久化管理员账号维护并读取安全审计。
type ManagementStore struct {
	pool      *database.Pool
	ids       snowflake.Source
	passwords *securityaccount.PasswordHasher
}

// NewManagementStore 创建管理员维护持久层。
func NewManagementStore(pool *database.Pool, ids snowflake.Source) *ManagementStore {
	return &ManagementStore{pool: pool, ids: ids, passwords: securityaccount.NewPasswordHasher(rand.Reader)}
}

// ListAccounts 返回不包含密码资料的管理员账号。
func (s *ManagementStore) ListAccounts(ctx context.Context, pageSize int) ([]admin.ManagedAccount, error) {
	if pageSize < 1 {
		pageSize = 50
	}
	if pageSize > 200 {
		pageSize = 200
	}
	rows, err := s.pool.Client(ctx).AdminAccount.Query().Order(adminaccount.ByUsernameKey(), adminaccount.ByID()).Limit(pageSize).All(ctx)
	if err != nil {
		return nil, err
	}
	result := make([]admin.ManagedAccount, 0, len(rows))
	for _, row := range rows {
		result = append(result, managedAccount(row))
	}
	return result, nil
}

// CreateAccount 创建启用的管理员账号、幂等响应和管理审计。
func (s *ManagementStore) CreateAccount(ctx context.Context, command admin.CreateManagedAccountCommand) (admin.ManagedAccount, error) {
	username, err := securityaccount.ParseUsername(strings.TrimSpace(command.Username))
	if err != nil {
		return admin.ManagedAccount{}, admin.ErrInvalidManagementCommand
	}
	displayName, err := securityaccount.ParseDisplayName(command.DisplayName)
	if err != nil {
		return admin.ManagedAccount{}, admin.ErrInvalidManagementCommand
	}
	if !command.ActorAccountID.IsValid() || !idempotency.ValidKey(command.IdempotencyKey) || strings.TrimSpace(command.RequestID) == "" {
		return admin.ManagedAccount{}, admin.ErrInvalidManagementCommand
	}
	credential, err := s.passwords.HashCredential(command.Password)
	if err != nil {
		return admin.ManagedAccount{}, admin.ErrInvalidManagementCommand
	}
	id, err := s.ids.Next(ctx)
	if err != nil {
		return admin.ManagedAccount{}, err
	}
	now := time.Now().UTC()
	result := admin.ManagedAccount{ID: id, Username: username.String(), DisplayName: displayName.String(), Status: string(securityaccount.StatusActive), Version: 1, CreatedAt: now, UpdatedAt: now}
	digest, err := idempotency.Digest(struct{ Username, DisplayName, Password string }{result.Username, result.DisplayName, command.Password})
	if err != nil {
		return admin.ManagedAccount{}, err
	}
	request := idempotency.Request{ActorAccountID: command.ActorAccountID, OperationID: "admin.account.create", Key: command.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	err = s.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := s.pool.Client(txctx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, s.ids))
		replay, claimErr := idempotency.ClaimResponse(txctx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		if _, saveErr := client.AdminAccount.Create().SetID(id).SetUsername(result.Username).SetUsernameKey(result.Username).SetDisplayName(result.DisplayName).SetPasswordHash(credential.Encoded).SetPasswordAlgorithm(credential.Algorithm).SetPasswordParameters(json.RawMessage(credential.Parameters)).SetStatus(result.Status).SetVersion(1).SetFailedLoginAttempts(0).SetCreatedAt(now).SetUpdatedAt(now).Save(txctx); saveErr != nil {
			return managementError(saveErr)
		}
		auditID, idErr := s.ids.Next(txctx)
		if idErr != nil {
			return idErr
		}
		objectID, reason := id.String(), "administrative_change"
		changes, _ := json.Marshal(struct{ Username, DisplayName, Status string }{result.Username, result.DisplayName, result.Status})
		if auditErr := platformaudit.Append(txctx, database.Executor(txctx, s.pool), platformaudit.AdminLedger, platformaudit.Entry{ID: auditID, ActorAccountID: &command.ActorAccountID, ActorKind: "admin", ActionCode: "admin.account.created", ObjectType: "admin_account", ObjectID: &objectID, RequestID: command.RequestID, Reason: &reason, Changes: changes, CreatedAt: now}); auditErr != nil {
			return auditErr
		}
		return idempotency.Complete(txctx, writer, request, result)
	})
	if err != nil {
		return admin.ManagedAccount{}, err
	}
	return result, nil
}

// SetAccountEnabled 启用或停用管理员账号，并禁止当前管理员停用自己。
func (s *ManagementStore) SetAccountEnabled(ctx context.Context, command admin.SetManagedAccountEnabledCommand) (admin.ManagedAccount, error) {
	if !command.ActorAccountID.IsValid() || !command.AccountID.IsValid() || command.ExpectedVersion <= 0 || !idempotency.ValidKey(command.IdempotencyKey) || strings.TrimSpace(command.RequestID) == "" {
		return admin.ManagedAccount{}, admin.ErrInvalidManagementCommand
	}
	if !command.Enabled && command.ActorAccountID == command.AccountID {
		return admin.ManagedAccount{}, admin.ErrCannotDisableSelf
	}
	status := string(securityaccount.StatusDisabled)
	if command.Enabled {
		status = string(securityaccount.StatusActive)
	}
	now := time.Now().UTC()
	result := admin.ManagedAccount{}
	digest, err := idempotency.Digest(command)
	if err != nil {
		return result, err
	}
	request := idempotency.Request{ActorAccountID: command.ActorAccountID, OperationID: "admin.account.set_enabled", Key: command.IdempotencyKey, RequestDigest: digest, CreatedAt: now}
	err = s.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		client := s.pool.Client(txctx)
		writer := idempotency.NewPersistentWriter(idempotency.NewAdminEntRecords(client, s.ids))
		replay, claimErr := idempotency.ClaimResponse(txctx, writer, request, &result)
		if claimErr != nil || replay {
			return claimErr
		}
		row, findErr := client.AdminAccount.Query().Where(adminaccount.IDEQ(command.AccountID)).Only(txctx)
		if findErr != nil {
			return managementError(findErr)
		}
		updated, saveErr := client.AdminAccount.UpdateOne(row).Where(adminaccount.VersionEQ(command.ExpectedVersion)).SetStatus(status).SetVersion(command.ExpectedVersion + 1).SetUpdatedAt(now).Save(txctx)
		if saveErr != nil {
			return managementError(saveErr)
		}
		result = managedAccount(updated)
		auditID, idErr := s.ids.Next(txctx)
		if idErr != nil {
			return idErr
		}
		objectID, reason := result.ID.String(), "administrative_change"
		changes, _ := json.Marshal(struct{ Before, After string }{row.Status, result.Status})
		if auditErr := platformaudit.Append(txctx, database.Executor(txctx, s.pool), platformaudit.AdminLedger, platformaudit.Entry{ID: auditID, ActorAccountID: &command.ActorAccountID, ActorKind: "admin", ActionCode: "admin.account.status_changed", ObjectType: "admin_account", ObjectID: &objectID, RequestID: command.RequestID, Reason: &reason, Changes: changes, CreatedAt: now}); auditErr != nil {
			return auditErr
		}
		return idempotency.Complete(txctx, writer, request, result)
	})
	if err != nil {
		return admin.ManagedAccount{}, err
	}
	return result, nil
}

// ListAuditLogs 返回审计业务字段，不读取或暴露哈希链原始字节。
func (s *ManagementStore) ListAuditLogs(ctx context.Context, pageSize int) ([]admin.AuditLog, error) {
	if pageSize < 1 {
		pageSize = 50
	}
	if pageSize > 200 {
		pageSize = 200
	}
	rows, err := s.pool.Client(ctx).AdminAuditLog.Query().Select(
		adminauditlog.FieldID,
		adminauditlog.FieldSequence,
		adminauditlog.FieldActorAccountID,
		adminauditlog.FieldActorKind,
		adminauditlog.FieldActorIdentifier,
		adminauditlog.FieldActionCode,
		adminauditlog.FieldObjectType,
		adminauditlog.FieldObjectID,
		adminauditlog.FieldRequestID,
		adminauditlog.FieldReason,
		adminauditlog.FieldCreatedAt,
	).Order(avalonent.Desc(adminauditlog.FieldSequence)).Limit(pageSize).All(ctx)
	if err != nil {
		return nil, err
	}
	result := make([]admin.AuditLog, 0, len(rows))
	for _, row := range rows {
		value := admin.AuditLog{ID: row.ID, Sequence: row.Sequence, ActorKind: row.ActorKind, ActionCode: row.ActionCode, ObjectType: row.ObjectType, RequestID: row.RequestID, CreatedAt: row.CreatedAt}
		if row.ActorAccountID != nil {
			value.ActorAccountID = *row.ActorAccountID
		}
		if row.ActorIdentifier != nil {
			value.ActorIdentifier = *row.ActorIdentifier
		}
		if row.ObjectID != nil {
			value.ObjectID = *row.ObjectID
		}
		if row.Reason != nil {
			value.Reason = *row.Reason
		}
		result = append(result, value)
	}
	return result, nil
}

func managedAccount(row *avalonent.AdminAccount) admin.ManagedAccount {
	return admin.ManagedAccount{ID: row.ID, Username: row.Username, DisplayName: row.DisplayName, Status: row.Status, Version: row.Version, CreatedAt: row.CreatedAt, UpdatedAt: row.UpdatedAt}
}
func managementError(err error) error {
	if avalonent.IsNotFound(err) {
		return admin.ErrAdminAccountNotFound
	}
	var pg *pgconn.PgError
	if errors.As(err, &pg) && pg.Code == "23505" || avalonent.IsConstraintError(err) {
		return admin.ErrAdminAccountConflict
	}
	return err
}
