package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnDamagingSkillSecondaryEffectImmunity 验证目标特性只阻止对手伤害技能的目标侧追加效果：本体伤害
// 仍会结算，但主要异常、能力下降、畏缩和易变状态不会写入，也不会消费这些必定效果之外的额外随机数。
func TestResolveTurnDamagingSkillSecondaryEffectImmunity(t *testing.T) {
	t.Parallel()
	result := resolveDamagingSkillSecondaryEffectImmunityTurn(t, false)
	target, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("未找到目标成员")
	}
	if target.CurrentHP >= target.MaxHP {
		t.Fatalf("追加效果免疫不应阻止本体伤害: %+v", target)
	}
	if target.MajorStatus != "" || target.StatStages[battleengine.StatAttack] != 0 || target.FlinchedTurn != 0 || target.TauntTurnsRemaining != 0 {
		t.Fatalf("追加效果免疫后的目标状态 = %+v", target)
	}
	if len(result.RandomTrace) != 2 || result.RandomTrace[0].Reason != "critical hit for "+testID("secondary-effect-skill").String() ||
		result.RandomTrace[1].Reason != "damage random for "+testID("secondary-effect-skill").String() {
		t.Fatalf("追加效果免疫不应产生额外随机轨迹: %+v", result.RandomTrace)
	}
}

// TestResolveTurnIgnoreTargetAbilityEffectsBypassesDamagingSkillSecondaryEffectImmunity 验证攻击方的无视目标特性
// 规则会使目标侧追加效果免疫失效，但不会额外改变伤害技能本体或原有的随机消费顺序。
func TestResolveTurnIgnoreTargetAbilityEffectsBypassesDamagingSkillSecondaryEffectImmunity(t *testing.T) {
	t.Parallel()
	result := resolveDamagingSkillSecondaryEffectImmunityTurn(t, true)
	target, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("未找到目标成员")
	}
	if target.MajorStatus != battleengine.MajorStatusPoison || target.StatStages[battleengine.StatAttack] != -1 || target.TauntTurnsRemaining == 0 {
		t.Fatalf("无视目标特性后的目标追加效果 = %+v", target)
	}
}

// TestResolveTurnHeldItemDamagingSkillSecondaryEffectImmunity 验证密探斗篷类道具阻止伤害技能的目标侧
// 追加效果但保留本体伤害；攻击方无视目标特性时仍不能绕过道具，且道具不会因此被消费。
func TestResolveTurnHeldItemDamagingSkillSecondaryEffectImmunity(t *testing.T) {
	t.Parallel()
	result := resolveDamagingSkillSecondaryEffectImmunityTurn(t, true, true)
	target, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || target.CurrentHP >= target.MaxHP {
		t.Fatalf("道具追加效果免疫后的目标 = %+v, found=%t", target, found)
	}
	if target.MajorStatus != "" || target.StatStages[battleengine.StatAttack] != 0 || target.FlinchedTurn != 0 || target.TauntTurnsRemaining != 0 {
		t.Fatalf("道具未阻止全部目标侧追加效果: %+v", target)
	}
	if target.ItemID != testID("covert-cloak") || !target.HeldItemDamagingSkillSecondaryEffectImmunity || len(result.RandomTrace) != 2 {
		t.Fatalf("道具生命周期或随机轨迹错误: target=%+v trace=%+v", target, result.RandomTrace)
	}
}

// resolveDamagingSkillSecondaryEffectImmunityTurn 构造一项同时携带主要异常、能力下降、畏缩与易变状态的物理技能。
// 使用明确随机轨迹隔离本体伤害，令两个测试只观察目标侧特性门控对追加效果的影响。
func resolveDamagingSkillSecondaryEffectImmunityTurn(t *testing.T, ignoreTargetAbilityEffects bool, heldItem ...bool) battleengine.TurnResult {
	t.Helper()
	attacker := newMember(1, "secondary-effect-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 200
	attacker.IgnoreTargetAbilityEffects = ignoreTargetAbilityEffects
	attacker.Skills[0].SkillID = testID("secondary-effect-skill")
	attacker.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{{
		Status: battleengine.MajorStatusPoison, Target: battleengine.EffectTargetSelected, ChancePercent: 100,
	}}
	attacker.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 100,
	}}
	attacker.Skills[0].FlinchChancePercent = 100
	attacker.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusTaunt, Target: battleengine.EffectTargetSelected, ChancePercent: 100, MinTurns: 2, MaxTurns: 2,
	}}
	target := newMember(1, "secondary-effect-target", 1_000, 1_000)
	target.Stats.Speed = 10
	if len(heldItem) != 0 && heldItem[0] {
		target.ItemID = testID("covert-cloak")
		target.HeldItemDamagingSkillSecondaryEffectImmunity = true
	} else {
		target.DamagingSkillSecondaryEffectImmunity = true
	}
	target.Skills[0].SkillID = testID("secondary-effect-pass")
	target.Skills[0].DamageClass = battleengine.DamageClassStatus
	target.Skills[0].Power = 0
	target.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 0,
	}}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "secondary-effect-immunity", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{attacker}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("secondary-effect-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("secondary-effect-skill").String(), Value: 15},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	return result
}
