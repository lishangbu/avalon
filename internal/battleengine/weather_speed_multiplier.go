package battleengine

import "fmt"

// WeatherSpeedMultiplier 是成员特性冻结到战斗快照后的普通天气行动速度整数分数倍率。
//
// 它只参与同优先度行动排序，不能修改成员基础速度或天气环境。整数分数用于确保快照、重放和不同平台的舍入
// 结果完全一致。
type WeatherSpeedMultiplier struct {
	// Weather 是本倍率适用的封闭普通天气种类。
	Weather WeatherKind `json:"weather"`
	// Numerator 是正整数倍率分子。
	Numerator uint32 `json:"numerator"`
	// Denominator 是正整数倍率分母。
	Denominator uint32 `json:"denominator"`
}

// validateWeatherSpeedMultipliers 校验成员冻结的天气速度倍率集合。
func validateWeatherSpeedMultipliers(values []WeatherSpeedMultiplier) error {
	if len(values) > 4 {
		return fmt.Errorf("天气速度倍率超过上限: %d", len(values))
	}
	seen := make(map[WeatherKind]struct{}, len(values))
	for _, value := range values {
		if !value.Weather.valid() || value.Numerator == 0 || value.Numerator > 65_535 || value.Denominator == 0 || value.Denominator > 65_535 {
			return fmt.Errorf("天气速度倍率无效: %+v", value)
		}
		if _, duplicated := seen[value.Weather]; duplicated {
			return fmt.Errorf("天气速度倍率天气重复: %q", value.Weather)
		}
		seen[value.Weather] = struct{}{}
	}
	return nil
}

// weatherSpeedMultiplier 返回成员在当前普通天气适用的速度整数分数倍率。
func weatherSpeedMultiplier(member MemberSnapshot, weather WeatherKind) (WeatherSpeedMultiplier, bool) {
	for _, candidate := range member.WeatherSpeedMultipliers {
		if candidate.Weather == weather {
			return candidate, true
		}
	}
	return WeatherSpeedMultiplier{}, false
}

// applySpeedMultiplier 按整数分数计算速度，并在上界饱和、下界保留为 1。
func applySpeedMultiplier(speed uint32, multiplier WeatherSpeedMultiplier) uint32 {
	value := uint64(speed) * uint64(multiplier.Numerator) / uint64(multiplier.Denominator)
	if value == 0 {
		return 1
	}
	if value > uint64(^uint32(0)) {
		return ^uint32(0)
	}
	return uint32(value)
}
