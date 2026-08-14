package persistence

import (
	"context"
	"fmt"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/itemrules"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GetItemRules 从规范化道具表构建 Battle 只读投影。
//
// 道具规则不再通过单例 JSON 文档读取；基础文案由 game_item 承载，规则族由各自关系表扩展。
func (s *Adapters) GetItemRules(ctx context.Context) (itemrules.Projection, error) {
	rows, err := s.pool.Client(ctx).GameItem.Query().All(ctx)
	if err != nil {
		return itemrules.Projection{}, fmt.Errorf("读取道具规则投影: %w", err)
	}
	client := s.pool.Client(ctx)
	actionRules, err := client.GameItemActionRule.Query().All(ctx)
	if err != nil {
		return itemrules.Projection{}, fmt.Errorf("读取道具行动规则投影: %w", err)
	}
	damageRules, err := client.GameItemDamageRule.Query().All(ctx)
	if err != nil {
		return itemrules.Projection{}, fmt.Errorf("读取道具伤害规则投影: %w", err)
	}
	statBoosterAbilities, err := client.GameItemStatBoosterAbility.Query().All(ctx)
	if err != nil {
		return itemrules.Projection{}, fmt.Errorf("读取道具最高能力强化 Ability 投影: %w", err)
	}
	statRules, err := client.GameItemStatRule.Query().All(ctx)
	if err != nil {
		return itemrules.Projection{}, fmt.Errorf("读取道具能力规则投影: %w", err)
	}
	statusRules, err := client.GameItemStatusRule.Query().All(ctx)
	if err != nil {
		return itemrules.Projection{}, fmt.Errorf("读取道具状态规则投影: %w", err)
	}
	switchRules, err := client.GameItemSwitchRule.Query().All(ctx)
	if err != nil {
		return itemrules.Projection{}, fmt.Errorf("读取道具换人规则投影: %w", err)
	}
	contactRules, err := client.GameItemContactRule.Query().All(ctx)
	if err != nil {
		return itemrules.Projection{}, fmt.Errorf("读取道具接触规则投影: %w", err)
	}
	recoveryRules, err := client.GameItemRecoveryRule.Query().All(ctx)
	if err != nil {
		return itemrules.Projection{}, fmt.Errorf("读取道具恢复规则投影: %w", err)
	}
	weatherRules, err := client.GameItemWeatherRule.Query().All(ctx)
	if err != nil {
		return itemrules.Projection{}, fmt.Errorf("读取道具天气规则投影: %w", err)
	}
	multiHitRules, err := client.GameItemMultiHitRule.Query().All(ctx)
	if err != nil {
		return itemrules.Projection{}, fmt.Errorf("读取道具连续命中规则投影: %w", err)
	}
	weightRules, err := client.GameItemWeightRule.Query().All(ctx)
	if err != nil {
		return itemrules.Projection{}, fmt.Errorf("读取道具重量规则投影: %w", err)
	}
	actionByItem := make(map[snowflake.ID]*avalonent.GameItemActionRule, len(actionRules))
	for _, rule := range actionRules {
		actionByItem[rule.ItemID] = rule
	}
	damageByItem := make(map[snowflake.ID]*avalonent.GameItemDamageRule, len(damageRules))
	for _, rule := range damageRules {
		damageByItem[rule.ItemID] = rule
	}
	abilitiesByItem := make(map[snowflake.ID][]snowflake.ID, len(statBoosterAbilities))
	for _, relation := range statBoosterAbilities {
		abilitiesByItem[relation.ItemID] = append(abilitiesByItem[relation.ItemID], relation.AbilityID)
	}
	statByItem := make(map[snowflake.ID]*avalonent.GameItemStatRule, len(statRules))
	for _, rule := range statRules {
		statByItem[rule.ItemID] = rule
	}
	statusByItem := make(map[snowflake.ID]*avalonent.GameItemStatusRule, len(statusRules))
	for _, rule := range statusRules {
		statusByItem[rule.ItemID] = rule
	}
	switchByItem := make(map[snowflake.ID]*avalonent.GameItemSwitchRule, len(switchRules))
	for _, rule := range switchRules {
		switchByItem[rule.ItemID] = rule
	}
	contactByItem := make(map[snowflake.ID]*avalonent.GameItemContactRule, len(contactRules))
	for _, rule := range contactRules {
		contactByItem[rule.ItemID] = rule
	}
	recoveryByItem := make(map[snowflake.ID]*avalonent.GameItemRecoveryRule, len(recoveryRules))
	for _, rule := range recoveryRules {
		recoveryByItem[rule.ItemID] = rule
	}
	weatherByItem := make(map[snowflake.ID]*avalonent.GameItemWeatherRule, len(weatherRules))
	for _, rule := range weatherRules {
		weatherByItem[rule.ItemID] = rule
	}
	multiHitByItem := make(map[snowflake.ID]*avalonent.GameItemMultiHitRule, len(multiHitRules))
	for _, rule := range multiHitRules {
		multiHitByItem[rule.ItemID] = rule
	}
	weightByItem := make(map[snowflake.ID]*avalonent.GameItemWeightRule, len(weightRules))
	for _, rule := range weightRules {
		weightByItem[rule.ItemID] = rule
	}
	data := itemrules.Projection{Details: make([]itemrules.Detail, 0, len(rows))}
	for _, row := range rows {
		if !row.Enabled {
			continue
		}
		detail := itemrules.Detail{ItemID: row.ID}
		if row.Description != nil {
			value := *row.Description
			detail.Description = &value
		}
		if row.Effect != nil {
			value := *row.Effect
			detail.Effect = &value
		}
		if row.ShortEffect != nil {
			value := *row.ShortEffect
			detail.ShortEffect = &value
		}
		if row.FlingEffectID != nil {
			value := *row.FlingEffectID
			detail.FlingEffectID = &value
		}
		if rule := actionByItem[row.ID]; rule != nil {
			detail.ChargeSkipOnce = rule.ChargeSkipOnce
			detail.ChoiceSkillLock = rule.ChoiceSkillLock
			detail.ForcedLastActionOrder = rule.ForcedLastActionOrder
			detail.LowHPActionOrderBoost = rule.LowHpActionOrderBoost
			detail.FieldSpeedOrderSpeedStageDrop = rule.FieldSpeedOrderSpeedStageDrop
			detail.AdditionalFlinchChancePercent = rule.AdditionalFlinchChancePercent
			detail.RandomActionOrderBoostChancePercent = rule.RandomActionOrderBoostChancePercent
			detail.AccuracyAfterTargetActedBoost = rule.AccuracyAfterTargetActedBoost
			detail.SurviveFatalDamageAtFullHP = rule.SurviveFatalDamageAtFullHp
			detail.BindingTurns = rule.BindingTurns
			detail.BindingDamageDenominator = rule.BindingDamageDenominator
		}
		if rule := damageByItem[row.ID]; rule != nil {
			detail.ElementDamageBoostElementID = copyIdentifier(rule.ElementBoostElementID)
			detail.ConsumableElementDamageBoostElementID = copyIdentifier(rule.ConsumableBoostElementID)
			detail.ConsumableElementDamageBoostNumerator = rule.ConsumableBoostNumerator
			detail.ConsumableElementDamageBoostDenominator = rule.ConsumableBoostDenominator
			detail.ElementDamageReductionElementID = copyIdentifier(rule.ReductionElementID)
			detail.ElementDamageReductionRequiresSuperEffective = rule.ReductionRequiresSuperEffective
			detail.PhysicalDamagePowerBoost = rule.PhysicalDamagePowerBoost
			detail.SpecialDamagePowerBoost = rule.SpecialDamagePowerBoost
			detail.PhysicalDamagePowerBoost50 = rule.PhysicalDamagePowerBoost50
			detail.SpecialDamagePowerBoost50 = rule.SpecialDamagePowerBoost50
			detail.SuperEffectiveDamageBoost = rule.SuperEffectiveDamageBoost
			detail.DamageBoostWithRecoil = rule.DamageBoostWithRecoil
			detail.DamageDealtHeal = rule.DamageDealtHeal
			detail.DrainHealingBoost = rule.DrainHealingBoost
			detail.WeaknessPolicy = rule.WeaknessPolicy
			detail.ConsecutiveSkillDamageBoost = rule.ConsecutiveSkillDamageBoost
		}
		if abilityIDs := abilitiesByItem[row.ID]; len(abilityIDs) > 0 {
			detail.HighestStatBoosterAbilityIDs = append([]snowflake.ID(nil), abilityIDs...)
		}
		if rule := statByItem[row.ID]; rule != nil {
			detail.SpecialDefenseBoost = rule.SpecialDefenseBoost
			detail.SpeedHalf = rule.SpeedHalf
			detail.SpeedBoost50 = rule.SpeedBoost50
			detail.AccuracyBoost = rule.AccuracyBoost
			detail.OpponentAccuracyReduction = rule.OpponentAccuracyReduction
			detail.CriticalHitStageBoost = rule.CriticalHitStageBoost
			detail.OpponentStatStageReductionImmunity = rule.OpponentStatStageReductionImmunity
			detail.NegativeStatStageReset = rule.NegativeStatStageReset
			detail.AbilityStatReductionSpeedBoost = rule.AbilityStatReductionSpeedBoost
			detail.OpponentPositiveStatStageCopy = rule.OpponentPositiveStatStageCopy
			if rule.AccuracyMissStatStageBoostStat != nil {
				detail.AccuracyMissStatStageBoostStat = battleStat(*rule.AccuracyMissStatStageBoostStat)
			}
			detail.AccuracyMissStatStageBoostDelta = rule.AccuracyMissStatStageBoostDelta
			detail.WaterDamageSpecialAttackBoostElementID = copyIdentifier(rule.WaterSpaElementID)
			detail.ElectricDamageAttackBoostElementID = copyIdentifier(rule.ElectricAtkElementID)
			detail.WaterDamageSpecialDefenseBoostElementID = copyIdentifier(rule.WaterSpdElementID)
			detail.IceDamageAttackBoostElementID = copyIdentifier(rule.IceAtkElementID)
		}
		if rule := statusByItem[row.ID]; rule != nil {
			detail.CuresParalysis = rule.CuresParalysis
			detail.CuresSleep = rule.CuresSleep
			detail.CuresPoison = rule.CuresPoison
			detail.CuresBurn = rule.CuresBurn
			detail.CuresFreeze = rule.CuresFreeze
			detail.CuresAllMajorStatuses = rule.CuresAllMajorStatuses
			detail.CuresConfusion = rule.CuresConfusion
			detail.PowderSkillImmunity = rule.PowderSkillImmunity
			detail.StatusSkillRestriction = rule.StatusSkillRestriction
			detail.DamagingSkillSecondaryEffectImmunity = rule.DamagingSkillSecondaryEffectImmunity
		}
		if rule := switchByItem[row.ID]; rule != nil {
			detail.DamagedForceSelfSwitch = rule.DamagedForceSelfSwitch
			detail.DamagedForceAttackerSwitch = rule.DamagedForceAttackerSwitch
			detail.NegativeStatStageForceSelfSwitch = rule.NegativeStatStageForceSelfSwitch
			detail.SwitchRestrictionImmunity = rule.SwitchRestrictionImmunity
			detail.EntryHazardImmunity = rule.EntryHazardImmunity
		}
		if rule := contactByItem[row.ID]; rule != nil {
			detail.ContactSideEffectImmunity = rule.ContactSideEffectImmunity
			detail.ContactDamageToAttackerDenominator = rule.ContactDamageToAttackerDenominator
			detail.ContactTransferToAttacker = rule.ContactTransferToAttacker
			detail.PunchBasedContactSuppression = rule.PunchBasedContactSuppression
			detail.PunchBasedSkillPowerBoost = rule.PunchBasedSkillPowerBoost
		}
		if rule := recoveryByItem[row.ID]; rule != nil {
			detail.EndTurnHealDenominator = rule.EndTurnHealDenominator
			detail.EndTurnDamageDenominator = rule.EndTurnDamageDenominator
			detail.EndTurnHealForElementDenominator = rule.EndTurnHealForElementDenominator
			detail.EndTurnHealForElementID = copyIdentifier(rule.EndTurnHealForElementID)
			detail.EndTurnDamageWithoutElementDenominator = rule.EndTurnDamageWithoutElementDenominator
			detail.EndTurnDamageWithoutElementID = copyIdentifier(rule.EndTurnDamageWithoutElementID)
			detail.DamageDealtHeal = rule.DamageDealtHeal
		}
		if rule := weatherByItem[row.ID]; rule != nil {
			detail.ReflectTurnsRemaining = rule.ReflectTurnsRemaining
			detail.LightScreenTurnsRemaining = rule.LightScreenTurnsRemaining
			detail.AuroraVeilTurnsRemaining = rule.AuroraVeilTurnsRemaining
			detail.RainTurnsRemaining = rule.RainTurnsRemaining
			detail.SandstormTurnsRemaining = rule.SandstormTurnsRemaining
			detail.SnowTurnsRemaining = rule.SnowTurnsRemaining
			detail.SunTurnsRemaining = rule.SunTurnsRemaining
			detail.TerrainTurnsRemaining = rule.TerrainTurnsRemaining
			detail.SandstormDamageImmunity = rule.SandstormDamageImmunity
		}
		if rule := multiHitByItem[row.ID]; rule != nil {
			detail.MultiHitCountMinimum = rule.CountMinimum
			detail.MultiHitCountMaximum = rule.CountMaximum
			detail.MultiHitRequiredMinimum = rule.RequiredMinimum
			detail.MultiHitRequiredMaximum = rule.RequiredMaximum
		}
		if rule := weightByItem[row.ID]; rule != nil {
			detail.WeightHalf = rule.WeightHalf
			detail.AirborneUntilDamaged = rule.AirborneUntilDamaged
			detail.ForceGrounded = rule.ForceGrounded
			detail.TypeImmunitySuppression = rule.TypeImmunitySuppression
		}
		data.Details = append(data.Details, detail)
	}
	return data, nil
}

func copyIdentifier(value *snowflake.ID) *snowflake.ID {
	if value == nil {
		return nil
	}
	result := *value
	return &result
}

func battleStat(value string) battleengine.Stat {
	return battleengine.Stat(value)
}
