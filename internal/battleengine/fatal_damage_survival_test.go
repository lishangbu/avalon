package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnSurviveFatalDamageAtFullHP 验证满生命致命伤害保留特性只在直接敌方技能伤害写入前生效。
//
// 此规则不消费随机数，也不会改变伤害公式本身：DamageAppliedEvent 仍记录实际扣除的生命，紧随其后的
// FatalDamageSurvivedEvent 则保留原始伤害、阻止量和特性来源，避免重放从剩余 1 HP 反向猜测保命原因。
func TestResolveTurnSurviveFatalDamageAtFullHP(t *testing.T) {
	t.Parallel()
	t.Run("满生命时保留一生命", func(t *testing.T) {
		t.Parallel()
		result := resolveFatalDamageSurvivalTurn(t, 100, false, fatalDamageSurvivalSourceAbility)
		if result.target.CurrentHP != 1 {
			t.Fatalf("满生命保命后的当前生命 = %d，期望 1", result.target.CurrentHP)
		}
		if result.fainted {
			t.Fatalf("满生命保命不应产生倒下事件: %+v", result.events)
		}
		if result.survived == nil || result.survived.SourceAbilityID != testID("fatal-survival-ability") ||
			result.survived.CurrentHP != 1 || result.survived.IncomingDamage <= result.damage.Amount ||
			result.survived.PreventedDamage != result.survived.IncomingDamage-result.damage.Amount {
			t.Fatalf("满生命保命事件 = %+v，伤害事件 = %+v", result.survived, result.damage)
		}
	})
	t.Run("非满生命不触发", func(t *testing.T) {
		t.Parallel()
		result := resolveFatalDamageSurvivalTurn(t, 99, false, fatalDamageSurvivalSourceAbility)
		if result.target.CurrentHP != 0 || !result.fainted || result.survived != nil {
			t.Fatalf("非满生命不应保命: target=%+v, survived=%+v, events=%+v", result.target, result.survived, result.events)
		}
	})
	t.Run("无视目标特性时不触发", func(t *testing.T) {
		t.Parallel()
		result := resolveFatalDamageSurvivalTurn(t, 100, true, fatalDamageSurvivalSourceAbility)
		if result.target.CurrentHP != 0 || !result.fainted || result.survived != nil {
			t.Fatalf("无视目标特性后不应保命: target=%+v, survived=%+v, events=%+v", result.target, result.survived, result.events)
		}
	})
}

// TestResolveTurnHeldItemSurviveFatalDamageAtFullHP 验证一次性道具保命与特性保命使用不同的来源和消费语义。
// 道具规则不属于目标特性，攻击方无视目标特性时仍会生效；触发后必须原子清空道具运行态，避免再次保命。
func TestResolveTurnHeldItemSurviveFatalDamageAtFullHP(t *testing.T) {
	t.Parallel()
	t.Run("满生命时消费道具并保留一生命", func(t *testing.T) {
		t.Parallel()
		result := resolveFatalDamageSurvivalTurn(t, 100, false, fatalDamageSurvivalSourceItem)
		if result.target.CurrentHP != 1 || result.target.ItemID != 0 || result.target.HeldItemSurviveFatalDamageAtFullHP || result.fainted {
			t.Fatalf("道具保命后的成员状态 = %+v, events=%+v", result.target, result.events)
		}
		if result.survived == nil || result.survived.SourceAbilityID != 0 || result.survived.SourceItemID != testID("fatal-survival-item") ||
			result.survived.CurrentHP != 1 || result.survived.IncomingDamage <= result.damage.Amount ||
			result.survived.PreventedDamage != result.survived.IncomingDamage-result.damage.Amount {
			t.Fatalf("道具保命事件 = %+v，伤害事件 = %+v", result.survived, result.damage)
		}
	})
	t.Run("非满生命不触发也不消费道具", func(t *testing.T) {
		t.Parallel()
		result := resolveFatalDamageSurvivalTurn(t, 99, false, fatalDamageSurvivalSourceItem)
		if result.target.CurrentHP != 0 || result.target.ItemID != testID("fatal-survival-item") || !result.target.HeldItemSurviveFatalDamageAtFullHP || !result.fainted || result.survived != nil {
			t.Fatalf("非满生命道具保命结果 = target:%+v, survived=%+v, events=%+v", result.target, result.survived, result.events)
		}
	})
	t.Run("无视目标特性不会绕过道具", func(t *testing.T) {
		t.Parallel()
		result := resolveFatalDamageSurvivalTurn(t, 100, true, fatalDamageSurvivalSourceItem)
		if result.target.CurrentHP != 1 || result.target.ItemID != 0 || result.fainted || result.survived == nil || result.survived.SourceItemID != testID("fatal-survival-item") {
			t.Fatalf("无视目标特性时的道具保命结果 = target:%+v, survived=%+v, events=%+v", result.target, result.survived, result.events)
		}
	})
	t.Run("特性优先时不消费道具", func(t *testing.T) {
		t.Parallel()
		result := resolveFatalDamageSurvivalTurn(t, 100, false, fatalDamageSurvivalSourceAbilityAndItem)
		if result.target.CurrentHP != 1 || result.target.ItemID != testID("fatal-survival-item") || !result.target.HeldItemSurviveFatalDamageAtFullHP || result.survived == nil || result.survived.SourceAbilityID != testID("fatal-survival-ability") || result.survived.SourceItemID != 0 {
			t.Fatalf("特性优先时的保命结果 = target:%+v, survived=%+v, events=%+v", result.target, result.survived, result.events)
		}
	})
}

// fatalDamageSurvivalSource 表示测试场景中提供满生命保命规则的来源组合。
type fatalDamageSurvivalSource uint8

const (
	// fatalDamageSurvivalSourceAbility 表示仅由防守成员特性提供保命规则。
	fatalDamageSurvivalSourceAbility fatalDamageSurvivalSource = iota + 1
	// fatalDamageSurvivalSourceItem 表示仅由防守成员持有道具提供一次性保命规则。
	fatalDamageSurvivalSourceItem
	// fatalDamageSurvivalSourceAbilityAndItem 表示两种来源同时存在，用于验证特性优先级。
	fatalDamageSurvivalSourceAbilityAndItem
)

// fatalDamageSurvivalResult 汇总测试需观察的最小结算事实，避免让断言依赖事件在完整回合中的绝对下标。
type fatalDamageSurvivalResult struct {
	// target 是结算后防守成员的权威快照。
	target battleengine.MemberSnapshot
	// damage 是攻击技能写入本体的实际伤害事件。
	damage battleengine.DamageAppliedEvent
	// survived 是满生命保命事件；未触发时为 nil。
	survived *battleengine.FatalDamageSurvivedEvent
	// fainted 表示防守成员是否因这段技能伤害产生倒下事件。
	fainted bool
	// events 保留完整事件流，以便失败信息说明额外事件来源。
	events []battleengine.Event
}

// resolveFatalDamageSurvivalTurn 执行固定随机轨迹下的一段必定致命普通伤害。
func resolveFatalDamageSurvivalTurn(
	t *testing.T,
	targetHP uint32,
	attackerIgnoresTargetAbilities bool,
	source fatalDamageSurvivalSource,
) fatalDamageSurvivalResult {
	t.Helper()
	attacker := newMember(1, "fatal-survival-attacker", 100, 100)
	attacker.Stats.Speed = 110
	attacker.Skills[0].SkillID = testID("fatal-survival-skill")
	attacker.Skills[0].Power = 250
	attacker.IgnoreTargetAbilityEffects = attackerIgnoresTargetAbilities
	target := newMember(1, "fatal-survival-target", 100, targetHP)
	target.Stats.Speed = 90
	if source == fatalDamageSurvivalSourceAbility || source == fatalDamageSurvivalSourceAbilityAndItem {
		target.AbilityID = testID("fatal-survival-ability")
		target.SurviveFatalDamageAtFullHP = true
	}
	if source == fatalDamageSurvivalSourceItem || source == fatalDamageSurvivalSourceAbilityAndItem {
		target.ItemID = testID("fatal-survival-item")
		target.HeldItemSurviveFatalDamageAtFullHP = true
	}
	// 防守成员的后续行动为无伤害变化技能，确保它仍可在成功保命后行动，但不额外消费随机数。
	target.Skills[0].SkillID = testID("fatal-survival-pass")
	target.Skills[0].DamageClass = battleengine.DamageClassStatus
	target.Skills[0].Power = 0
	target.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetUser, StageDelta: 1, ChancePercent: 100,
	}}

	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "fatal-damage-survival", ActiveSlotsPerSide: 1, TeamSize: 1},
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
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("fatal-survival-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("fatal-survival-skill").String(), Value: 15},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	resolved, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
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
	result := fatalDamageSurvivalResult{events: resolved.Events}
	for _, event := range resolved.Events {
		switch value := event.(type) {
		case battleengine.DamageAppliedEvent:
			if value.SkillID == testID("fatal-survival-skill") {
				result.damage = value
			}
		case battleengine.FatalDamageSurvivedEvent:
			copied := value
			result.survived = &copied
		case battleengine.ParticipantFaintedEvent:
			if value.Target.Side == battleengine.SideTwo && value.Target.Position == 1 {
				result.fainted = true
			}
		}
	}
	if result.damage.SkillID == 0 {
		t.Fatalf("攻击技能伤害事件缺失: %+v", resolved.Events)
	}
	resolvedTarget, found := resolved.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("结算后目标成员不存在")
	}
	result.target = resolvedTarget
	return result
}
