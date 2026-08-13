package battle

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"sort"
	"strings"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
	"github.com/lishangbu/avalon/internal/team"
)

var (
	// ErrBattleFormatRuleCompilation 表示启用赛制引用的组件不能编译为当前二进制支持的引擎规则。
	ErrBattleFormatRuleCompilation = errors.New("战斗赛制规则编译失败")
	// ErrBattleFormatTeamRuleViolation 表示 Team 在进入 Challenge 或 Training Battle 时违反冻结赛制规则。
	ErrBattleFormatTeamRuleViolation = errors.New("team 不符合战斗赛制规则")
)

// BattleFormatComponentQuery 读取 BattleFormat 所引用的三类实时规则组件。
type BattleFormatComponentQuery interface {
	// GetClause 返回指定 Battle Clause 的当前实时资料。
	GetClause(context.Context, snowflake.ID) (battleformat.Clause, error)
	// GetRestriction 返回指定 Battle Restriction 的当前实时资料。
	GetRestriction(context.Context, snowflake.ID) (battleformat.Restriction, error)
	// GetMechanic 返回指定 Special Mechanic 的当前实时资料。
	GetMechanic(context.Context, snowflake.ID) (battleformat.Mechanic, error)
}

// BattleFormatTeamCatalog 读取 Team 入场名单限制所需的同修订实时资料目录。
type BattleFormatTeamCatalog interface {
	// Current 返回同一资料修订下的 Team 引用目录。
	Current(context.Context) (team.ReferenceCatalog, error)
}

// BattleFormatRuleCompiler 将已注册的游戏资料效果编译为 Battle Engine 规则，并校验 Team 入场约束。
//
// 管理端仍分别维护 Clause、Restriction、Mechanic 的 CRUD。本编译器仅在玩家 Battle 边界读取它们，
// 把可执行的有限规则投影为强类型 RuleSnapshot；它不是动态规则引擎，也不会解释未在二进制注册的效果。
type BattleFormatRuleCompiler struct {
	// components 读取赛制引用的实时规则组件。
	components BattleFormatComponentQuery
	// catalog 将 Team 引用 Identifier 映射为限制规则使用的 Stable Code。
	catalog BattleFormatTeamCatalog
	// effects 是随二进制明确构造的效果注册表。
	effects *effect.Registry
}

// NewBattleFormatRuleCompiler 使用显式规则组件读取器、Team 目录和效果注册表创建编译器。
func NewBattleFormatRuleCompiler(
	components BattleFormatComponentQuery,
	catalog BattleFormatTeamCatalog,
	effects *effect.Registry,
) *BattleFormatRuleCompiler {
	return &BattleFormatRuleCompiler{components: components, catalog: catalog, effects: effects}
}

// CompileRules 把 BattleFormat 的启用组件编译为可持久化、可重放的 Battle Engine 规则快照。
func (compiler *BattleFormatRuleCompiler) CompileRules(
	ctx context.Context,
	format battleformat.Format,
) (battleengine.RuleSnapshot, error) {
	if compiler == nil || compiler.components == nil || compiler.effects == nil || format.ID == snowflake.ID(0) {
		return battleengine.RuleSnapshot{}, ErrBattleFormatRuleCompilation
	}
	rules := battleengine.RuleSnapshot{SchemaVersion: 1}
	for _, componentID := range format.ClauseIDs {
		clause, err := compiler.components.GetClause(ctx, componentID)
		if err != nil {
			return battleengine.RuleSnapshot{}, fmt.Errorf("读取 Battle Clause %s: %w", componentID, err)
		}
		if clause.ID != componentID || !clause.Enabled {
			return battleengine.RuleSnapshot{}, ErrBattleFormatRuleCompilation
		}
		definition, err := compiler.compile(clause.Definition)
		if err != nil {
			return battleengine.RuleSnapshot{}, err
		}
		switch definition.Kind {
		case effect.KindUniqueHeldItemClause:
			rules.UniqueHeldItemClause = true
		case effect.KindUniqueSpeciesClause:
			rules.UniqueSpeciesClause = true
		default:
			return battleengine.RuleSnapshot{}, ErrBattleFormatRuleCompilation
		}
	}
	for _, componentID := range format.RestrictionIDs {
		restriction, err := compiler.components.GetRestriction(ctx, componentID)
		if err != nil {
			return battleengine.RuleSnapshot{}, fmt.Errorf("读取 Battle Restriction %s: %w", componentID, err)
		}
		if restriction.ID != componentID || !restriction.Enabled {
			return battleengine.RuleSnapshot{}, ErrBattleFormatRuleCompilation
		}
		definition, err := compiler.compile(restriction.Definition)
		if err != nil {
			return battleengine.RuleSnapshot{}, err
		}
		if definition.Kind != effect.KindStableCodeListRestriction {
			return battleengine.RuleSnapshot{}, ErrBattleFormatRuleCompilation
		}
		var parameters effect.StableCodeListParameters
		if err := json.Unmarshal(definition.Parameters, &parameters); err != nil {
			return battleengine.RuleSnapshot{}, ErrBattleFormatRuleCompilation
		}
		rules.StableCodeRestrictions = append(rules.StableCodeRestrictions, battleengine.StableCodeRestriction{
			Mode: parameters.Mode, ResourceType: parameters.ResourceType, StableCodes: append([]string(nil), parameters.StableCodes...),
		})
	}
	for _, componentID := range format.MechanicIDs {
		mechanic, err := compiler.components.GetMechanic(ctx, componentID)
		if err != nil {
			return battleengine.RuleSnapshot{}, fmt.Errorf("读取 Special Mechanic %s: %w", componentID, err)
		}
		if mechanic.ID != componentID || !mechanic.Enabled {
			return battleengine.RuleSnapshot{}, ErrBattleFormatRuleCompilation
		}
		definition, err := compiler.compile(mechanic.Definition)
		if err != nil {
			return battleengine.RuleSnapshot{}, err
		}
		switch definition.Kind {
		case effect.KindLevelNormalizationMechanic:
			var parameters effect.LevelNormalizationParameters
			if err := json.Unmarshal(definition.Parameters, &parameters); err != nil || parameters.Level < 1 || parameters.Level > 100 {
				return battleengine.RuleSnapshot{}, ErrBattleFormatRuleCompilation
			}
			if rules.NormalizedLevel != 0 && rules.NormalizedLevel != uint8(parameters.Level) {
				return battleengine.RuleSnapshot{}, ErrBattleFormatRuleCompilation
			}
			rules.NormalizedLevel = uint8(parameters.Level)
		case effect.KindTerastallizationMechanic:
			if rules.TerastallizationEnabled {
				return battleengine.RuleSnapshot{}, ErrBattleFormatRuleCompilation
			}
			rules.TerastallizationEnabled = true
		default:
			return battleengine.RuleSnapshot{}, ErrBattleFormatRuleCompilation
		}
	}
	if err := validateLevelNormalization(format, rules.NormalizedLevel); err != nil {
		return battleengine.RuleSnapshot{}, err
	}
	sort.Slice(rules.StableCodeRestrictions, func(left, right int) bool {
		leftRule, rightRule := rules.StableCodeRestrictions[left], rules.StableCodeRestrictions[right]
		if leftRule.ResourceType != rightRule.ResourceType {
			return leftRule.ResourceType < rightRule.ResourceType
		}
		if leftRule.Mode != rightRule.Mode {
			return leftRule.Mode < rightRule.Mode
		}
		return strings.Join(leftRule.StableCodes, "\x00") < strings.Join(rightRule.StableCodes, "\x00")
	})
	return rules, nil
}

// ValidateTeam 在 Challenge 或 Training Battle 创建前验证一份已重新读取的 Team 是否符合赛制条款和名单限制。
func (compiler *BattleFormatRuleCompiler) ValidateTeam(
	ctx context.Context,
	format battleformat.Format,
	value team.Team,
) error {
	rules, err := compiler.CompileRules(ctx, format)
	if err != nil {
		return err
	}
	if rules.UniqueHeldItemClause && hasDuplicateHeldItem(value) {
		return ErrBattleFormatTeamRuleViolation
	}
	if !rules.UniqueSpeciesClause && len(rules.StableCodeRestrictions) == 0 {
		return nil
	}
	if compiler.catalog == nil {
		return ErrBattleFormatRuleCompilation
	}
	catalog, err := compiler.catalog.Current(ctx)
	if err != nil {
		return fmt.Errorf("读取 Team 名单限制资料目录: %w", err)
	}
	if rules.UniqueSpeciesClause {
		duplicated, duplicateErr := hasDuplicateSpecies(value, catalog)
		if duplicateErr != nil {
			return duplicateErr
		}
		if duplicated {
			return ErrBattleFormatTeamRuleViolation
		}
	}
	for _, restriction := range rules.StableCodeRestrictions {
		codes, codeErr := teamReferenceCodes(value, catalog, restriction.ResourceType)
		if codeErr != nil {
			return codeErr
		}
		allowed := make(map[string]struct{}, len(restriction.StableCodes))
		for _, code := range restriction.StableCodes {
			allowed[code] = struct{}{}
		}
		for _, code := range codes {
			_, listed := allowed[code]
			if restriction.Mode == "allow" && !listed || restriction.Mode == "deny" && listed {
				return ErrBattleFormatTeamRuleViolation
			}
		}
	}
	return nil
}

func (compiler *BattleFormatRuleCompiler) compile(definition effect.Definition) (effect.CompiledDefinition, error) {
	compiled, issues := compiler.effects.Compile(definition)
	if len(issues) > 0 {
		return effect.CompiledDefinition{}, ErrBattleFormatRuleCompilation
	}
	return compiled, nil
}

func validateLevelNormalization(format battleformat.Format, normalizedLevel uint8) error {
	if normalizedLevel == 0 {
		return nil
	}
	if format.LevelRule.Mode != battleformat.LevelRuleNormalize || format.LevelRule.Level == nil ||
		*format.LevelRule.Level != int32(normalizedLevel) {
		return ErrBattleFormatRuleCompilation
	}
	return nil
}

func hasDuplicateHeldItem(value team.Team) bool {
	seen := make(map[snowflake.ID]struct{}, len(value.Members))
	for _, member := range value.Members {
		if member.ItemID == nil {
			continue
		}
		if _, duplicate := seen[*member.ItemID]; duplicate {
			return true
		}
		seen[*member.ItemID] = struct{}{}
	}
	return false
}

func hasDuplicateSpecies(value team.Team, catalog team.ReferenceCatalog) (bool, error) {
	creatureSpecies := make(map[snowflake.ID]snowflake.ID, len(catalog.CreatureMetadata.Creatures))
	for _, creature := range catalog.CreatureMetadata.Creatures {
		if creature.Enabled && creature.SpeciesID != snowflake.ID(0) {
			creatureSpecies[creature.ID] = creature.SpeciesID
		}
	}
	seen := make(map[snowflake.ID]struct{}, len(value.Members))
	for _, member := range value.Members {
		speciesID, found := creatureSpecies[member.CreatureID]
		if !found {
			return false, ErrBattleFormatRuleCompilation
		}
		if _, duplicate := seen[speciesID]; duplicate {
			return true, nil
		}
		seen[speciesID] = struct{}{}
	}
	return false, nil
}

func teamReferenceCodes(
	value team.Team,
	catalog team.ReferenceCatalog,
	resourceType string,
) ([]string, error) {
	if resourceType == "creature" {
		codes := make([]string, 0, len(value.Members))
		byID := make(map[snowflake.ID]string, len(catalog.CreatureMetadata.Creatures))
		for _, creature := range catalog.CreatureMetadata.Creatures {
			if creature.Enabled && strings.TrimSpace(creature.Code) != "" {
				byID[creature.ID] = creature.Code
			}
		}
		for _, member := range value.Members {
			code, found := byID[member.CreatureID]
			if !found {
				return nil, ErrBattleFormatRuleCompilation
			}
			codes = append(codes, code)
		}
		return codes, nil
	}
	var references []team.Reference
	switch resourceType {
	case "ability":
		references = catalog.Abilities
	case "item":
		references = catalog.Items
	case "skill":
		references = catalog.Skills
	default:
		return nil, ErrBattleFormatRuleCompilation
	}
	byID := make(map[snowflake.ID]string, len(references))
	for _, reference := range references {
		if reference.Enabled && strings.TrimSpace(reference.Code) != "" {
			byID[reference.ID] = reference.Code
		}
	}
	codes := make([]string, 0, len(value.Members)*battleengine.MaximumSkillsPerMember)
	for _, member := range value.Members {
		switch resourceType {
		case "ability":
			code, found := byID[member.AbilityID]
			if !found {
				return nil, ErrBattleFormatRuleCompilation
			}
			codes = append(codes, code)
		case "item":
			if member.ItemID == nil {
				continue
			}
			code, found := byID[*member.ItemID]
			if !found {
				return nil, ErrBattleFormatRuleCompilation
			}
			codes = append(codes, code)
		case "skill":
			for _, skill := range member.Skills {
				code, found := byID[skill.SkillID]
				if !found {
					return nil, ErrBattleFormatRuleCompilation
				}
				codes = append(codes, code)
			}
		}
	}
	return codes, nil
}
