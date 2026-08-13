package battleengine

// applyChargeSkipOnceItem 消耗成员当前的一次性蓄力跳过道具，并记录技能直接进入本回合结算的事实。
//
// 调用方只在技能已经通过行动前校验、完成 PP 消耗且确认其仍需蓄力时调用本函数。天气跳过蓄力不会进入此处，
// 因而不会错误消耗道具；非蓄力技能也不会读取该字段。
func applyChargeSkipOnceItem(state State, actorRef MemberRef, skill SkillSnapshot) (State, []Event, bool) {
	actor, exists := state.member(actorRef.Side, actorRef.Position)
	if !exists || actor.CurrentHP == 0 || actor.ItemID == 0 || !actor.ChargeSkipOnce {
		return state, nil, false
	}
	itemID := actor.ItemID
	actor = clearHeldItemRuntimeState(actor)
	state.replaceMember(actorRef.Side, actor)
	return state, []Event{SkillChargeSkippedByItemEvent{
		Type: EventKindSkillChargeSkippedByItem, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actorRef, SkillID: skill.SkillID, ItemID: itemID,
	}}, true
}
