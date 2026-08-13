package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInAllyStatStageReset 验证初始双打上场时重置特性只清除同侧队友的能力阶级。
func TestInitialStateAppliesSwitchInAllyStatStageReset(t *testing.T) {
	t.Parallel()
	source := newMember(1, "switch-in-ally-stat-stage-reset-source", 1_000, 1_000)
	source.SwitchInAllyStatStageReset = true
	ally := newMember(2, "switch-in-ally-stat-stage-reset-ally", 1_000, 1_000)
	ally.StatStages = copiedStatStages()
	opponentOne := newMember(1, "switch-in-ally-stat-stage-reset-opponent-one", 1_000, 1_000)
	opponentOne.StatStages = copiedStatStages()
	opponentTwo := newMember(2, "switch-in-ally-stat-stage-reset-opponent-two", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-ally-stat-stage-reset-initial", ActiveSlotsPerSide: 2, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{source, ally}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{opponentOne, opponentTwo}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	updatedAlly, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 2})
	if !found || anyNonZeroStatStage(updatedAlly.StatStages) {
		t.Fatalf("初始入场能力阶级重置后的队友 = %+v, found=%t", updatedAlly.StatStages, found)
	}
	updatedOpponent, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || !anyNonZeroStatStage(updatedOpponent.StatStages) {
		t.Fatalf("初始入场能力阶级重置不应影响对手: stages=%+v, found=%t", updatedOpponent.StatStages, found)
	}
}

// TestResolveTurnSwitchInAllyStatStageResetPublishesStatEvents 验证实际换入后重置的每项能力阶级都有独立事件。
func TestResolveTurnSwitchInAllyStatStageResetPublishesStatEvents(t *testing.T) {
	t.Parallel()
	first := newMember(1, "switch-in-ally-stat-stage-reset-first", 1_000, 1_000)
	ally := newMember(2, "switch-in-ally-stat-stage-reset-active-ally", 1_000, 1_000)
	ally.StatStages = copiedStatStages()
	incoming := newMember(3, "switch-in-ally-stat-stage-reset-incoming", 1_000, 1_000)
	incoming.SwitchInAllyStatStageReset = true
	opponentOne := newMember(1, "switch-in-ally-stat-stage-reset-opponent-one", 1_000, 1_000)
	opponentTwo := newMember(2, "switch-in-ally-stat-stage-reset-opponent-two", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-ally-stat-stage-reset-switch", ActiveSlotsPerSide: 2, TeamSize: 3},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{first, ally, incoming}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{opponentOne, opponentTwo}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 3}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 2}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}, mustRandom(t, 269))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	changes := 0
	for _, event := range result.Events {
		changed, ok := event.(battleengine.StatStageChangedEvent)
		if ok && changed.Actor == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 3}) &&
			changed.Target == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) && changed.CurrentStage == 0 {
			changes++
		}
	}
	if changes != 7 {
		t.Fatalf("换入能力阶级重置事件数 = %d, events=%+v", changes, result.Events)
	}
	updated, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 2})
	if !found || anyNonZeroStatStage(updated.StatStages) {
		t.Fatalf("换入能力阶级重置后的队友 = %+v, found=%t", updated.StatStages, found)
	}
}

// anyNonZeroStatStage 判断七项能力阶级中是否仍有未被重置的值。
func anyNonZeroStatStage(stages map[battleengine.Stat]int8) bool {
	for _, stat := range []battleengine.Stat{
		battleengine.StatAttack, battleengine.StatDefense, battleengine.StatSpecialAttack, battleengine.StatSpecialDefense,
		battleengine.StatSpeed, battleengine.StatAccuracy, battleengine.StatEvasion,
	} {
		if stages[stat] != 0 {
			return true
		}
	}
	return false
}
