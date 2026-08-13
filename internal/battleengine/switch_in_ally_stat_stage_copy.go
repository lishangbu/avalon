package battleengine

// resolveSwitchInAllyStatStageCopy 结算成员实际换入后的同侧能力阶级复制特性。
func resolveSwitchInAllyStatStageCopy(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || !member.SwitchInAllyStatStageCopy {
		return state, nil
	}
	return applySwitchInAllyStatStageCopy(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeSwitchInAllyStatStageCopies 按冻结阵营和槽位顺序处理双方初始上场成员的同侧能力阶级复制特性。
//
// 初始阶段只更新权威快照；后续实际换入才会发布每项实际变化对应的 StatStageChangedEvent。
func initializeSwitchInAllyStatStageCopies(state State) State {
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || !member.SwitchInAllyStatStageCopy {
				continue
			}
			state, _ = applySwitchInAllyStatStageCopy(state, MemberRef{Side: side.Side, Position: member.Position})
		}
	}
	return state
}

// applySwitchInAllyStatStageCopy 将同侧稳定槽位顺序中的第一名其它存活上场成员的全部能力阶级复制给来源。
//
// 复制会覆盖来源所有七项可变能力的当前值，而不是叠加差值；每项实际变化仍各自记录结构化事件，保证重放能
// 恢复完整状态，也避免把一组能力变化压缩为无来源的泛型效果文本。
func applySwitchInAllyStatStageCopy(state State, source MemberRef) (State, []Event) {
	var ally MemberSnapshot
	foundAlly := false
	for _, side := range state.sides {
		if side.Side != source.Side {
			continue
		}
		for _, position := range side.ActiveMembers {
			if position == source.Position {
				continue
			}
			candidate, found := state.member(source.Side, position)
			if found && candidate.CurrentHP > 0 {
				ally, foundAlly = candidate, true
				break
			}
		}
	}
	if !foundAlly {
		return state, nil
	}
	member, found := state.member(source.Side, source.Position)
	if !found || member.CurrentHP == 0 {
		return state, nil
	}
	stats := []Stat{StatAttack, StatDefense, StatSpecialAttack, StatSpecialDefense, StatSpeed, StatAccuracy, StatEvasion}
	events := make([]Event, 0, len(stats))
	stages := make(map[Stat]int8, len(stats))
	for _, stat := range stats {
		before := member.StatStages[stat]
		after := ally.StatStages[stat]
		stages[stat] = after
		if before != after {
			events = append(events, StatStageChangedEvent{
				Type: EventKindStatStageChanged, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: source, Target: source, Stat: stat, Delta: after - before, CurrentStage: after,
			})
		}
	}
	member.StatStages = stages
	state.replaceMember(source.Side, member)
	return state, events
}
