package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnOpponentStatusSkillImmunity 验证目标特性只阻止对手变化技能，并可被攻击方无视目标特性规则绕过。
//
// 阻止发生在命中与附加效果前，因此不会消费命中随机数、不会改变目标能力阶级，但使用者已经宣告技能并正常
// 消耗 PP；这与保护造成的“已使用、未影响目标”生命周期保持一致。
func TestResolveTurnOpponentStatusSkillImmunity(t *testing.T) {
	t.Parallel()
	t.Run("阻止对手变化技能", func(t *testing.T) {
		t.Parallel()
		result := resolveOpponentStatusSkillImmunityTurn(t, false)
		if result.target.StatStages[battleengine.StatAttack] != 0 || result.blocked == nil ||
			result.blocked.Reason != battleengine.SkillBlockReasonOpponentStatusSkillImmunity {
			t.Fatalf("变化技能免疫结算 = target:%+v, blocked:%+v, events:%+v", result.target, result.blocked, result.events)
		}
	})
	t.Run("无视目标特性时绕过", func(t *testing.T) {
		t.Parallel()
		result := resolveOpponentStatusSkillImmunityTurn(t, true)
		if result.target.StatStages[battleengine.StatAttack] != -1 || result.blocked != nil {
			t.Fatalf("无视目标特性后的变化技能结算 = target:%+v, blocked:%+v, events:%+v", result.target, result.blocked, result.events)
		}
	})
}

// opponentStatusSkillImmunityResult 汇总变化技能免疫测试需观察的权威状态和阻止事件。
type opponentStatusSkillImmunityResult struct {
	// target 是结算后免疫目标的权威成员快照。
	target battleengine.MemberSnapshot
	// blocked 是攻击方变化技能被阻止时生成的事件；绕过时为 nil。
	blocked *battleengine.SkillBlockedEvent
	// events 保存完整事件流，便于断言失败时定位生命周期顺序。
	events []battleengine.Event
}

// resolveOpponentStatusSkillImmunityTurn 用固定的单打场景执行一次对手变化技能。
func resolveOpponentStatusSkillImmunityTurn(t *testing.T, attackerIgnoresTargetAbilities bool) opponentStatusSkillImmunityResult {
	t.Helper()
	attacker := newMember(1, "status-immunity-attacker", 100, 100)
	attacker.Stats.Speed = 110
	attacker.IgnoreTargetAbilityEffects = attackerIgnoresTargetAbilities
	attacker.Skills[0].SkillID = testID("status-immunity-skill")
	attacker.Skills[0].DamageClass = battleengine.DamageClassStatus
	attacker.Skills[0].Power = 0
	attacker.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 100,
	}}
	target := newMember(1, "status-immunity-target", 100, 100)
	target.Stats.Speed = 90
	target.OpponentStatusSkillImmunity = true
	target.Skills[0].SkillID = testID("status-immunity-pass")
	target.Skills[0].DamageClass = battleengine.DamageClassStatus
	target.Skills[0].Power = 0
	target.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatDefense, Target: battleengine.EffectTargetUser, StageDelta: 1, ChancePercent: 100,
	}}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "opponent-status-skill-immunity", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
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
	result := opponentStatusSkillImmunityResult{events: resolved.Events}
	for _, event := range resolved.Events {
		if value, ok := event.(battleengine.SkillBlockedEvent); ok && value.SkillID == testID("status-immunity-skill") {
			copied := value
			result.blocked = &copied
		}
	}
	member, found := resolved.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("结算后变化技能免疫目标不存在")
	}
	result.target = member
	return result
}
