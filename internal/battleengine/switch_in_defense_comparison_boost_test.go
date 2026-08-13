package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInOpponentDefenseComparisonBoost 验证入场特性按对手基础防御总和强化正确攻击能力。
func TestInitialStateAppliesSwitchInOpponentDefenseComparisonBoost(t *testing.T) {
	t.Parallel()
	booster := newMember(1, "switch-in-defense-comparison", 1_000, 1_000)
	booster.SwitchInOpponentDefenseComparisonBoost = true
	opponent := newMember(1, "switch-in-defense-comparison-opponent", 1_000, 1_000)
	opponent.Stats.Defense = 80
	opponent.Stats.SpecialDefense = 120
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, booster, opponent)
	member, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || member.StatStages[battleengine.StatAttack] != 1 || member.StatStages[battleengine.StatSpecialAttack] != 0 {
		t.Fatalf("初始入场防御比较强化 = %+v, found=%t", member.StatStages, found)
	}
}

// TestResolveTurnSwitchInOpponentDefenseComparisonBoostPublishesStatEvent 验证实际换入后的比较强化会发布能力阶级事件。
func TestResolveTurnSwitchInOpponentDefenseComparisonBoostPublishesStatEvent(t *testing.T) {
	t.Parallel()
	first := newMember(1, "switch-in-defense-comparison-first", 1_000, 1_000)
	incoming := newMember(2, "switch-in-defense-comparison-incoming", 1_000, 1_000)
	incoming.SwitchInOpponentDefenseComparisonBoost = true
	opponent := newMember(1, "switch-in-defense-comparison-switch-opponent", 1_000, 1_000)
	opponent.Stats.Defense = 100
	opponent.Stats.SpecialDefense = 100
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-defense-comparison", ActiveSlotsPerSide: 1, TeamSize: 2},
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
	}, mustRandom(t, 257))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	found := false
	for _, event := range result.Events {
		changed, ok := event.(battleengine.StatStageChangedEvent)
		if ok && changed.Actor == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) && changed.Target == changed.Actor {
			found = changed.Stat == battleengine.StatSpecialAttack && changed.Delta == 1 && changed.CurrentStage == 1
		}
	}
	if !found {
		t.Fatalf("换入防御比较强化缺少正确能力阶级事件: %+v", result.Events)
	}
}
