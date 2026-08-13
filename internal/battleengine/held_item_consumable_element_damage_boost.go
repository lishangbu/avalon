package battleengine

// applyConsumableElementDamageBoostAfterBodyDamage 在匹配属性技能造成真实本体伤害后消费一次性威力强化道具。
//
// 强化倍率已在普通伤害公式的威力阶段读取；本函数只负责以同一段 DamageAppliedEvent 的真实本体损失作为消费
// 条件。替身伤害、未命中、免疫和其它零伤害路径都没有 bodyDamage，故绝不能清空道具。消费发生在接触道具
// 转移之前，避免同一段伤害把本应消耗的道具错误转交给攻击者或目标。
func applyConsumableElementDamageBoostAfterBodyDamage(
	state State,
	actorRef MemberRef,
	skill SkillSnapshot,
	bodyDamage uint32,
) (State, []Event) {
	if bodyDamage == 0 {
		return state, nil
	}
	actor, found := state.member(actorRef.Side, actorRef.Position)
	if !found || actor.CurrentHP == 0 || actor.ItemID == 0 ||
		actor.HeldItemConsumableElementDamageBoostElementID == 0 ||
		actor.HeldItemConsumableElementDamageBoostNumerator == 0 ||
		actor.HeldItemConsumableElementDamageBoostDenominator == 0 {
		return state, nil
	}
	elementID := effectiveSkillElementForMember(actor, skill, effectiveSkillWeather(state, actor))
	if elementID != actor.HeldItemConsumableElementDamageBoostElementID {
		return state, nil
	}
	itemID := actor.ItemID
	actor = clearHeldItemRuntimeState(actor)
	state.replaceMember(actorRef.Side, actor)
	return state, []Event{HeldItemElementDamageBoostConsumedEvent{
		Type: EventKindHeldItemElementDamageBoostConsumed, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actorRef, SkillID: skill.SkillID, ItemID: itemID, ElementID: elementID,
	}}
}
