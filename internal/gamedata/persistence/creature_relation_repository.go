package persistence

import (
	"context"
	"errors"
	"fmt"
	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgtype"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamecreature"
	"github.com/lishangbu/avalon/ent/gamecreatureability"
	"github.com/lishangbu/avalon/ent/gamecreatureevolution"
	"github.com/lishangbu/avalon/ent/gamecreatureform"
	"github.com/lishangbu/avalon/ent/gamecreatureformelement"
	"github.com/lishangbu/avalon/ent/gamecreaturehelditem"
	"github.com/lishangbu/avalon/ent/gamecreatureskilllearn"
	"github.com/lishangbu/avalon/ent/gamecreatureskin"
	"github.com/lishangbu/avalon/ent/gamecreaturestat"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
)

const replaceCreatureRelationsOperationID = "game-data.creature-relations.replace"

// GetCreatureRelations 读取一个 Creature 的全部独立关系记录，不扫描其它 Creature。
func (s *Adapters) GetCreatureRelations(ctx context.Context, creatureID snowflake.ID) (creaturemetadata.CreatureRelations, error) {
	if _, err := s.pool.Client(ctx).GameCreature.Query().Where(gamecreature.IDEQ(creatureID)).Only(ctx); avalonent.IsNotFound(err) {
		return creaturemetadata.CreatureRelations{}, creaturemetadata.ErrCreatureDataNotFound
	} else if err != nil {
		return creaturemetadata.CreatureRelations{}, fmt.Errorf("查询关系所属 Creature: %w", err)
	}
	return readManagedCreatureRelations(ctx, s.pool.Client(ctx), creatureID)
}

// ReplaceCreatureRelations 原子保存一个 Creature 的关系集合，并禁用载荷中已经移除的记录。
func (w *creatureDataTransactionRepository) ReplaceCreatureRelations(ctx context.Context, record creaturemetadata.ReplaceRelationsRecord) (creaturemetadata.CreatureRelations, error) {
	value := record.Relations
	requestPayload := creatureRelationsIdempotencyPayload(value)
	replay, request, writer, err := claimCreatureResponseForPayload(ctx, w.client, w.parent.newID, record.ActorAccountID, replaceCreatureRelationsOperationID, record.IdempotencyKey, record.At, requestPayload, &value)
	if err != nil || replay {
		if err != nil {
			return value, fmt.Errorf("认领 Creature 关系幂等键: %w", err)
		}
		return value, nil
	}
	if _, err := w.client.GameCreature.Query().Where(gamecreature.IDEQ(record.CreatureID)).Only(ctx); avalonent.IsNotFound(err) {
		return creaturemetadata.CreatureRelations{}, creaturemetadata.ErrCreatureDataNotFound
	} else if err != nil {
		return creaturemetadata.CreatureRelations{}, fmt.Errorf("锁定关系所属 Creature: %w", err)
	}
	before, err := readManagedCreatureRelations(ctx, w.client, record.CreatureID)
	if err != nil {
		return creaturemetadata.CreatureRelations{}, fmt.Errorf("读取 Creature 关系快照: %w", err)
	}
	if err := w.saveCreatureRelations(ctx, record); err != nil {
		return creaturemetadata.CreatureRelations{}, err
	}
	value, err = readManagedCreatureRelations(ctx, w.client, record.CreatureID)
	if err != nil {
		return creaturemetadata.CreatureRelations{}, fmt.Errorf("读取 Creature 关系结果: %w", err)
	}
	if err := w.completeCreatureWrite(ctx, record.GameDataWriteContext, record.At, "game-data.creature-relations.replaced", "game_creature", record.CreatureID, &before, &value, request, writer, value); err != nil {
		return creaturemetadata.CreatureRelations{}, fmt.Errorf("完成 Creature 关系写入: %w", err)
	}
	return value, nil
}

// creatureRelationsIdempotencyPayload 清除新关系由服务端生成的 Identifier，使客户端原始载荷相同的重试
// 命中第一次响应；已有关系的身份和版本仍保留在摘要中，防止不同更新错误复用幂等键。
func creatureRelationsIdempotencyPayload(value creaturemetadata.CreatureRelations) creaturemetadata.CreatureRelations {
	value.Forms = append([]creaturemetadata.Form(nil), value.Forms...)
	for index := range value.Forms {
		if value.Forms[index].Version == 0 {
			value.Forms[index].ID = snowflake.ID(0)
		}
	}
	value.Stats = append([]creaturemetadata.StatBinding(nil), value.Stats...)
	for index := range value.Stats {
		if value.Stats[index].Version == 0 {
			value.Stats[index].ID = snowflake.ID(0)
		}
	}
	value.SkillLearns = append([]creaturemetadata.SkillLearn(nil), value.SkillLearns...)
	for index := range value.SkillLearns {
		if value.SkillLearns[index].Version == 0 {
			value.SkillLearns[index].ID = snowflake.ID(0)
		}
	}
	value.Abilities = append([]creaturemetadata.AbilityBinding(nil), value.Abilities...)
	for index := range value.Abilities {
		if value.Abilities[index].Version == 0 {
			value.Abilities[index].ID = snowflake.ID(0)
		}
	}
	value.HeldItems = append([]creaturemetadata.HeldItem(nil), value.HeldItems...)
	for index := range value.HeldItems {
		if value.HeldItems[index].Version == 0 {
			value.HeldItems[index].ID = snowflake.ID(0)
		}
	}
	value.Skins = append([]creaturemetadata.Skin(nil), value.Skins...)
	for index := range value.Skins {
		if value.Skins[index].Version == 0 {
			value.Skins[index].ID = snowflake.ID(0)
		}
	}
	value.Evolutions = append([]creaturemetadata.Evolution(nil), value.Evolutions...)
	for index := range value.Evolutions {
		if value.Evolutions[index].Version == 0 {
			value.Evolutions[index].ID = snowflake.ID(0)
		}
	}
	return value
}

func readManagedCreatureRelations(ctx context.Context, client *avalonent.Client, creatureID snowflake.ID) (creaturemetadata.CreatureRelations, error) {
	var result creaturemetadata.CreatureRelations
	creatureEntID := creatureID
	forms, err := client.GameCreatureForm.Query().Where(gamecreatureform.CreatureIDEQ(creatureEntID)).Order(gamecreatureform.ByCode()).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询 Creature 形态: %w", err)
	}
	formIDs := make([]snowflake.ID, len(forms))
	for index := range forms {
		formIDs[index] = forms[index].ID
	}
	elements, err := client.GameCreatureFormElement.Query().Where(gamecreatureformelement.FormIDIn(formIDs...)).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询 Creature 形态属性: %w", err)
	}
	elementsByForm := make(map[snowflake.ID][]snowflake.ID)
	for _, row := range elements {
		elementsByForm[snowflake.ID(row.FormID)] = append(elementsByForm[snowflake.ID(row.FormID)], snowflake.ID(row.ElementID))
	}
	result.Forms = make([]creaturemetadata.Form, len(forms))
	for index, row := range forms {
		result.Forms[index] = creaturemetadata.Form{ID: snowflake.ID(row.ID), Code: row.Code, Name: row.Name, CreatureID: snowflake.ID(row.CreatureID), FormName: row.FormName, SortOrder: row.SortOrder, FormOrder: row.FormOrder, BattleOnly: row.BattleOnly, DefaultForm: row.DefaultForm, EnhancedForm: row.EnhancedForm, Enabled: row.Enabled, Version: row.Version, ElementIDs: elementsByForm[snowflake.ID(row.ID)]}
	}
	stats, err := client.GameCreatureStat.Query().Where(gamecreaturestat.CreatureIDEQ(creatureEntID)).Order(gamecreaturestat.ByID()).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询 Creature 能力关系: %w", err)
	}
	result.Stats = make([]creaturemetadata.StatBinding, len(stats))
	for index, row := range stats {
		effort := int32(row.Effort)
		result.Stats[index] = creaturemetadata.StatBinding{ID: snowflake.ID(row.ID), CreatureID: snowflake.ID(row.CreatureID), StatID: snowflake.ID(row.StatID), BaseValue: row.BaseValue, Effort: &effort, Enabled: row.Enabled, Version: row.Version}
	}
	learns, err := client.GameCreatureSkillLearn.Query().Where(gamecreatureskilllearn.CreatureIDEQ(creatureEntID)).Order(gamecreatureskilllearn.ByID()).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询 Creature 技能学习关系: %w", err)
	}
	result.SkillLearns = make([]creaturemetadata.SkillLearn, len(learns))
	for index, row := range learns {
		result.SkillLearns[index] = creaturemetadata.SkillLearn{ID: snowflake.ID(row.ID), CreatureID: snowflake.ID(row.CreatureID), SkillID: snowflake.ID(row.SkillID), LearnMethodID: snowflake.ID(row.LearnMethodID), LevelLearnedAt: row.LevelLearnedAt, Enabled: row.Enabled, Version: row.Version}
	}
	abilities, err := client.GameCreatureAbility.Query().Where(gamecreatureability.CreatureIDEQ(creatureEntID)).Order(gamecreatureability.ByID()).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询 Creature 特性关系: %w", err)
	}
	result.Abilities = make([]creaturemetadata.AbilityBinding, len(abilities))
	for index, row := range abilities {
		result.Abilities[index] = creaturemetadata.AbilityBinding{ID: snowflake.ID(row.ID), CreatureID: snowflake.ID(row.CreatureID), AbilityID: snowflake.ID(row.AbilityID), Hidden: row.Hidden, Slot: row.Slot, Enabled: row.Enabled, Version: row.Version}
	}
	heldItems, err := client.GameCreatureHeldItem.Query().Where(gamecreaturehelditem.CreatureIDEQ(creatureEntID)).Order(gamecreaturehelditem.ByID()).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询 Creature 携带物关系: %w", err)
	}
	result.HeldItems = make([]creaturemetadata.HeldItem, len(heldItems))
	for index, row := range heldItems {
		result.HeldItems[index] = creaturemetadata.HeldItem{ID: snowflake.ID(row.ID), CreatureID: snowflake.ID(row.CreatureID), ItemID: snowflake.ID(row.ItemID), Rarity: row.Rarity, Enabled: row.Enabled, Version: row.Version}
	}
	skins, err := client.GameCreatureSkin.Query().Where(gamecreatureskin.CreatureIDEQ(creatureEntID)).Order(gamecreatureskin.ByCode()).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询 Creature 皮肤: %w", err)
	}
	result.Skins = make([]creaturemetadata.Skin, len(skins))
	for index, row := range skins {
		result.Skins[index] = creaturemetadata.Skin{ID: snowflake.ID(row.ID), CreatureID: snowflake.ID(row.CreatureID), Code: row.Code, Name: row.Name, AssetID: optionalIdentifier(row.AssetID), Enabled: row.Enabled, Version: row.Version}
	}
	evolutions, err := client.GameCreatureEvolution.Query().Where(gamecreatureevolution.FromCreatureIDEQ(creatureEntID)).Order(gamecreatureevolution.ByID()).All(ctx)
	if err != nil {
		return result, fmt.Errorf("查询 Creature Evolution 关系: %w", err)
	}
	result.Evolutions = make([]creaturemetadata.Evolution, len(evolutions))
	for index, row := range evolutions {
		result.Evolutions[index] = creaturemetadata.Evolution{
			ID: snowflake.ID(row.ID), FromCreatureID: snowflake.ID(row.FromCreatureID), ToCreatureID: snowflake.ID(row.ToCreatureID),
			TriggerType: creaturemetadata.EvolutionTriggerType(row.TriggerType), MinimumLevel: row.MinimumLevel,
			TriggerItemID: optionalIdentifier(row.TriggerItemID), MinimumFriendship: row.MinimumFriendship,
			TimeOfDay: row.TimeOfDay, Gender: row.Gender, RequiredSkillID: optionalIdentifier(row.RequiredSkillID),
			ConditionText: row.ConditionText, Enabled: row.Enabled, Version: row.Version,
		}
	}
	return result, nil
}

func (w *creatureDataTransactionRepository) saveCreatureRelations(ctx context.Context, record creaturemetadata.ReplaceRelationsRecord) error {
	at := databaseTime(record.At)
	// 唯一索引只允许一个默认形态。事务内先清除旧标记，再按载荷恢复目标默认形态，
	// 从而使默认形态切换不依赖前端数组顺序；任何后续失败都会随事务整体回滚。
	if _, err := w.client.GameCreatureForm.Update().Where(gamecreatureform.CreatureIDEQ(record.CreatureID), gamecreatureform.DefaultFormEQ(true)).SetDefaultForm(false).SetUpdatedAt(record.At.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("准备切换 Creature 默认形态: %w", err)
	}
	for _, value := range record.Relations.Forms {
		formID := value.ID
		if value.Version == 0 {
			_, err := w.client.GameCreatureForm.Create().SetID(formID).SetCode(value.Code).SetName(value.Name).SetCreatureID(record.CreatureID).SetNillableFormName(value.FormName).SetNillableSortOrder(value.SortOrder).SetNillableFormOrder(value.FormOrder).SetBattleOnly(value.BattleOnly).SetDefaultForm(value.DefaultForm).SetEnhancedForm(value.EnhancedForm).SetEnabled(value.Enabled).SetVersion(1).SetCreatedAt(record.At.UTC()).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature 形态", err)
			}
		} else {
			rows, err := w.client.GameCreatureForm.Update().Where(gamecreatureform.IDEQ(formID), gamecreatureform.VersionEQ(value.Version)).SetCode(value.Code).SetName(value.Name).SetNillableFormName(value.FormName).SetNillableSortOrder(value.SortOrder).SetNillableFormOrder(value.FormOrder).SetBattleOnly(value.BattleOnly).SetDefaultForm(value.DefaultForm).SetEnhancedForm(value.EnhancedForm).SetEnabled(value.Enabled).SetVersion(value.Version + 1).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature 形态", err)
			}
			if rows != 1 {
				return creaturemetadata.ErrCreatureDataConflict
			}
		}
		if _, err := w.client.GameCreatureFormElement.Delete().Where(gamecreatureformelement.FormIDEQ(formID)).Exec(ctx); err != nil {
			return fmt.Errorf("替换 Creature 形态属性: %w", err)
		}
		for _, elementID := range value.ElementIDs {
			relationID, err := w.parent.newID.Next(ctx)
			if err != nil {
				return fmt.Errorf("生成 Creature 形态属性关系 Identifier: %w", err)
			}
			if _, err := w.client.GameCreatureFormElement.Create().SetID(relationID).SetFormID(formID).SetElementID(elementID).Save(ctx); err != nil {
				return creatureDataDatabaseError("保存 Creature 形态属性", err)
			}
		}
	}
	for _, value := range record.Relations.Stats {
		id := value.ID
		if value.Version == 0 {
			_, err := w.client.GameCreatureStat.Create().SetID(id).SetCreatureID(record.CreatureID).SetStatID(value.StatID).SetBaseValue(value.BaseValue).SetEffort(int16(*value.Effort)).SetEnabled(value.Enabled).SetVersion(1).SetCreatedAt(record.At.UTC()).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature 能力关系", err)
			}
		} else {
			rows, err := w.client.GameCreatureStat.Update().Where(gamecreaturestat.IDEQ(id), gamecreaturestat.VersionEQ(value.Version)).SetStatID(value.StatID).SetBaseValue(value.BaseValue).SetEffort(int16(*value.Effort)).SetEnabled(value.Enabled).SetVersion(value.Version + 1).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature 能力关系", err)
			}
			if rows != 1 {
				return creaturemetadata.ErrCreatureDataConflict
			}
		}
	}
	for _, value := range record.Relations.SkillLearns {
		id := value.ID
		if value.Version == 0 {
			_, err := w.client.GameCreatureSkillLearn.Create().SetID(id).SetCreatureID(record.CreatureID).SetSkillID(value.SkillID).SetLearnMethodID(value.LearnMethodID).SetLevelLearnedAt(value.LevelLearnedAt).SetEnabled(value.Enabled).SetVersion(1).SetCreatedAt(record.At.UTC()).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature 技能学习关系", err)
			}
		} else {
			rows, err := w.client.GameCreatureSkillLearn.Update().Where(gamecreatureskilllearn.IDEQ(id), gamecreatureskilllearn.VersionEQ(value.Version)).SetSkillID(value.SkillID).SetLearnMethodID(value.LearnMethodID).SetLevelLearnedAt(value.LevelLearnedAt).SetEnabled(value.Enabled).SetVersion(value.Version + 1).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature 技能学习关系", err)
			}
			if rows != 1 {
				return creaturemetadata.ErrCreatureDataConflict
			}
		}
	}
	for _, value := range record.Relations.Abilities {
		id := value.ID
		if value.Version == 0 {
			_, err := w.client.GameCreatureAbility.Create().SetID(id).SetCreatureID(record.CreatureID).SetAbilityID(value.AbilityID).SetHidden(value.Hidden).SetSlot(value.Slot).SetEnabled(value.Enabled).SetVersion(1).SetCreatedAt(record.At.UTC()).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature 特性关系", err)
			}
		} else {
			rows, err := w.client.GameCreatureAbility.Update().Where(gamecreatureability.IDEQ(id), gamecreatureability.VersionEQ(value.Version)).SetAbilityID(value.AbilityID).SetHidden(value.Hidden).SetSlot(value.Slot).SetEnabled(value.Enabled).SetVersion(value.Version + 1).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature 特性关系", err)
			}
			if rows != 1 {
				return creaturemetadata.ErrCreatureDataConflict
			}
		}
	}
	for _, value := range record.Relations.HeldItems {
		id := value.ID
		if value.Version == 0 {
			_, err := w.client.GameCreatureHeldItem.Create().SetID(id).SetCreatureID(record.CreatureID).SetItemID(value.ItemID).SetRarity(value.Rarity).SetEnabled(value.Enabled).SetVersion(1).SetCreatedAt(record.At.UTC()).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature 携带物关系", err)
			}
		} else {
			rows, err := w.client.GameCreatureHeldItem.Update().Where(gamecreaturehelditem.IDEQ(id), gamecreaturehelditem.VersionEQ(value.Version)).SetItemID(value.ItemID).SetRarity(value.Rarity).SetEnabled(value.Enabled).SetVersion(value.Version + 1).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature 携带物关系", err)
			}
			if rows != 1 {
				return creaturemetadata.ErrCreatureDataConflict
			}
		}
	}
	for _, value := range record.Relations.Skins {
		id := value.ID
		if value.Version == 0 {
			_, err := w.client.GameCreatureSkin.Create().SetID(id).SetCreatureID(record.CreatureID).SetCode(value.Code).SetName(value.Name).SetNillableAssetID(optionalEntIdentifier(value.AssetID)).SetEnabled(value.Enabled).SetVersion(1).SetCreatedAt(record.At.UTC()).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature 皮肤", err)
			}
		} else {
			rows, err := w.client.GameCreatureSkin.Update().Where(gamecreatureskin.IDEQ(id), gamecreatureskin.VersionEQ(value.Version)).SetCode(value.Code).SetName(value.Name).SetNillableAssetID(optionalEntIdentifier(value.AssetID)).SetEnabled(value.Enabled).SetVersion(value.Version + 1).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature 皮肤", err)
			}
			if rows != 1 {
				return creaturemetadata.ErrCreatureDataConflict
			}
		}
	}
	for _, value := range record.Relations.Evolutions {
		id := value.ID
		if value.Version == 0 {
			_, err := w.client.GameCreatureEvolution.Create().SetID(id).SetFromCreatureID(record.CreatureID).SetToCreatureID(value.ToCreatureID).SetTriggerType(string(value.TriggerType)).SetNillableMinimumLevel(value.MinimumLevel).SetNillableTriggerItemID(optionalEntIdentifier(value.TriggerItemID)).SetNillableMinimumFriendship(value.MinimumFriendship).SetNillableTimeOfDay(value.TimeOfDay).SetNillableGender(value.Gender).SetNillableRequiredSkillID(optionalEntIdentifier(value.RequiredSkillID)).SetConditionText(value.ConditionText).SetEnabled(value.Enabled).SetVersion(1).SetCreatedAt(record.At.UTC()).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature Evolution 关系", err)
			}
		} else {
			rows, err := w.client.GameCreatureEvolution.Update().Where(gamecreatureevolution.IDEQ(id), gamecreatureevolution.VersionEQ(value.Version)).SetToCreatureID(value.ToCreatureID).SetTriggerType(string(value.TriggerType)).SetNillableMinimumLevel(value.MinimumLevel).SetNillableTriggerItemID(optionalEntIdentifier(value.TriggerItemID)).SetNillableMinimumFriendship(value.MinimumFriendship).SetNillableTimeOfDay(value.TimeOfDay).SetNillableGender(value.Gender).SetNillableRequiredSkillID(optionalEntIdentifier(value.RequiredSkillID)).SetConditionText(value.ConditionText).SetEnabled(value.Enabled).SetVersion(value.Version + 1).SetUpdatedAt(record.At.UTC()).Save(ctx)
			if err != nil {
				return creatureDataDatabaseError("保存 Creature Evolution 关系", err)
			}
			if rows != 1 {
				return creaturemetadata.ErrCreatureDataConflict
			}
		}
	}
	return w.disableMissingCreatureRelations(ctx, record.CreatureID, record.Relations, at)
}

func (w *creatureDataTransactionRepository) disableMissingCreatureRelations(ctx context.Context, creatureID snowflake.ID, relations creaturemetadata.CreatureRelations, at pgtype.Timestamptz) error {
	creatureEntID := creatureID
	formIDs := relationEntIDs(relations.Forms, func(value creaturemetadata.Form) snowflake.ID { return value.ID })
	formQuery := w.client.GameCreatureForm.Update().Where(gamecreatureform.CreatureIDEQ(creatureEntID), gamecreatureform.EnabledEQ(true))
	if len(formIDs) > 0 {
		formQuery.Where(gamecreatureform.IDNotIn(formIDs...))
	}
	if _, err := formQuery.SetEnabled(false).AddVersion(1).SetUpdatedAt(at.Time.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("禁用已移除 Creature 形态: %w", err)
	}
	statIDs := relationEntIDs(relations.Stats, func(value creaturemetadata.StatBinding) snowflake.ID { return value.ID })
	statQuery := w.client.GameCreatureStat.Update().Where(gamecreaturestat.CreatureIDEQ(creatureEntID), gamecreaturestat.EnabledEQ(true))
	if len(statIDs) > 0 {
		statQuery.Where(gamecreaturestat.IDNotIn(statIDs...))
	}
	if _, err := statQuery.SetEnabled(false).AddVersion(1).SetUpdatedAt(at.Time.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("禁用已移除 Creature 能力关系: %w", err)
	}
	skillIDs := relationEntIDs(relations.SkillLearns, func(value creaturemetadata.SkillLearn) snowflake.ID { return value.ID })
	skillQuery := w.client.GameCreatureSkillLearn.Update().Where(gamecreatureskilllearn.CreatureIDEQ(creatureEntID), gamecreatureskilllearn.EnabledEQ(true))
	if len(skillIDs) > 0 {
		skillQuery.Where(gamecreatureskilllearn.IDNotIn(skillIDs...))
	}
	if _, err := skillQuery.SetEnabled(false).AddVersion(1).SetUpdatedAt(at.Time.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("禁用已移除 Creature 技能学习关系: %w", err)
	}
	abilityIDs := relationEntIDs(relations.Abilities, func(value creaturemetadata.AbilityBinding) snowflake.ID { return value.ID })
	abilityQuery := w.client.GameCreatureAbility.Update().Where(gamecreatureability.CreatureIDEQ(creatureEntID), gamecreatureability.EnabledEQ(true))
	if len(abilityIDs) > 0 {
		abilityQuery.Where(gamecreatureability.IDNotIn(abilityIDs...))
	}
	if _, err := abilityQuery.SetEnabled(false).AddVersion(1).SetUpdatedAt(at.Time.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("禁用已移除 Creature 特性关系: %w", err)
	}
	heldIDs := relationEntIDs(relations.HeldItems, func(value creaturemetadata.HeldItem) snowflake.ID { return value.ID })
	heldQuery := w.client.GameCreatureHeldItem.Update().Where(gamecreaturehelditem.CreatureIDEQ(creatureEntID), gamecreaturehelditem.EnabledEQ(true))
	if len(heldIDs) > 0 {
		heldQuery.Where(gamecreaturehelditem.IDNotIn(heldIDs...))
	}
	if _, err := heldQuery.SetEnabled(false).AddVersion(1).SetUpdatedAt(at.Time.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("禁用已移除 Creature 携带物关系: %w", err)
	}
	skinIDs := relationEntIDs(relations.Skins, func(value creaturemetadata.Skin) snowflake.ID { return value.ID })
	skinQuery := w.client.GameCreatureSkin.Update().Where(gamecreatureskin.CreatureIDEQ(creatureEntID), gamecreatureskin.EnabledEQ(true))
	if len(skinIDs) > 0 {
		skinQuery.Where(gamecreatureskin.IDNotIn(skinIDs...))
	}
	if _, err := skinQuery.SetEnabled(false).AddVersion(1).SetUpdatedAt(at.Time.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("禁用已移除 Creature 皮肤: %w", err)
	}
	evolutionIDs := relationEntIDs(relations.Evolutions, func(value creaturemetadata.Evolution) snowflake.ID { return value.ID })
	evolutionQuery := w.client.GameCreatureEvolution.Update().Where(gamecreatureevolution.FromCreatureIDEQ(creatureEntID), gamecreatureevolution.EnabledEQ(true))
	if len(evolutionIDs) > 0 {
		evolutionQuery.Where(gamecreatureevolution.IDNotIn(evolutionIDs...))
	}
	if _, err := evolutionQuery.SetEnabled(false).AddVersion(1).SetUpdatedAt(at.Time.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("禁用已移除 Creature Evolution 关系: %w", err)
	}
	return nil
}

// relationEntIDs 将关系载荷中的领域 Identifier 转为 Ent 谓词使用的 Identifier 列表。
func relationEntIDs[T any](values []T, id func(T) snowflake.ID) []snowflake.ID {
	result := make([]snowflake.ID, len(values))
	for index, value := range values {
		result[index] = id(value)
	}
	return result
}

// optionalIdentifier 将 Ent 的可空 Identifier 指针转换为领域层可空 Identifier。
func optionalIdentifier(value *snowflake.ID) *snowflake.ID {
	if value == nil {
		return nil
	}
	converted := snowflake.ID(*value)
	return &converted
}

func existingExpectedVersion(version int64) int64 { return version }

func relationSaveError(action string, err error) error {
	if errors.Is(err, pgx.ErrNoRows) {
		return creaturemetadata.ErrCreatureDataConflict
	}
	if err != nil {
		return creatureDataDatabaseError(action, err)
	}
	return nil
}

func relationDatabaseIDs[T any](values []T, id func(T) snowflake.ID) []pgtype.Int8 {
	result := make([]pgtype.Int8, len(values))
	for index, value := range values {
		result[index] = databaseIdentifier(id(value))
	}
	return result
}
