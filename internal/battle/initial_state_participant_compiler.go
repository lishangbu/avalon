package battle

import "math"
import "strings"
import "github.com/lishangbu/avalon/internal/platform/snowflake"
import "github.com/lishangbu/avalon/internal/battleengine"
import "github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
import "github.com/lishangbu/avalon/internal/gamedata/battleformat"
import "github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
import "github.com/lishangbu/avalon/internal/gamedata/nature"
import "github.com/lishangbu/avalon/internal/gamedata/stat"
import "github.com/lishangbu/avalon/internal/team"

func (compiler *initialMemberCompiler) compileTeam(snapshot TeamSnapshot) ([]BattleMemberFacts, error) {
	if len(snapshot.Members) == 0 || len(snapshot.Members) > battleengine.MaximumMembersPerSide {
		return nil, ErrInitialStateCompilation
	}
	result := make([]BattleMemberFacts, 0, len(snapshot.Members))
	for _, member := range snapshot.Members {
		compiled, err := compiler.compileMember(member)
		if err != nil {
			return nil, err
		}
		result = append(result, compiled)
	}
	return result, nil
}

func (compiler *initialMemberCompiler) compileMember(member team.Member) (BattleMemberFacts, error) {
	if member.Position < 1 || member.Position > battleengine.MaximumMembersPerSide || member.CreatureID == snowflake.ID(0) ||
		member.AbilityID == snowflake.ID(0) || member.TeraElementID == snowflake.ID(0) || member.NatureID == snowflake.ID(0) || len(member.Skills) == 0 || len(member.Skills) > battleengine.MaximumSkillsPerMember {
		return BattleMemberFacts{}, ErrInitialStateCompilation
	}
	if _, err := compiler.ability(member.AbilityID); err != nil {
		return BattleMemberFacts{}, err
	}
	genderCode, err := compiler.genderCode(member.GenderID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	weatherDamageImmunities, err := compiler.abilityWeatherDamageImmunities(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	weatherEffectsSuppressed, err := compiler.abilityWeatherEffectsSuppressed(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	reactiveAbilityRules, err := compiler.abilityReactiveAbilityRules(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	accuracyRules, err := compiler.abilityAccuracyRules(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	damageMultiplierRules, err := compiler.abilityDamageMultiplierRules(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	formulaDetail, err := compiler.abilityDetail(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	if formulaDetail == nil {
		formulaDetail = &abilitydetail.RuleSet{}
	}
	if formulaDetail.SkillWeatherOverride != "" {
		switch formulaDetail.SkillWeatherOverride {
		case battleengine.WeatherKindSun, battleengine.WeatherKindRain, battleengine.WeatherKindSandstorm, battleengine.WeatherKindSnow:
		default:
			return BattleMemberFacts{}, ErrInitialStateCompilation
		}
	}
	if battleengine.ValidateSkillElementConversion(formulaDetail.SkillElementConversion) != nil ||
		battleengine.ValidateDamageFraction(formulaDetail.ContactSkillProtectionBypassDamageMultiplier) != nil ||
		formulaDetail.ContactSkillProtectionBypassDamageMultiplier != nil && !formulaDetail.ContactSkillProtectionBypass {
		return BattleMemberFacts{}, ErrInitialStateCompilation
	}
	if !formulaDetail.ValidForBattle() {
		return BattleMemberFacts{}, ErrInitialStateCompilation
	}
	forcedSwitchImmunity, err := compiler.abilityForcedSwitchImmunity(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	opponentSwitchRestriction, err := compiler.abilityOpponentSwitchRestriction(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	damageCrossedHalfHPForceSelfSwitch, err := compiler.abilityDamageCrossedHalfHPForceSelfSwitch(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchOutMajorStatusCure, err := compiler.abilitySwitchOutMajorStatusCure(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchOutHealDenominator, err := compiler.abilitySwitchOutHealDenominator(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	weatherEndTurnHealing, err := compiler.abilityWeatherEndTurnHealing(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	weatherSpeedMultipliers, err := compiler.abilityWeatherSpeedMultipliers(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	environmentHighestStatMultiplier, err := compiler.abilityEnvironmentHighestStatMultiplier(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInStrongWeather, err := compiler.abilitySwitchInStrongWeather(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInWeather, err := compiler.abilitySwitchInWeather(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInTerrain, err := compiler.abilitySwitchInTerrain(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInStatStageChange, err := compiler.abilitySwitchInStatStageChange(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInAllyHeal, err := compiler.abilitySwitchInAllyHeal(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInOpponentDefenseComparisonBoost, err := compiler.abilitySwitchInOpponentDefenseComparisonBoost(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInAllyStatStageCopy, err := compiler.abilitySwitchInAllyStatStageCopy(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInAllyStatStageReset, err := compiler.abilitySwitchInAllyStatStageReset(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInClearAllSideDamageReductions, err := compiler.abilitySwitchInClearAllSideDamageReductions(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInCopyOpponentAbility, err := compiler.abilitySwitchInCopyOpponentAbility(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInRevealOpponentHeldItems, err := compiler.abilitySwitchInRevealOpponentHeldItems(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInRevealOpponentHighestPowerSkill, err := compiler.abilitySwitchInRevealOpponentHighestPowerSkill(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInTransformIntoOpponent, err := compiler.abilitySwitchInTransformIntoOpponent(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInDetectDangerousOpponentSkill, err := compiler.abilitySwitchInDetectDangerousOpponentSkill(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInDisguiseAsLastHealthyAlly, err := compiler.abilitySwitchInDisguiseAsLastHealthyAlly(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInHeldItemElementIdentity, err := compiler.abilitySwitchInHeldItemElementIdentity(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchInFormChange, err := compiler.abilitySwitchInFormChange(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchOutFormChange, err := compiler.abilitySwitchOutFormChange(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	weatherFormChange, err := compiler.abilityWeatherFormChange(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	terastallizationStatStageChange, err := compiler.abilityTerastallizationStatStageChange(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	terastallizationEnvironmentClear, err := compiler.abilityTerastallizationEnvironmentClear(member.AbilityID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	level, err := levelForFormat(compiler.format, compiler.normalizedLevel, member.Level)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	form, err := creatureForm(compiler.metadata, member)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	stats, maxHP, err := compiler.memberStatsForCreature(member, member.CreatureID, level)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	weight, err := creatureWeight(compiler.metadata, member.CreatureID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	formProfiles, err := compiler.compileFormProfiles(
		member, level, form, stats, maxHP, weight, switchInFormChange, switchOutFormChange, weatherFormChange,
	)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	skills := make([]battleengine.SkillSnapshot, 0, len(member.Skills))
	for expectedPosition, slot := range member.Skills {
		if slot.Position != int32(expectedPosition+1) {
			return BattleMemberFacts{}, ErrInitialStateCompilation
		}
		skill, err := compiler.skill(slot.SkillID, battleengine.SkillPosition(slot.Position))
		if err != nil {
			return BattleMemberFacts{}, err
		}
		skills = append(skills, skill)
	}
	elements, err := compiler.elementIDsForForm(form)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	var itemID snowflake.ID
	if member.ItemID != nil {
		itemID = *member.ItemID
	}
	heldItemElementID, err := compiler.heldItemElementID(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	highestStatBoosterAbilityIDs, err := compiler.highestStatBoosterAbilityIDs(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	damagedForceSelfSwitch, damagedForceAttackerSwitch, negativeStatStageForceSelfSwitch, err := compiler.itemForcedSwitchRules(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	switchRestrictionImmunity, err := compiler.itemSwitchRestrictionImmunity(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	contactSideEffectImmunity, err := compiler.itemContactSideEffectImmunity(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemContactDamageToAttackerDenominator, err := compiler.itemContactDamageToAttackerDenominator(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemEndTurnHealDenominator, err := compiler.itemEndTurnHealDenominator(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemEndTurnHealForElementID, heldItemEndTurnHealForElementDenominator, err := compiler.itemEndTurnHealForElement(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemEndTurnDamageDenominator, err := compiler.itemEndTurnDamageDenominator(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemEndTurnDamageWithoutElementID, heldItemEndTurnDamageWithoutElementDenominator, err := compiler.itemEndTurnDamageWithoutElement(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemConsumableElementDamageBoostElementID, heldItemConsumableElementDamageBoostNumerator, heldItemConsumableElementDamageBoostDenominator, err := compiler.itemConsumableElementDamageBoost(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	contactTransferToAttacker, err := compiler.itemContactTransferToAttacker(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	chargeSkipOnce, err := compiler.itemChargeSkipOnce(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemSurviveFatalDamageAtFullHP, err := compiler.itemSurviveFatalDamageAtFullHP(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemReflectTurnsRemaining, err := compiler.itemReflectTurnsRemaining(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemLightScreenTurnsRemaining, err := compiler.itemLightScreenTurnsRemaining(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemAuroraVeilTurnsRemaining, err := compiler.itemAuroraVeilTurnsRemaining(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemRainTurnsRemaining, err := compiler.itemRainTurnsRemaining(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemSandstormTurnsRemaining, err := compiler.itemSandstormTurnsRemaining(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemSnowTurnsRemaining, err := compiler.itemSnowTurnsRemaining(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemSunTurnsRemaining, err := compiler.itemSunTurnsRemaining(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemTerrainTurnsRemaining, err := compiler.itemTerrainTurnsRemaining(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemSandstormDamageImmunity, err := compiler.itemSandstormDamageImmunity(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemEntryHazardImmunity, err := compiler.itemEntryHazardImmunity(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemWeightHalf, err := compiler.itemWeightHalf(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemCuresParalysis, err := compiler.itemCuresParalysis(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemCuresSleep, err := compiler.itemCuresSleep(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemCuresPoison, err := compiler.itemCuresPoison(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemCuresBurn, err := compiler.itemCuresBurn(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemCuresFreeze, err := compiler.itemCuresFreeze(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemCuresAllMajorStatuses, err := compiler.itemCuresAllMajorStatuses(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemCuresConfusion, err := compiler.itemCuresConfusion(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemPunchBasedSkillPowerBoost, err := compiler.itemPunchBasedSkillPowerBoost(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemPhysicalDamagePowerBoost, heldItemSpecialDamagePowerBoost, err := compiler.itemDamageClassPowerBoosts(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemElementDamageReductionElementID, heldItemElementDamageReductionRequiresSuperEffective, err := compiler.itemElementDamageReduction(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemSuperEffectiveDamageBoost, heldItemDamageBoostWithRecoil, err := compiler.itemConditionalDamageBoosts(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemUtilities, err := compiler.itemUtilityEffects(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemPunchBasedContactSuppression, err := compiler.itemPunchBasedContactSuppression(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemPowderSkillImmunity, err := compiler.itemPowderSkillImmunity(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	heldItemMultiHitCountMinimum, heldItemMultiHitCountMaximum, heldItemMultiHitRequiredMinimum, heldItemMultiHitRequiredMaximum, err := compiler.itemMultiHitRangeOverride(member.ItemID)
	if err != nil {
		return BattleMemberFacts{}, err
	}
	return BattleMemberFacts{
		Position: battleengine.MemberPosition(member.Position), CreatureID: member.CreatureID, NatureID: member.NatureID, GenderCode: genderCode, Level: level,
		MaxHP: maxHP, Stats: stats, Weight: weight, ElementIDs: elements, TeraElementID: member.TeraElementID, FormProfiles: formProfiles,
		Skills: skills, AbilityID: member.AbilityID,
		WeatherDamageImmunities: weatherDamageImmunities, WeatherEffectsSuppressed: weatherEffectsSuppressed,
		ReactiveAbilityRules:                         reactiveAbilityRules,
		AccuracyMultiplier:                           accuracyRules.accuracyMultiplier,
		PhysicalSkillAccuracyMultiplier:              accuracyRules.physicalSkillAccuracyMultiplier,
		BasePowerAtMostDamageBoost:                   damageMultiplierRules.basePowerAtMostDamageBoost,
		RecoilSkillDamageBoost:                       damageMultiplierRules.recoilSkillDamageBoost,
		LowHPElementDamageBoost:                      damageMultiplierRules.lowHPElementDamageBoost,
		WeatherElementDamageBoost:                    damageMultiplierRules.weatherElementDamageBoost,
		ElementSkillDamageBoost:                      damageMultiplierRules.elementSkillDamageBoost,
		SameElementBonusOverride:                     damageMultiplierRules.sameElementBonusOverride,
		ContactBasedSkillDamageBoost:                 damageMultiplierRules.contactBasedSkillDamageBoost,
		CriticalHitDamageBoost:                       damageMultiplierRules.criticalHitDamageBoost,
		SuperEffectiveDamageBoost:                    damageMultiplierRules.superEffectiveDamageBoost,
		NotVeryEffectiveDamageBoost:                  damageMultiplierRules.notVeryEffectiveDamageBoost,
		TargetGenderDamageMultiplier:                 cloneFormulaRule(formulaDetail.TargetGenderDamageMultiplier),
		PunchBasedSkillDamageBoost:                   cloneFormulaRule(formulaDetail.PunchBasedSkillDamageBoost),
		SlicingBasedSkillDamageBoost:                 cloneFormulaRule(formulaDetail.SlicingBasedSkillDamageBoost),
		SoundBasedSkillDamageBoost:                   cloneFormulaRule(formulaDetail.SoundBasedSkillDamageBoost),
		PulseBasedSkillDamageBoost:                   cloneFormulaRule(formulaDetail.PulseBasedSkillDamageBoost),
		BiteBasedSkillDamageBoost:                    cloneFormulaRule(formulaDetail.BiteBasedSkillDamageBoost),
		SecondaryEffectsSuppressedDamageBoost:        cloneFormulaRule(formulaDetail.SecondaryEffectsSuppressedDamageBoost),
		SoundBasedSkillDamageReduction:               cloneFormulaRule(formulaDetail.SoundBasedSkillDamageReduction),
		SuperEffectiveDamageReduction:                cloneFormulaRule(formulaDetail.SuperEffectiveDamageReduction),
		FullHPDamageReduction:                        cloneFormulaRule(formulaDetail.FullHPDamageReduction),
		DamageClassDamageReduction:                   cloneFormulaDamageClassReduction(formulaDetail.DamageClassDamageReduction),
		ElementSkillDamageReduction:                  cloneFormulaElementReduction(formulaDetail.ElementSkillDamageReduction),
		ContactBasedSkillDamageReduction:             cloneFormulaRule(formulaDetail.ContactBasedSkillDamageReduction),
		AttackingStatMultiplier:                      cloneFormulaAttackingStat(formulaDetail.AttackingStatMultiplier),
		OpponentAttackingStatMultiplier:              cloneFormulaRule(formulaDetail.OpponentAttackingStatMultiplier),
		DefendingStatMultiplier:                      cloneFormulaRule(formulaDetail.DefendingStatMultiplier),
		OpponentDefendingStatMultiplier:              cloneFormulaRule(formulaDetail.OpponentDefendingStatMultiplier),
		AllySkillDamageBoost:                         cloneFormulaAllySkillBoost(formulaDetail.AllySkillDamageBoost),
		AllyReceivedDamageReduction:                  cloneFormulaRule(formulaDetail.AllyReceivedDamageReduction),
		AllyAbilityGroupCode:                         formulaDetail.AllyAbilityGroupCode,
		AllyAbilityPresenceAttackingStatMultiplier:   cloneFormulaRule(formulaDetail.AllyAbilityPresenceAttackingStatMultiplier),
		OpponentAccuracySandstormMultiplier:          accuracyRules.opponentAccuracySandstormMultiplier,
		OpponentAccuracySnowMultiplier:               accuracyRules.opponentAccuracySnowMultiplier,
		OpponentAccuracyConfusionMultiplier:          accuracyRules.opponentAccuracyConfusionMultiplier,
		AccuracyAlwaysHits:                           accuracyRules.accuracyAlwaysHits,
		StatusSkillAccuracyCap:                       accuracyRules.statusSkillAccuracyCap,
		IgnoreOpponentAccuracyStatStages:             accuracyRules.ignoreOpponentAccuracyStatStages,
		CriticalHitImmunity:                          accuracyRules.criticalHitImmunity,
		SkillRecoilDamageImmunity:                    accuracyRules.skillRecoilDamageImmunity,
		IndirectDamageImmunity:                       accuracyRules.indirectDamageImmunity,
		ContactDamageToAttackerDenominator:           accuracyRules.contactDamageToAttackerDenominator,
		IgnoreOpponentDamageStatStages:               accuracyRules.ignoreOpponentDamageStatStages,
		IgnoreTargetAbilityEffects:                   accuracyRules.ignoreTargetAbilityEffects,
		SurviveFatalDamageAtFullHP:                   accuracyRules.surviveFatalDamageAtFullHP,
		OpponentStatusSkillImmunity:                  accuracyRules.opponentStatusSkillImmunity,
		NonSuperEffectiveDamageImmunity:              accuracyRules.nonSuperEffectiveDamageImmunity,
		CriticalHitStageBoost:                        accuracyRules.criticalHitStageBoost,
		MultiHitMaximum:                              accuracyRules.multiHitMaximum,
		DamagingSkillSecondaryEffectImmunity:         accuracyRules.damagingSkillSecondaryEffectImmunity,
		PriorityMoveImmunityForSideEnabled:           accuracyRules.priorityMoveImmunityForSideEnabled,
		PriorityMoveImmunityForSideProtectsAllies:    accuracyRules.priorityMoveImmunityForSideProtectsAllies,
		StatusSkillMovesLastAndIgnoresTargetAbility:  accuracyRules.statusSkillMovesLastAndIgnoresTargetAbility,
		ContactSkillProtectionBypass:                 accuracyRules.contactSkillProtectionBypass,
		ContactSkillProtectionBypassDamageMultiplier: cloneBattleDamageFraction(formulaDetail.ContactSkillProtectionBypassDamageMultiplier),
		SkillWeatherOverride:                         formulaDetail.SkillWeatherOverride,
		SkillElementConversion:                       cloneBattleSkillElementConversion(formulaDetail.SkillElementConversion),
		ContactSuppression:                           accuracyRules.contactSuppression,
		ReceivedContactDamageHalved:                  accuracyRules.receivedContactDamageHalved,
		ReceivedFireDamageDoubled:                    accuracyRules.receivedFireDamageDoubled,
		ForcedSwitchImmunity:                         forcedSwitchImmunity,
		OpponentSwitchRestriction:                    opponentSwitchRestriction,
		DamageCrossedHalfHPForceSelfSwitch:           damageCrossedHalfHPForceSelfSwitch,
		SwitchOutMajorStatusCure:                     switchOutMajorStatusCure,
		SwitchOutHealDenominator:                     switchOutHealDenominator,
		WeatherEndTurnHealing:                        weatherEndTurnHealing, WeatherSpeedMultipliers: weatherSpeedMultipliers,
		EnvironmentHighestStatMultiplier: environmentHighestStatMultiplier,
		SwitchInStrongWeather:            switchInStrongWeather, SwitchInWeather: switchInWeather,
		SwitchInTerrain: switchInTerrain, SwitchInStatStageChange: switchInStatStageChange,
		SwitchInAllyHeal:                       switchInAllyHeal,
		SwitchInOpponentDefenseComparisonBoost: switchInOpponentDefenseComparisonBoost, ItemID: itemID,
		SwitchInAllyStatStageCopy:                            switchInAllyStatStageCopy,
		SwitchInAllyStatStageReset:                           switchInAllyStatStageReset,
		SwitchInClearAllSideDamageReductions:                 switchInClearAllSideDamageReductions,
		SwitchInCopyOpponentAbility:                          switchInCopyOpponentAbility,
		SwitchInRevealOpponentHeldItems:                      switchInRevealOpponentHeldItems,
		SwitchInRevealOpponentHighestPowerSkill:              switchInRevealOpponentHighestPowerSkill,
		SwitchInTransformIntoOpponent:                        switchInTransformIntoOpponent,
		SwitchInDetectDangerousOpponentSkill:                 switchInDetectDangerousOpponentSkill,
		SwitchInDisguiseAsLastHealthyAlly:                    switchInDisguiseAsLastHealthyAlly,
		SwitchInFormChange:                                   switchInFormChange,
		SwitchOutFormChange:                                  switchOutFormChange,
		WeatherFormChange:                                    weatherFormChange,
		TerastallizationStatStageChange:                      terastallizationStatStageChange,
		TerastallizationEnvironmentClear:                     terastallizationEnvironmentClear,
		SwitchInHeldItemElementIdentity:                      switchInHeldItemElementIdentity,
		HeldItemElementID:                                    heldItemElementID,
		HighestStatBoosterAbilityIDs:                         highestStatBoosterAbilityIDs,
		DamagedForceSelfSwitch:                               damagedForceSelfSwitch,
		DamagedForceAttackerSwitch:                           damagedForceAttackerSwitch,
		NegativeStatStageForceSelfSwitch:                     negativeStatStageForceSelfSwitch,
		SwitchRestrictionImmunity:                            switchRestrictionImmunity,
		ContactSideEffectImmunity:                            contactSideEffectImmunity,
		HeldItemContactDamageToAttackerDenominator:           heldItemContactDamageToAttackerDenominator,
		HeldItemEndTurnHealDenominator:                       heldItemEndTurnHealDenominator,
		HeldItemEndTurnHealForElementID:                      heldItemEndTurnHealForElementID,
		HeldItemEndTurnHealForElementDenominator:             heldItemEndTurnHealForElementDenominator,
		HeldItemEndTurnDamageDenominator:                     heldItemEndTurnDamageDenominator,
		HeldItemEndTurnDamageWithoutElementID:                heldItemEndTurnDamageWithoutElementID,
		HeldItemEndTurnDamageWithoutElementDenominator:       heldItemEndTurnDamageWithoutElementDenominator,
		HeldItemConsumableElementDamageBoostElementID:        heldItemConsumableElementDamageBoostElementID,
		HeldItemConsumableElementDamageBoostNumerator:        heldItemConsumableElementDamageBoostNumerator,
		HeldItemConsumableElementDamageBoostDenominator:      heldItemConsumableElementDamageBoostDenominator,
		ContactTransferToAttacker:                            contactTransferToAttacker,
		ChargeSkipOnce:                                       chargeSkipOnce,
		HeldItemSurviveFatalDamageAtFullHP:                   heldItemSurviveFatalDamageAtFullHP,
		HeldItemReflectTurnsRemaining:                        heldItemReflectTurnsRemaining,
		HeldItemLightScreenTurnsRemaining:                    heldItemLightScreenTurnsRemaining,
		HeldItemAuroraVeilTurnsRemaining:                     heldItemAuroraVeilTurnsRemaining,
		HeldItemRainTurnsRemaining:                           heldItemRainTurnsRemaining,
		HeldItemSandstormTurnsRemaining:                      heldItemSandstormTurnsRemaining,
		HeldItemSnowTurnsRemaining:                           heldItemSnowTurnsRemaining,
		HeldItemSunTurnsRemaining:                            heldItemSunTurnsRemaining,
		HeldItemTerrainTurnsRemaining:                        heldItemTerrainTurnsRemaining,
		HeldItemSandstormDamageImmunity:                      heldItemSandstormDamageImmunity,
		HeldItemEntryHazardImmunity:                          heldItemEntryHazardImmunity,
		HeldItemWeightHalf:                                   heldItemWeightHalf,
		HeldItemCuresParalysis:                               heldItemCuresParalysis,
		HeldItemCuresSleep:                                   heldItemCuresSleep,
		HeldItemCuresPoison:                                  heldItemCuresPoison,
		HeldItemCuresBurn:                                    heldItemCuresBurn,
		HeldItemCuresFreeze:                                  heldItemCuresFreeze,
		HeldItemCuresAllMajorStatuses:                        heldItemCuresAllMajorStatuses,
		HeldItemCuresConfusion:                               heldItemCuresConfusion,
		HeldItemPunchBasedSkillPowerBoost:                    heldItemPunchBasedSkillPowerBoost,
		HeldItemPhysicalDamagePowerBoost:                     heldItemPhysicalDamagePowerBoost,
		HeldItemSpecialDamagePowerBoost:                      heldItemSpecialDamagePowerBoost,
		HeldItemElementDamageReductionElementID:              heldItemElementDamageReductionElementID,
		HeldItemElementDamageReductionRequiresSuperEffective: heldItemElementDamageReductionRequiresSuperEffective,
		HeldItemSuperEffectiveDamageBoost:                    heldItemSuperEffectiveDamageBoost,
		HeldItemDamageBoostWithRecoil:                        heldItemDamageBoostWithRecoil,
		HeldItemDamageDealtHeal:                              heldItemUtilities.damageDealtHeal,
		HeldItemDrainHealingBoost:                            heldItemUtilities.drainHealingBoost,
		HeldItemAccuracyBoost:                                heldItemUtilities.accuracyBoost,
		HeldItemOpponentAccuracyReduction:                    heldItemUtilities.opponentAccuracyReduction,
		HeldItemCriticalHitStageBoost:                        heldItemUtilities.criticalHitStageBoost,
		HeldItemAirborneUntilDamaged:                         heldItemUtilities.airborneUntilDamaged,
		HeldItemForceGrounded:                                heldItemUtilities.forceGrounded,
		HeldItemSpeedHalf:                                    heldItemUtilities.speedHalf,
		HeldItemSpecialDefenseBoost:                          heldItemUtilities.specialDefenseBoost,
		HeldItemStatusSkillRestriction:                       heldItemUtilities.statusSkillRestriction,
		HeldItemPhysicalDamagePowerBoost50:                   heldItemUtilities.physicalDamagePowerBoost50,
		HeldItemSpecialDamagePowerBoost50:                    heldItemUtilities.specialDamagePowerBoost50,
		HeldItemChoiceSkillLock:                              heldItemUtilities.choiceSkillLock,
		HeldItemSpeedBoost50:                                 heldItemUtilities.speedBoost50,
		HeldItemAccuracyAfterTargetActedBoost:                heldItemUtilities.accuracyAfterTargetActedBoost,
		HeldItemTypeImmunitySuppression:                      heldItemUtilities.typeImmunitySuppression,
		HeldItemOpponentStatStageReductionImmunity:           heldItemUtilities.opponentStatStageReductionImmunity,
		HeldItemNegativeStatStageReset:                       heldItemUtilities.negativeStatStageReset,
		HeldItemAbilityStatReductionSpeedBoost:               heldItemUtilities.abilityStatReductionSpeedBoost,
		HeldItemOpponentPositiveStatStageCopy:                heldItemUtilities.opponentPositiveStatStageCopy,
		HeldItemDamagingSkillSecondaryEffectImmunity:         heldItemUtilities.damagingSkillSecondaryEffectImmunity,
		HeldItemBindingTurns:                                 heldItemUtilities.bindingTurns,
		HeldItemBindingDamageDenominator:                     heldItemUtilities.bindingDamageDenominator,
		HeldItemAccuracyMissStatStageBoostStat:               heldItemUtilities.accuracyMissStatStageBoostStat,
		HeldItemAccuracyMissStatStageBoostDelta:              heldItemUtilities.accuracyMissStatStageBoostDelta,
		HeldItemWeaknessPolicy:                               heldItemUtilities.weaknessPolicy,
		HeldItemWaterDamageSpecialAttackBoostElementID:       heldItemUtilities.waterDamageSpecialAttackBoostElementID,
		HeldItemElectricDamageAttackBoostElementID:           heldItemUtilities.electricDamageAttackBoostElementID,
		HeldItemWaterDamageSpecialDefenseBoostElementID:      heldItemUtilities.waterDamageSpecialDefenseBoostElementID,
		HeldItemIceDamageAttackBoostElementID:                heldItemUtilities.iceDamageAttackBoostElementID,
		HeldItemAdditionalFlinchChancePercent:                heldItemUtilities.additionalFlinchChancePercent,
		HeldItemRandomActionOrderBoostChancePercent:          heldItemUtilities.randomActionOrderBoostChancePercent,
		HeldItemForcedLastActionOrder:                        heldItemUtilities.forcedLastActionOrder,
		HeldItemLowHPActionOrderBoost:                        heldItemUtilities.lowHPActionOrderBoost,
		HeldItemFieldSpeedOrderSpeedStageDrop:                heldItemUtilities.fieldSpeedOrderSpeedStageDrop,
		HeldItemConsecutiveSkillDamageBoost:                  heldItemUtilities.consecutiveSkillDamageBoost,
		HeldItemPunchBasedContactSuppression:                 heldItemPunchBasedContactSuppression,
		HeldItemPowderSkillImmunity:                          heldItemPowderSkillImmunity,
		HeldItemMultiHitCountMinimum:                         heldItemMultiHitCountMinimum,
		HeldItemMultiHitCountMaximum:                         heldItemMultiHitCountMaximum,
		HeldItemMultiHitRequiredMinimum:                      heldItemMultiHitRequiredMinimum,
		HeldItemMultiHitRequiredMaximum:                      heldItemMultiHitRequiredMaximum,
	}, nil
}

// genderCode 将 Team 中可选性别 Identifier 解析为启用 Current Game Data 的稳定代码。
// 空引用明确表示无性别；未知或停用引用会阻止 Battle 启动，避免运行时比较 Identifier 或展示名称。
func (compiler *initialMemberCompiler) genderCode(genderID *snowflake.ID) (string, error) {
	if genderID == nil {
		return "", nil
	}
	for _, gender := range compiler.metadata.Genders {
		if gender.ID == *genderID && gender.Enabled {
			if gender.Code == "" {
				return "", ErrInitialStateCompilation
			}
			return gender.Code, nil
		}
	}
	return "", ErrInitialStateCompilation
}

func levelForFormat(format battleformat.Format, normalizedLevel uint8, memberLevel int32) (uint8, error) {
	if normalizedLevel != 0 {
		if format.LevelRule.Mode != battleformat.LevelRuleNormalize || format.LevelRule.Level == nil ||
			*format.LevelRule.Level < 1 || *format.LevelRule.Level > 100 || uint8(*format.LevelRule.Level) != normalizedLevel {
			return 0, ErrInitialStateCompilation
		}
		return normalizedLevel, nil
	}
	switch format.LevelRule.Mode {
	case battleformat.LevelRuleNormalize:
		if format.LevelRule.Level == nil || *format.LevelRule.Level < 1 || *format.LevelRule.Level > 100 {
			return 0, ErrInitialStateCompilation
		}
		return uint8(*format.LevelRule.Level), nil
	case battleformat.LevelRulePreserve:
		if memberLevel < 1 || memberLevel > 100 {
			return 0, ErrInitialStateCompilation
		}
		return uint8(memberLevel), nil
	default:
		return 0, ErrInitialStateCompilation
	}
}

func creatureForm(data creaturemetadata.Data, member team.Member) (creaturemetadata.Form, error) {
	creatureFound := false
	for _, creature := range data.Creatures {
		if creature.ID == member.CreatureID {
			creatureFound = creature.Enabled
			break
		}
	}
	if !creatureFound {
		return creaturemetadata.Form{}, ErrInitialStateCompilation
	}
	for _, form := range data.Forms {
		if form.CreatureID != member.CreatureID || !form.Enabled {
			continue
		}
		if member.FormID != nil && form.ID == *member.FormID || member.FormID == nil && form.DefaultForm {
			if len(form.ElementIDs) < 1 || len(form.ElementIDs) > 2 {
				return creaturemetadata.Form{}, ErrInitialStateCompilation
			}
			return form, nil
		}
	}
	return creaturemetadata.Form{}, ErrInitialStateCompilation
}

// creatureWeight 从已启用精灵资料读取对局中不可变的体重整数刻度。体重不是展示信息：动态威力公式需要它
// 与阈值使用同一单位，因此缺失、零值或负值的资料必须在 Battle 启动前拒绝。
func creatureWeight(data creaturemetadata.Data, creatureID snowflake.ID) (uint32, error) {
	for _, creature := range data.Creatures {
		if creature.ID != creatureID {
			continue
		}
		if !creature.Enabled || creature.Weight == nil || *creature.Weight <= 0 {
			return 0, ErrInitialStateCompilation
		}
		return uint32(*creature.Weight), nil
	}
	return 0, ErrInitialStateCompilation
}

// defaultCreatureForm 读取一个启用精灵的启用默认形态。
//
// 特性形态规则只引用 Creature Data Projection 的 Creature Identifier，而不引用展示用的 Form Identifier。因而每个被规则引用的目标
// 都必须拥有确定的默认形态，不能在运行时按名称、Stable Code 或数据库返回顺序猜测属性。
func defaultCreatureForm(data creaturemetadata.Data, creatureID snowflake.ID) (creaturemetadata.Form, error) {
	if creatureID == snowflake.ID(0) {
		return creaturemetadata.Form{}, ErrInitialStateCompilation
	}
	creatureFound := false
	for _, creature := range data.Creatures {
		if creature.ID == creatureID {
			creatureFound = creature.Enabled
			break
		}
	}
	if !creatureFound {
		return creaturemetadata.Form{}, ErrInitialStateCompilation
	}
	var result *creaturemetadata.Form
	for index := range data.Forms {
		form := &data.Forms[index]
		if form.CreatureID != creatureID || !form.Enabled || !form.DefaultForm {
			continue
		}
		if result != nil || len(form.ElementIDs) < 1 || len(form.ElementIDs) > 2 {
			return creaturemetadata.Form{}, ErrInitialStateCompilation
		}
		result = form
	}
	if result == nil {
		return creaturemetadata.Form{}, ErrInitialStateCompilation
	}
	return *result, nil
}

// elementIDsForForm 将资料形态的属性 Identifier 列表编译为引擎快照使用的稳定文本标识。
//
// 本函数同时确认每个属性仍启用且没有重复项，保证目标形态的属性完整性在 Battle 启动时失败，而不是在战斗中
// 才因为动态属性相性找不到资料而产生不确定行为。
func (compiler *initialMemberCompiler) elementIDsForForm(form creaturemetadata.Form) ([]battleengine.Identifier, error) {
	if len(form.ElementIDs) < 1 || len(form.ElementIDs) > 2 {
		return nil, ErrInitialStateCompilation
	}
	result := make([]battleengine.Identifier, 0, len(form.ElementIDs))
	seen := make(map[snowflake.ID]struct{}, len(form.ElementIDs))
	for _, elementID := range form.ElementIDs {
		if elementID == snowflake.ID(0) {
			return nil, ErrInitialStateCompilation
		}
		if _, duplicated := seen[elementID]; duplicated {
			return nil, ErrInitialStateCompilation
		}
		if _, found := compiler.elementIDEnabled(elementID); !found {
			return nil, ErrInitialStateCompilation
		}
		seen[elementID] = struct{}{}
		result = append(result, elementID)
	}
	return result, nil
}

// compileFormProfiles 冻结当前成员可由特性形态规则引用的完整战斗画像。
//
// 每个引用的精灵都复用当前成员的等级和培养值，但读取自身的基础能力、体重与默认形态属性。资料缺失、禁用、
// 重复或不完整时必须拒绝启动新 Battle；战斗运行期不能再用 Identifier、名称或 Stable Code 回查实时资料。
func (compiler *initialMemberCompiler) compileFormProfiles(
	member team.Member,
	level uint8,
	currentForm creaturemetadata.Form,
	currentStats battleengine.StatBlock,
	currentMaxHP uint32,
	currentWeight uint32,
	switchIn *battleengine.SwitchInFormChange,
	switchOut *battleengine.SwitchOutFormChange,
	weather *battleengine.WeatherFormChange,
) ([]battleengine.FormProfile, error) {
	if switchIn == nil && switchOut == nil && weather == nil {
		return nil, nil
	}
	ids := make([]snowflake.ID, 0, 7)
	seen := make(map[snowflake.ID]struct{}, 7)
	appendID := func(id snowflake.ID) error {
		if id == snowflake.ID(0) {
			return ErrInitialStateCompilation
		}
		if _, found := seen[id]; !found {
			seen[id] = struct{}{}
			ids = append(ids, id)
		}
		return nil
	}
	if err := appendID(member.CreatureID); err != nil {
		return nil, err
	}
	if switchIn != nil {
		if err := appendID(switchIn.BaseCreatureID); err != nil {
			return nil, err
		}
		if err := appendID(switchIn.AlternateCreatureID); err != nil {
			return nil, err
		}
	}
	if switchOut != nil {
		if err := appendID(switchOut.BaseCreatureID); err != nil {
			return nil, err
		}
		if err := appendID(switchOut.AlternateCreatureID); err != nil {
			return nil, err
		}
	}
	if weather != nil {
		if err := appendID(weather.DefaultCreatureID); err != nil {
			return nil, err
		}
		for _, target := range weather.Targets {
			if err := appendID(target.CreatureID); err != nil {
				return nil, err
			}
		}
	}

	profiles := make([]battleengine.FormProfile, 0, len(ids))
	for _, creatureID := range ids {
		form := currentForm
		stats, maxHP, weight := currentStats, currentMaxHP, currentWeight
		if creatureID != member.CreatureID {
			var err error
			form, err = defaultCreatureForm(compiler.metadata, creatureID)
			if err != nil {
				return nil, err
			}
			stats, maxHP, err = compiler.memberStatsForCreature(member, creatureID, level)
			if err != nil {
				return nil, err
			}
			weight, err = creatureWeight(compiler.metadata, creatureID)
			if err != nil {
				return nil, err
			}
		}
		elementIDs, err := compiler.elementIDsForForm(form)
		if err != nil {
			return nil, err
		}
		profiles = append(profiles, battleengine.FormProfile{
			CreatureID: creatureID, MaxHP: maxHP, Stats: stats, Weight: weight, ElementIDs: elementIDs,
		})
	}
	return profiles, nil
}

// memberStatsForCreature 按成员培养值、指定精灵的基础能力和冻结等级计算战斗数值。
//
// 形态切换不会共享原始精灵的基础能力；它只共享同一 Team 成员已冻结的训练输入。因此目标精灵缺少任一六项
// 能力、引用禁用数值项或拥有重复能力绑定都会阻止新 Battle。
func (compiler *initialMemberCompiler) memberStatsForCreature(member team.Member, creatureID snowflake.ID, level uint8) (battleengine.StatBlock, uint32, error) {
	natureValue, err := compiler.nature(member.NatureID)
	if err != nil || !natureValue.Enabled {
		return battleengine.StatBlock{}, 0, ErrInitialStateCompilation
	}
	increasedCode, decreasedCode := "", ""
	if natureValue.IncreasedStatID != nil {
		increasedStat, increasedErr := compiler.stat(*natureValue.IncreasedStatID)
		decreasedStat, decreasedErr := compiler.stat(*natureValue.DecreasedStatID)
		if increasedErr != nil || decreasedErr != nil || !increasedStat.Enabled || !decreasedStat.Enabled ||
			!natureStatCode(increasedStat.Code) || !natureStatCode(decreasedStat.Code) || increasedStat.Code == decreasedStat.Code {
			return battleengine.StatBlock{}, 0, ErrInitialStateCompilation
		}
		increasedCode, decreasedCode = increasedStat.Code, decreasedStat.Code
	}
	baseByCode := make(map[string]int32)
	training := make(map[snowflake.ID]team.MemberStat, len(member.Stats))
	for _, value := range member.Stats {
		if _, duplicate := training[value.StatID]; duplicate || value.IndividualValue < 0 || value.IndividualValue > 31 || value.EffortValue < 0 || value.EffortValue > 252 {
			return battleengine.StatBlock{}, 0, ErrInitialStateCompilation
		}
		training[value.StatID] = value
	}
	for _, binding := range compiler.metadata.Stats {
		if binding.CreatureID != creatureID || binding.BaseValue <= 0 {
			continue
		}
		stat, err := compiler.stat(binding.StatID)
		if err != nil || !stat.Enabled {
			return battleengine.StatBlock{}, 0, ErrInitialStateCompilation
		}
		if _, duplicate := baseByCode[stat.Code]; duplicate {
			return battleengine.StatBlock{}, 0, ErrInitialStateCompilation
		}
		baseByCode[stat.Code] = binding.BaseValue
	}
	value := func(code string, hp bool) (uint32, error) {
		base, found := baseByCode[code]
		if !found {
			return 0, ErrInitialStateCompilation
		}
		var individual, effort int32
		for statID, assigned := range training {
			resolved, err := compiler.stat(statID)
			if err != nil {
				return 0, err
			}
			if resolved.Code == code {
				individual, effort = assigned.IndividualValue, assigned.EffortValue
				break
			}
		}
		calculated := ((2*int64(base)+int64(individual)+int64(effort)/4)*int64(level))/100 + 5
		if hp {
			calculated += int64(level) + 5
		} else if code == increasedCode {
			calculated = calculated * 110 / 100
		} else if code == decreasedCode {
			calculated = calculated * 90 / 100
		}
		if calculated < 1 || calculated > math.MaxUint32 {
			return 0, ErrInitialStateCompilation
		}
		return uint32(calculated), nil
	}
	hp, err := value("hp", true)
	if err != nil {
		return battleengine.StatBlock{}, 0, err
	}
	attack, err := value("attack", false)
	if err != nil {
		return battleengine.StatBlock{}, 0, err
	}
	defense, err := value("defense", false)
	if err != nil {
		return battleengine.StatBlock{}, 0, err
	}
	specialAttack, err := value("special-attack", false)
	if err != nil {
		return battleengine.StatBlock{}, 0, err
	}
	specialDefense, err := value("special-defense", false)
	if err != nil {
		return battleengine.StatBlock{}, 0, err
	}
	speed, err := value("speed", false)
	if err != nil {
		return battleengine.StatBlock{}, 0, err
	}
	return battleengine.StatBlock{Attack: attack, Defense: defense, SpecialAttack: specialAttack, SpecialDefense: specialDefense, Speed: speed}, hp, nil
}

func natureStatCode(code string) bool {
	return code == "attack" || code == "defense" || code == "special-attack" || code == "special-defense" || code == "speed"
}

func (compiler *initialMemberCompiler) nature(id snowflake.ID) (nature.Nature, error) {
	if value, ok := compiler.natures[id]; ok {
		return value, nil
	}
	value, found := compiler.snapshot.natures[id]
	if !found || value.ID != id || !value.Enabled {
		return nature.Nature{}, ErrInitialStateCompilation
	}
	compiler.natures[id] = value
	return value, nil
}

func (compiler *initialMemberCompiler) stat(id snowflake.ID) (stat.Stat, error) {
	if cached, found := compiler.stats[id]; found {
		return cached, nil
	}
	data, found := compiler.snapshot.stats[id]
	if !found || !data.Enabled || strings.TrimSpace(data.Code) == "" {
		return stat.Stat{}, ErrInitialStateCompilation
	}
	compiler.stats[id] = data
	return data, nil
}

func battleStatForCode(code string) (battleengine.Stat, bool) {
	switch code {
	case "attack":
		return battleengine.StatAttack, true
	case "defense":
		return battleengine.StatDefense, true
	case "special-attack":
		return battleengine.StatSpecialAttack, true
	case "special-defense":
		return battleengine.StatSpecialDefense, true
	case "speed":
		return battleengine.StatSpeed, true
	case "accuracy":
		return battleengine.StatAccuracy, true
	case "evasion":
		return battleengine.StatEvasion, true
	default:
		return "", false
	}
}
