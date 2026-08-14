package api

import (
	"fmt"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"log/slog"
	"time"

	battle "github.com/lishangbu/avalon/internal/battle"
	battlepersistence "github.com/lishangbu/avalon/internal/battle/persistence"
	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/gamedata/elementeffectiveness"
	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/gamedata/itemcategory"
	"github.com/lishangbu/avalon/internal/gamedata/itemdictionary"
	"github.com/lishangbu/avalon/internal/gamedata/nature"
	gamedatapersistence "github.com/lishangbu/avalon/internal/gamedata/persistence"
	"github.com/lishangbu/avalon/internal/gamedata/referencedictionary"
	"github.com/lishangbu/avalon/internal/gamedata/skill"
	"github.com/lishangbu/avalon/internal/gamedata/skillailment"
	"github.com/lishangbu/avalon/internal/gamedata/skillcategory"
	"github.com/lishangbu/avalon/internal/gamedata/skilldamageclass"
	"github.com/lishangbu/avalon/internal/gamedata/skilllearnmethod"
	"github.com/lishangbu/avalon/internal/gamedata/skillstatchange"
	"github.com/lishangbu/avalon/internal/gamedata/skilltarget"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
	"github.com/lishangbu/avalon/internal/platform/database"
)

// NewAdministrationServices 使用显式依赖构造完整游戏资料管理切片。
//
// 该函数只负责无副作用的对象装配，数据库事务仍由各应用服务和 Repository 明确划定。
func NewAdministrationServices(
	pool *database.Pool,
	assets AssetService,
	identifiers snowflake.Source,
	logger *slog.Logger,
) (*KratosService, error) {
	adapters := gamedatapersistence.NewAdapters(pool, identifiers)
	effectRegistry, err := effect.NewDefaultRegistry()
	if err != nil {
		return nil, fmt.Errorf("创建效果注册表: %w", err)
	}
	elements := element.NewService(adapters, adapters, adapters, identifiers, time.Now)
	elementEffectiveness := elementeffectiveness.NewService(gamedatapersistence.NewElementEffectivenessRepository(adapters), identifiers, time.Now)
	abilities := ability.NewService(adapters, adapters, adapters, identifiers, time.Now)
	itemCategories := itemcategory.NewService(adapters, adapters, adapters, identifiers, time.Now)
	itemDictionaries := itemdictionary.NewService(adapters, adapters, identifiers, time.Now)
	referenceDictionaries := referencedictionary.NewService(adapters, adapters, identifiers, time.Now)
	items := item.NewService(adapters, adapters, adapters, identifiers, time.Now)
	stats := stat.NewService(adapters, adapters, adapters, identifiers, time.Now)
	natures := nature.NewService(gamedatapersistence.NewNatureRepository(adapters), stats, identifiers, time.Now)
	damageClasses := skilldamageclass.NewService(adapters, identifiers, time.Now)
	skills := skill.NewService(adapters, identifiers, time.Now)
	skillAilments := skillailment.NewService(adapters, identifiers, time.Now)
	skillCategories := skillcategory.NewService(adapters, identifiers, time.Now)
	skillTargets := skilltarget.NewService(adapters, identifiers, time.Now)
	skillLearnMethods := skilllearnmethod.NewService(adapters, identifiers, time.Now)
	skillStatChanges := skillstatchange.NewService(adapters, identifiers, time.Now)
	creatureMetadataService := creaturemetadata.NewService(adapters)
	creatureAdministrationService := creaturemetadata.NewAdministrationService(adapters, adapters, adapters, identifiers, time.Now)
	battleRules := battleformat.NewService(adapters, effectRegistry, identifiers, time.Now)
	botStrategyRepository := battlepersistence.NewAdapters(pool, identifiers, nil)
	botStrategies := battle.NewBotStrategyAdministrationService(
		botStrategyRepository, botStrategyRepository, botStrategyRepository, time.Now,
	)
	native := NewKratosService(NativeServices{
		Assets: assets, BattleRules: battleRules, BotStrategies: botStrategies, Elements: elements,
		ElementEffectiveness: elementEffectiveness, Natures: natures, Abilities: abilities,
		CreatureMetadata:       creatureMetadataService,
		CreatureAdministration: creatureAdministrationService,
		ItemCategories:         itemCategories, ItemDictionaries: itemDictionaries,
		ReferenceDictionaries: referenceDictionaries,
		Items:                 items, Stats: stats, DamageClasses: damageClasses, Skills: skills,
		SkillAilments: skillAilments, SkillCategories: skillCategories, SkillTargets: skillTargets,
		SkillLearnMethods: skillLearnMethods, SkillStatChanges: skillStatChanges,
	}, logger)
	return native, nil
}
