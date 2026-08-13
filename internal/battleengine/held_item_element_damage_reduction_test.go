package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemElementDamageReductions 验证十种一次性抗性道具按冻结属性与克制条件减半本体伤害并消费。
//
// 每个子测试代表一条规则，同时覆盖属性匹配、属性不匹配、严格克制、一般属性例外、替身不触发、
// 消费事件顺序、随机轨迹和最终状态快照。运行时只读取 Identifier 与显式条件，不按树果名称解释效果。
func TestHeldItemElementDamageReductions(t *testing.T) {
	t.Parallel()
	rules := []struct {
		name                   string
		elementID              Identifier
		itemID                 Identifier
		requiresSuperEffective bool
	}{
		{name: "一般属性", elementID: testID("normal-element"), itemID: testID("chilan-berry")},
		{name: "火属性", elementID: testID("fire-element"), itemID: testID("occa-berry"), requiresSuperEffective: true},
		{name: "水属性", elementID: testID("water-element"), itemID: testID("passho-berry"), requiresSuperEffective: true},
		{name: "电属性", elementID: testID("electric-element"), itemID: testID("wacan-berry"), requiresSuperEffective: true},
		{name: "草属性", elementID: testID("grass-element"), itemID: testID("rindo-berry"), requiresSuperEffective: true},
		{name: "冰属性", elementID: testID("ice-element"), itemID: testID("yache-berry"), requiresSuperEffective: true},
		{name: "格斗属性", elementID: testID("fighting-element"), itemID: testID("chople-berry"), requiresSuperEffective: true},
		{name: "毒属性", elementID: testID("poison-element"), itemID: testID("kebia-berry"), requiresSuperEffective: true},
		{name: "地面属性", elementID: testID("ground-element"), itemID: testID("shuca-berry"), requiresSuperEffective: true},
		{name: "飞行属性", elementID: testID("flying-element"), itemID: testID("coba-berry"), requiresSuperEffective: true},
		{name: "超能力属性", elementID: testID("psychic-element"), itemID: testID("payapa-berry"), requiresSuperEffective: true},
		{name: "虫属性", elementID: testID("bug-element"), itemID: testID("tanga-berry"), requiresSuperEffective: true},
		{name: "岩石属性", elementID: testID("rock-element"), itemID: testID("charti-berry"), requiresSuperEffective: true},
		{name: "幽灵属性", elementID: testID("ghost-element"), itemID: testID("kasib-berry"), requiresSuperEffective: true},
		{name: "龙属性", elementID: testID("dragon-element"), itemID: testID("haban-berry"), requiresSuperEffective: true},
		{name: "恶属性", elementID: testID("dark-element"), itemID: testID("colbur-berry"), requiresSuperEffective: true},
		{name: "钢属性", elementID: testID("steel-element"), itemID: testID("babiri-berry"), requiresSuperEffective: true},
		{name: "妖精属性", elementID: testID("fairy-element"), itemID: testID("roseli-berry"), requiresSuperEffective: true},
	}
	for _, rule := range rules {
		rule := rule
		t.Run(rule.name, func(t *testing.T) {
			t.Parallel()
			plain := resolveHeldItemElementDamageReductionTurn(t, rule.elementID, 0, 0, false, true, false)
			reduced := resolveHeldItemElementDamageReductionTurn(t, rule.elementID, rule.itemID, rule.elementID, rule.requiresSuperEffective, true, false)
			mismatched := resolveHeldItemElementDamageReductionTurn(t, rule.elementID, rule.itemID, testID("other-element"), rule.requiresSuperEffective, true, false)
			substitute := resolveHeldItemElementDamageReductionTurn(t, rule.elementID, rule.itemID, rule.elementID, rule.requiresSuperEffective, true, true)

			if reduced.damage == 0 || reduced.damage >= plain.damage {
				t.Fatalf("抗性伤害 = %d，期望低于无道具基线 %d", reduced.damage, plain.damage)
			}
			if reduced.target.ItemID != 0 || len(reduced.consumed) != 1 || reduced.consumed[0].ItemID != rule.itemID ||
				reduced.consumed[0].ElementID != rule.elementID {
				t.Fatalf("抗性道具消费结果错误: target=%+v events=%+v", reduced.target, reduced.consumed)
			}
			if mismatched.damage != plain.damage || mismatched.target.ItemID != rule.itemID || len(mismatched.consumed) != 0 {
				t.Fatalf("属性不匹配仍触发抗性: mismatch=%+v plain=%+v", mismatched, plain)
			}
			if rule.requiresSuperEffective {
				neutral := resolveHeldItemElementDamageReductionTurn(t, rule.elementID, rule.itemID, rule.elementID, true, false, false)
				neutralPlain := resolveHeldItemElementDamageReductionTurn(t, rule.elementID, 0, 0, false, false, false)
				if neutral.damage != neutralPlain.damage || neutral.target.ItemID != rule.itemID || len(neutral.consumed) != 0 {
					t.Fatalf("非克制伤害错误触发抗性: neutral=%+v plain=%+v", neutral, neutralPlain)
				}
			}
			if substitute.target.ItemID != rule.itemID || len(substitute.consumed) != 0 || substitute.target.SubstituteHP >= 100 {
				t.Fatalf("替身承伤错误触发抗性消费: %+v", substitute)
			}
			if !eventOccursBefore(reduced.events, battleengine.EventKindDamageApplied, battleengine.EventKindHeldItemElementDamageReductionConsumed) ||
				!eventOccursBefore(reduced.events, battleengine.EventKindHeldItemElementDamageReductionConsumed, battleengine.EventKindTurnEnded) ||
				len(reduced.randomTrace) != len(plain.randomTrace) {
				t.Fatalf("抗性事件或随机轨迹错误: events=%v trace=%+v", eventKinds(reduced.events), reduced.randomTrace)
			}
		})
	}
}

// heldItemElementDamageReductionResult 保存一次抗性道具结算的权威状态、事件和随机轨迹。
type heldItemElementDamageReductionResult struct {
	damage      uint32
	target      battleengine.MemberSnapshot
	events      []battleengine.Event
	consumed    []battleengine.HeldItemElementDamageReductionConsumedEvent
	randomTrace []battleengine.RandomTraceEntry
}

// resolveHeldItemElementDamageReductionTurn 构造克制倍率为 2 倍的单体普通伤害；一般属性道具显式关闭严格克制要求。
func resolveHeldItemElementDamageReductionTurn(
	t *testing.T,
	skillElementID, itemID, reductionElementID Identifier,
	requiresSuperEffective, superEffective, substitute bool,
) heldItemElementDamageReductionResult {
	t.Helper()
	attacker := newMember(1, "element-reduction-attacker", 500, 500)
	attacker.ElementIDs = testIDs("attacker-element")
	attacker.Stats.Speed = 200
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("element-reduction-skill"), Name: "抗性测试", ElementID: skillElementID,
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 100, Accuracy: 100, RemainingPP: 10, MaxPP: 10,
	}
	target := newMember(1, "element-reduction-target", 500, 500)
	target.ElementIDs = testIDs("target-element")
	target.Stats.Speed = 10
	target.ItemID = itemID
	target.HeldItemElementDamageReductionElementID = reductionElementID
	target.HeldItemElementDamageReductionRequiresSuperEffective = requiresSuperEffective
	if substitute {
		// 初始快照不能伪造易变状态；目标通过更高优先度技能在攻击前合法建立替身。
		target.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("element-reduction-substitute"), Name: "替身", ElementID: testID("normal-element"),
			DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
			Priority: 1, Accuracy: 100, RemainingPP: 10, MaxPP: 10,
			VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
				Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
				ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
			}},
		}
	}
	effectiveness := []battleengine.ElementEffectiveness(nil)
	if superEffective {
		effectiveness = []battleengine.ElementEffectiveness{{
			AttackElementID: skillElementID, DefenseElementID: testID("target-element"), Numerator: 2, Denominator: 1,
		}}
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "held-item-element-damage-reduction", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1, ElementEffectiveness: effectiveness},
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
	}, mustRandom(t, 741))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	resolved, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("找不到受击成员")
	}
	consumed := make([]battleengine.HeldItemElementDamageReductionConsumedEvent, 0, 1)
	for _, event := range result.Events {
		if value, ok := event.(battleengine.HeldItemElementDamageReductionConsumedEvent); ok {
			consumed = append(consumed, value)
		}
	}
	return heldItemElementDamageReductionResult{
		damage: 500 - resolved.CurrentHP, target: resolved, events: result.Events, consumed: consumed, randomTrace: result.RandomTrace,
	}
}
