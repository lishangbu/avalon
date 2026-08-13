package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnContactSkillProtectionBypass 验证接触技能保护穿透只跳过本次对手个人保护 gate。
//
// 三个分支分别固定接触标签、特性开关和保护生命周期：成功穿透后保护链仍然保留，非接触技能及没有特性的
// 接触技能仍会在命中和伤害随机数之前被阻止，避免把该特性扩大成一般性的防护破除。
func TestResolveTurnContactSkillProtectionBypass(t *testing.T) {
	t.Parallel()
	t.Run("接触技能穿透个人保护但不破除状态", func(t *testing.T) {
		t.Parallel()
		result := resolveContactSkillProtectionBypassTurn(t, true, true, false)
		if result.target.CurrentHP >= result.target.MaxHP || result.blocked != nil || result.target.ProtectionChain != 1 {
			t.Fatalf("接触技能穿透个人保护结果 = target:%+v, blocked:%+v", result.target, result.blocked)
		}
	})
	t.Run("非接触技能仍被个人保护阻止", func(t *testing.T) {
		t.Parallel()
		result := resolveContactSkillProtectionBypassTurn(t, false, true, false)
		assertContactSkillProtectionBlocked(t, result)
	})
	t.Run("没有特性的接触技能仍被个人保护阻止", func(t *testing.T) {
		t.Parallel()
		result := resolveContactSkillProtectionBypassTurn(t, true, false, false)
		assertContactSkillProtectionBlocked(t, result)
	})
	t.Run("接触抑制特性使静态接触技能仍被个人保护阻止", func(t *testing.T) {
		t.Parallel()
		result := resolveContactSkillProtectionBypassTurn(t, true, true, true)
		assertContactSkillProtectionBlocked(t, result)
	})
}

// contactSkillProtectionBypassResult 汇集测试需要观察的目标快照和接触技能对应的阻止事件。
type contactSkillProtectionBypassResult struct {
	// target 是个人保护建立并在同回合受到对手技能作用后的权威成员快照。
	target battleengine.MemberSnapshot
	// blocked 是接触技能被个人保护阻止时写入的事件；成功穿透时保持 nil。
	blocked *battleengine.SkillBlockedEvent
}

// resolveContactSkillProtectionBypassTurn 构造先建立个人保护、再由对手发动技能的固定单打场景。
func resolveContactSkillProtectionBypassTurn(
	t *testing.T,
	makesContact bool,
	contactSkillProtectionBypass bool,
	contactSuppression bool,
) contactSkillProtectionBypassResult {
	t.Helper()
	attacker := newMember(1, "contact-protection-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 100
	attacker.ContactSkillProtectionBypass = contactSkillProtectionBypass
	attacker.ContactSuppression = contactSuppression
	attacker.Skills[0].SkillID = testID("contact-protection-strike")
	attacker.Skills[0].Name = "接触测试攻击"
	attacker.Skills[0].DamageClass = battleengine.DamageClassPhysical
	attacker.Skills[0].TargetScope = battleengine.SkillTargetScopeSelectedTarget
	attacker.Skills[0].Power = 80
	attacker.Skills[0].Accuracy = 100
	attacker.Skills[0].RemainingPP = 10
	attacker.Skills[0].MaxPP = 10
	attacker.Skills[0].MakesContact = makesContact

	target := newMember(1, "contact-protection-target", 1_000, 1_000)
	target.Stats.Speed = 200
	target.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("contact-protection"), Name: "保护", ElementID: target.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10,
		VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
			Status: battleengine.VolatileStatusProtection, Target: battleengine.EffectTargetUser,
			ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
		}},
	}

	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "contact-skill-protection-bypass", ActiveSlotsPerSide: 1, TeamSize: 1},
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
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
		},
	}, mustRandom(t, 2_184))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}

	result := contactSkillProtectionBypassResult{}
	for _, event := range resolved.Events {
		value, ok := event.(battleengine.SkillBlockedEvent)
		if ok && value.SkillID == testID("contact-protection-strike") {
			copied := value
			result.blocked = &copied
		}
	}
	target, found := resolved.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("结算后未找到个人保护目标")
	}
	result.target = target
	return result
}

// assertContactSkillProtectionBlocked 断言不满足接触穿透条件的技能仍被个人保护阻止。
func assertContactSkillProtectionBlocked(t *testing.T, result contactSkillProtectionBypassResult) {
	t.Helper()
	if result.target.CurrentHP != result.target.MaxHP || result.blocked == nil ||
		result.blocked.Reason != battleengine.SkillBlockReasonProtection || result.target.ProtectionChain != 1 {
		t.Fatalf("个人保护阻止接触技能结果 = target:%+v, blocked:%+v", result.target, result.blocked)
	}
}
