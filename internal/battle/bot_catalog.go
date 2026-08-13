package battle

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"strings"
	"unicode/utf8"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/team"
)

const (
	botDefinitionSchemaVersion uint32 = 1
	botPlannerFirstAvailable          = "first_available"
	botGeneratorMirror                = "mirror"
	botGeneratorTemplate              = "template"
)

var (
	// ErrBotDefinitionInvalid 表示持久化的 Bot 定义不符合当前受支持的严格配置契约。
	//
	// 定义无效时必须阻止新的 Training Battle；不能猜测默认值或在运行中解释未知字段，否则已冻结
	// Battle 将无法可靠重放。
	ErrBotDefinitionInvalid = errors.New("对战机器人策略定义无效")
)

// BotStrategyDefinition 是按代码和版本保存到实时资料库、并在创建 Training Battle 时完整冻结的 Bot 配置。
//
// 它刻意不使用动态脚本或反射。Planner 与 Generator 只能选择 Go 代码明确注册的有限种类，JSON 只提供
// 参数和 Team 模板；因此管理员可以管理资料，同时不会把不受控代码带入 Server 进程。
type BotStrategyDefinition struct {
	// SchemaVersion 是定义 JSON 的稳定结构版本；当前仅支持 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// DisplayName 是创建 Battle 时显示给玩家的冻结 Bot 名称。
	DisplayName string `json:"displayName"`
	// Planner 指定生成合法回合选择的确定性算法及其受限保底算法。
	Planner BotPlannerDefinition `json:"planner"`
	// Generator 指定生成 Bot Team 快照的受限来源。
	Generator BotTeamGeneratorDefinition `json:"generator"`
	// Budget 是 Planner 执行时允许使用的固定资源上限，未知或超额配置会被拒绝。
	Budget BotDecisionBudget `json:"budget"`
}

// BotPlannerDefinition 描述一个由代码实现、可确定性执行的 Bot 回合决策器。
type BotPlannerDefinition struct {
	// Kind 是当前主决策器种类；首版仅支持 first_available。
	Kind string `json:"kind"`
	// FallbackKind 是主决策器不能产生完整动作时使用的确定性保底种类；当前同样仅支持 first_available。
	FallbackKind string `json:"fallbackKind"`
}

// BotTeamGeneratorDefinition 描述一个不访问数据库或外部系统的 Bot Team 生成器。
type BotTeamGeneratorDefinition struct {
	// Kind 是生成器种类：mirror 复制真人已校验 Team，template 使用 Members 中的固定模板。
	Kind string `json:"kind"`
	// Members 是 template 生成器的完整固定 Team 成员；mirror 生成器必须保持为空。
	Members []team.Member `json:"members,omitempty"`
}

// BotDecisionBudget 限制 Bot 定义可声明的资料规模与确定性决策时间预算。
//
// 当前 first_available 算法是有界线性扫描，不会启动后台 goroutine；该预算仍被显式冻结，以便未来新增
// 搜索型 Planner 时能够在不改变既有契约的前提下实施相同上限。
type BotDecisionBudget struct {
	// MaxMembers 是生成 Team 允许拥有的最大成员数，范围为 1 至 6。
	MaxMembers uint8 `json:"maxMembers"`
	// MaxSkillsPerMember 是单个成员允许拥有的最大技能数，范围为 1 至 4。
	MaxSkillsPerMember uint8 `json:"maxSkillsPerMember"`
	// MaxDecisionMillis 是单回合决策的墙钟预算，范围为 1 至 1000 毫秒。
	MaxDecisionMillis uint16 `json:"maxDecisionMillis"`
}

// BotStrategyRecord 是从资料库读取的一条已启用、不可变版本 Bot 定义。
type BotStrategyRecord struct {
	// Code 是玩家 Training Battle 请求使用的稳定 Bot 标识。
	Code string
	// Version 是该 Code 的不可变递增版本。
	Version uint32
	// Definition 是尚未信任的原始 JSON 定义；调用方必须先经 DecodeBotStrategyDefinition 验证。
	Definition json.RawMessage
}

// EnabledBotStrategyReader 读取某个代码当前唯一启用的 Bot 定义。
//
// 存储实现必须保证同一 Code 同时最多存在一个 enabled 版本，避免 Training Battle 创建时出现不确定的版本选择。
type EnabledBotStrategyReader interface {
	// GetEnabledBotStrategy 返回指定 Code 当前可用于新 Training Battle 的定义。
	GetEnabledBotStrategy(context.Context, string) (BotStrategyRecord, error)
}

// PersistentTrainingBotCatalog 从 PostgreSQL 资料定义构建可冻结的 Training Bot Profile。
type PersistentTrainingBotCatalog struct {
	// reader 读取当前已启用的版本化定义。
	reader EnabledBotStrategyReader
	// identifiers 为模板 Bot 的独立冻结 Team Snapshot 生成稳定身份。
	identifiers snowflake.Source
}

// NewPersistentTrainingBotCatalog 创建由已启用资料定义驱动的 Training Bot 目录。
func NewPersistentTrainingBotCatalog(reader EnabledBotStrategyReader, identifiers snowflake.Source) *PersistentTrainingBotCatalog {
	return &PersistentTrainingBotCatalog{reader: reader, identifiers: identifiers}
}

// ResolveTrainingBot 读取、严格校验并规范化已启用定义，再生成独立 Team Snapshot 和可供启动期重建的定义。
func (catalog *PersistentTrainingBotCatalog) ResolveTrainingBot(
	ctx context.Context,
	code string,
	playerTeam team.Team,
	format battleformat.Format,
) (BotProfile, error) {
	if catalog == nil || catalog.reader == nil || catalog.identifiers == nil || strings.TrimSpace(code) == "" || playerTeam.ID == snowflake.ID(0) ||
		playerTeam.Version < 1 || format.RosterCount < 1 || format.RosterCount > 6 {
		return BotProfile{}, ErrBotStrategyUnavailable
	}
	record, err := catalog.reader.GetEnabledBotStrategy(ctx, strings.TrimSpace(code))
	if err != nil {
		return BotProfile{}, err
	}
	if record.Code != strings.TrimSpace(code) || record.Version == 0 {
		return BotProfile{}, ErrBotStrategyUnavailable
	}
	definition, canonical, err := DecodeBotStrategyDefinition(record.Definition)
	if err != nil {
		return BotProfile{}, err
	}
	botTeam, err := definition.GenerateTeam(ctx, catalog.identifiers, record.Code, record.Version, playerTeam, format)
	if err != nil {
		return BotProfile{}, err
	}
	return BotProfile{
		Code: record.Code, StrategyVersion: record.Version, DisplayName: definition.DisplayName,
		Team: botTeam, Definition: canonical,
	}, nil
}

// DecodeBotStrategyDefinition 严格解码资料库中的 Bot 定义，并返回规范化、可冻结的 JSON。
//
// 禁止未知字段可避免管理员拼写错误被静默忽略；重新编码后的 canonical JSON 让 Participant 快照具有
// 稳定结构，不依赖 PostgreSQL JSONB 的键顺序或原始格式。
func DecodeBotStrategyDefinition(raw json.RawMessage) (BotStrategyDefinition, json.RawMessage, error) {
	decoder := json.NewDecoder(strings.NewReader(string(raw)))
	decoder.DisallowUnknownFields()
	var definition BotStrategyDefinition
	if err := decoder.Decode(&definition); err != nil {
		return BotStrategyDefinition{}, nil, fmt.Errorf("%w: 解析 JSON: %v", ErrBotDefinitionInvalid, err)
	}
	if err := ensureJSONEOF(decoder); err != nil {
		return BotStrategyDefinition{}, nil, err
	}
	if err := definition.Validate(); err != nil {
		return BotStrategyDefinition{}, nil, err
	}
	canonical, err := json.Marshal(definition)
	if err != nil {
		return BotStrategyDefinition{}, nil, fmt.Errorf("%w: 编码规范定义: %v", ErrBotDefinitionInvalid, err)
	}
	return definition, canonical, nil
}

// Validate 检查当前代码明确支持的 Planner、Generator、预算和模板成员形状。
func (definition BotStrategyDefinition) Validate() error {
	if definition.SchemaVersion != botDefinitionSchemaVersion || !validBotDisplayName(definition.DisplayName) ||
		definition.Planner.Kind != botPlannerFirstAvailable || definition.Planner.FallbackKind != botPlannerFirstAvailable ||
		definition.Budget.MaxMembers < 1 || definition.Budget.MaxMembers > 6 ||
		definition.Budget.MaxSkillsPerMember < 1 || definition.Budget.MaxSkillsPerMember > 4 ||
		definition.Budget.MaxDecisionMillis < 1 || definition.Budget.MaxDecisionMillis > 1000 {
		return ErrBotDefinitionInvalid
	}
	switch definition.Generator.Kind {
	case botGeneratorMirror:
		if len(definition.Generator.Members) != 0 {
			return ErrBotDefinitionInvalid
		}
	case botGeneratorTemplate:
		if err := validateBotTemplateMembers(definition.Generator.Members, definition.Budget); err != nil {
			return err
		}
	default:
		return ErrBotDefinitionInvalid
	}
	return nil
}

// GenerateTeam 将已验证的定义转换为本次 Training Battle 专属的冻结 Team Snapshot。
func (definition BotStrategyDefinition) GenerateTeam(
	ctx context.Context,
	identifiers snowflake.Source,
	code string,
	version uint32,
	playerTeam team.Team,
	format battleformat.Format,
) (TeamSnapshot, error) {
	if err := definition.Validate(); err != nil || identifiers == nil || strings.TrimSpace(code) == "" || version == 0 ||
		format.RosterCount < 1 || format.RosterCount > 6 {
		return TeamSnapshot{}, ErrBotDefinitionInvalid
	}
	switch definition.Generator.Kind {
	case botGeneratorMirror:
		if len(playerTeam.Members) != int(format.RosterCount) || len(playerTeam.Members) > int(definition.Budget.MaxMembers) {
			return TeamSnapshot{}, ErrBotStrategyUnavailable
		}
		for _, member := range playerTeam.Members {
			if len(member.Skills) > int(definition.Budget.MaxSkillsPerMember) {
				return TeamSnapshot{}, ErrBotStrategyUnavailable
			}
		}
		return FreezeTeam(playerTeam), nil
	case botGeneratorTemplate:
		if len(definition.Generator.Members) != int(format.RosterCount) {
			return TeamSnapshot{}, ErrBotStrategyUnavailable
		}
		sourceTeamID, err := identifiers.Next(ctx)
		if err != nil {
			return TeamSnapshot{}, err
		}
		return TeamSnapshot{
			SourceTeamID: sourceTeamID, SourceTeamVersion: int64(version),
			Members: cloneBotTemplateMembers(definition.Generator.Members),
		}, nil
	default:
		return TeamSnapshot{}, ErrBotDefinitionInvalid
	}
}

// NewBotStrategyFromFrozenDefinition 根据 Participant 中已经冻结的定义创建运行时 Bot 策略。
//
// 对局启动绝不重新查询 battle_bot_strategy；即使管理员随后禁用了代码或启用了新版本，已有 Battle 仍由
// 其创建时冻结的 Definition 解释。
func NewBotStrategyFromFrozenDefinition(participant Participant) (BotStrategy, error) {
	if !participant.IsBot || strings.TrimSpace(participant.BotCode) == "" || participant.BotStrategyVersion == 0 {
		return nil, ErrBotStrategyUnavailable
	}
	definition, _, err := DecodeBotStrategyDefinition(participant.BotDefinition)
	if err != nil {
		return nil, err
	}
	if definition.Planner.Kind != botPlannerFirstAvailable || definition.Planner.FallbackKind != botPlannerFirstAvailable {
		return nil, ErrBotStrategyUnavailable
	}
	return NewFirstAvailableBot(participant.BotCode, participant.BotStrategyVersion)
}

func botStrategiesForFrozenBattle(session Battle) ([]BotStrategy, error) {
	strategies := make([]BotStrategy, 0, len(session.Participants))
	for _, participant := range session.Participants {
		if !participant.IsBot {
			continue
		}
		strategy, err := NewBotStrategyFromFrozenDefinition(participant)
		if err != nil {
			return nil, err
		}
		strategies = append(strategies, strategy)
	}
	return strategies, nil
}

func ensureJSONEOF(decoder *json.Decoder) error {
	var trailing any
	if err := decoder.Decode(&trailing); !errors.Is(err, io.EOF) {
		if err == nil {
			return fmt.Errorf("%w: JSON 只能包含一个对象", ErrBotDefinitionInvalid)
		}
		return fmt.Errorf("%w: 读取 JSON 结尾: %v", ErrBotDefinitionInvalid, err)
	}
	return nil
}

func validBotDisplayName(value string) bool {
	value = strings.TrimSpace(value)
	return value != "" && utf8.RuneCountInString(value) <= 64
}

func validateBotTemplateMembers(members []team.Member, budget BotDecisionBudget) error {
	if len(members) < 1 || len(members) > int(budget.MaxMembers) {
		return ErrBotDefinitionInvalid
	}
	positions := make(map[int32]struct{}, len(members))
	for index, member := range members {
		if member.Position != int32(index+1) || member.CreatureID == snowflake.ID(0) || member.AbilityID == snowflake.ID(0) ||
			member.TeraElementID == snowflake.ID(0) || member.Level < 1 || member.Level > 100 ||
			!validOptionalBotIdentifier(member.FormID) || !validOptionalBotIdentifier(member.GenderID) ||
			!validOptionalBotIdentifier(member.SkinID) || !validOptionalBotIdentifier(member.ItemID) ||
			len(member.Skills) < 1 || len(member.Skills) > int(budget.MaxSkillsPerMember) || len(member.Stats) > 32 {
			return ErrBotDefinitionInvalid
		}
		if _, duplicate := positions[member.Position]; duplicate {
			return ErrBotDefinitionInvalid
		}
		positions[member.Position] = struct{}{}
		if err := validateBotMemberSkills(member.Skills); err != nil {
			return err
		}
		if err := validateBotMemberStats(member.Stats); err != nil {
			return err
		}
	}
	return nil
}

func validOptionalBotIdentifier(value *snowflake.ID) bool {
	return value == nil || *value != snowflake.ID(0)
}

func validateBotMemberSkills(skills []team.MemberSkill) error {
	seen := make(map[snowflake.ID]struct{}, len(skills))
	for index, skill := range skills {
		if skill.Position != int32(index+1) || skill.SkillID == snowflake.ID(0) {
			return ErrBotDefinitionInvalid
		}
		if _, duplicate := seen[skill.SkillID]; duplicate {
			return ErrBotDefinitionInvalid
		}
		seen[skill.SkillID] = struct{}{}
	}
	return nil
}

func validateBotMemberStats(stats []team.MemberStat) error {
	seen := make(map[snowflake.ID]struct{}, len(stats))
	var totalEffort int32
	for _, stat := range stats {
		if stat.StatID == snowflake.ID(0) || stat.IndividualValue < 0 || stat.IndividualValue > 31 ||
			stat.EffortValue < 0 || stat.EffortValue > 252 {
			return ErrBotDefinitionInvalid
		}
		if _, duplicate := seen[stat.StatID]; duplicate {
			return ErrBotDefinitionInvalid
		}
		seen[stat.StatID] = struct{}{}
		totalEffort += stat.EffortValue
	}
	if totalEffort > 510 {
		return ErrBotDefinitionInvalid
	}
	return nil
}

func cloneBotTemplateMembers(source []team.Member) []team.Member {
	result := make([]team.Member, len(source))
	for index, member := range source {
		result[index] = member
		result[index].FormID = cloneBotIdentifier(member.FormID)
		result[index].GenderID = cloneBotIdentifier(member.GenderID)
		result[index].SkinID = cloneBotIdentifier(member.SkinID)
		result[index].ItemID = cloneBotIdentifier(member.ItemID)
		result[index].Skills = append([]team.MemberSkill(nil), member.Skills...)
		result[index].Stats = append([]team.MemberStat(nil), member.Stats...)
	}
	return result
}

func cloneBotIdentifier(source *snowflake.ID) *snowflake.ID {
	if source == nil {
		return nil
	}
	value := *source
	return &value
}
