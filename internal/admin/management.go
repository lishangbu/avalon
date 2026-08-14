package admin

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrInvalidManagementCommand 表示管理员维护命令字段无效。
	ErrInvalidManagementCommand = errors.New("管理员维护命令无效")
	// ErrAdminAccountConflict 表示用户名或乐观版本冲突。
	ErrAdminAccountConflict = errors.New("管理员账号冲突")
	// ErrAdminAccountNotFound 表示管理员账号不存在。
	ErrAdminAccountNotFound = errors.New("管理员账号不存在")
	// ErrCannotDisableSelf 表示管理员试图停用当前账号。
	ErrCannotDisableSelf = errors.New("不能停用当前管理员账号")
)

// ManagedAccount 是不包含密码资料的管理员账号维护视图。
type ManagedAccount struct {
	ID                    snowflake.ID
	Username, DisplayName string
	Status                string
	Version               int64
	CreatedAt, UpdatedAt  time.Time
}

// AuditLog 是隐藏哈希链字节的管理员安全审计视图。
type AuditLog struct {
	ID                                                                              snowflake.ID
	Sequence                                                                        int64
	ActorAccountID                                                                  snowflake.ID
	ActorKind, ActorIdentifier, ActionCode, ObjectType, ObjectID, RequestID, Reason string
	CreatedAt                                                                       time.Time
}

// CreateManagedAccountCommand 是管理员账号创建命令。
type CreateManagedAccountCommand struct {
	ActorAccountID                                             snowflake.ID
	Username, DisplayName, Password, IdempotencyKey, RequestID string
}

// SetManagedAccountEnabledCommand 是带乐观版本的账号状态命令。
type SetManagedAccountEnabledCommand struct {
	ActorAccountID, AccountID snowflake.ID
	Enabled                   bool
	ExpectedVersion           int64
	IdempotencyKey, RequestID string
}

// ManagementRepository 是管理员账号和审计维护的关系型持久化端口。
type ManagementRepository interface {
	ListAccounts(context.Context, int) ([]ManagedAccount, error)
	CreateAccount(context.Context, CreateManagedAccountCommand) (ManagedAccount, error)
	SetAccountEnabled(context.Context, SetManagedAccountEnabledCommand) (ManagedAccount, error)
	ListAuditLogs(context.Context, int) ([]AuditLog, error)
}
