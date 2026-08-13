package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateDetectsDangerousOpponentSkill 验证入场侦测优先公开稳定排序中的危险对手技能。
func TestInitialStateDetectsDangerousOpponentSkill(t *testing.T) {
	t.Parallel()
	receiver := newMember(1, "danger-receiver", 1_000, 1_000)
	receiver.SwitchInDetectDangerousOpponentSkill = true
	receiver.ElementIDs = testIDs("grass")
	opponent := newMember(1, "danger-opponent", 1_000, 1_000)
	opponent.Skills[0].SkillID = testID("00000000-0000-0000-0000-000000000200")
	opponent.Skills[0].ElementID = testID("fire")
	opponent.Skills[0].Power = 90
	opponent.Skills = append(opponent.Skills, battleengine.SkillSnapshot{TargetScope: battleengine.SkillTargetScopeSelectedTarget, MinHits: 1, MaxHits: 1, Position: 2, SkillID: testID("00000000-0000-0000-0000-000000000100"), Name: "一击必杀",
		ElementID: testID("normal"), DamageClass: battleengine.DamageClassPhysical, DamageMode: battleengine.SkillDamageModeOneHitKnockOut,
		OneHitKnockOutBaseAccuracy: 30, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
	})
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-danger-initial", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules: battleengine.RuleSnapshot{
			SchemaVersion:        1,
			ElementEffectiveness: []battleengine.ElementEffectiveness{{AttackElementID: testID("fire"), DefenseElementID: testID("grass"), Numerator: 2, Denominator: 1}},
		},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{receiver}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	events := state.InitialEvents()
	if len(events) != 1 {
		t.Fatalf("初始危险技能侦测事件数量 = %d, events=%+v", len(events), events)
	}
	detected, ok := events[0].(battleengine.DangerousOpponentSkillDetectedEvent)
	expectedSkillID := min(opponent.Skills[0].SkillID, opponent.Skills[1].SkillID)
	if !ok || detected.Actor != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}) ||
		detected.Target != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) || detected.SkillID != expectedSkillID {
		t.Fatalf("危险技能侦测事件 = %+v", events[0])
	}
}

// TestInitialStateDisguisesAsLastHealthyAlly 验证视觉身份只取同侧最后一名可战斗成员。
func TestInitialStateDisguisesAsLastHealthyAlly(t *testing.T) {
	t.Parallel()
	receiver := newMember(1, "disguise-receiver", 1_000, 1_000)
	receiver.SwitchInDisguiseAsLastHealthyAlly = true
	firstAlly := newMember(2, "disguise-first-ally", 1_000, 1_000)
	lastHealthyAlly := newMember(3, "disguise-last-healthy-ally", 1_000, 1_000)
	opponent := newMember(1, "disguise-opponent", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-disguise-initial", ActiveSlotsPerSide: 1, TeamSize: 3},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{receiver, firstAlly, lastHealthyAlly}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	member, ok := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !ok || member.CreatureID != receiver.CreatureID || member.ApparentCreatureID != lastHealthyAlly.CreatureID {
		t.Fatalf("伪装成员画像 = %+v", member)
	}
	if summary := state.Summary(); summary.Members[0].ApparentCreatureID != lastHealthyAlly.CreatureID {
		t.Fatalf("状态摘要未披露伪装种类: %+v", summary)
	}
}
