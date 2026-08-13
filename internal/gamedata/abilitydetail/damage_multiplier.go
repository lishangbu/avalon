package abilitydetail

import "github.com/lishangbu/avalon/internal/platform/snowflake"

// BasePowerAtMostDamageBoost 是特性详情中按技能原始基础威力上限触发的最终伤害倍率。
type BasePowerAtMostDamageBoost struct {
	// MaximumPower 是允许触发强化的最大原始基础威力，范围为 1 至 65535。
	MaximumPower int32
	// Numerator 是伤害倍率的正整数分子，范围为 1 至 65535。
	Numerator int32
	// Denominator 是伤害倍率的正整数分母，范围为 1 至 65535。
	Denominator int32
}

// RecoilSkillDamageBoost 是特性详情中按实际伤害产生反作用的技能最终伤害倍率。
type RecoilSkillDamageBoost struct {
	// Numerator 是伤害倍率的正整数分子，范围为 1 至 65535。
	Numerator int32
	// Denominator 是伤害倍率的正整数分母，范围为 1 至 65535。
	Denominator int32
}

// LowHPElementDamageBoost 是特性详情中低生命阈值下指定有效属性的最终伤害倍率。
type LowHPElementDamageBoost struct {
	// ElementID 是本次技能有效属性必须匹配的非零稳定 Identifier。
	ElementID snowflake.ID
	// HPThresholdNumerator 是生命阈值分数的正整数分子，范围为 1 至 65535。
	HPThresholdNumerator int32
	// HPThresholdDenominator 是生命阈值分数的正整数分母，必须不小于分子。
	HPThresholdDenominator int32
	// DamageNumerator 是伤害倍率的正整数分子，范围为 1 至 65535。
	DamageNumerator int32
	// DamageDenominator 是伤害倍率的正整数分母，范围为 1 至 65535。
	DamageDenominator int32
}

// WeatherElementDamageBoost 是特性详情中指定天气和一组有效属性共同触发的最终伤害倍率。
type WeatherElementDamageBoost struct {
	// Weather 是触发强化所需的封闭普通天气类别。
	Weather WeatherKind
	// ElementIDs 是允许触发强化的非空、无重复稳定属性 Identifier 集合。
	ElementIDs []snowflake.ID
	// Numerator 是伤害倍率的正整数分子，范围为 1 至 65535。
	Numerator int32
	// Denominator 是伤害倍率的正整数分母，范围为 1 至 65535。
	Denominator int32
}

// ElementSkillDamageBoost 是特性详情中一组技能有效属性触发的最终伤害倍率。
type ElementSkillDamageBoost struct {
	// ElementIDs 是允许触发强化的非空、无重复稳定属性 Identifier 集合。
	ElementIDs []snowflake.ID
	// Numerator 是伤害倍率的正整数分子，范围为 1 至 65535。
	Numerator int32
	// Denominator 是伤害倍率的正整数分母，范围为 1 至 65535。
	Denominator int32
}

// SameElementBonusOverride 是特性详情中替换默认属性一致加成的精确倍率。
type SameElementBonusOverride struct {
	// Numerator 是替代倍率的正整数分子，范围为 1 至 65535。
	Numerator int32
	// Denominator 是替代倍率的正整数分母，范围为 1 至 65535。
	Denominator int32
}

// ContactBasedSkillDamageBoost 是特性详情中有效接触技能触发的最终伤害倍率。
type ContactBasedSkillDamageBoost struct {
	// Numerator 是伤害倍率的正整数分子，范围为 1 至 65535。
	Numerator int32
	// Denominator 是伤害倍率的正整数分母，范围为 1 至 65535。
	Denominator int32
}

// CriticalHitDamageBoost 是特性详情中实际击中要害触发的额外最终伤害倍率。
type CriticalHitDamageBoost struct {
	// Numerator 是伤害倍率的正整数分子，范围为 1 至 65535。
	Numerator int32
	// Denominator 是伤害倍率的正整数分母，范围为 1 至 65535。
	Denominator int32
}

// SuperEffectiveDamageBoost 是特性详情中最终属性相性严格大于一时触发的最终伤害倍率。
type SuperEffectiveDamageBoost struct {
	// Numerator 是伤害倍率的正整数分子，范围为 1 至 65535。
	Numerator int32
	// Denominator 是伤害倍率的正整数分母，范围为 1 至 65535。
	Denominator int32
}

// NotVeryEffectiveDamageBoost 是特性详情中最终属性相性位于零与一之间时触发的最终伤害倍率。
type NotVeryEffectiveDamageBoost struct {
	// Numerator 是伤害倍率的正整数分子，范围为 1 至 65535。
	Numerator int32
	// Denominator 是伤害倍率的正整数分母，范围为 1 至 65535。
	Denominator int32
}

// validAbilityDamageMultiplierValues 校验十类可选攻击方伤害倍率规则的完整性和封闭数值范围。
func validAbilityDamageMultiplierValues(values OptionalValues) bool {
	return validBasePowerAtMostDamageBoost(values.BasePowerAtMostDamageBoost) &&
		validRecoilSkillDamageBoost(values.RecoilSkillDamageBoost) &&
		validLowHPElementDamageBoost(values.LowHPElementDamageBoost) &&
		validWeatherElementDamageBoost(values.WeatherElementDamageBoost) &&
		validElementSkillDamageBoost(values.ElementSkillDamageBoost) &&
		validSameElementBonusOverride(values.SameElementBonusOverride) &&
		validContactBasedSkillDamageBoost(values.ContactBasedSkillDamageBoost) &&
		validCriticalHitDamageBoost(values.CriticalHitDamageBoost) &&
		validSuperEffectiveDamageBoost(values.SuperEffectiveDamageBoost) &&
		validNotVeryEffectiveDamageBoost(values.NotVeryEffectiveDamageBoost)
}

// validDamageFraction 校验资料层使用的整数倍率位于可无损冻结为 uint16 的正数范围。
func validDamageFraction(numerator, denominator int32) bool {
	return numerator >= 1 && numerator <= 65_535 && denominator >= 1 && denominator <= 65_535
}

// validBasePowerAtMostDamageBoost 校验基础威力上限与伤害倍率。
func validBasePowerAtMostDamageBoost(value *BasePowerAtMostDamageBoost) bool {
	return value == nil || (value.MaximumPower >= 1 && value.MaximumPower <= 65_535 &&
		validDamageFraction(value.Numerator, value.Denominator))
}

// validRecoilSkillDamageBoost 校验按实际伤害反作用技能倍率。
func validRecoilSkillDamageBoost(value *RecoilSkillDamageBoost) bool {
	return value == nil || validDamageFraction(value.Numerator, value.Denominator)
}

// validLowHPElementDamageBoost 校验低生命属性身份、阈值分数与伤害倍率。
func validLowHPElementDamageBoost(value *LowHPElementDamageBoost) bool {
	return value == nil || (value.ElementID != snowflake.ID(0) && value.HPThresholdNumerator >= 1 &&
		value.HPThresholdNumerator <= value.HPThresholdDenominator && value.HPThresholdDenominator <= 65_535 &&
		validDamageFraction(value.DamageNumerator, value.DamageDenominator))
}

// validWeatherElementDamageBoost 校验天气、属性集合与伤害倍率。
func validWeatherElementDamageBoost(value *WeatherElementDamageBoost) bool {
	return value == nil || (validWeatherKind(value.Weather) && validDamageBoostElementIDs(value.ElementIDs) &&
		validDamageFraction(value.Numerator, value.Denominator))
}

// validElementSkillDamageBoost 校验属性集合与伤害倍率。
func validElementSkillDamageBoost(value *ElementSkillDamageBoost) bool {
	return value == nil || (validDamageBoostElementIDs(value.ElementIDs) &&
		validDamageFraction(value.Numerator, value.Denominator))
}

// validSameElementBonusOverride 校验属性一致加成替代倍率。
func validSameElementBonusOverride(value *SameElementBonusOverride) bool {
	return value == nil || validDamageFraction(value.Numerator, value.Denominator)
}

// validContactBasedSkillDamageBoost 校验有效接触技能伤害倍率。
func validContactBasedSkillDamageBoost(value *ContactBasedSkillDamageBoost) bool {
	return value == nil || validDamageFraction(value.Numerator, value.Denominator)
}

// validCriticalHitDamageBoost 校验击中要害额外伤害倍率。
func validCriticalHitDamageBoost(value *CriticalHitDamageBoost) bool {
	return value == nil || validDamageFraction(value.Numerator, value.Denominator)
}

// validSuperEffectiveDamageBoost 校验严格克制伤害倍率。
func validSuperEffectiveDamageBoost(value *SuperEffectiveDamageBoost) bool {
	return value == nil || validDamageFraction(value.Numerator, value.Denominator)
}

// validNotVeryEffectiveDamageBoost 校验非零抗性伤害倍率。
func validNotVeryEffectiveDamageBoost(value *NotVeryEffectiveDamageBoost) bool {
	return value == nil || validDamageFraction(value.Numerator, value.Denominator)
}

// validDamageBoostElementIDs 校验属性集合非空、无零 Identifier、无重复且保持合理上限。
func validDamageBoostElementIDs(values []snowflake.ID) bool {
	if len(values) == 0 || len(values) > 32 {
		return false
	}
	seen := make(map[snowflake.ID]struct{}, len(values))
	for _, value := range values {
		if value == snowflake.ID(0) {
			return false
		}
		if _, duplicate := seen[value]; duplicate {
			return false
		}
		seen[value] = struct{}{}
	}
	return true
}

// cloneBasePowerAtMostDamageBoost 深复制可选基础威力上限伤害强化规则。
func cloneBasePowerAtMostDamageBoost(value *BasePowerAtMostDamageBoost) *BasePowerAtMostDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneRecoilSkillDamageBoost 深复制可选按实际伤害反作用技能强化规则。
func cloneRecoilSkillDamageBoost(value *RecoilSkillDamageBoost) *RecoilSkillDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneLowHPElementDamageBoost 深复制可选低生命属性伤害强化规则。
func cloneLowHPElementDamageBoost(value *LowHPElementDamageBoost) *LowHPElementDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneWeatherElementDamageBoost 深复制可选天气属性伤害强化规则及其属性集合。
func cloneWeatherElementDamageBoost(value *WeatherElementDamageBoost) *WeatherElementDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.ElementIDs = append([]snowflake.ID(nil), value.ElementIDs...)
	return &cloned
}

// cloneElementSkillDamageBoost 深复制可选属性技能伤害强化规则及其属性集合。
func cloneElementSkillDamageBoost(value *ElementSkillDamageBoost) *ElementSkillDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.ElementIDs = append([]snowflake.ID(nil), value.ElementIDs...)
	return &cloned
}

// cloneSameElementBonusOverride 深复制可选属性一致加成覆盖规则。
func cloneSameElementBonusOverride(value *SameElementBonusOverride) *SameElementBonusOverride {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneContactBasedSkillDamageBoost 深复制可选有效接触技能伤害强化规则。
func cloneContactBasedSkillDamageBoost(value *ContactBasedSkillDamageBoost) *ContactBasedSkillDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneCriticalHitDamageBoost 深复制可选击中要害伤害强化规则。
func cloneCriticalHitDamageBoost(value *CriticalHitDamageBoost) *CriticalHitDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneSuperEffectiveDamageBoost 深复制可选严格克制伤害强化规则。
func cloneSuperEffectiveDamageBoost(value *SuperEffectiveDamageBoost) *SuperEffectiveDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneNotVeryEffectiveDamageBoost 深复制可选非零抗性伤害强化规则。
func cloneNotVeryEffectiveDamageBoost(value *NotVeryEffectiveDamageBoost) *NotVeryEffectiveDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}
