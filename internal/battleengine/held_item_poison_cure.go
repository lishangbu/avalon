package battleengine

// applyHeldItemPoisonCure 在成员刚成功获得普通中毒或剧毒后结算一次性中毒净化道具。
// 调用方必须先写入 MajorStatusAppliedEvent；本函数随后清除主要异常、剧毒计数和道具投影，保持可重放事件顺序。
func applyHeldItemPoisonCure(state State, memberRef MemberRef, status MajorStatus) (State, []Event) {
	if status != MajorStatusPoison && status != MajorStatusBadPoison {
		return state, nil
	}
	member, found := state.member(memberRef.Side, memberRef.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || !member.HeldItemCuresPoison || member.MajorStatus != status {
		return state, nil
	}
	itemID := member.ItemID
	member.MajorStatus = ""
	member.BadPoisonCounter = 0
	member = clearHeldItemRuntimeState(member)
	state.replaceMember(memberRef.Side, member)
	return state, []Event{HeldItemPoisonCuredEvent{
		Type: EventKindHeldItemPoisonCured, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Member: memberRef, ItemID: itemID, Status: status,
	}}
}
