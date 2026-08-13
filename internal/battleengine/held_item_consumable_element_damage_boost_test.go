package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemConsumableElementDamageBoost 验证匹配属性的一次性威力强化只在造成真实本体伤害后消费道具。
//
// 强化进入普通伤害公式的威力阶段，不能误用于属性不匹配的技能；替身仅承受替身生命伤害，不足以消费道具。
// 成功本体命中时，事件必须位于 DamageApplied 之后且 TurnEnded 之前，成员快照须清除整套道具运行态。
func TestHeldItemConsumableElementDamageBoost(t *testing.T) {
	t.Parallel()
	plain := resolveConsumableElementDamageBoostTurn(t, testID("fire-element"), false, false)
	matched := resolveConsumableElementDamageBoostTurn(t, testID("fire-element"), true, false)
	mismatched := resolveConsumableElementDamageBoostTurn(t, testID("water-element"), true, false)
	substitute := resolveConsumableElementDamageBoostTurn(t, testID("fire-element"), true, true)
	if matched.damage <= plain.damage {
		t.Fatalf("匹配属性的一次性强化伤害 = %d，期望高于未持有道具的 %d", matched.damage, plain.damage)
	}
	if mismatched.damage != plain.damage || mismatched.attacker.ItemID == 0 || len(mismatched.events) != 0 {
		t.Fatalf("属性不匹配时不应强化或消费道具 = result:%+v plain:%+v", mismatched, plain)
	}
	if substitute.damage != 0 || substitute.attacker.ItemID == 0 || len(substitute.events) != 0 {
		t.Fatalf("替身伤害不能消费一次性属性强化道具 = %+v", substitute)
	}
	if matched.attacker.ItemID != 0 || len(matched.events) != 1 || matched.events[0].ItemID != testID("consumable-element-boost-item") ||
		matched.events[0].ElementID != testID("fire-element") || !eventOccursBefore(matched.allEvents, battleengine.EventKindDamageApplied, battleengine.EventKindHeldItemElementDamageBoostConsumed) ||
		!eventOccursBefore(matched.allEvents, battleengine.EventKindHeldItemElementDamageBoostConsumed, battleengine.EventKindTurnEnded) {
		t.Fatalf("匹配属性的一次性强化消费结果 = %+v", matched)
	}
}

// consumableElementDamageBoostResult 汇总一次性属性强化规则断言所需的伤害、成员状态、事件和随机轨迹。
type consumableElementDamageBoostResult struct {
	// damage 是目标本体实际损失的生命值；替身路径固定为零。
	damage uint32
	// attacker 是完整回合结束后攻击方的冻结成员快照。
	attacker battleengine.MemberSnapshot
	// events 是本规则产生的道具消费事件。
	events []battleengine.HeldItemElementDamageBoostConsumedEvent
	// allEvents 是完整事件流，用于断言伤害、消费与回合结束的稳定顺序。
	allEvents []battleengine.Event
}

// resolveConsumableElementDamageBoostTurn 构造最小单打伤害回合，并提取一次性属性强化的可观察结果。
func resolveConsumableElementDamageBoostTurn(
	t *testing.T,
	skillElementID Identifier,
	holdsItem bool,
	targetHasSubstitute bool,
) consumableElementDamageBoostResult {
	t.Helper()
	attacker := newMember(1, "consumable-element-boost-attacker", 500, 500)
	attacker.Stats.Speed = 200
	if holdsItem {
		attacker.ItemID = testID("consumable-element-boost-item")
		attacker.HeldItemConsumableElementDamageBoostElementID = testID("fire-element")
		attacker.HeldItemConsumableElementDamageBoostNumerator = 6
		attacker.HeldItemConsumableElementDamageBoostDenominator = 5
	}
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("consumable-element-boost-strike"), Name: "一次性属性强化测试", ElementID: skillElementID,
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 100, Accuracy: 100, RemainingPP: 10, MaxPP: 10,
	}
	target := newMember(1, "consumable-element-boost-target", 500, 500)
	target.Stats.Speed = 10
	if targetHasSubstitute {
		// 初始快照不能伪造易变状态，因此让目标以更高优先度先合法建立替身，
		// 再由攻击方在同一回合命中它。
		target.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("consumable-element-boost-substitute"), Name: "替身",
			ElementID: testID("normal-element"), DamageClass: battleengine.DamageClassStatus,
			TargetScope: battleengine.SkillTargetScopeSelf, Priority: 1, Accuracy: 100, RemainingPP: 10, MaxPP: 10,
			VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
				Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
				ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
			}},
		}
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "consumable-element-boost", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{attacker}},
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
	}, mustRandom(t, 290))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	finalAttacker, ok := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !ok {
		t.Fatal("攻击方在回合结束后不在场")
	}
	finalTarget, ok := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !ok {
		t.Fatal("目标在回合结束后不在场")
	}
	var events []battleengine.HeldItemElementDamageBoostConsumedEvent
	for _, event := range result.Events {
		if consumed, ok := event.(battleengine.HeldItemElementDamageBoostConsumedEvent); ok {
			events = append(events, consumed)
		}
	}
	// 替身由目标在本回合先支付四分之一最大生命建立；该费用不是攻击方造成的本体伤害，
	// 因而须从建立替身后的生命基线计算，才能准确断言道具不会被替身承伤误消费。
	targetBodyHPBeforeAttack := uint32(500)
	if targetHasSubstitute {
		targetBodyHPBeforeAttack = 375
	}
	return consumableElementDamageBoostResult{
		damage:    targetBodyHPBeforeAttack - finalTarget.CurrentHP,
		attacker:  finalAttacker,
		events:    events,
		allEvents: result.Events,
	}
}
