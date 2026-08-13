package abilitydetail

// StrongWeatherKind 是特性资料可声明的封闭强天气种类。
//
// 强天气不是普通天气的无限时长写法：它只能被同级强天气覆盖，并在最后一个来源离场后结束。因此资料、
// Battle 编译和战斗快照都使用独立枚举，禁止复用普通天气字段或由显示文本推断。
type StrongWeatherKind string

const (
	// StrongWeatherKindHarshSunlight 表示终结之地式强日照。
	StrongWeatherKindHarshSunlight StrongWeatherKind = "harshSunlight"
	// StrongWeatherKindHeavyRain 表示始源之海式强降雨。
	StrongWeatherKindHeavyRain StrongWeatherKind = "heavyRain"
	// StrongWeatherKindStrongWinds 表示德尔塔气流式强风。
	StrongWeatherKindStrongWinds StrongWeatherKind = "strongWinds"
)

// SwitchInStrongWeather 是特性在持有成员进入场地时建立的一条独立强天气规则。
type SwitchInStrongWeather struct {
	// Weather 是进入场地后建立的封闭强天气种类。
	Weather StrongWeatherKind
}

// cloneSwitchInStrongWeather 深复制可选的入场强天气资料。
func cloneSwitchInStrongWeather(value *SwitchInStrongWeather) *SwitchInStrongWeather {
	if value == nil {
		return nil
	}
	return &SwitchInStrongWeather{Weather: value.Weather}
}

// validSwitchInStrongWeather 校验完整的入场强天气资料。
func validSwitchInStrongWeather(value *SwitchInStrongWeather) bool {
	if value == nil {
		return true
	}
	switch value.Weather {
	case StrongWeatherKindHarshSunlight, StrongWeatherKindHeavyRain, StrongWeatherKindStrongWinds:
		return true
	default:
		return false
	}
}
