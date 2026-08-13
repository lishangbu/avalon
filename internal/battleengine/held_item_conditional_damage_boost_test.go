package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemSuperEffectiveDamageBoost 验证达人带只强化最终属性相性严格大于 1 的普通直接伤害。
func TestHeldItemSuperEffectiveDamageBoost(t *testing.T) {
	t.Parallel()
	plain := resolveConditionalDamageBoostTurn(t, false, false, true)
	boosted := resolveConditionalDamageBoostTurn(t, true, false, true)
	neutral := resolveConditionalDamageBoostTurn(t, true, false, false)
	neutralPlain := resolveConditionalDamageBoostTurn(t, false, false, false)
	if boosted.targetDamage <= plain.targetDamage || neutral.targetDamage != neutralPlain.targetDamage {
		t.Fatalf("达人带伤害错误: boosted=%d plain=%d neutral=%d neutralPlain=%d", boosted.targetDamage, plain.targetDamage, neutral.targetDamage, neutralPlain.targetDamage)
	}
	if boosted.actor.ItemID != testID("conditional-boost-item") || len(boosted.recoilEvents) != 0 ||
		!eventOccursBefore(boosted.events, battleengine.EventKindDamageApplied, battleengine.EventKindTurnEnded) {
		t.Fatalf("达人带状态或事件错误: actor=%+v events=%v", boosted.actor, eventKinds(boosted.events))
	}
}

// TestHeldItemDamageBoostWithRecoil 验证生命宝珠强化普通直接伤害后按最大生命十分之一反伤且不消费。
func TestHeldItemDamageBoostWithRecoil(t *testing.T) {
	t.Parallel()
	plain := resolveConditionalDamageBoostTurn(t, false, false, false)
	boosted := resolveConditionalDamageBoostTurn(t, false, true, false)
	if boosted.targetDamage <= plain.targetDamage || boosted.actor.ItemID != testID("conditional-boost-item") {
		t.Fatalf("生命宝珠结算错误: boosted=%+v plain=%+v", boosted, plain)
	}
	if len(boosted.recoilEvents) != 1 || boosted.recoilEvents[0].Amount != 50 || boosted.recoilEvents[0].CurrentHP != 450 ||
		!eventOccursBefore(boosted.events, battleengine.EventKindDamageApplied, battleengine.EventKindHeldItemRecoilDamageApplied) ||
		!eventOccursBefore(boosted.events, battleengine.EventKindHeldItemRecoilDamageApplied, battleengine.EventKindTurnEnded) {
		t.Fatalf("生命宝珠反伤事件错误: %+v events=%v", boosted.recoilEvents, eventKinds(boosted.events))
	}
	if len(boosted.randomTrace) != len(plain.randomTrace) {
		t.Fatalf("生命宝珠不应改变随机消费: boosted=%+v plain=%+v", boosted.randomTrace, plain.randomTrace)
	}
}

// conditionalDamageBoostResult 保存条件伤害强化测试的权威结果。
type conditionalDamageBoostResult struct {
	targetDamage uint32
	actor        battleengine.MemberSnapshot
	events       []battleengine.Event
	recoilEvents []battleengine.HeldItemRecoilDamageAppliedEvent
	randomTrace  []battleengine.RandomTraceEntry
}

// resolveConditionalDamageBoostTurn 构造可切换严格克制关系的最小普通直接伤害回合。
func resolveConditionalDamageBoostTurn(t *testing.T, superEffectiveBoost, boostWithRecoil, superEffective bool) conditionalDamageBoostResult {
	t.Helper()
	actor := newMember(1, "conditional-boost-actor", 500, 500)
	actor.ElementIDs = testIDs("actor-element")
	actor.Stats.Speed = 200
	actor.ItemID = testID("conditional-boost-item")
	actor.HeldItemSuperEffectiveDamageBoost = superEffectiveBoost
	actor.HeldItemDamageBoostWithRecoil = boostWithRecoil
	actor.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("conditional-boost-skill"), Name: "条件强化测试", ElementID: testID("attack-element"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 100, Accuracy: 100, RemainingPP: 10, MaxPP: 10,
	}
	target := newMember(1, "conditional-boost-target", 500, 500)
	target.ElementIDs = testIDs("target-element")
	target.Stats.Speed = 10
	effectiveness := []battleengine.ElementEffectiveness(nil)
	if superEffective {
		effectiveness = []battleengine.ElementEffectiveness{{AttackElementID: testID("attack-element"), DefenseElementID: testID("target-element"), Numerator: 2, Denominator: 1}}
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "held-item-conditional-damage-boost", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1, ElementEffectiveness: effectiveness},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{actor}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}, mustRandom(t, 751))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	finalActor, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	finalTarget, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	recoil := make([]battleengine.HeldItemRecoilDamageAppliedEvent, 0, 1)
	for _, event := range result.Events {
		if value, ok := event.(battleengine.HeldItemRecoilDamageAppliedEvent); ok {
			recoil = append(recoil, value)
		}
	}
	return conditionalDamageBoostResult{targetDamage: 500 - finalTarget.CurrentHP, actor: finalActor, events: result.Events, recoilEvents: recoil, randomTrace: result.RandomTrace}
}
