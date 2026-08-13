package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemPowderSkillImmunity 验证粉末免疫道具只阻止带 PowderBased 标签的对手技能。
//
// 阻止发生在命中骰和主要异常写入前：使用者的 PP 已消耗，目标没有异常，且事件明确记录道具防护原因；
// 非粉末技能不能被该道具扩大为通用技能免疫。
func TestHeldItemPowderSkillImmunity(t *testing.T) {
	t.Parallel()
	t.Run("粉末技能在命中前被持有道具阻止", func(t *testing.T) {
		t.Parallel()
		result := resolveHeldItemPowderSkillImmunityTurn(t, true, true)
		if result.target.MajorStatus != "" || result.blocked == nil ||
			result.blocked.Reason != battleengine.SkillBlockReasonPowderSkillImmunity || result.attacker.Skills[0].RemainingPP != 9 {
			t.Fatalf("粉末技能道具免疫结算 = attacker:%+v target:%+v blocked:%+v events:%+v", result.attacker, result.target, result.blocked, result.events)
		}
	})
	t.Run("非粉末技能不受持有道具阻止", func(t *testing.T) {
		t.Parallel()
		result := resolveHeldItemPowderSkillImmunityTurn(t, false, true)
		if result.target.MajorStatus != battleengine.MajorStatusSleep || result.blocked != nil || result.attacker.Skills[0].RemainingPP != 9 {
			t.Fatalf("非粉末技能道具免疫边界 = attacker:%+v target:%+v blocked:%+v events:%+v", result.attacker, result.target, result.blocked, result.events)
		}
	})
	t.Run("失去道具后粉末技能恢复生效", func(t *testing.T) {
		t.Parallel()
		result := resolveHeldItemPowderSkillImmunityTurn(t, true, false)
		if result.target.MajorStatus != battleengine.MajorStatusSleep || result.blocked != nil {
			t.Fatalf("失去粉末免疫道具后的结算 = target:%+v blocked:%+v events:%+v", result.target, result.blocked, result.events)
		}
	})
}

// heldItemPowderSkillImmunityResult 汇集粉末免疫边界的状态快照与可审计阻止事件。
type heldItemPowderSkillImmunityResult struct {
	// attacker 是结算后技能使用者的权威快照，用于检查 PP 生命周期。
	attacker battleengine.MemberSnapshot
	// target 是结算后道具持有者的权威快照，用于检查主要异常没有越过阻止 gate。
	target battleengine.MemberSnapshot
	// blocked 是被粉末免疫阻止时的结构化事件；技能成功时为 nil。
	blocked *battleengine.SkillBlockedEvent
	// events 保存完整事件流，便于失败时检查阻止相对技能使用的顺序。
	events []battleengine.Event
}

// resolveHeldItemPowderSkillImmunityTurn 构造一项带睡眠后效的变化粉末技能并完成一次单打回合。
func resolveHeldItemPowderSkillImmunityTurn(
	t *testing.T,
	powderBased bool,
	holdsImmunityItem bool,
) heldItemPowderSkillImmunityResult {
	t.Helper()
	attacker := newMember(1, "powder-immunity-attacker", 100, 100)
	attacker.Stats.Speed = 100
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("powder-sleep"), Name: "粉末睡眠", ElementID: attacker.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, PowderBased: powderBased,
		StatusApplications: []battleengine.MajorStatusApplication{{
			Status: battleengine.MajorStatusSleep, Target: battleengine.EffectTargetSelected, ChancePercent: 100,
		}},
	}
	target := newMember(1, "powder-immunity-target", 100, 100)
	// 让目标先行动，避免未免疫的睡眠状态使其随后“等待”行动变为不可用，从而把断言聚焦于粉末免疫本身。
	target.Stats.Speed = 300
	if holdsImmunityItem {
		target.ItemID = testID("powder-immunity-item")
		target.HeldItemPowderSkillImmunity = true
	}
	target.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("powder-immunity-pass"), Name: "等待", ElementID: target.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		// 最小回复使等待动作在命令校验中属于可执行技能，满生命时不会影响本规则的状态断言。
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, HealingPercent: 1,
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "held-item-powder-skill-immunity", ActiveSlotsPerSide: 1, TeamSize: 1},
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
	}, mustRandom(t, 2_189))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	result := heldItemPowderSkillImmunityResult{events: resolved.Events}
	for _, event := range resolved.Events {
		if value, ok := event.(battleengine.SkillBlockedEvent); ok && value.SkillID == testID("powder-sleep") {
			copied := value
			result.blocked = &copied
		}
	}
	result.attacker, _ = resolved.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	result.target, _ = resolved.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	return result
}
