package battleengine

// resolveSwitchInOpponentDefenseComparisonBoost 结算成员实际换入后的对手防御比较强化特性。
func resolveSwitchInOpponentDefenseComparisonBoost(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || !member.SwitchInOpponentDefenseComparisonBoost {
		return state, nil
	}
	return applySwitchInOpponentDefenseComparisonBoost(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeSwitchInOpponentDefenseComparisonBoosts 按冻结阵营和槽位顺序处理双方初始上场成员的比较强化特性。
//
// 初始阶段只更新权威快照，不向第 0 回合事件流补写能力阶级变化事件。
func initializeSwitchInOpponentDefenseComparisonBoosts(state State) State {
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || !member.SwitchInOpponentDefenseComparisonBoost {
				continue
			}
			state, _ = applySwitchInOpponentDefenseComparisonBoost(state, MemberRef{Side: side.Side, Position: member.Position})
		}
	}
	return state
}

// applySwitchInOpponentDefenseComparisonBoost 汇总所有当前上场对手的基础防御并提升来源的一项攻击能力阶级。
//
// 物防总和严格低于特防总和时提升物理攻击；相等或更高时提升特殊攻击。比较读取冻结基础能力而非当前能力
// 阶级，避免先前能力变化或遍历顺序改变入场特性的判定结果。
func applySwitchInOpponentDefenseComparisonBoost(state State, source MemberRef) (State, []Event) {
	var totalDefense uint64
	var totalSpecialDefense uint64
	foundOpponent := false
	for _, side := range state.sides {
		if side.Side == source.Side {
			continue
		}
		for _, position := range side.ActiveMembers {
			opponent, found := state.member(side.Side, position)
			if !found || opponent.CurrentHP == 0 {
				continue
			}
			foundOpponent = true
			totalDefense += uint64(opponent.Stats.Defense)
			totalSpecialDefense += uint64(opponent.Stats.SpecialDefense)
		}
	}
	if !foundOpponent {
		return state, nil
	}
	stat := StatSpecialAttack
	if totalDefense < totalSpecialDefense {
		stat = StatAttack
	}
	member, found := state.member(source.Side, source.Position)
	if !found || member.CurrentHP == 0 {
		return state, nil
	}
	before := member.StatStages[stat]
	after := min(int8(6), before+1)
	if before == after {
		return state, nil
	}
	stages := make(map[Stat]int8, len(member.StatStages)+1)
	for currentStat, stage := range member.StatStages {
		stages[currentStat] = stage
	}
	stages[stat] = after
	member.StatStages = stages
	state.replaceMember(source.Side, member)
	return state, []Event{StatStageChangedEvent{
		Type: EventKindStatStageChanged, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: source, Target: source, Stat: stat, Delta: after - before, CurrentStage: after,
	}}
}
