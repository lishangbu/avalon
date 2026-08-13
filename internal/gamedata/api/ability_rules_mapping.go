package api

import (
	"encoding/json"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
	"github.com/lishangbu/avalon/internal/gamedata/battlerules"
)

func abilityRuleValuesFromMessage(body *domainv1.GameAbilityRuleGroup) (abilitydetail.OptionalValues, error) {
	weatherDamageImmunities, err := abilityWeatherDamageImmunitiesFromMessages(body.GetWeatherDamageImmunities())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	reactiveAbilityRules, err := abilityReactiveAbilityRulesFromMessage(body.GetReactiveAbilityRules())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	protectionBypassDamageMultiplier, err := abilityProtectionBypassDamageMultiplierFromMessage(body.GetContactSkillProtectionBypassDamageMultiplier())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	skillWeatherOverride, err := abilitySkillWeatherOverrideFromMessage(body.GetSkillWeatherOverride())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	skillElementConversion, err := abilitySkillElementConversionFromMessage(body.GetSkillElementConversion())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	formulaRules, err := formulaAbilityRulesFromMessages(formulaAbilityCreateInput(body))
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	opponentSwitchRestriction, err := abilityOpponentSwitchRestrictionFromMessage(body.GetOpponentSwitchRestriction())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	weatherEndTurnHeal, err := abilityWeatherEndTurnHealFromMessage(body.GetWeatherEndTurnHeal())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	weatherSpeedMultipliers, err := abilityWeatherSpeedMultipliersFromMessages(body.GetWeatherSpeedMultipliers())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	accuracyMultiplier, err := abilityAccuracyMultiplierFromMessage(body.GetAccuracyMultiplier())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	physicalSkillAccuracyMultiplier, err := abilityAccuracyMultiplierFromMessage(body.GetPhysicalSkillAccuracyMultiplier())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	opponentAccuracySandstormMultiplier, err := abilityAccuracyMultiplierFromMessage(body.GetOpponentAccuracySandstormMultiplier())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	opponentAccuracySnowMultiplier, err := abilityAccuracyMultiplierFromMessage(body.GetOpponentAccuracySnowMultiplier())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	opponentAccuracyConfusionMultiplier, err := abilityAccuracyMultiplierFromMessage(body.GetOpponentAccuracyConfusionMultiplier())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	basePowerAtMostDamageBoost, err := abilityBasePowerAtMostDamageBoostFromMessage(body.GetBasePowerAtMostDamageBoost())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	recoilSkillDamageBoost, err := abilityRecoilSkillDamageBoostFromMessage(body.GetRecoilSkillDamageBoost())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	lowHPElementDamageBoost, err := abilityLowHPElementDamageBoostFromMessage(body.GetLowHpElementDamageBoost())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	weatherElementDamageBoost, err := abilityWeatherElementDamageBoostFromMessage(body.GetWeatherElementDamageBoost())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	elementSkillDamageBoost, err := abilityElementSkillDamageBoostFromMessage(body.GetElementSkillDamageBoost())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	sameElementBonusOverride, err := abilitySameElementBonusOverrideFromMessage(body.GetSameElementBonusOverride())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	contactBasedSkillDamageBoost, err := abilityContactBasedSkillDamageBoostFromMessage(body.GetContactBasedSkillDamageBoost())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	criticalHitDamageBoost, err := abilityCriticalHitDamageBoostFromMessage(body.GetCriticalHitDamageBoost())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	superEffectiveDamageBoost, err := abilitySuperEffectiveDamageBoostFromMessage(body.GetSuperEffectiveDamageBoost())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	notVeryEffectiveDamageBoost, err := abilityNotVeryEffectiveDamageBoostFromMessage(body.GetNotVeryEffectiveDamageBoost())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	environmentHighestStatMultiplier, err := abilityEnvironmentHighestStatMultiplierFromMessage(body.GetEnvironmentHighestStatMultiplier())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	switchInStrongWeather, err := abilitySwitchInStrongWeatherFromMessage(body.GetSwitchInStrongWeather())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	switchInWeather, err := abilitySwitchInWeatherFromMessage(body.GetSwitchInWeather())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	switchInTerrain, err := abilitySwitchInTerrainFromMessage(body.GetSwitchInTerrain())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	switchInStatStageChange, err := abilitySwitchInStatStageChangeFromMessage(body.GetSwitchInStatStageChange())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	switchInAllyHeal, err := abilitySwitchInAllyHealFromMessage(body.GetSwitchInAllyHeal())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	switchInFormChange, err := abilitySwitchInFormChangeFromMessage(body.GetSwitchInFormChange())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	switchOutFormChange, err := abilitySwitchOutFormChangeFromMessage(body.GetSwitchOutFormChange())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	switchOutHealDenominator, err := abilitySwitchOutHealDenominator(body.GetSwitchOutHealDenominator())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	weatherFormChange, err := abilityWeatherFormChangeFromMessage(body.GetWeatherFormChange())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	terastallizationStatStageChange, err := abilityTerastallizationStatStageChangeFromMessage(body.GetTerastallizationStatStageChange())
	if err != nil {
		return abilitydetail.OptionalValues{}, err
	}
	if switchInStrongWeather != nil && switchInWeather != nil {
		return abilitydetail.OptionalValues{}, kratoserrors.BadRequest("INCOMPATIBLE_ABILITY_SWITCH_IN_WEATHERS", "同一特性不能同时声明普通与强天气入场效果")
	}
	return abilitydetail.OptionalValues{
		WeatherDamageImmunities:                      weatherDamageImmunities,
		WeatherEffectsSuppressed:                     body.GetWeatherEffectsSuppressed(),
		ReactiveAbilityRules:                         reactiveAbilityRules,
		BasePowerAtMostDamageBoost:                   basePowerAtMostDamageBoost,
		RecoilSkillDamageBoost:                       recoilSkillDamageBoost,
		LowHPElementDamageBoost:                      lowHPElementDamageBoost,
		WeatherElementDamageBoost:                    weatherElementDamageBoost,
		ElementSkillDamageBoost:                      elementSkillDamageBoost,
		SameElementBonusOverride:                     sameElementBonusOverride,
		ContactBasedSkillDamageBoost:                 contactBasedSkillDamageBoost,
		CriticalHitDamageBoost:                       criticalHitDamageBoost,
		SuperEffectiveDamageBoost:                    superEffectiveDamageBoost,
		NotVeryEffectiveDamageBoost:                  notVeryEffectiveDamageBoost,
		TargetGenderDamageMultiplier:                 formulaRules.TargetGenderDamageMultiplier,
		PunchBasedSkillDamageBoost:                   formulaRules.PunchBasedSkillDamageBoost,
		SlicingBasedSkillDamageBoost:                 formulaRules.SlicingBasedSkillDamageBoost,
		SoundBasedSkillDamageBoost:                   formulaRules.SoundBasedSkillDamageBoost,
		PulseBasedSkillDamageBoost:                   formulaRules.PulseBasedSkillDamageBoost,
		BiteBasedSkillDamageBoost:                    formulaRules.BiteBasedSkillDamageBoost,
		SecondaryEffectsSuppressedDamageBoost:        formulaRules.SecondaryEffectsSuppressedDamageBoost,
		SoundBasedSkillDamageReduction:               formulaRules.SoundBasedSkillDamageReduction,
		SuperEffectiveDamageReduction:                formulaRules.SuperEffectiveDamageReduction,
		FullHPDamageReduction:                        formulaRules.FullHPDamageReduction,
		DamageClassDamageReduction:                   formulaRules.DamageClassDamageReduction,
		ElementSkillDamageReduction:                  formulaRules.ElementSkillDamageReduction,
		ContactBasedSkillDamageReduction:             formulaRules.ContactBasedSkillDamageReduction,
		AttackingStatMultiplier:                      formulaRules.AttackingStatMultiplier,
		OpponentAttackingStatMultiplier:              formulaRules.OpponentAttackingStatMultiplier,
		DefendingStatMultiplier:                      formulaRules.DefendingStatMultiplier,
		OpponentDefendingStatMultiplier:              formulaRules.OpponentDefendingStatMultiplier,
		AllySkillDamageBoost:                         formulaRules.AllySkillDamageBoost,
		AllyReceivedDamageReduction:                  formulaRules.AllyReceivedDamageReduction,
		AllyAbilityGroupCode:                         formulaRules.AllyAbilityGroupCode,
		AllyAbilityPresenceAttackingStatMultiplier:   formulaRules.AllyAbilityPresenceAttackingStatMultiplier,
		AccuracyMultiplier:                           accuracyMultiplier,
		PhysicalSkillAccuracyMultiplier:              physicalSkillAccuracyMultiplier,
		OpponentAccuracySandstormMultiplier:          opponentAccuracySandstormMultiplier,
		OpponentAccuracySnowMultiplier:               opponentAccuracySnowMultiplier,
		OpponentAccuracyConfusionMultiplier:          opponentAccuracyConfusionMultiplier,
		AccuracyAlwaysHits:                           body.GetAccuracyAlwaysHits(),
		StatusSkillAccuracyCap:                       body.GetStatusSkillAccuracyCap(),
		IgnoreOpponentAccuracyStatStages:             body.GetIgnoreOpponentAccuracyStatStages(),
		CriticalHitImmunity:                          body.GetCriticalHitImmunity(),
		SkillRecoilDamageImmunity:                    body.GetSkillRecoilDamageImmunity(),
		IndirectDamageImmunity:                       body.GetIndirectDamageImmunity(),
		ContactDamageToAttackerDenominator:           body.GetContactDamageToAttackerDenominator(),
		IgnoreOpponentDamageStatStages:               body.GetIgnoreOpponentDamageStatStages(),
		IgnoreTargetAbilityEffects:                   body.GetIgnoreTargetAbilityEffects(),
		SurviveFatalDamageAtFullHP:                   body.GetSurviveFatalDamageAtFullHp(),
		OpponentStatusSkillImmunity:                  body.GetOpponentStatusSkillImmunity(),
		NonSuperEffectiveDamageImmunity:              body.GetNonSuperEffectiveDamageImmunity(),
		CriticalHitStageBoost:                        body.GetCriticalHitStageBoost(),
		MultiHitMaximum:                              body.GetMultiHitMaximum(),
		DamagingSkillSecondaryEffectImmunity:         body.GetDamagingSkillSecondaryEffectImmunity(),
		PriorityMoveImmunityForSideEnabled:           body.GetPriorityMoveImmunityForSideEnabled(),
		PriorityMoveImmunityForSideProtectsAllies:    body.GetPriorityMoveImmunityForSideProtectsAllies(),
		StatusSkillMovesLastAndIgnoresTargetAbility:  body.GetStatusSkillMovesLastAndIgnoresTargetAbility(),
		ContactSkillProtectionBypass:                 body.GetContactSkillProtectionBypass(),
		ContactSkillProtectionBypassDamageMultiplier: protectionBypassDamageMultiplier,
		SkillWeatherOverride:                         skillWeatherOverride,
		SkillElementConversion:                       skillElementConversion,
		ContactSuppression:                           body.GetContactSuppression(),
		ReceivedContactDamageHalved:                  body.GetReceivedContactDamageHalved(),
		ReceivedFireDamageDoubled:                    body.GetReceivedFireDamageDoubled(),
		ForcedSwitchImmunity:                         body.GetForcedSwitchImmunity(),
		OpponentSwitchRestriction:                    opponentSwitchRestriction,
		DamageCrossedHalfHPForceSelfSwitch:           body.GetDamageCrossedHalfHpForceSelfSwitch(),
		WeatherEndTurnHeal:                           weatherEndTurnHeal,
		WeatherSpeedMultipliers:                      weatherSpeedMultipliers,
		EnvironmentHighestStatMultiplier:             environmentHighestStatMultiplier,
		SwitchInStrongWeather:                        switchInStrongWeather,
		SwitchInWeather:                              switchInWeather,
		SwitchInTerrain:                              switchInTerrain,
		SwitchInStatStageChange:                      switchInStatStageChange,
		SwitchInAllyHeal:                             switchInAllyHeal,
		SwitchInOpponentDefenseComparisonBoost:       body.GetSwitchInOpponentDefenseComparisonBoost(),
		SwitchInAllyStatStageCopy:                    body.GetSwitchInAllyStatStageCopy(),
		SwitchInAllyStatStageReset:                   body.GetSwitchInAllyStatStageReset(),
		SwitchInClearAllSideDamageReductions:         body.GetSwitchInClearAllSideDamageReductions(),
		SwitchInCopyOpponentAbility:                  body.GetSwitchInCopyOpponentAbility(),
		SwitchInRevealOpponentHeldItems:              body.GetSwitchInRevealOpponentHeldItems(),
		SwitchInRevealOpponentHighestPowerSkill:      body.GetSwitchInRevealOpponentHighestPowerSkill(),
		SwitchInTransformIntoOpponent:                body.GetSwitchInTransformIntoOpponent(),
		SwitchInDetectDangerousOpponentSkill:         body.GetSwitchInDetectDangerousOpponentSkill(),
		SwitchInDisguiseAsLastHealthyAlly:            body.GetSwitchInDisguiseAsLastHealthyAlly(),
		SwitchInHeldItemElementIdentity:              body.GetSwitchInHeldItemElementIdentity(),
		SwitchOutMajorStatusCure:                     body.GetSwitchOutMajorStatusCure(),
		SwitchOutHealDenominator:                     switchOutHealDenominator,
		SwitchOutFormChange:                          switchOutFormChange,
		SwitchInFormChange:                           switchInFormChange,
		WeatherFormChange:                            weatherFormChange,
		TerastallizationStatStageChange:              terastallizationStatStageChange,
		TerastallizationEnvironmentClear:             body.GetTerastallizationEnvironmentClear(),
	}, nil
}

func abilityRulesFromMessage(message *domainv1.GameAbilityRules) (battlerules.Ability, error) {
	if message == nil {
		return battlerules.Ability{}, nil
	}
	var rules battlerules.Ability
	groups := []struct {
		message *domainv1.GameAbilityRuleGroup
		assign  func(abilitydetail.OptionalValues)
	}{
		{message.GetPassive(), func(value abilitydetail.OptionalValues) {
			rules.Passive = &battlerules.AbilityPassive{OptionalValues: value}
		}},
		{message.GetReactive(), func(value abilitydetail.OptionalValues) {
			rules.Reactive = &battlerules.AbilityReactive{OptionalValues: value}
		}},
		{message.GetOnSwitchIn(), func(value abilitydetail.OptionalValues) {
			rules.OnSwitchIn = &battlerules.AbilityOnSwitchIn{OptionalValues: value}
		}},
		{message.GetOnSwitchOut(), func(value abilitydetail.OptionalValues) {
			rules.OnSwitchOut = &battlerules.AbilityOnSwitchOut{OptionalValues: value}
		}},
		{message.GetOnDamage(), func(value abilitydetail.OptionalValues) {
			rules.OnDamage = &battlerules.AbilityOnDamage{OptionalValues: value}
		}},
		{message.GetOnTurnEnd(), func(value abilitydetail.OptionalValues) {
			rules.OnTurnEnd = &battlerules.AbilityOnTurnEnd{OptionalValues: value}
		}},
		{message.GetOnEnvironmentChange(), func(value abilitydetail.OptionalValues) {
			rules.OnEnvironmentChange = &battlerules.AbilityOnEnvironmentChange{OptionalValues: value}
		}},
		{message.GetOnTerastallization(), func(value abilitydetail.OptionalValues) {
			rules.OnTerastallization = &battlerules.AbilityOnTerastallization{OptionalValues: value}
		}},
	}
	for _, group := range groups {
		if group.message == nil {
			continue
		}
		values, err := abilityRuleValuesFromMessage(group.message)
		if err != nil {
			return battlerules.Ability{}, err
		}
		values.Effect = nil
		values.ShortEffect = nil
		values.Introduction = nil
		group.assign(values)
	}
	payload, err := json.Marshal(rules)
	if err != nil {
		return battlerules.Ability{}, kratoserrors.BadRequest("INVALID_GAME_ABILITY_RULES", "特性战斗规则无效")
	}
	canonical, err := battlerules.ParseAbility(payload)
	if err != nil {
		return battlerules.Ability{}, kratoserrors.BadRequest("INVALID_GAME_ABILITY_RULES", "特性战斗规则与执行时机不匹配")
	}
	return canonical, nil
}

func abilityRulesMessage(rules battlerules.Ability) *domainv1.GameAbilityRules {
	return &domainv1.GameAbilityRules{
		Passive:             abilityRuleGroupMessage(optionalAbilityValues(rules.Passive)),
		Reactive:            abilityRuleGroupMessage(optionalAbilityValues(rules.Reactive)),
		OnSwitchIn:          abilityRuleGroupMessage(optionalAbilityValues(rules.OnSwitchIn)),
		OnSwitchOut:         abilityRuleGroupMessage(optionalAbilityValues(rules.OnSwitchOut)),
		OnDamage:            abilityRuleGroupMessage(optionalAbilityValues(rules.OnDamage)),
		OnTurnEnd:           abilityRuleGroupMessage(optionalAbilityValues(rules.OnTurnEnd)),
		OnEnvironmentChange: abilityRuleGroupMessage(optionalAbilityValues(rules.OnEnvironmentChange)),
		OnTerastallization:  abilityRuleGroupMessage(optionalAbilityValues(rules.OnTerastallization)),
	}
}

type abilityRuleGroup interface {
	*battlerules.AbilityPassive | *battlerules.AbilityReactive | *battlerules.AbilityOnSwitchIn |
		*battlerules.AbilityOnSwitchOut | *battlerules.AbilityOnDamage | *battlerules.AbilityOnTurnEnd |
		*battlerules.AbilityOnEnvironmentChange | *battlerules.AbilityOnTerastallization
}

func optionalAbilityValues[T abilityRuleGroup](group T) *abilitydetail.OptionalValues {
	if group == nil {
		return nil
	}
	value := any(group)
	switch typed := value.(type) {
	case *battlerules.AbilityPassive:
		return &typed.OptionalValues
	case *battlerules.AbilityReactive:
		return &typed.OptionalValues
	case *battlerules.AbilityOnSwitchIn:
		return &typed.OptionalValues
	case *battlerules.AbilityOnSwitchOut:
		return &typed.OptionalValues
	case *battlerules.AbilityOnDamage:
		return &typed.OptionalValues
	case *battlerules.AbilityOnTurnEnd:
		return &typed.OptionalValues
	case *battlerules.AbilityOnEnvironmentChange:
		return &typed.OptionalValues
	case *battlerules.AbilityOnTerastallization:
		return &typed.OptionalValues
	default:
		return nil
	}
}

func abilityRuleGroupMessage(values *abilitydetail.OptionalValues) *domainv1.GameAbilityRuleGroup {
	if values == nil {
		return nil
	}
	return gameAbilityRuleGroupMessage(abilitydetail.RuleSet{OptionalValues: *values})
}
