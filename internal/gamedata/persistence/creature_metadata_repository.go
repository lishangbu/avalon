package persistence

import (
	"context"
	"fmt"
	"github.com/lishangbu/avalon/internal/platform/snowflake"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamecreature"
	"github.com/lishangbu/avalon/ent/gamecreatureability"
	"github.com/lishangbu/avalon/ent/gamecreatureevolution"
	"github.com/lishangbu/avalon/ent/gamecreatureform"
	"github.com/lishangbu/avalon/ent/gamecreaturehelditem"
	"github.com/lishangbu/avalon/ent/gamecreatureskilllearn"
	"github.com/lishangbu/avalon/ent/gamecreatureskin"
	"github.com/lishangbu/avalon/ent/gamecreaturestat"
	"github.com/lishangbu/avalon/ent/gameegggroup"
	"github.com/lishangbu/avalon/ent/gamegender"
	"github.com/lishangbu/avalon/ent/gamegrowthrate"
	"github.com/lishangbu/avalon/ent/gamehabitat"
	"github.com/lishangbu/avalon/ent/gamespecies"
	"github.com/lishangbu/avalon/ent/gamespeciescolor"
	"github.com/lishangbu/avalon/ent/gamespeciesshape"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
)

// GetCreatureMetadata 从拆分后的关系表组装运行时需要的 Creature 资料投影。
// 该投影沿用既有 Data 类型以服务 Team 校验与对战冻结，不引入额外 Catalog 概念。
func (s *Adapters) GetCreatureMetadata(ctx context.Context) (creaturemetadata.Snapshot, error) {
	data, err := readCreatureMetadata(ctx, s.pool.Client(ctx))
	if err != nil {
		return creaturemetadata.Snapshot{}, err
	}
	return creaturemetadata.Snapshot{Data: data}, nil
}

// readCreatureMetadata 逐表读取资料并只在存储适配器内部恢复关系切片。
func readCreatureMetadata(ctx context.Context, client *avalonent.Client) (creaturemetadata.Data, error) {
	var data creaturemetadata.Data

	eggGroups, err := client.GameEggGroup.Query().Order(gameegggroup.BySortOrder(), gameegggroup.ByCode()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询蛋组资料: %w", err)
	}
	data.EggGroups = make([]creaturemetadata.EggGroup, len(eggGroups))
	for index, row := range eggGroups {
		data.EggGroups[index] = creaturemetadata.EggGroup{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled}
	}

	genders, err := client.GameGender.Query().Order(gamegender.BySortOrder(), gamegender.ByCode()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询性别资料: %w", err)
	}
	data.Genders = make([]creaturemetadata.Gender, len(genders))
	for index, row := range genders {
		data.Genders[index] = creaturemetadata.Gender{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled}
	}

	growthRates, err := client.GameGrowthRate.Query().Order(gamegrowthrate.ByCode()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询成长速率资料: %w", err)
	}
	data.GrowthRates = make([]creaturemetadata.GrowthRate, len(growthRates))
	for index, row := range growthRates {
		data.GrowthRates[index] = creaturemetadata.GrowthRate{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, Formula: row.Formula, Description: row.Description, Enabled: row.Enabled}
	}

	habitats, err := client.GameHabitat.Query().Order(gamehabitat.BySortOrder(), gamehabitat.ByCode()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询栖息地资料: %w", err)
	}
	data.Habitats = make([]creaturemetadata.Habitat, len(habitats))
	for index, row := range habitats {
		data.Habitats[index] = creaturemetadata.Habitat{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled}
	}

	colors, err := client.GameSpeciesColor.Query().Order(gamespeciescolor.BySortOrder(), gamespeciescolor.ByCode()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Species 颜色资料: %w", err)
	}
	data.Colors = make([]creaturemetadata.SpeciesColor, len(colors))
	for index, row := range colors {
		data.Colors[index] = creaturemetadata.SpeciesColor{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled}
	}

	shapes, err := client.GameSpeciesShape.Query().Order(gamespeciesshape.BySortOrder(), gamespeciesshape.ByCode()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Species 外形资料: %w", err)
	}
	data.Shapes = make([]creaturemetadata.SpeciesShape, len(shapes))
	for index, row := range shapes {
		data.Shapes[index] = creaturemetadata.SpeciesShape{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled}
	}

	speciesRows, err := client.GameSpecies.Query().Order(gamespecies.ByNationalDexNumber(), gamespecies.ByID()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Species 资料: %w", err)
	}
	speciesEggGroups, err := client.GameSpeciesEggGroup.Query().All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Species 蛋组关系: %w", err)
	}
	eggGroupsBySpecies := make(map[snowflake.ID][]snowflake.ID)
	for _, row := range speciesEggGroups {
		eggGroupsBySpecies[snowflake.ID(row.SpeciesID)] = append(eggGroupsBySpecies[snowflake.ID(row.SpeciesID)], snowflake.ID(row.EggGroupID))
	}
	data.Species = make([]creaturemetadata.Species, len(speciesRows))
	for index, row := range speciesRows {
		eggGroupIDs := append([]snowflake.ID(nil), eggGroupsBySpecies[snowflake.ID(row.ID)]...)
		data.Species[index] = creaturemetadata.Species{
			ID: snowflake.ID(row.ID), NationalDexNumber: row.NationalDexNumber, Code: row.Code, Name: row.Name,
			GrowthRateID: optionalIdentifier(row.GrowthRateID), HabitatID: optionalIdentifier(row.HabitatID),
			ColorID: optionalIdentifier(row.ColorID), ShapeID: optionalIdentifier(row.ShapeID),
			EggGroupIDs: eggGroupIDs, Genus: row.Genus, PokedexEntry: row.PokedexEntry,
			Description: row.Description, Profile: row.Profile,
			DesignOrigin: row.DesignOrigin, Trivia: row.Trivia,
			GenderDifferences: row.GenderDifferences, FormsSwitchable: row.FormsSwitchable, Enabled: row.Enabled,
		}
	}

	creatures, err := client.GameCreature.Query().Order(gamecreature.ByCode(), gamecreature.ByID()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Creature 资料: %w", err)
	}
	data.Creatures = make([]creaturemetadata.Creature, len(creatures))
	for index, row := range creatures {
		data.Creatures[index] = creaturemetadata.Creature{
			ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, SpeciesID: snowflake.ID(row.SpeciesID),
			InheritsFromCreatureID: optionalIdentifier(row.InheritsFromCreatureID), Height: row.Height,
			Weight: row.Weight, BaseExperience: row.BaseExperience,
			CaptureRate: row.CaptureRate, HatchCycles: row.HatchCycles,
			GenderRatio: &creaturemetadata.GenderRatio{MaleEighths: int32(row.MaleEighths), FemaleEighths: int32(row.FemaleEighths)},
			DefaultForm: row.DefaultForm, Enabled: row.Enabled,
		}
	}

	forms, err := client.GameCreatureForm.Query().Order(gamecreatureform.ByCode(), gamecreatureform.ByID()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Creature 形态: %w", err)
	}
	formElements, err := client.GameCreatureFormElement.Query().All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Creature 形态属性关系: %w", err)
	}
	elementsByForm := make(map[snowflake.ID][]snowflake.ID)
	for _, row := range formElements {
		elementsByForm[snowflake.ID(row.FormID)] = append(elementsByForm[snowflake.ID(row.FormID)], snowflake.ID(row.ElementID))
	}
	data.Forms = make([]creaturemetadata.Form, len(forms))
	for index, row := range forms {
		elementIDs := append([]snowflake.ID(nil), elementsByForm[snowflake.ID(row.ID)]...)
		data.Forms[index] = creaturemetadata.Form{
			ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, CreatureID: snowflake.ID(row.CreatureID),
			FormName: row.FormName, SortOrder: row.SortOrder, FormOrder: row.FormOrder,
			BattleOnly: row.BattleOnly, DefaultForm: row.DefaultForm, EnhancedForm: row.EnhancedForm, Enabled: row.Enabled, Version: row.Version, ElementIDs: elementIDs,
		}
	}

	stats, err := client.GameCreatureStat.Query().Order(gamecreaturestat.ByID()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Creature 能力关系: %w", err)
	}
	data.Stats = make([]creaturemetadata.StatBinding, len(stats))
	for index, row := range stats {
		effort := int32(row.Effort)
		data.Stats[index] = creaturemetadata.StatBinding{ID: snowflake.ID(row.ID), CreatureID: snowflake.ID(row.CreatureID), StatID: snowflake.ID(row.StatID), BaseValue: row.BaseValue, Effort: &effort, Enabled: row.Enabled, Version: row.Version}
	}

	learns, err := client.GameCreatureSkillLearn.Query().Order(gamecreatureskilllearn.ByID()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Creature 技能学习关系: %w", err)
	}
	data.SkillLearns = make([]creaturemetadata.SkillLearn, len(learns))
	for index, row := range learns {
		data.SkillLearns[index] = creaturemetadata.SkillLearn{ID: snowflake.ID(row.ID), CreatureID: snowflake.ID(row.CreatureID), SkillID: snowflake.ID(row.SkillID), LearnMethodID: snowflake.ID(row.LearnMethodID), LevelLearnedAt: row.LevelLearnedAt, Enabled: row.Enabled, Version: row.Version}
	}

	abilities, err := client.GameCreatureAbility.Query().Order(gamecreatureability.ByID()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Creature 特性关系: %w", err)
	}
	data.Abilities = make([]creaturemetadata.AbilityBinding, len(abilities))
	for index, row := range abilities {
		data.Abilities[index] = creaturemetadata.AbilityBinding{ID: snowflake.ID(row.ID), CreatureID: snowflake.ID(row.CreatureID), AbilityID: snowflake.ID(row.AbilityID), Hidden: row.Hidden, Slot: row.Slot, Enabled: row.Enabled, Version: row.Version}
	}

	heldItems, err := client.GameCreatureHeldItem.Query().Order(gamecreaturehelditem.ByID()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Creature 携带物关系: %w", err)
	}
	data.HeldItems = make([]creaturemetadata.HeldItem, len(heldItems))
	for index, row := range heldItems {
		data.HeldItems[index] = creaturemetadata.HeldItem{ID: snowflake.ID(row.ID), CreatureID: snowflake.ID(row.CreatureID), ItemID: snowflake.ID(row.ItemID), Rarity: row.Rarity, Enabled: row.Enabled, Version: row.Version}
	}

	skins, err := client.GameCreatureSkin.Query().Order(gamecreatureskin.ByCode()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Creature 皮肤: %w", err)
	}
	data.Skins = make([]creaturemetadata.Skin, len(skins))
	for index, row := range skins {
		data.Skins[index] = creaturemetadata.Skin{ID: snowflake.ID(row.ID), CreatureID: snowflake.ID(row.CreatureID), Code: row.Code, Name: row.Name, AssetID: optionalIdentifier(row.AssetID), Enabled: row.Enabled, Version: row.Version}
	}

	evolutions, err := client.GameCreatureEvolution.Query().Order(gamecreatureevolution.ByID()).All(ctx)
	if err != nil {
		return data, fmt.Errorf("查询 Creature Evolution 关系: %w", err)
	}
	data.Evolutions = make([]creaturemetadata.Evolution, len(evolutions))
	for index, row := range evolutions {
		data.Evolutions[index] = creaturemetadata.Evolution{
			ID: snowflake.ID(row.ID), FromCreatureID: snowflake.ID(row.FromCreatureID), ToCreatureID: snowflake.ID(row.ToCreatureID),
			TriggerType: creaturemetadata.EvolutionTriggerType(row.TriggerType), MinimumLevel: row.MinimumLevel,
			TriggerItemID: optionalIdentifier(row.TriggerItemID), MinimumFriendship: row.MinimumFriendship,
			TimeOfDay: row.TimeOfDay, Gender: row.Gender, RequiredSkillID: optionalIdentifier(row.RequiredSkillID),
			ConditionText: row.ConditionText, Enabled: row.Enabled, Version: row.Version,
		}
	}
	return data, nil
}
