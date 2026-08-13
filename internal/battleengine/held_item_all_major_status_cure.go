package battleengine

// applyHeldItemAllMajorStatusCure 在成员刚成功获得任一种可治疗主要异常后结算一次性全范围净化道具。
//
// 它只接受资料明确声明的灼伤、麻痹、中毒、剧毒、睡眠和冰冻；调用方必须先写入 MajorStatusAppliedEvent。
// 睡眠和剧毒的附属计数会与 MajorStatus 同时清零，防止异常消失后仍留下行动限制或回合末伤害计数。
func applyHeldItemAllMajorStatusCure(state State, memberRef MemberRef, status MajorStatus) (State, []Event) {
	if !isCuredByAllMajorStatusItem(status) {
		return state, nil
	}
	member, found := state.member(memberRef.Side, memberRef.Position)
	if !found || member.CurrentHP == 0 || member.ItemID == 0 || !member.HeldItemCuresAllMajorStatuses || member.MajorStatus != status {
		return state, nil
	}
	itemID := member.ItemID
	member.MajorStatus = ""
	member.SleepTurnsRemaining = 0
	member.BadPoisonCounter = 0
	member = clearHeldItemRuntimeState(member)
	state.replaceMember(memberRef.Side, member)
	return state, []Event{HeldItemAllMajorStatusCuredEvent{
		Type: EventKindHeldItemAllMajorStatusCured, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Member: memberRef, ItemID: itemID, Status: status,
	}}
}

// isCuredByAllMajorStatusItem 将全主要异常净化规则限定为不可扩展的六种状态。
func isCuredByAllMajorStatusItem(status MajorStatus) bool {
	switch status {
	case MajorStatusBurn, MajorStatusParalysis, MajorStatusPoison, MajorStatusBadPoison, MajorStatusSleep, MajorStatusFreeze:
		return true
	default:
		return false
	}
}
