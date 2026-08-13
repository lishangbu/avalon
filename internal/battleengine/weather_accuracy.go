package battleengine

import "fmt"

// WeatherAccuracyOverride 描述一项技能在指定普通天气下替换基础命中率的冻结规则。
//
// AccuracyPercent 为 0 表示该天气下必中；1 至 100 表示在命中/闪避阶级修正前替换技能原始命中率。
// 它只描述技能自己的天气例外，不承载天气建立、天气伤害、天气持续时间或未来特性造成的有效天气变化。
type WeatherAccuracyOverride struct {
	// Weather 是此命中覆盖适用的普通天气种类，不能是 none。
	Weather WeatherKind `json:"weather"`
	// AccuracyPercent 是该天气下覆盖后的基础命中率；0 表示必中，1 至 100 表示固定百分比。
	AccuracyPercent uint8 `json:"accuracyPercent"`
}

// validateWeatherAccuracyOverrides 校验单个技能冻结的天气命中覆盖集合。
//
// 每种天气至多出现一次，避免两个资料事实竞争同一命中率；WeatherKindNone 没有运行时天气语义，不能作为
// 覆盖键。零命中率保留为明确的“必中”而不是未配置或随机失败。
func validateWeatherAccuracyOverrides(overrides []WeatherAccuracyOverride) error {
	if len(overrides) > 4 {
		return fmt.Errorf("天气命中覆盖超过上限: %d", len(overrides))
	}
	seen := make(map[WeatherKind]struct{}, len(overrides))
	for _, override := range overrides {
		if !override.Weather.valid() {
			return fmt.Errorf("天气命中覆盖天气无效: %q", override.Weather)
		}
		if _, duplicated := seen[override.Weather]; duplicated {
			return fmt.Errorf("天气命中覆盖天气重复: %q", override.Weather)
		}
		seen[override.Weather] = struct{}{}
	}
	return nil
}

// weatherAccuracy 返回当前普通天气对技能基础命中率的可选覆盖。
//
// false 表示不存在覆盖，调用者必须继续使用 SkillSnapshot.Accuracy；true 且数值为 0 表示明确必中。
func weatherAccuracy(overrides []WeatherAccuracyOverride, weather *WeatherEffect) (uint8, bool) {
	if weather == nil {
		return 0, false
	}
	for _, override := range overrides {
		if override.Weather == weather.Kind {
			return override.AccuracyPercent, true
		}
	}
	return 0, false
}
