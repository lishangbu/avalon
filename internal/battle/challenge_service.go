package battle

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/playercharacter"
	"github.com/lishangbu/avalon/internal/team"
)

var (
	// ErrChallengeCreationUnavailable 表示维护窗口、未启用赛制或不一致实时资料阻止了 Challenge 操作。
	ErrChallengeCreationUnavailable = errors.New("当前无法创建或接受 Challenge")
	// ErrChallengeActorMismatch 表示当前账号的活动 PlayerCharacter 不是该 Challenge 的发起方或接收方。
	ErrChallengeActorMismatch = errors.New("挑战操作者不匹配")
)

// ActiveCharacterQuery 读取账号当前跨设备共享的活动 PlayerCharacter 绑定和其展示名称。
type ActiveCharacterQuery interface {
	// GetActive 返回账号跨设备共享的当前活动角色绑定。
	GetActive(context.Context, snowflake.ID) (playercharacter.ActiveBinding, error)
	// GetOwned 验证指定角色属于账号并返回角色事实。
	GetOwned(context.Context, snowflake.ID, snowflake.ID) (playercharacter.PlayerCharacter, error)
}

// ChallengeTargetQuery 解析可被当前账号挑战的另一名在线活动 PlayerCharacter。
type ChallengeTargetQuery interface {
	// ResolveChallengeTarget 按展示名称解析可被当前账号挑战的在线活动角色。
	ResolveChallengeTarget(context.Context, snowflake.ID, string) (playercharacter.ChallengeTarget, error)
}

// TeamAdmission 在对战入口重新读取并验证指定账号活动角色拥有的 Team。
type TeamAdmission interface {
	// ValidateOwned 按账号与活动角色边界重新读取并校验可入场 Team。
	ValidateOwned(context.Context, snowflake.ID, snowflake.ID, snowflake.ID) (team.Team, error)
}

// ChallengeFormatQuery 读取当前实时资料中的 BattleFormat。
type ChallengeFormatQuery interface {
	// GetFormat 返回指定稳定 Identifier 的当前实时 BattleFormat。
	GetFormat(context.Context, snowflake.ID) (battleformat.Format, error)
}

// ChallengeReader 返回指定 Challenge 的完整冻结领域事实。
type ChallengeReader interface {
	// GetChallenge 返回指定 Challenge 的完整冻结事实。
	GetChallenge(context.Context, snowflake.ID) (Challenge, error)
}

// ChallengeRepository 保存 Challenge 生命周期及接受后创建的 Preview Battle。
type ChallengeRepository interface {
	// CreateChallenge 保存新的待处理 Challenge。
	CreateChallenge(context.Context, Challenge) error
	// AcceptChallenge 接受 Challenge 并在同一事务中创建 Preview Battle。
	AcceptChallenge(context.Context, snowflake.ID, snowflake.ID, team.Team, Format, time.Time) (Battle, error)
	// RejectChallenge 把接收方仍可处理的 Challenge 标记为 rejected。
	RejectChallenge(context.Context, snowflake.ID, snowflake.ID, time.Time) (Challenge, error)
	// WithdrawChallenge 把发起方仍可处理的 Challenge 标记为 withdrawn。
	WithdrawChallenge(context.Context, snowflake.ID, snowflake.ID, time.Time) (Challenge, error)
	// ExpireChallenge 将已到期且仍待处理的 Challenge 标记为 expired。
	ExpireChallenge(context.Context, snowflake.ID, time.Time) (Challenge, error)
}

// CreateChallengeApplicationCommand 是已认证 RPC 层传入的创建 Challenge 意图。
type CreateChallengeApplicationCommand struct {
	// TeamID 是发起方活动角色拥有且将被冻结的 Team 稳定 Identifier。
	TeamID snowflake.ID
	// TargetDisplayName 是目标在线活动角色的全局唯一展示名称。
	TargetDisplayName string
	// BattleFormatID 是必须同时启用 Challenge 可用性的实时 BattleFormat 稳定 Identifier。
	BattleFormatID snowflake.ID
}

// AcceptChallengeApplicationCommand 是接收方接受 Challenge 时重新验证并冻结 Team 的意图。
type AcceptChallengeApplicationCommand struct {
	// ChallengeID 是待接受 Challenge 的稳定 Identifier。
	ChallengeID snowflake.ID
	// TeamID 是接收方活动角色拥有且将被冻结的 Team 稳定 Identifier。
	TeamID snowflake.ID
}

// ChallengeApplicationService 在挑战领域、玩家角色、Team 入场和实时资料之间编排明确的应用用例。
//
// 它不构建通用“对战资料”聚合：每个读取边界仍归其原始领域所有，服务只冻结本次 Challenge 需要的
// 事实。接受后得到 Preview Battle；双方 Preview 完成后的战斗启动由 StartService 处理。
type ChallengeApplicationService struct {
	// reader 读取 Challenge 的完整冻结事实。
	reader ChallengeReader
	// repository 拥有 Challenge 生命周期和接受事务。
	repository ChallengeRepository
	// characters 读取调用账号的活动角色和展示名称。
	characters ActiveCharacterQuery
	// targets 在不向公共 API 暴露 Identifier 的前提下解析挑战对象。
	targets ChallengeTargetQuery
	// teams 重新加载并校验即将冻结的 Team。
	teams TeamAdmission
	// formats 读取挑战入口允许使用的当前实时赛制。
	formats ChallengeFormatQuery
	// rules 在冻结 Team 写入 Challenge 前执行 Clause 与 Restriction 的入场校验。
	rules *BattleFormatRuleCompiler
	// newID 创建 Challenge 稳定 Identifier。
	newID snowflake.Source
	// now 提供 Challenge 固定生命周期的权威时间。
	now func() time.Time
}

// NewChallengeApplicationServiceWithRules 创建会在 Team 入场边界强制执行赛制规则的 Challenge 服务。
func NewChallengeApplicationServiceWithRules(
	reader ChallengeReader,
	repository ChallengeRepository,
	characters ActiveCharacterQuery,
	targets ChallengeTargetQuery,
	teams TeamAdmission,
	formats ChallengeFormatQuery,
	rules *BattleFormatRuleCompiler,
	newID snowflake.Source,
	now func() time.Time,
) *ChallengeApplicationService {
	if now == nil {
		now = time.Now
	}
	return &ChallengeApplicationService{
		reader: reader, repository: repository, characters: characters, targets: targets, teams: teams, formats: formats,
		rules: rules, newID: newID, now: now,
	}
}

// Create 为当前账号的活动 PlayerCharacter 创建一个五分钟有效、已冻结发起方 Team 和赛制的 Challenge。
func (service *ChallengeApplicationService) Create(
	ctx context.Context,
	accountID snowflake.ID,
	command CreateChallengeApplicationCommand,
) (Challenge, error) {
	if service == nil || service.repository == nil || service.characters == nil || service.targets == nil ||
		service.teams == nil || service.formats == nil || service.newID == nil || accountID == snowflake.ID(0) ||
		command.TeamID == snowflake.ID(0) || command.BattleFormatID == snowflake.ID(0) {
		return Challenge{}, ErrChallengeCreationUnavailable
	}
	active, err := service.characters.GetActive(ctx, accountID)
	if err != nil {
		return Challenge{}, err
	}
	challenger, err := service.characters.GetOwned(ctx, accountID, active.PlayerCharacterID)
	if err != nil {
		return Challenge{}, err
	}
	target, err := service.targets.ResolveChallengeTarget(ctx, accountID, command.TargetDisplayName)
	if err != nil {
		return Challenge{}, err
	}
	challengerTeam, err := service.teams.ValidateOwned(ctx, accountID, active.PlayerCharacterID, command.TeamID)
	if err != nil {
		return Challenge{}, err
	}
	format, err := service.formats.GetFormat(ctx, command.BattleFormatID)
	if err != nil {
		return Challenge{}, err
	}
	if !format.Enabled || !format.Availability.Challenge || len(challengerTeam.Members) != int(format.RosterCount) {
		return Challenge{}, ErrChallengeCreationUnavailable
	}
	if err := service.validateTeamRules(ctx, format, challengerTeam); err != nil {
		return Challenge{}, err
	}
	formatSnapshot, err := json.Marshal(format)
	if err != nil {
		return Challenge{}, fmt.Errorf("编码 Challenge 赛制快照: %w", err)
	}
	challenge, err := NewChallenge(ctx, CreateChallengeCommand{
		ChallengerAccountID: accountID, ChallengerPlayerCharacterID: challenger.ID, ChallengerDisplayName: challenger.DisplayName,
		ChallengerTeam: challengerTeam, TargetAccountID: target.AccountID, TargetPlayerCharacterID: target.PlayerCharacterID,
		TargetDisplayName: target.DisplayName, BattleFormatID: format.ID, BattleFormatSnapshot: formatSnapshot,
	}, service.newID, service.now)
	if err != nil {
		return Challenge{}, err
	}
	if err := service.repository.CreateChallenge(ctx, challenge); err != nil {
		return Challenge{}, err
	}
	return challenge, nil
}

// Accept 重新验证接收方活动角色、Team 与当前赛制可用性，并原子接受 Challenge、创建 Preview Battle。
func (service *ChallengeApplicationService) Accept(
	ctx context.Context,
	accountID snowflake.ID,
	command AcceptChallengeApplicationCommand,
) (Battle, error) {
	if service == nil || service.reader == nil || service.repository == nil || service.characters == nil || service.teams == nil || service.formats == nil ||
		accountID == snowflake.ID(0) || command.ChallengeID == snowflake.ID(0) || command.TeamID == snowflake.ID(0) {
		return Battle{}, ErrChallengeCreationUnavailable
	}
	challenge, err := service.reader.GetChallenge(ctx, command.ChallengeID)
	if err != nil {
		return Battle{}, err
	}
	active, err := service.characters.GetActive(ctx, accountID)
	if err != nil {
		return Battle{}, err
	}
	if challenge.TargetAccountID != accountID || challenge.TargetPlayerCharacterID != active.PlayerCharacterID {
		return Battle{}, ErrChallengeActorMismatch
	}
	challengerTeam, err := service.teams.ValidateOwned(
		ctx, challenge.ChallengerAccountID, challenge.ChallengerPlayerCharacterID, challenge.ChallengerTeam.SourceTeamID,
	)
	if err != nil {
		return Battle{}, err
	}
	// Challenge 明确冻结发起时的 Team；若拥有者随后修改了这支 Team，就要求其重新发起邀请，不能把
	// 旧快照静默带入新 Battle。
	if challengerTeam.Version != challenge.ChallengerTeam.SourceTeamVersion {
		return Battle{}, ErrChallengeCreationUnavailable
	}
	targetTeam, err := service.teams.ValidateOwned(ctx, accountID, active.PlayerCharacterID, command.TeamID)
	if err != nil {
		return Battle{}, err
	}
	currentFormat, err := service.formats.GetFormat(ctx, challenge.BattleFormatID)
	if err != nil {
		return Battle{}, err
	}
	if !currentFormat.Enabled || !currentFormat.Availability.Challenge ||
		len(challengerTeam.Members) != int(currentFormat.RosterCount) || len(targetTeam.Members) != int(currentFormat.RosterCount) {
		return Battle{}, ErrChallengeCreationUnavailable
	}
	if err := service.validateTeamRules(ctx, currentFormat, challengerTeam); err != nil {
		return Battle{}, err
	}
	if err := service.validateTeamRules(ctx, currentFormat, targetTeam); err != nil {
		return Battle{}, err
	}
	frozenFormat, err := decodeFrozenFormat(challenge.BattleFormatSnapshot)
	if err != nil {
		return Battle{}, err
	}
	format, err := sessionFormatFromBattleFormat(frozenFormat)
	if err != nil {
		return Battle{}, err
	}
	return service.repository.AcceptChallenge(
		ctx, challenge.ID, active.PlayerCharacterID, targetTeam, format, service.now().UTC(),
	)
}

// validateTeamRules 仅在完整玩家 Server 装配规则编译器时执行 Team 入场校验。
//
// 旧的离线查询和针对单一 Challenge 转换的单元测试可以省略该可选依赖；正式构造器使用带规则的版本，
// 因而不会开放绕过 Clause 或 Restriction 的玩家入口。
func (service *ChallengeApplicationService) validateTeamRules(
	ctx context.Context,
	format battleformat.Format,
	value team.Team,
) error {
	if service.rules == nil {
		return nil
	}
	err := service.rules.ValidateTeam(ctx, format, value)
	if errors.Is(err, ErrBattleFormatRuleCompilation) || errors.Is(err, ErrBattleFormatTeamRuleViolation) {
		return fmt.Errorf("%w: %v", ErrChallengeCreationUnavailable, err)
	}
	return err
}

// Get 返回当前账号作为发起方或接收方参与的 Challenge；其他账号统一视为资源不存在。
func (service *ChallengeApplicationService) Get(ctx context.Context, accountID, challengeID snowflake.ID) (Challenge, error) {
	if service == nil || service.reader == nil || accountID == snowflake.ID(0) || challengeID == snowflake.ID(0) {
		return Challenge{}, ErrChallengeActorMismatch
	}
	challenge, err := service.reader.GetChallenge(ctx, challengeID)
	if err != nil {
		return Challenge{}, err
	}
	if challenge.ChallengerAccountID != accountID && challenge.TargetAccountID != accountID {
		return Challenge{}, ErrChallengeActorMismatch
	}
	return challenge, nil
}

// Reject 让当前账号的活动接收方拒绝一个仍处于 pending 的 Challenge。
func (service *ChallengeApplicationService) Reject(ctx context.Context, accountID, challengeID snowflake.ID) (Challenge, error) {
	challenge, active, err := service.actingChallenge(ctx, accountID, challengeID)
	if err != nil {
		return Challenge{}, err
	}
	if challenge.TargetAccountID != accountID || challenge.TargetPlayerCharacterID != active.PlayerCharacterID {
		return Challenge{}, ErrChallengeActorMismatch
	}
	return service.repository.RejectChallenge(ctx, challengeID, active.PlayerCharacterID, service.now().UTC())
}

// Withdraw 让当前账号的活动发起方撤回一个仍处于 pending 的 Challenge。
func (service *ChallengeApplicationService) Withdraw(ctx context.Context, accountID, challengeID snowflake.ID) (Challenge, error) {
	challenge, active, err := service.actingChallenge(ctx, accountID, challengeID)
	if err != nil {
		return Challenge{}, err
	}
	if challenge.ChallengerAccountID != accountID || challenge.ChallengerPlayerCharacterID != active.PlayerCharacterID {
		return Challenge{}, ErrChallengeActorMismatch
	}
	return service.repository.WithdrawChallenge(ctx, challengeID, active.PlayerCharacterID, service.now().UTC())
}

// Expire 在读取或周期任务发现 Challenge 到期后尝试将其转换为明确终态。
func (service *ChallengeApplicationService) Expire(ctx context.Context, challengeID snowflake.ID) (Challenge, error) {
	if service == nil || service.repository == nil || challengeID == snowflake.ID(0) {
		return Challenge{}, ErrChallengeCreationUnavailable
	}
	return service.repository.ExpireChallenge(ctx, challengeID, service.now().UTC())
}

func (service *ChallengeApplicationService) actingChallenge(
	ctx context.Context,
	accountID snowflake.ID,
	challengeID snowflake.ID,
) (Challenge, playercharacter.ActiveBinding, error) {
	if service == nil || service.reader == nil || service.characters == nil || accountID == snowflake.ID(0) || challengeID == snowflake.ID(0) {
		return Challenge{}, playercharacter.ActiveBinding{}, ErrChallengeActorMismatch
	}
	challenge, err := service.reader.GetChallenge(ctx, challengeID)
	if err != nil {
		return Challenge{}, playercharacter.ActiveBinding{}, err
	}
	active, err := service.characters.GetActive(ctx, accountID)
	if err != nil {
		return Challenge{}, playercharacter.ActiveBinding{}, err
	}
	return challenge, active, nil
}

func decodeFrozenFormat(snapshot json.RawMessage) (battleformat.Format, error) {
	var format battleformat.Format
	if !json.Valid(snapshot) || json.Unmarshal(snapshot, &format) != nil || format.ID == snowflake.ID(0) {
		return battleformat.Format{}, ErrChallengeCreationUnavailable
	}
	return format, nil
}

func sessionFormatFromBattleFormat(format battleformat.Format) (Format, error) {
	if format.RosterCount < 1 || format.RosterCount > 6 || format.SelectCount < 1 ||
		format.SelectCount > format.RosterCount || format.ActiveParticipantsPerSide < 1 ||
		format.ActiveParticipantsPerSide > format.SelectCount {
		return Format{}, ErrChallengeCreationUnavailable
	}
	result := Format{
		RosterCount: uint8(format.RosterCount), SelectCount: uint8(format.SelectCount),
		ActiveParticipantsPerSide: uint8(format.ActiveParticipantsPerSide),
		PreviewDuration:           time.Duration(format.Deadlines.PreviewSeconds) * time.Second,
		TurnDuration:              time.Duration(format.Deadlines.TurnSeconds) * time.Second,
		BattleDuration:            time.Duration(format.Deadlines.BattleSeconds) * time.Second,
	}
	if !validBattleFormat(result) {
		return Format{}, ErrChallengeCreationUnavailable
	}
	return result, nil
}
