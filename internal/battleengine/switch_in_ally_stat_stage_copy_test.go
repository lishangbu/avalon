package battleengine_test

import (
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInAllyStatStageCopy 验证初始双打上场时复制特性会覆盖来源的全部能力阶级。
//
// 初始建立不生成第 0 回合事件，但必须将同侧稳定槽位顺序中第一名其它存活上场成员的七项阶级写入权威快照。
func TestInitialStateAppliesSwitchInAllyStatStageCopy(t *testing.T) {
	t.Parallel()
	source := newMember(1, "switch-in-ally-stat-stage-copy-source", 1_000, 1_000)
	source.SwitchInAllyStatStageCopy = true
	ally := newMember(2, "switch-in-ally-stat-stage-copy-ally", 1_000, 1_000)
	ally.StatStages = copiedStatStages()
	opponentOne := newMember(1, "switch-in-ally-stat-stage-copy-opponent-one", 1_000, 1_000)
	opponentTwo := newMember(2, "switch-in-ally-stat-stage-copy-opponent-two", 1_000, 1_000)

	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-ally-stat-stage-copy-initial", ActiveSlotsPerSide: 2, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{source, ally}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{opponentOne, opponentTwo}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	updated, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || !reflect.DeepEqual(updated.StatStages, ally.StatStages) {
		t.Fatalf("初始入场能力阶级复制 = %+v, 队友 = %+v, found=%t", updated.StatStages, ally.StatStages, found)
	}
}

// TestResolveTurnSwitchInAllyStatStageCopyPublishesStatEvents 验证实际换入后每项覆盖都会产生可重放的能力阶级事件。
func TestResolveTurnSwitchInAllyStatStageCopyPublishesStatEvents(t *testing.T) {
	t.Parallel()
	first := newMember(1, "switch-in-ally-stat-stage-copy-first", 1_000, 1_000)
	ally := newMember(2, "switch-in-ally-stat-stage-copy-active-ally", 1_000, 1_000)
	ally.StatStages = copiedStatStages()
	incoming := newMember(3, "switch-in-ally-stat-stage-copy-incoming", 1_000, 1_000)
	incoming.SwitchInAllyStatStageCopy = true
	opponentOne := newMember(1, "switch-in-ally-stat-stage-copy-opponent-one", 1_000, 1_000)
	opponentTwo := newMember(2, "switch-in-ally-stat-stage-copy-opponent-two", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-ally-stat-stage-copy-switch", ActiveSlotsPerSide: 2, TeamSize: 3},
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
	}, mustRandom(t, 263))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	changes := 0
	for _, event := range result.Events {
		changed, ok := event.(battleengine.StatStageChangedEvent)
		if ok && changed.Actor == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 3}) && changed.Target == changed.Actor {
			changes++
		}
	}
	if changes != 7 {
		t.Fatalf("换入能力阶级复制事件数 = %d, events=%+v", changes, result.Events)
	}
	updated, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || updated.Position != 3 || !reflect.DeepEqual(updated.StatStages, ally.StatStages) {
		t.Fatalf("换入能力阶级复制 = %+v, 队友 = %+v, found=%t", updated, ally.StatStages, found)
	}
}

// TestSwitchInAllyStatStageCopySkipsAbsentAlly 验证没有其它存活上场队友时不会修改来源已有的能力阶级。
func TestSwitchInAllyStatStageCopySkipsAbsentAlly(t *testing.T) {
	t.Parallel()
	source := newMember(1, "switch-in-ally-stat-stage-copy-alone", 1_000, 1_000)
	source.SwitchInAllyStatStageCopy = true
	source.StatStages = map[battleengine.Stat]int8{battleengine.StatAttack: -2}
	opponent := newMember(1, "switch-in-ally-stat-stage-copy-alone-opponent", 1_000, 1_000)
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, source, opponent)
	updated, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || updated.StatStages[battleengine.StatAttack] != -2 {
		t.Fatalf("没有队友时入场能力阶级复制 = %+v, found=%t", updated.StatStages, found)
	}
}

// copiedStatStages 返回用于验证覆盖式复制的一组完整七项能力阶级。
func copiedStatStages() map[battleengine.Stat]int8 {
	return map[battleengine.Stat]int8{
		battleengine.StatAttack:         2,
		battleengine.StatDefense:        -1,
		battleengine.StatSpecialAttack:  3,
		battleengine.StatSpecialDefense: -2,
		battleengine.StatSpeed:          1,
		battleengine.StatAccuracy:       -3,
		battleengine.StatEvasion:        2,
	}
}
