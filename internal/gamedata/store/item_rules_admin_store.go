package store

import (
	"context"
	"fmt"
	"reflect"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameitem"
	"github.com/lishangbu/avalon/ent/gameitemactionrule"
	"github.com/lishangbu/avalon/ent/gameitemcontactrule"
	"github.com/lishangbu/avalon/ent/gameitemdamagerule"
	"github.com/lishangbu/avalon/ent/gameitemmultihitrule"
	"github.com/lishangbu/avalon/ent/gameitemrecoveryrule"
	"github.com/lishangbu/avalon/ent/gameitemstatboosterability"
	"github.com/lishangbu/avalon/ent/gameitemstatrule"
	"github.com/lishangbu/avalon/ent/gameitemstatusrule"
	"github.com/lishangbu/avalon/ent/gameitemswitchrule"
	"github.com/lishangbu/avalon/ent/gameitemweatherrule"
	"github.com/lishangbu/avalon/ent/gameitemweightrule"
	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/gamedata/itemrules"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GetManagedItemRules 读取指定道具及其全部规范化规则关系。
func (s *Store) GetManagedItemRules(ctx context.Context, itemID snowflake.ID) (item.Rules, error) {
	row, err := s.pool.Client(ctx).GameItem.Query().Where(gameitem.IDEQ(itemID)).
		WithActionRules().WithDamageRules().WithStatRules().WithStatusRules().WithSwitchRules().
		WithContactRules().WithRecoveryRules().WithWeatherRules().WithMultiHitRules().WithWeightRules().
		WithStatBoosterAbilities().Only(ctx)
	if avalonent.IsNotFound(err) {
		return item.Rules{}, item.ErrItemNotFound
	}
	if err != nil {
		return item.Rules{}, fmt.Errorf("读取道具规则聚合: %w", err)
	}
	return item.Rules{ItemID: itemID, Version: row.Version, Rules: itemRuleDetail(row)}, nil
}

// ReplaceItemRules 使用道具主体版本在单一事务内替换全部规范化规则关系。
func (s *Store) ReplaceItemRules(ctx context.Context, record item.ReplaceRulesRecord) (item.Rules, error) {
	var result item.Rules
	err := s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := s.pool.Client(transactionCtx)
		digest, err := idempotency.Digest(struct {
			ItemID          snowflake.ID
			ExpectedVersion int64
			Rules           itemrules.Detail
		}{record.ItemID, record.ExpectedVersion, record.Rules})
		if err != nil {
			return fmt.Errorf("计算道具规则幂等摘要: %w", err)
		}
		request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: "game-data.item-rules.replace", Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt}
		writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(client, s.newID))
		if replay, claimErr := idempotency.ClaimResponse(transactionCtx, writer, request, &result); claimErr != nil {
			return fmt.Errorf("认领道具规则幂等键: %w", claimErr)
		} else if replay {
			return nil
		}
		current, err := client.GameItem.Query().Where(gameitem.IDEQ(record.ItemID)).Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			return item.ErrItemNotFound
		}
		if err != nil {
			return fmt.Errorf("读取待更新道具: %w", err)
		}
		if current.Version != record.ExpectedVersion {
			return item.ErrItemVersionConflict
		}
		if err := deleteItemRuleRelations(transactionCtx, client, record.ItemID); err != nil {
			return err
		}
		if err := s.createItemRuleRelations(transactionCtx, client, record); err != nil {
			return err
		}
		count, err := client.GameItem.Update().Where(gameitem.IDEQ(record.ItemID), gameitem.VersionEQ(record.ExpectedVersion)).
			SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.UpdatedAt).Save(transactionCtx)
		if err != nil {
			return fmt.Errorf("递增道具规则聚合版本: %w", err)
		}
		if count != 1 {
			return item.ErrItemVersionConflict
		}
		if err := s.recordGameDataAudit(transactionCtx, database.Executor(transactionCtx, s.pool), record.ActorAccountID,
			"game-data.item-rules.replace", "game_item_rules", record.ItemID, record.RequestID,
			record.UpdatedAt, nil, record.Rules); err != nil {
			return err
		}
		result = item.Rules{ItemID: record.ItemID, Version: record.ExpectedVersion + 1, Rules: record.Rules}
		if err := idempotency.Complete(transactionCtx, writer, request, result); err != nil {
			return fmt.Errorf("保存道具规则幂等结果: %w", err)
		}
		return nil
	})
	return result, err
}

func deleteItemRuleRelations(ctx context.Context, client *avalonent.Client, itemID snowflake.ID) error {
	deletes := []func() error{
		func() error {
			_, err := client.GameItemActionRule.Delete().Where(gameitemactionrule.ItemIDEQ(itemID)).Exec(ctx)
			return err
		},
		func() error {
			_, err := client.GameItemDamageRule.Delete().Where(gameitemdamagerule.ItemIDEQ(itemID)).Exec(ctx)
			return err
		},
		func() error {
			_, err := client.GameItemStatRule.Delete().Where(gameitemstatrule.ItemIDEQ(itemID)).Exec(ctx)
			return err
		},
		func() error {
			_, err := client.GameItemStatusRule.Delete().Where(gameitemstatusrule.ItemIDEQ(itemID)).Exec(ctx)
			return err
		},
		func() error {
			_, err := client.GameItemSwitchRule.Delete().Where(gameitemswitchrule.ItemIDEQ(itemID)).Exec(ctx)
			return err
		},
		func() error {
			_, err := client.GameItemContactRule.Delete().Where(gameitemcontactrule.ItemIDEQ(itemID)).Exec(ctx)
			return err
		},
		func() error {
			_, err := client.GameItemRecoveryRule.Delete().Where(gameitemrecoveryrule.ItemIDEQ(itemID)).Exec(ctx)
			return err
		},
		func() error {
			_, err := client.GameItemWeatherRule.Delete().Where(gameitemweatherrule.ItemIDEQ(itemID)).Exec(ctx)
			return err
		},
		func() error {
			_, err := client.GameItemMultiHitRule.Delete().Where(gameitemmultihitrule.ItemIDEQ(itemID)).Exec(ctx)
			return err
		},
		func() error {
			_, err := client.GameItemWeightRule.Delete().Where(gameitemweightrule.ItemIDEQ(itemID)).Exec(ctx)
			return err
		},
		func() error {
			_, err := client.GameItemStatBoosterAbility.Delete().Where(gameitemstatboosterability.ItemIDEQ(itemID)).Exec(ctx)
			return err
		},
	}
	for _, remove := range deletes {
		if err := remove(); err != nil {
			return fmt.Errorf("清理旧道具规则关系: %w", err)
		}
	}
	return nil
}

func (s *Store) createItemRuleRelations(ctx context.Context, client *avalonent.Client, record item.ReplaceRulesRecord) error {
	r := record.Rules
	createID := func() (snowflake.ID, error) { return s.newID.Next(ctx) }
	if anyRule(r, "ChargeSkipOnce", "ChoiceSkillLock", "ForcedLastActionOrder", "LowHPActionOrderBoost", "FieldSpeedOrderSpeedStageDrop", "AdditionalFlinchChancePercent", "RandomActionOrderBoostChancePercent", "AccuracyAfterTargetActedBoost", "SurviveFatalDamageAtFullHP", "BindingTurns", "BindingDamageDenominator") {
		id, err := createID()
		if err != nil {
			return err
		}
		_, err = client.GameItemActionRule.Create().SetID(id).SetItemID(record.ItemID).SetChargeSkipOnce(r.ChargeSkipOnce).SetChoiceSkillLock(r.ChoiceSkillLock).SetForcedLastActionOrder(r.ForcedLastActionOrder).SetLowHpActionOrderBoost(r.LowHPActionOrderBoost).SetFieldSpeedOrderSpeedStageDrop(r.FieldSpeedOrderSpeedStageDrop).SetAdditionalFlinchChancePercent(r.AdditionalFlinchChancePercent).SetRandomActionOrderBoostChancePercent(r.RandomActionOrderBoostChancePercent).SetAccuracyAfterTargetActedBoost(r.AccuracyAfterTargetActedBoost).SetSurviveFatalDamageAtFullHp(r.SurviveFatalDamageAtFullHP).SetBindingTurns(r.BindingTurns).SetBindingDamageDenominator(r.BindingDamageDenominator).SetVersion(1).SetCreatedAt(record.UpdatedAt).SetUpdatedAt(record.UpdatedAt).Save(ctx)
		if err != nil {
			return fmt.Errorf("保存道具行动规则: %w", err)
		}
	}
	if anyRule(r, "PhysicalDamagePowerBoost", "SpecialDamagePowerBoost", "PhysicalDamagePowerBoost50", "SpecialDamagePowerBoost50", "SuperEffectiveDamageBoost", "DamageBoostWithRecoil", "DamageDealtHeal", "DrainHealingBoost", "WeaknessPolicy", "ConsecutiveSkillDamageBoost", "ElementDamageBoostElementID", "ConsumableElementDamageBoostElementID", "ConsumableElementDamageBoostNumerator", "ConsumableElementDamageBoostDenominator", "ElementDamageReductionElementID", "ElementDamageReductionRequiresSuperEffective") {
		id, err := createID()
		if err != nil {
			return err
		}
		b := client.GameItemDamageRule.Create().SetID(id).SetItemID(record.ItemID).SetPhysicalDamagePowerBoost(r.PhysicalDamagePowerBoost).SetSpecialDamagePowerBoost(r.SpecialDamagePowerBoost).SetPhysicalDamagePowerBoost50(r.PhysicalDamagePowerBoost50).SetSpecialDamagePowerBoost50(r.SpecialDamagePowerBoost50).SetSuperEffectiveDamageBoost(r.SuperEffectiveDamageBoost).SetDamageBoostWithRecoil(r.DamageBoostWithRecoil).SetDamageDealtHeal(r.DamageDealtHeal).SetDrainHealingBoost(r.DrainHealingBoost).SetWeaknessPolicy(r.WeaknessPolicy).SetConsecutiveSkillDamageBoost(r.ConsecutiveSkillDamageBoost).SetConsumableBoostNumerator(r.ConsumableElementDamageBoostNumerator).SetConsumableBoostDenominator(r.ConsumableElementDamageBoostDenominator).SetReductionRequiresSuperEffective(r.ElementDamageReductionRequiresSuperEffective).SetVersion(1).SetCreatedAt(record.UpdatedAt).SetUpdatedAt(record.UpdatedAt)
		b.SetNillableElementBoostElementID(r.ElementDamageBoostElementID).SetNillableConsumableBoostElementID(r.ConsumableElementDamageBoostElementID).SetNillableReductionElementID(r.ElementDamageReductionElementID)
		if _, err = b.Save(ctx); err != nil {
			return fmt.Errorf("保存道具伤害规则: %w", err)
		}
	}
	if err := s.createRemainingItemRules(ctx, client, record, createID); err != nil {
		return err
	}
	for _, abilityID := range r.HighestStatBoosterAbilityIDs {
		id, err := createID()
		if err != nil {
			return err
		}
		if _, err = client.GameItemStatBoosterAbility.Create().SetID(id).SetItemID(record.ItemID).SetAbilityID(abilityID).SetCreatedAt(record.UpdatedAt).Save(ctx); err != nil {
			return fmt.Errorf("保存道具特性规则绑定: %w", err)
		}
	}
	return nil
}

func (s *Store) createRemainingItemRules(ctx context.Context, client *avalonent.Client, record item.ReplaceRulesRecord, createID func() (snowflake.ID, error)) error {
	r := record.Rules
	stamp := func() (snowflake.ID, error) { return createID() }
	if anyRule(r, "SpecialDefenseBoost", "SpeedHalf", "SpeedBoost50", "AccuracyBoost", "OpponentAccuracyReduction", "CriticalHitStageBoost", "OpponentStatStageReductionImmunity", "NegativeStatStageReset", "AbilityStatReductionSpeedBoost", "OpponentPositiveStatStageCopy", "AccuracyMissStatStageBoostStat", "AccuracyMissStatStageBoostDelta", "WaterDamageSpecialAttackBoostElementID", "ElectricDamageAttackBoostElementID", "WaterDamageSpecialDefenseBoostElementID", "IceDamageAttackBoostElementID") {
		id, err := stamp()
		if err != nil {
			return err
		}
		var stat *string
		if r.AccuracyMissStatStageBoostStat != "" {
			value := string(r.AccuracyMissStatStageBoostStat)
			stat = &value
		}
		b := client.GameItemStatRule.Create().SetID(id).SetItemID(record.ItemID).SetSpecialDefenseBoost(r.SpecialDefenseBoost).SetSpeedHalf(r.SpeedHalf).SetSpeedBoost50(r.SpeedBoost50).SetAccuracyBoost(r.AccuracyBoost).SetOpponentAccuracyReduction(r.OpponentAccuracyReduction).SetCriticalHitStageBoost(r.CriticalHitStageBoost).SetOpponentStatStageReductionImmunity(r.OpponentStatStageReductionImmunity).SetNegativeStatStageReset(r.NegativeStatStageReset).SetAbilityStatReductionSpeedBoost(r.AbilityStatReductionSpeedBoost).SetOpponentPositiveStatStageCopy(r.OpponentPositiveStatStageCopy).SetNillableAccuracyMissStatStageBoostStat(stat).SetAccuracyMissStatStageBoostDelta(r.AccuracyMissStatStageBoostDelta).SetNillableWaterSpaElementID(r.WaterDamageSpecialAttackBoostElementID).SetNillableElectricAtkElementID(r.ElectricDamageAttackBoostElementID).SetNillableWaterSpdElementID(r.WaterDamageSpecialDefenseBoostElementID).SetNillableIceAtkElementID(r.IceDamageAttackBoostElementID).SetVersion(1).SetCreatedAt(record.UpdatedAt).SetUpdatedAt(record.UpdatedAt)
		if _, err = b.Save(ctx); err != nil {
			return fmt.Errorf("保存道具能力规则: %w", err)
		}
	}
	if anyRule(r, "CuresParalysis", "CuresSleep", "CuresPoison", "CuresBurn", "CuresFreeze", "CuresAllMajorStatuses", "CuresConfusion", "PowderSkillImmunity", "StatusSkillRestriction", "DamagingSkillSecondaryEffectImmunity") {
		id, err := stamp()
		if err != nil {
			return err
		}
		_, err = client.GameItemStatusRule.Create().SetID(id).SetItemID(record.ItemID).SetCuresParalysis(r.CuresParalysis).SetCuresSleep(r.CuresSleep).SetCuresPoison(r.CuresPoison).SetCuresBurn(r.CuresBurn).SetCuresFreeze(r.CuresFreeze).SetCuresAllMajorStatuses(r.CuresAllMajorStatuses).SetCuresConfusion(r.CuresConfusion).SetPowderSkillImmunity(r.PowderSkillImmunity).SetStatusSkillRestriction(r.StatusSkillRestriction).SetDamagingSkillSecondaryEffectImmunity(r.DamagingSkillSecondaryEffectImmunity).SetVersion(1).SetCreatedAt(record.UpdatedAt).SetUpdatedAt(record.UpdatedAt).Save(ctx)
		if err != nil {
			return fmt.Errorf("保存道具状态规则: %w", err)
		}
	}
	if anyRule(r, "DamagedForceSelfSwitch", "DamagedForceAttackerSwitch", "NegativeStatStageForceSelfSwitch", "SwitchRestrictionImmunity", "EntryHazardImmunity") {
		id, err := stamp()
		if err != nil {
			return err
		}
		_, err = client.GameItemSwitchRule.Create().SetID(id).SetItemID(record.ItemID).SetDamagedForceSelfSwitch(r.DamagedForceSelfSwitch).SetDamagedForceAttackerSwitch(r.DamagedForceAttackerSwitch).SetNegativeStatStageForceSelfSwitch(r.NegativeStatStageForceSelfSwitch).SetSwitchRestrictionImmunity(r.SwitchRestrictionImmunity).SetEntryHazardImmunity(r.EntryHazardImmunity).SetVersion(1).SetCreatedAt(record.UpdatedAt).SetUpdatedAt(record.UpdatedAt).Save(ctx)
		if err != nil {
			return fmt.Errorf("保存道具换人规则: %w", err)
		}
	}
	if anyRule(r, "ContactSideEffectImmunity", "ContactDamageToAttackerDenominator", "ContactTransferToAttacker", "PunchBasedContactSuppression", "PunchBasedSkillPowerBoost") {
		id, err := stamp()
		if err != nil {
			return err
		}
		_, err = client.GameItemContactRule.Create().SetID(id).SetItemID(record.ItemID).SetContactSideEffectImmunity(r.ContactSideEffectImmunity).SetContactDamageToAttackerDenominator(r.ContactDamageToAttackerDenominator).SetContactTransferToAttacker(r.ContactTransferToAttacker).SetPunchBasedContactSuppression(r.PunchBasedContactSuppression).SetPunchBasedSkillPowerBoost(r.PunchBasedSkillPowerBoost).SetVersion(1).SetCreatedAt(record.UpdatedAt).SetUpdatedAt(record.UpdatedAt).Save(ctx)
		if err != nil {
			return fmt.Errorf("保存道具接触规则: %w", err)
		}
	}
	if anyRule(r, "EndTurnHealDenominator", "EndTurnDamageDenominator", "EndTurnHealForElementDenominator", "EndTurnHealForElementID", "EndTurnDamageWithoutElementDenominator", "EndTurnDamageWithoutElementID", "DamageDealtHeal") {
		id, err := stamp()
		if err != nil {
			return err
		}
		_, err = client.GameItemRecoveryRule.Create().SetID(id).SetItemID(record.ItemID).SetEndTurnHealDenominator(r.EndTurnHealDenominator).SetEndTurnDamageDenominator(r.EndTurnDamageDenominator).SetEndTurnHealForElementDenominator(r.EndTurnHealForElementDenominator).SetNillableEndTurnHealForElementID(r.EndTurnHealForElementID).SetEndTurnDamageWithoutElementDenominator(r.EndTurnDamageWithoutElementDenominator).SetNillableEndTurnDamageWithoutElementID(r.EndTurnDamageWithoutElementID).SetDamageDealtHeal(r.DamageDealtHeal).SetVersion(1).SetCreatedAt(record.UpdatedAt).SetUpdatedAt(record.UpdatedAt).Save(ctx)
		if err != nil {
			return fmt.Errorf("保存道具恢复规则: %w", err)
		}
	}
	if anyRule(r, "ReflectTurnsRemaining", "LightScreenTurnsRemaining", "AuroraVeilTurnsRemaining", "RainTurnsRemaining", "SandstormTurnsRemaining", "SnowTurnsRemaining", "SunTurnsRemaining", "TerrainTurnsRemaining", "SandstormDamageImmunity") {
		id, err := stamp()
		if err != nil {
			return err
		}
		_, err = client.GameItemWeatherRule.Create().SetID(id).SetItemID(record.ItemID).SetReflectTurnsRemaining(r.ReflectTurnsRemaining).SetLightScreenTurnsRemaining(r.LightScreenTurnsRemaining).SetAuroraVeilTurnsRemaining(r.AuroraVeilTurnsRemaining).SetRainTurnsRemaining(r.RainTurnsRemaining).SetSandstormTurnsRemaining(r.SandstormTurnsRemaining).SetSnowTurnsRemaining(r.SnowTurnsRemaining).SetSunTurnsRemaining(r.SunTurnsRemaining).SetTerrainTurnsRemaining(r.TerrainTurnsRemaining).SetSandstormDamageImmunity(r.SandstormDamageImmunity).SetVersion(1).SetCreatedAt(record.UpdatedAt).SetUpdatedAt(record.UpdatedAt).Save(ctx)
		if err != nil {
			return fmt.Errorf("保存道具天气规则: %w", err)
		}
	}
	if anyRule(r, "MultiHitCountMinimum", "MultiHitCountMaximum", "MultiHitRequiredMinimum", "MultiHitRequiredMaximum") {
		id, err := stamp()
		if err != nil {
			return err
		}
		_, err = client.GameItemMultiHitRule.Create().SetID(id).SetItemID(record.ItemID).SetCountMinimum(r.MultiHitCountMinimum).SetCountMaximum(r.MultiHitCountMaximum).SetRequiredMinimum(r.MultiHitRequiredMinimum).SetRequiredMaximum(r.MultiHitRequiredMaximum).SetVersion(1).SetCreatedAt(record.UpdatedAt).SetUpdatedAt(record.UpdatedAt).Save(ctx)
		if err != nil {
			return fmt.Errorf("保存道具连续命中规则: %w", err)
		}
	}
	if anyRule(r, "WeightHalf", "AirborneUntilDamaged", "ForceGrounded", "TypeImmunitySuppression") {
		id, err := stamp()
		if err != nil {
			return err
		}
		_, err = client.GameItemWeightRule.Create().SetID(id).SetItemID(record.ItemID).SetWeightHalf(r.WeightHalf).SetAirborneUntilDamaged(r.AirborneUntilDamaged).SetForceGrounded(r.ForceGrounded).SetTypeImmunitySuppression(r.TypeImmunitySuppression).SetVersion(1).SetCreatedAt(record.UpdatedAt).SetUpdatedAt(record.UpdatedAt).Save(ctx)
		if err != nil {
			return fmt.Errorf("保存道具体重规则: %w", err)
		}
	}
	return nil
}

func anyRule(r itemrules.Detail, fields ...string) bool {
	v := reflect.ValueOf(r)
	for _, name := range fields {
		if !v.FieldByName(name).IsZero() {
			return true
		}
	}
	return false
}

func itemRuleDetail(row *avalonent.GameItem) itemrules.Detail {
	r := itemrules.Detail{ItemID: row.ID}
	if len(row.Edges.ActionRules) > 0 {
		v := row.Edges.ActionRules[0]
		r.ChargeSkipOnce = v.ChargeSkipOnce
		r.ChoiceSkillLock = v.ChoiceSkillLock
		r.ForcedLastActionOrder = v.ForcedLastActionOrder
		r.LowHPActionOrderBoost = v.LowHpActionOrderBoost
		r.FieldSpeedOrderSpeedStageDrop = v.FieldSpeedOrderSpeedStageDrop
		r.AdditionalFlinchChancePercent = v.AdditionalFlinchChancePercent
		r.RandomActionOrderBoostChancePercent = v.RandomActionOrderBoostChancePercent
		r.AccuracyAfterTargetActedBoost = v.AccuracyAfterTargetActedBoost
		r.SurviveFatalDamageAtFullHP = v.SurviveFatalDamageAtFullHp
		r.BindingTurns = v.BindingTurns
		r.BindingDamageDenominator = v.BindingDamageDenominator
	}
	if len(row.Edges.DamageRules) > 0 {
		v := row.Edges.DamageRules[0]
		r.PhysicalDamagePowerBoost = v.PhysicalDamagePowerBoost
		r.SpecialDamagePowerBoost = v.SpecialDamagePowerBoost
		r.PhysicalDamagePowerBoost50 = v.PhysicalDamagePowerBoost50
		r.SpecialDamagePowerBoost50 = v.SpecialDamagePowerBoost50
		r.SuperEffectiveDamageBoost = v.SuperEffectiveDamageBoost
		r.DamageBoostWithRecoil = v.DamageBoostWithRecoil
		r.DamageDealtHeal = v.DamageDealtHeal
		r.DrainHealingBoost = v.DrainHealingBoost
		r.WeaknessPolicy = v.WeaknessPolicy
		r.ConsecutiveSkillDamageBoost = v.ConsecutiveSkillDamageBoost
		r.ElementDamageBoostElementID = copyIdentifier(v.ElementBoostElementID)
		r.ConsumableElementDamageBoostElementID = copyIdentifier(v.ConsumableBoostElementID)
		r.ConsumableElementDamageBoostNumerator = v.ConsumableBoostNumerator
		r.ConsumableElementDamageBoostDenominator = v.ConsumableBoostDenominator
		r.ElementDamageReductionElementID = copyIdentifier(v.ReductionElementID)
		r.ElementDamageReductionRequiresSuperEffective = v.ReductionRequiresSuperEffective
	}
	applyRemainingItemRules(row, &r)
	for _, v := range row.Edges.StatBoosterAbilities {
		r.HighestStatBoosterAbilityIDs = append(r.HighestStatBoosterAbilityIDs, v.AbilityID)
	}
	return r
}

func applyRemainingItemRules(row *avalonent.GameItem, r *itemrules.Detail) {
	if len(row.Edges.StatRules) > 0 {
		v := row.Edges.StatRules[0]
		r.SpecialDefenseBoost = v.SpecialDefenseBoost
		r.SpeedHalf = v.SpeedHalf
		r.SpeedBoost50 = v.SpeedBoost50
		r.AccuracyBoost = v.AccuracyBoost
		r.OpponentAccuracyReduction = v.OpponentAccuracyReduction
		r.CriticalHitStageBoost = v.CriticalHitStageBoost
		r.OpponentStatStageReductionImmunity = v.OpponentStatStageReductionImmunity
		r.NegativeStatStageReset = v.NegativeStatStageReset
		r.AbilityStatReductionSpeedBoost = v.AbilityStatReductionSpeedBoost
		r.OpponentPositiveStatStageCopy = v.OpponentPositiveStatStageCopy
		if v.AccuracyMissStatStageBoostStat != nil {
			r.AccuracyMissStatStageBoostStat = battleStat(*v.AccuracyMissStatStageBoostStat)
		}
		r.AccuracyMissStatStageBoostDelta = v.AccuracyMissStatStageBoostDelta
		r.WaterDamageSpecialAttackBoostElementID = copyIdentifier(v.WaterSpaElementID)
		r.ElectricDamageAttackBoostElementID = copyIdentifier(v.ElectricAtkElementID)
		r.WaterDamageSpecialDefenseBoostElementID = copyIdentifier(v.WaterSpdElementID)
		r.IceDamageAttackBoostElementID = copyIdentifier(v.IceAtkElementID)
	}
	if len(row.Edges.StatusRules) > 0 {
		v := row.Edges.StatusRules[0]
		r.CuresParalysis = v.CuresParalysis
		r.CuresSleep = v.CuresSleep
		r.CuresPoison = v.CuresPoison
		r.CuresBurn = v.CuresBurn
		r.CuresFreeze = v.CuresFreeze
		r.CuresAllMajorStatuses = v.CuresAllMajorStatuses
		r.CuresConfusion = v.CuresConfusion
		r.PowderSkillImmunity = v.PowderSkillImmunity
		r.StatusSkillRestriction = v.StatusSkillRestriction
		r.DamagingSkillSecondaryEffectImmunity = v.DamagingSkillSecondaryEffectImmunity
	}
	if len(row.Edges.SwitchRules) > 0 {
		v := row.Edges.SwitchRules[0]
		r.DamagedForceSelfSwitch = v.DamagedForceSelfSwitch
		r.DamagedForceAttackerSwitch = v.DamagedForceAttackerSwitch
		r.NegativeStatStageForceSelfSwitch = v.NegativeStatStageForceSelfSwitch
		r.SwitchRestrictionImmunity = v.SwitchRestrictionImmunity
		r.EntryHazardImmunity = v.EntryHazardImmunity
	}
	if len(row.Edges.ContactRules) > 0 {
		v := row.Edges.ContactRules[0]
		r.ContactSideEffectImmunity = v.ContactSideEffectImmunity
		r.ContactDamageToAttackerDenominator = v.ContactDamageToAttackerDenominator
		r.ContactTransferToAttacker = v.ContactTransferToAttacker
		r.PunchBasedContactSuppression = v.PunchBasedContactSuppression
		r.PunchBasedSkillPowerBoost = v.PunchBasedSkillPowerBoost
	}
	if len(row.Edges.RecoveryRules) > 0 {
		v := row.Edges.RecoveryRules[0]
		r.EndTurnHealDenominator = v.EndTurnHealDenominator
		r.EndTurnDamageDenominator = v.EndTurnDamageDenominator
		r.EndTurnHealForElementDenominator = v.EndTurnHealForElementDenominator
		r.EndTurnHealForElementID = copyIdentifier(v.EndTurnHealForElementID)
		r.EndTurnDamageWithoutElementDenominator = v.EndTurnDamageWithoutElementDenominator
		r.EndTurnDamageWithoutElementID = copyIdentifier(v.EndTurnDamageWithoutElementID)
		r.DamageDealtHeal = v.DamageDealtHeal
	}
	if len(row.Edges.WeatherRules) > 0 {
		v := row.Edges.WeatherRules[0]
		r.ReflectTurnsRemaining = v.ReflectTurnsRemaining
		r.LightScreenTurnsRemaining = v.LightScreenTurnsRemaining
		r.AuroraVeilTurnsRemaining = v.AuroraVeilTurnsRemaining
		r.RainTurnsRemaining = v.RainTurnsRemaining
		r.SandstormTurnsRemaining = v.SandstormTurnsRemaining
		r.SnowTurnsRemaining = v.SnowTurnsRemaining
		r.SunTurnsRemaining = v.SunTurnsRemaining
		r.TerrainTurnsRemaining = v.TerrainTurnsRemaining
		r.SandstormDamageImmunity = v.SandstormDamageImmunity
	}
	if len(row.Edges.MultiHitRules) > 0 {
		v := row.Edges.MultiHitRules[0]
		r.MultiHitCountMinimum = v.CountMinimum
		r.MultiHitCountMaximum = v.CountMaximum
		r.MultiHitRequiredMinimum = v.RequiredMinimum
		r.MultiHitRequiredMaximum = v.RequiredMaximum
	}
	if len(row.Edges.WeightRules) > 0 {
		v := row.Edges.WeightRules[0]
		r.WeightHalf = v.WeightHalf
		r.AirborneUntilDamaged = v.AirborneUntilDamaged
		r.ForceGrounded = v.ForceGrounded
		r.TypeImmunitySuppression = v.TypeImmunitySuppression
	}
}
