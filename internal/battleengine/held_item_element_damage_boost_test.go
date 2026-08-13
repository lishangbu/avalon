package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemElementDamageBoosts 验证十八种传统属性强化携带道具都只强化匹配属性的普通直接伤害。
//
// 每个子测试分别代表一条独立规则；共同断言固定 6/5 威力倍率、道具不消费、成员属性不改变、伤害事件先于回合
// 结束以及冻结的道具属性身份仍保留在最终状态。资料 Identifier 到 HeldItemElementID 的 Battle 冻结、道具转移和变身
// 还原由既有通用链路测试覆盖，本测试不按道具名称复制十套运行时分支。
func TestHeldItemElementDamageBoosts(t *testing.T) {
	t.Parallel()
	rules := []struct {
		name      string
		elementID Identifier
		itemID    Identifier
	}{
		{name: "一般属性", elementID: testID("normal-element"), itemID: testID("silk-scarf")},
		{name: "火属性", elementID: testID("fire-element"), itemID: testID("charcoal")},
		{name: "水属性", elementID: testID("water-element"), itemID: testID("mystic-water")},
		{name: "电属性", elementID: testID("electric-element"), itemID: testID("magnet")},
		{name: "草属性", elementID: testID("grass-element"), itemID: testID("miracle-seed")},
		{name: "冰属性", elementID: testID("ice-element"), itemID: testID("never-melt-ice")},
		{name: "格斗属性", elementID: testID("fighting-element"), itemID: testID("black-belt")},
		{name: "毒属性", elementID: testID("poison-element"), itemID: testID("poison-barb")},
		{name: "地面属性", elementID: testID("ground-element"), itemID: testID("soft-sand")},
		{name: "飞行属性", elementID: testID("flying-element"), itemID: testID("sharp-beak")},
		{name: "超能力属性", elementID: testID("psychic-element"), itemID: testID("twisted-spoon")},
		{name: "虫属性", elementID: testID("bug-element"), itemID: testID("silver-powder")},
		{name: "岩石属性", elementID: testID("rock-element"), itemID: testID("hard-stone")},
		{name: "幽灵属性", elementID: testID("ghost-element"), itemID: testID("spell-tag")},
		{name: "龙属性", elementID: testID("dragon-element"), itemID: testID("dragon-fang")},
		{name: "恶属性", elementID: testID("dark-element"), itemID: testID("black-glasses")},
		{name: "钢属性", elementID: testID("steel-element"), itemID: testID("metal-coat")},
		{name: "妖精属性", elementID: testID("fairy-element"), itemID: testID("fairy-feather")},
	}
	for _, rule := range rules {
		rule := rule
		t.Run(rule.name, func(t *testing.T) {
			t.Parallel()
			plain := resolveHeldItemElementDamageBoostTurn(t, rule.elementID, rule.itemID, 0, false)
			boosted := resolveHeldItemElementDamageBoostTurn(t, rule.elementID, rule.itemID, rule.elementID, true)
			mismatched := resolveHeldItemElementDamageBoostTurn(t, rule.elementID, rule.itemID, testID("other-element"), true)

			if boosted.damage <= plain.damage {
				t.Fatalf("匹配属性伤害 = %d，期望高于无道具基线 %d", boosted.damage, plain.damage)
			}
			if mismatched.damage != plain.damage {
				t.Fatalf("不匹配属性伤害 = %d，期望等于基线 %d", mismatched.damage, plain.damage)
			}
			if boosted.attacker.ItemID != rule.itemID || boosted.attacker.HeldItemElementID != rule.elementID ||
				len(boosted.attacker.ElementIDs) != 1 || boosted.attacker.ElementIDs[0] != testID("attacker-natural-element") {
				t.Fatalf("强化后攻击者快照 = %+v，期望道具、冻结身份和自然属性均保持", boosted.attacker)
			}
			if !eventOccursBefore(boosted.events, battleengine.EventKindDamageApplied, battleengine.EventKindTurnEnded) {
				t.Fatalf("强化伤害事件顺序错误: %v", eventKinds(boosted.events))
			}
			if len(boosted.randomTrace) != len(plain.randomTrace) {
				t.Fatalf("稳定道具不应改变随机消费: boosted=%+v plain=%+v", boosted.randomTrace, plain.randomTrace)
			}
		})
	}
}

// TestHeldItemDamageClassPowerBoosts 验证力量头带和博识眼镜只强化各自声明的普通直接伤害分类。
//
// 两条规则均不消费道具、不产生额外事件或随机轨迹；测试同时固定相反分类边界与最终状态快照，避免一个资料
// 开关被错误扩张为全伤害强化。
func TestHeldItemDamageClassPowerBoosts(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name        string
		itemID      Identifier
		damageClass battleengine.DamageClass
		physical    bool
		special     bool
	}{
		{name: "物理威力提高 10%", itemID: testID("muscle-band"), damageClass: battleengine.DamageClassPhysical, physical: true},
		{name: "特殊威力提高 10%", itemID: testID("wise-glasses"), damageClass: battleengine.DamageClassSpecial, special: true},
	}
	for _, test := range tests {
		test := test
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			plain := resolveHeldItemDamageClassPowerBoostTurn(t, test.damageClass, test.itemID, false, false)
			boosted := resolveHeldItemDamageClassPowerBoostTurn(t, test.damageClass, test.itemID, test.physical, test.special)
			oppositeClass := battleengine.DamageClassPhysical
			if test.damageClass == battleengine.DamageClassPhysical {
				oppositeClass = battleengine.DamageClassSpecial
			}
			opposite := resolveHeldItemDamageClassPowerBoostTurn(t, oppositeClass, test.itemID, test.physical, test.special)
			oppositePlain := resolveHeldItemDamageClassPowerBoostTurn(t, oppositeClass, test.itemID, false, false)
			if boosted.damage <= plain.damage {
				t.Fatalf("匹配分类伤害 = %d，期望高于基线 %d", boosted.damage, plain.damage)
			}
			if opposite.damage != oppositePlain.damage {
				t.Fatalf("相反分类伤害 = %d，期望等于基线 %d", opposite.damage, oppositePlain.damage)
			}
			if boosted.attacker.ItemID != test.itemID || boosted.attacker.HeldItemPhysicalDamagePowerBoost != test.physical ||
				boosted.attacker.HeldItemSpecialDamagePowerBoost != test.special {
				t.Fatalf("最终攻击者快照 = %+v，期望保留道具及冻结分类开关", boosted.attacker)
			}
			if !eventOccursBefore(boosted.events, battleengine.EventKindDamageApplied, battleengine.EventKindTurnEnded) ||
				len(boosted.randomTrace) != len(plain.randomTrace) {
				t.Fatalf("事件或随机轨迹错误: events=%v trace=%+v", eventKinds(boosted.events), boosted.randomTrace)
			}
		})
	}
}

// resolveHeldItemDamageClassPowerBoostTurn 构造指定伤害分类的最小普通伤害回合。
func resolveHeldItemDamageClassPowerBoostTurn(
	t *testing.T,
	damageClass battleengine.DamageClass,
	itemID Identifier,
	physical, special bool,
) heldItemElementDamageBoostResult {
	t.Helper()
	attacker := newMember(1, "damage-class-boost-attacker", 500, 500)
	attacker.ElementIDs = testIDs("attacker-natural-element")
	attacker.Stats.Speed = 200
	attacker.ItemID = itemID
	attacker.HeldItemPhysicalDamagePowerBoost = physical
	attacker.HeldItemSpecialDamagePowerBoost = special
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("damage-class-boost-skill"), Name: "分类强化测试", ElementID: testID("skill-element"),
		DamageClass: damageClass, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 100, Accuracy: 100, RemainingPP: 10, MaxPP: 10,
	}
	target := newMember(1, "damage-class-boost-target", 500, 500)
	target.Stats.Speed = 10
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "held-item-damage-class-power-boost", ActiveSlotsPerSide: 1, TeamSize: 1},
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
	}, mustRandom(t, 731))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	resolvedTarget, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("找不到受击成员")
	}
	resolvedAttacker, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found {
		t.Fatal("找不到攻击成员")
	}
	return heldItemElementDamageBoostResult{
		damage: 500 - resolvedTarget.CurrentHP, attacker: resolvedAttacker,
		events: result.Events, randomTrace: result.RandomTrace,
	}
}

// heldItemElementDamageBoostResult 保存一次属性强化规则测试需要比较的权威结果。
type heldItemElementDamageBoostResult struct {
	damage      uint32
	attacker    battleengine.MemberSnapshot
	events      []battleengine.Event
	randomTrace []battleengine.RandomTraceEntry
}

// resolveHeldItemElementDamageBoostTurn 构造无本系加成、无属性相性和无其它威力倍率的最小普通伤害回合。
func resolveHeldItemElementDamageBoostTurn(
	t *testing.T,
	skillElementID, itemID, itemElementID Identifier,
	holdsItem bool,
) heldItemElementDamageBoostResult {
	t.Helper()
	attacker := newMember(1, "element-boost-attacker-"+skillElementID.String(), 500, 500)
	attacker.ElementIDs = testIDs("attacker-natural-element")
	attacker.Stats.Speed = 200
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("element-boost-skill-" + skillElementID.String()), Name: "属性强化测试",
		ElementID: skillElementID, DamageClass: battleengine.DamageClassPhysical,
		TargetScope: battleengine.SkillTargetScopeSelectedTarget, Power: 100, Accuracy: 100, RemainingPP: 10, MaxPP: 10,
	}
	if holdsItem {
		attacker.ItemID = itemID
		attacker.HeldItemElementID = itemElementID
	}
	target := newMember(1, "element-boost-target-"+skillElementID.String(), 500, 500)
	target.Stats.Speed = 10
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "held-item-element-damage-boost", ActiveSlotsPerSide: 1, TeamSize: 1},
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
	}, mustRandom(t, 721))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	resolvedTarget, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("找不到受击成员")
	}
	resolvedAttacker, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found {
		t.Fatal("找不到攻击成员")
	}
	return heldItemElementDamageBoostResult{
		damage: 500 - resolvedTarget.CurrentHP, attacker: resolvedAttacker,
		events: result.Events, randomTrace: result.RandomTrace,
	}
}
