package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInStatStageChange 验证初始上场成员的自身能力阶级特性会直接写入权威快照。
//
// 初始建立不向第 0 回合事件流补写事件；能力阶级变化仍使用和技能一致的 -6 至 6 状态边界，但没有技能
// 概率或目标选择语义。
func TestInitialStateAppliesSwitchInStatStageChange(t *testing.T) {
	t.Parallel()
	setter := newMember(1, "switch-in-attack-boost", 1_000, 1_000)
	setter.SwitchInStatStageChange = &battleengine.SwitchInStatStageChange{
		Target: battleengine.SwitchInStatStageTargetSelf, Stat: battleengine.StatAttack, StageDelta: 1,
	}
	observer := newMember(1, "switch-in-stat-stage-observer", 1_000, 1_000)
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, setter, observer)
	member, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || member.StatStages[battleengine.StatAttack] != 1 {
		t.Fatalf("初始入场自身攻击阶级 = %+v, found=%t", member.StatStages, found)
	}
}

// TestResolveTurnSwitchInStatStageChangeAffectsOpponents 验证实际换入后的对手能力阶级变化会产生结构化事件。
//
// 对侧当前上场成员按稳定阵营与槽位顺序分别结算；这里单打只验证一名对手，且回合内事件必须记录来源和
// 经过边界夹取后的实际变化量。
func TestResolveTurnSwitchInStatStageChangeAffectsOpponents(t *testing.T) {
	t.Parallel()
	first := newMember(1, "switch-in-stat-stage-first", 1_000, 1_000)
	incoming := newMember(2, "switch-in-defense-drop", 1_000, 1_000)
	incoming.SwitchInStatStageChange = &battleengine.SwitchInStatStageChange{
		Target: battleengine.SwitchInStatStageTargetOpponents, Stat: battleengine.StatDefense, StageDelta: -1,
	}
	opponent := newMember(1, "switch-in-stat-stage-opponent", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-stat-stage", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first, incoming}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
			fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
		},
	}, mustRandom(t, 241))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	found := false
	for _, event := range result.Events {
		changed, ok := event.(battleengine.StatStageChangedEvent)
		if ok && changed.Actor == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) &&
			changed.Target == (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) {
			found = changed.Stat == battleengine.StatDefense && changed.Delta == -1 && changed.CurrentStage == -1
		}
	}
	if !found {
		t.Fatalf("换入特性缺少正确能力阶级事件: %+v", result.Events)
	}
	updated, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !exists || updated.StatStages[battleengine.StatDefense] != -1 {
		t.Fatalf("换入特性后的对手防御阶级 = %+v, exists=%t", updated.StatStages, exists)
	}
}

// TestResolveTurnHeldItemBlocksOpponentSwitchInStatStageReduction 验证清净坠饰类道具同时阻止
// 对手入场特性造成的能力阶级下降，而不只阻止技能声明的降阶效果。
//
// 该门禁必须保留目标的权威能力阶级和持有道具，也不能伪造能力变化事件或随机轨迹；入场特性本身
// 没有概率，因此阻止路径不应消费任何随机数。
func TestResolveTurnHeldItemBlocksOpponentSwitchInStatStageReduction(t *testing.T) {
	t.Parallel()
	first := newMember(1, "switch-in-item-immunity-first", 1_000, 1_000)
	incoming := newMember(2, "switch-in-item-immunity-reducer", 1_000, 1_000)
	incoming.SwitchInStatStageChange = &battleengine.SwitchInStatStageChange{
		Target: battleengine.SwitchInStatStageTargetOpponents, Stat: battleengine.StatAttack, StageDelta: -1,
	}
	opponent := newMember(1, "switch-in-item-immunity-target", 1_000, 1_000)
	opponent.ItemID = testID("clear-amulet")
	opponent.HeldItemOpponentStatStageReductionImmunity = true
	opponent.Skills = []battleengine.SkillSnapshot{fieldSpeedOrderSkill(1, "建立戏法空间", 0)}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-item-immunity", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first, incoming}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
			fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
		},
	}, mustRandom(t, 251))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	updated, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || updated.StatStages[battleengine.StatAttack] != 0 || updated.ItemID != testID("clear-amulet") ||
		!updated.HeldItemOpponentStatStageReductionImmunity {
		t.Fatalf("道具阻止入场降阶后的目标 = %+v, found=%t", updated, found)
	}
	for _, event := range result.Events {
		changed, ok := event.(battleengine.StatStageChangedEvent)
		if ok && changed.Target == (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) {
			t.Fatalf("道具阻止入场降阶后不应出现目标能力事件: %+v", result.Events)
		}
	}
	if len(result.RandomTrace) != 0 {
		t.Fatalf("道具阻止无概率入场降阶的随机轨迹 = %+v，期望为空", result.RandomTrace)
	}
}
