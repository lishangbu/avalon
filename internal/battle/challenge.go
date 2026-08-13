// Package battle 定义 Challenge、Training 与权威 Battle 的领域生命周期。
package battle

import (
	"context"
	"encoding/json"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/team"
)

const (
	// ChallengeLifetime 是已创建邀请等待接收方处理的固定时长。
	ChallengeLifetime = 5 * time.Minute
)

var (
	// ErrInvalidChallenge 表示邀请缺少身份、冻结资料或其他不可恢复的创建事实。
	ErrInvalidChallenge = errors.New("挑战无效")
	// ErrChallengeNotPending 表示调用者尝试改变已经进入终态的邀请。
	ErrChallengeNotPending = errors.New("挑战已不处于待处理状态")
	// ErrChallengeExpired 表示邀请在状态改变前已超过固定有效期。
	ErrChallengeExpired = errors.New("挑战已过期")
	// ErrChallengeRecipientMismatch 表示只有受邀 PlayerCharacter 可以接受或拒绝邀请。
	ErrChallengeRecipientMismatch = errors.New("挑战接收方不匹配")
)

// ChallengeStatus 是邀请生命周期中持久化的稳定状态值。
type ChallengeStatus string

const (
	// ChallengePending 表示邀请仍在等待目标角色响应。
	ChallengePending ChallengeStatus = "pending"
	// ChallengeAccepted 表示已接受并由同一事务创建 Battle。
	ChallengeAccepted ChallengeStatus = "accepted"
	// ChallengeRejected 表示接收方明确拒绝了邀请。
	ChallengeRejected ChallengeStatus = "rejected"
	// ChallengeWithdrawn 表示发起方撤回了邀请。
	ChallengeWithdrawn ChallengeStatus = "withdrawn"
	// ChallengeExpired 表示等待时间到期且尚未接受。
	ChallengeExpired ChallengeStatus = "expired"
	// ChallengeSuperseded 表示其中一方进入其他对局，因此该邀请不再有效。
	ChallengeSuperseded ChallengeStatus = "superseded"
	// ChallengeCancelled 表示角色归档、实时资料失效等系统原因取消邀请。
	ChallengeCancelled ChallengeStatus = "cancelled"
)

// TeamSnapshot 是从可变 Team 复制出的不可变参赛阵容事实。
//
// Snapshot 不依赖后续 Team 编辑或实时资料变更；创建/接受入口仍必须先完成实时资料校验。
type TeamSnapshot struct {
	// SourceTeamID 是生成快照时的可变 Team 稳定 Identifier，仅用于审计和来源追踪。
	SourceTeamID snowflake.ID
	// SourceTeamVersion 是生成快照时 Team 的乐观版本。
	SourceTeamVersion int64
	// Members 保存各成员、技能和培养参数的深拷贝，位置在此后保持稳定。
	Members []team.Member
}

// Challenge 是两个不同账号角色之间的短期、可审计对局邀请。
type Challenge struct {
	// ID 是邀请的 Snowflake Identifier 稳定标识。
	ID snowflake.ID
	// ChallengerAccountID 是发起方玩家安全域账号。
	ChallengerAccountID snowflake.ID
	// ChallengerPlayerCharacterID 是发起邀请时的活动角色。
	ChallengerPlayerCharacterID snowflake.ID
	// ChallengerDisplayName 保存创建时冻结的发起方展示名称。
	ChallengerDisplayName string
	// ChallengerTeam 保存创建入口已实时校验的发起方阵容快照。
	ChallengerTeam TeamSnapshot
	// TargetAccountID 是接收邀请的另一玩家安全域账号。
	TargetAccountID snowflake.ID
	// TargetPlayerCharacterID 是接收邀请时必须仍为活动角色的目标。
	TargetPlayerCharacterID snowflake.ID
	// TargetDisplayName 保存创建时冻结的目标展示名称。
	TargetDisplayName string
	// BattleFormatID 是当前启用 BattleFormat 的稳定 Identifier。
	BattleFormatID snowflake.ID
	// BattleFormatSnapshot 是创建时序列化的赛制与规则组合，不引用可变资料行。
	BattleFormatSnapshot json.RawMessage
	// Status 是邀请当前生命周期状态。
	Status ChallengeStatus
	// TerminalReason 保存状态离开 pending 后的稳定原因码。
	TerminalReason string
	// ExpiresAt 是创建时固定的五分钟到期时间。
	ExpiresAt time.Time
	// ResolvedAt 是邀请进入非 pending 状态的时间；pending 时为零值。
	ResolvedAt time.Time
	// Version 是持久化状态转换使用的乐观版本。
	Version int64
	// CreatedAt 是创建时间。
	CreatedAt time.Time
	// UpdatedAt 是最后一次状态变化时间。
	UpdatedAt time.Time
}

// CreateChallengeCommand 包含创建邀请时必须一次冻结的身份、Team 和赛制事实。
type CreateChallengeCommand struct {
	// ChallengerAccountID 是发起方账号。
	ChallengerAccountID snowflake.ID
	// ChallengerPlayerCharacterID 是发起方活动角色。
	ChallengerPlayerCharacterID snowflake.ID
	// ChallengerDisplayName 是发起方当前展示名称。
	ChallengerDisplayName string
	// ChallengerTeam 是已由入场服务重新读取并实时校验的 Team。
	ChallengerTeam team.Team
	// TargetAccountID 是目标账号。
	TargetAccountID snowflake.ID
	// TargetPlayerCharacterID 是目标活动角色。
	TargetPlayerCharacterID snowflake.ID
	// TargetDisplayName 是目标角色当前展示名称。
	TargetDisplayName string
	// BattleFormatID 是当前可用于 Challenge 的启用赛制。
	BattleFormatID snowflake.ID
	// BattleFormatSnapshot 是赛制及相关规则的规范 JSON 快照。
	BattleFormatSnapshot json.RawMessage
}

// NewChallenge 创建状态为 pending 的固定五分钟邀请。
func NewChallenge(
	ctx context.Context,
	command CreateChallengeCommand,
	newID snowflake.Source,
	now func() time.Time,
) (Challenge, error) {
	if !validCreateChallenge(command) || newID == nil || now == nil {
		return Challenge{}, ErrInvalidChallenge
	}
	id, err := newID.Next(ctx)
	if err != nil {
		return Challenge{}, err
	}
	createdAt := now().UTC()
	return Challenge{
		ID:                          id,
		ChallengerAccountID:         command.ChallengerAccountID,
		ChallengerPlayerCharacterID: command.ChallengerPlayerCharacterID,
		ChallengerDisplayName:       command.ChallengerDisplayName,
		ChallengerTeam:              FreezeTeam(command.ChallengerTeam),
		TargetAccountID:             command.TargetAccountID,
		TargetPlayerCharacterID:     command.TargetPlayerCharacterID,
		TargetDisplayName:           command.TargetDisplayName,
		BattleFormatID:              command.BattleFormatID,
		BattleFormatSnapshot:        append(json.RawMessage(nil), command.BattleFormatSnapshot...),
		Status:                      ChallengePending,
		ExpiresAt:                   createdAt.Add(ChallengeLifetime),
		Version:                     1,
		CreatedAt:                   createdAt,
		UpdatedAt:                   createdAt,
	}, nil
}

// Accept 将 pending Challenge 变为 accepted；调用方必须在同一数据库事务创建 Battle、
// 写入账号占用并 Supersede 其他邀请。
func (value Challenge) Accept(targetPlayerCharacterID snowflake.ID, acceptedAt time.Time) (Challenge, error) {
	if targetPlayerCharacterID != value.TargetPlayerCharacterID {
		return Challenge{}, ErrChallengeRecipientMismatch
	}
	return value.resolve(ChallengeAccepted, "accepted", acceptedAt)
}

// Reject 将 pending Challenge 变为接收方明确拒绝的终态。
func (value Challenge) Reject(targetPlayerCharacterID snowflake.ID, rejectedAt time.Time) (Challenge, error) {
	if targetPlayerCharacterID != value.TargetPlayerCharacterID {
		return Challenge{}, ErrChallengeRecipientMismatch
	}
	return value.resolve(ChallengeRejected, "rejected", rejectedAt)
}

// Withdraw 将 pending Challenge 变为发起方主动撤回的终态。
func (value Challenge) Withdraw(challengerPlayerCharacterID snowflake.ID, withdrawnAt time.Time) (Challenge, error) {
	if challengerPlayerCharacterID != value.ChallengerPlayerCharacterID {
		return Challenge{}, ErrChallengeRecipientMismatch
	}
	return value.resolve(ChallengeWithdrawn, "withdrawn", withdrawnAt)
}

// Expire 将已超过有效期的 pending Challenge 标记为过期。
func (value Challenge) Expire(observedAt time.Time) (Challenge, error) {
	if value.Status != ChallengePending {
		return Challenge{}, ErrChallengeNotPending
	}
	if observedAt.UTC().Before(value.ExpiresAt) {
		return Challenge{}, ErrChallengeExpired
	}
	// resolve 会拒绝到期时刻及之后的普通状态转换，防止接受或拒绝已经
	// 到期的邀请；过期转换本身是该规则唯一的例外，因此在这里直接落为终态。
	observedAt = observedAt.UTC()
	value.Status = ChallengeExpired
	value.TerminalReason = "expired"
	value.ResolvedAt = observedAt
	value.UpdatedAt = observedAt
	value.Version++
	return value, nil
}

// FreezeTeam 深拷贝可变 Team 的成员、技能和培养数据，避免后续命令改写历史事实。
func FreezeTeam(value team.Team) TeamSnapshot {
	members := make([]team.Member, len(value.Members))
	for index, member := range value.Members {
		members[index] = member
		members[index].Skills = append([]team.MemberSkill(nil), member.Skills...)
		members[index].Stats = append([]team.MemberStat(nil), member.Stats...)
	}
	return TeamSnapshot{SourceTeamID: value.ID, SourceTeamVersion: value.Version, Members: members}
}

func validCreateChallenge(command CreateChallengeCommand) bool {
	return command.ChallengerAccountID != snowflake.ID(0) && command.TargetAccountID != snowflake.ID(0) &&
		command.ChallengerAccountID != command.TargetAccountID &&
		command.ChallengerPlayerCharacterID != snowflake.ID(0) && command.TargetPlayerCharacterID != snowflake.ID(0) &&
		command.ChallengerPlayerCharacterID != command.TargetPlayerCharacterID &&
		command.ChallengerDisplayName != "" && command.TargetDisplayName != "" &&
		command.ChallengerTeam.ID != snowflake.ID(0) && command.ChallengerTeam.Version >= 1 &&
		len(command.ChallengerTeam.Members) > 0 && command.BattleFormatID != snowflake.ID(0) &&
		json.Valid(command.BattleFormatSnapshot)
}

func (value Challenge) resolve(status ChallengeStatus, reason string, resolvedAt time.Time) (Challenge, error) {
	if value.Status != ChallengePending {
		return Challenge{}, ErrChallengeNotPending
	}
	resolvedAt = resolvedAt.UTC()
	if !resolvedAt.Before(value.ExpiresAt) {
		return Challenge{}, ErrChallengeExpired
	}
	value.Status = status
	value.TerminalReason = reason
	value.ResolvedAt = resolvedAt
	value.UpdatedAt = resolvedAt
	value.Version++
	return value, nil
}
