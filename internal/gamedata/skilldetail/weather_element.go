package skilldetail

import "github.com/lishangbu/avalon/internal/platform/snowflake"

// WeatherElementOverride 描述技能在指定普通天气下替换基础属性的一条独立资料规则。
//
// ElementID 必须引用实时启用的属性资料；匹配时它会成为本次伤害、属性相性、同属性加成和场地修正共同使用的
// 有效属性。它不与天气命中或天气威力规则共享无约束的效果列表。
type WeatherElementOverride struct {
	// Weather 是此规则适用的普通天气种类，不能为 none。
	Weather WeatherKind `json:"weather"`
	// ElementID 是匹配天气时替换技能基础属性的稳定 Identifier。
	ElementID snowflake.ID `json:"elementId"`
}

// validWeatherElementOverrides 校验天气属性覆盖的封闭天气枚举、唯一键和引用 Identifier。
func validWeatherElementOverrides(values []WeatherElementOverride) bool {
	if len(values) > 4 {
		return false
	}
	seen := make(map[WeatherKind]struct{}, len(values))
	for _, value := range values {
		if !value.Weather.Valid() || value.ElementID == snowflake.ID(0) {
			return false
		}
		if _, duplicate := seen[value.Weather]; duplicate {
			return false
		}
		seen[value.Weather] = struct{}{}
	}
	return true
}

// cloneWeatherElementOverrides 复制天气属性覆盖切片，防止调用方修改已验证并将要审计或冻结的资料事实。
func cloneWeatherElementOverrides(values []WeatherElementOverride) []WeatherElementOverride {
	if values == nil {
		return []WeatherElementOverride{}
	}
	return append([]WeatherElementOverride(nil), values...)
}
