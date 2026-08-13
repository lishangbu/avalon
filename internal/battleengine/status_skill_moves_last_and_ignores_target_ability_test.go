package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnStatusSkillMovesLastAndIgnoresTargetAbility 验证变化技能后置特性只作用于变化技能。
//
// 即使持有成员速度更高，变化技能仍在同优先度的普通伤害技能之后行动；随后它能够穿透目标的变化技能免疫。
// 对照分支证明相同特性不会让物理技能获得对目标伤害免疫的穿透，避免把“无视特性”扩大为全技能开关。
func TestResolveTurnStatusSkillMovesLastAndIgnoresTargetAbility(t *testing.T) {
	t.Parallel()
	t.Run("变化技能后置且穿透目标特性", func(t *testing.T) {
		t.Parallel()
		result := resolveStatusSkillMovesLastAndIgnoresTargetAbilityTurn(t, battleengine.DamageClassStatus)
		if result.firstSkillActor != battleengine.SideTwo || result.target.StatStages[battleengine.StatAttack] != -1 || result.blocked {
			t.Fatalf("变化技能后置及特性穿透结算 = %+v", result)
		}
	})
	t.Run("伤害技能不获得特性穿透", func(t *testing.T) {
		t.Parallel()
		result := resolveStatusSkillMovesLastAndIgnoresTargetAbilityTurn(t, battleengine.DamageClassPhysical)
		if result.firstSkillActor != battleengine.SideOne || !result.blocked || result.target.CurrentHP != result.target.MaxHP {
			t.Fatalf("伤害技能不应获得变化技能特性效果 = %+v", result)
		}
	})
}

// statusSkillMovesLastAndIgnoresTargetAbilityResult 汇总排序、目标状态和阻止事实。
type statusSkillMovesLastAndIgnoresTargetAbilityResult struct {
	// firstSkillActor 是本回合第一个实际宣告技能的阵营，用于观察同优先度内的后置排序。
	firstSkillActor battleengine.Side
	// target 是被持有特性的成员选择的对手在回合结束后的权威快照。
	target battleengine.MemberSnapshot
	// blocked 表示对手特性阻止了持有者的技能，仅伤害技能对照分支应为 true。
	blocked bool
}

// resolveStatusSkillMovesLastAndIgnoresTargetAbilityTurn 构造单打同优先度场景并执行指定伤害类别的技能。
func resolveStatusSkillMovesLastAndIgnoresTargetAbilityTurn(
	t *testing.T,
	damageClass battleengine.DamageClass,
) statusSkillMovesLastAndIgnoresTargetAbilityResult {
	t.Helper()
	actor := newMember(1, "status-last-actor", 1_000, 1_000)
	actor.Stats.Speed = 300
	actor.StatusSkillMovesLastAndIgnoresTargetAbility = true
	actor.Skills[0].SkillID = testID("status-last-skill")
	actor.Skills[0].DamageClass = damageClass
	if damageClass == battleengine.DamageClassStatus {
		actor.Skills[0].Power = 0
		actor.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
			Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 100,
		}}
	}
	target := newMember(1, "status-last-target", 1_000, 1_000)
	target.Stats.Speed = 100
	target.OpponentStatusSkillImmunity = true
	target.NonSuperEffectiveDamageImmunity = true
	target.Skills[0].SkillID = testID("status-last-target-skill")

	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "status-skill-moves-last", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{actor}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	resolved, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			statusSkillMovesLastUseSkillAction(battleengine.SideOne, battleengine.SideTwo),
			statusSkillMovesLastUseSkillAction(battleengine.SideTwo, battleengine.SideOne),
		},
	}, mustRandom(t, 1_003))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	result := statusSkillMovesLastAndIgnoresTargetAbilityResult{}
	for _, event := range resolved.Events {
		if used, ok := event.(battleengine.SkillUsedEvent); ok && result.firstSkillActor == 0 {
			result.firstSkillActor = used.Actor.Side
		}
		if blocked, ok := event.(battleengine.SkillBlockedEvent); ok && blocked.SkillID == testID("status-last-skill") {
			result.blocked = true
		}
	}
	member, found := resolved.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("结算后未找到目标成员")
	}
	result.target = member
	return result
}

// statusSkillMovesLastUseSkillAction 创建测试双方都必须提交的单体技能行动。
func statusSkillMovesLastUseSkillAction(actorSide, targetSide battleengine.Side) battleengine.Action {
	return battleengine.Action{
		Kind:  battleengine.ActionKindUseSkill,
		Actor: battleengine.SlotRef{Side: actorSide, Position: 1},
		UseSkill: &battleengine.UseSkillAction{
			SkillPosition: 1,
			Target:        battleengine.SlotRef{Side: targetSide, Position: 1},
		},
	}
}
