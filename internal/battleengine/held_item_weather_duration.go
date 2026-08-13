package battleengine

// extendWeatherDurationByHeldItem 返回成员以技能或入场特性建立普通天气时应写入的完整持续回合。
// 当前规则仅处理降雨延长道具；道具不会缩短来源已有持续回合，也不修改不匹配天气、强天气或既有环境。
func extendWeatherDurationByHeldItem(state State, source MemberRef, effect WeatherEffect) WeatherEffect {
	if effect.Kind == WeatherKindRain {
		return extendRainDurationByHeldItem(state, source, effect)
	}
	if effect.Kind == WeatherKindSandstorm {
		return extendSandstormDurationByHeldItem(state, source, effect)
	}
	if effect.Kind == WeatherKindSnow {
		return extendSnowDurationByHeldItem(state, source, effect)
	}
	if effect.Kind == WeatherKindSun {
		return extendSunDurationByHeldItem(state, source, effect)
	}
	return effect
}

// extendRainDurationByHeldItem 仅在持有者当前拥有降雨延长道具时延长普通降雨。
// 道具值只能抬高来源声明的初始持续回合，不能改变其它天气或已经建立的环境。
func extendRainDurationByHeldItem(state State, source MemberRef, effect WeatherEffect) WeatherEffect {
	member, found := state.member(source.Side, source.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || member.HeldItemRainTurnsRemaining <= effect.TurnsRemaining {
		return effect
	}
	effect.TurnsRemaining = member.HeldItemRainTurnsRemaining
	return effect
}

// extendSandstormDurationByHeldItem 仅在持有者当前拥有沙暴延长道具时延长普通沙暴。
// 它使用独立运行时字段，不能因降雨道具或其它天气资料而取得额外持续时间。
func extendSandstormDurationByHeldItem(state State, source MemberRef, effect WeatherEffect) WeatherEffect {
	member, found := state.member(source.Side, source.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || member.HeldItemSandstormTurnsRemaining <= effect.TurnsRemaining {
		return effect
	}
	effect.TurnsRemaining = member.HeldItemSandstormTurnsRemaining
	return effect
}

// extendSnowDurationByHeldItem 仅在持有者当前拥有降雪延长道具时延长普通降雪。
// 它使用独立冻结字段，不能由其它天气道具、无道具成员或已消费道具触发。
func extendSnowDurationByHeldItem(state State, source MemberRef, effect WeatherEffect) WeatherEffect {
	member, found := state.member(source.Side, source.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || member.HeldItemSnowTurnsRemaining <= effect.TurnsRemaining {
		return effect
	}
	effect.TurnsRemaining = member.HeldItemSnowTurnsRemaining
	return effect
}

// extendSunDurationByHeldItem 仅在持有者当前拥有日照延长道具时延长普通日照。
// 道具消失、来源倒下、天气不匹配或来源自身持续回合更长时，均保持原始效果不变。
func extendSunDurationByHeldItem(state State, source MemberRef, effect WeatherEffect) WeatherEffect {
	member, found := state.member(source.Side, source.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || member.HeldItemSunTurnsRemaining <= effect.TurnsRemaining {
		return effect
	}
	effect.TurnsRemaining = member.HeldItemSunTurnsRemaining
	return effect
}
