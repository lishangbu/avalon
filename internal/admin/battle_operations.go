package admin

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var (
	// ErrInvalidBattleOperationsQuery 表示 Battle 运维筛选或分页参数无效。
	ErrInvalidBattleOperationsQuery = errors.New("Battle 运维查询参数无效")
	// ErrBattleOperationsNotFound 表示目标 Battle 不存在。
	ErrBattleOperationsNotFound = errors.New("Battle 不存在")
)

// BattleOperationsQuery 保存管理员 Battle 列表的分页和可选筛选条件。
type BattleOperationsQuery struct {
	Page       int
	PageSize   int
	Mode       string
	SourceType string
	Status     string
}

// BattleOperationsItem 是一场 Battle 的只读生命周期摘要。
type BattleOperationsItem struct {
	ID             snowflake.ID
	Mode           string
	SourceType     string
	Status         string
	StateVersion   int64
	TerminalReason string
	CreatedAt      time.Time
	UpdatedAt      time.Time
	StartedAt      *time.Time
	CompletedAt    *time.Time
}

// BattleOperationsParticipant 是 Battle 创建时冻结的参赛主体摘要。
type BattleOperationsParticipant struct {
	Side              int16
	ParticipantType   string
	InputType         string
	DisplayName       string
	PlayerCharacterID *snowflake.ID
	BotCode           string
}

// BattleRuntimeLeaseView 是当前 Battle Runtime Lease 的只读状态。
type BattleRuntimeLeaseView struct {
	HolderID     string
	FencingToken int64
	ExpiresAt    time.Time
	RenewedAt    time.Time
}

// BattleRecoveryAttemptView 是一次不可变 Battle 恢复尝试的运维摘要。
type BattleRecoveryAttemptView struct {
	ID            snowflake.ID
	AttemptNumber int32
	State         string
	AvailableAt   time.Time
	ClaimedBy     string
	FailureReason string
}

// BattleOperationsPage 返回 Battle 运维摘要页和精确总数。
type BattleOperationsPage struct {
	Items []BattleOperationsItem
	Total int64
}

// BattleOperationsDetail 组合单场 Battle 的 Participant、Lease、Recovery 与 Outbox 状态。
type BattleOperationsDetail struct {
	Battle             BattleOperationsItem
	Participants       []BattleOperationsParticipant
	RuntimeLease       *BattleRuntimeLeaseView
	RecoveryAttempts   []BattleRecoveryAttemptView
	PendingOutboxCount int
}

// BattleOperationsReader 定义管理端只读 Battle 运维查询边界。
type BattleOperationsReader interface {
	ListBattles(context.Context, BattleOperationsQuery) (BattleOperationsPage, error)
	GetBattleOperationsDetail(context.Context, snowflake.ID) (BattleOperationsDetail, error)
}
