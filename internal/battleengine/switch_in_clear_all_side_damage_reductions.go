package battleengine

// resolveSwitchInClearAllSideDamageReductions 结算成员实际换入后清除所有阵营减伤屏障的特性。
func resolveSwitchInClearAllSideDamageReductions(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || !member.SwitchInClearAllSideDamageReductions {
		return state, nil
	}
	return applySwitchInClearAllSideDamageReductions(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeSwitchInClearAllSideDamageReductions 按冻结阵营和槽位顺序处理双方初始上场成员的全阵营减伤屏障清除特性。
//
// 初始阶段只更新权威快照；后续实际换入才会为每个实际发生清除的阵营发布独立事件。
func initializeSwitchInClearAllSideDamageReductions(state State) State {
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || !member.SwitchInClearAllSideDamageReductions {
				continue
			}
			state, _ = applySwitchInClearAllSideDamageReductions(state, MemberRef{Side: side.Side, Position: member.Position})
		}
	}
	return state
}

// applySwitchInClearAllSideDamageReductions 清除双方阵营当前存在的反射壁、光墙和极光幕。
//
// 顺风和所有入场危害不属于减伤屏障，必须保留；每个被实际清除屏障的阵营都会产生独立事件，保证重放可观察
// 到清除的准确范围，而不是仅记录一条没有目标阵营的信息文本。
func applySwitchInClearAllSideDamageReductions(state State, source MemberRef) (State, []Event) {
	events := make([]Event, 0, len(state.sides))
	for index := range state.sides {
		conditions := &state.sides[index].Conditions
		clearedReflect := conditions.Reflect != nil
		clearedLightScreen := conditions.LightScreen != nil
		clearedAuroraVeil := conditions.AuroraVeil != nil
		if !clearedReflect && !clearedLightScreen && !clearedAuroraVeil {
			continue
		}
		conditions.Reflect = nil
		conditions.LightScreen = nil
		conditions.AuroraVeil = nil
		events = append(events, AbilitySideDamageReductionsClearedEvent{
			Type: EventKindAbilitySideDamageReductionsCleared, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Actor: source, Side: state.sides[index].Side,
			ClearedReflect: clearedReflect, ClearedLightScreen: clearedLightScreen, ClearedAuroraVeil: clearedAuroraVeil,
		})
	}
	return state, events
}
