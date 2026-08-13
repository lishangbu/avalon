package api

import (
	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
)

func gameAbilityRuleGroupMessage(value abilitydetail.RuleSet) *domainv1.GameAbilityRuleGroup {
	return &domainv1.GameAbilityRuleGroup{
		WeatherDamageImmunities:                      gameAbilityWeatherDamageImmunities(value.WeatherDamageImmunities),
		WeatherEffectsSuppressed:                     value.WeatherEffectsSuppressed,
		ReactiveAbilityRules:                         gameReactiveAbilityRulesMessage(value.ReactiveAbilityRules),
		BasePowerAtMostDamageBoost:                   gameAbilityBasePowerAtMostDamageBoostMessage(value.BasePowerAtMostDamageBoost),
		RecoilSkillDamageBoost:                       gameAbilityRecoilSkillDamageBoostMessage(value.RecoilSkillDamageBoost),
		LowHpElementDamageBoost:                      gameAbilityLowHPElementDamageBoostMessage(value.LowHPElementDamageBoost),
		WeatherElementDamageBoost:                    gameAbilityWeatherElementDamageBoostMessage(value.WeatherElementDamageBoost),
		ElementSkillDamageBoost:                      gameAbilityElementSkillDamageBoostMessage(value.ElementSkillDamageBoost),
		SameElementBonusOverride:                     gameAbilitySameElementBonusOverrideMessage(value.SameElementBonusOverride),
		ContactBasedSkillDamageBoost:                 gameAbilityContactBasedSkillDamageBoostMessage(value.ContactBasedSkillDamageBoost),
		CriticalHitDamageBoost:                       gameAbilityCriticalHitDamageBoostMessage(value.CriticalHitDamageBoost),
		SuperEffectiveDamageBoost:                    gameAbilitySuperEffectiveDamageBoostMessage(value.SuperEffectiveDamageBoost),
		NotVeryEffectiveDamageBoost:                  gameAbilityNotVeryEffectiveDamageBoostMessage(value.NotVeryEffectiveDamageBoost),
		TargetGenderDamageMultiplier:                 targetGenderMultiplierMessage(value.TargetGenderDamageMultiplier),
		PunchBasedSkillDamageBoost:                   punchMultiplierMessage(value.PunchBasedSkillDamageBoost),
		SlicingBasedSkillDamageBoost:                 slicingMultiplierMessage(value.SlicingBasedSkillDamageBoost),
		SoundBasedSkillDamageBoost:                   soundBoostMultiplierMessage(value.SoundBasedSkillDamageBoost),
		PulseBasedSkillDamageBoost:                   pulseMultiplierMessage(value.PulseBasedSkillDamageBoost),
		BiteBasedSkillDamageBoost:                    biteMultiplierMessage(value.BiteBasedSkillDamageBoost),
		SecondaryEffectsSuppressedDamageBoost:        suppressedMultiplierMessage(value.SecondaryEffectsSuppressedDamageBoost),
		SoundBasedSkillDamageReduction:               soundReductionMultiplierMessage(value.SoundBasedSkillDamageReduction),
		SuperEffectiveDamageReduction:                superReductionMultiplierMessage(value.SuperEffectiveDamageReduction),
		FullHpDamageReduction:                        fullHPMultiplierMessage(value.FullHPDamageReduction),
		DamageClassDamageReduction:                   damageClassReductionMessage(value.DamageClassDamageReduction),
		ElementSkillDamageReduction:                  elementReductionMessage(value.ElementSkillDamageReduction),
		ContactBasedSkillDamageReduction:             contactReductionMultiplierMessage(value.ContactBasedSkillDamageReduction),
		AttackingStatMultiplier:                      attackingStatMultiplierMessage(value.AttackingStatMultiplier),
		OpponentAttackingStatMultiplier:              opponentAttackingStatMultiplierMessage(value.OpponentAttackingStatMultiplier),
		DefendingStatMultiplier:                      defendingStatMultiplierMessage(value.DefendingStatMultiplier),
		OpponentDefendingStatMultiplier:              opponentDefendingStatMultiplierMessage(value.OpponentDefendingStatMultiplier),
		AllySkillDamageBoost:                         allySkillDamageBoostMessage(value.AllySkillDamageBoost),
		AllyReceivedDamageReduction:                  allyReductionMultiplierMessage(value.AllyReceivedDamageReduction),
		AllyAbilityGroupCode:                         value.AllyAbilityGroupCode,
		AllyAbilityPresenceAttackingStatMultiplier:   allyPresenceMultiplierMessage(value.AllyAbilityPresenceAttackingStatMultiplier),
		AccuracyMultiplier:                           gameAbilityAccuracyMultiplierMessage(value.AccuracyMultiplier),
		PhysicalSkillAccuracyMultiplier:              gameAbilityAccuracyMultiplierMessage(value.PhysicalSkillAccuracyMultiplier),
		OpponentAccuracySandstormMultiplier:          gameAbilityAccuracyMultiplierMessage(value.OpponentAccuracySandstormMultiplier),
		OpponentAccuracySnowMultiplier:               gameAbilityAccuracyMultiplierMessage(value.OpponentAccuracySnowMultiplier),
		OpponentAccuracyConfusionMultiplier:          gameAbilityAccuracyMultiplierMessage(value.OpponentAccuracyConfusionMultiplier),
		AccuracyAlwaysHits:                           value.AccuracyAlwaysHits,
		StatusSkillAccuracyCap:                       value.StatusSkillAccuracyCap,
		IgnoreOpponentAccuracyStatStages:             value.IgnoreOpponentAccuracyStatStages,
		CriticalHitImmunity:                          value.CriticalHitImmunity,
		SkillRecoilDamageImmunity:                    value.SkillRecoilDamageImmunity,
		IndirectDamageImmunity:                       value.IndirectDamageImmunity,
		ContactDamageToAttackerDenominator:           value.ContactDamageToAttackerDenominator,
		IgnoreOpponentDamageStatStages:               value.IgnoreOpponentDamageStatStages,
		IgnoreTargetAbilityEffects:                   value.IgnoreTargetAbilityEffects,
		SurviveFatalDamageAtFullHp:                   value.SurviveFatalDamageAtFullHP,
		OpponentStatusSkillImmunity:                  value.OpponentStatusSkillImmunity,
		NonSuperEffectiveDamageImmunity:              value.NonSuperEffectiveDamageImmunity,
		CriticalHitStageBoost:                        value.CriticalHitStageBoost,
		MultiHitMaximum:                              value.MultiHitMaximum,
		DamagingSkillSecondaryEffectImmunity:         value.DamagingSkillSecondaryEffectImmunity,
		PriorityMoveImmunityForSideEnabled:           value.PriorityMoveImmunityForSideEnabled,
		PriorityMoveImmunityForSideProtectsAllies:    value.PriorityMoveImmunityForSideProtectsAllies,
		StatusSkillMovesLastAndIgnoresTargetAbility:  value.StatusSkillMovesLastAndIgnoresTargetAbility,
		ContactSkillProtectionBypass:                 value.ContactSkillProtectionBypass,
		ContactSkillProtectionBypassDamageMultiplier: gameAbilityProtectionBypassDamageMultiplierMessage(value.ContactSkillProtectionBypassDamageMultiplier),
		SkillWeatherOverride:                         gameAbilitySkillWeatherOverrideMessage(value.SkillWeatherOverride),
		SkillElementConversion:                       gameAbilitySkillElementConversionMessage(value.SkillElementConversion),
		ContactSuppression:                           value.ContactSuppression,
		ReceivedContactDamageHalved:                  value.ReceivedContactDamageHalved,
		ReceivedFireDamageDoubled:                    value.ReceivedFireDamageDoubled,
		ForcedSwitchImmunity:                         value.ForcedSwitchImmunity,
		OpponentSwitchRestriction:                    gameAbilityOpponentSwitchRestrictionMessage(value.OpponentSwitchRestriction),
		DamageCrossedHalfHpForceSelfSwitch:           value.DamageCrossedHalfHPForceSelfSwitch,
		WeatherEndTurnHeal:                           gameAbilityWeatherEndTurnHealMessage(value.WeatherEndTurnHeal),
		WeatherSpeedMultipliers:                      gameAbilityWeatherSpeedMultiplierMessages(value.WeatherSpeedMultipliers),
		EnvironmentHighestStatMultiplier:             gameAbilityEnvironmentHighestStatMultiplierMessage(value.EnvironmentHighestStatMultiplier),
		SwitchInStrongWeather:                        gameAbilitySwitchInStrongWeatherMessage(value.SwitchInStrongWeather),
		SwitchInWeather:                              gameAbilitySwitchInWeatherMessage(value.SwitchInWeather),
		SwitchInTerrain:                              gameAbilitySwitchInTerrainMessage(value.SwitchInTerrain),
		SwitchInStatStageChange:                      gameAbilitySwitchInStatStageChangeMessage(value.SwitchInStatStageChange),
		SwitchInAllyHeal:                             gameAbilitySwitchInAllyHealMessage(value.SwitchInAllyHeal),
		SwitchInOpponentDefenseComparisonBoost:       value.SwitchInOpponentDefenseComparisonBoost,
		SwitchInAllyStatStageCopy:                    value.SwitchInAllyStatStageCopy,
		SwitchInAllyStatStageReset:                   value.SwitchInAllyStatStageReset,
		SwitchInClearAllSideDamageReductions:         value.SwitchInClearAllSideDamageReductions,
		SwitchInCopyOpponentAbility:                  value.SwitchInCopyOpponentAbility,
		SwitchInRevealOpponentHeldItems:              value.SwitchInRevealOpponentHeldItems,
		SwitchInRevealOpponentHighestPowerSkill:      value.SwitchInRevealOpponentHighestPowerSkill,
		SwitchInTransformIntoOpponent:                value.SwitchInTransformIntoOpponent,
		SwitchInDetectDangerousOpponentSkill:         value.SwitchInDetectDangerousOpponentSkill,
		SwitchInDisguiseAsLastHealthyAlly:            value.SwitchInDisguiseAsLastHealthyAlly,
		SwitchInHeldItemElementIdentity:              value.SwitchInHeldItemElementIdentity,
		SwitchOutMajorStatusCure:                     value.SwitchOutMajorStatusCure,
		SwitchOutHealDenominator:                     value.SwitchOutHealDenominator,
		SwitchOutFormChange:                          gameAbilitySwitchOutFormChangeMessage(value.SwitchOutFormChange),
		SwitchInFormChange:                           gameAbilitySwitchInFormChangeMessage(value.SwitchInFormChange),
		WeatherFormChange:                            gameAbilityWeatherFormChangeMessage(value.WeatherFormChange),
		TerastallizationStatStageChange:              gameAbilityTerastallizationStatStageChangeMessage(value.TerastallizationStatStageChange),
		TerastallizationEnvironmentClear:             value.TerastallizationEnvironmentClear,
	}
}

// abilityReactiveAbilityRulesFromMessage 将管理 API 的反应型特性消息转换为 Battle Engine 冻结规则。
// 所有窄整数都在转换前校验范围，防止 int32 到 uint8、uint16 或 int8 的截断改变资料语义。
func abilityReactiveAbilityRulesFromMessage(value *domainv1.GameReactiveAbilityRules) (*battleengine.ReactiveAbilityRules, error) {
	if value == nil {
		return nil, nil
	}
	rules := &battleengine.ReactiveAbilityRules{
		FaintHighestStatBoost:             value.GetFaintHighestStatBoost(),
		ExplosionEffectSuppression:        value.GetExplosionEffectSuppression(),
		ReceivedDamageAttackerMajorStatus: battleengine.MajorStatus(value.GetReceivedDamageAttackerMajorStatus()),
	}
	var ok bool
	if rules.EndTurnAllyMajorStatusCureChance, ok = reactiveUint8(value.GetEndTurnAllyMajorStatusCureChance()); !ok {
		return nil, invalidReactiveAbilityRules()
	}
	var err error
	if rules.EndTurnStatStageChanges, err = reactiveStatStageDeltas(value.GetEndTurnStatStageChanges()); err != nil {
		return nil, err
	}
	if rules.OncePerBattleCausedFaintMultiStatBoost, err = reactiveStatStageDeltas(value.GetOncePerBattleCausedFaintMultiStatBoost()); err != nil {
		return nil, err
	}
	if rules.DamageCrossedHalfHPStatStageChanges, err = reactiveStatStageDeltas(value.GetDamageCrossedHalfHpStatStageChanges()); err != nil {
		return nil, err
	}
	if v := value.GetMajorStatusEndTurnHealing(); v != nil {
		denominator, valid := reactiveUint16(v.GetDenominator())
		if !valid {
			return nil, invalidReactiveAbilityRules()
		}
		rules.MajorStatusEndTurnHealing = &battleengine.MajorStatusEndTurnHealing{Statuses: reactiveMajorStatuses(v.GetStatuses()), Denominator: denominator}
	}
	if v := value.GetWeatherEndTurnDamage(); v != nil {
		denominator, valid := reactiveUint16(v.GetDenominator())
		if !valid {
			return nil, invalidReactiveAbilityRules()
		}
		rules.WeatherEndTurnDamage = &battleengine.WeatherEndTurnDamage{Weathers: reactiveWeathers(v.GetWeathers()), Denominator: denominator}
	}
	if v := value.GetOpponentMajorStatusEndTurnDamage(); v != nil {
		denominator, valid := reactiveUint16(v.GetDenominator())
		if !valid {
			return nil, invalidReactiveAbilityRules()
		}
		rules.OpponentMajorStatusEndTurnDamage = &battleengine.OpponentMajorStatusEndTurnDamage{Statuses: reactiveMajorStatuses(v.GetStatuses()), Denominator: denominator}
	}
	if v := value.GetEndTurnMajorStatusCure(); v != nil {
		chance, valid := reactiveUint8(v.GetChancePercent())
		if !valid {
			return nil, invalidReactiveAbilityRules()
		}
		rules.EndTurnMajorStatusCure = &battleengine.EndTurnMajorStatusCure{ChancePercent: chance, RequiredWeathers: reactiveWeathers(v.GetRequiredWeathers())}
	}
	if v := value.GetEndTurnRandomStatStageChange(); v != nil {
		raise, validRaise := reactiveInt8(v.GetRaiseDelta())
		lower, validLower := reactiveInt8(v.GetLowerDelta())
		if !validRaise || !validLower {
			return nil, invalidReactiveAbilityRules()
		}
		rules.EndTurnRandomStatStageChange = &battleengine.EndTurnRandomStatStageChange{RaiseDelta: raise, LowerDelta: lower}
	}
	for _, v := range value.GetFaintStatStageBoosts() {
		delta, valid := reactiveInt8(v.GetDelta())
		if !valid {
			return nil, invalidReactiveAbilityRules()
		}
		rules.FaintStatStageBoosts = append(rules.FaintStatStageBoosts, battleengine.FaintStatStageBoost{Stat: battleengine.Stat(v.GetStat()), Delta: delta, RequiresCausedFaint: v.GetRequiresCausedFaint()})
	}
	if v := value.GetFaintAttackerDamage(); v != nil {
		denominator, valid := reactiveUint16(v.GetAttackerMaxHpDenominator())
		if !valid {
			return nil, invalidReactiveAbilityRules()
		}
		rules.FaintAttackerDamage = &battleengine.FaintAttackerDamage{RequiresContact: v.GetRequiresContact(), AttackerMaxHPDenominator: denominator, UsesDamageTaken: v.GetUsesDamageTaken(), SuppressedByExplosionSuppression: v.GetSuppressedByExplosionSuppression()}
	}
	if v := value.GetCriticalDamageSetStatStage(); v != nil {
		delta, valid := reactiveInt8(v.GetDelta())
		if !valid {
			return nil, invalidReactiveAbilityRules()
		}
		rules.CriticalDamageSetStatStage = &battleengine.StatStageDelta{Stat: battleengine.Stat(v.GetStat()), Delta: delta}
	}
	for _, v := range value.GetReceivedDamageStatStageChanges() {
		changes, convertErr := reactiveStatStageDeltas(v.GetChanges())
		if convertErr != nil {
			return nil, convertErr
		}
		elementIDs, convertErr := gameDataIdentifiers(v.GetElementIds(), "INVALID_REACTIVE_ABILITY_RULES")
		if convertErr != nil {
			return nil, convertErr
		}
		rules.ReceivedDamageStatStageChanges = append(rules.ReceivedDamageStatStageChanges, battleengine.ReceivedDamageStatStageChange{Changes: changes, RequiresContact: v.GetRequiresContact(), ChangesAttacker: v.GetChangesAttacker(), ElementIDs: elementIDs})
	}
	if v := value.GetReceivedDamageCharge(); v != nil {
		numerator, validNumerator := reactiveUint16(v.GetNumerator())
		denominator, validDenominator := reactiveUint16(v.GetDenominator())
		if !validNumerator || !validDenominator {
			return nil, invalidReactiveAbilityRules()
		}
		elementID, convertErr := gameDataIdentifier(v.GetElementId(), "INVALID_REACTIVE_ABILITY_RULES")
		if convertErr != nil {
			return nil, convertErr
		}
		rules.ReceivedDamageCharge = &battleengine.ReceivedDamageCharge{ElementID: elementID, Numerator: numerator, Denominator: denominator}
	}
	if err := battleengine.ValidateReactiveAbilityRules(rules); err != nil {
		return nil, invalidReactiveAbilityRules()
	}
	return rules, nil
}

// abilityProtectionBypassDamageMultiplierFromMessage 将管理 API 的保护穿透伤害倍率转换为引擎规则。
// 省略消息表示穿透保护后保持完整伤害；显式倍率必须是 uint16 范围内的正整数分数。
func abilityProtectionBypassDamageMultiplierFromMessage(value *domainv1.GameAbilityProtectionBypassDamageMultiplier) (*battleengine.DamageFraction, error) {
	if value == nil {
		return nil, nil
	}
	numerator, validNumerator := reactiveUint16(value.GetNumerator())
	denominator, validDenominator := reactiveUint16(value.GetDenominator())
	result := &battleengine.DamageFraction{Numerator: numerator, Denominator: denominator}
	if !validNumerator || !validDenominator || battleengine.ValidateDamageFraction(result) != nil {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_PROTECTION_BYPASS_DAMAGE_MULTIPLIER", "保护穿透伤害倍率无效")
	}
	return result, nil
}

// abilitySkillWeatherOverrideFromMessage 将管理 API 的普通天气枚举转换为技能使用时的局部天气语义。
// 未指定枚举表示不覆盖天气；该规则不会建立或修改全场环境天气。
func abilitySkillWeatherOverrideFromMessage(value domainv1.GameSkillWeatherKind) (battleengine.WeatherKind, error) {
	if value == domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_UNSPECIFIED {
		return "", nil
	}
	weather, err := abilityWeatherKindFromMessage(value, "INVALID_ABILITY_SKILL_WEATHER_OVERRIDE")
	return battleengine.WeatherKind(weather), err
}

// abilitySkillElementConversionFromMessage 将管理 API 的技能属性转换消息转换为引擎规则。
// 两端属性身份都必须是稳定 Identifier，且转换倍率必须是 uint16 范围内的正整数分数。
func abilitySkillElementConversionFromMessage(value *domainv1.GameAbilitySkillElementConversion) (*battleengine.SkillElementConversion, error) {
	if value == nil {
		return nil, nil
	}
	sourceElementID, err := gameDataIdentifier(value.GetSourceElementId(), "INVALID_ABILITY_SKILL_ELEMENT_CONVERSION")
	if err != nil {
		return nil, err
	}
	targetElementID, err := gameDataIdentifier(value.GetTargetElementId(), "INVALID_ABILITY_SKILL_ELEMENT_CONVERSION")
	if err != nil {
		return nil, err
	}
	damageNumerator, validNumerator := reactiveUint16(value.GetDamageNumerator())
	damageDenominator, validDenominator := reactiveUint16(value.GetDamageDenominator())
	result := &battleengine.SkillElementConversion{
		SourceElementID: sourceElementID, TargetElementID: targetElementID,
		DamageNumerator: damageNumerator, DamageDenominator: damageDenominator,
	}
	if !validNumerator || !validDenominator || battleengine.ValidateSkillElementConversion(result) != nil {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SKILL_ELEMENT_CONVERSION", "技能属性转换规则无效")
	}
	return result, nil
}

// gameAbilityProtectionBypassDamageMultiplierMessage 将引擎保护穿透伤害倍率映射为管理 API 消息。
func gameAbilityProtectionBypassDamageMultiplierMessage(value *battleengine.DamageFraction) *domainv1.GameAbilityProtectionBypassDamageMultiplier {
	if value == nil {
		return nil
	}
	return &domainv1.GameAbilityProtectionBypassDamageMultiplier{Numerator: int32(value.Numerator), Denominator: int32(value.Denominator)}
}

// gameAbilitySkillWeatherOverrideMessage 将局部技能天气覆盖映射为管理 API 枚举。
func gameAbilitySkillWeatherOverrideMessage(value battleengine.WeatherKind) domainv1.GameSkillWeatherKind {
	weather, _ := gameAbilityWeatherKind(abilitydetail.WeatherKind(value))
	return weather
}

// gameAbilitySkillElementConversionMessage 将引擎技能属性转换规则无损映射为管理 API 消息。
func gameAbilitySkillElementConversionMessage(value *battleengine.SkillElementConversion) *domainv1.GameAbilitySkillElementConversion {
	if value == nil {
		return nil
	}
	return &domainv1.GameAbilitySkillElementConversion{
		SourceElementId: value.SourceElementID.String(), TargetElementId: value.TargetElementID.String(),
		DamageNumerator: int32(value.DamageNumerator), DamageDenominator: int32(value.DamageDenominator),
	}
}

func reactiveStatStageDeltas(values []*domainv1.GameStatStageDelta) ([]battleengine.StatStageDelta, error) {
	result := make([]battleengine.StatStageDelta, 0, len(values))
	for _, value := range values {
		delta, ok := reactiveInt8(value.GetDelta())
		if !ok {
			return nil, invalidReactiveAbilityRules()
		}
		result = append(result, battleengine.StatStageDelta{Stat: battleengine.Stat(value.GetStat()), Delta: delta})
	}
	return result, nil
}

// reactiveMajorStatuses 把传输层稳定代码集合转换为引擎主要异常集合；合法性由聚合规则校验统一判定。
func reactiveMajorStatuses(values []string) []battleengine.MajorStatus {
	result := make([]battleengine.MajorStatus, len(values))
	for index, value := range values {
		result[index] = battleengine.MajorStatus(value)
	}
	return result
}

// reactiveWeathers 把传输层稳定代码集合转换为引擎天气集合；合法性由聚合规则校验统一判定。
func reactiveWeathers(values []string) []battleengine.WeatherKind {
	result := make([]battleengine.WeatherKind, len(values))
	for index, value := range values {
		result[index] = battleengine.WeatherKind(value)
	}
	return result
}

// reactiveUint8 在窄化前验证无符号八位整数范围。
func reactiveUint8(value int32) (uint8, bool) { return uint8(value), value >= 0 && value <= 255 }

// reactiveUint16 在窄化前验证无符号十六位整数范围。
func reactiveUint16(value int32) (uint16, bool) { return uint16(value), value >= 0 && value <= 65535 }

// reactiveInt8 在窄化前验证有符号八位整数范围。
func reactiveInt8(value int32) (int8, bool) { return int8(value), value >= -128 && value <= 127 }

// invalidReactiveAbilityRules 返回管理 API 使用的稳定无效规则错误。
func invalidReactiveAbilityRules() error {
	return kratoserrors.BadRequest("INVALID_REACTIVE_ABILITY_RULES", "反应型特性规则无效")
}

// gameReactiveAbilityRulesMessage 将 Battle Engine 冻结规则无损映射为管理 API 消息。
func gameReactiveAbilityRulesMessage(value *battleengine.ReactiveAbilityRules) *domainv1.GameReactiveAbilityRules {
	if value == nil {
		return nil
	}
	message := &domainv1.GameReactiveAbilityRules{
		EndTurnStatStageChanges: reactiveStatStageDeltaMessages(value.EndTurnStatStageChanges), EndTurnAllyMajorStatusCureChance: int32(value.EndTurnAllyMajorStatusCureChance),
		OncePerBattleCausedFaintMultiStatBoost: reactiveStatStageDeltaMessages(value.OncePerBattleCausedFaintMultiStatBoost), FaintHighestStatBoost: value.FaintHighestStatBoost,
		ExplosionEffectSuppression: value.ExplosionEffectSuppression, DamageCrossedHalfHpStatStageChanges: reactiveStatStageDeltaMessages(value.DamageCrossedHalfHPStatStageChanges),
		ReceivedDamageAttackerMajorStatus: string(value.ReceivedDamageAttackerMajorStatus),
	}
	if v := value.MajorStatusEndTurnHealing; v != nil {
		message.MajorStatusEndTurnHealing = &domainv1.GameMajorStatusEndTurnHealing{Statuses: majorStatusStrings(v.Statuses), Denominator: int32(v.Denominator)}
	}
	if v := value.WeatherEndTurnDamage; v != nil {
		message.WeatherEndTurnDamage = &domainv1.GameWeatherEndTurnDamage{Weathers: weatherStrings(v.Weathers), Denominator: int32(v.Denominator)}
	}
	if v := value.OpponentMajorStatusEndTurnDamage; v != nil {
		message.OpponentMajorStatusEndTurnDamage = &domainv1.GameOpponentMajorStatusEndTurnDamage{Statuses: majorStatusStrings(v.Statuses), Denominator: int32(v.Denominator)}
	}
	if v := value.EndTurnMajorStatusCure; v != nil {
		message.EndTurnMajorStatusCure = &domainv1.GameEndTurnMajorStatusCure{ChancePercent: int32(v.ChancePercent), RequiredWeathers: weatherStrings(v.RequiredWeathers)}
	}
	if v := value.EndTurnRandomStatStageChange; v != nil {
		message.EndTurnRandomStatStageChange = &domainv1.GameEndTurnRandomStatStageChange{RaiseDelta: int32(v.RaiseDelta), LowerDelta: int32(v.LowerDelta)}
	}
	for _, v := range value.FaintStatStageBoosts {
		message.FaintStatStageBoosts = append(message.FaintStatStageBoosts, &domainv1.GameFaintStatStageBoost{Stat: string(v.Stat), Delta: int32(v.Delta), RequiresCausedFaint: v.RequiresCausedFaint})
	}
	if v := value.FaintAttackerDamage; v != nil {
		message.FaintAttackerDamage = &domainv1.GameFaintAttackerDamage{RequiresContact: v.RequiresContact, AttackerMaxHpDenominator: int32(v.AttackerMaxHPDenominator), UsesDamageTaken: v.UsesDamageTaken, SuppressedByExplosionSuppression: v.SuppressedByExplosionSuppression}
	}
	if v := value.CriticalDamageSetStatStage; v != nil {
		message.CriticalDamageSetStatStage = &domainv1.GameStatStageDelta{Stat: string(v.Stat), Delta: int32(v.Delta)}
	}
	for _, v := range value.ReceivedDamageStatStageChanges {
		message.ReceivedDamageStatStageChanges = append(message.ReceivedDamageStatStageChanges, &domainv1.GameReceivedDamageStatStageChange{Changes: reactiveStatStageDeltaMessages(v.Changes), RequiresContact: v.RequiresContact, ChangesAttacker: v.ChangesAttacker, ElementIds: identifierStrings(v.ElementIDs)})
	}
	if v := value.ReceivedDamageCharge; v != nil {
		message.ReceivedDamageCharge = &domainv1.GameReceivedDamageCharge{ElementId: v.ElementID.String(), Numerator: int32(v.Numerator), Denominator: int32(v.Denominator)}
	}
	return message
}

// reactiveStatStageDeltaMessages 把冻结能力变化集合转换为 API 消息集合。
func reactiveStatStageDeltaMessages(values []battleengine.StatStageDelta) []*domainv1.GameStatStageDelta {
	result := make([]*domainv1.GameStatStageDelta, len(values))
	for index, value := range values {
		result[index] = &domainv1.GameStatStageDelta{Stat: string(value.Stat), Delta: int32(value.Delta)}
	}
	return result
}

// majorStatusStrings 把引擎主要异常集合转换为传输层稳定代码集合。
func majorStatusStrings(values []battleengine.MajorStatus) []string {
	result := make([]string, len(values))
	for index, value := range values {
		result[index] = string(value)
	}
	return result
}

// weatherStrings 把引擎天气集合转换为传输层稳定代码集合。
func weatherStrings(values []battleengine.WeatherKind) []string {
	result := make([]string, len(values))
	for index, value := range values {
		result[index] = string(value)
	}
	return result
}

// abilityOpponentSwitchRestrictionFromMessage 将 API 的主动换人限制资料转换为领域规则。
//
// 消息缺失明确表示未声明规则；存在但所有条件为默认值仍保留为“限制所有对手”的有效规则，不能在传输边界
// 擅自折叠为 nil。属性 Identifier 只验证格式，是否存在且启用由 Battle 启动时的实时资料冻结边界复核。
func abilityOpponentSwitchRestrictionFromMessage(value *domainv1.GameAbilityOpponentSwitchRestriction) (*abilitydetail.OpponentSwitchRestriction, error) {
	if value == nil {
		return nil, nil
	}
	requiredTargetElementID, err := optionalGameDataIdentifier(value.GetRequiredTargetElementId(), "INVALID_OPPONENT_SWITCH_RESTRICTION_TARGET_ELEMENT_ID")
	if err != nil {
		return nil, err
	}
	return &abilitydetail.OpponentSwitchRestriction{
		RequiredTargetElementID:  requiredTargetElementID,
		RequiresGroundedTarget:   value.GetRequiresGroundedTarget(),
		SameEffectGrantsImmunity: value.GetSameEffectGrantsImmunity(),
	}, nil
}

// gameAbilityOpponentSwitchRestrictionMessage 将领域主动换人限制规则转换为 API 消息。
func gameAbilityOpponentSwitchRestrictionMessage(value *abilitydetail.OpponentSwitchRestriction) *domainv1.GameAbilityOpponentSwitchRestriction {
	if value == nil {
		return nil
	}
	requiredTargetElementID := ""
	if value.RequiredTargetElementID != nil {
		requiredTargetElementID = value.RequiredTargetElementID.String()
	}
	return &domainv1.GameAbilityOpponentSwitchRestriction{
		RequiredTargetElementId:  requiredTargetElementID,
		RequiresGroundedTarget:   value.RequiresGroundedTarget,
		SameEffectGrantsImmunity: value.SameEffectGrantsImmunity,
	}
}

// abilityWeatherEffectsSuppressedChange 为完整更新构造特性天气封锁开关的替换意图。
//
// false 仍需携带非 nil 指针，以区分“显式取消封锁”与省略字段并避免旧的天气封锁规则残留在资料中。
func abilityWeatherEffectsSuppressedChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// validAbilityDamageFraction 报告运输层伤害倍率是否能够无损写入领域和战斗快照。
func validAbilityDamageFraction(numerator, denominator int32) bool {
	return numerator >= 1 && numerator <= 65_535 && denominator >= 1 && denominator <= 65_535
}

// abilityDamageElementIDsFromMessages 将运输层稳定 Identifier 字符串集合转换为领域属性身份集合。
func abilityDamageElementIDsFromMessages(values []string) ([]snowflake.ID, error) {
	result := make([]snowflake.ID, len(values))
	seen := make(map[snowflake.ID]struct{}, len(values))
	for index, value := range values {
		parsed, err := gameDataIdentifier(value, "INVALID_ABILITY_DAMAGE_ELEMENT_ID")
		if err != nil {
			return nil, err
		}
		if _, duplicated := seen[parsed]; duplicated {
			return nil, kratoserrors.BadRequest("DUPLICATED_ABILITY_DAMAGE_ELEMENT_ID", "特性伤害倍率属性集合不能包含重复值")
		}
		seen[parsed] = struct{}{}
		result[index] = parsed
	}
	return result, nil
}

// gameAbilityDamageElementIDs 将领域属性身份集合映射为运输层稳定 Identifier 字符串集合。
func gameAbilityDamageElementIDs(values []snowflake.ID) []string {
	result := make([]string, len(values))
	seen := make(map[snowflake.ID]struct{}, len(values))
	for index, value := range values {
		if value == snowflake.ID(0) {
			return []string{}
		}
		if _, duplicated := seen[value]; duplicated {
			return []string{}
		}
		seen[value] = struct{}{}
		result[index] = value.String()
	}
	return result
}

// abilityBasePowerAtMostDamageBoostFromMessage 转换基础威力上限伤害强化消息。
func abilityBasePowerAtMostDamageBoostFromMessage(value *domainv1.GameAbilityBasePowerAtMostDamageBoost) (*abilitydetail.BasePowerAtMostDamageBoost, error) {
	if value == nil {
		return nil, nil
	}
	if value.GetMaximumPower() < 1 || value.GetMaximumPower() > 65_535 ||
		!validAbilityDamageFraction(value.GetNumerator(), value.GetDenominator()) {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_BASE_POWER_DAMAGE_BOOST", "特性基础威力上限伤害倍率参数无效")
	}
	return &abilitydetail.BasePowerAtMostDamageBoost{
		MaximumPower: value.GetMaximumPower(), Numerator: value.GetNumerator(), Denominator: value.GetDenominator(),
	}, nil
}

// abilityRecoilSkillDamageBoostFromMessage 转换按实际伤害反作用技能强化消息。
func abilityRecoilSkillDamageBoostFromMessage(value *domainv1.GameAbilityRecoilSkillDamageBoost) (*abilitydetail.RecoilSkillDamageBoost, error) {
	if value == nil {
		return nil, nil
	}
	if !validAbilityDamageFraction(value.GetNumerator(), value.GetDenominator()) {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_RECOIL_SKILL_DAMAGE_BOOST", "特性反作用技能伤害倍率参数无效")
	}
	return &abilitydetail.RecoilSkillDamageBoost{Numerator: value.GetNumerator(), Denominator: value.GetDenominator()}, nil
}

// abilityLowHPElementDamageBoostFromMessage 转换低生命指定属性伤害强化消息。
func abilityLowHPElementDamageBoostFromMessage(value *domainv1.GameAbilityLowHPElementDamageBoost) (*abilitydetail.LowHPElementDamageBoost, error) {
	if value == nil {
		return nil, nil
	}
	elementID, err := gameDataIdentifier(value.GetElementId(), "INVALID_ABILITY_LOW_HP_DAMAGE_ELEMENT_ID")
	if err != nil {
		return nil, err
	}
	if value.GetHpThresholdNumerator() < 1 || value.GetHpThresholdNumerator() > value.GetHpThresholdDenominator() ||
		value.GetHpThresholdDenominator() > 65_535 || !validAbilityDamageFraction(value.GetDamageNumerator(), value.GetDamageDenominator()) {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_LOW_HP_ELEMENT_DAMAGE_BOOST", "特性低生命属性伤害倍率参数无效")
	}
	return &abilitydetail.LowHPElementDamageBoost{
		ElementID: elementID, HPThresholdNumerator: value.GetHpThresholdNumerator(),
		HPThresholdDenominator: value.GetHpThresholdDenominator(), DamageNumerator: value.GetDamageNumerator(),
		DamageDenominator: value.GetDamageDenominator(),
	}, nil
}

// abilityWeatherElementDamageBoostFromMessage 转换指定天气属性伤害强化消息。
func abilityWeatherElementDamageBoostFromMessage(value *domainv1.GameAbilityWeatherElementDamageBoost) (*abilitydetail.WeatherElementDamageBoost, error) {
	if value == nil {
		return nil, nil
	}
	weather, err := abilityWeatherKindFromMessage(value.GetWeather(), "INVALID_ABILITY_WEATHER_ELEMENT_DAMAGE_BOOST")
	if err != nil {
		return nil, err
	}
	elementIDs, err := abilityDamageElementIDsFromMessages(value.GetElementIds())
	if err != nil {
		return nil, err
	}
	if len(elementIDs) == 0 || len(elementIDs) > 32 || !validAbilityDamageFraction(value.GetNumerator(), value.GetDenominator()) {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_ELEMENT_DAMAGE_BOOST", "特性天气属性伤害倍率参数无效")
	}
	return &abilitydetail.WeatherElementDamageBoost{
		Weather: weather, ElementIDs: elementIDs, Numerator: value.GetNumerator(), Denominator: value.GetDenominator(),
	}, nil
}

// abilityElementSkillDamageBoostFromMessage 转换指定有效属性伤害强化消息。
func abilityElementSkillDamageBoostFromMessage(value *domainv1.GameAbilityElementSkillDamageBoost) (*abilitydetail.ElementSkillDamageBoost, error) {
	if value == nil {
		return nil, nil
	}
	elementIDs, err := abilityDamageElementIDsFromMessages(value.GetElementIds())
	if err != nil {
		return nil, err
	}
	if len(elementIDs) == 0 || len(elementIDs) > 32 || !validAbilityDamageFraction(value.GetNumerator(), value.GetDenominator()) {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_ELEMENT_SKILL_DAMAGE_BOOST", "特性属性技能伤害倍率参数无效")
	}
	return &abilitydetail.ElementSkillDamageBoost{
		ElementIDs: elementIDs, Numerator: value.GetNumerator(), Denominator: value.GetDenominator(),
	}, nil
}

// abilitySameElementBonusOverrideFromMessage 转换属性一致加成覆盖消息。
func abilitySameElementBonusOverrideFromMessage(value *domainv1.GameAbilitySameElementBonusOverride) (*abilitydetail.SameElementBonusOverride, error) {
	if value == nil {
		return nil, nil
	}
	if !validAbilityDamageFraction(value.GetNumerator(), value.GetDenominator()) {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SAME_ELEMENT_BONUS_OVERRIDE", "特性属性一致加成倍率参数无效")
	}
	return &abilitydetail.SameElementBonusOverride{Numerator: value.GetNumerator(), Denominator: value.GetDenominator()}, nil
}

// abilityContactBasedSkillDamageBoostFromMessage 转换有效接触技能伤害强化消息。
func abilityContactBasedSkillDamageBoostFromMessage(value *domainv1.GameAbilityContactBasedSkillDamageBoost) (*abilitydetail.ContactBasedSkillDamageBoost, error) {
	if value == nil {
		return nil, nil
	}
	if !validAbilityDamageFraction(value.GetNumerator(), value.GetDenominator()) {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_CONTACT_DAMAGE_BOOST", "特性接触技能伤害倍率参数无效")
	}
	return &abilitydetail.ContactBasedSkillDamageBoost{Numerator: value.GetNumerator(), Denominator: value.GetDenominator()}, nil
}

// abilityCriticalHitDamageBoostFromMessage 转换击中要害额外伤害强化消息。
func abilityCriticalHitDamageBoostFromMessage(value *domainv1.GameAbilityCriticalHitDamageBoost) (*abilitydetail.CriticalHitDamageBoost, error) {
	if value == nil {
		return nil, nil
	}
	if !validAbilityDamageFraction(value.GetNumerator(), value.GetDenominator()) {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_CRITICAL_HIT_DAMAGE_BOOST", "特性击中要害伤害倍率参数无效")
	}
	return &abilitydetail.CriticalHitDamageBoost{Numerator: value.GetNumerator(), Denominator: value.GetDenominator()}, nil
}

// abilitySuperEffectiveDamageBoostFromMessage 转换严格克制伤害强化消息。
func abilitySuperEffectiveDamageBoostFromMessage(value *domainv1.GameAbilitySuperEffectiveDamageBoost) (*abilitydetail.SuperEffectiveDamageBoost, error) {
	if value == nil {
		return nil, nil
	}
	if !validAbilityDamageFraction(value.GetNumerator(), value.GetDenominator()) {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SUPER_EFFECTIVE_DAMAGE_BOOST", "特性严格克制伤害倍率参数无效")
	}
	return &abilitydetail.SuperEffectiveDamageBoost{Numerator: value.GetNumerator(), Denominator: value.GetDenominator()}, nil
}

// abilityNotVeryEffectiveDamageBoostFromMessage 转换非零抗性伤害强化消息。
func abilityNotVeryEffectiveDamageBoostFromMessage(value *domainv1.GameAbilityNotVeryEffectiveDamageBoost) (*abilitydetail.NotVeryEffectiveDamageBoost, error) {
	if value == nil {
		return nil, nil
	}
	if !validAbilityDamageFraction(value.GetNumerator(), value.GetDenominator()) {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_NOT_VERY_EFFECTIVE_DAMAGE_BOOST", "特性非零抗性伤害倍率参数无效")
	}
	return &abilitydetail.NotVeryEffectiveDamageBoost{Numerator: value.GetNumerator(), Denominator: value.GetDenominator()}, nil
}

// gameAbilityBasePowerAtMostDamageBoostMessage 映射基础威力上限伤害强化领域值。
func gameAbilityBasePowerAtMostDamageBoostMessage(value *abilitydetail.BasePowerAtMostDamageBoost) *domainv1.GameAbilityBasePowerAtMostDamageBoost {
	if value == nil || value.MaximumPower < 1 || value.MaximumPower > 65_535 || !validAbilityDamageFraction(value.Numerator, value.Denominator) {
		return nil
	}
	return &domainv1.GameAbilityBasePowerAtMostDamageBoost{MaximumPower: value.MaximumPower, Numerator: value.Numerator, Denominator: value.Denominator}
}

// gameAbilityRecoilSkillDamageBoostMessage 映射按实际伤害反作用技能强化领域值。
func gameAbilityRecoilSkillDamageBoostMessage(value *abilitydetail.RecoilSkillDamageBoost) *domainv1.GameAbilityRecoilSkillDamageBoost {
	if value == nil || !validAbilityDamageFraction(value.Numerator, value.Denominator) {
		return nil
	}
	return &domainv1.GameAbilityRecoilSkillDamageBoost{Numerator: value.Numerator, Denominator: value.Denominator}
}

// gameAbilityLowHPElementDamageBoostMessage 映射低生命指定属性伤害强化领域值。
func gameAbilityLowHPElementDamageBoostMessage(value *abilitydetail.LowHPElementDamageBoost) *domainv1.GameAbilityLowHPElementDamageBoost {
	if value == nil || value.ElementID == snowflake.ID(0) || value.HPThresholdNumerator < 1 ||
		value.HPThresholdNumerator > value.HPThresholdDenominator || value.HPThresholdDenominator > 65_535 ||
		!validAbilityDamageFraction(value.DamageNumerator, value.DamageDenominator) {
		return nil
	}
	return &domainv1.GameAbilityLowHPElementDamageBoost{
		ElementId: value.ElementID.String(), HpThresholdNumerator: value.HPThresholdNumerator,
		HpThresholdDenominator: value.HPThresholdDenominator, DamageNumerator: value.DamageNumerator,
		DamageDenominator: value.DamageDenominator,
	}
}

// gameAbilityWeatherElementDamageBoostMessage 映射指定天气属性伤害强化领域值。
func gameAbilityWeatherElementDamageBoostMessage(value *abilitydetail.WeatherElementDamageBoost) *domainv1.GameAbilityWeatherElementDamageBoost {
	if value == nil || len(value.ElementIDs) == 0 || len(value.ElementIDs) > 32 || !validAbilityDamageFraction(value.Numerator, value.Denominator) {
		return nil
	}
	weather, valid := gameAbilityWeatherKind(value.Weather)
	if !valid {
		return nil
	}
	elementIDs := gameAbilityDamageElementIDs(value.ElementIDs)
	if len(elementIDs) != len(value.ElementIDs) {
		return nil
	}
	return &domainv1.GameAbilityWeatherElementDamageBoost{
		Weather: weather, ElementIds: elementIDs, Numerator: value.Numerator, Denominator: value.Denominator,
	}
}

// gameAbilityElementSkillDamageBoostMessage 映射指定有效属性伤害强化领域值。
func gameAbilityElementSkillDamageBoostMessage(value *abilitydetail.ElementSkillDamageBoost) *domainv1.GameAbilityElementSkillDamageBoost {
	if value == nil || len(value.ElementIDs) == 0 || len(value.ElementIDs) > 32 || !validAbilityDamageFraction(value.Numerator, value.Denominator) {
		return nil
	}
	elementIDs := gameAbilityDamageElementIDs(value.ElementIDs)
	if len(elementIDs) != len(value.ElementIDs) {
		return nil
	}
	return &domainv1.GameAbilityElementSkillDamageBoost{ElementIds: elementIDs, Numerator: value.Numerator, Denominator: value.Denominator}
}

// gameAbilitySameElementBonusOverrideMessage 映射属性一致加成覆盖领域值。
func gameAbilitySameElementBonusOverrideMessage(value *abilitydetail.SameElementBonusOverride) *domainv1.GameAbilitySameElementBonusOverride {
	if value == nil || !validAbilityDamageFraction(value.Numerator, value.Denominator) {
		return nil
	}
	return &domainv1.GameAbilitySameElementBonusOverride{Numerator: value.Numerator, Denominator: value.Denominator}
}

// gameAbilityContactBasedSkillDamageBoostMessage 映射有效接触技能伤害强化领域值。
func gameAbilityContactBasedSkillDamageBoostMessage(value *abilitydetail.ContactBasedSkillDamageBoost) *domainv1.GameAbilityContactBasedSkillDamageBoost {
	if value == nil || !validAbilityDamageFraction(value.Numerator, value.Denominator) {
		return nil
	}
	return &domainv1.GameAbilityContactBasedSkillDamageBoost{Numerator: value.Numerator, Denominator: value.Denominator}
}

// gameAbilityCriticalHitDamageBoostMessage 映射击中要害额外伤害强化领域值。
func gameAbilityCriticalHitDamageBoostMessage(value *abilitydetail.CriticalHitDamageBoost) *domainv1.GameAbilityCriticalHitDamageBoost {
	if value == nil || !validAbilityDamageFraction(value.Numerator, value.Denominator) {
		return nil
	}
	return &domainv1.GameAbilityCriticalHitDamageBoost{Numerator: value.Numerator, Denominator: value.Denominator}
}

// gameAbilitySuperEffectiveDamageBoostMessage 映射严格克制伤害强化领域值。
func gameAbilitySuperEffectiveDamageBoostMessage(value *abilitydetail.SuperEffectiveDamageBoost) *domainv1.GameAbilitySuperEffectiveDamageBoost {
	if value == nil || !validAbilityDamageFraction(value.Numerator, value.Denominator) {
		return nil
	}
	return &domainv1.GameAbilitySuperEffectiveDamageBoost{Numerator: value.Numerator, Denominator: value.Denominator}
}

// gameAbilityNotVeryEffectiveDamageBoostMessage 映射非零抗性伤害强化领域值。
func gameAbilityNotVeryEffectiveDamageBoostMessage(value *abilitydetail.NotVeryEffectiveDamageBoost) *domainv1.GameAbilityNotVeryEffectiveDamageBoost {
	if value == nil || !validAbilityDamageFraction(value.Numerator, value.Denominator) {
		return nil
	}
	return &domainv1.GameAbilityNotVeryEffectiveDamageBoost{Numerator: value.Numerator, Denominator: value.Denominator}
}

// abilityAccuracyMultiplierFromMessage 将管理 API 的可选命中分数转换为领域规则。
//
// 空消息表示显式不声明；分子和分母必须为正且在数据库约束范围内，避免把浮点误差或零分母带入权威对局快照。
func abilityAccuracyMultiplierFromMessage(value *domainv1.GameAbilityAccuracyMultiplier) (*abilitydetail.AccuracyMultiplier, error) {
	if value == nil {
		return nil, nil
	}
	if value.GetNumerator() < 1 || value.GetNumerator() > 65_535 || value.GetDenominator() < 1 || value.GetDenominator() > 65_535 {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_ACCURACY_MULTIPLIER", "特性命中倍率参数无效")
	}
	return &abilitydetail.AccuracyMultiplier{Numerator: value.GetNumerator(), Denominator: value.GetDenominator()}, nil
}

// gameAbilityAccuracyMultiplierMessage 将领域命中分数映射为管理 API 的可选运输消息。
//
// 读取到损坏的领域分数时返回 nil 而非猜测倍率；Battle 编译边界仍会拒绝该资料，保证展示与执行不会产生不同解释。
func gameAbilityAccuracyMultiplierMessage(value *abilitydetail.AccuracyMultiplier) *domainv1.GameAbilityAccuracyMultiplier {
	if value == nil || value.Numerator < 1 || value.Numerator > 65_535 || value.Denominator < 1 || value.Denominator > 65_535 {
		return nil
	}
	return &domainv1.GameAbilityAccuracyMultiplier{Numerator: value.Numerator, Denominator: value.Denominator}
}

// abilityAccuracyAlwaysHitsChange 为完整更新构造普通命中跳过开关的替换意图。
func abilityAccuracyAlwaysHitsChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityStatusSkillAccuracyCapChange 为完整更新构造变化技能最终命中上限的替换意图。
func abilityStatusSkillAccuracyCapChange(value int32) abilitydetail.Change[int32] {
	return abilitydetail.Change[int32]{Specified: true, Value: &value}
}

// abilityIgnoreOpponentAccuracyStatStagesChange 为完整更新构造无视对手命中或闪避阶级开关的替换意图。
func abilityIgnoreOpponentAccuracyStatStagesChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityCriticalHitImmunityChange 为完整更新构造击中要害免疫开关的替换意图。
func abilityCriticalHitImmunityChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySkillRecoilDamageImmunityChange 为完整更新构造技能反作用伤害免疫开关的替换意图。
func abilitySkillRecoilDamageImmunityChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityIndirectDamageImmunityChange 为完整更新构造间接伤害免疫开关的替换意图。
func abilityIndirectDamageImmunityChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityContactDamageToAttackerDenominatorChange 为完整更新构造接触反伤分母的替换意图。
// 0 会显式清除接触反伤规则。
func abilityContactDamageToAttackerDenominatorChange(value int32) abilitydetail.Change[int32] {
	return abilitydetail.Change[int32]{Specified: true, Value: &value}
}

// abilityIgnoreOpponentDamageStatStagesChange 为完整更新构造无视对手伤害能力阶级开关的替换意图。
func abilityIgnoreOpponentDamageStatStagesChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityIgnoreTargetAbilityEffectsChange 为完整更新构造无视目标防守特性开关的替换意图。
func abilityIgnoreTargetAbilityEffectsChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySurviveFatalDamageAtFullHPChange 为完整更新构造满生命致命伤害保留 1 HP 开关的替换意图。
func abilitySurviveFatalDamageAtFullHPChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityOpponentStatusSkillImmunityChange 为完整更新构造免疫对手变化技能开关的替换意图。
func abilityOpponentStatusSkillImmunityChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityNonSuperEffectiveDamageImmunityChange 为完整更新构造免疫非克制伤害技能开关的替换意图。
func abilityNonSuperEffectiveDamageImmunityChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityCriticalHitStageBoostChange 为完整更新构造固定要害等级增益的替换意图。
func abilityCriticalHitStageBoostChange(value int32) abilitydetail.Change[int32] {
	return abilitydetail.Change[int32]{Specified: true, Value: &value}
}

// abilityMultiHitMaximumChange 为完整更新构造可变连续命中取最大段数开关的替换意图。
func abilityMultiHitMaximumChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityDamagingSkillSecondaryEffectImmunityChange 为完整更新构造伤害技能追加效果免疫开关的替换意图。
func abilityDamagingSkillSecondaryEffectImmunityChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityPriorityMoveImmunityForSideEnabledChange 为完整更新构造先制技能侧免疫开关的替换意图。
func abilityPriorityMoveImmunityForSideEnabledChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityPriorityMoveImmunityForSideProtectsAlliesChange 为完整更新构造先制技能侧免疫伙伴保护范围的替换意图。
func abilityPriorityMoveImmunityForSideProtectsAlliesChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityStatusSkillMovesLastAndIgnoresTargetAbilityChange 为完整更新构造变化技能后置及特性穿透开关的替换意图。
func abilityStatusSkillMovesLastAndIgnoresTargetAbilityChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityContactSkillProtectionBypassChange 为完整更新构造接触技能穿透个人保护开关的替换意图。
func abilityContactSkillProtectionBypassChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityContactSuppressionChange 为完整更新构造特性接触抑制开关的替换意图。
func abilityContactSuppressionChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityReceivedContactDamageHalvedChange 为完整更新构造承受接触伤害减半开关的替换意图。
func abilityReceivedContactDamageHalvedChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityReceivedFireDamageDoubledChange 为完整更新构造承受火属性伤害翻倍开关的替换意图。
func abilityReceivedFireDamageDoubledChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityForcedSwitchImmunityChange 为完整更新构造特性强制换人免疫开关的替换意图。
//
// false 仍需携带非 nil 指针，以区分“显式取消免疫”与省略字段，避免旧规则在完整替换后继续生效。
func abilityForcedSwitchImmunityChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityDamageCrossedHalfHPForceSelfSwitchChange 为完整更新构造半血跨越强制自换开关的替换意图。
func abilityDamageCrossedHalfHPForceSelfSwitchChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySwitchOutMajorStatusCureChange 为完整更新构造成功离场主要异常净化开关的替换意图。
func abilitySwitchOutMajorStatusCureChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityTerastallizationEnvironmentClearChange 为完整更新构造太晶化普通环境清除开关的替换意图。
//
// false 仍需携带非 nil 指针，以区分“显式取消清除”与省略字段，避免旧的太晶化环境清除规则残留在实时资料中。
func abilityTerastallizationEnvironmentClearChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySwitchInOpponentDefenseComparisonBoostChange 为完整更新构造入场防御比较强化开关的替换意图。
//
// false 仍需携带非 nil 指针，以区分显式取消与省略字段，避免旧的入场强化规则残留在实时资料中。
func abilitySwitchInOpponentDefenseComparisonBoostChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySwitchInHeldItemElementIdentityChange 为完整更新构造入场携带道具属性身份替换开关的替换意图。
//
// false 必须保持非 nil，避免完整替换时把“显式禁用”误当作省略字段而保留旧规则。
func abilitySwitchInHeldItemElementIdentityChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySwitchInAllyStatStageCopyChange 为完整更新构造入场同侧能力阶级复制开关的替换意图。
//
// false 仍需携带非 nil 指针，以区分显式取消与省略字段，避免旧的入场复制规则残留在实时资料中。
func abilitySwitchInAllyStatStageCopyChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySwitchInAllyStatStageResetChange 为完整更新构造入场同侧能力阶级重置开关的替换意图。
//
// false 仍需携带非 nil 指针，以区分显式取消与省略字段，避免旧的入场重置规则残留在实时资料中。
func abilitySwitchInAllyStatStageResetChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySwitchInClearAllSideDamageReductionsChange 为完整更新构造入场全阵营减伤屏障清除开关的替换意图。
//
// false 仍需携带非 nil 指针，以区分显式取消与省略字段，避免旧的入场清除规则残留在实时资料中。
func abilitySwitchInClearAllSideDamageReductionsChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySwitchInCopyOpponentAbilityChange 为完整更新构造入场复制对手特性开关的替换意图。
//
// false 仍需携带非 nil 指针，以区分显式取消与省略字段，避免旧的入场特性复制规则残留在实时资料中。
func abilitySwitchInCopyOpponentAbilityChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySwitchInRevealOpponentHeldItemsChange 为完整更新构造入场公开对手道具开关的替换意图。
//
// false 仍需携带非 nil 指针，以区分显式取消与省略字段，避免旧的入场道具公开规则残留在实时资料中。
func abilitySwitchInRevealOpponentHeldItemsChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySwitchInRevealOpponentHighestPowerSkillChange 为完整更新构造入场公开对手最高威力技能开关的替换意图。
func abilitySwitchInRevealOpponentHighestPowerSkillChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySwitchInTransformIntoOpponentChange 为完整更新构造入场复制对手战斗画像开关的替换意图。
func abilitySwitchInTransformIntoOpponentChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySwitchInDetectDangerousOpponentSkillChange 为完整更新构造入场危险技能侦测开关的替换意图。
func abilitySwitchInDetectDangerousOpponentSkillChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilitySwitchInDisguiseAsLastHealthyAllyChange 为完整更新构造入场视觉伪装开关的替换意图。
func abilitySwitchInDisguiseAsLastHealthyAllyChange(value bool) abilitydetail.Change[bool] {
	return abilitydetail.Change[bool]{Specified: true, Value: &value}
}

// abilityWeatherEndTurnHealFromMessage 将管理 API 的独立天气回复消息转换为特性领域资料。
//
// nil 表示该特性没有这条规则；非 nil 消息必须包含非空、无重复的封闭天气集合及正分母。它不能复用天气
// 伤害免疫的验证结果，因为两者虽然使用同一运输枚举，生命周期和分母语义完全不同。
func abilityWeatherEndTurnHealFromMessage(value *domainv1.GameAbilityWeatherEndTurnHeal) (*abilitydetail.WeatherEndTurnHeal, error) {
	if value == nil {
		return nil, nil
	}
	if len(value.GetWeathers()) == 0 || len(value.GetWeathers()) > 4 || value.GetHealDenominator() < 1 || value.GetHealDenominator() > 65_535 {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_END_TURN_HEAL", "特性天气回合末回复规则无效")
	}
	weathers := make([]abilitydetail.WeatherKind, 0, len(value.GetWeathers()))
	seen := make(map[abilitydetail.WeatherKind]struct{}, len(value.GetWeathers()))
	for _, source := range value.GetWeathers() {
		var weather abilitydetail.WeatherKind
		switch source {
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN:
			weather = abilitydetail.WeatherKindSun
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN:
			weather = abilitydetail.WeatherKindRain
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM:
			weather = abilitydetail.WeatherKindSandstorm
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW:
			weather = abilitydetail.WeatherKindSnow
		default:
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_END_TURN_HEAL", "特性天气回合末回复天气无效")
		}
		if _, duplicated := seen[weather]; duplicated {
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_END_TURN_HEAL", "特性天气回合末回复天气重复")
		}
		seen[weather] = struct{}{}
		weathers = append(weathers, weather)
	}
	return &abilitydetail.WeatherEndTurnHeal{Weathers: weathers, HealDenominator: value.GetHealDenominator()}, nil
}

// gameAbilityWeatherEndTurnHealMessage 将特性领域中的独立天气回合末回复规则映射为管理 API 消息。
//
// 损坏资料不会被伪造成一条可执行规则；响应中返回 nil，随后 Battle 编译会在启动边界拒绝该条资料。
func gameAbilityWeatherEndTurnHealMessage(value *abilitydetail.WeatherEndTurnHeal) *domainv1.GameAbilityWeatherEndTurnHeal {
	if value == nil || len(value.Weathers) == 0 || len(value.Weathers) > 4 || value.HealDenominator < 1 || value.HealDenominator > 65_535 {
		return nil
	}
	weathers := make([]domainv1.GameSkillWeatherKind, 0, len(value.Weathers))
	seen := make(map[abilitydetail.WeatherKind]struct{}, len(value.Weathers))
	for _, source := range value.Weathers {
		var weather domainv1.GameSkillWeatherKind
		switch source {
		case abilitydetail.WeatherKindSun:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN
		case abilitydetail.WeatherKindRain:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN
		case abilitydetail.WeatherKindSandstorm:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM
		case abilitydetail.WeatherKindSnow:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW
		default:
			return nil
		}
		if _, duplicated := seen[source]; duplicated {
			return nil
		}
		seen[source] = struct{}{}
		weathers = append(weathers, weather)
	}
	return &domainv1.GameAbilityWeatherEndTurnHeal{Weathers: weathers, HealDenominator: value.HealDenominator}
}

// abilityWeatherSpeedMultipliersFromMessages 将管理 API 的天气速度倍率集合转换为特性领域资料。
//
// 每种天气只能声明一项，分子与分母必须为正整数；不接受浮点倍率，以保证行动排序在权威状态与重放中的舍入一致。
func abilityWeatherSpeedMultipliersFromMessages(values []*domainv1.GameAbilityWeatherSpeedMultiplier) ([]abilitydetail.WeatherSpeedMultiplier, error) {
	if len(values) > 4 {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_SPEED_MULTIPLIERS", "特性天气速度倍率数量超过上限")
	}
	result := make([]abilitydetail.WeatherSpeedMultiplier, 0, len(values))
	seen := make(map[abilitydetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		if value == nil || value.GetNumerator() < 1 || value.GetNumerator() > 65_535 || value.GetDenominator() < 1 || value.GetDenominator() > 65_535 {
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_SPEED_MULTIPLIERS", "特性天气速度倍率参数无效")
		}
		var weather abilitydetail.WeatherKind
		switch value.GetWeather() {
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN:
			weather = abilitydetail.WeatherKindSun
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN:
			weather = abilitydetail.WeatherKindRain
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM:
			weather = abilitydetail.WeatherKindSandstorm
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW:
			weather = abilitydetail.WeatherKindSnow
		default:
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_SPEED_MULTIPLIERS", "特性天气速度倍率天气无效")
		}
		if _, duplicated := seen[weather]; duplicated {
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_SPEED_MULTIPLIERS", "特性天气速度倍率天气重复")
		}
		seen[weather] = struct{}{}
		result = append(result, abilitydetail.WeatherSpeedMultiplier{
			Weather: weather, Numerator: value.GetNumerator(), Denominator: value.GetDenominator(),
		})
	}
	return result, nil
}

// gameAbilityWeatherSpeedMultiplierMessages 将特性领域天气速度倍率映射为管理 API 的独立运输消息。
//
// 遇到损坏的领域资料时返回空集合而非猜测速度语义；Battle 启动边界仍会拒绝这条资料。
func gameAbilityWeatherSpeedMultiplierMessages(values []abilitydetail.WeatherSpeedMultiplier) []*domainv1.GameAbilityWeatherSpeedMultiplier {
	result := make([]*domainv1.GameAbilityWeatherSpeedMultiplier, 0, len(values))
	seen := make(map[abilitydetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		if value.Numerator < 1 || value.Numerator > 65_535 || value.Denominator < 1 || value.Denominator > 65_535 {
			return []*domainv1.GameAbilityWeatherSpeedMultiplier{}
		}
		var weather domainv1.GameSkillWeatherKind
		switch value.Weather {
		case abilitydetail.WeatherKindSun:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN
		case abilitydetail.WeatherKindRain:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN
		case abilitydetail.WeatherKindSandstorm:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM
		case abilitydetail.WeatherKindSnow:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW
		default:
			return []*domainv1.GameAbilityWeatherSpeedMultiplier{}
		}
		if _, duplicated := seen[value.Weather]; duplicated {
			return []*domainv1.GameAbilityWeatherSpeedMultiplier{}
		}
		seen[value.Weather] = struct{}{}
		result = append(result, &domainv1.GameAbilityWeatherSpeedMultiplier{
			Weather: weather, Numerator: value.Numerator, Denominator: value.Denominator,
		})
	}
	return result
}

// abilitySwitchInStrongWeatherFromMessage 将管理 API 的入场强天气消息转换为独立特性领域资料。
//
// 空消息表示该特性不提供强天气；未指定或未知枚举不能退化成普通天气或空规则，必须在 HTTP 边界直接拒绝。
func abilitySwitchInStrongWeatherFromMessage(value *domainv1.GameAbilitySwitchInStrongWeather) (*abilitydetail.SwitchInStrongWeather, error) {
	if value == nil {
		return nil, nil
	}
	var weather abilitydetail.StrongWeatherKind
	switch value.GetWeather() {
	case domainv1.GameAbilityStrongWeatherKind_GAME_ABILITY_STRONG_WEATHER_KIND_HARSH_SUNLIGHT:
		weather = abilitydetail.StrongWeatherKindHarshSunlight
	case domainv1.GameAbilityStrongWeatherKind_GAME_ABILITY_STRONG_WEATHER_KIND_HEAVY_RAIN:
		weather = abilitydetail.StrongWeatherKindHeavyRain
	case domainv1.GameAbilityStrongWeatherKind_GAME_ABILITY_STRONG_WEATHER_KIND_STRONG_WINDS:
		weather = abilitydetail.StrongWeatherKindStrongWinds
	default:
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SWITCH_IN_STRONG_WEATHER", "特性入场强天气无效")
	}
	return &abilitydetail.SwitchInStrongWeather{Weather: weather}, nil
}

// gameAbilitySwitchInStrongWeatherMessage 将独立特性领域资料映射为管理 API 的入场强天气消息。
//
// 损坏领域值返回 nil 而非猜测映射；Battle 启动前仍会严格拒绝对应实时资料，避免 API 展示制造可执行假象。
func gameAbilitySwitchInStrongWeatherMessage(value *abilitydetail.SwitchInStrongWeather) *domainv1.GameAbilitySwitchInStrongWeather {
	if value == nil {
		return nil
	}
	var weather domainv1.GameAbilityStrongWeatherKind
	switch value.Weather {
	case abilitydetail.StrongWeatherKindHarshSunlight:
		weather = domainv1.GameAbilityStrongWeatherKind_GAME_ABILITY_STRONG_WEATHER_KIND_HARSH_SUNLIGHT
	case abilitydetail.StrongWeatherKindHeavyRain:
		weather = domainv1.GameAbilityStrongWeatherKind_GAME_ABILITY_STRONG_WEATHER_KIND_HEAVY_RAIN
	case abilitydetail.StrongWeatherKindStrongWinds:
		weather = domainv1.GameAbilityStrongWeatherKind_GAME_ABILITY_STRONG_WEATHER_KIND_STRONG_WINDS
	default:
		return nil
	}
	return &domainv1.GameAbilitySwitchInStrongWeather{Weather: weather}
}

// abilitySwitchInWeatherFromMessage 将管理 API 的入场普通天气消息转换为独立特性领域资料。
//
// 空消息表示该特性没有入场普通天气；未知天气或非正持续回合必须在 HTTP 边界拒绝，不能退化为技能天气
// 或不受管理的无限持续时间。
func abilitySwitchInWeatherFromMessage(value *domainv1.GameAbilitySwitchInWeather) (*abilitydetail.SwitchInWeather, error) {
	if value == nil {
		return nil, nil
	}
	if value.GetTurnsRemaining() < 1 || value.GetTurnsRemaining() > 100 {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SWITCH_IN_WEATHER", "特性入场普通天气持续回合无效")
	}
	var weather abilitydetail.WeatherKind
	switch value.GetWeather() {
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN:
		weather = abilitydetail.WeatherKindSun
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN:
		weather = abilitydetail.WeatherKindRain
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM:
		weather = abilitydetail.WeatherKindSandstorm
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW:
		weather = abilitydetail.WeatherKindSnow
	default:
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SWITCH_IN_WEATHER", "特性入场普通天气种类无效")
	}
	return &abilitydetail.SwitchInWeather{Weather: weather, TurnsRemaining: value.GetTurnsRemaining()}, nil
}

// gameAbilitySwitchInWeatherMessage 将独立特性领域资料映射为管理 API 的入场普通天气消息。
//
// 损坏领域值返回 nil 而不猜测出可执行天气；Battle 启动边界会拒绝该条实时资料。
func gameAbilitySwitchInWeatherMessage(value *abilitydetail.SwitchInWeather) *domainv1.GameAbilitySwitchInWeather {
	if value == nil || value.TurnsRemaining < 1 || value.TurnsRemaining > 100 {
		return nil
	}
	var weather domainv1.GameSkillWeatherKind
	switch value.Weather {
	case abilitydetail.WeatherKindSun:
		weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN
	case abilitydetail.WeatherKindRain:
		weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN
	case abilitydetail.WeatherKindSandstorm:
		weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM
	case abilitydetail.WeatherKindSnow:
		weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW
	default:
		return nil
	}
	return &domainv1.GameAbilitySwitchInWeather{Weather: weather, TurnsRemaining: value.TurnsRemaining}
}

// abilitySwitchInTerrainFromMessage 将管理 API 的入场普通场地消息转换为独立特性领域资料。
//
// 空消息表示该特性没有入场普通场地；未知场地或非正持续回合必须在 HTTP 边界拒绝，不能退化为技能场地
// 或不受管理的无限持续时间。
func abilitySwitchInTerrainFromMessage(value *domainv1.GameAbilitySwitchInTerrain) (*abilitydetail.SwitchInTerrain, error) {
	if value == nil {
		return nil, nil
	}
	if value.GetTurnsRemaining() < 1 || value.GetTurnsRemaining() > 100 {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SWITCH_IN_TERRAIN", "特性入场普通场地持续回合无效")
	}
	var terrain abilitydetail.TerrainKind
	switch value.GetTerrain() {
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_ELECTRIC:
		terrain = abilitydetail.TerrainKindElectric
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_GRASSY:
		terrain = abilitydetail.TerrainKindGrassy
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_MISTY:
		terrain = abilitydetail.TerrainKindMisty
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_PSYCHIC:
		terrain = abilitydetail.TerrainKindPsychic
	default:
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SWITCH_IN_TERRAIN", "特性入场普通场地种类无效")
	}
	return &abilitydetail.SwitchInTerrain{Terrain: terrain, TurnsRemaining: value.GetTurnsRemaining()}, nil
}

// gameAbilitySwitchInTerrainMessage 将独立特性领域资料映射为管理 API 的入场普通场地消息。
//
// 损坏领域值返回 nil 而不猜测出可执行场地；Battle 启动边界会拒绝该条实时资料。
func gameAbilitySwitchInTerrainMessage(value *abilitydetail.SwitchInTerrain) *domainv1.GameAbilitySwitchInTerrain {
	if value == nil || value.TurnsRemaining < 1 || value.TurnsRemaining > 100 {
		return nil
	}
	var terrain domainv1.GameSkillTerrainKind
	switch value.Terrain {
	case abilitydetail.TerrainKindElectric:
		terrain = domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_ELECTRIC
	case abilitydetail.TerrainKindGrassy:
		terrain = domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_GRASSY
	case abilitydetail.TerrainKindMisty:
		terrain = domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_MISTY
	case abilitydetail.TerrainKindPsychic:
		terrain = domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_PSYCHIC
	default:
		return nil
	}
	return &domainv1.GameAbilitySwitchInTerrain{Terrain: terrain, TurnsRemaining: value.TurnsRemaining}
}

// abilityEnvironmentHighestStatMultiplierFromMessage 将管理 API 的环境最高原始能力强化消息转换为独立特性领域资料。
//
// 请求只能指定一个已知普通天气或普通场地。最高能力项和倍率是战斗规则，不允许由外部契约填写，避免同一种
// 特性在不同客户端被解释为不同数值效果。
func abilityEnvironmentHighestStatMultiplierFromMessage(value *domainv1.GameAbilityEnvironmentHighestStatMultiplier) (*abilitydetail.EnvironmentHighestStatMultiplier, error) {
	if value == nil {
		return nil, nil
	}
	result := &abilitydetail.EnvironmentHighestStatMultiplier{}
	switch value.GetRequiredWeather() {
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_UNSPECIFIED:
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN:
		weather := abilitydetail.WeatherKindSun
		result.RequiredWeather = &weather
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN:
		weather := abilitydetail.WeatherKindRain
		result.RequiredWeather = &weather
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM:
		weather := abilitydetail.WeatherKindSandstorm
		result.RequiredWeather = &weather
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW:
		weather := abilitydetail.WeatherKindSnow
		result.RequiredWeather = &weather
	default:
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_ENVIRONMENT_HIGHEST_STAT_MULTIPLIER", "特性环境最高能力强化天气无效")
	}
	switch value.GetRequiredTerrain() {
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_UNSPECIFIED:
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_ELECTRIC:
		terrain := abilitydetail.TerrainKindElectric
		result.RequiredTerrain = &terrain
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_GRASSY:
		terrain := abilitydetail.TerrainKindGrassy
		result.RequiredTerrain = &terrain
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_MISTY:
		terrain := abilitydetail.TerrainKindMisty
		result.RequiredTerrain = &terrain
	case domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_PSYCHIC:
		terrain := abilitydetail.TerrainKindPsychic
		result.RequiredTerrain = &terrain
	default:
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_ENVIRONMENT_HIGHEST_STAT_MULTIPLIER", "特性环境最高能力强化场地无效")
	}
	if result.RequiredWeather == nil && result.RequiredTerrain == nil || result.RequiredWeather != nil && result.RequiredTerrain != nil {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_ENVIRONMENT_HIGHEST_STAT_MULTIPLIER", "特性环境最高能力强化必须且只能指定一种环境")
	}
	return result, nil
}

// gameAbilityEnvironmentHighestStatMultiplierMessage 将独立特性领域资料映射为管理 API 的环境最高原始能力强化消息。
//
// 读取到损坏的领域值时返回 nil；不能猜测天气、场地或默认能力项，Battle 启动边界会阻止该实时资料进入新对局。
func gameAbilityEnvironmentHighestStatMultiplierMessage(value *abilitydetail.EnvironmentHighestStatMultiplier) *domainv1.GameAbilityEnvironmentHighestStatMultiplier {
	if value == nil || value.RequiredWeather != nil && value.RequiredTerrain != nil {
		return nil
	}
	result := &domainv1.GameAbilityEnvironmentHighestStatMultiplier{}
	if value.RequiredWeather != nil {
		switch *value.RequiredWeather {
		case abilitydetail.WeatherKindSun:
			result.RequiredWeather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN
		case abilitydetail.WeatherKindRain:
			result.RequiredWeather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN
		case abilitydetail.WeatherKindSandstorm:
			result.RequiredWeather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM
		case abilitydetail.WeatherKindSnow:
			result.RequiredWeather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW
		default:
			return nil
		}
		return result
	}
	if value.RequiredTerrain != nil {
		switch *value.RequiredTerrain {
		case abilitydetail.TerrainKindElectric:
			result.RequiredTerrain = domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_ELECTRIC
		case abilitydetail.TerrainKindGrassy:
			result.RequiredTerrain = domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_GRASSY
		case abilitydetail.TerrainKindMisty:
			result.RequiredTerrain = domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_MISTY
		case abilitydetail.TerrainKindPsychic:
			result.RequiredTerrain = domainv1.GameSkillTerrainKind_GAME_SKILL_TERRAIN_KIND_PSYCHIC
		default:
			return nil
		}
		return result
	}
	return nil
}

// abilitySwitchInStatStageChangeFromMessage 将管理 API 的入场能力阶级变化消息转换为独立特性领域资料。
//
// 空消息表示没有该规则；目标、能力 Identifier 和非零阶段变化必须同时完整出现，不能退化为技能命中效果或默认
// 作用于自身。
func abilitySwitchInStatStageChangeFromMessage(value *domainv1.GameAbilitySwitchInStatStageChange) (*abilitydetail.SwitchInStatStageChange, error) {
	if value == nil {
		return nil, nil
	}
	statID, err := gameDataIdentifier(value.GetStatId(), "INVALID_ABILITY_SWITCH_IN_STAT_STAGE_STAT_ID")
	if err != nil {
		return nil, err
	}
	if value.GetStageDelta() < -6 || value.GetStageDelta() > 6 || value.GetStageDelta() == 0 {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SWITCH_IN_STAT_STAGE_DELTA", "特性入场能力阶级变化量无效")
	}
	var target abilitydetail.SwitchInStatStageTarget
	switch value.GetTarget() {
	case domainv1.GameAbilitySwitchInStatStageTarget_GAME_ABILITY_SWITCH_IN_STAT_STAGE_TARGET_SELF:
		target = abilitydetail.SwitchInStatStageTargetSelf
	case domainv1.GameAbilitySwitchInStatStageTarget_GAME_ABILITY_SWITCH_IN_STAT_STAGE_TARGET_OPPONENTS:
		target = abilitydetail.SwitchInStatStageTargetOpponents
	default:
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SWITCH_IN_STAT_STAGE_TARGET", "特性入场能力阶级变化目标无效")
	}
	return &abilitydetail.SwitchInStatStageChange{Target: target, StatID: statID, StageDelta: value.GetStageDelta()}, nil
}

// gameAbilitySwitchInStatStageChangeMessage 将独立特性领域资料映射为管理 API 的入场能力阶级变化消息。
//
// 损坏领域值返回 nil 而不猜测默认目标或能力项；Battle 启动边界会拒绝损坏的实时资料。
func gameAbilitySwitchInStatStageChangeMessage(value *abilitydetail.SwitchInStatStageChange) *domainv1.GameAbilitySwitchInStatStageChange {
	if value == nil || value.StatID == snowflake.ID(0) || value.StageDelta < -6 || value.StageDelta > 6 || value.StageDelta == 0 {
		return nil
	}
	var target domainv1.GameAbilitySwitchInStatStageTarget
	switch value.Target {
	case abilitydetail.SwitchInStatStageTargetSelf:
		target = domainv1.GameAbilitySwitchInStatStageTarget_GAME_ABILITY_SWITCH_IN_STAT_STAGE_TARGET_SELF
	case abilitydetail.SwitchInStatStageTargetOpponents:
		target = domainv1.GameAbilitySwitchInStatStageTarget_GAME_ABILITY_SWITCH_IN_STAT_STAGE_TARGET_OPPONENTS
	default:
		return nil
	}
	return &domainv1.GameAbilitySwitchInStatStageChange{Target: target, StatId: value.StatID.String(), StageDelta: value.StageDelta}
}

// abilityTerastallizationStatStageChangeFromMessage 将管理 API 的太晶化能力阶级变化消息转换为独立特性领域资料。
//
// 空消息表示没有该规则；能力 Identifier 与非零阶段变化必须同时完整出现，不能退化为入场、技能或默认自身能力变化。
func abilityTerastallizationStatStageChangeFromMessage(value *domainv1.GameAbilityTerastallizationStatStageChange) (*abilitydetail.TerastallizationStatStageChange, error) {
	if value == nil {
		return nil, nil
	}
	statID, err := gameDataIdentifier(value.GetStatId(), "INVALID_ABILITY_TERASTALLIZATION_STAT_STAGE_STAT_ID")
	if err != nil {
		return nil, err
	}
	if value.GetStageDelta() < -6 || value.GetStageDelta() > 6 || value.GetStageDelta() == 0 {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_TERASTALLIZATION_STAT_STAGE_DELTA", "特性太晶化能力阶级变化量无效")
	}
	return &abilitydetail.TerastallizationStatStageChange{StatID: statID, StageDelta: value.GetStageDelta()}, nil
}

// gameAbilityTerastallizationStatStageChangeMessage 将独立特性领域资料映射为管理 API 的太晶化能力阶级变化消息。
//
// 损坏领域值返回 nil 而不猜测默认能力项或阶段变化；Battle 启动边界会拒绝损坏的实时资料。
func gameAbilityTerastallizationStatStageChangeMessage(value *abilitydetail.TerastallizationStatStageChange) *domainv1.GameAbilityTerastallizationStatStageChange {
	if value == nil || value.StatID == snowflake.ID(0) || value.StageDelta < -6 || value.StageDelta > 6 || value.StageDelta == 0 {
		return nil
	}
	return &domainv1.GameAbilityTerastallizationStatStageChange{StatId: value.StatID.String(), StageDelta: value.StageDelta}
}

// abilitySwitchInAllyHealFromMessage 将管理 API 的入场同侧回复消息转换为独立特性领域资料。
//
// 空消息表示该特性没有这条规则；非空消息必须提供正回复分母，不能退化为默认回复比例或技能治疗。
func abilitySwitchInAllyHealFromMessage(value *domainv1.GameAbilitySwitchInAllyHeal) (*abilitydetail.SwitchInAllyHeal, error) {
	if value == nil {
		return nil, nil
	}
	if value.GetHealDenominator() < 1 || value.GetHealDenominator() > 65_535 {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SWITCH_IN_ALLY_HEAL", "特性入场同侧回复分母无效")
	}
	return &abilitydetail.SwitchInAllyHeal{HealDenominator: value.GetHealDenominator()}, nil
}

// gameAbilitySwitchInAllyHealMessage 将独立特性领域资料映射为管理 API 的入场同侧回复消息。
//
// 损坏领域值返回 nil 而不猜测默认分母；Battle 启动边界会拒绝损坏的实时资料。
func gameAbilitySwitchInAllyHealMessage(value *abilitydetail.SwitchInAllyHeal) *domainv1.GameAbilitySwitchInAllyHeal {
	if value == nil || value.HealDenominator < 1 || value.HealDenominator > 65_535 {
		return nil
	}
	return &domainv1.GameAbilitySwitchInAllyHeal{HealDenominator: value.HealDenominator}
}

// abilitySwitchInFormChangeFromMessage 将管理 API 的入场形态切换消息转换为独立特性领域资料。
//
// 空消息表示没有该规则。两个形态 Identifier 必须完整且不同；具体能力和生命不由请求携带，避免管理 API 伪造已
// 冻结的运行时画像。
func abilitySwitchInFormChangeFromMessage(value *domainv1.GameAbilitySwitchInFormChange) (*abilitydetail.SwitchInFormChange, error) {
	if value == nil {
		return nil, nil
	}
	baseCreatureID, err := gameDataIdentifier(value.GetBaseCreatureId(), "INVALID_ABILITY_SWITCH_IN_FORM_BASE_CREATURE_ID")
	if err != nil {
		return nil, err
	}
	alternateCreatureID, err := gameDataIdentifier(value.GetAlternateCreatureId(), "INVALID_ABILITY_SWITCH_IN_FORM_ALTERNATE_CREATURE_ID")
	if err != nil {
		return nil, err
	}
	if baseCreatureID == alternateCreatureID {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SWITCH_IN_FORM", "特性入场形态切换的两个形态不能相同")
	}
	return &abilitydetail.SwitchInFormChange{
		BaseCreatureID: baseCreatureID, AlternateCreatureID: alternateCreatureID,
		AddsMaximumHPDifference: value.GetAddsMaximumHpDifference(),
	}, nil
}

// gameAbilitySwitchInFormChangeMessage 将独立入场形态切换领域资料映射为管理 API 消息。
//
// 损坏资料返回 nil 而不合成看似可执行的形态规则；Battle 启动边界会继续拒绝相同资料。
func gameAbilitySwitchInFormChangeMessage(value *abilitydetail.SwitchInFormChange) *domainv1.GameAbilitySwitchInFormChange {
	if value == nil || value.BaseCreatureID == snowflake.ID(0) || value.AlternateCreatureID == snowflake.ID(0) ||
		value.BaseCreatureID == value.AlternateCreatureID {
		return nil
	}
	return &domainv1.GameAbilitySwitchInFormChange{
		BaseCreatureId: value.BaseCreatureID.String(), AlternateCreatureId: value.AlternateCreatureID.String(),
		AddsMaximumHpDifference: value.AddsMaximumHPDifference,
	}
}

// abilitySwitchOutFormChangeFromMessage 将管理 API 的离场形态切换资料转换为领域值。
//
// 两个 Identifier 必须同时给出且不同；省略整个消息才表示没有规则，禁止把不完整输入默默解释为清除或无效果。
func abilitySwitchOutFormChangeFromMessage(value *domainv1.GameAbilitySwitchOutFormChange) (*abilitydetail.SwitchOutFormChange, error) {
	if value == nil {
		return nil, nil
	}
	baseCreatureID, err := gameDataIdentifier(value.GetBaseCreatureId(), "INVALID_ABILITY_SWITCH_OUT_FORM_BASE_CREATURE_ID")
	if err != nil {
		return nil, err
	}
	alternateCreatureID, err := gameDataIdentifier(value.GetAlternateCreatureId(), "INVALID_ABILITY_SWITCH_OUT_FORM_ALTERNATE_CREATURE_ID")
	if err != nil {
		return nil, err
	}
	if baseCreatureID == alternateCreatureID {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_SWITCH_OUT_FORM_CHANGE", "离场形态切换的基础形态和目标形态不能相同")
	}
	return &abilitydetail.SwitchOutFormChange{BaseCreatureID: baseCreatureID, AlternateCreatureID: alternateCreatureID}, nil
}

// gameAbilitySwitchOutFormChangeMessage 将已校验的离场形态切换领域值映射为管理 API 消息。
func gameAbilitySwitchOutFormChangeMessage(value *abilitydetail.SwitchOutFormChange) *domainv1.GameAbilitySwitchOutFormChange {
	if value == nil || value.BaseCreatureID == snowflake.ID(0) || value.AlternateCreatureID == snowflake.ID(0) ||
		value.BaseCreatureID == value.AlternateCreatureID {
		return nil
	}
	return &domainv1.GameAbilitySwitchOutFormChange{
		BaseCreatureId: value.BaseCreatureID.String(), AlternateCreatureId: value.AlternateCreatureID.String(),
	}
}

// abilitySwitchOutHealDenominator 校验成功离场固定比例回复分母。
// 0 是无规则的唯一显式表示，正值必须处于 PostgreSQL 约束和引擎安全计算共同支持的范围。
func abilitySwitchOutHealDenominator(value int32) (int32, error) {
	if value < 0 || value > 65_535 {
		return 0, kratoserrors.BadRequest("INVALID_ABILITY_SWITCH_OUT_HEAL_DENOMINATOR", "离场回复分母必须介于 0 和 65535 之间")
	}
	return value, nil
}

// abilityWeatherFormChangeFromMessage 将管理 API 的天气形态消息转换为独立特性领域资料。
//
// 空消息表示没有规则。每个天气只能映射一个目标形态，未知天气、重复天气、空条目或无效 Identifier 都在 HTTP
// 边界拒绝，避免将数组顺序或展示文本带入权威战斗。
func abilityWeatherFormChangeFromMessage(value *domainv1.GameAbilityWeatherFormChange) (*abilitydetail.WeatherFormChange, error) {
	if value == nil {
		return nil, nil
	}
	defaultCreatureID, err := gameDataIdentifier(value.GetDefaultCreatureId(), "INVALID_ABILITY_WEATHER_FORM_DEFAULT_CREATURE_ID")
	if err != nil {
		return nil, err
	}
	if len(value.GetTargets()) < 1 || len(value.GetTargets()) > 4 {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_FORM", "特性天气形态目标数量无效")
	}
	targets := make([]abilitydetail.WeatherFormTarget, 0, len(value.GetTargets()))
	seen := make(map[abilitydetail.WeatherKind]struct{}, len(value.GetTargets()))
	for _, target := range value.GetTargets() {
		if target == nil {
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_FORM", "特性天气形态目标不能为空")
		}
		weather, weatherErr := abilityWeatherKindFromMessage(target.GetWeather(), "INVALID_ABILITY_WEATHER_FORM")
		if weatherErr != nil {
			return nil, weatherErr
		}
		if _, duplicated := seen[weather]; duplicated {
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_FORM", "特性天气形态天气重复")
		}
		creatureID, creatureErr := gameDataIdentifier(target.GetCreatureId(), "INVALID_ABILITY_WEATHER_FORM_CREATURE_ID")
		if creatureErr != nil {
			return nil, creatureErr
		}
		seen[weather] = struct{}{}
		targets = append(targets, abilitydetail.WeatherFormTarget{Weather: weather, CreatureID: creatureID})
	}
	return &abilitydetail.WeatherFormChange{DefaultCreatureID: defaultCreatureID, Targets: targets}, nil
}

// gameAbilityWeatherFormChangeMessage 将独立天气形态领域资料映射为管理 API 消息。
func gameAbilityWeatherFormChangeMessage(value *abilitydetail.WeatherFormChange) *domainv1.GameAbilityWeatherFormChange {
	if value == nil || value.DefaultCreatureID == snowflake.ID(0) || len(value.Targets) < 1 || len(value.Targets) > 4 {
		return nil
	}
	targets := make([]*domainv1.GameAbilityWeatherFormTarget, 0, len(value.Targets))
	seen := make(map[abilitydetail.WeatherKind]struct{}, len(value.Targets))
	for _, target := range value.Targets {
		if target.CreatureID == snowflake.ID(0) {
			return nil
		}
		weather, valid := gameAbilityWeatherKind(target.Weather)
		if !valid {
			return nil
		}
		if _, duplicated := seen[target.Weather]; duplicated {
			return nil
		}
		seen[target.Weather] = struct{}{}
		targets = append(targets, &domainv1.GameAbilityWeatherFormTarget{Weather: weather, CreatureId: target.CreatureID.String()})
	}
	return &domainv1.GameAbilityWeatherFormChange{DefaultCreatureId: value.DefaultCreatureID.String(), Targets: targets}
}

// abilityWeatherKindFromMessage 将共享运输层的天气枚举收窄为特性领域天气值。
func abilityWeatherKindFromMessage(value domainv1.GameSkillWeatherKind, code string) (abilitydetail.WeatherKind, error) {
	switch value {
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN:
		return abilitydetail.WeatherKindSun, nil
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN:
		return abilitydetail.WeatherKindRain, nil
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM:
		return abilitydetail.WeatherKindSandstorm, nil
	case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW:
		return abilitydetail.WeatherKindSnow, nil
	default:
		return "", kratoserrors.BadRequest(code, "特性天气形态天气无效")
	}
}

// gameAbilityWeatherKind 将特性领域天气映射为共享运输层枚举。
func gameAbilityWeatherKind(value abilitydetail.WeatherKind) (domainv1.GameSkillWeatherKind, bool) {
	switch value {
	case abilitydetail.WeatherKindSun:
		return domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN, true
	case abilitydetail.WeatherKindRain:
		return domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN, true
	case abilitydetail.WeatherKindSandstorm:
		return domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM, true
	case abilitydetail.WeatherKindSnow:
		return domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW, true
	default:
		return domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_UNSPECIFIED, false
	}
}

// abilityWeatherDamageImmunitiesFromMessages 将管理 API 的封闭天气枚举转换为特性独有的天气伤害免疫资料。
//
// 即使技能与特性共用同一运输层枚举，特性资料仍在这里转换为自己的领域类型，避免共享可变“效果”结构而使不同
// 生命周期互相耦合。重复或未知天气必须在 HTTP 边界拒绝。
func abilityWeatherDamageImmunitiesFromMessages(values []domainv1.GameSkillWeatherKind) ([]abilitydetail.WeatherKind, error) {
	if len(values) > 4 {
		return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_DAMAGE_IMMUNITIES", "特性天气伤害免疫数量超过上限")
	}
	result := make([]abilitydetail.WeatherKind, 0, len(values))
	seen := make(map[abilitydetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		var weather abilitydetail.WeatherKind
		switch value {
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN:
			weather = abilitydetail.WeatherKindSun
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN:
			weather = abilitydetail.WeatherKindRain
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM:
			weather = abilitydetail.WeatherKindSandstorm
		case domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW:
			weather = abilitydetail.WeatherKindSnow
		default:
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_DAMAGE_IMMUNITIES", "特性天气伤害免疫天气无效")
		}
		if _, duplicated := seen[weather]; duplicated {
			return nil, kratoserrors.BadRequest("INVALID_ABILITY_WEATHER_DAMAGE_IMMUNITIES", "特性天气伤害免疫天气重复")
		}
		seen[weather] = struct{}{}
		result = append(result, weather)
	}
	return result, nil
}

// gameAbilityWeatherDamageImmunities 将特性领域天气免疫映射为管理 API 的封闭枚举。
//
// 读取到未知或重复数据库资料时返回空集合而不是猜测运行时含义；Battle 编译器会继续拒绝这条损坏资料。
func gameAbilityWeatherDamageImmunities(values []abilitydetail.WeatherKind) []domainv1.GameSkillWeatherKind {
	result := make([]domainv1.GameSkillWeatherKind, 0, len(values))
	seen := make(map[abilitydetail.WeatherKind]struct{}, len(values))
	for _, value := range values {
		var weather domainv1.GameSkillWeatherKind
		switch value {
		case abilitydetail.WeatherKindSun:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SUN
		case abilitydetail.WeatherKindRain:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_RAIN
		case abilitydetail.WeatherKindSandstorm:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SANDSTORM
		case abilitydetail.WeatherKindSnow:
			weather = domainv1.GameSkillWeatherKind_GAME_SKILL_WEATHER_KIND_SNOW
		default:
			return []domainv1.GameSkillWeatherKind{}
		}
		if _, duplicated := seen[value]; duplicated {
			return []domainv1.GameSkillWeatherKind{}
		}
		seen[value] = struct{}{}
		result = append(result, weather)
	}
	return result
}

// textValue 把领域层可空文本转换为当前响应契约使用的字符串。
func textValue(value *string) string {
	if value == nil {
		return ""
	}
	return *value
}
