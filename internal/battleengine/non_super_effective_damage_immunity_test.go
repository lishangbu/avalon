package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnNonSuperEffectiveDamageImmunity 验证目标特性只阻止相性不克制的对手伤害技能。
//
// 判定在命中和伤害随机数前完成，因而被阻止时目标生命不变且没有 DamageAppliedEvent；克制伤害和攻击方
// 无视目标特性都必须继续进入普通伤害流程。
func TestResolveTurnNonSuperEffectiveDamageImmunity(t *testing.T) {
	t.Parallel()
	t.Run("不克制伤害被阻止", func(t *testing.T) {
		t.Parallel()
		result := resolveNonSuperEffectiveDamageImmunityTurn(t, testID("water"), false)
		if result.target.CurrentHP != result.target.MaxHP || result.blocked == nil ||
			result.blocked.Reason != battleengine.SkillBlockReasonNonSuperEffectiveDamageImmunity || result.damageApplied {
			t.Fatalf("不克制伤害免疫结算 = target:%+v, blocked:%+v, damageApplied:%t, events:%+v", result.target, result.blocked, result.damageApplied, result.events)
		}
	})
	t.Run("克制伤害正常结算", func(t *testing.T) {
		t.Parallel()
		result := resolveNonSuperEffectiveDamageImmunityTurn(t, testID("grass"), false)
		if result.target.CurrentHP >= result.target.MaxHP || result.blocked != nil || !result.damageApplied {
			t.Fatalf("克制伤害不应被免疫: target:%+v, blocked:%+v, damageApplied:%t, events:%+v", result.target, result.blocked, result.damageApplied, result.events)
		}
	})
	t.Run("无视目标特性时绕过", func(t *testing.T) {
		t.Parallel()
		result := resolveNonSuperEffectiveDamageImmunityTurn(t, testID("water"), true)
		if result.target.CurrentHP >= result.target.MaxHP || result.blocked != nil || !result.damageApplied {
			t.Fatalf("无视目标特性后的不克制伤害 = target:%+v, blocked:%+v, damageApplied:%t, events:%+v", result.target, result.blocked, result.damageApplied, result.events)
		}
	})
}

// nonSuperEffectiveDamageImmunityResult 汇总测试需观察的目标状态与技能阻止事实。
type nonSuperEffectiveDamageImmunityResult struct {
	// target 是结算后防守成员的权威快照。
	target battleengine.MemberSnapshot
	// blocked 是本段伤害被特性阻止时的事件；正常伤害时为 nil。
	blocked *battleengine.SkillBlockedEvent
	// damageApplied 表示攻击技能是否已经写入目标本体生命。
	damageApplied bool
	// events 保存完整事件流，以便断言失败时说明事件先后关系。
	events []battleengine.Event
}

// resolveNonSuperEffectiveDamageImmunityTurn 按给定目标属性执行火属性物理技能。
func resolveNonSuperEffectiveDamageImmunityTurn(
	t *testing.T,
	targetElementID Identifier,
	attackerIgnoresTargetAbilities bool,
) nonSuperEffectiveDamageImmunityResult {
	t.Helper()
	attacker := newMember(1, "non-super-effective-attacker", 100, 100)
	attacker.Stats.Speed = 110
	attacker.IgnoreTargetAbilityEffects = attackerIgnoresTargetAbilities
	attacker.Skills[0].SkillID = testID("non-super-effective-fire-skill")
	attacker.Skills[0].ElementID = testID("fire")
	attacker.Skills[0].Power = 80
	target := newMember(1, "non-super-effective-target", 100, 100)
	target.Stats.Speed = 90
	target.ElementIDs = testIDs(targetElementID)
	target.NonSuperEffectiveDamageImmunity = true
	target.Skills[0].SkillID = testID("non-super-effective-pass")
	target.Skills[0].DamageClass = battleengine.DamageClassStatus
	target.Skills[0].Power = 0
	target.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatDefense, Target: battleengine.EffectTargetUser, StageDelta: 1, ChancePercent: 100,
	}}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "non-super-effective-damage-immunity", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules: battleengine.RuleSnapshot{
			SchemaVersion: 1,
			ElementEffectiveness: []battleengine.ElementEffectiveness{
				{AttackElementID: testID("fire"), DefenseElementID: testID("water"), Numerator: 1, Denominator: 2},
				{AttackElementID: testID("fire"), DefenseElementID: testID("grass"), Numerator: 2, Denominator: 1},
			},
		},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{attacker}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	resolved, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}, mustRandom(t, 284))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	result := nonSuperEffectiveDamageImmunityResult{events: resolved.Events}
	for _, event := range resolved.Events {
		switch value := event.(type) {
		case battleengine.SkillBlockedEvent:
			if value.SkillID == testID("non-super-effective-fire-skill") {
				copied := value
				result.blocked = &copied
			}
		case battleengine.DamageAppliedEvent:
			if value.SkillID == testID("non-super-effective-fire-skill") {
				result.damageApplied = true
			}
		}
	}
	member, found := resolved.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("结算后非克制伤害免疫目标不存在")
	}
	result.target = member
	return result
}
