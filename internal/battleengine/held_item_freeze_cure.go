package battleengine

// applyHeldItemFreezeCure 在成员刚成功获得冰冻后结算一次性冰冻净化道具。
//
// 调用方必须先把 MajorStatusAppliedEvent 写入事件流；本函数随后才清除冰冻和道具，确保重放能观察到完整顺序。
// 它只处理 freeze，不能复用为其它主要异常的泛化治疗入口，避免扩大道具资料声明的适用范围。
func applyHeldItemFreezeCure(state State, memberRef MemberRef, status MajorStatus) (State, []Event) {
	if status != MajorStatusFreeze {
		return state, nil
	}
	member, found := state.member(memberRef.Side, memberRef.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || !member.HeldItemCuresFreeze || member.MajorStatus != MajorStatusFreeze {
		return state, nil
	}
	itemID := member.ItemID
	member.MajorStatus = ""
	member = clearHeldItemRuntimeState(member)
	state.replaceMember(memberRef.Side, member)
	return state, []Event{HeldItemFreezeCuredEvent{
		Type: EventKindHeldItemFreezeCured, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Member: memberRef, ItemID: itemID, Status: MajorStatusFreeze,
	}}
}
