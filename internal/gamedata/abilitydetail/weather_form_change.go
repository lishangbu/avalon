package abilitydetail

import "github.com/lishangbu/avalon/internal/platform/snowflake"

// WeatherFormTarget 是一种普通天气与目标形态的独立资料绑定。
type WeatherFormTarget struct {
	// Weather 是会选择本目标形态的封闭普通天气。
	Weather WeatherKind
	// CreatureID 是匹配天气时应使用的目标形态稳定 Identifier。
	CreatureID snowflake.ID
}

// WeatherFormChange 是特性持有成员按当前有效普通天气同步形态的资料规则。
//
// DefaultCreatureID 用于无天气、未映射天气以及天气效果被封锁时的默认形态。Targets 只允许四种普通天气，
// 每种天气最多一项，避免数组顺序成为隐藏规则优先级。
type WeatherFormChange struct {
	// DefaultCreatureID 是未命中天气映射时应使用的默认形态稳定 Identifier。
	DefaultCreatureID snowflake.ID
	// Targets 是天气到目标形态的无重复映射。
	Targets []WeatherFormTarget
}

// validWeatherFormChange 校验可选的天气形态资料。
func validWeatherFormChange(value *WeatherFormChange) bool {
	if value == nil {
		return true
	}
	if value.DefaultCreatureID == snowflake.ID(0) || len(value.Targets) < 1 || len(value.Targets) > 4 {
		return false
	}
	seen := make(map[WeatherKind]struct{}, len(value.Targets))
	for _, target := range value.Targets {
		if !validWeatherKind(target.Weather) || target.CreatureID == snowflake.ID(0) {
			return false
		}
		if _, duplicated := seen[target.Weather]; duplicated {
			return false
		}
		seen[target.Weather] = struct{}{}
	}
	return true
}

// cloneWeatherFormChange 深复制可选的天气形态资料。
func cloneWeatherFormChange(value *WeatherFormChange) *WeatherFormChange {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.Targets = append([]WeatherFormTarget(nil), value.Targets...)
	return &cloned
}
