package battleengine

import "fmt"

// SwitchInTerrain 是成员特性冻结到战斗快照后的普通场地入场规则。
//
// 它只在成员实际进入场地后尝试建立场地，不携带概率，也不代表技能场地。场地的接地判定、伤害、异常阻止、
// 回合末回复和自然结束仍由各自明确结算阶段负责，不能收拢为泛型入场效果。
type SwitchInTerrain struct {
	// Effect 是入场后尝试写入环境的普通场地和完整持续回合。
	Effect TerrainEffect `json:"effect"`
}

// validateSwitchInTerrain 校验成员冻结的入场普通场地规则。
func validateSwitchInTerrain(value *SwitchInTerrain) error {
	if value == nil {
		return nil
	}
	if err := validateTerrainEffect(value.Effect); err != nil {
		return fmt.Errorf("入场普通场地无效: %w", err)
	}
	return nil
}

// cloneSwitchInTerrain 深复制可选的入场普通场地规则。
func cloneSwitchInTerrain(value *SwitchInTerrain) *SwitchInTerrain {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// startSwitchInTerrain 尝试将成员入场特性声明的普通场地写入环境。
//
// 同种场地且持续回合相同时不产生状态或事件变化；不同场地会覆盖当前场地。天气、强天气和侧状态不参与
// 本规则的覆盖判定，保证场地生命周期保持独立。
func startSwitchInTerrain(state State, source MemberRef, rule SwitchInTerrain) (State, []Event) {
	effect := extendTerrainDurationByHeldItem(state, source, rule.Effect)
	if current := state.environment.Terrain; current != nil && *current == effect {
		return state, nil
	}
	state.environment.Terrain = &effect
	return state, []Event{AbilityTerrainStartedEvent{
		Type: EventKindAbilityTerrainStarted, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Source: source, Terrain: effect.Kind, TurnsRemaining: effect.TurnsRemaining,
	}}
}

// resolveSwitchInTerrain 结算成员实际换入且入场危害结束后的普通场地特性。
func resolveSwitchInTerrain(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || member.SwitchInTerrain == nil {
		return state, nil
	}
	return startSwitchInTerrain(state, MemberRef{Side: slot.Side, Position: member.Position}, *member.SwitchInTerrain)
}

// initializeSwitchInTerrain 按冻结阵营和槽位顺序处理双方初始上场成员的普通场地特性。
//
// 第 0 回合初始入场只写入权威环境快照；后续换入会输出 AbilityTerrainStartedEvent。遍历顺序是快照的
// 确定性规则，避免客户端数组顺序影响同一资料生成的环境状态。
func initializeSwitchInTerrain(state State) State {
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || member.SwitchInTerrain == nil {
				continue
			}
			state, _ = startSwitchInTerrain(state, MemberRef{Side: side.Side, Position: member.Position}, *member.SwitchInTerrain)
		}
	}
	return state
}
