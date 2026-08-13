package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInClearAllSideDamageReductions 验证初始入场会清除双方三种屏障，但保留顺风和入场危害。
func TestInitialStateAppliesSwitchInClearAllSideDamageReductions(t *testing.T) {
	t.Parallel()
	source := newMember(1, "switch-in-clear-side-reductions-source", 1_000, 1_000)
	source.SwitchInClearAllSideDamageReductions = true
	opponent := newMember(1, "switch-in-clear-side-reductions-opponent", 1_000, 1_000)
	conditions := battleengine.SideConditionSnapshot{
		Reflect: &battleengine.ReflectEffect{TurnsRemaining: 2}, LightScreen: &battleengine.LightScreenEffect{TurnsRemaining: 2},
		AuroraVeil: &battleengine.AuroraVeilEffect{TurnsRemaining: 2}, Tailwind: &battleengine.TailwindEffect{TurnsRemaining: 2},
		SpikesLayers: 2,
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-clear-side-reductions-initial", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source}, Conditions: conditions},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}, Conditions: conditions},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	for _, side := range state.Snapshot().Sides {
		if side.Conditions.Reflect != nil || side.Conditions.LightScreen != nil || side.Conditions.AuroraVeil != nil {
			t.Fatalf("初始入场未清除阵营屏障: %+v", side.Conditions)
		}
		if side.Conditions.Tailwind == nil || side.Conditions.SpikesLayers != 2 {
			t.Fatalf("初始入场错误清除非屏障侧状态: %+v", side.Conditions)
		}
	}
}

// TestResolveTurnSwitchInClearAllSideDamageReductionsPublishesSideEvents 验证实际换入会为每个发生清除的阵营发布独立事件。
func TestResolveTurnSwitchInClearAllSideDamageReductionsPublishesSideEvents(t *testing.T) {
	t.Parallel()
	first := newMember(1, "switch-in-clear-side-reductions-first", 1_000, 1_000)
	incoming := newMember(2, "switch-in-clear-side-reductions-incoming", 1_000, 1_000)
	incoming.SwitchInClearAllSideDamageReductions = true
	opponent := newMember(1, "switch-in-clear-side-reductions-opponent", 1_000, 1_000)
	conditions := battleengine.SideConditionSnapshot{
		Reflect: &battleengine.ReflectEffect{TurnsRemaining: 2}, LightScreen: &battleengine.LightScreenEffect{TurnsRemaining: 2},
		AuroraVeil: &battleengine.AuroraVeilEffect{TurnsRemaining: 2}, Tailwind: &battleengine.TailwindEffect{TurnsRemaining: 2},
		SpikesLayers: 2,
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-clear-side-reductions-switch", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first, incoming}, Conditions: conditions},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}, Conditions: conditions},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}, mustRandom(t, 271))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	cleared := make(map[battleengine.Side]bool, 2)
	for _, event := range result.Events {
		removed, ok := event.(battleengine.AbilitySideDamageReductionsClearedEvent)
		if ok && removed.Actor == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) &&
			removed.ClearedReflect && removed.ClearedLightScreen && removed.ClearedAuroraVeil {
			cleared[removed.Side] = true
		}
	}
	if len(cleared) != 2 || !cleared[battleengine.SideOne] || !cleared[battleengine.SideTwo] {
		t.Fatalf("换入清除阵营屏障事件不完整: %+v", result.Events)
	}
	for _, side := range result.State.Snapshot().Sides {
		if side.Conditions.Reflect != nil || side.Conditions.LightScreen != nil || side.Conditions.AuroraVeil != nil {
			t.Fatalf("换入后仍存在阵营屏障: %+v", side.Conditions)
		}
		if side.Conditions.Tailwind == nil || side.Conditions.SpikesLayers != 2 {
			t.Fatalf("换入错误清除非屏障侧状态: %+v", side.Conditions)
		}
	}
}
