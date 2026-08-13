package abilitydetail

// WeatherKind 是特性资料可声明的普通全场天气类别。
//
// 特性资料拥有独立的天气枚举，不能依赖技能详情包中的类型；两类资料有不同的生命周期和校验边界，Battle 会在
// 创建对局时将它们共同编译为战斗引擎的冻结天气规则。
type WeatherKind string

const (
	// WeatherKindSun 表示日照天气。
	WeatherKindSun WeatherKind = "sun"
	// WeatherKindRain 表示降雨天气。
	WeatherKindRain WeatherKind = "rain"
	// WeatherKindSandstorm 表示沙暴天气。
	WeatherKindSandstorm WeatherKind = "sandstorm"
	// WeatherKindSnow 表示降雪天气。
	WeatherKindSnow WeatherKind = "snow"
)

// validWeatherKind 报告天气是否属于当前战斗引擎支持的封闭集合。
func validWeatherKind(value WeatherKind) bool {
	switch value {
	case WeatherKindSun, WeatherKindRain, WeatherKindSandstorm, WeatherKindSnow:
		return true
	default:
		return false
	}
}

// cloneWeatherDamageImmunities 深复制特性声明的天气伤害免疫集合。
func cloneWeatherDamageImmunities(values []WeatherKind) []WeatherKind {
	return append([]WeatherKind(nil), values...)
}

// validWeatherDamageImmunities 校验特性声明的天气伤害免疫集合。
//
// 该资料只用于免除普通天气的回合末环境伤害，不能隐式影响天气建立、持续时间、命中、属性替换或技能威力。
// 每种天气最多出现一次，避免资料数组顺序形成隐藏的规则优先级。
func validWeatherDamageImmunities(values []WeatherKind) bool {
	if len(values) > 4 {
		return false
	}
	seen := make(map[WeatherKind]struct{}, len(values))
	for _, value := range values {
		if !validWeatherKind(value) {
			return false
		}
		if _, duplicated := seen[value]; duplicated {
			return false
		}
		seen[value] = struct{}{}
	}
	return true
}
