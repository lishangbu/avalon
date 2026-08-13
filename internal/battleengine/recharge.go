package battleengine

// applyRechargeAfterBodyDamage 在技能成功扣除目标本体生命后，为仍能战斗的使用者写入一次休整。
//
// bodyDamage 必须来自 DamageAppliedEvent，而不能使用包含替身承伤的“实际伤害”返回值。这样替身、保护、
// 未命中、属性免疫和零伤害都无法错误触发休整。函数幂等：多段或范围技能的后续目标命中不会重复叠加休整，
// 也不会重复写入 RechargeStartedEvent。
func applyRechargeAfterBodyDamage(
	state State,
	actorRef MemberRef,
	skill SkillSnapshot,
	bodyDamage uint32,
) (State, []Event) {
	if !skill.RechargesAfterUse || bodyDamage == 0 {
		return state, nil
	}
	actor, exists := state.member(actorRef.Side, actorRef.Position)
	if !exists || actor.CurrentHP == 0 || actor.RechargeTurnsRemaining != 0 {
		return state, nil
	}
	actor.RechargeTurnsRemaining = 1
	state.replaceMember(actorRef.Side, actor)
	return state, []Event{RechargeStartedEvent{
		Type: EventKindRechargeStarted, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Actor: actorRef, SkillID: skill.SkillID, TurnsRemainingAfterCurrent: actor.RechargeTurnsRemaining,
	}}
}
