package battleengine

// resolveSwitchInRevealOpponentHeldItems 结算成员实际换入后公开存活上场对手持有道具的规则。
func resolveSwitchInRevealOpponentHeldItems(state State, slot SlotRef) (State, []Event) {
	member, found := state.ActiveMember(slot)
	if !found || member.CurrentHP == 0 || !member.SwitchInRevealOpponentHeldItems {
		return state, nil
	}
	return applySwitchInRevealOpponentHeldItems(state, MemberRef{Side: slot.Side, Position: member.Position})
}

// initializeSwitchInRevealOpponentHeldItems 按冻结阵营和槽位顺序处理双方初始上场成员的道具公开规则。
//
// 初始阶段只产生可观察事件不会改变权威快照；后续实际换入会再次按当前存活对手公开。事件顺序使用快照的
// 阵营与槽位顺序，避免依赖客户端请求数组或数据库查询顺序。

func initializeSwitchInRevealOpponentHeldItems(state State) (State, []Event) {
	events := make([]Event, 0, 2)
	for _, side := range state.sides {
		for _, position := range side.ActiveMembers {
			member, found := state.member(side.Side, position)
			if !found || member.CurrentHP == 0 || !member.SwitchInRevealOpponentHeldItems {
				continue
			}
			var revealed []Event
			state, revealed = applySwitchInRevealOpponentHeldItems(state, MemberRef{Side: side.Side, Position: member.Position})
			events = append(events, revealed...)
		}
	}
	return state, events
}

// applySwitchInRevealOpponentHeldItems 为每一名当前存活且确实持有道具的对手产生独立公开事件。
func applySwitchInRevealOpponentHeldItems(state State, actor MemberRef) (State, []Event) {
	receiver, found := state.member(actor.Side, actor.Position)
	if !found || receiver.CurrentHP == 0 || !receiver.SwitchInRevealOpponentHeldItems {
		return state, nil
	}
	events := make([]Event, 0, 2)
	for _, side := range state.sides {
		if side.Side == actor.Side {
			continue
		}
		for _, position := range side.ActiveMembers {
			target, found := state.member(side.Side, position)
			if !found || target.CurrentHP == 0 || target.ItemID == 0 {
				continue
			}
			events = append(events, OpponentHeldItemRevealedEvent{
				Type: EventKindOpponentHeldItemRevealed, SchemaVersion: 1, TurnNumber: state.turnNumber,
				Actor: actor, Target: MemberRef{Side: side.Side, Position: target.Position}, ItemID: target.ItemID,
			})
		}
	}
	return state, events
}
