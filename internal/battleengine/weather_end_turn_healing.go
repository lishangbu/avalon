package battleengine

import "fmt"

// WeatherEndTurnHealing 是成员特性冻结到战斗快照后的普通天气回合末回复规则。
//
// 它不保存特性资料身份，也不建立、延长或删除天气；引擎只在普通天气回合末阶段读取该快照，并为实际回复
// 单独产生 weatherHealingApplied 事件。
type WeatherEndTurnHealing struct {
	// Weathers 是触发回复的封闭普通天气集合；每种天气最多出现一次。
	Weathers []WeatherKind `json:"weathers"`
	// HealDenominator 是最大生命值回复比例的正分母，例如 16 表示回复十六分之一。
	HealDenominator uint32 `json:"healDenominator"`
}

// validateWeatherEndTurnHealing 校验成员冻结的天气回合末回复规则。
func validateWeatherEndTurnHealing(value *WeatherEndTurnHealing) error {
	if value == nil {
		return nil
	}
	if len(value.Weathers) == 0 || len(value.Weathers) > 4 {
		return fmt.Errorf("天气回合末回复天气数量无效: %d", len(value.Weathers))
	}
	if value.HealDenominator == 0 || value.HealDenominator > 65_535 {
		return fmt.Errorf("天气回合末回复分母无效: %d", value.HealDenominator)
	}
	seen := make(map[WeatherKind]struct{}, len(value.Weathers))
	for _, weather := range value.Weathers {
		if !weather.valid() {
			return fmt.Errorf("天气回合末回复天气无效: %q", weather)
		}
		if _, duplicated := seen[weather]; duplicated {
			return fmt.Errorf("天气回合末回复天气重复: %q", weather)
		}
		seen[weather] = struct{}{}
	}
	return nil
}

// cloneWeatherEndTurnHealing 深复制成员冻结的天气回合末回复规则。
func cloneWeatherEndTurnHealing(value *WeatherEndTurnHealing) *WeatherEndTurnHealing {
	if value == nil {
		return nil
	}
	return &WeatherEndTurnHealing{
		Weathers:        append([]WeatherKind(nil), value.Weathers...),
		HealDenominator: value.HealDenominator,
	}
}

// healsInWeather 报告成员的特性规则是否会在指定普通天气回复生命。
func healsInWeather(member MemberSnapshot, weather WeatherKind) (uint32, bool) {
	rule := member.WeatherEndTurnHealing
	if rule == nil {
		return 0, false
	}
	for _, candidate := range rule.Weathers {
		if candidate == weather {
			return rule.HealDenominator, true
		}
	}
	return 0, false
}
