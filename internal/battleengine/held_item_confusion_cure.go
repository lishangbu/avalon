package battleengine

// applyHeldItemConfusionCure 在成员刚成功获得混乱后结算一次性混乱净化道具。
//
// 调用方必须先把 VolatileStatusAppliedEvent 写入事件流；本函数随后清除混乱持续回合和道具，确保回放顺序完整。
// 它只处理 confusion，不能复用为束缚、挑衅、定身、蓄力、锁招、保护或替身的泛化治疗入口。
func applyHeldItemConfusionCure(state State, memberRef MemberRef, status VolatileStatus) (State, []Event) {
	if status != VolatileStatusConfusion {
		return state, nil
	}
	member, found := state.member(memberRef.Side, memberRef.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || !member.HeldItemCuresConfusion || member.ConfusionTurnsRemaining == 0 {
		return state, nil
	}
	itemID := member.ItemID
	member.ConfusionTurnsRemaining = 0
	member = clearHeldItemRuntimeState(member)
	state.replaceMember(memberRef.Side, member)
	return state, []Event{HeldItemConfusionCuredEvent{
		Type: EventKindHeldItemConfusionCured, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Member: memberRef, ItemID: itemID, Status: VolatileStatusConfusion,
	}}
}
