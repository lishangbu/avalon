package api

import (
	"context"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/gamedata/elementeffectiveness"
	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/gamedata/itemcategory"
	"github.com/lishangbu/avalon/internal/gamedata/itemdictionary"
	"github.com/lishangbu/avalon/internal/gamedata/nature"
	"github.com/lishangbu/avalon/internal/gamedata/skill"
	"github.com/lishangbu/avalon/internal/gamedata/skillailment"
	"github.com/lishangbu/avalon/internal/gamedata/skillcategory"
	"github.com/lishangbu/avalon/internal/gamedata/skilldamageclass"
	"github.com/lishangbu/avalon/internal/gamedata/skilllearnmethod"
	"github.com/lishangbu/avalon/internal/gamedata/skillstatchange"
	"github.com/lishangbu/avalon/internal/gamedata/skilltarget"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
)

// BattleRuleService 定义原生传输层需要的四类独立对战资料能力。
type BattleRuleService interface {
	CreateClause(context.Context, battleformat.CreateClauseCommand) (battleformat.Clause, error)
	UpdateClause(context.Context, battleformat.UpdateClauseCommand) (battleformat.Clause, error)
	GetClause(context.Context, snowflake.ID) (battleformat.Clause, error)
	ListClauses(context.Context, battleformat.ClauseListQuery) (battleformat.ClausePage, error)
	CreateRestriction(context.Context, battleformat.CreateRestrictionCommand) (battleformat.Restriction, error)
	UpdateRestriction(context.Context, battleformat.UpdateRestrictionCommand) (battleformat.Restriction, error)
	GetRestriction(context.Context, snowflake.ID) (battleformat.Restriction, error)
	ListRestrictions(context.Context, battleformat.RestrictionListQuery) (battleformat.RestrictionPage, error)
	CreateMechanic(context.Context, battleformat.CreateMechanicCommand) (battleformat.Mechanic, error)
	UpdateMechanic(context.Context, battleformat.UpdateMechanicCommand) (battleformat.Mechanic, error)
	GetMechanic(context.Context, snowflake.ID) (battleformat.Mechanic, error)
	ListMechanics(context.Context, battleformat.MechanicListQuery) (battleformat.MechanicPage, error)
	CreateFormat(context.Context, battleformat.CreateFormatCommand) (battleformat.Format, error)
	UpdateFormat(context.Context, battleformat.UpdateFormatCommand) (battleformat.Format, error)
	GetFormat(context.Context, snowflake.ID) (battleformat.Format, error)
	ListFormats(context.Context, battleformat.FormatListQuery) (battleformat.FormatPage, error)
}

// BotStrategyService 管理只增版本、可停用但不可修改的 Training Bot 资料。
//
// Bot 策略不复用通用资料 CRUD：发布新版本、冻结给已创建 Battle，以及停用当前版本均具有独立的
// 领域含义和审计要求。
type BotStrategyService interface {
	// Create 创建新稳定 Code 的首个可启用版本。
	Create(context.Context, battle.CreateBotStrategyCommand) (battle.ManagedBotStrategy, error)
	// PublishNext 发布已有稳定 Code 的下一个不可变版本。
	PublishNext(context.Context, battle.PublishNextBotStrategyCommand) (battle.ManagedBotStrategy, error)
	// Disable 停用一个既有版本而保留历史冻结定义。
	Disable(context.Context, battle.DisableBotStrategyCommand) error
	// Get 返回指定稳定 Code 和版本的不可变资料。
	Get(context.Context, string, uint32) (battle.ManagedBotStrategy, error)
	// List 按页读取版本化 Bot 策略资料。
	List(context.Context, battle.BotStrategyListQuery) (battle.BotStrategyPage, error)
}

// CreatureMetadataService 读取运行时 Team 与 Battle 所需的完整关系投影。
type CreatureMetadataService interface {
	Get(context.Context) (creaturemetadata.Snapshot, error)
}

// CreatureAdministrationService 管理 Species 与 Creature 的独立记录。
type CreatureAdministrationService interface {
	GetReferenceOptions(context.Context) (creaturemetadata.ReferenceOptions, error)
	ListSpecies(context.Context, creaturemetadata.SpeciesListQuery) (creaturemetadata.SpeciesPage, error)
	GetSpecies(context.Context, snowflake.ID) (creaturemetadata.ManagedSpecies, error)
	CreateSpecies(context.Context, creaturemetadata.CreateSpeciesCommand) (creaturemetadata.ManagedSpecies, error)
	UpdateSpecies(context.Context, creaturemetadata.UpdateSpeciesCommand) (creaturemetadata.ManagedSpecies, error)
	ListCreatures(context.Context, creaturemetadata.CreatureListQuery) (creaturemetadata.CreaturePage, error)
	GetCreature(context.Context, snowflake.ID) (creaturemetadata.ManagedCreature, error)
	CreateCreature(context.Context, creaturemetadata.CreateCreatureCommand) (creaturemetadata.ManagedCreature, error)
	UpdateCreature(context.Context, creaturemetadata.UpdateCreatureCommand) (creaturemetadata.ManagedCreature, error)
	GetCreatureRelations(context.Context, snowflake.ID) (creaturemetadata.CreatureRelations, error)
	ReplaceRelations(context.Context, creaturemetadata.ReplaceRelationsCommand) (creaturemetadata.CreatureRelations, error)
}

// ElementService 管理属性资料。
type ElementService interface {
	Create(context.Context, element.CreateCommand) (element.Element, error)
	Get(context.Context, snowflake.ID) (element.Element, error)
	List(context.Context, element.ListQuery) (element.Page, error)
	Update(context.Context, element.UpdateCommand) (element.Element, error)
}

// ElementEffectivenessService 管理属性克制资料，并为 Battle 提供启用倍率快照。
type ElementEffectivenessService interface {
	Create(context.Context, elementeffectiveness.CreateCommand) (elementeffectiveness.Effectiveness, error)
	Get(context.Context, snowflake.ID) (elementeffectiveness.Effectiveness, error)
	List(context.Context, elementeffectiveness.ListQuery) (elementeffectiveness.Page, error)
	Update(context.Context, elementeffectiveness.UpdateCommand) (elementeffectiveness.Effectiveness, error)
	ListEnabled(context.Context) ([]elementeffectiveness.Effectiveness, error)
}

// NatureService 管理 Team 成员可选择的 Nature 资料。
type NatureService interface {
	Create(context.Context, nature.CreateCommand) (nature.Nature, error)
	Get(context.Context, snowflake.ID) (nature.Nature, error)
	List(context.Context, nature.ListQuery) (nature.Page, error)
	Update(context.Context, nature.UpdateCommand) (nature.Nature, error)
}

// AbilityService 管理特性主体资料。
type AbilityService interface {
	Create(context.Context, ability.CreateCommand) (ability.Ability, error)
	Get(context.Context, snowflake.ID) (ability.Ability, error)
	List(context.Context, ability.ListQuery) (ability.Page, error)
	Update(context.Context, ability.UpdateCommand) (ability.Ability, error)
}

// ItemCategoryService 管理道具分类资料。
type ItemCategoryService interface {
	Create(context.Context, itemcategory.CreateCommand) (itemcategory.Category, error)
	Get(context.Context, snowflake.ID) (itemcategory.Category, error)
	List(context.Context, itemcategory.ListQuery) (itemcategory.Page, error)
	Update(context.Context, itemcategory.UpdateCommand) (itemcategory.Category, error)
}

// ItemDictionaryService 管理 Pocket、Attribute 与 Fling Effect 三类独立规范化字典。
type ItemDictionaryService interface {
	List(context.Context, itemdictionary.Kind) ([]itemdictionary.Entry, error)
	Create(context.Context, itemdictionary.CreateCommand) (itemdictionary.Entry, error)
	Update(context.Context, itemdictionary.UpdateCommand) (itemdictionary.Entry, error)
}

// ItemService 管理道具主体资料。
type ItemService interface {
	Create(context.Context, item.CreateCommand) (item.Item, error)
	Get(context.Context, snowflake.ID) (item.Item, error)
	List(context.Context, item.ListQuery) (item.Page, error)
	Update(context.Context, item.UpdateCommand) (item.Item, error)
}

// StatService 管理数值项资料。
type StatService interface {
	Create(context.Context, stat.CreateCommand) (stat.Stat, error)
	Get(context.Context, snowflake.ID) (stat.Stat, error)
	List(context.Context, stat.ListQuery) (stat.Page, error)
	Update(context.Context, stat.UpdateCommand) (stat.Stat, error)
}

// SkillDamageClassService 管理技能伤害分类。
type SkillDamageClassService interface {
	Create(context.Context, skilldamageclass.CreateCommand) (skilldamageclass.DamageClass, error)
	Get(context.Context, snowflake.ID) (skilldamageclass.DamageClass, error)
	List(context.Context, skilldamageclass.ListQuery) (skilldamageclass.Page, error)
	Update(context.Context, skilldamageclass.UpdateCommand) (skilldamageclass.DamageClass, error)
}

// SkillService 管理技能主体资料。
type SkillService interface {
	Create(context.Context, skill.CreateCommand) (skill.Skill, error)
	Get(context.Context, snowflake.ID) (skill.Skill, error)
	List(context.Context, skill.ListQuery) (skill.Page, error)
	Update(context.Context, skill.UpdateCommand) (skill.Skill, error)
}

// SkillAilmentService 管理技能异常资料。
type SkillAilmentService interface {
	Create(context.Context, skillailment.CreateCommand) (skillailment.Ailment, error)
	Get(context.Context, snowflake.ID) (skillailment.Ailment, error)
	List(context.Context, skillailment.ListQuery) (skillailment.Page, error)
	Update(context.Context, skillailment.UpdateCommand) (skillailment.Ailment, error)
}

// SkillCategoryService 管理技能元分类。
type SkillCategoryService interface {
	Create(context.Context, skillcategory.CreateCommand) (skillcategory.Category, error)
	Get(context.Context, snowflake.ID) (skillcategory.Category, error)
	List(context.Context, skillcategory.ListQuery) (skillcategory.Page, error)
	Update(context.Context, skillcategory.UpdateCommand) (skillcategory.Category, error)
}

// SkillTargetService 管理技能目标资料。
type SkillTargetService interface {
	Create(context.Context, skilltarget.CreateCommand) (skilltarget.Target, error)
	Get(context.Context, snowflake.ID) (skilltarget.Target, error)
	List(context.Context, skilltarget.ListQuery) (skilltarget.Page, error)
	Update(context.Context, skilltarget.UpdateCommand) (skilltarget.Target, error)
}

// SkillLearnMethodService 管理技能学习方式。
type SkillLearnMethodService interface {
	Create(context.Context, skilllearnmethod.CreateCommand) (skilllearnmethod.Method, error)
	Get(context.Context, snowflake.ID) (skilllearnmethod.Method, error)
	List(context.Context, skilllearnmethod.ListQuery) (skilllearnmethod.Page, error)
	Update(context.Context, skilllearnmethod.UpdateCommand) (skilllearnmethod.Method, error)
}

// SkillStatChangeService 管理可显式禁用的技能数值变化。
type SkillStatChangeService interface {
	Create(context.Context, skillstatchange.CreateCommand) (skillstatchange.Change, error)
	Get(context.Context, snowflake.ID) (skillstatchange.Change, error)
	List(context.Context, skillstatchange.ListQuery) (skillstatchange.Page, error)
	Update(context.Context, skillstatchange.UpdateCommand) (skillstatchange.Change, error)
	Disable(context.Context, skillstatchange.DisableCommand) error
}
