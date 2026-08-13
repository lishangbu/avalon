package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnAppliesDamageBasedSkillHealthEffects 验证资料中的正向 drain 会按实际伤害吸取，
// 负向 drain 会按同一实际伤害产生反作用；两者都使用结构化事件而不是隐式修改生命值。
func TestResolveTurnAppliesDamageBasedSkillHealthEffects(t *testing.T) {
	t.Parallel()
	tests := []struct {
		// name 是当前技能生命后效的稳定测试名称。
		name string
		// drainPercent 是冻结技能中的有符号伤害后效百分比。
		drainPercent int8
		// initialHP 是使用者执行技能前的生命值。
		initialHP uint32
		// expectedHP 是伤害和后效写入后的使用者生命值。
		expectedHP uint32
		// expectedKind 是本例必须观察到的结构化生命事件种类。
		expectedKind battleengine.EventKind
		// skillRecoilDamageImmunity 表示使用者是否携带只阻止伤害回算反作用的特性规则。
		skillRecoilDamageImmunity bool
		// expectsHealthEvent 表示本例是否应产生对应的生命后效事件。
		expectsHealthEvent bool
	}{
		{name: "吸取", drainPercent: 50, initialHP: 50, expectedHP: 64, expectedKind: battleengine.EventKindSkillHealingApplied, expectsHealthEvent: true},
		{name: "反作用", drainPercent: -50, initialHP: 100, expectedHP: 86, expectedKind: battleengine.EventKindSkillRecoilDamageApplied, expectsHealthEvent: true},
		{name: "反作用免疫", drainPercent: -50, initialHP: 100, expectedHP: 100, expectedKind: battleengine.EventKindSkillRecoilDamageApplied, skillRecoilDamageImmunity: true},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			left := newMember(1, "hp-effect-user", 100, test.initialHP)
			left.Stats.Speed = 110
			left.Skills[0].SkillID = testID("hp-effect-skill")
			left.Skills[0].DrainPercent = test.drainPercent
			left.SkillRecoilDamageImmunity = test.skillRecoilDamageImmunity
			right := newMember(1, "hp-effect-target", 200, 200)
			right.Stats.Speed = 90
			right.Skills[0].DamageClass = battleengine.DamageClassStatus
			right.Skills[0].Power = 0
			right.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
				Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 0,
			}}
			state, err := battleengine.NewState(battleengine.InitialState{
				Format: battleengine.FormatSnapshot{Code: "hp-effect-single", ActiveSlotsPerSide: 1, TeamSize: 1},
				Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
				Sides: []battleengine.SideSnapshot{
					{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
					{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
				},
			})
			if err != nil {
				t.Fatalf("NewState() error = %v", err)
			}
			random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
				{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("hp-effect-skill").String(), Value: 1},
				{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("hp-effect-skill").String(), Value: 15},
			})
			if err != nil {
				t.Fatalf("NewTracedRandom() error = %v", err)
			}
			result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
				SchemaVersion: 1, TurnNumber: 1,
				Actions: []battleengine.Action{
					hpEffectAction(battleengine.SideOne, battleengine.SideTwo),
					hpEffectAction(battleengine.SideTwo, battleengine.SideOne),
				},
			}, random)
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			actor, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
			if !exists || actor.CurrentHP != test.expectedHP {
				t.Fatalf("使用者生命值 = %d，期望 %d", actor.CurrentHP, test.expectedHP)
			}
			if got := hpEffectContainsEvent(result.Events, test.expectedKind); got != test.expectsHealthEvent {
				t.Fatalf("事件类型 = %v，%s 出现 = %t，期望 %t", hpEffectEventKinds(result.Events), test.expectedKind, got, test.expectsHealthEvent)
			}
		})
	}
}

// TestResolveTurnSkillRecoilDamageImmunityDoesNotBlockFixedSelfCost 验证技能反作用免疫不扩大为所有自身伤害
// 免疫。按最大生命支付的固定技能代价没有依赖目标实际损失生命，因此必须继续结算。
func TestResolveTurnSkillRecoilDamageImmunityDoesNotBlockFixedSelfCost(t *testing.T) {
	t.Parallel()
	left := newMember(1, "fixed-self-cost-user", 100, 100)
	left.Stats.Speed = 110
	left.SkillRecoilDamageImmunity = true
	left.Skills[0].SkillID = testID("fixed-self-cost-skill")
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].TargetScope = battleengine.SkillTargetScopeSelf
	left.Skills[0].HealingPercent = -50
	right := newMember(1, "fixed-self-cost-target", 100, 100)
	right.Stats.Speed = 90
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 0,
	}}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "fixed-self-cost-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{}}},
			hpEffectAction(battleengine.SideTwo, battleengine.SideOne),
		},
	}, mustRandom(t, 82))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	actor, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !exists || actor.CurrentHP != 50 {
		t.Fatalf("固定技能代价后的使用者生命值 = %d，期望 50", actor.CurrentHP)
	}
	if !hpEffectContainsEvent(result.Events, battleengine.EventKindSkillRecoilDamageApplied) {
		t.Fatalf("事件类型 = %v，缺少固定技能代价事件", hpEffectEventKinds(result.Events))
	}
}

// TestResolveTurnAppliesFixedSkillHealing 验证不造成普通伤害的自身技能也可以通过 healing 百分比
// 成功回复，并且自身范围不依赖客户端提交的对手目标。
func TestResolveTurnAppliesFixedSkillHealing(t *testing.T) {
	t.Parallel()
	left := newMember(1, "fixed-heal-user", 100, 50)
	left.Stats.Speed = 110
	left.Skills[0].SkillID = testID("fixed-heal-skill")
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].TargetScope = battleengine.SkillTargetScopeSelf
	left.Skills[0].HealingPercent = 50
	right := newMember(1, "fixed-heal-target", 100, 100)
	right.Stats.Speed = 90
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 0,
	}}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "fixed-heal-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewTracedRandom(nil)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{
				Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{}},
			},
			hpEffectAction(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	actor, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !exists || actor.CurrentHP != 100 {
		t.Fatalf("使用者生命值 = %d，期望 100", actor.CurrentHP)
	}
	for _, event := range result.Events {
		healing, isHealing := event.(battleengine.SkillHealingAppliedEvent)
		if isHealing && healing.SkillID == testID("fixed-heal-skill") && healing.Source == battleengine.SkillHealingSourceFixed && healing.Amount == 50 {
			return
		}
	}
	t.Fatalf("事件类型 = %v，缺少固定比例回复", hpEffectEventKinds(result.Events))
}

// TestResolveTurnCuresUsersOwnMajorStatus 验证状态技能成功后只会清除使用者自己的主要异常状态。
//
// 该测试以 ResolveTurn 作为公开边界：它同时约束技能资料编译后的冻结字段、运行态写入与结构化重放事件，
// 而不依赖任何内部状态清理辅助函数。
func TestResolveTurnCuresUsersOwnMajorStatus(t *testing.T) {
	t.Parallel()
	left := newMember(1, "self-status-cure-user", 100, 100)
	left.Stats.Speed = 110
	left.MajorStatus = battleengine.MajorStatusPoison
	left.Skills[0].SkillID = testID("self-status-cure-skill")
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].TargetScope = battleengine.SkillTargetScopeSelf
	left.Skills[0].CuresUserMajorStatus = true
	right := newMember(1, "self-status-cure-target", 100, 100)
	right.Stats.Speed = 90
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 0,
	}}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "self-status-cure-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewTracedRandom(nil)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{}}},
			hpEffectAction(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	actor, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !exists || actor.MajorStatus != "" {
		t.Fatalf("使用者主要异常 = %q，期望已清除", actor.MajorStatus)
	}
	for _, event := range result.Events {
		cleared, isCleared := event.(battleengine.MajorStatusClearedEvent)
		if isCleared && cleared.Target == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}) &&
			cleared.Status == battleengine.MajorStatusPoison {
			return
		}
	}
	t.Fatalf("事件类型 = %v，缺少使用者主要异常清除事件", hpEffectEventKinds(result.Events))
}

// hpEffectAction 构造单打测试中的一个普通目标技能行动。
func hpEffectAction(actorSide, targetSide battleengine.Side) battleengine.Action {
	return battleengine.Action{
		Kind:  battleengine.ActionKindUseSkill,
		Actor: battleengine.SlotRef{Side: actorSide, Position: 1},
		UseSkill: &battleengine.UseSkillAction{
			SkillPosition: 1, Target: battleengine.SlotRef{Side: targetSide, Position: 1},
		},
	}
}

// hpEffectContainsEvent 报告事件流是否包含指定的稳定生命后效种类。
func hpEffectContainsEvent(events []battleengine.Event, expected battleengine.EventKind) bool {
	for _, event := range events {
		if event.Kind() == expected {
			return true
		}
	}
	return false
}

// hpEffectEventKinds 返回事件流种类，供失败信息直观定位生命后效顺序。
func hpEffectEventKinds(events []battleengine.Event) []battleengine.EventKind {
	result := make([]battleengine.EventKind, len(events))
	for index, event := range events {
		result[index] = event.Kind()
	}
	return result
}
