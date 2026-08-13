package skilldetail

// WeatherAccuracyOverride 描述技能在指定普通天气下替换基础命中率的一条独立资料规则。
//
// AccuracyPercent 为 0 明确表示必中；1 至 100 表示替换技能基础命中率。它不与天气建立、天气伤害或
// 天气持续时间共享通用效果结构，避免互不相同的结算语义在资料层被压缩。
type WeatherAccuracyOverride struct {
	// Weather 是此规则适用的普通天气种类，不能为 none。
	Weather WeatherKind `json:"weather"`
	// AccuracyPercent 是指定天气下的基础命中率；0 表示必中，1 至 100 表示固定命中百分比。
	AccuracyPercent int32 `json:"accuracyPercent"`
}

// validWeatherAccuracyOverrides 校验天气命中覆盖的封闭天气枚举、唯一键与命中率边界。
func validWeatherAccuracyOverrides(values []WeatherAccuracyOverride) bool {
	if len(values) > 4 {
		return false
	}
	seen := make(map[WeatherKind]struct{}, len(values))
	for _, value := range values {
		if !value.Weather.Valid() || value.AccuracyPercent < 0 || value.AccuracyPercent > 100 {
			return false
		}
		if _, duplicate := seen[value.Weather]; duplicate {
			return false
		}
		seen[value.Weather] = struct{}{}
	}
	return true
}

// cloneWeatherAccuracyOverrides 复制天气命中覆盖切片，防止调用方修改已验证并将要审计或冻结的资料事实。
func cloneWeatherAccuracyOverrides(values []WeatherAccuracyOverride) []WeatherAccuracyOverride {
	if values == nil {
		return []WeatherAccuracyOverride{}
	}
	return append([]WeatherAccuracyOverride(nil), values...)
}
