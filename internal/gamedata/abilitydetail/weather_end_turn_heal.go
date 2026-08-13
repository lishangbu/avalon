package abilitydetail

// WeatherEndTurnHeal 是特性声明的普通天气回合末固定比例回复规则。
//
// 它只描述哪些天气会触发以及回复最大生命值时使用的正分母。天气持续、天气伤害和其它生命效果拥有各自的
// 生命周期，不能合并到此资料中，也不能用特性显示文本推断。
type WeatherEndTurnHeal struct {
	// Weathers 是可以触发本次回复的封闭普通天气集合；每种天气最多出现一次。
	Weathers []WeatherKind
	// HealDenominator 是最大生命值回复比例的正分母，例如 16 表示每回合回复最大生命值的十六分之一。
	HealDenominator int32
}

// cloneWeatherEndTurnHeal 深复制一条可选的天气回合末回复规则。
func cloneWeatherEndTurnHeal(value *WeatherEndTurnHeal) *WeatherEndTurnHeal {
	if value == nil {
		return nil
	}
	return &WeatherEndTurnHeal{
		Weathers:        append([]WeatherKind(nil), value.Weathers...),
		HealDenominator: value.HealDenominator,
	}
}

// validWeatherEndTurnHeal 校验天气回合末回复的完整资料。
//
// 未声明该效果时使用 nil；一旦声明，天气集合和正分母必须同时存在。这样读取边界可以可靠地区分“没有这条
// 特性规则”与“被应用外 SQL 写坏的半条规则”。
func validWeatherEndTurnHeal(value *WeatherEndTurnHeal) bool {
	if value == nil {
		return true
	}
	if len(value.Weathers) == 0 || len(value.Weathers) > 4 || value.HealDenominator < 1 || value.HealDenominator > 65_535 {
		return false
	}
	seen := make(map[WeatherKind]struct{}, len(value.Weathers))
	for _, weather := range value.Weathers {
		if !validWeatherKind(weather) {
			return false
		}
		if _, duplicated := seen[weather]; duplicated {
			return false
		}
		seen[weather] = struct{}{}
	}
	return true
}
