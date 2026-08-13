package abilitydetail

// WeatherSpeedMultiplier 是特性声明的指定普通天气行动速度整数分数倍率。
//
// 它只参与回合行动排序，不改变成员基础速度、能力阶级、天气持续时间或伤害公式。分子和分母避免使用浮点数，
// 使冻结快照与离线重放在所有平台上保持确定性。
type WeatherSpeedMultiplier struct {
	// Weather 是此速度倍率适用的封闭普通天气种类。
	Weather WeatherKind `json:"weather"`
	// Numerator 是正整数倍率分子。
	Numerator int32 `json:"numerator"`
	// Denominator 是正整数倍率分母。
	Denominator int32 `json:"denominator"`
}

// cloneWeatherSpeedMultipliers 深复制特性声明的天气速度倍率集合。
func cloneWeatherSpeedMultipliers(values []WeatherSpeedMultiplier) []WeatherSpeedMultiplier {
	return append([]WeatherSpeedMultiplier(nil), values...)
}

// validWeatherSpeedMultipliers 校验天气速度倍率集合。
//
// 同一天气只允许一项，避免数组顺序决定行动先后；最多四项对应当前封闭普通天气集合。
func validWeatherSpeedMultipliers(values []WeatherSpeedMultiplier) bool {
	if len(values) > 4 {
		return false
	}
	seen := make(map[WeatherKind]struct{}, len(values))
	for _, value := range values {
		if !validWeatherKind(value.Weather) || value.Numerator < 1 || value.Numerator > 65_535 || value.Denominator < 1 || value.Denominator > 65_535 {
			return false
		}
		if _, duplicated := seen[value.Weather]; duplicated {
			return false
		}
		seen[value.Weather] = struct{}{}
	}
	return true
}
