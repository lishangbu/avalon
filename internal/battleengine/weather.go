package battleengine

import "fmt"

// WeatherKind 是引擎支持的普通全场天气种类。
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

// valid 报告天气种类是否为当前引擎能够严格解释的封闭值。
func (kind WeatherKind) valid() bool {
	return kind == WeatherKindSun || kind == WeatherKindRain || kind == WeatherKindSandstorm || kind == WeatherKindSnow
}

// WeatherEffect 是已写入全场环境、会跨回合持续的普通天气。
type WeatherEffect struct {
	// Kind 是当前生效的封闭天气种类。
	Kind WeatherKind `json:"kind"`
	// TurnsRemaining 是包含当前结算回合在内的剩余完整天气回合数，必须为正数。
	TurnsRemaining uint8 `json:"turnsRemaining"`
}

// advanceTurn 推进一个完整回合后的天气；nil 表示天气在本回合末自然结束。
func (effect WeatherEffect) advanceTurn() *WeatherEffect {
	if effect.TurnsRemaining <= 1 {
		return nil
	}
	effect.TurnsRemaining--
	return &effect
}

// WeatherApplication 描述技能命中后尝试建立普通天气的独立规则。
type WeatherApplication struct {
	// Effect 是成功建立时写入全场环境的天气和持续回合。
	Effect WeatherEffect `json:"effect"`
	// ChancePercent 是天气建立的独立触发概率；100 表示必定建立且不消费概率随机数。
	ChancePercent uint8 `json:"chancePercent"`
}

// validateWeatherEffect 校验已经生效或即将生效的天气值。
func validateWeatherEffect(effect WeatherEffect) error {
	if !effect.Kind.valid() || effect.TurnsRemaining == 0 {
		return fmt.Errorf("%w: 天气效果无效", ErrInvalidInitialState)
	}
	return nil
}

// validateWeatherApplication 校验资料编译后冻结到技能快照的天气建立规则。
func validateWeatherApplication(application WeatherApplication) error {
	if application.ChancePercent == 0 || application.ChancePercent > 100 {
		return fmt.Errorf("%w: 天气触发概率无效", ErrInvalidInitialState)
	}
	return validateWeatherEffect(application.Effect)
}
