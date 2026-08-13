package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemPunchBasedSkillPowerBoost 验证拳击类技能仅在持有道具时于普通直接伤害的威力阶段获得固定 10% 强化。
// 测试同时固定非拳击和失去道具的回退，避免把道具错误扩大到其它标签、固定伤害或持续持有者之外的场景。
func TestHeldItemPunchBasedSkillPowerBoost(t *testing.T) {
	t.Parallel()
	plainDamage := resolvePunchPowerBoostDamage(t, true, false)
	boostedDamage := resolvePunchPowerBoostDamage(t, true, true)
	nonPunchDamage := resolvePunchPowerBoostDamage(t, false, true)
	if boostedDamage <= plainDamage {
		t.Fatalf("拳击类技能伤害 = %d，期望高于未持有道具时的 %d", boostedDamage, plainDamage)
	}
	if nonPunchDamage != plainDamage {
		t.Fatalf("非拳击技能伤害 = %d，期望回退到未强化的 %d", nonPunchDamage, plainDamage)
	}
}

// resolvePunchPowerBoostDamage 构造最小单打直接伤害场景，并返回受击者本体实际损失的生命值。
func resolvePunchPowerBoostDamage(t *testing.T, punchBased, holdsBoostItem bool) uint32 {
	t.Helper()
	attacker := newMember(1, "punch-power-attacker", 500, 500)
	attacker.Stats.Speed = 200
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("punch-power-strike"), Name: "拳击威力测试", ElementID: attacker.ElementIDs[0],
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 100, Accuracy: 100, RemainingPP: 10, MaxPP: 10, PunchBased: punchBased,
	}
	if holdsBoostItem {
		attacker.ItemID = testID("punch-power-item")
		attacker.HeldItemPunchBasedSkillPowerBoost = true
	}
	target := newMember(1, "punch-power-target", 500, 500)
	target.Stats.Speed = 10
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "punch-power-boost", ActiveSlotsPerSide: 1, TeamSize: 1},
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
	}, mustRandom(t, 560))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("找不到受击成员")
	}
	return 500 - member.CurrentHP
}
