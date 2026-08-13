package battleengine

// heldItemElementDamageReductionApplies 判断防守方当前抗性道具是否应减半本次普通本体伤害。
//
// 规则只读取 Battle 冻结的技能属性 Identifier、目标当前属性与相性表。替身存在时伤害不会命中本体，因此不减伤也不
// 消费；要求严格克制的道具会在强风等规则修正后的最终相性乘积大于 1 时才触发。一般属性专用道具可显式关闭
// 严格克制要求，但仍必须匹配技能有效属性。
func heldItemElementDamageReductionApplies(
	rules RuleSnapshot,
	strongWeather *StrongWeatherState,
	defender MemberSnapshot,
	skillElementID Identifier,
) bool {
	if defender.ItemID == 0 || defender.SubstituteHP != 0 || defender.HeldItemElementDamageReductionElementID == 0 ||
		skillElementID != defender.HeldItemElementDamageReductionElementID {
		return false
	}
	if !defender.HeldItemElementDamageReductionRequiresSuperEffective {
		return true
	}
	numerator, denominator := uint64(1), uint64(1)
	for _, defenseElementID := range defender.ElementIDs {
		effectivenessNumerator, effectivenessDenominator := rules.effectiveness(skillElementID, defenseElementID)
		if strongWeather != nil && strongWindsNeutralizeFlyingWeakness(*strongWeather, rules, skillElementID, defenseElementID) {
			effectivenessNumerator, effectivenessDenominator = 1, 1
		}
		if effectivenessNumerator == 0 {
			return false
		}
		numerator *= uint64(effectivenessNumerator)
		denominator *= uint64(effectivenessDenominator)
	}
	return numerator > denominator
}

// applyElementDamageReductionAfterBodyDamage 在抗性道具已减免真实本体伤害后清空其完整运行态并记录消费事件。
//
// bodyDamage 来自同一段 DamageAppliedEvent；零值明确排除替身、未命中、免疫和其它无本体损失路径。再次读取
// 当前状态可以确保多段技能只有第一段享受并消费道具，后续段看到的 ItemID 已为空。
func applyElementDamageReductionAfterBodyDamage(
	state State,
	actorRef MemberRef,
	targetRef MemberRef,
	skill SkillSnapshot,
	bodyDamage uint32,
) (State, []Event) {
	if bodyDamage == 0 {
		return state, nil
	}
	actor, actorFound := state.member(actorRef.Side, actorRef.Position)
	target, found := state.member(targetRef.Side, targetRef.Position)
	if !actorFound || !found || target.ItemID == 0 || target.HeldItemElementDamageReductionElementID == 0 {
		return state, nil
	}
	elementID := effectiveSkillElementForMember(actor, skill, effectiveSkillWeather(state, actor))
	// 此处目标已经扣除本体生命但属性与相性事实未改变，可以复用同一判定；SubstituteHP 必为零。
	if !heldItemElementDamageReductionApplies(state.rules, effectiveStrongWeather(state), target, elementID) {
		return state, nil
	}
	itemID := target.ItemID
	target = clearHeldItemRuntimeState(target)
	state.replaceMember(targetRef.Side, target)
	return state, []Event{HeldItemElementDamageReductionConsumedEvent{
		Type: EventKindHeldItemElementDamageReductionConsumed, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Target: targetRef, SkillID: skill.SkillID, ItemID: itemID, ElementID: elementID,
	}}
}
