package battleengine

import "fmt"

// validateChargeSkippedWeathers 校验技能声明的天气跳过蓄力集合。
//
// 该集合只描述已经具备蓄力控制规则的技能在何种普通天气下可以直接完成；它不建立天气、不会消耗道具，也不替代
// 已经开始蓄力的下一回合强制动作。每种天气最多一项，以免资料数组顺序影响权威结算。
func validateChargeSkippedWeathers(weathers []WeatherKind, hasChargingApplication bool) error {
	if len(weathers) > 4 {
		return fmt.Errorf("跳过蓄力天气超过上限: %d", len(weathers))
	}
	if len(weathers) != 0 && !hasChargingApplication {
		return fmt.Errorf("跳过蓄力天气要求技能声明蓄力控制规则")
	}
	seen := make(map[WeatherKind]struct{}, len(weathers))
	for _, weather := range weathers {
		if !weather.valid() {
			return fmt.Errorf("跳过蓄力天气无效: %q", weather)
		}
		if _, duplicated := seen[weather]; duplicated {
			return fmt.Errorf("跳过蓄力天气重复: %q", weather)
		}
		seen[weather] = struct{}{}
	}
	return nil
}

// weatherSkipsCharge 报告技能能否在当前已生效的普通天气下省略首次蓄力等待。
//
// 没有天气、天气不匹配或技能没有明确资料时一律返回 false；这避免把“晴天跳过蓄力”误扩展为所有蓄力技能的
// 全局规则。
func weatherSkipsCharge(skill SkillSnapshot, weather *WeatherEffect) bool {
	if weather == nil {
		return false
	}
	for _, candidate := range skill.ChargeSkippedWeathers {
		if candidate == weather.Kind {
			return true
		}
	}
	return false
}
