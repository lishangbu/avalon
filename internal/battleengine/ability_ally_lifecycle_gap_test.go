package battleengine_test

import (
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

var allyLifecycleAttackSkillID = testID("ally-lifecycle-attack")

// TestResolveTurnAllyAbilityRulesIgnoreReserveAndFaintedMembers 验证规则 129—131 只读取其它存活上场伙伴：
// 后备成员、已经倒下的场上成员以及攻击者自己声明的互助组资格都不能激活伙伴倍率。
func TestResolveTurnAllyAbilityRulesIgnoreReserveAndFaintedMembers(t *testing.T) {
	t.Parallel()

	t.Run("后备伙伴不生效", func(t *testing.T) {
		t.Parallel()
		actor := allyLifecycleAttacker()
		actor.AllyAbilityGroupCode = "plus-minus"
		actor.AllyAbilityPresenceAttackingStatMultiplier = &battleengine.AllyAbilityPresenceAttackingStatMultiplier{
			GroupCode: "plus-minus", Stat: battleengine.StatAttack, Numerator: 3, Denominator: 2,
		}
		activeAlly := allyLifecyclePassiveMember(2, "reserve-filter-active-ally")
		reserveAlly := allyLifecyclePassiveMember(3, "reserve-filter-reserve-ally")
		reserveAlly.AllySkillDamageBoost = &battleengine.AllySkillDamageBoost{
			DamageClasses: []battleengine.DamageClass{battleengine.DamageClassPhysical}, Numerator: 13, Denominator: 10,
		}
		reserveAlly.AllyAbilityGroupCode = "plus-minus"

		target := allyLifecyclePassiveMember(1, "reserve-filter-target")
		targetAlly := allyLifecyclePassiveMember(2, "reserve-filter-target-ally")
		targetReserve := allyLifecyclePassiveMember(3, "reserve-filter-target-reserve")
		targetReserve.AllyReceivedDamageReduction = &battleengine.AllyReceivedDamageReduction{Numerator: 3, Denominator: 4}

		state := newAllyLifecycleState(t,
			[]battleengine.MemberSnapshot{actor, activeAlly, reserveAlly},
			[]battleengine.MemberPosition{1, 2},
			[]battleengine.MemberSnapshot{target, targetAlly, targetReserve},
			[]battleengine.MemberPosition{1, 2},
		)
		_, damage, _ := resolveAllyLifecycleTurn(t, state, 1, allyLifecycleStandardActions())
		if damage != 37 {
			t.Fatalf("后备伙伴存在时伤害 = %d，期望无伙伴倍率基线 37", damage)
		}
	})

	t.Run("倒下伙伴不生效", func(t *testing.T) {
		t.Parallel()
		actor := allyLifecycleAttacker()
		actor.AllyAbilityGroupCode = "plus-minus"
		actor.AllyAbilityPresenceAttackingStatMultiplier = &battleengine.AllyAbilityPresenceAttackingStatMultiplier{
			GroupCode: "plus-minus", Stat: battleengine.StatAttack, Numerator: 3, Denominator: 2,
		}
		faintedAlly := allyLifecyclePassiveMember(2, "fainted-filter-ally")
		faintedAlly.CurrentHP = 0
		faintedAlly.AllySkillDamageBoost = &battleengine.AllySkillDamageBoost{
			DamageClasses: []battleengine.DamageClass{battleengine.DamageClassPhysical}, Numerator: 13, Denominator: 10,
		}
		faintedAlly.AllyAbilityGroupCode = "plus-minus"

		target := allyLifecyclePassiveMember(1, "fainted-filter-target")
		faintedTargetAlly := allyLifecyclePassiveMember(2, "fainted-filter-target-ally")
		faintedTargetAlly.CurrentHP = 0
		faintedTargetAlly.AllyReceivedDamageReduction = &battleengine.AllyReceivedDamageReduction{Numerator: 3, Denominator: 4}

		state := newAllyLifecycleState(t,
			[]battleengine.MemberSnapshot{actor, faintedAlly},
			[]battleengine.MemberPosition{1, 2},
			[]battleengine.MemberSnapshot{target, faintedTargetAlly},
			[]battleengine.MemberPosition{1, 2},
		)
		actions := []battleengine.Action{
			allyLifecycleUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
			allyLifecycleUseSkillAction(battleengine.SideTwo, 1, battleengine.SideTwo, 1),
		}
		_, damage, _ := resolveAllyLifecycleTurn(t, state, 1, actions)
		if damage != 37 {
			t.Fatalf("倒下伙伴仍占场上槽位时伤害 = %d，期望无伙伴倍率基线 37", damage)
		}
	})
}

// TestResolveTurnAllyAbilityRulesFollowSameTurnSwitches 验证伙伴在技能伤害结算前换入或换出时，
// 规则 129—131 会立即改读换人后的场上快照，不会缓存回合开始时的伙伴特性。
func TestResolveTurnAllyAbilityRulesFollowSameTurnSwitches(t *testing.T) {
	t.Parallel()

	configureRules := func(actor, actorAlly, targetAlly *battleengine.MemberSnapshot) {
		actor.AllyAbilityPresenceAttackingStatMultiplier = &battleengine.AllyAbilityPresenceAttackingStatMultiplier{
			GroupCode: "plus-minus", Stat: battleengine.StatAttack, Numerator: 3, Denominator: 2,
		}
		actorAlly.AllySkillDamageBoost = &battleengine.AllySkillDamageBoost{
			DamageClasses: []battleengine.DamageClass{battleengine.DamageClassPhysical}, Numerator: 13, Denominator: 10,
		}
		actorAlly.AllyAbilityGroupCode = "plus-minus"
		targetAlly.AllyReceivedDamageReduction = &battleengine.AllyReceivedDamageReduction{Numerator: 3, Denominator: 4}
	}

	t.Run("换出后同回合立即失效", func(t *testing.T) {
		t.Parallel()
		actor := allyLifecycleAttacker()
		outgoingAlly := allyLifecyclePassiveMember(2, "switch-out-actor-ally")
		incomingAlly := allyLifecyclePassiveMember(3, "switch-out-actor-reserve")
		target := allyLifecyclePassiveMember(1, "switch-out-target")
		outgoingTargetAlly := allyLifecyclePassiveMember(2, "switch-out-target-ally")
		incomingTargetAlly := allyLifecyclePassiveMember(3, "switch-out-target-reserve")
		configureRules(&actor, &outgoingAlly, &outgoingTargetAlly)

		state := newAllyLifecycleState(t,
			[]battleengine.MemberSnapshot{actor, outgoingAlly, incomingAlly},
			[]battleengine.MemberPosition{1, 2},
			[]battleengine.MemberSnapshot{target, outgoingTargetAlly, incomingTargetAlly},
			[]battleengine.MemberPosition{1, 2},
		)
		actions := allyLifecyclePartnerSwitchActions(3, 3)
		_, damage, _ := resolveAllyLifecycleTurn(t, state, 1, actions)
		if damage != 37 {
			t.Fatalf("伙伴换出后的同回合伤害 = %d，期望规则立即失效后的基线 37", damage)
		}
	})

	t.Run("换入后同回合立即生效", func(t *testing.T) {
		t.Parallel()
		actor := allyLifecycleAttacker()
		outgoingAlly := allyLifecyclePassiveMember(2, "switch-in-actor-ally")
		incomingAlly := allyLifecyclePassiveMember(3, "switch-in-actor-reserve")
		target := allyLifecyclePassiveMember(1, "switch-in-target")
		outgoingTargetAlly := allyLifecyclePassiveMember(2, "switch-in-target-ally")
		incomingTargetAlly := allyLifecyclePassiveMember(3, "switch-in-target-reserve")
		configureRules(&actor, &incomingAlly, &incomingTargetAlly)

		state := newAllyLifecycleState(t,
			[]battleengine.MemberSnapshot{actor, outgoingAlly, incomingAlly},
			[]battleengine.MemberPosition{1, 2},
			[]battleengine.MemberSnapshot{target, outgoingTargetAlly, incomingTargetAlly},
			[]battleengine.MemberPosition{1, 2},
		)
		actions := allyLifecyclePartnerSwitchActions(3, 3)
		_, damage, _ := resolveAllyLifecycleTurn(t, state, 1, actions)
		if damage != 52 {
			t.Fatalf("伙伴换入后的同回合伤害 = %d，期望三类伙伴倍率组合后的 52", damage)
		}
	})
}

// TestResolveTurnAllyAbilityMultipliersComposeAcrossCurrentPartners 验证规则 129 的最终增伤、规则 130 的
// 最终减伤与规则 131 的攻击能力倍率同时存在时按各自公式位置组合，而不是由后读取的伙伴规则覆盖前者。
func TestResolveTurnAllyAbilityMultipliersComposeAcrossCurrentPartners(t *testing.T) {
	t.Parallel()

	actor := allyLifecycleAttacker()
	actor.AllyAbilityPresenceAttackingStatMultiplier = &battleengine.AllyAbilityPresenceAttackingStatMultiplier{
		GroupCode: "plus-minus", Stat: battleengine.StatAttack, Numerator: 3, Denominator: 2,
	}
	actorAlly := allyLifecyclePassiveMember(2, "compose-actor-ally")
	actorAlly.AllySkillDamageBoost = &battleengine.AllySkillDamageBoost{
		DamageClasses: []battleengine.DamageClass{battleengine.DamageClassPhysical}, Numerator: 13, Denominator: 10,
	}
	actorAlly.AllyAbilityGroupCode = "plus-minus"
	target := allyLifecyclePassiveMember(1, "compose-target")
	targetAlly := allyLifecyclePassiveMember(2, "compose-target-ally")
	targetAlly.AllyReceivedDamageReduction = &battleengine.AllyReceivedDamageReduction{Numerator: 3, Denominator: 4}

	state := newAllyLifecycleState(t,
		[]battleengine.MemberSnapshot{actor, actorAlly},
		[]battleengine.MemberPosition{1, 2},
		[]battleengine.MemberSnapshot{target, targetAlly},
		[]battleengine.MemberPosition{1, 2},
	)
	_, damage, _ := resolveAllyLifecycleTurn(t, state, 1, allyLifecycleStandardActions())
	if damage != 52 {
		t.Fatalf("三类伙伴倍率组合后的伤害 = %d，期望规则公式顺序对应的 52", damage)
	}
}

// TestResolveTurnTargetAbilityIgnoringPreservesAllyGuard 验证规则 130 属于目标的其它伙伴：攻击者无视目标
// 自身特性时仍须保留伙伴守护，并且同一个最终倍率也必须进入替身承伤路径。
func TestResolveTurnTargetAbilityIgnoringPreservesAllyGuard(t *testing.T) {
	t.Parallel()

	t.Run("目标本体承伤", func(t *testing.T) {
		t.Parallel()
		actor := allyLifecycleAttacker()
		actor.IgnoreTargetAbilityEffects = true
		actorAlly := allyLifecyclePassiveMember(2, "ignore-target-body-actor-ally")
		target := allyLifecyclePassiveMember(1, "ignore-target-body")
		// 目标先在满生命使用无随机等待技能，避免回合末的测试占位回复掩盖本体伤害快照。
		target.Stats.Speed = 250
		target.DamageClassDamageReduction = &battleengine.DamageClassDamageReduction{
			DamageClasses: []battleengine.DamageClass{battleengine.DamageClassPhysical}, Numerator: 1, Denominator: 2,
		}
		targetAlly := allyLifecyclePassiveMember(2, "ignore-target-body-ally")
		targetAlly.AllyReceivedDamageReduction = &battleengine.AllyReceivedDamageReduction{Numerator: 3, Denominator: 4}

		state := newAllyLifecycleState(t,
			[]battleengine.MemberSnapshot{actor, actorAlly},
			[]battleengine.MemberPosition{1, 2},
			[]battleengine.MemberSnapshot{target, targetAlly},
			[]battleengine.MemberPosition{1, 2},
		)
		resolved, damage, events := resolveAllyLifecycleTurn(t, state, 1, allyLifecycleStandardActions())
		if damage != 27 {
			t.Fatalf("无视目标特性后的伙伴守护伤害 = %d，期望只保留伙伴 3/4 倍率后的 27", damage)
		}
		finalTarget, found := resolved.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
		if !found || finalTarget.CurrentHP != 973 || !allyLifecycleEventKindExists(events, battleengine.EventKindDamageApplied) {
			t.Fatalf("目标本体承伤快照或事件错误: target=%+v events=%v", finalTarget, eventKinds(events))
		}
	})

	t.Run("替身承伤", func(t *testing.T) {
		t.Parallel()
		actor := allyLifecycleAttacker()
		actor.IgnoreTargetAbilityEffects = true
		actorAlly := allyLifecyclePassiveMember(2, "ignore-target-substitute-actor-ally")
		target := allyLifecycleSubstituteMember(1, "ignore-target-substitute")
		target.DamageClassDamageReduction = &battleengine.DamageClassDamageReduction{
			DamageClasses: []battleengine.DamageClass{battleengine.DamageClassPhysical}, Numerator: 1, Denominator: 2,
		}
		targetAlly := allyLifecyclePassiveMember(2, "ignore-target-substitute-ally")
		targetAlly.AllyReceivedDamageReduction = &battleengine.AllyReceivedDamageReduction{Numerator: 3, Denominator: 4}

		state := newAllyLifecycleState(t,
			[]battleengine.MemberSnapshot{actor, actorAlly},
			[]battleengine.MemberPosition{1, 2},
			[]battleengine.MemberSnapshot{target, targetAlly},
			[]battleengine.MemberPosition{1, 2},
		)
		actions := []battleengine.Action{
			allyLifecycleUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
			allyLifecycleUseSkillAction(battleengine.SideOne, 2, battleengine.SideOne, 2),
			allyLifecycleUseSkillAction(battleengine.SideTwo, 1, battleengine.SideTwo, 1),
			allyLifecycleUseSkillAction(battleengine.SideTwo, 2, battleengine.SideTwo, 2),
		}
		resolved, damage, events := resolveAllyLifecycleTurn(t, state, 1, actions)
		if damage != 27 {
			t.Fatalf("替身承受的伙伴守护伤害 = %d，期望 27", damage)
		}
		finalTarget, found := resolved.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
		if !found || finalTarget.CurrentHP != 750 || finalTarget.SubstituteHP != 223 ||
			!eventOccursBefore(events, battleengine.EventKindSubstituteStarted, battleengine.EventKindSubstituteDamageApplied) ||
			allyLifecycleEventKindExists(events, battleengine.EventKindDamageApplied) {
			t.Fatalf("替身路径快照或事件错误: target=%+v events=%v", finalTarget, eventKinds(events))
		}
	})
}

// TestResolveTurnCopiedAbilityImmediatelyProvidesAllyRules 验证入场复制取得的不是静态特性名称，而是规则
// 129 与 131 的完整冻结字段；复制完成后同侧伙伴的下一次伤害会立即读取新规则。
func TestResolveTurnCopiedAbilityImmediatelyProvidesAllyRules(t *testing.T) {
	t.Parallel()

	actor := allyLifecycleAttacker()
	actor.AllyAbilityPresenceAttackingStatMultiplier = &battleengine.AllyAbilityPresenceAttackingStatMultiplier{
		GroupCode: "plus-minus", Stat: battleengine.StatAttack, Numerator: 3, Denominator: 2,
	}
	receiver := allyLifecyclePassiveMember(2, "copy-ally-rule-receiver")
	receiver.AbilityID = testID("trace-ability")
	receiver.SwitchInCopyOpponentAbility = true
	target := allyLifecyclePassiveMember(1, "copy-ally-rule-source")
	target.AbilityID = testID("copied-ally-aura")
	target.AllySkillDamageBoost = &battleengine.AllySkillDamageBoost{
		DamageClasses: []battleengine.DamageClass{battleengine.DamageClassPhysical}, Numerator: 13, Denominator: 10,
	}
	target.AllyAbilityGroupCode = "plus-minus"
	targetAlly := allyLifecyclePassiveMember(2, "copy-ally-rule-target-ally")

	state := newAllyLifecycleState(t,
		[]battleengine.MemberSnapshot{actor, receiver},
		[]battleengine.MemberPosition{1, 2},
		[]battleengine.MemberSnapshot{target, targetAlly},
		[]battleengine.MemberPosition{1, 2},
	)
	copied, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 2})
	if !found || copied.AbilityID != target.AbilityID || copied.AllySkillDamageBoost == nil ||
		copied.AllySkillDamageBoost.Numerator != 13 || copied.AllySkillDamageBoost.Denominator != 10 ||
		copied.AllyAbilityGroupCode != "plus-minus" {
		t.Fatalf("复制后的伙伴规则不完整: %+v", copied)
	}
	_, damage, _ := resolveAllyLifecycleTurn(t, state, 1, allyLifecycleStandardActions())
	if damage != 70 {
		t.Fatalf("复制伙伴特性后的伤害 = %d，期望互助组与伙伴增伤组合后的 70", damage)
	}
}

// TestResolveTurnTransformedAllyRulesRestoreAndReapply 验证变身会把来源的规则 129 与 131 带入当前画像；
// 变身者离场时原始画像立即恢复，再次换入变身后伙伴规则重新生效，整个过程不读取实时资料。
func TestResolveTurnTransformedAllyRulesRestoreAndReapply(t *testing.T) {
	t.Parallel()

	actor := allyLifecycleAttacker()
	actor.AllyAbilityPresenceAttackingStatMultiplier = &battleengine.AllyAbilityPresenceAttackingStatMultiplier{
		GroupCode: "plus-minus", Stat: battleengine.StatAttack, Numerator: 3, Denominator: 2,
	}
	transformer := allyLifecyclePassiveMember(2, "transform-ally-rule-receiver")
	transformer.AbilityID = testID("transform-ability")
	transformer.SwitchInTransformIntoOpponent = true
	// 变身会复制来源的速度和技能；后攻道具投影保持属于变身者自身，从而避免双方等待行动形成与规则无关的同速随机。
	transformer.ItemID = testID("transform-order-item")
	transformer.HeldItemForcedLastActionOrder = true
	reserve := allyLifecyclePassiveMember(3, "transform-ally-rule-reserve")
	target := allyLifecyclePassiveMember(1, "transform-ally-rule-source")
	target.AbilityID = testID("transform-source-aura")
	target.AllySkillDamageBoost = &battleengine.AllySkillDamageBoost{
		DamageClasses: []battleengine.DamageClass{battleengine.DamageClassPhysical}, Numerator: 13, Denominator: 10,
	}
	target.AllyAbilityGroupCode = "plus-minus"
	targetAlly := allyLifecyclePassiveMember(2, "transform-ally-rule-target-ally")

	state := newAllyLifecycleState(t,
		[]battleengine.MemberSnapshot{actor, transformer, reserve},
		[]battleengine.MemberPosition{1, 2},
		[]battleengine.MemberSnapshot{target, targetAlly},
		[]battleengine.MemberPosition{1, 2},
	)
	transformed, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 2})
	if !found || transformed.TransformSnapshot == nil || transformed.AllySkillDamageBoost == nil ||
		transformed.AllyAbilityGroupCode != "plus-minus" {
		t.Fatalf("初始变身后的伙伴规则不完整: %+v", transformed)
	}

	first, firstDamage, _ := resolveAllyLifecycleTurn(t, state, 1, allyLifecycleStandardActions())
	if firstDamage != 70 {
		t.Fatalf("变身取得伙伴规则后的第一回合伤害 = %d，期望 70", firstDamage)
	}

	secondActions := []battleengine.Action{
		allyLifecycleUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		allyLifecycleSwitchAction(battleengine.SideOne, 2, 3),
		allyLifecycleUseSkillAction(battleengine.SideTwo, 1, battleengine.SideTwo, 1),
		allyLifecycleUseSkillAction(battleengine.SideTwo, 2, battleengine.SideTwo, 2),
	}
	second, secondDamage, _ := resolveAllyLifecycleTurn(t, first, 2, secondActions)
	if secondDamage != 37 {
		t.Fatalf("变身者离场后的同回合伤害 = %d，期望恢复无伙伴规则基线 37", secondDamage)
	}
	restored := allyLifecycleMemberByPosition(t, second, battleengine.SideOne, 2)
	if restored.TransformSnapshot != nil || restored.AllySkillDamageBoost != nil || restored.AllyAbilityGroupCode != "" ||
		!restored.SwitchInTransformIntoOpponent || restored.AbilityID != testID("transform-ability") {
		t.Fatalf("变身者离场后的原始伙伴规则画像未恢复: %+v", restored)
	}

	thirdActions := []battleengine.Action{
		allyLifecycleUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		allyLifecycleSwitchAction(battleengine.SideOne, 2, 2),
		allyLifecycleUseSkillAction(battleengine.SideTwo, 1, battleengine.SideTwo, 1),
		allyLifecycleUseSkillAction(battleengine.SideTwo, 2, battleengine.SideTwo, 2),
	}
	third, thirdDamage, thirdEvents := resolveAllyLifecycleTurn(t, second, 3, thirdActions)
	if thirdDamage != 70 {
		t.Fatalf("变身者再次换入后的同回合伤害 = %d，期望重新取得伙伴规则后的 70", thirdDamage)
	}
	retransformed, found := third.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 2})
	if !found || retransformed.TransformSnapshot == nil || retransformed.AllySkillDamageBoost == nil ||
		!allyLifecycleEventKindExists(thirdEvents, battleengine.EventKindParticipantTransformed) {
		t.Fatalf("再次换入后的变身状态或事件缺失: member=%+v events=%v", retransformed, eventKinds(thirdEvents))
	}
}

// allyLifecycleAttacker 构造规则 129—131 测试中唯一产生普通伤害的攻击者。
func allyLifecycleAttacker() battleengine.MemberSnapshot {
	member := newMember(1, "ally-lifecycle-attacker", 1_000, 1_000)
	member.Stats.Speed = 200
	member.ElementIDs = testIDs("ally-lifecycle-neutral")
	member.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: allyLifecycleAttackSkillID, Name: "伙伴规则攻击", ElementID: testID("ally-lifecycle-attack-element"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 10, MaxPP: 10,
	}
	return member
}

// allyLifecyclePassiveMember 构造不会产生伤害、随机消费或额外战斗状态的完整回合参与者。
func allyLifecyclePassiveMember(position battleengine.MemberPosition, creatureID string) battleengine.MemberSnapshot {
	member := newMember(position, creatureID, 1_000, 1_000)
	// 测试成员按稳定标识取得互不相关的确定速度，避免等待行动之间的同速决胜污染伙伴规则随机轨迹。
	speedCode := uint32(17)
	for _, value := range []byte(creatureID) {
		speedCode = (speedCode*31 + uint32(value)) % 150
	}
	member.Stats.Speed = 20 + speedCode
	member.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID(creatureID + "-wait"), Name: "等待", ElementID: testID("ally-lifecycle-neutral"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, HealingPercent: 1,
	}
	return member
}

// allyLifecycleSubstituteMember 构造会在攻击者行动前合法建立四分之一最大生命替身的目标成员。
func allyLifecycleSubstituteMember(position battleengine.MemberPosition, creatureID string) battleengine.MemberSnapshot {
	member := allyLifecyclePassiveMember(position, creatureID)
	member.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID(creatureID + "-substitute"), Name: "替身", ElementID: testID("ally-lifecycle-neutral"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Priority: 1, Accuracy: 100, RemainingPP: 10, MaxPP: 10,
		VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
			Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
			ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
		}},
	}
	return member
}

// newAllyLifecycleState 通过公开初始状态入口构造双打快照，并允许每侧携带一名后备成员。
func newAllyLifecycleState(
	t *testing.T,
	sideOneMembers []battleengine.MemberSnapshot,
	sideOneActive []battleengine.MemberPosition,
	sideTwoMembers []battleengine.MemberSnapshot,
	sideTwoActive []battleengine.MemberPosition,
) battleengine.State {
	t.Helper()
	teamSize := max(len(sideOneMembers), len(sideTwoMembers))
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{
			Code: "ally-lifecycle-double", ActiveSlotsPerSide: battleengine.SlotPosition(len(sideOneActive)), TeamSize: uint8(teamSize),
		},
		Rules: battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: sideOneActive, Members: sideOneMembers},
			{Side: battleengine.SideTwo, ActiveMembers: sideTwoActive, Members: sideTwoMembers},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// resolveAllyLifecycleTurn 使用批准用例的固定要害与满伤害浮动轨迹结算一回合，
// 并返回攻击者写入目标本体或替身的单段实际伤害。
func resolveAllyLifecycleTurn(
	t *testing.T,
	state battleengine.State,
	turnNumber uint32,
	actions []battleengine.Action,
) (battleengine.State, uint32, []battleengine.Event) {
	t.Helper()
	script := []battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + allyLifecycleAttackSkillID.String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + allyLifecycleAttackSkillID.String(), Value: 15},
	}
	random, err := battleengine.NewTracedRandom(script)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: turnNumber, Actions: actions,
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if !reflect.DeepEqual(result.RandomTrace, script) {
		t.Fatalf("伙伴规则改变了随机消费: got=%+v want=%+v", result.RandomTrace, script)
	}
	actor := battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}
	for _, event := range result.Events {
		switch value := event.(type) {
		case battleengine.DamageAppliedEvent:
			if value.Actor == actor && value.SkillID == allyLifecycleAttackSkillID {
				return result.State, value.Amount, result.Events
			}
		case battleengine.SubstituteDamageAppliedEvent:
			if value.Actor == actor && value.SkillID == allyLifecycleAttackSkillID {
				return result.State, value.Amount, result.Events
			}
		}
	}
	t.Fatalf("伙伴规则测试未产生攻击者伤害事件: %v", eventKinds(result.Events))
	return result.State, 0, result.Events
}

// allyLifecycleStandardActions 构造四名存活上场成员的完整双打回合命令。
func allyLifecycleStandardActions() []battleengine.Action {
	return []battleengine.Action{
		allyLifecycleUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		allyLifecycleUseSkillAction(battleengine.SideOne, 2, battleengine.SideOne, 2),
		allyLifecycleUseSkillAction(battleengine.SideTwo, 1, battleengine.SideTwo, 1),
		allyLifecycleUseSkillAction(battleengine.SideTwo, 2, battleengine.SideTwo, 2),
	}
}

// allyLifecyclePartnerSwitchActions 构造双方第二槽伙伴在攻击者行动前同时换人的完整命令。
func allyLifecyclePartnerSwitchActions(
	sideOneMember battleengine.MemberPosition,
	sideTwoMember battleengine.MemberPosition,
) []battleengine.Action {
	return []battleengine.Action{
		allyLifecycleUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		allyLifecycleSwitchAction(battleengine.SideOne, 2, sideOneMember),
		allyLifecycleUseSkillAction(battleengine.SideTwo, 1, battleengine.SideTwo, 1),
		allyLifecycleSwitchAction(battleengine.SideTwo, 2, sideTwoMember),
	}
}

// allyLifecycleUseSkillAction 构造伙伴生命周期测试使用的显式技能行动。
func allyLifecycleUseSkillAction(
	actorSide battleengine.Side,
	actorSlot battleengine.SlotPosition,
	targetSide battleengine.Side,
	targetSlot battleengine.SlotPosition,
) battleengine.Action {
	return battleengine.Action{
		Kind:  battleengine.ActionKindUseSkill,
		Actor: battleengine.SlotRef{Side: actorSide, Position: actorSlot},
		UseSkill: &battleengine.UseSkillAction{
			SkillPosition: 1, Target: battleengine.SlotRef{Side: targetSide, Position: targetSlot},
		},
	}
}

// allyLifecycleSwitchAction 构造伙伴生命周期测试使用的显式换人行动。
func allyLifecycleSwitchAction(
	side battleengine.Side,
	slot battleengine.SlotPosition,
	member battleengine.MemberPosition,
) battleengine.Action {
	return battleengine.Action{
		Kind:   battleengine.ActionKindSwitch,
		Actor:  battleengine.SlotRef{Side: side, Position: slot},
		Switch: &battleengine.SwitchAction{MemberPosition: member},
	}
}

// allyLifecycleMemberByPosition 从公开状态快照中按阵营和 Team 成员位置读取成员。
func allyLifecycleMemberByPosition(
	t *testing.T,
	state battleengine.State,
	side battleengine.Side,
	position battleengine.MemberPosition,
) battleengine.MemberSnapshot {
	t.Helper()
	for _, sideSnapshot := range state.Snapshot().Sides {
		if sideSnapshot.Side != side {
			continue
		}
		for _, member := range sideSnapshot.Members {
			if member.Position == position {
				return member
			}
		}
	}
	t.Fatalf("状态中不存在阵营 %d 的成员位置 %d", side, position)
	return battleengine.MemberSnapshot{}
}

// allyLifecycleEventKindExists 报告公开事件流是否包含指定稳定事件种类。
func allyLifecycleEventKindExists(events []battleengine.Event, kind battleengine.EventKind) bool {
	for _, event := range events {
		if event.Kind() == kind {
			return true
		}
	}
	return false
}
