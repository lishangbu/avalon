package battleengine

import "fmt"

// validateWeatherDamageImmunities 校验成员特性冻结的天气伤害免疫集合。
//
// 该集合仅影响天气在回合末主动造成的伤害，不能隐式扩大到天气威力、命中率、属性替换或持续时间。每种普通天气
// 最多配置一次，避免资料数组顺序决定同一成员是否应受环境伤害。
func validateWeatherDamageImmunities(weathers []WeatherKind) error {
	if len(weathers) > 4 {
		return fmt.Errorf("天气伤害免疫超过上限: %d", len(weathers))
	}
	seen := make(map[WeatherKind]struct{}, len(weathers))
	for _, weather := range weathers {
		if !weather.valid() {
			return fmt.Errorf("天气伤害免疫天气无效: %q", weather)
		}
		if _, duplicated := seen[weather]; duplicated {
			return fmt.Errorf("天气伤害免疫天气重复: %q", weather)
		}
		seen[weather] = struct{}{}
	}
	return nil
}

// immuneToWeatherDamage 报告成员是否具有针对当前天气伤害阶段的显式特性免疫。
//
// 仅调用者已经确认天气会产生该阶段伤害时才会查询本函数。未配置或未匹配一律返回 false，不能依据特性 ID、名称
// 或属性文本推断免疫。
func immuneToWeatherDamage(member MemberSnapshot, weather WeatherKind) bool {
	for _, candidate := range member.WeatherDamageImmunities {
		if candidate == weather {
			return true
		}
	}
	return false
}

// effectiveWeather 返回当前真正参与普通天气结算的环境效果。
//
// 特性天气封锁不会清除 state.environment.Weather：天气仍须在原始环境状态中保持其建立事件、剩余回合与自然结束
// 事件，以保证回放可重建。只要任一存活的场上成员冻结了封锁规则，就把普通天气对伤害、命中、威力、属性替换和
// 蓄力跳过的读取统一降为 nil；成员换下、倒下或失去该冻结快照后，原有天气会在尚未到期时自然恢复效果。
func effectiveWeather(state State) *WeatherEffect {
	if weatherEffectsSuppressed(state) {
		return nil
	}
	if strongWeather := state.environment.StrongWeather; strongWeather != nil {
		if kind, ok := strongWeather.Kind.effectiveWeatherKind(); ok {
			// 强天气没有会递减的持续回合。这里的非零占位仅满足 WeatherEffect 的内部值不变量；所有
			// 读取方只关心 Kind，强天气的结束完全由来源同步阶段决定。
			return &WeatherEffect{Kind: kind, TurnsRemaining: 1}
		}
		return nil
	}
	return state.environment.Weather
}

// weatherEffectsSuppressed 报告任一存活场上成员是否暂停所有天气可执行规则。
//
// 该判断不删除普通或强天气的环境状态，也不影响强天气来源同步；它只供伤害、属性、命中、速度和技能
// 阻止等可执行阶段统一读取，保证封锁解除后仍可在剩余生命周期内恢复效果。
func weatherEffectsSuppressed(state State) bool {
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if found && member.CurrentHP > 0 && member.WeatherEffectsSuppressed {
				return true
			}
		}
	}
	return false
}

// effectiveStrongWeather 返回当前没有被天气封锁特性暂停的强天气。
func effectiveStrongWeather(state State) *StrongWeatherState {
	if weatherEffectsSuppressed(state) || state.environment.StrongWeather == nil {
		return nil
	}
	value := *state.environment.StrongWeather
	return &value
}
