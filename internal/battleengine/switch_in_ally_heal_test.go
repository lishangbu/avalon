package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInAllyHeal 验证初始双打上场时的入场特性只回复同侧其它受伤场上成员。
//
// 回复量以接收者自己的最大生命计算；触发者自身、对侧成员和后备成员均不属于该独立规则的目标集合。
func TestInitialStateAppliesSwitchInAllyHeal(t *testing.T) {
	t.Parallel()
	source := newMember(1, "switch-in-ally-heal-source", 1_000, 1_000)
	source.SwitchInAllyHeal = &battleengine.SwitchInAllyHeal{HealDenominator: 16}
	ally := newMember(2, "switch-in-ally-heal-recipient", 1_000, 500)
	opponentOne := newMember(1, "switch-in-ally-heal-opponent-one", 1_000, 500)
	opponentTwo := newMember(2, "switch-in-ally-heal-opponent-two", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-ally-heal-initial", ActiveSlotsPerSide: 2, TeamSize: 2},
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
	if !found || updatedAlly.CurrentHP != 562 {
		t.Fatalf("初始入场同侧回复后的队友生命 = %d, found=%t", updatedAlly.CurrentHP, found)
	}
	updatedOpponent, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || updatedOpponent.CurrentHP != 500 {
		t.Fatalf("初始入场同侧回复不应影响对手: hp=%d, found=%t", updatedOpponent.CurrentHP, found)
	}
}

// TestResolveTurnSwitchInAllyHealPublishesDedicatedEvent 验证实际换入后的同侧回复会产生独立事件。
func TestResolveTurnSwitchInAllyHealPublishesDedicatedEvent(t *testing.T) {
	t.Parallel()
	first := newMember(1, "switch-in-ally-heal-first", 1_000, 1_000)
	ally := newMember(2, "switch-in-ally-heal-ally", 1_000, 500)
	incoming := newMember(3, "switch-in-ally-heal-incoming", 1_000, 1_000)
	incoming.SwitchInAllyHeal = &battleengine.SwitchInAllyHeal{HealDenominator: 16}
	opponentOne := newMember(1, "switch-in-ally-heal-switch-opponent-one", 1_000, 1_000)
	opponentTwo := newMember(2, "switch-in-ally-heal-switch-opponent-two", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-ally-heal-switch", ActiveSlotsPerSide: 2, TeamSize: 3},
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
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}, UseSkill: &battleengine.UseSkillAction{
				SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
			}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{
				SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
			}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 2}, UseSkill: &battleengine.UseSkillAction{
				SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
			}},
		},
	}, mustRandom(t, 251))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	found := false
	for _, event := range result.Events {
		healed, ok := event.(battleengine.SwitchInAllyHealingAppliedEvent)
		if ok && healed.Source == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 3}) &&
			healed.Recipient == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) {
			found = healed.Amount == 62 && healed.CurrentHP == 562
		}
	}
	if !found {
		t.Fatalf("换入同侧回复缺少正确事件: %+v", result.Events)
	}
	updated, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 2})
	if !found || updated.CurrentHP != 562 {
		t.Fatalf("换入同侧回复后的队友生命 = %d, found=%t", updated.CurrentHP, found)
	}
}
