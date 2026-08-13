package battleengine

import (
	"fmt"
)

// WeatherElementOverride 描述一项技能在指定普通天气下替换基础属性的冻结规则。
//
// 它只决定本次使用的有效属性：属性相性、同属性加成、天气和场地伤害修正以及一击必杀的同属性例外都读取该
// 结果。该规则不建立天气，也不承载威力、命中率或天气持续时间，避免不同生命周期的战斗事实被压缩为泛型效果。
type WeatherElementOverride struct {
	// Weather 是此属性覆盖适用的普通天气种类，不能是 none。
	Weather WeatherKind `json:"weather"`
	// ElementID 是匹配天气时本次技能使用的属性稳定 Identifier。
	ElementID Identifier `json:"elementId"`
}

// validateWeatherElementOverrides 校验单个技能冻结的天气属性覆盖集合。
//
// 每种天气至多拥有一个替换属性；空属性会导致属性相性和同属性加成的规则依据不确定，因此在 State 创建时
// 一并拒绝，而不是在战斗过程中回退或猜测。
func validateWeatherElementOverrides(overrides []WeatherElementOverride) error {
	if len(overrides) > 4 {
		return fmt.Errorf("天气属性覆盖超过上限: %d", len(overrides))
	}
	seen := make(map[WeatherKind]struct{}, len(overrides))
	for _, override := range overrides {
		if !override.Weather.valid() {
			return fmt.Errorf("天气属性覆盖天气无效: %q", override.Weather)
		}
		if !override.ElementID.IsValid() {
			return fmt.Errorf("天气属性覆盖目标属性为空")
		}
		if _, duplicated := seen[override.Weather]; duplicated {
			return fmt.Errorf("天气属性覆盖天气重复: %q", override.Weather)
		}
		seen[override.Weather] = struct{}{}
	}
	return nil
}

// effectiveSkillElement 返回本次技能在当前普通天气下实际使用的属性稳定 Identifier。
//
// 无天气或不存在匹配覆盖时严格回退 SkillSnapshot.ElementID。调用方必须把本次结算读取到的天气快照传入，
// 使多目标、多段伤害和天气建立后的后续行动都只根据当时的权威环境计算，不从名称或自由文本推断属性。
func effectiveSkillElement(skill SkillSnapshot, weather *WeatherEffect) Identifier {
	if weather == nil {
		return skill.ElementID
	}
	for _, override := range skill.WeatherElementOverrides {
		if override.Weather == weather.Kind {
			return override.ElementID
		}
	}
	return skill.ElementID
}
