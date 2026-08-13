package battleengine

import "fmt"

// WeatherPowerMultiplier 描述技能在指定普通天气下调整基础威力的一条冻结规则。
//
// 它只在普通伤害公式读取基础威力后生效，不建立天气，也不改变技能属性、命中率或天气自身的火水属性修正。分子
// 和分母以整数保存，避免浮点运算使权威回放在不同平台产生舍入差异。
type WeatherPowerMultiplier struct {
	// Weather 是此倍率适用的普通天气，不能为 none。
	Weather WeatherKind `json:"weather"`
	// Numerator 是威力倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是威力倍率的正整数分母，不能为零。
	Denominator uint16 `json:"denominator"`
}

// validateWeatherPowerMultipliers 校验单个技能冻结的天气威力倍率集合。
//
// 每种天气最多配置一条倍率。倍率上限与资料层保持为十倍，既能表达已迁移的翻倍和减半规则，也避免异常资料在
// 整数伤害公式中造成无意义的大数计算。
func validateWeatherPowerMultipliers(multipliers []WeatherPowerMultiplier) error {
	if len(multipliers) > 4 {
		return fmt.Errorf("天气威力倍率超过上限: %d", len(multipliers))
	}
	seen := make(map[WeatherKind]struct{}, len(multipliers))
	for _, multiplier := range multipliers {
		if !multiplier.Weather.valid() {
			return fmt.Errorf("天气威力倍率天气无效: %q", multiplier.Weather)
		}
		if multiplier.Numerator == 0 || multiplier.Denominator == 0 {
			return fmt.Errorf("天气威力倍率分子或分母为零")
		}
		if uint32(multiplier.Numerator) > uint32(multiplier.Denominator)*10 {
			return fmt.Errorf("天气威力倍率超过十倍上限")
		}
		if _, duplicated := seen[multiplier.Weather]; duplicated {
			return fmt.Errorf("天气威力倍率天气重复: %q", multiplier.Weather)
		}
		seen[multiplier.Weather] = struct{}{}
	}
	return nil
}

// weatherPowerMultiplier 返回当前普通天气下技能应使用的基础威力分数。
//
// 没有生效天气或找不到匹配条目时严格返回一倍；调用方不会根据技能名称、属性或天气名称推断隐式倍率。
func weatherPowerMultiplier(skill SkillSnapshot, weather *WeatherEffect) (uint64, uint64) {
	if weather == nil {
		return 1, 1
	}
	for _, multiplier := range skill.WeatherPowerMultipliers {
		if multiplier.Weather == weather.Kind {
			return uint64(multiplier.Numerator), uint64(multiplier.Denominator)
		}
	}
	return 1, 1
}
