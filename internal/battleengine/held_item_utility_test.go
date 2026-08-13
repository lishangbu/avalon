package battleengine_test

import (
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemDamageDealtHealing 验证贝壳之铃按实际技能伤害八分之一回复且伤害事件先于回复事件。
func TestHeldItemDamageDealtHealing(t *testing.T) {
	t.Parallel()
	result := resolveHeldItemUtilityDamageTurn(t, battleengine.DamageClassPhysical, 0, func(member *battleengine.MemberSnapshot) {
		member.HeldItemDamageDealtHeal = true
	})
	if result.actor.CurrentHP <= 400 || !healingSourceExists(result.events, battleengine.SkillHealingSourceHeldItemDamageDealt) ||
		!eventOccursBefore(result.events, battleengine.EventKindDamageApplied, battleengine.EventKindSkillHealingApplied) {
		t.Fatalf("伤害后道具回复错误: actor=%+v events=%v", result.actor, eventKinds(result.events))
	}
}

// TestHeldItemDrainHealingBoost 验证大根茎只把正吸取回复在原始结果上提高 30%。
func TestHeldItemDrainHealingBoost(t *testing.T) {
	t.Parallel()
	plain := resolveHeldItemUtilityDamageTurn(t, battleengine.DamageClassPhysical, 50, nil)
	boosted := resolveHeldItemUtilityDamageTurn(t, battleengine.DamageClassPhysical, 50, func(member *battleengine.MemberSnapshot) {
		member.HeldItemDrainHealingBoost = true
	})
	if boosted.actor.CurrentHP <= plain.actor.CurrentHP {
		t.Fatalf("吸取强化后生命 = %d，期望高于基线 %d", boosted.actor.CurrentHP, plain.actor.CurrentHP)
	}
}

// TestHeldItemSpecialDefenseBoost 验证突击背心只降低普通特殊伤害，不影响物理伤害。
func TestHeldItemSpecialDefenseBoost(t *testing.T) {
	t.Parallel()
	withVestSpecial := resolveHeldItemUtilityDefenseTurn(t, battleengine.DamageClassSpecial, true)
	withoutVestSpecial := resolveHeldItemUtilityDefenseTurn(t, battleengine.DamageClassSpecial, false)
	withVestPhysical := resolveHeldItemUtilityDefenseTurn(t, battleengine.DamageClassPhysical, true)
	withoutVestPhysical := resolveHeldItemUtilityDefenseTurn(t, battleengine.DamageClassPhysical, false)
	if withVestSpecial >= withoutVestSpecial || withVestPhysical != withoutVestPhysical {
		t.Fatalf("突击背心伤害错误: special=%d/%d physical=%d/%d", withVestSpecial, withoutVestSpecial, withVestPhysical, withoutVestPhysical)
	}
}

// TestHeldItemStatusSkillRestriction 验证突击背心在命令校验阶段拒绝变化技能，但不影响普通伤害技能。
func TestHeldItemStatusSkillRestriction(t *testing.T) {
	t.Parallel()
	actor := newMember(1, "assault-vest-actor", 500, 500)
	actor.ItemID = testID("assault-vest")
	actor.HeldItemStatusSkillRestriction = true
	actor.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("status-skill"), Name: "变化技能", DamageClass: battleengine.DamageClassStatus,
		ElementID: actor.ElementIDs[0], TargetScope: battleengine.SkillTargetScopeSelf, Accuracy: 100, RemainingPP: 10, MaxPP: 10, HealingPercent: 50,
	}
	target := newMember(1, "assault-vest-target", 500, 500)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "assault-vest", ActiveSlotsPerSide: 1, TeamSize: 1}, Rules: battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{actor}}, {Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}}},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	_, err = battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{{
		Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
		UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}},
	}}}, mustRandom(t, 761))
	if !errors.Is(err, battleengine.ErrInvalidTurnCommand) {
		t.Fatalf("ResolveTurn() 变化技能限制 error = %v", err)
	}
}

// TestHeldItemAirborneEndsAfterBodyDamage 验证气球在真实本体伤害后失去空中效果，但道具所有权保持。
func TestHeldItemAirborneEndsAfterBodyDamage(t *testing.T) {
	t.Parallel()
	damage, events := resolveHeldItemUtilityDefenseResult(t, battleengine.DamageClassPhysical, func(target *battleengine.MemberSnapshot) {
		target.ItemID = testID("air-balloon")
		target.HeldItemAirborneUntilDamaged = true
	})
	if damage.ItemID != testID("air-balloon") || damage.HeldItemAirborneUntilDamaged {
		t.Fatalf("气球受伤后快照 = %+v", damage)
	}
	if !eventOccursBefore(events, battleengine.EventKindDamageApplied, battleengine.EventKindHeldItemAirborneEnded) {
		t.Fatalf("气球失效事件顺序错误: %v", eventKinds(events))
	}
}

// heldItemUtilityDamageResult 保存道具伤害、回复测试的最终成员和事件。
type heldItemUtilityDamageResult struct {
	actor, target battleengine.MemberSnapshot
	events        []battleengine.Event
}

// resolveHeldItemUtilityDamageTurn 构造攻击方缺失生命、目标只执行无害自我回复的普通伤害回合。
func resolveHeldItemUtilityDamageTurn(t *testing.T, class battleengine.DamageClass, drain int8, configure func(*battleengine.MemberSnapshot)) heldItemUtilityDamageResult {
	t.Helper()
	actor := newMember(1, "utility-actor", 500, 400)
	actor.Stats.Speed = 200
	actor.ItemID = testID("utility-item")
	actor.Skills[0].DamageClass = class
	actor.Skills[0].DrainPercent = drain
	if configure != nil {
		configure(&actor)
	}
	target := newMember(1, "utility-target", 500, 500)
	target.Stats.Speed = 10
	target.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("wait"), Name: "等待", ElementID: target.ElementIDs[0], DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf, Accuracy: 100, RemainingPP: 10, MaxPP: 10, CuresUserMajorStatus: true}
	state, err := battleengine.NewState(battleengine.InitialState{Format: battleengine.FormatSnapshot{Code: "utility", ActiveSlotsPerSide: 1, TeamSize: 1}, Rules: battleengine.RuleSnapshot{SchemaVersion: 1}, Sides: []battleengine.SideSnapshot{{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{actor}}, {Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}}}})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}}, {Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}}}}, mustRandom(t, 763))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	finalActor, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	finalTarget, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	return heldItemUtilityDamageResult{actor: finalActor, target: finalTarget, events: result.Events}
}

// resolveHeldItemUtilityDefenseTurn 返回目标在指定分类普通伤害下的实际生命损失。
func resolveHeldItemUtilityDefenseTurn(t *testing.T, class battleengine.DamageClass, vest bool) uint32 {
	t.Helper()
	before := uint32(500)
	after := resolveHeldItemUtilityDefenseSnapshot(t, class, func(target *battleengine.MemberSnapshot) {
		if vest {
			target.ItemID = testID("assault-vest")
			target.HeldItemSpecialDefenseBoost = true
		}
	})
	return before - after.CurrentHP
}

// resolveHeldItemUtilityDefenseSnapshot 构造目标只执行无害自我回复的防守场景并返回最终快照。
func resolveHeldItemUtilityDefenseSnapshot(t *testing.T, class battleengine.DamageClass, configure func(*battleengine.MemberSnapshot)) battleengine.MemberSnapshot {
	member, _ := resolveHeldItemUtilityDefenseResult(t, class, configure)
	return member
}

// resolveHeldItemUtilityDefenseResult 构造防守场景，并同时返回最终目标快照与结构化事件流。
func resolveHeldItemUtilityDefenseResult(t *testing.T, class battleengine.DamageClass, configure func(*battleengine.MemberSnapshot)) (battleengine.MemberSnapshot, []battleengine.Event) {
	t.Helper()
	actor := newMember(1, "utility-defense-actor", 500, 500)
	actor.Stats.Speed = 200
	actor.Skills[0].DamageClass = class
	target := newMember(1, "utility-defense-target", 500, 500)
	target.Stats.Speed = 10
	target.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("wait"), Name: "等待", ElementID: target.ElementIDs[0], DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf, Accuracy: 100, RemainingPP: 10, MaxPP: 10, CuresUserMajorStatus: true}
	configure(&target)
	state, err := battleengine.NewState(battleengine.InitialState{Format: battleengine.FormatSnapshot{Code: "utility-defense", ActiveSlotsPerSide: 1, TeamSize: 1}, Rules: battleengine.RuleSnapshot{SchemaVersion: 1}, Sides: []battleengine.SideSnapshot{{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{actor}}, {Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}}}})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}}, {Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}}}}, mustRandom(t, 767))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	final, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	return final, result.Events
}

// healingSourceExists 报告事件流是否包含指定来源的技能回复事件。
func healingSourceExists(events []battleengine.Event, source battleengine.SkillHealingSource) bool {
	for _, event := range events {
		if value, ok := event.(battleengine.SkillHealingAppliedEvent); ok && value.Source == source {
			return true
		}
	}
	return false
}
