package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInRevealOpponentHeldItems 验证初始入场公开存活且有道具的对手，并保留初始事件账本。
func TestInitialStateAppliesSwitchInRevealOpponentHeldItems(t *testing.T) {
	t.Parallel()
	receiver := newMember(1, "switch-in-reveal-item-receiver", 1_000, 1_000)
	receiver.SwitchInRevealOpponentHeldItems = true
	source := newMember(1, "switch-in-reveal-item-source", 1_000, 1_000)
	source.ItemID = testID("held-item")
	withoutItem := newMember(2, "switch-in-reveal-item-without-item", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-reveal-item-initial", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{receiver, withoutItem}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source, withoutItem}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	events := state.InitialEvents()
	if len(events) != 1 {
		t.Fatalf("初始道具公开事件数量 = %d, events=%+v", len(events), events)
	}
	revealed, ok := events[0].(battleengine.OpponentHeldItemRevealedEvent)
	if !ok || revealed.Actor != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}) ||
		revealed.Target != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) || revealed.ItemID != source.ItemID {
		t.Fatalf("初始道具公开事件不正确: %+v", events[0])
	}
}

// TestResolveTurnSwitchInRevealOpponentHeldItemsPublishesEvents 验证实际换入只公开仍存活且确实持有道具的对手。
func TestResolveTurnSwitchInRevealOpponentHeldItemsPublishesEvents(t *testing.T) {
	t.Parallel()
	first := newMember(1, "switch-in-reveal-item-first", 1_000, 1_000)
	incoming := newMember(2, "switch-in-reveal-item-incoming", 1_000, 1_000)
	incoming.SwitchInRevealOpponentHeldItems = true
	source := newMember(1, "switch-in-reveal-item-source", 1_000, 1_000)
	source.ItemID = testID("held-item")
	withoutItem := newMember(2, "switch-in-reveal-item-without-item", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-reveal-item-switch", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first, incoming}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source, withoutItem}},
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
	}, mustRandom(t, 283))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	var revealed []battleengine.OpponentHeldItemRevealedEvent
	for _, event := range result.Events {
		if value, ok := event.(battleengine.OpponentHeldItemRevealedEvent); ok {
			revealed = append(revealed, value)
		}
	}
	if len(revealed) != 1 || revealed[0].Actor != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) ||
		revealed[0].Target != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) || revealed[0].ItemID != source.ItemID {
		t.Fatalf("换入道具公开事件不正确: %+v", result.Events)
	}
}
