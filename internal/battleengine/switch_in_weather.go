package battleengine

import "fmt"

// SwitchInWeather 是成员特性冻结到战斗快照后的普通天气入场规则。
//
// 它只在成员实际进入场地后尝试建立普通天气，不携带概率，也不代表技能天气。强天气存在时本规则不生效；
// 普通天气的持续、自然结束和可执行效果仍分别由 EnvironmentSnapshot 与天气结算阶段负责。
type SwitchInWeather struct {
	// Effect 是入场后尝试写入环境的普通天气和完整持续回合。
	Effect WeatherEffect `json:"effect"`
}

// validateSwitchInWeather 校验成员冻结的入场普通天气规则。
func validateSwitchInWeather(value *SwitchInWeather) error {
	if value == nil {
		return nil
	}
	if err := validateWeatherEffect(value.Effect); err != nil {
		return fmt.Errorf("入场普通天气无效: %w", err)
	}
	return nil
}

// cloneSwitchInWeather 深复制可选的入场普通天气规则。
func cloneSwitchInWeather(value *SwitchInWeather) *SwitchInWeather {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// startSwitchInWeather 尝试将成员入场特性声明的普通天气写入环境。
//
// 同种天气且剩余回合相同时不产生状态或事件变化；不同普通天气会覆盖当前普通天气。任何强天气存在时
// 保持环境不变，不能由普通特性绕过强天气的独立覆盖层级。
func startSwitchInWeather(state State, source MemberRef, rule SwitchInWeather) (State, []Event) {
	if state.environment.StrongWeather != nil {
		return state, nil
	}
	effect := extendWeatherDurationByHeldItem(state, source, rule.Effect)
	if current := state.environment.Weather; current != nil && *current == effect {
		return state, nil
	}
	state.environment.Weather = &effect
	return state, []Event{AbilityWeatherStartedEvent{
		Type: EventKindAbilityWeatherStarted, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Source: source, Weather: effect.Kind, TurnsRemaining: effect.TurnsRemaining,
	}}
}

// resolveSwitchInWeather 结算成员实际换入且入场危害结束后的普通天气特性。
func resolveSwitchInWeather(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || member.SwitchInWeather == nil {
		return state, nil
	}
	return startSwitchInWeather(state, MemberRef{Side: slot.Side, Position: member.Position}, *member.SwitchInWeather)
}

// initializeSwitchInWeather 按冻结阵营和槽位顺序处理双方初始上场成员的普通天气特性。
//
// 第 0 回合的初始入场只写入权威环境快照；后续换入会输出 AbilityWeatherStartedEvent。遍历顺序是快照
// 的确定性规则，而不是客户端请求数组顺序，确保同一初始资料能得到一致的环境状态。
func initializeSwitchInWeather(state State) State {
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || member.SwitchInWeather == nil {
				continue
			}
			state, _ = startSwitchInWeather(state, MemberRef{Side: side.Side, Position: member.Position}, *member.SwitchInWeather)
		}
	}
	return state
}
