package battleengine

import "fmt"

// DamageFraction 描述一项只在明确结算窗口生效的正整数伤害倍率。
//
// 该值不携带触发条件；持有它的具体特性规则负责决定何时应用，避免同一个分数被误解为基础威力、最终伤害或
// 属性相性。分子和分母都必须为正数。
type DamageFraction struct {
	// Numerator 是伤害倍率的正分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是伤害倍率的正分母。
	Denominator uint16 `json:"denominator"`
}

// SkillElementConversion 描述特性对持有成员所使用技能执行的单向属性转换及转换专属威力倍率。
//
// 只有技能在天气等资料规则处理后的有效属性等于 SourceElementID 时才会转换；DamageNumerator 与
// DamageDenominator 只强化本次确实发生的转换，原生目标属性技能不会误获倍率。
type SkillElementConversion struct {
	// SourceElementID 是允许转换的来源属性稳定 Identifier。
	SourceElementID Identifier `json:"sourceElementId"`
	// TargetElementID 是转换后的目标属性稳定 Identifier。
	TargetElementID Identifier `json:"targetElementId"`
	// DamageNumerator 是转换成功后基础威力倍率的正分子。
	DamageNumerator uint16 `json:"damageNumerator"`
	// DamageDenominator 是转换成功后基础威力倍率的正分母。
	DamageDenominator uint16 `json:"damageDenominator"`
}

// validateDamageFraction 校验独立伤害倍率具备可执行的正整数分数。
func validateDamageFraction(value *DamageFraction) error {
	if value == nil {
		return nil
	}
	if value.Numerator == 0 || value.Denominator == 0 {
		return fmt.Errorf("伤害倍率必须为正整数分数")
	}
	return nil
}

// cloneDamageFraction 深复制独立伤害倍率。
func cloneDamageFraction(value *DamageFraction) *DamageFraction {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// ValidateDamageFraction 校验资料层准备冻结的独立伤害倍率。
func ValidateDamageFraction(value *DamageFraction) error {
	return validateDamageFraction(value)
}

// validateSkillElementConversion 校验技能属性转换拥有完整身份与可执行倍率。
func validateSkillElementConversion(value *SkillElementConversion) error {
	if value == nil {
		return nil
	}
	if value.SourceElementID == 0 || value.TargetElementID == 0 || value.SourceElementID == value.TargetElementID ||
		value.DamageNumerator == 0 || value.DamageDenominator == 0 {
		return fmt.Errorf("技能属性转换规则无效")
	}
	return nil
}

// cloneSkillElementConversion 深复制技能属性转换规则。
func cloneSkillElementConversion(value *SkillElementConversion) *SkillElementConversion {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// ValidateSkillElementConversion 校验资料层准备冻结的技能属性转换规则。
func ValidateSkillElementConversion(value *SkillElementConversion) error {
	return validateSkillElementConversion(value)
}

// effectiveSkillWeather 返回持有成员本次使用技能时应观察到的普通天气语义。
//
// 天气封锁优先于特性内部覆盖；否则显式覆盖只影响该成员使用技能的蓄力、命中、属性和伤害计算，不创建环境天气，
// 也不影响行动速度、回合末天气伤害或其它成员的技能。
func effectiveSkillWeather(state State, actor MemberSnapshot) *WeatherEffect {
	if weatherEffectsSuppressed(state) {
		return nil
	}
	if actor.SkillWeatherOverride.valid() {
		return &WeatherEffect{Kind: actor.SkillWeatherOverride, TurnsRemaining: 1}
	}
	return effectiveWeather(state)
}

// effectiveSkillElementForMember 返回应用天气属性覆盖和持有者特性转换后的最终技能属性。
func effectiveSkillElementForMember(actor MemberSnapshot, skill SkillSnapshot, weather *WeatherEffect) Identifier {
	elementID := effectiveSkillElement(skill, weather)
	if conversion := actor.SkillElementConversion; conversion != nil && elementID == conversion.SourceElementID {
		return conversion.TargetElementID
	}
	return elementID
}

// skillElementConversionPowerMultiplier 返回本次确实发生属性转换时使用的基础威力倍率。
func skillElementConversionPowerMultiplier(actor MemberSnapshot, skill SkillSnapshot, weather *WeatherEffect) (uint64, uint64) {
	conversion := actor.SkillElementConversion
	if conversion == nil || effectiveSkillElement(skill, weather) != conversion.SourceElementID {
		return 1, 1
	}
	return uint64(conversion.DamageNumerator), uint64(conversion.DamageDenominator)
}

// protectionBypassDamageMultiplier 返回接触特性实际穿透当前个人保护时使用的独立伤害倍率。
func protectionBypassDamageMultiplier(attacker, defender MemberSnapshot, skill SkillSnapshot) (uint64, uint64) {
	if defender.ProtectionTurnsRemaining == 0 || !attacker.ContactSkillProtectionBypass ||
		!skillMakesEffectiveContact(attacker, skill) || attacker.ContactSkillProtectionBypassDamageMultiplier == nil {
		return 1, 1
	}
	value := attacker.ContactSkillProtectionBypassDamageMultiplier
	return uint64(value.Numerator), uint64(value.Denominator)
}
