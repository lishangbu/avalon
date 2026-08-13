package battle

import (
	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
)

// abilityDamageMultiplierRules 编译指定特性当前详情中的全部攻击方伤害倍率规则。
// 缺少详情合法地表示没有规则；任何损坏数值、未知天气或零属性引用都会阻止 Battle 创建。
func (compiler *initialMemberCompiler) abilityDamageMultiplierRules(abilityID snowflake.ID) (abilityDamageMultiplierRules, error) {
	detail, err := compiler.abilityDetail(abilityID)
	if err != nil || detail == nil {
		return abilityDamageMultiplierRules{}, err
	}
	rules := abilityDamageMultiplierRules{}
	if value := detail.BasePowerAtMostDamageBoost; value != nil {
		maximum, ok := damageUint16(value.MaximumPower)
		numerator, denominator, fractionOK := damageFraction(value.Numerator, value.Denominator)
		if !ok || !fractionOK {
			return abilityDamageMultiplierRules{}, ErrInitialStateCompilation
		}
		rules.basePowerAtMostDamageBoost = &battleengine.BasePowerAtMostDamageBoost{MaximumPower: maximum, Numerator: numerator, Denominator: denominator}
	}
	if value := detail.RecoilSkillDamageBoost; value != nil {
		numerator, denominator, ok := damageFraction(value.Numerator, value.Denominator)
		if !ok {
			return abilityDamageMultiplierRules{}, ErrInitialStateCompilation
		}
		rules.recoilSkillDamageBoost = &battleengine.RecoilSkillDamageBoost{Numerator: numerator, Denominator: denominator}
	}
	if value := detail.LowHPElementDamageBoost; value != nil {
		hpNumerator, hpDenominator, hpOK := damageFraction(value.HPThresholdNumerator, value.HPThresholdDenominator)
		damageNumerator, damageDenominator, damageOK := damageFraction(value.DamageNumerator, value.DamageDenominator)
		if value.ElementID == snowflake.ID(0) || !hpOK || hpNumerator > hpDenominator || !damageOK {
			return abilityDamageMultiplierRules{}, ErrInitialStateCompilation
		}
		rules.lowHPElementDamageBoost = &battleengine.LowHPElementDamageBoost{
			ElementID: value.ElementID, HPThresholdNumerator: hpNumerator, HPThresholdDenominator: hpDenominator,
			DamageNumerator: damageNumerator, DamageDenominator: damageDenominator,
		}
	}
	if value := detail.WeatherElementDamageBoost; value != nil {
		weather, weatherOK := battleWeatherKind(value.Weather)
		elements, elementsOK := damageElementIDs(value.ElementIDs)
		numerator, denominator, fractionOK := damageFraction(value.Numerator, value.Denominator)
		if !weatherOK || !elementsOK || !fractionOK {
			return abilityDamageMultiplierRules{}, ErrInitialStateCompilation
		}
		rules.weatherElementDamageBoost = &battleengine.WeatherElementDamageBoost{Weather: weather, ElementIDs: elements, Numerator: numerator, Denominator: denominator}
	}
	if value := detail.ElementSkillDamageBoost; value != nil {
		elements, elementsOK := damageElementIDs(value.ElementIDs)
		numerator, denominator, fractionOK := damageFraction(value.Numerator, value.Denominator)
		if !elementsOK || !fractionOK {
			return abilityDamageMultiplierRules{}, ErrInitialStateCompilation
		}
		rules.elementSkillDamageBoost = &battleengine.ElementSkillDamageBoost{ElementIDs: elements, Numerator: numerator, Denominator: denominator}
	}
	var fractionOK bool
	rules.sameElementBonusOverride, fractionOK = compileSameElementBonusOverride(detail.SameElementBonusOverride)
	if !fractionOK {
		return abilityDamageMultiplierRules{}, ErrInitialStateCompilation
	}
	rules.contactBasedSkillDamageBoost, fractionOK = compileContactBasedSkillDamageBoost(detail.ContactBasedSkillDamageBoost)
	if !fractionOK {
		return abilityDamageMultiplierRules{}, ErrInitialStateCompilation
	}
	rules.criticalHitDamageBoost, fractionOK = compileCriticalHitDamageBoost(detail.CriticalHitDamageBoost)
	if !fractionOK {
		return abilityDamageMultiplierRules{}, ErrInitialStateCompilation
	}
	rules.superEffectiveDamageBoost, fractionOK = compileSuperEffectiveDamageBoost(detail.SuperEffectiveDamageBoost)
	if !fractionOK {
		return abilityDamageMultiplierRules{}, ErrInitialStateCompilation
	}
	rules.notVeryEffectiveDamageBoost, fractionOK = compileNotVeryEffectiveDamageBoost(detail.NotVeryEffectiveDamageBoost)
	if !fractionOK {
		return abilityDamageMultiplierRules{}, ErrInitialStateCompilation
	}
	return rules, nil
}

// damageFraction 将资料层正整数分数无损转换为引擎 uint16。
func damageFraction(numerator, denominator int32) (uint16, uint16, bool) {
	if numerator < 1 || numerator > 65_535 || denominator < 1 || denominator > 65_535 {
		return 0, 0, false
	}
	return uint16(numerator), uint16(denominator), true
}

// damageUint16 将资料层正整数参数无损转换为引擎 uint16。
func damageUint16(value int32) (uint16, bool) {
	if value < 1 || value > 65_535 {
		return 0, false
	}
	return uint16(value), true
}

// damageElementIDs 将非空、无重复稳定属性 Identifier 集合冻结为文本身份。
func damageElementIDs(values []snowflake.ID) ([]battleengine.Identifier, bool) {
	if len(values) == 0 || len(values) > 32 {
		return nil, false
	}
	seen := make(map[snowflake.ID]struct{}, len(values))
	result := make([]battleengine.Identifier, 0, len(values))
	for _, value := range values {
		if value == snowflake.ID(0) {
			return nil, false
		}
		if _, duplicate := seen[value]; duplicate {
			return nil, false
		}
		seen[value] = struct{}{}
		result = append(result, value)
	}
	return result, true
}

// battleWeatherKind 将特性详情的封闭普通天气转换为纯引擎天气。
func battleWeatherKind(value abilitydetail.WeatherKind) (battleengine.WeatherKind, bool) {
	switch value {
	case abilitydetail.WeatherKindSun:
		return battleengine.WeatherKindSun, true
	case abilitydetail.WeatherKindRain:
		return battleengine.WeatherKindRain, true
	case abilitydetail.WeatherKindSandstorm:
		return battleengine.WeatherKindSandstorm, true
	case abilitydetail.WeatherKindSnow:
		return battleengine.WeatherKindSnow, true
	default:
		return "", false
	}
}

func compileSameElementBonusOverride(value *abilitydetail.SameElementBonusOverride) (*battleengine.SameElementBonusOverride, bool) {
	if value == nil {
		return nil, true
	}
	numerator, denominator, ok := damageFraction(value.Numerator, value.Denominator)
	return &battleengine.SameElementBonusOverride{Numerator: numerator, Denominator: denominator}, ok
}

func compileContactBasedSkillDamageBoost(value *abilitydetail.ContactBasedSkillDamageBoost) (*battleengine.ContactBasedSkillDamageBoost, bool) {
	if value == nil {
		return nil, true
	}
	numerator, denominator, ok := damageFraction(value.Numerator, value.Denominator)
	return &battleengine.ContactBasedSkillDamageBoost{Numerator: numerator, Denominator: denominator}, ok
}

func compileCriticalHitDamageBoost(value *abilitydetail.CriticalHitDamageBoost) (*battleengine.CriticalHitDamageBoost, bool) {
	if value == nil {
		return nil, true
	}
	numerator, denominator, ok := damageFraction(value.Numerator, value.Denominator)
	return &battleengine.CriticalHitDamageBoost{Numerator: numerator, Denominator: denominator}, ok
}

func compileSuperEffectiveDamageBoost(value *abilitydetail.SuperEffectiveDamageBoost) (*battleengine.SuperEffectiveDamageBoost, bool) {
	if value == nil {
		return nil, true
	}
	numerator, denominator, ok := damageFraction(value.Numerator, value.Denominator)
	return &battleengine.SuperEffectiveDamageBoost{Numerator: numerator, Denominator: denominator}, ok
}

func compileNotVeryEffectiveDamageBoost(value *abilitydetail.NotVeryEffectiveDamageBoost) (*battleengine.NotVeryEffectiveDamageBoost, bool) {
	if value == nil {
		return nil, true
	}
	numerator, denominator, ok := damageFraction(value.Numerator, value.Denominator)
	return &battleengine.NotVeryEffectiveDamageBoost{Numerator: numerator, Denominator: denominator}, ok
}

func cloneBattleBasePowerAtMostDamageBoost(value *battleengine.BasePowerAtMostDamageBoost) *battleengine.BasePowerAtMostDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
func cloneBattleRecoilSkillDamageBoost(value *battleengine.RecoilSkillDamageBoost) *battleengine.RecoilSkillDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
func cloneBattleLowHPElementDamageBoost(value *battleengine.LowHPElementDamageBoost) *battleengine.LowHPElementDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
func cloneBattleWeatherElementDamageBoost(value *battleengine.WeatherElementDamageBoost) *battleengine.WeatherElementDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.ElementIDs = append([]battleengine.Identifier(nil), value.ElementIDs...)
	return &cloned
}
func cloneBattleElementSkillDamageBoost(value *battleengine.ElementSkillDamageBoost) *battleengine.ElementSkillDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.ElementIDs = append([]battleengine.Identifier(nil), value.ElementIDs...)
	return &cloned
}
func cloneBattleSameElementBonusOverride(value *battleengine.SameElementBonusOverride) *battleengine.SameElementBonusOverride {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
func cloneBattleContactBasedSkillDamageBoost(value *battleengine.ContactBasedSkillDamageBoost) *battleengine.ContactBasedSkillDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
func cloneBattleCriticalHitDamageBoost(value *battleengine.CriticalHitDamageBoost) *battleengine.CriticalHitDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
func cloneBattleSuperEffectiveDamageBoost(value *battleengine.SuperEffectiveDamageBoost) *battleengine.SuperEffectiveDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
func cloneBattleNotVeryEffectiveDamageBoost(value *battleengine.NotVeryEffectiveDamageBoost) *battleengine.NotVeryEffectiveDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
