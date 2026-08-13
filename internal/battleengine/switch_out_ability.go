package battleengine

// resolveSwitchOutAbilities 在仍存活成员完成一次实际换出前结算其成功离场特性。
//
// 倒下成员进入补位时不会调用本函数，因此不会错误触发净化、回复或形态变化。规则顺序固定为形态变化、主要
// 异常净化、固定比例回复，且都发生在 leaveBattlefield 清理连续在场状态之前；这样回复与状态能随成员保留到
// 后备席，形态变化也不会被换出清理提前抹除。
func resolveSwitchOutAbilities(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 {
		return state, nil
	}
	memberRef := MemberRef{Side: slot.Side, Position: member.Position}
	events := make([]Event, 0, 3)

	if rule := member.SwitchOutFormChange; rule != nil && member.CreatureID == rule.BaseCreatureID {
		if profile, exists := member.formProfile(rule.AlternateCreatureID); exists {
			fromCreatureID := member.CreatureID
			member = applyFormProfile(member, profile)
			state.replaceMember(slot.Side, member)
			events = append(events, FormChangedEvent{
				Type: EventKindFormChanged, SchemaVersion: 1, TurnNumber: state.turnNumber, Member: memberRef,
				FromCreatureID: fromCreatureID, ToCreatureID: member.CreatureID, Reason: FormChangeReasonSwitchOutAbility,
			})
		}
	}

	// 前一步形态切换可能已改变最大生命，因此后续净化与回复必须读取 State 中刚写入的成员，而不是调用前的副本。
	member, found = state.member(slot.Side, memberRef.Position)
	if !found || member.CurrentHP == 0 {
		return state, events
	}
	if member.SwitchOutMajorStatusCure && member.MajorStatus != "" {
		status := member.MajorStatus
		member.MajorStatus = ""
		member.BadPoisonCounter = 0
		member.SleepTurnsRemaining = 0
		state.replaceMember(slot.Side, member)
		events = append(events, MajorStatusClearedEvent{
			Type: EventKindMajorStatusCleared, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Target: memberRef, Status: status,
		})
	}

	member, found = state.member(slot.Side, memberRef.Position)
	if !found || member.CurrentHP == 0 || member.SwitchOutHealDenominator == 0 || member.CurrentHP >= member.MaxHP {
		return state, events
	}
	amount := max(member.MaxHP/uint32(member.SwitchOutHealDenominator), uint32(1))
	amount = min(amount, member.MaxHP-member.CurrentHP)
	member.CurrentHP += amount
	state.replaceMember(slot.Side, member)
	events = append(events, SwitchOutHealingAppliedEvent{
		Type: EventKindSwitchOutHealingApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Member: memberRef, Amount: amount, CurrentHP: member.CurrentHP,
	})
	return state, events
}
