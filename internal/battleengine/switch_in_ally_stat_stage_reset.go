package battleengine

// resolveSwitchInAllyStatStageReset 结算成员实际换入后的同侧能力阶级重置特性。
func resolveSwitchInAllyStatStageReset(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || !member.SwitchInAllyStatStageReset {
		return state, nil
	}
	return applySwitchInAllyStatStageReset(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeSwitchInAllyStatStageResets 按冻结阵营和槽位顺序处理双方初始上场成员的同侧能力阶级重置特性。
//
// 初始阶段只更新权威快照；后续实际换入才会发布每项实际变化对应的 StatStageChangedEvent。
func initializeSwitchInAllyStatStageResets(state State) State {
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || !member.SwitchInAllyStatStageReset {
				continue
			}
			state, _ = applySwitchInAllyStatStageReset(state, MemberRef{Side: side.Side, Position: member.Position})
		}
	}
	return state
}

// applySwitchInAllyStatStageReset 将来源同侧其它当前上场成员的所有非零能力阶级重置为零。
//
// 重置不会影响来源自身或后备成员；每项实际变化均保留独立事件，使重放既能恢复最终状态，也能保留来源、接收者和
// 阶级变化量，而不会将多个规则压缩为无结构的通用效果文本。
func applySwitchInAllyStatStageReset(state State, source MemberRef) (State, []Event) {
	stats := []Stat{StatAttack, StatDefense, StatSpecialAttack, StatSpecialDefense, StatSpeed, StatAccuracy, StatEvasion}
	events := make([]Event, 0, len(stats))
	for _, side := range state.sides {
		if side.Side != source.Side {
			continue
		}
		for _, position := range side.ActiveMembers {
			if position == source.Position {
				continue
			}
			ally, found := state.member(source.Side, position)
			if !found {
				continue
			}
			stages := make(map[Stat]int8, len(stats))
			changed := false
			for _, stat := range stats {
				before := ally.StatStages[stat]
				stages[stat] = 0
				if before == 0 {
					continue
				}
				changed = true
				events = append(events, StatStageChangedEvent{
					Type: EventKindStatStageChanged, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Actor: source, Target: MemberRef{Side: source.Side, Position: ally.Position}, Stat: stat,
					Delta: -before, CurrentStage: 0,
				})
			}
			if changed {
				ally.StatStages = stages
				state.replaceMember(source.Side, ally)
			}
		}
	}
	return state, events
}
