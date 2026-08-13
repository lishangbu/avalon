package battleengine

// applyContactItemTransferToAttacker 在目标因有效接触技能受到真实本体伤害后，向尚未携带道具的攻击者转移目标道具。
//
// bodyDamage 必须来自同一段 DamageAppliedEvent，因此替身、保护、未命中和零伤害不会触发转移。攻击者的接触
// 副作用免疫会阻止整个接触后置链；无视目标特性不影响道具规则。转移发生在接触反伤前，使已被移走的道具不会
// 在同一段伤害中继续作为目标侧反伤来源。
func applyContactItemTransferToAttacker(
	state State,
	attackerRef MemberRef,
	defenderRef MemberRef,
	skill SkillSnapshot,
	bodyDamage uint32,
) (State, []Event) {
	if bodyDamage == 0 || attackerRef.Side == defenderRef.Side {
		return state, nil
	}
	attacker, attackerExists := state.member(attackerRef.Side, attackerRef.Position)
	defender, defenderExists := state.member(defenderRef.Side, defenderRef.Position)
	if !attackerExists || !defenderExists || attacker.CurrentHP == 0 || attacker.ItemID != 0 ||
		!skillMakesEffectiveContact(attacker, skill) || attacker.ContactSideEffectImmunity ||
		defender.ItemID == 0 || !defender.ContactTransferToAttacker {
		return state, nil
	}

	itemID := defender.ItemID
	itemSource := defender
	defender = clearHeldItemRuntimeState(defender)
	attacker = receiveTransferredHeldItem(attacker, itemID, itemSource)
	state.replaceMember(defenderRef.Side, defender)
	state.replaceMember(attackerRef.Side, attacker)
	return state, []Event{HeldItemTransferredEvent{
		Type: EventKindHeldItemTransferred, SchemaVersion: 1, TurnNumber: state.turnNumber,
		From: defenderRef, To: attackerRef, ItemID: itemID, SkillID: skill.SkillID,
	}}
}

// clearHeldItemRuntimeState 清除已经消费或交出的道具运行时投影，并恢复可能由该道具覆盖的自然属性。
func clearHeldItemRuntimeState(member MemberSnapshot) MemberSnapshot {
	member = restoreHeldItemElementIdentity(member)
	member.ItemID = 0
	member.HighestStatBoosterAbilityIDs = nil
	member.DamagedForceSelfSwitch = false
	member.DamagedForceAttackerSwitch = false
	member.NegativeStatStageForceSelfSwitch = false
	member.SwitchRestrictionImmunity = false
	member.ContactSideEffectImmunity = false
	member.HeldItemContactDamageToAttackerDenominator = 0
	member.HeldItemEndTurnHealDenominator = 0
	member.HeldItemEndTurnHealForElementID = 0
	member.HeldItemEndTurnHealForElementDenominator = 0
	member.HeldItemEndTurnDamageDenominator = 0
	member.HeldItemEndTurnDamageWithoutElementID = 0
	member.HeldItemEndTurnDamageWithoutElementDenominator = 0
	member.HeldItemConsumableElementDamageBoostElementID = 0
	member.HeldItemConsumableElementDamageBoostNumerator = 0
	member.HeldItemConsumableElementDamageBoostDenominator = 0
	member.ContactTransferToAttacker = false
	member.ChargeSkipOnce = false
	member.HeldItemSurviveFatalDamageAtFullHP = false
	member.HeldItemReflectTurnsRemaining = 0
	member.HeldItemLightScreenTurnsRemaining = 0
	member.HeldItemAuroraVeilTurnsRemaining = 0
	member.HeldItemRainTurnsRemaining = 0
	member.HeldItemSandstormTurnsRemaining = 0
	member.HeldItemSnowTurnsRemaining = 0
	member.HeldItemSunTurnsRemaining = 0
	member.HeldItemTerrainTurnsRemaining = 0
	member.HeldItemSandstormDamageImmunity = false
	member.HeldItemEntryHazardImmunity = false
	member.HeldItemWeightHalf = false
	member.HeldItemCuresParalysis = false
	member.HeldItemCuresSleep = false
	member.HeldItemCuresPoison = false
	member.HeldItemCuresBurn = false
	member.HeldItemCuresFreeze = false
	member.HeldItemCuresAllMajorStatuses = false
	member.HeldItemCuresConfusion = false
	member.HeldItemPunchBasedSkillPowerBoost = false
	member.HeldItemPhysicalDamagePowerBoost = false
	member.HeldItemSpecialDamagePowerBoost = false
	member.HeldItemElementDamageReductionElementID = 0
	member.HeldItemElementDamageReductionRequiresSuperEffective = false
	member.HeldItemSuperEffectiveDamageBoost = false
	member.HeldItemDamageBoostWithRecoil = false
	member.HeldItemDamageDealtHeal = false
	member.HeldItemDrainHealingBoost = false
	member.HeldItemAccuracyBoost = false
	member.HeldItemOpponentAccuracyReduction = false
	member.HeldItemCriticalHitStageBoost = false
	member.HeldItemAirborneUntilDamaged = false
	member.HeldItemForceGrounded = false
	member.HeldItemSpeedHalf = false
	member.HeldItemSpecialDefenseBoost = false
	member.HeldItemStatusSkillRestriction = false
	member.HeldItemPhysicalDamagePowerBoost50 = false
	member.HeldItemSpecialDamagePowerBoost50 = false
	member.HeldItemChoiceSkillLock = false
	member.HeldItemChoiceLockedSkillPosition = 0
	member.HeldItemSpeedBoost50 = false
	member.HeldItemAccuracyAfterTargetActedBoost = false
	member.HeldItemTypeImmunitySuppression = false
	member.HeldItemOpponentStatStageReductionImmunity = false
	member.HeldItemNegativeStatStageReset = false
	member.HeldItemAbilityStatReductionSpeedBoost = false
	member.HeldItemOpponentPositiveStatStageCopy = false
	member.HeldItemDamagingSkillSecondaryEffectImmunity = false
	member.HeldItemBindingTurns = 0
	member.HeldItemBindingDamageDenominator = 0
	member.HeldItemAccuracyMissStatStageBoostStat = ""
	member.HeldItemAccuracyMissStatStageBoostDelta = 0
	member.HeldItemWeaknessPolicy = false
	member.HeldItemWaterDamageSpecialAttackBoostElementID = 0
	member.HeldItemElectricDamageAttackBoostElementID = 0
	member.HeldItemWaterDamageSpecialDefenseBoostElementID = 0
	member.HeldItemIceDamageAttackBoostElementID = 0
	member.HeldItemAdditionalFlinchChancePercent = 0
	member.HeldItemRandomActionOrderBoostChancePercent = 0
	member.HeldItemForcedLastActionOrder = false
	member.HeldItemLowHPActionOrderBoost = false
	member.HeldItemFieldSpeedOrderSpeedStageDrop = false
	member.HeldItemConsecutiveSkillDamageBoost = false
	member.HeldItemPunchBasedContactSuppression = false
	member.HeldItemPowderSkillImmunity = false
	member.HeldItemMultiHitCountMinimum = 0
	member.HeldItemMultiHitCountMaximum = 0
	member.HeldItemMultiHitRequiredMinimum = 0
	member.HeldItemMultiHitRequiredMaximum = 0
	member.HeldItemElementID = 0
	return member
}

// receiveTransferredHeldItem 将失主的道具运行时投影复制给攻击者，但不迁移任何只属于失主连续上场周期的状态。
//
// 例如 HeldItemElementIdentityBaseElementIDs 是失主被道具改写属性前的私有恢复基线，绝不能作为道具效果转给
// 攻击者；BoosterEnergyStat 同样是已消费道具留下的成员持续状态，不属于当前可转移道具。
func receiveTransferredHeldItem(member MemberSnapshot, itemID Identifier, source MemberSnapshot) MemberSnapshot {
	member.ItemID = itemID
	member.HighestStatBoosterAbilityIDs = append([]Identifier(nil), source.HighestStatBoosterAbilityIDs...)
	member.DamagedForceSelfSwitch = source.DamagedForceSelfSwitch
	member.DamagedForceAttackerSwitch = source.DamagedForceAttackerSwitch
	member.NegativeStatStageForceSelfSwitch = source.NegativeStatStageForceSelfSwitch
	member.SwitchRestrictionImmunity = source.SwitchRestrictionImmunity
	member.ContactSideEffectImmunity = source.ContactSideEffectImmunity
	member.HeldItemContactDamageToAttackerDenominator = source.HeldItemContactDamageToAttackerDenominator
	member.HeldItemEndTurnHealDenominator = source.HeldItemEndTurnHealDenominator
	member.HeldItemEndTurnHealForElementID = source.HeldItemEndTurnHealForElementID
	member.HeldItemEndTurnHealForElementDenominator = source.HeldItemEndTurnHealForElementDenominator
	member.HeldItemEndTurnDamageDenominator = source.HeldItemEndTurnDamageDenominator
	member.HeldItemEndTurnDamageWithoutElementID = source.HeldItemEndTurnDamageWithoutElementID
	member.HeldItemEndTurnDamageWithoutElementDenominator = source.HeldItemEndTurnDamageWithoutElementDenominator
	member.HeldItemConsumableElementDamageBoostElementID = source.HeldItemConsumableElementDamageBoostElementID
	member.HeldItemConsumableElementDamageBoostNumerator = source.HeldItemConsumableElementDamageBoostNumerator
	member.HeldItemConsumableElementDamageBoostDenominator = source.HeldItemConsumableElementDamageBoostDenominator
	member.ContactTransferToAttacker = source.ContactTransferToAttacker
	member.ChargeSkipOnce = source.ChargeSkipOnce
	member.HeldItemSurviveFatalDamageAtFullHP = source.HeldItemSurviveFatalDamageAtFullHP
	member.HeldItemReflectTurnsRemaining = source.HeldItemReflectTurnsRemaining
	member.HeldItemLightScreenTurnsRemaining = source.HeldItemLightScreenTurnsRemaining
	member.HeldItemAuroraVeilTurnsRemaining = source.HeldItemAuroraVeilTurnsRemaining
	member.HeldItemRainTurnsRemaining = source.HeldItemRainTurnsRemaining
	member.HeldItemSandstormTurnsRemaining = source.HeldItemSandstormTurnsRemaining
	member.HeldItemSnowTurnsRemaining = source.HeldItemSnowTurnsRemaining
	member.HeldItemSunTurnsRemaining = source.HeldItemSunTurnsRemaining
	member.HeldItemTerrainTurnsRemaining = source.HeldItemTerrainTurnsRemaining
	member.HeldItemSandstormDamageImmunity = source.HeldItemSandstormDamageImmunity
	member.HeldItemEntryHazardImmunity = source.HeldItemEntryHazardImmunity
	member.HeldItemWeightHalf = source.HeldItemWeightHalf
	member.HeldItemCuresParalysis = source.HeldItemCuresParalysis
	member.HeldItemCuresSleep = source.HeldItemCuresSleep
	member.HeldItemCuresPoison = source.HeldItemCuresPoison
	member.HeldItemCuresBurn = source.HeldItemCuresBurn
	member.HeldItemCuresFreeze = source.HeldItemCuresFreeze
	member.HeldItemCuresAllMajorStatuses = source.HeldItemCuresAllMajorStatuses
	member.HeldItemCuresConfusion = source.HeldItemCuresConfusion
	member.HeldItemPunchBasedSkillPowerBoost = source.HeldItemPunchBasedSkillPowerBoost
	member.HeldItemPhysicalDamagePowerBoost = source.HeldItemPhysicalDamagePowerBoost
	member.HeldItemSpecialDamagePowerBoost = source.HeldItemSpecialDamagePowerBoost
	member.HeldItemElementDamageReductionElementID = source.HeldItemElementDamageReductionElementID
	member.HeldItemElementDamageReductionRequiresSuperEffective = source.HeldItemElementDamageReductionRequiresSuperEffective
	member.HeldItemSuperEffectiveDamageBoost = source.HeldItemSuperEffectiveDamageBoost
	member.HeldItemDamageBoostWithRecoil = source.HeldItemDamageBoostWithRecoil
	member.HeldItemDamageDealtHeal = source.HeldItemDamageDealtHeal
	member.HeldItemDrainHealingBoost = source.HeldItemDrainHealingBoost
	member.HeldItemAccuracyBoost = source.HeldItemAccuracyBoost
	member.HeldItemOpponentAccuracyReduction = source.HeldItemOpponentAccuracyReduction
	member.HeldItemCriticalHitStageBoost = source.HeldItemCriticalHitStageBoost
	member.HeldItemAirborneUntilDamaged = source.HeldItemAirborneUntilDamaged
	member.HeldItemForceGrounded = source.HeldItemForceGrounded
	member.HeldItemSpeedHalf = source.HeldItemSpeedHalf
	member.HeldItemSpecialDefenseBoost = source.HeldItemSpecialDefenseBoost
	member.HeldItemStatusSkillRestriction = source.HeldItemStatusSkillRestriction
	member.HeldItemPhysicalDamagePowerBoost50 = source.HeldItemPhysicalDamagePowerBoost50
	member.HeldItemSpecialDamagePowerBoost50 = source.HeldItemSpecialDamagePowerBoost50
	member.HeldItemChoiceSkillLock = source.HeldItemChoiceSkillLock
	member.HeldItemChoiceLockedSkillPosition = 0
	member.HeldItemSpeedBoost50 = source.HeldItemSpeedBoost50
	member.HeldItemAccuracyAfterTargetActedBoost = source.HeldItemAccuracyAfterTargetActedBoost
	member.HeldItemTypeImmunitySuppression = source.HeldItemTypeImmunitySuppression
	member.HeldItemOpponentStatStageReductionImmunity = source.HeldItemOpponentStatStageReductionImmunity
	member.HeldItemNegativeStatStageReset = source.HeldItemNegativeStatStageReset
	member.HeldItemAbilityStatReductionSpeedBoost = source.HeldItemAbilityStatReductionSpeedBoost
	member.HeldItemOpponentPositiveStatStageCopy = source.HeldItemOpponentPositiveStatStageCopy
	member.HeldItemDamagingSkillSecondaryEffectImmunity = source.HeldItemDamagingSkillSecondaryEffectImmunity
	member.HeldItemBindingTurns = source.HeldItemBindingTurns
	member.HeldItemBindingDamageDenominator = source.HeldItemBindingDamageDenominator
	member.HeldItemAccuracyMissStatStageBoostStat = source.HeldItemAccuracyMissStatStageBoostStat
	member.HeldItemAccuracyMissStatStageBoostDelta = source.HeldItemAccuracyMissStatStageBoostDelta
	member.HeldItemWeaknessPolicy = source.HeldItemWeaknessPolicy
	member.HeldItemWaterDamageSpecialAttackBoostElementID = source.HeldItemWaterDamageSpecialAttackBoostElementID
	member.HeldItemElectricDamageAttackBoostElementID = source.HeldItemElectricDamageAttackBoostElementID
	member.HeldItemWaterDamageSpecialDefenseBoostElementID = source.HeldItemWaterDamageSpecialDefenseBoostElementID
	member.HeldItemIceDamageAttackBoostElementID = source.HeldItemIceDamageAttackBoostElementID
	member.HeldItemAdditionalFlinchChancePercent = source.HeldItemAdditionalFlinchChancePercent
	member.HeldItemRandomActionOrderBoostChancePercent = source.HeldItemRandomActionOrderBoostChancePercent
	member.HeldItemForcedLastActionOrder = source.HeldItemForcedLastActionOrder
	member.HeldItemLowHPActionOrderBoost = source.HeldItemLowHPActionOrderBoost
	member.HeldItemFieldSpeedOrderSpeedStageDrop = source.HeldItemFieldSpeedOrderSpeedStageDrop
	member.HeldItemConsecutiveSkillDamageBoost = source.HeldItemConsecutiveSkillDamageBoost
	member.HeldItemPunchBasedContactSuppression = source.HeldItemPunchBasedContactSuppression
	member.HeldItemPowderSkillImmunity = source.HeldItemPowderSkillImmunity
	member.HeldItemMultiHitCountMinimum = source.HeldItemMultiHitCountMinimum
	member.HeldItemMultiHitCountMaximum = source.HeldItemMultiHitCountMaximum
	member.HeldItemMultiHitRequiredMinimum = source.HeldItemMultiHitRequiredMinimum
	member.HeldItemMultiHitRequiredMaximum = source.HeldItemMultiHitRequiredMaximum
	member.HeldItemElementID = source.HeldItemElementID
	return member
}
