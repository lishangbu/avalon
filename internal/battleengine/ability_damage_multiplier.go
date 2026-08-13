package battleengine

import "math/big"

// BasePowerAtMostDamageBoost 描述只强化原始基础威力不超过上限技能的攻击方特性。
//
// 动态威力技能若没有声明原始 Power，不会因为运行时威力较低而触发本规则；这与规则定义读取技能资料原始
// power 的语义一致，也避免资料编译器根据战斗中的临时数值反推特性条件。
type BasePowerAtMostDamageBoost struct {
	// MaximumPower 是允许触发强化的最大原始基础威力，必须大于零。
	MaximumPower uint16 `json:"maximumPower"`
	// Numerator 是最终伤害倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是最终伤害倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// RecoilSkillDamageBoost 描述只强化带有按实际伤害反作用规则技能的攻击方特性。
type RecoilSkillDamageBoost struct {
	// Numerator 是最终伤害倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是最终伤害倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// LowHPElementDamageBoost 描述低生命阈值下对一个指定有效属性的攻击方伤害强化。
type LowHPElementDamageBoost struct {
	// ElementID 是必须与本次技能有效属性一致的稳定属性 Identifier。
	ElementID Identifier `json:"elementId"`
	// HPThresholdNumerator 是生命阈值分数的正整数分子。
	HPThresholdNumerator uint16 `json:"hpThresholdNumerator"`
	// HPThresholdDenominator 是生命阈值分数的正整数分母，必须不小于分子。
	HPThresholdDenominator uint16 `json:"hpThresholdDenominator"`
	// DamageNumerator 是最终伤害倍率的正整数分子。
	DamageNumerator uint16 `json:"damageNumerator"`
	// DamageDenominator 是最终伤害倍率的正整数分母。
	DamageDenominator uint16 `json:"damageDenominator"`
}

// WeatherElementDamageBoost 描述指定有效天气下对一组有效属性的攻击方伤害强化。
type WeatherElementDamageBoost struct {
	// Weather 是触发规则所需的普通天气；强天气会先映射为对应的有效普通天气。
	Weather WeatherKind `json:"weather"`
	// ElementIDs 是允许触发强化的非空、无重复稳定属性 Identifier 集合。
	ElementIDs []Identifier `json:"elementIds"`
	// Numerator 是最终伤害倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是最终伤害倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// ElementSkillDamageBoost 描述对一组技能有效属性的攻击方伤害强化。
type ElementSkillDamageBoost struct {
	// ElementIDs 是允许触发强化的非空、无重复稳定属性 Identifier 集合。
	ElementIDs []Identifier `json:"elementIds"`
	// Numerator 是最终伤害倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是最终伤害倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// SameElementBonusOverride 描述技能有效属性与使用者当前属性一致时替换默认本系加成的规则。
type SameElementBonusOverride struct {
	// Numerator 是替代默认本系加成的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是替代默认本系加成的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// ContactBasedSkillDamageBoost 描述技能本次仍构成有效接触时的攻击方伤害强化。
type ContactBasedSkillDamageBoost struct {
	// Numerator 是最终伤害倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是最终伤害倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// CriticalHitDamageBoost 描述技能本次实际击中要害时的额外攻击方伤害强化。
type CriticalHitDamageBoost struct {
	// Numerator 是最终伤害倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是最终伤害倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// SuperEffectiveDamageBoost 描述最终属性相性严格大于一时的攻击方伤害强化。
type SuperEffectiveDamageBoost struct {
	// Numerator 是最终伤害倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是最终伤害倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// NotVeryEffectiveDamageBoost 描述最终属性相性严格位于零与一之间时的攻击方伤害强化。
type NotVeryEffectiveDamageBoost struct {
	// Numerator 是最终伤害倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是最终伤害倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// validAbilityDamageMultipliers 校验成员冻结的十类攻击方伤害倍率规则。
func validAbilityDamageMultipliers(member MemberSnapshot) bool {
	return validBasePowerAtMostDamageBoost(member.BasePowerAtMostDamageBoost) &&
		validPositiveDamageFraction(member.RecoilSkillDamageBoost) &&
		validLowHPElementDamageBoost(member.LowHPElementDamageBoost) &&
		validWeatherElementDamageBoost(member.WeatherElementDamageBoost) &&
		validElementSkillDamageBoost(member.ElementSkillDamageBoost) &&
		validPositiveDamageFraction(member.SameElementBonusOverride) &&
		validPositiveDamageFraction(member.ContactBasedSkillDamageBoost) &&
		validPositiveDamageFraction(member.CriticalHitDamageBoost) &&
		validPositiveDamageFraction(member.SuperEffectiveDamageBoost) &&
		validPositiveDamageFraction(member.NotVeryEffectiveDamageBoost)
}

// positiveDamageFraction 是仅供本文件统一校验各独立强类型分数的最小内部契约。
type positiveDamageFraction interface {
	RecoilSkillDamageBoost | SameElementBonusOverride | ContactBasedSkillDamageBoost |
		CriticalHitDamageBoost | SuperEffectiveDamageBoost | NotVeryEffectiveDamageBoost
}

// validPositiveDamageFraction 校验一个可选独立倍率拥有非零分子和分母。
func validPositiveDamageFraction[T positiveDamageFraction](value *T) bool {
	if value == nil {
		return true
	}
	switch fraction := any(*value).(type) {
	case RecoilSkillDamageBoost:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case SameElementBonusOverride:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case ContactBasedSkillDamageBoost:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case CriticalHitDamageBoost:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case SuperEffectiveDamageBoost:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case NotVeryEffectiveDamageBoost:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	default:
		return false
	}
}

// validBasePowerAtMostDamageBoost 校验低基础威力强化的上限和正分数。
func validBasePowerAtMostDamageBoost(value *BasePowerAtMostDamageBoost) bool {
	return value == nil || (value.MaximumPower != 0 && value.Numerator != 0 && value.Denominator != 0)
}

// validLowHPElementDamageBoost 校验低生命属性强化的属性、阈值和伤害正分数。
func validLowHPElementDamageBoost(value *LowHPElementDamageBoost) bool {
	return value == nil || (value.ElementID != 0 && value.HPThresholdNumerator != 0 &&
		value.HPThresholdDenominator >= value.HPThresholdNumerator && value.DamageNumerator != 0 &&
		value.DamageDenominator != 0)
}

// validWeatherElementDamageBoost 校验天气属性强化的天气、属性集合和正分数。
func validWeatherElementDamageBoost(value *WeatherElementDamageBoost) bool {
	return value == nil || (value.Weather.valid() && validDamageBoostElementIDs(value.ElementIDs) &&
		value.Numerator != 0 && value.Denominator != 0)
}

// validElementSkillDamageBoost 校验属性技能强化的属性集合和正分数。
func validElementSkillDamageBoost(value *ElementSkillDamageBoost) bool {
	return value == nil || (validDamageBoostElementIDs(value.ElementIDs) && value.Numerator != 0 && value.Denominator != 0)
}

// validDamageBoostElementIDs 校验伤害倍率使用的属性集合非空、无空值且没有重复项。
func validDamageBoostElementIDs(values []Identifier) bool {
	if len(values) == 0 || len(values) > 32 {
		return false
	}
	seen := make(map[Identifier]struct{}, len(values))
	for _, value := range values {
		if !value.IsValid() {
			return false
		}
		if _, duplicate := seen[value]; duplicate {
			return false
		}
		seen[value] = struct{}{}
	}
	return true
}

// abilitySameElementBonus 返回本次属性一致加成使用的精确分数。
//
// 太晶化只覆盖当前防守属性，不能抹除冻结的自然属性攻击加成：技能匹配自然属性或太晶属性任一项时仍获得
// 加成；两者同时匹配时现代规则使用 2 倍，拥有属性一致覆盖特性时使用固定 9/4 倍。该特例对应资料
// sameElementBonus 的结算位置，不能把覆盖倍率再作为最终倍率叠乘。
func abilitySameElementBonus(attacker MemberSnapshot, skillElementID Identifier) (uint64, uint64) {
	originalElementIDs := attacker.NaturalElementIDs
	if len(originalElementIDs) == 0 {
		// NewState 会补齐自然属性；此回退仅保持纯函数夹具和损坏诊断路径可读。
		originalElementIDs = attacker.ElementIDs
	}
	hasOriginalBonus := containsString(originalElementIDs, skillElementID)
	hasTeraBonus := attacker.Terastallized && attacker.TeraElementID == skillElementID
	if !hasOriginalBonus && !hasTeraBonus {
		return 1, 1
	}
	if hasOriginalBonus && hasTeraBonus {
		if attacker.SameElementBonusOverride != nil {
			return 9, 4
		}
		return 2, 1
	}
	if attacker.SameElementBonusOverride != nil {
		return uint64(attacker.SameElementBonusOverride.Numerator), uint64(attacker.SameElementBonusOverride.Denominator)
	}
	return 3, 2
}

// abilityFinalDamageMultiplier 合并本次已经满足条件的九类最终伤害倍率。
//
// 本系覆盖由 abilitySameElementBonus 在独立取整位置处理。这里使用标准库大整数承载倍率乘积，避免一个特性同时
// 声明多条结构化效果时因 uint64 中间乘积溢出而破坏确定性重放。
func abilityFinalDamageMultiplier(
	weather *WeatherEffect,
	attacker, defender MemberSnapshot,
	skill SkillSnapshot,
	skillElementID Identifier,
	criticalHit bool,
	effectivenessNumerator, effectivenessDenominator uint64,
	ignoreDefenderAbility bool,
	allyModifiers abilityAllyDamageModifiers,
) (*big.Int, *big.Int) {
	numerator, denominator := big.NewInt(1), big.NewInt(1)
	appendFraction := func(fractionNumerator, fractionDenominator uint16) {
		numerator.Mul(numerator, big.NewInt(int64(fractionNumerator)))
		denominator.Mul(denominator, big.NewInt(int64(fractionDenominator)))
	}
	if value := attacker.BasePowerAtMostDamageBoost; value != nil && skill.Power != 0 && skill.Power <= value.MaximumPower {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.RecoilSkillDamageBoost; value != nil && skill.DrainPercent < 0 {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.LowHPElementDamageBoost; value != nil && skillElementID == value.ElementID &&
		uint64(attacker.CurrentHP)*uint64(value.HPThresholdDenominator) <= uint64(attacker.MaxHP)*uint64(value.HPThresholdNumerator) {
		appendFraction(value.DamageNumerator, value.DamageDenominator)
	}
	if value := attacker.WeatherElementDamageBoost; value != nil && weather != nil && weather.Kind == value.Weather &&
		containsString(value.ElementIDs, skillElementID) {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.ElementSkillDamageBoost; value != nil && containsString(value.ElementIDs, skillElementID) {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.ContactBasedSkillDamageBoost; value != nil && skillMakesEffectiveContact(attacker, skill) {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.CriticalHitDamageBoost; value != nil && criticalHit {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.SuperEffectiveDamageBoost; value != nil && effectivenessNumerator > effectivenessDenominator {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.NotVeryEffectiveDamageBoost; value != nil && effectivenessNumerator != 0 &&
		effectivenessNumerator < effectivenessDenominator {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.TargetGenderDamageMultiplier; value != nil && attacker.GenderCode != "" && defender.GenderCode != "" {
		if attacker.GenderCode == defender.GenderCode {
			appendFraction(value.SameGenderNumerator, value.SameGenderDenominator)
		} else {
			appendFraction(value.OppositeGenderNumerator, value.OppositeGenderDenominator)
		}
	}
	if value := attacker.PunchBasedSkillDamageBoost; value != nil && skill.PunchBased {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.SlicingBasedSkillDamageBoost; value != nil && skill.SlicingBased {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.SoundBasedSkillDamageBoost; value != nil && skill.SoundBased {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.PulseBasedSkillDamageBoost; value != nil && skill.PulseBased {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.BiteBasedSkillDamageBoost; value != nil && skill.BiteBased {
		appendFraction(value.Numerator, value.Denominator)
	}
	if value := attacker.SecondaryEffectsSuppressedDamageBoost; value != nil && skillHasSecondaryStatusOrStatEffects(skill) {
		appendFraction(value.Numerator, value.Denominator)
	}
	if !ignoreDefenderAbility {
		if value := defender.SoundBasedSkillDamageReduction; value != nil && skill.SoundBased {
			appendFraction(value.Numerator, value.Denominator)
		}
		if value := defender.SuperEffectiveDamageReduction; value != nil && effectivenessNumerator > effectivenessDenominator {
			appendFraction(value.Numerator, value.Denominator)
		}
		if value := defender.FullHPDamageReduction; value != nil && defender.CurrentHP >= defender.MaxHP {
			appendFraction(value.Numerator, value.Denominator)
		}
		if value := defender.DamageClassDamageReduction; value != nil && containsDamageClass(value.DamageClasses, skill.DamageClass) {
			appendFraction(value.Numerator, value.Denominator)
		}
		if value := defender.ElementSkillDamageReduction; value != nil && containsString(value.ElementIDs, skillElementID) {
			appendFraction(value.Numerator, value.Denominator)
		}
		if value := defender.ContactBasedSkillDamageReduction; value != nil && skillMakesEffectiveContact(attacker, skill) {
			appendFraction(value.Numerator, value.Denominator)
		}
	}
	for _, value := range allyModifiers.skillDamageBoosts {
		appendFraction(value.numerator, value.denominator)
	}
	for _, value := range allyModifiers.receivedDamageReductions {
		appendFraction(value.numerator, value.denominator)
	}
	return numerator, denominator
}

// finalizeDamageWithAbilityMultiplier 把现有公式分数与攻击方特性倍率合并后只执行一次向下取整。
func finalizeDamageWithAbilityMultiplier(
	baseNumerator, baseDenominator uint64,
	weather *WeatherEffect,
	attacker, defender MemberSnapshot,
	skill SkillSnapshot,
	skillElementID Identifier,
	criticalHit bool,
	effectivenessNumerator, effectivenessDenominator uint64,
	ignoreDefenderAbility bool,
	allyModifiers abilityAllyDamageModifiers,
) uint32 {
	if baseNumerator == 0 {
		return 0
	}
	abilityNumerator, abilityDenominator := abilityFinalDamageMultiplier(
		weather,
		attacker,
		defender,
		skill,
		skillElementID,
		criticalHit,
		effectivenessNumerator,
		effectivenessDenominator,
		ignoreDefenderAbility,
		allyModifiers,
	)
	numerator := new(big.Int).SetUint64(baseNumerator)
	numerator.Mul(numerator, abilityNumerator)
	denominator := new(big.Int).SetUint64(baseDenominator)
	denominator.Mul(denominator, abilityDenominator)
	damage := new(big.Int).Quo(numerator, denominator)
	if damage.Sign() == 0 {
		return 1
	}
	if !damage.IsUint64() || damage.Uint64() > uint64(^uint32(0)) {
		return ^uint32(0)
	}
	return uint32(damage.Uint64())
}

// cloneBasePowerAtMostDamageBoost 深复制可选低基础威力伤害强化规则。
func cloneBasePowerAtMostDamageBoost(value *BasePowerAtMostDamageBoost) *BasePowerAtMostDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneRecoilSkillDamageBoost 深复制可选反作用技能伤害强化规则。
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
	cloned.ElementIDs = append([]Identifier(nil), value.ElementIDs...)
	return &cloned
}

// cloneElementSkillDamageBoost 深复制可选属性技能伤害强化规则及其属性集合。
func cloneElementSkillDamageBoost(value *ElementSkillDamageBoost) *ElementSkillDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.ElementIDs = append([]Identifier(nil), value.ElementIDs...)
	return &cloned
}

// cloneSameElementBonusOverride 深复制可选本系加成覆盖规则。
func cloneSameElementBonusOverride(value *SameElementBonusOverride) *SameElementBonusOverride {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneContactBasedSkillDamageBoost 深复制可选接触技能伤害强化规则。
func cloneContactBasedSkillDamageBoost(value *ContactBasedSkillDamageBoost) *ContactBasedSkillDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneCriticalHitDamageBoost 深复制可选要害伤害强化规则。
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
