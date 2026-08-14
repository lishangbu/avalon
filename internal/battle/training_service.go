package battle

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/team"
)

// TrainingBattleRepository 保存由 Training 用例冻结出的 Preview Battle。
type TrainingBattleRepository interface {
	// Create 保存新建的 Preview Training Battle。
	Create(context.Context, Battle) error
}

// TrainingBotResolver 基于已校验的真人 Team 与当前赛制生成可冻结的 Bot Profile。
//
// 返回 Profile 的 Team 必须由生成器独立拥有，不能与调用者输入共享可变切片；策略代码和版本会保存到
// Battle Participant，后续策略升级只能注册新版本。
type TrainingBotResolver interface {
	// ResolveTrainingBot 基于已校验真人 Team 和赛制生成可冻结的 Bot Profile。
	ResolveTrainingBot(context.Context, string, team.Team, battleformat.Format) (BotProfile, error)
}

// CreateTrainingApplicationCommand 是当前活动角色创建一场训练战的输入。
type CreateTrainingApplicationCommand struct {
	// TeamID 是当前活动角色拥有并将在创建时重新验证的 Team 稳定 Identifier。
	TeamID snowflake.ID
	// BattleFormatID 是必须启用 Training 可用性的实时赛制稳定 Identifier。
	BattleFormatID snowflake.ID
	// BotCode 是想使用的当前已启用 Bot 稳定代码；实际版本在创建时由资料库解析并冻结。
	BotCode string
}

// TrainingApplicationService 编排活动角色、Team 入场、实时赛制和 Bot 生成器以创建训练战。
//
// Training 与 Challenge 共享 Battle 生命周期但不复用其邀请模型：训练战没有目标账号、不会写 Challenge，
// Bot 也不占用账号。两者保留各自的命令和资料形状，避免把不同领域硬合并为通用对战请求。
type TrainingApplicationService struct {
	// repository 保存 Battle、真人账号占用和 Bot Participant 快照。
	repository TrainingBattleRepository
	// characters 读取当前账号的活动角色及其展示名称。
	characters ActiveCharacterReader
	// teams 在入场边界重新读取并校验真人 Team。
	teams TeamAdmission
	// formats 读取可用于 Training 的当前赛制。
	formats ChallengeFormatReader
	// rules 在真人与 Bot Team 冻结为 Training Battle 前执行 Clause 与 Restriction 入场校验。
	rules *BattleFormatRuleCompiler
	// bots 为真人 Team 和赛制生成版本固定的 Bot Profile。
	bots TrainingBotResolver
	// newID 创建 Training Battle 稳定 Identifier。
	newID snowflake.Source
	// now 提供 Battle 创建时的权威时间。
	now func() time.Time
}

// NewTrainingApplicationServiceWithRules 创建会在真人与 Bot Team 入场边界执行赛制规则的 Training 服务。
func NewTrainingApplicationServiceWithRules(
	repository TrainingBattleRepository,
	characters ActiveCharacterReader,
	teams TeamAdmission,
	formats ChallengeFormatReader,
	bots TrainingBotResolver,
	rules *BattleFormatRuleCompiler,
	newID snowflake.Source,
	now func() time.Time,
) *TrainingApplicationService {
	if now == nil {
		now = time.Now
	}
	return &TrainingApplicationService{
		repository: repository, characters: characters, teams: teams, formats: formats,
		bots: bots, rules: rules, newID: newID, now: now,
	}
}

// Create 创建真人对固定版本 Bot 的 Preview Training Battle。
func (service *TrainingApplicationService) Create(
	ctx context.Context,
	accountID snowflake.ID,
	command CreateTrainingApplicationCommand,
) (Battle, error) {
	if service == nil || service.repository == nil || service.characters == nil || service.teams == nil || service.formats == nil ||
		service.bots == nil || service.newID == nil || accountID == snowflake.ID(0) || command.TeamID == snowflake.ID(0) ||
		command.BattleFormatID == snowflake.ID(0) || strings.TrimSpace(command.BotCode) == "" {
		return Battle{}, ErrChallengeCreationUnavailable
	}
	active, err := service.characters.GetActive(ctx, accountID)
	if err != nil {
		return Battle{}, err
	}
	player, err := service.characters.GetOwned(ctx, accountID, active.PlayerCharacterID)
	if err != nil {
		return Battle{}, err
	}
	playerTeam, err := service.teams.ValidateOwned(ctx, accountID, active.PlayerCharacterID, command.TeamID)
	if err != nil {
		return Battle{}, err
	}
	format, err := service.formats.GetFormat(ctx, command.BattleFormatID)
	if err != nil {
		return Battle{}, err
	}
	if !format.Enabled || !format.Availability.Training || len(playerTeam.Members) != int(format.RosterCount) {
		return Battle{}, ErrChallengeCreationUnavailable
	}
	if err := service.validateTeamRules(ctx, format, playerTeam); err != nil {
		return Battle{}, err
	}
	bot, err := service.bots.ResolveTrainingBot(ctx, command.BotCode, playerTeam, format)
	if err != nil {
		return Battle{}, err
	}
	// Bot Profile 保存的是不可变 TeamSnapshot；规则校验只依赖成员引用，因此在这里构造最小 Team
	// 视图，保证镜像 Bot 或后续资料化 Bot 也不能绕过同一套入场约束。
	if err := service.validateTeamRules(ctx, format, team.Team{Members: bot.Team.Members}); err != nil {
		return Battle{}, err
	}
	formatSnapshot, err := json.Marshal(format)
	if err != nil {
		return Battle{}, fmt.Errorf("编码 Training 赛制快照: %w", err)
	}
	sessionFormat, err := sessionFormatFromBattleFormat(format)
	if err != nil {
		return Battle{}, err
	}
	session, err := NewTrainingBattle(ctx, NewTrainingBattleCommand{
		AccountID: accountID, PlayerCharacterID: player.ID, DisplayName: player.DisplayName, Team: playerTeam,
		BattleFormatID: format.ID, BattleFormatSnapshot: formatSnapshot,
		Format: sessionFormat, Bot: bot,
	}, service.newID, service.now)
	if err != nil {
		return Battle{}, err
	}
	if err := service.repository.Create(ctx, session); err != nil {
		return Battle{}, err
	}
	return session, nil
}

// validateTeamRules 把可预期的赛制规则拒绝映射为 Training Battle 不可创建，并保留目录读取故障供调用方记录。
func (service *TrainingApplicationService) validateTeamRules(
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
