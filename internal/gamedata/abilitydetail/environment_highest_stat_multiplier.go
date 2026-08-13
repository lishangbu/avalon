package abilitydetail

// EnvironmentHighestStatMultiplier 是特性在指定普通天气或普通场地下强化成员最高原始能力的独立规则。
//
// 它只描述触发环境，不保存能力项或倍率：能力项由 Battle Engine 依照攻击、防御、特攻、特防、速度的固定
// 优先顺序从冻结成员画像计算；速度使用三分之二以外的专有 3/2 倍率，其它能力使用 13/10。这避免资料以可变
// 数字或展示文案重复表达对局规则。
type EnvironmentHighestStatMultiplier struct {
	// RequiredWeather 是激活规则所需的普通天气；与 RequiredTerrain 必须且只能设置一个。
	RequiredWeather *WeatherKind
	// RequiredTerrain 是激活规则所需的普通场地；与 RequiredWeather 必须且只能设置一个。
	RequiredTerrain *TerrainKind
}

// cloneEnvironmentHighestStatMultiplier 深复制可选的环境最高能力强化规则。
func cloneEnvironmentHighestStatMultiplier(value *EnvironmentHighestStatMultiplier) *EnvironmentHighestStatMultiplier {
	if value == nil {
		return nil
	}
	cloned := &EnvironmentHighestStatMultiplier{}
	if value.RequiredWeather != nil {
		weather := *value.RequiredWeather
		cloned.RequiredWeather = &weather
	}
	if value.RequiredTerrain != nil {
		terrain := *value.RequiredTerrain
		cloned.RequiredTerrain = &terrain
	}
	return cloned
}

// validEnvironmentHighestStatMultiplier 校验规则只依赖一个已知的普通环境条件。
func validEnvironmentHighestStatMultiplier(value *EnvironmentHighestStatMultiplier) bool {
	if value == nil {
		return true
	}
	if (value.RequiredWeather == nil) == (value.RequiredTerrain == nil) {
		return false
	}
	return value.RequiredWeather != nil && validWeatherKind(*value.RequiredWeather) ||
		value.RequiredTerrain != nil && validTerrainKind(*value.RequiredTerrain)
}
