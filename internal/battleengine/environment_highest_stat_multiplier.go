package battleengine

import "errors"

// EnvironmentHighestStatMultiplier 是成员特性在指定环境下强化最高原始能力的冻结规则。
//
// 它与入场天气、场地和速度倍率分离：本规则不建立环境，也不保存可配置倍率或能力项。引擎始终以攻击、
// 防御、特攻、特防、速度的固定次序选择最高原始能力，并使用速度 3/2、其它能力 13/10 的整数分数。
type EnvironmentHighestStatMultiplier struct {
	// RequiredWeather 是激活规则所需的有效天气；空值表示规则不依赖天气。
	RequiredWeather WeatherKind `json:"requiredWeather,omitempty"`
	// RequiredTerrain 是激活规则所需的普通场地；空值表示规则不依赖场地。
	RequiredTerrain TerrainKind `json:"requiredTerrain,omitempty"`
}

// cloneEnvironmentHighestStatMultiplier 深复制可选的环境最高能力强化规则。
func cloneEnvironmentHighestStatMultiplier(value *EnvironmentHighestStatMultiplier) *EnvironmentHighestStatMultiplier {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// validateEnvironmentHighestStatMultiplier 校验规则仅依赖一种受支持的环境条件。
func validateEnvironmentHighestStatMultiplier(value *EnvironmentHighestStatMultiplier) error {
	if value == nil {
		return nil
	}
	if (value.RequiredWeather == "") == (value.RequiredTerrain == "") {
		return errors.New("环境最高能力强化必须且只能声明天气或场地")
	}
	if value.RequiredWeather != "" && !value.RequiredWeather.valid() {
		return errors.New("环境最高能力强化天气无效")
	}
	if value.RequiredTerrain != "" && !value.RequiredTerrain.valid() {
		return errors.New("环境最高能力强化场地无效")
	}
	return nil
}

// highestRawBattleStat 返回成员五项原始能力中数值最高的一项。
//
// 相等时严格按攻击、防御、特攻、特防、速度的顺序决胜；不能依赖 map 遍历、资料排序或运行平台差异，确保
// 道具激活、环境强化和离线重放得到同一能力项。
func highestRawBattleStat(stats StatBlock) Stat {
	highest := StatAttack
	value := stats.Attack
	for _, candidate := range []struct {
		stat  Stat
		value uint32
	}{
		{stat: StatDefense, value: stats.Defense},
		{stat: StatSpecialAttack, value: stats.SpecialAttack},
		{stat: StatSpecialDefense, value: stats.SpecialDefense},
		{stat: StatSpeed, value: stats.Speed},
	} {
		if candidate.value > value {
			highest, value = candidate.stat, candidate.value
		}
	}
	return highest
}

// validHighestRawBattleStat 报告能力项能否作为最高原始能力强化的已选结果。
//
// 命中、闪避不属于五项原始战斗能力，不能被环境特性或消耗道具选择；单独保留该函数可让初始状态校验明确
// 拒绝损坏重放或手工构造快照中的其它 Stat 值。
func validHighestRawBattleStat(stat Stat) bool {
	return stat == StatAttack || stat == StatDefense || stat == StatSpecialAttack || stat == StatSpecialDefense || stat == StatSpeed
}

// environmentHighestStatMultiplierActive 报告成员规则是否由当前有效环境激活。
//
// 天气通过 effectiveWeather 传入，因而会自然遵守天气封锁和强天气映射；场地直接读取当前全场状态，不会因
// 成员是否接地而失效，因为该特性本身的触发条件是全局环境而不是目标落点。
func environmentHighestStatMultiplierActive(member MemberSnapshot, weather *WeatherEffect, terrain *TerrainEffect) bool {
	rule := member.EnvironmentHighestStatMultiplier
	if rule == nil {
		return false
	}
	if rule.RequiredWeather != "" {
		return weather != nil && weather.Kind == rule.RequiredWeather
	}
	return terrain != nil && terrain.Kind == rule.RequiredTerrain
}

// highestStatMultiplier 返回成员在指定能力上由环境最高能力强化得到的整数分数倍率。
//
// 匹配环境时环境特性优先于已经消耗的道具强化；只有环境未激活时，BoosterEnergyStat 才持续提供同一固定倍率。
// 当前没有任一强化或请求能力不匹配时保持 1/1；该函数刻意不读取实时资料、特性名称或道具文本。
func highestStatMultiplier(member MemberSnapshot, weather *WeatherEffect, terrain *TerrainEffect, stat Stat) (uint32, uint32) {
	if environmentHighestStatMultiplierActive(member, weather, terrain) {
		if highestRawBattleStat(member.Stats) != stat {
			return 1, 1
		}
	} else if member.BoosterEnergyStat != stat {
		return 1, 1
	}
	if stat == StatSpeed {
		return 3, 2
	}
	return 13, 10
}

// applyHighestStatMultiplier 对已经完成能力阶级及其它前置修正的能力值应用整数分数强化。
//
// 使用 uint64 中间值和饱和上界，避免异常初始快照把速度或伤害公式中的 uint32 能力绕回成较小数值；结果至少
// 为一，保持所有调用方既有的正能力不变量。
func applyHighestStatMultiplier(value uint32, numerator, denominator uint32) uint32 {
	if value == 0 || numerator == 0 || denominator == 0 {
		return 1
	}
	result := uint64(value) * uint64(numerator) / uint64(denominator)
	if result == 0 {
		return 1
	}
	if result > uint64(^uint32(0)) {
		return ^uint32(0)
	}
	return uint32(result)
}
