package battleengine

// applyContactDamageToAttacker 在目标因有效接触技能失去本体生命后，结算其特性对攻击者造成的固定比例反制伤害。
//
// bodyDamage 必须来自同一段 DamageAppliedEvent。替身、保护、未命中、属性免疫和零伤害均不会产生该事件，
// 因而不会误触发反制。反制基数是攻击者最大生命而不是本段伤害，避免多段、要害和减伤改变特性本身的规则语义。
func applyContactDamageToAttacker(
	state State,
	attackerRef MemberRef,
	defenderRef MemberRef,
	skill SkillSnapshot,
	bodyDamage uint32,
) (State, []Event) {
	if bodyDamage == 0 || attackerRef.Side == defenderRef.Side {
		return state, nil
	}
	attacker, attackerExists := state.member(attackerRef.Side, attackerRef.Position)
	defender, defenderExists := state.member(defenderRef.Side, defenderRef.Position)
	if !attackerExists || !defenderExists || attacker.CurrentHP == 0 ||
		!skillMakesEffectiveContact(attacker, skill) || attacker.ContactSideEffectImmunity ||
		attacker.IndirectDamageImmunity {
		return state, nil
	}
	var events []Event
	if defender.ContactDamageToAttackerDenominator != 0 && !ignoresTargetAbilityEffects(attacker, skill) {
		state, events = applySingleContactDamageToAttacker(
			state, attackerRef, defenderRef, skill, defender.ContactDamageToAttackerDenominator, defender.AbilityID, 0,
		)
	}
	if defender.HeldItemContactDamageToAttackerDenominator == 0 {
		return state, events
	}
	var itemEvents []Event
	state, itemEvents = applySingleContactDamageToAttacker(
		state, attackerRef, defenderRef, skill, defender.HeldItemContactDamageToAttackerDenominator, 0, defender.ItemID,
	)
	return state, append(events, itemEvents...)
}

// applySingleContactDamageToAttacker 执行一个确定来源的接触反制伤害，并保留来源特性或持有道具供回放消费者审计。
//
// 调用方已完成“真实本体伤害、有效接触、攻击方免疫”这些共享 gate。该函数仍重新读取攻击者当前生命，保证
// 前一条特性反制已经击倒攻击者时，后续道具反制不会写出零伤害或重复倒下事件。
func applySingleContactDamageToAttacker(
	state State,
	attackerRef MemberRef,
	defenderRef MemberRef,
	skill SkillSnapshot,
	denominatorValue uint16,
	sourceAbilityID Identifier,
	sourceItemID Identifier,
) (State, []Event) {
	attacker, exists := state.member(attackerRef.Side, attackerRef.Position)
	if !exists || attacker.CurrentHP == 0 || denominatorValue == 0 {
		return state, nil
	}
	denominator := uint32(denominatorValue)
	damage := max(attacker.MaxHP/denominator, 1)
	damage = min(damage, attacker.CurrentHP)
	attacker.CurrentHP -= damage
	state.replaceMember(attackerRef.Side, attacker)
	events := []Event{ContactDamageAppliedEvent{
		Type: EventKindContactDamageApplied, SchemaVersion: 1, TurnNumber: state.turnNumber,
		Source: defenderRef, Target: attackerRef, SkillID: skill.SkillID, SourceAbilityID: sourceAbilityID, SourceItemID: sourceItemID,
		Denominator: denominatorValue, Amount: damage, CurrentHP: attacker.CurrentHP,
	}}
	if attacker.CurrentHP == 0 {
		events = append(events, ParticipantFaintedEvent{
			Type: EventKindParticipantFainted, SchemaVersion: 1, TurnNumber: state.turnNumber,
			Target: attackerRef, Cause: FaintCauseContactDamage, SkillID: skill.SkillID,
		})
	}
	return state, events
}
