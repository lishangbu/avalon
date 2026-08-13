package skilldetail

// WeatherPowerMultiplier 描述技能在指定普通天气下调整基础威力的一条独立资料规则。
//
// Numerator 与 Denominator 使用整数分数表达倍率，避免持久化浮点数使权威伤害回放出现平台相关舍入差异。它只
// 改变普通伤害公式的基础威力，不建立天气，也不替换属性或命中率。
type WeatherPowerMultiplier struct {
	// Weather 是此倍率适用的普通天气种类，不能为 none。
	Weather WeatherKind `json:"weather"`
	// Numerator 是正倍率分子。
	Numerator int32 `json:"numerator"`
	// Denominator 是正倍率分母，不能为零。
	Denominator int32 `json:"denominator"`
}

// validWeatherPowerMultipliers 校验天气种类、每种天气唯一性和十倍倍率上限。
func validWeatherPowerMultipliers(values []WeatherPowerMultiplier) bool {
	if len(values) > 4 {
		return false
	}
	seen := make(map[WeatherKind]struct{}, len(values))
	for _, value := range values {
		if !value.Weather.Valid() || value.Numerator < 1 || value.Denominator < 1 ||
			int64(value.Numerator) > int64(value.Denominator)*10 {
			return false
		}
		if _, duplicate := seen[value.Weather]; duplicate {
			return false
		}
		seen[value.Weather] = struct{}{}
	}
	return true
}

// cloneWeatherPowerMultipliers 复制倍率切片，隔离命令、审计快照和 Battle 冻结边界持有的可变底层数组。
func cloneWeatherPowerMultipliers(values []WeatherPowerMultiplier) []WeatherPowerMultiplier {
	if values == nil {
		return []WeatherPowerMultiplier{}
	}
	return append([]WeatherPowerMultiplier(nil), values...)
}
