package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInRevealOpponentHighestPowerSkill 验证初始入场公开最高威力技能，并以稳定 SkillID 处理威力并列。
func TestInitialStateAppliesSwitchInRevealOpponentHighestPowerSkill(t *testing.T) {
	t.Parallel()
	receiver := newMember(1, "switch-in-reveal-skill-receiver", 1_000, 1_000)
	receiver.SwitchInRevealOpponentHighestPowerSkill = true
	source := newMember(1, "switch-in-reveal-skill-source", 1_000, 1_000)
	source.Skills[0].SkillID = testID("00000000-0000-0000-0000-000000000200")
	source.Skills[0].Power = 120
	source.Skills = append(source.Skills, battleengine.SkillSnapshot{TargetScope: battleengine.SkillTargetScopeSelectedTarget, DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 2, SkillID: testID("00000000-0000-0000-0000-000000000100"), Name: "并列技能",
		ElementID: source.ElementIDs[0], DamageClass: battleengine.DamageClassPhysical, Power: 120,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10,
	})
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-reveal-skill-initial", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{receiver}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	events := state.InitialEvents()
	if len(events) != 1 {
		t.Fatalf("初始最高威力技能公开事件数量 = %d, events=%+v", len(events), events)
	}
	revealed, ok := events[0].(battleengine.OpponentSkillRevealedEvent)
	expectedSkillID := min(source.Skills[0].SkillID, source.Skills[1].SkillID)
	if !ok || revealed.Actor != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}) ||
		revealed.Target != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) || revealed.SkillID != expectedSkillID {
		t.Fatalf("初始最高威力技能公开事件不正确: %+v", events[0])
	}
}

// TestResolveTurnSwitchInRevealOpponentHighestPowerSkillPublishesEvent 验证实际换入只从当前存活上场对手选择冻结技能。
func TestResolveTurnSwitchInRevealOpponentHighestPowerSkillPublishesEvent(t *testing.T) {
	t.Parallel()
	first := newMember(1, "switch-in-reveal-skill-first", 1_000, 1_000)
	incoming := newMember(2, "switch-in-reveal-skill-incoming", 1_000, 1_000)
	incoming.SwitchInRevealOpponentHighestPowerSkill = true
	source := newMember(1, "switch-in-reveal-skill-source", 1_000, 1_000)
	source.Skills[0].SkillID = testID("00000000-0000-0000-0000-000000000300")
	source.Skills[0].Power = 150
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-reveal-skill-switch", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first, incoming}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source}},
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
	}, mustRandom(t, 284))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	var revealed []battleengine.OpponentSkillRevealedEvent
	for _, event := range result.Events {
		if value, ok := event.(battleengine.OpponentSkillRevealedEvent); ok {
			revealed = append(revealed, value)
		}
	}
	if len(revealed) != 1 || revealed[0].Actor != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) ||
		revealed[0].Target != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) || revealed[0].SkillID != source.Skills[0].SkillID {
		t.Fatalf("换入最高威力技能公开事件不正确: %+v", result.Events)
	}
}
