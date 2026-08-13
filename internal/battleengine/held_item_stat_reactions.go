package battleengine

// applyHeldItemAbilityStatReductionSpeedBoost 在对手入场特性实际降低能力后提升持有者一级速度并消费道具。
// 已处于速度上限时不消费，保证“发生下降”与“能够得到补偿强化”两个条件都可由最终事件流验证。
func applyHeldItemAbilityStatReductionSpeedBoost(state State, targetRef MemberRef) (State, []Event) {
	target, found := state.member(targetRef.Side, targetRef.Position)
	if !found || target.ItemID == 0 || !target.HeldItemAbilityStatReductionSpeedBoost {
		return state, nil
	}
	before := target.StatStages[StatSpeed]
	if before >= 6 {
		return state, nil
	}
	stages := cloneStatStages(target.StatStages)
	stages[StatSpeed] = before + 1
	itemID := target.ItemID
	target.StatStages = stages
	target = clearHeldItemRuntimeState(target)
	state.replaceMember(targetRef.Side, target)
	return state, []Event{StatStageChangedEvent{
		Type: EventKindStatStageChanged, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: targetRef, Target: targetRef, Stat: StatSpeed, Delta: 1, CurrentStage: before + 1,
	}, HeldItemStatReactionConsumedEvent{
		Type: EventKindHeldItemStatReactionConsumed, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Target: targetRef, ItemID: itemID, Reason: "abilityStatReductionSpeedBoost",
	}}
}

// applyHeldItemOpponentPositiveStatStageCopy 让对手侧第一名符合条件的持有者复制来源全部正能力阶级并消费道具。
// 复制采用逐项覆盖而非叠加；只有至少一项最终值发生变化时才消费，避免空强化错误触发。
func applyHeldItemOpponentPositiveStatStageCopy(state State, sourceRef MemberRef) (State, []Event) {
	source, found := state.member(sourceRef.Side, sourceRef.Position)
	if !found {
		return state, nil
	}
	for _, side := range state.sides {
		if side.Side == sourceRef.Side {
			continue
		}
		for _, position := range side.ActiveMembers {
			targetRef := MemberRef{Side: side.Side, Position: position}
			target, ok := state.member(side.Side, position)
			if !ok || target.CurrentHP == 0 || target.ItemID == 0 || !target.HeldItemOpponentPositiveStatStageCopy {
				continue
			}
			stages := cloneStatStages(target.StatStages)
			events := make([]Event, 0, 7)
			for _, stat := range []Stat{StatAttack, StatDefense, StatSpecialAttack, StatSpecialDefense, StatSpeed, StatAccuracy, StatEvasion} {
				after := source.StatStages[stat]
				before := stages[stat]
				if after <= 0 || before == after {
					continue
				}
				stages[stat] = after
				events = append(events, StatStageChangedEvent{
					Type: EventKindStatStageChanged, SchemaVersion: 1, TurnNumber: state.turnNumber,
					Actor: targetRef, Target: targetRef, Stat: stat, Delta: after - before, CurrentStage: after,
				})
			}
			if len(events) == 0 {
				continue
			}
			itemID := target.ItemID
			target.StatStages = stages
			target = clearHeldItemRuntimeState(target)
			state.replaceMember(targetRef.Side, target)
			events = append(events, HeldItemStatReactionConsumedEvent{
				Type: EventKindHeldItemStatReactionConsumed, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Target: targetRef, ItemID: itemID, Reason: "opponentPositiveStatStageCopy",
			})
			return state, events
		}
	}
	return state, nil
}
