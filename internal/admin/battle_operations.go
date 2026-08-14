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
	// Side 是本场 Battle 内从一开始编号的稳定阵营位置。
	Side int16
	// ParticipantType 区分真人 PlayerCharacter 与服务端 Bot。
	ParticipantType string
	// InputType 表示该参与方被冻结为 party、team 或 generated 快照。
	InputType string
	// DisplayName 是 Battle 创建时冻结的参与方展示名称。
	DisplayName string
	// PlayerCharacterID 是真人参与方身份；Bot 参与方为 nil。
	PlayerCharacterID *snowflake.ID
	// BotCode 是 Bot 参与方冻结的策略代码；真人参与方为空。
	BotCode string
	// SourcePartyID 是 Encounter 真人参与方冻结输入的来源 Party；其它输入类型为零值。
	SourcePartyID snowflake.ID
	// SourcePartyVersion 是创建 Battle 时冻结的 Party 乐观版本；其它输入类型为零。
	SourcePartyVersion int64
	// FrozenMembers 是从不可变输入快照提取的有限成员诊断信息。
	FrozenMembers []BattleOperationsFrozenMember
}

// BattleOperationsFrozenMember 是管理端可见的冻结 Party 或生成对手成员摘要。
type BattleOperationsFrozenMember struct {
	// Position 是成员在冻结 Team 中从一开始的稳定位置。
	Position int32
	// CreatureID 是创建 Battle 时冻结的 Creature 资料身份。
	CreatureID snowflake.ID
	// PlayerCharacterCreatureID 是 Party 成员的 Owned Creature 身份；生成对手为零值。
	PlayerCharacterCreatureID snowflake.ID
	// Level 是创建 Battle 时冻结的成员等级。
	Level int32
	// CurrentHP 是 Party 进入 Encounter 时的持久生命；其它输入类型为零。
	CurrentHP int32
	// MaximumHP 是 Party 创建快照时计算出的恢复生命上限；其它输入类型为零。
	MaximumHP int32
}

// BattleOperationsRecoveredMember 是 Encounter 终局事务最终写入的 Owned Creature 生命。
type BattleOperationsRecoveredMember struct {
	// PlayerCharacterCreatureID 是终局事务实际更新的 Owned Creature。
	PlayerCharacterCreatureID snowflake.ID
	// CurrentHP 是终局事务最终提交的持久生命。
	CurrentHP int32
	// MaximumHP 是该场 Battle 创建时冻结的恢复生命上限。
	MaximumHP int32
}

// BattleOperationsEncounterView 组合 Pending Encounter 抽样事实与不可变 Checkpoint 终局结果。
type BattleOperationsEncounterView struct {
	// PendingEncounterID 是移动事务创建并被该 Battle 接受的待处理遭遇。
	PendingEncounterID snowflake.ID
	// EncounterTableID 是触发时实际参与抽样的遭遇表。
	EncounterTableID snowflake.ID
	// EncounterEntryID 是本次加权抽样实际命中的候选关系。
	EncounterEntryID snowflake.ID
	// EncounterLevel 是触发时在候选等级区间内确定的冻结等级。
	EncounterLevel int16
	// State 是 Pending Encounter 当前持久状态。
	State string
	// ExpiresAt 是 Pending Encounter 创建时冻结的失效时间。
	ExpiresAt time.Time
	// PlayerDefeated 表示 Battle 存在明确胜方且真人一方落败。
	PlayerDefeated bool
	// CheckpointRecovered 表示终局事务实际提交了位置与满生命恢复。
	CheckpointRecovered bool
	// CheckpointID 是终局事务实际采用的恢复点；未恢复时为零值。
	CheckpointID snowflake.ID
	// RecoveryLocationID 是终局事务实际写入的位置；未恢复时为零值。
	RecoveryLocationID snowflake.ID
	// RecoveredMembers 是终局事务最终写入的 Owned Creature 生命事实。
	RecoveredMembers []BattleOperationsRecoveredMember
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
	// Encounter 仅对 Encounter 来源 Battle 存在；其它来源保持 nil。
	Encounter *BattleOperationsEncounterView
}

// BattleOperationsProjectionQuery 定义管理端 Battle 运维分页与详情投影查询边界。
type BattleOperationsProjectionQuery interface {
	ListBattles(context.Context, BattleOperationsQuery) (BattleOperationsPage, error)
	GetBattleOperationsDetail(context.Context, snowflake.ID) (BattleOperationsDetail, error)
}
