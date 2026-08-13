package abilitydetail

// SwitchInWeather 是特性持有成员进入场地时建立普通天气的独立资料规则。
//
// 它与技能天气和强天气分开：本规则没有概率，会在成员实际换入后尝试覆盖普通天气；强天气存在时不会
// 生效。持续回合属于本规则自身，不能通过特性名称或空值推断。
type SwitchInWeather struct {
	// Weather 是进入场地后尝试建立的封闭普通天气种类。
	Weather WeatherKind
	// TurnsRemaining 是天气建立时的正剩余完整回合数。
	TurnsRemaining int32
}

// cloneSwitchInWeather 深复制可选的入场普通天气资料。
func cloneSwitchInWeather(value *SwitchInWeather) *SwitchInWeather {
	if value == nil {
		return nil
	}
	return &SwitchInWeather{Weather: value.Weather, TurnsRemaining: value.TurnsRemaining}
}

// validSwitchInWeather 校验完整的入场普通天气资料。
func validSwitchInWeather(value *SwitchInWeather) bool {
	return value == nil || (validWeatherKind(value.Weather) && value.TurnsRemaining >= 1 && value.TurnsRemaining <= 100)
}
