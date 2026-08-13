package battleengine

// applyHeldItemSleepCure 在成员刚成功获得睡眠后结算一次性睡眠净化道具。
//
// 调用方已经消费睡眠时长随机数并写入 MajorStatusAppliedEvent；本函数随后同时清除 MajorStatus 和
// SleepTurnsRemaining，防止下一次行动读取到与空主要异常矛盾的睡眠限制。它只处理 sleep，不能扩大为通用净化。
func applyHeldItemSleepCure(state State, memberRef MemberRef, status MajorStatus) (State, []Event) {
	if status != MajorStatusSleep {
		return state, nil
	}
	member, found := state.member(memberRef.Side, memberRef.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || !member.HeldItemCuresSleep || member.MajorStatus != MajorStatusSleep {
		return state, nil
	}
	itemID := member.ItemID
	member.MajorStatus = ""
	member.SleepTurnsRemaining = 0
	member = clearHeldItemRuntimeState(member)
	state.replaceMember(memberRef.Side, member)
	return state, []Event{HeldItemSleepCuredEvent{
		Type: EventKindHeldItemSleepCured, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Member: memberRef, ItemID: itemID, Status: MajorStatusSleep,
	}}
}
