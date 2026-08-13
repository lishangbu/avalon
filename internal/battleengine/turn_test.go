package battleengine_test

import (
	"errors"
	"fmt"
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

func TestResolveTurnExecutesCompleteSingleBattleCommandInStableOrder(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}

	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			{
				Kind:  battleengine.ActionKindUseSkill,
				Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{
					SkillPosition: 1,
					Target:        battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				},
			},
			{
				Kind:  battleengine.ActionKindUseSkill,
				Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				UseSkill: &battleengine.UseSkillAction{
					SkillPosition: 1,
					Target:        battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				},
			},
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}

	leftAfter, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	rightAfter, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if leftAfter.CurrentHP != 73 || rightAfter.CurrentHP != 74 {
		t.Fatalf("HP after turn = %d, %d, want 73, 74", leftAfter.CurrentHP, rightAfter.CurrentHP)
	}
	if leftAfter.Skills[0].RemainingPP != 34 || rightAfter.Skills[0].RemainingPP != 34 {
		t.Fatalf("remaining PP = %d, %d, want 34, 34", leftAfter.Skills[0].RemainingPP, rightAfter.Skills[0].RemainingPP)
	}
	if result.State.TurnNumber() != 1 {
		t.Fatalf("TurnNumber() = %d, want 1", result.State.TurnNumber())
	}
	if len(result.Events) != 6 || result.Events[0].Kind() != battleengine.EventKindTurnStarted ||
		result.Events[1].Kind() != battleengine.EventKindSkillUsed ||
		result.Events[2].Kind() != battleengine.EventKindDamageApplied ||
		result.Events[5].Kind() != battleengine.EventKindTurnEnded {
		t.Fatalf("event kinds = %v, want stable turn lifecycle", eventKinds(result.Events))
	}
	if len(result.RandomTrace) != 4 || result.RandomTrace[0].Reason != "critical hit for "+testID("00000000-0000-0000-0000-000000000020").String() ||
		result.RandomTrace[1].Reason != "damage random for "+testID("00000000-0000-0000-0000-000000000020").String() {
		t.Fatalf("random trace = %+v", result.RandomTrace)
	}
}

func TestResolveTurnCanonicalizesSpeedTieBeforeConsumingRandomTrace(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 50, 50)
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 50, 50)
	left.Skills[0].Power = 250
	right.Skills[0].Power = 250
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}

	if len(result.RandomTrace) != 4 || result.RandomTrace[0].Reason != "speed tie for side 1 member 1" ||
		result.RandomTrace[1].Reason != "speed tie for side 2 member 1" {
		t.Fatalf("speed tie trace = %+v", result.RandomTrace)
	}
	if len(result.Events) != 6 || result.Events[3].Kind() != battleengine.EventKindParticipantFainted ||
		result.Events[4].Kind() != battleengine.EventKindBattleEnded ||
		result.Events[5].Kind() != battleengine.EventKindTurnEnded {
		t.Fatalf("event kinds = %v, want battle end before turn end", eventKinds(result.Events))
	}
	damage, ok := result.Events[2].(battleengine.DamageAppliedEvent)
	if !ok || damage.Actor.Side != battleengine.SideOne {
		t.Fatalf("first damage event = %#v, want SideOne actor", result.Events[2])
	}
	battleResult, ok := result.State.Result()
	if !ok || battleResult.WinningSide != battleengine.SideOne || battleResult.Reason != battleengine.BattleResultReasonAllMembersFainted {
		t.Fatalf("Result() = %+v, %t, want SideOne allMembersFainted", battleResult, ok)
	}
}

func TestResolveTurnRestartsRandomTraceSequenceForEveryTurn(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 500, 500)
	left.Stats.Speed = 110
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 500, 500)
	right.Stats.Speed = 90
	state, _ := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	first, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("first ResolveTurn() error = %v", err)
	}
	second, err := battleengine.ResolveTurn(first.State, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    2,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, first.RandomSource)
	if err != nil {
		t.Fatalf("second ResolveTurn() error = %v", err)
	}
	if len(second.RandomTrace) == 0 || second.RandomTrace[0].Sequence != 1 {
		t.Fatalf("second turn random trace = %+v, want sequence restarting at 1", second.RandomTrace)
	}
}

func TestResolveTurnTargetsTheNewMemberAfterAnEarlierSwitch(t *testing.T) {
	t.Parallel()

	leftLead := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	leftLead.Stats.Speed = 80
	leftReserve := newMember(2, "00000000-0000-0000-0000-000000000002", 100, 100)
	right := newMember(1, "00000000-0000-0000-0000-000000000003", 100, 100)
	right.Stats.Speed = 120
	state, _ := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{
				Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1},
				Members: []battleengine.MemberSnapshot{leftLead, leftReserve},
			},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			{
				Kind:   battleengine.ActionKindSwitch,
				Actor:  battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				Switch: &battleengine.SwitchAction{MemberPosition: 2},
			},
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}

	active, ok := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !ok || active.Position != 2 || active.CurrentHP != 74 {
		t.Fatalf("active member = %+v, %t, want switched member 2 with 74 HP", active, ok)
	}
	if len(result.Events) != 5 || result.Events[1].Kind() != battleengine.EventKindParticipantSwitched ||
		result.Events[3].Kind() != battleengine.EventKindDamageApplied {
		t.Fatalf("event kinds = %v, want switch before redirected damage", eventKinds(result.Events))
	}
	damage, ok := result.Events[3].(battleengine.DamageAppliedEvent)
	if !ok || damage.Target.Position != 2 {
		t.Fatalf("damage event = %#v, want memberPosition 2", result.Events[3])
	}
}

func TestResolveTurnAllowsForcedReplacementForAFaintedActiveMember(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 200, 200)
	left.Stats.Speed = 120
	left.Skills[0].Power = 250
	rightLead := newMember(1, "00000000-0000-0000-0000-000000000002", 50, 50)
	rightLead.Stats.Speed = 80
	rightReserve := newMember(2, "00000000-0000-0000-0000-000000000003", 100, 100)
	state, _ := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{
				Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1},
				Members: []battleengine.MemberSnapshot{rightLead, rightReserve},
			},
		},
	})
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	first, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("first ResolveTurn() error = %v", err)
	}
	if _, ended := first.State.Result(); ended {
		t.Fatal("Result() ended battle while fainted side still has a reserve")
	}

	second, err := battleengine.ResolveTurn(first.State, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    2,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			{
				Kind:   battleengine.ActionKindSwitch,
				Actor:  battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				Switch: &battleengine.SwitchAction{MemberPosition: 2},
			},
		},
	}, first.RandomSource)
	if err != nil {
		t.Fatalf("second ResolveTurn() error = %v", err)
	}
	switched, ok := second.Events[1].(battleengine.ParticipantSwitchedEvent)
	if !ok || !switched.Forced || switched.PreviousMember.Position != 1 || switched.NextMember.Position != 2 {
		t.Fatalf("forced switch event = %#v", second.Events[1])
	}
	active, ok := second.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !ok || active.Position != 2 {
		t.Fatalf("active member = %+v, %t, want reserve member 2", active, ok)
	}
}

func TestResolveTurnAppliesFrozenElementEffectiveness(t *testing.T) {
	t.Parallel()

	var (
		attackElement  = testID("00000000-0000-0000-0000-000000000010")
		defenseElement = testID("00000000-0000-0000-0000-000000000011")
	)
	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	right.ElementIDs = testIDs(defenseElement)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules: battleengine.RuleSnapshot{
			SchemaVersion: 1,
			ElementEffectiveness: []battleengine.ElementEffectiveness{
				{AttackElementID: attackElement, DefenseElementID: defenseElement, Numerator: 2, Denominator: 1},
			},
		},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	damage, ok := result.Events[2].(battleengine.DamageAppliedEvent)
	if !ok || damage.Amount != 52 {
		t.Fatalf("first damage event = %#v, want 52 super-effective damage", result.Events[2])
	}
}

func TestResolveTurnEndsWithoutWinnerAtTheFrozenTurnLimit(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 500, 500)
	left.Stats.Speed = 110
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 500, 500)
	right.Stats.Speed = 90
	state, _ := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{
			Code: "one-turn-preview", ActiveSlotsPerSide: 1, TeamSize: 1, MaxTurns: 1,
		},
		Rules: battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	battleResult, ok := result.State.Result()
	if !ok || battleResult.WinningSide != 0 || battleResult.Reason != battleengine.BattleResultReasonMaxTurnsReached {
		t.Fatalf("Result() = %+v, %t, want no winner at max turn", battleResult, ok)
	}
	if result.Events[len(result.Events)-2].Kind() != battleengine.EventKindBattleEnded ||
		result.Events[len(result.Events)-1].Kind() != battleengine.EventKindTurnEnded {
		t.Fatalf("event kinds = %v, want battle end before turn end", eventKinds(result.Events))
	}
}

func TestResolveTurnAppliesGuaranteedMajorStatusWithoutExtraRandomConsumption(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{
		{Status: battleengine.MajorStatusBurn, Target: battleengine.EffectTargetSelected, ChancePercent: 100},
	}
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if result.Events[2].Kind() != battleengine.EventKindMajorStatusApplied {
		t.Fatalf("event kinds = %v, want major status application", eventKinds(result.Events))
	}
	target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if target.MajorStatus != battleengine.MajorStatusBurn {
		t.Fatalf("target major status = %q, want burn", target.MajorStatus)
	}
	if len(result.RandomTrace) != 2 {
		t.Fatalf("random trace = %+v, guaranteed status must not consume an extra roll", result.RandomTrace)
	}
}

func TestResolveTurnAppliesBurnAttackReductionAndEndTurnDamage(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	left.MajorStatus = battleengine.MajorStatusBurn
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}

	firstDamage, ok := result.Events[2].(battleengine.DamageAppliedEvent)
	if !ok || firstDamage.Amount != 13 {
		t.Fatalf("burned actor damage = %#v, want 13 after physical attack reduction", result.Events[2])
	}
	leftAfter, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if leftAfter.CurrentHP != 67 {
		t.Fatalf("burned member HP = %d, want 67 after 27 skill damage and 6 burn damage", leftAfter.CurrentHP)
	}
	statusDamage, ok := result.Events[len(result.Events)-2].(battleengine.MajorStatusDamageAppliedEvent)
	if !ok || statusDamage.Target.Side != battleengine.SideOne || statusDamage.Amount != 6 ||
		statusDamage.Status != battleengine.MajorStatusBurn {
		t.Fatalf("end-turn status damage = %#v", result.Events[len(result.Events)-2])
	}
}

func TestResolveTurnRecordsAnExplicitSkillMissEvent(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	left.Skills[0].Accuracy = 50
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	state, _ := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	missed, ok := result.Events[2].(battleengine.SkillMissedEvent)
	if !ok || missed.Roll != 66 || missed.Accuracy != 50 || missed.Actor.Side != battleengine.SideOne {
		t.Fatalf("skill missed event = %#v", result.Events[2])
	}
	leftAfter, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	rightAfter, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if leftAfter.CurrentHP != 72 || rightAfter.CurrentHP != 100 {
		t.Fatalf("HP after miss = %d, %d, want 72, 100", leftAfter.CurrentHP, rightAfter.CurrentHP)
	}
}

func TestResolveTurnTreatsZeroAccuracyAsGuaranteedHit(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	left.Skills[0].Accuracy = 0
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	state, _ := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if result.Events[2].Kind() != battleengine.EventKindDamageApplied || len(result.RandomTrace) != 4 {
		t.Fatalf("events = %v, trace = %+v; zero accuracy must hit without accuracy roll", eventKinds(result.Events), result.RandomTrace)
	}
}

func TestResolveTurnRejectsDuplicateSwitchTargetsAcrossDoubleBattleSlots(t *testing.T) {
	t.Parallel()

	leftMembers := []battleengine.MemberSnapshot{
		newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100),
		newMember(2, "00000000-0000-0000-0000-000000000002", 100, 100),
		newMember(3, "00000000-0000-0000-0000-000000000003", 100, 100),
	}
	rightMembers := []battleengine.MemberSnapshot{
		newMember(1, "00000000-0000-0000-0000-000000000004", 100, 100),
		newMember(2, "00000000-0000-0000-0000-000000000005", 100, 100),
	}
	state, _ := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-double", ActiveSlotsPerSide: 2, TeamSize: 3},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: leftMembers},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: rightMembers},
		},
	})
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	_, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 3}},
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}, Switch: &battleengine.SwitchAction{MemberPosition: 3}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 2}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}}},
		},
	}, random)
	if !errors.Is(err, battleengine.ErrInvalidTurnCommand) {
		t.Fatalf("ResolveTurn() error = %v, want ErrInvalidTurnCommand", err)
	}
	var commandError *battleengine.TurnCommandError
	if !errors.As(err, &commandError) || commandError.Code != battleengine.TurnCommandErrorDuplicateSwitchTarget ||
		commandError.Field != "/actions/1/switch/memberPosition" {
		t.Fatalf("structured command error = %#v", commandError)
	}
}

func TestResolveTurnCanReplayARecordedRandomTrace(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	state, _ := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	command := battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	generated, err := battleengine.ResolveTurn(state, command, random)
	if err != nil {
		t.Fatalf("generated ResolveTurn() error = %v", err)
	}
	replay, err := battleengine.NewTracedRandom(generated.RandomTrace)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	replayed, err := battleengine.ResolveTurn(state, command, replay)
	if err != nil {
		t.Fatalf("replayed ResolveTurn() error = %v", err)
	}
	if !reflect.DeepEqual(replayed.State.Snapshot(), generated.State.Snapshot()) ||
		!reflect.DeepEqual(replayed.Events, generated.Events) ||
		!reflect.DeepEqual(replayed.RandomTrace, generated.RandomTrace) {
		t.Fatalf("replayed result differs from generated result")
	}
}

func TestResolveTurnRejectsAnUnderConsumedReplayTrace(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	state, _ := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	command := battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	generated, _ := battleengine.ResolveTurn(state, command, random)
	extraTrace := append([]battleengine.RandomTraceEntry(nil), generated.RandomTrace...)
	extraTrace = append(extraTrace, battleengine.RandomTraceEntry{
		Sequence: int32(len(extraTrace) + 1), Bound: 1, Reason: "unexpected-extra-roll", Value: 0,
	})
	replay, err := battleengine.NewTracedRandom(extraTrace)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	if _, err := battleengine.ResolveTurn(state, command, replay); !errors.Is(err, battleengine.ErrRandomTraceDiverged) {
		t.Fatalf("ResolveTurn() error = %v, want ErrRandomTraceDiverged", err)
	}
}

func TestResolveTurnRejectsAPartiallyConsumedReplayInput(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	state, _ := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	command := battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	generated, _ := battleengine.ResolveTurn(state, command, random)
	replay, _ := battleengine.NewTracedRandom(generated.RandomTrace)
	firstEntry := generated.RandomTrace[0]
	_, partiallyConsumed, _, err := replay.Next(firstEntry.Bound, firstEntry.Reason)
	if err != nil {
		t.Fatalf("replay.Next() error = %v", err)
	}
	if _, err := battleengine.ResolveTurn(state, command, partiallyConsumed); !errors.Is(err, battleengine.ErrInvalidRandomTrace) {
		t.Fatalf("ResolveTurn() error = %v, want ErrInvalidRandomTrace", err)
	}
}

func TestResolveTurnIncreasesBadPoisonDamageEveryCompletedTurn(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 160, 160)
	left.Stats.Speed = 110
	left.MajorStatus = battleengine.MajorStatusBadPoison
	left.BadPoisonCounter = 1
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{
		{Status: battleengine.MajorStatusBurn, Target: battleengine.EffectTargetSelected, ChancePercent: 0},
	}
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 160, 160)
	right.Stats.Speed = 90
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{
		{Status: battleengine.MajorStatusBurn, Target: battleengine.EffectTargetSelected, ChancePercent: 0},
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	command := battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	first, err := battleengine.ResolveTurn(state, command, random)
	if err != nil {
		t.Fatalf("first ResolveTurn() error = %v", err)
	}
	leftAfterFirst, _ := first.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if leftAfterFirst.CurrentHP != 150 || leftAfterFirst.BadPoisonCounter != 2 {
		t.Fatalf("first turn bad poison state = %+v, want 150 HP and counter 2", leftAfterFirst)
	}
	command.TurnNumber = 2
	second, err := battleengine.ResolveTurn(first.State, command, first.RandomSource)
	if err != nil {
		t.Fatalf("second ResolveTurn() error = %v", err)
	}
	leftAfterSecond, _ := second.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if leftAfterSecond.CurrentHP != 130 || leftAfterSecond.BadPoisonCounter != 3 {
		t.Fatalf("second turn bad poison state = %+v, want 130 HP and counter 3", leftAfterSecond)
	}
}

func TestResolveTurnResetsBadPoisonCounterWhenMemberLeavesBattlefield(t *testing.T) {
	t.Parallel()

	poisoned := newMember(1, "00000000-0000-0000-0000-000000000001", 160, 160)
	poisoned.MajorStatus = battleengine.MajorStatusBadPoison
	poisoned.BadPoisonCounter = 5
	poisoned.StatStages = map[battleengine.Stat]int8{battleengine.StatAttack: 3, battleengine.StatEvasion: -2}
	bench := newMember(2, "00000000-0000-0000-0000-000000000003", 160, 160)
	opponent := newMember(1, "00000000-0000-0000-0000-000000000002", 160, 160)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{
				Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1},
				Members: []battleengine.MemberSnapshot{poisoned, bench},
			},
			{
				Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1},
				Members: []battleengine.MemberSnapshot{opponent},
			},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			{
				Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				Switch: &battleengine.SwitchAction{MemberPosition: 2},
			},
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	snapshot := result.State.Snapshot()
	if counter := snapshot.Sides[0].Members[0].BadPoisonCounter; counter != 1 {
		t.Fatalf("switched-out bad poison counter = %d, want 1", counter)
	}
	if stages := snapshot.Sides[0].Members[0].StatStages; len(stages) != 0 {
		t.Fatalf("switched-out stat stages = %v, want empty", stages)
	}
}

func TestResolveTurnAppliesParalysisSpeedReductionAndPreMoveBlock(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 150
	left.MajorStatus = battleengine.MajorStatusParalysis
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 100
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	replay, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 15},
		{Sequence: 3, Bound: 100, Reason: "paralysis chance for side 1 member 1", Value: 0},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, replay)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	firstSkill, ok := result.Events[1].(battleengine.SkillUsedEvent)
	if !ok || firstSkill.Actor.Side != battleengine.SideTwo {
		t.Fatalf("first skill event = %#v, want faster SideTwo after paralysis speed reduction", result.Events[1])
	}
	prevented, ok := result.Events[3].(battleengine.SkillPreventedEvent)
	if !ok || prevented.Actor.Side != battleengine.SideOne || prevented.Reason != battleengine.SkillPreventionReasonParalysis {
		t.Fatalf("skill prevented event = %#v", result.Events[3])
	}
	leftAfter, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if leftAfter.Skills[0].RemainingPP != 35 {
		t.Fatalf("paralyzed member remaining PP = %d, want 35", leftAfter.Skills[0].RemainingPP)
	}
}

func TestResolveTurnConsumesTheLastSleepBlockedActionAndWakesTheMember(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	left.MajorStatus = battleengine.MajorStatusSleep
	left.SleepTurnsRemaining = 1
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	prevented, ok := result.Events[1].(battleengine.SkillPreventedEvent)
	if !ok || prevented.Reason != battleengine.SkillPreventionReasonSleep || prevented.TurnsRemainingBefore != 1 {
		t.Fatalf("sleep prevented event = %#v", result.Events[1])
	}
	cleared, ok := result.Events[2].(battleengine.MajorStatusClearedEvent)
	if !ok || cleared.Status != battleengine.MajorStatusSleep || cleared.Target.Side != battleengine.SideOne {
		t.Fatalf("sleep cleared event = %#v", result.Events[2])
	}
	leftAfter, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if leftAfter.MajorStatus != "" || leftAfter.SleepTurnsRemaining != 0 || leftAfter.Skills[0].RemainingPP != 35 {
		t.Fatalf("woken member state = %+v", leftAfter)
	}
}

func TestResolveTurnClearsFreezeBeforeContinuingAfterNaturalThaw(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	left.MajorStatus = battleengine.MajorStatusFreeze
	left.Skills[0].Power = 250
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 50, 50)
	right.Stats.Speed = 90
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	replay, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 100, Reason: "freeze thaw chance for side 1 member 1", Value: 0},
		{Sequence: 2, Bound: 24, Reason: "critical hit for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 1},
		{Sequence: 3, Bound: 16, Reason: "damage random for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 15},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, replay)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	cleared, ok := result.Events[1].(battleengine.MajorStatusClearedEvent)
	if !ok || cleared.Status != battleengine.MajorStatusFreeze {
		t.Fatalf("freeze cleared event = %#v", result.Events[1])
	}
	if result.Events[2].Kind() != battleengine.EventKindSkillUsed {
		t.Fatalf("event kinds = %v, want skill use after natural thaw", eventKinds(result.Events))
	}
	leftAfter, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if leftAfter.MajorStatus != "" || leftAfter.Skills[0].RemainingPP != 34 {
		t.Fatalf("thawed member state = %+v", leftAfter)
	}
}

func TestResolveTurnRecordsElementImmunityWhenMajorStatusIsBlocked(t *testing.T) {
	t.Parallel()

	tests := []struct {
		// name 是子测试名称，用于定位失败的异常与免疫属性组合。
		name string
		// status 是技能尝试施加给目标的主要异常。
		status battleengine.MajorStatus
		// elementCode 是规则快照中识别免疫属性的稳定 code。
		elementCode string
	}{
		{name: "火属性免疫灼伤", status: battleengine.MajorStatusBurn, elementCode: "fire"},
		{name: "毒属性免疫中毒", status: battleengine.MajorStatusPoison, elementCode: "poison"},
		{name: "钢属性免疫中毒", status: battleengine.MajorStatusPoison, elementCode: "steel"},
		{name: "毒属性免疫剧毒", status: battleengine.MajorStatusBadPoison, elementCode: "poison"},
		{name: "钢属性免疫剧毒", status: battleengine.MajorStatusBadPoison, elementCode: "steel"},
		{name: "电属性免疫麻痹", status: battleengine.MajorStatusParalysis, elementCode: "electric"},
		{name: "冰属性免疫冰冻", status: battleengine.MajorStatusFreeze, elementCode: "ice"},
	}
	for index, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()

			elementID := testID(fmt.Sprintf("00000000-0000-0000-0000-%012d", 100+index))
			left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
			left.Stats.Speed = 110
			left.Skills[0].DamageClass = battleengine.DamageClassStatus
			left.Skills[0].Power = 0
			left.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{
				{Status: test.status, Target: battleengine.EffectTargetSelected, ChancePercent: 100},
			}
			right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
			right.Stats.Speed = 90
			right.ElementIDs = testIDs(elementID)
			state, err := battleengine.NewState(battleengine.InitialState{
				Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
				Rules: battleengine.RuleSnapshot{
					SchemaVersion: 1,
					ElementIDs:    map[string]Identifier{test.elementCode: elementID},
				},
				Sides: []battleengine.SideSnapshot{
					{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
					{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
				},
			})
			if err != nil {
				t.Fatalf("NewState() error = %v", err)
			}
			random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
			result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
				SchemaVersion: 1,
				TurnNumber:    1,
				Actions: []battleengine.Action{
					useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
					useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
				},
			}, random)
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			blocked, ok := result.Events[2].(battleengine.MajorStatusBlockedEvent)
			if !ok || blocked.Status != test.status ||
				blocked.Reason != battleengine.MajorStatusBlockReasonElementImmunity {
				t.Fatalf("major status blocked event = %#v", result.Events[2])
			}
			target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
			if target.MajorStatus != "" {
				t.Fatalf("immune target major status = %q, want empty", target.MajorStatus)
			}
		})
	}
}

func TestResolveTurnRecordsExistingMajorStatusWhenApplicationIsBlocked(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{
		{Status: battleengine.MajorStatusBurn, Target: battleengine.EffectTargetSelected, ChancePercent: 100},
	}
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	right.MajorStatus = battleengine.MajorStatusPoison
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	blocked, ok := result.Events[2].(battleengine.MajorStatusBlockedEvent)
	if !ok || blocked.Status != battleengine.MajorStatusBurn ||
		blocked.Reason != battleengine.MajorStatusBlockReasonExistingStatus {
		t.Fatalf("major status blocked event = %#v", result.Events[2])
	}
	target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if target.MajorStatus != battleengine.MajorStatusPoison {
		t.Fatalf("target major status = %q, want poison retained", target.MajorStatus)
	}
}

func TestResolveTurnUsesLatestTargetStateForSequentialMajorStatusApplications(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{
		{Status: battleengine.MajorStatusBurn, Target: battleengine.EffectTargetSelected, ChancePercent: 100},
		{Status: battleengine.MajorStatusPoison, Target: battleengine.EffectTargetSelected, ChancePercent: 100},
	}
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{
		{Status: battleengine.MajorStatusBurn, Target: battleengine.EffectTargetSelected, ChancePercent: 0},
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if target.MajorStatus != battleengine.MajorStatusBurn {
		t.Fatalf("target major status = %q, want first application burn retained", target.MajorStatus)
	}
	blocked, ok := result.Events[3].(battleengine.MajorStatusBlockedEvent)
	if !ok || blocked.Status != battleengine.MajorStatusPoison ||
		blocked.Reason != battleengine.MajorStatusBlockReasonExistingStatus {
		t.Fatalf("second major status event = %#v, want existing-status block", result.Events[3])
	}
}

func TestResolveTurnAppliesNormalPoisonResidualDamage(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 160, 160)
	left.MajorStatus = battleengine.MajorStatusPoison
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{
		{Status: battleengine.MajorStatusBurn, Target: battleengine.EffectTargetSelected, ChancePercent: 0},
	}
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 160, 160)
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{
		{Status: battleengine.MajorStatusBurn, Target: battleengine.EffectTargetSelected, ChancePercent: 0},
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	poisoned, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if poisoned.CurrentHP != 150 {
		t.Fatalf("poisoned member HP = %d, want 150", poisoned.CurrentHP)
	}
	statusDamage, ok := result.Events[len(result.Events)-2].(battleengine.MajorStatusDamageAppliedEvent)
	if !ok || statusDamage.Status != battleengine.MajorStatusPoison || statusDamage.Amount != 10 {
		t.Fatalf("poison damage event = %#v", result.Events[len(result.Events)-2])
	}
}

func TestResolveTurnAppliesAndClampsDeclaredStatStageEffects(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	left.StatStages = map[battleengine.Stat]int8{battleengine.StatAttack: 5}
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].StatStageEffects = []battleengine.StatStageEffect{
		{Stat: battleengine.StatAttack, Target: battleengine.EffectTargetUser, StageDelta: 2, ChancePercent: 100},
		{Stat: battleengine.StatDefense, Target: battleengine.EffectTargetSelected, StageDelta: -2, ChancePercent: 100},
	}
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{
		{Status: battleengine.MajorStatusBurn, Target: battleengine.EffectTargetSelected, ChancePercent: 0},
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	actor, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if actor.StatStages[battleengine.StatAttack] != 6 || target.StatStages[battleengine.StatDefense] != -2 {
		t.Fatalf("stat stages = actor %v, target %v", actor.StatStages, target.StatStages)
	}
	first, firstOK := result.Events[2].(battleengine.StatStageChangedEvent)
	second, secondOK := result.Events[3].(battleengine.StatStageChangedEvent)
	if !firstOK || !secondOK || first.Delta != 1 || first.CurrentStage != 6 || second.Delta != -2 {
		t.Fatalf("stat stage events = %#v, %#v", result.Events[2], result.Events[3])
	}
}

func TestResolveTurnUsesSpeedStageForActionOrdering(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 100
	left.StatStages = map[battleengine.Stat]int8{battleengine.StatSpeed: 1}
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 120
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	first, ok := result.Events[1].(battleengine.SkillUsedEvent)
	if !ok || first.Actor.Side != battleengine.SideOne {
		t.Fatalf("first skill event = %#v, want boosted SideOne", result.Events[1])
	}
}

func TestResolveTurnUsesAttackStageInDamageFormula(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 200, 200)
	left.Stats.Speed = 110
	left.StatStages = map[battleengine.Stat]int8{battleengine.StatAttack: 2}
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 200, 200)
	right.Stats.Speed = 90
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	replay, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 15},
		{Sequence: 3, Bound: 24, Reason: "critical hit for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 1},
		{Sequence: 4, Bound: 16, Reason: "damage random for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 15},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, replay)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	damage, ok := result.Events[2].(battleengine.DamageAppliedEvent)
	if !ok || damage.Amount != 55 {
		t.Fatalf("boosted damage event = %#v, want 55 damage", result.Events[2])
	}
}

func TestResolveTurnUsesAccuracyAndEvasionStagesForHitCheck(t *testing.T) {
	t.Parallel()

	left := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	left.Stats.Speed = 110
	left.Skills[0].Accuracy = 75
	right := newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)
	right.Stats.Speed = 90
	right.StatStages = map[battleengine.Stat]int8{battleengine.StatEvasion: 1}
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{
		{Status: battleengine.MajorStatusBurn, Target: battleengine.EffectTargetSelected, ChancePercent: 0},
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	replay, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 100, Reason: "accuracy for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 60},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
			useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
		},
	}, replay)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	missed, ok := result.Events[2].(battleengine.SkillMissedEvent)
	if !ok || missed.Accuracy != 56 || missed.Roll != 61 {
		t.Fatalf("skill missed event = %#v, want effective accuracy 56 and roll 61", result.Events[2])
	}
}

func useSkillCommand(actorSide, targetSide battleengine.Side) battleengine.Action {
	return battleengine.Action{
		Kind:  battleengine.ActionKindUseSkill,
		Actor: battleengine.SlotRef{Side: actorSide, Position: 1},
		UseSkill: &battleengine.UseSkillAction{
			SkillPosition: 1,
			Target:        battleengine.SlotRef{Side: targetSide, Position: 1},
		},
	}
}

func eventKinds(events []battleengine.Event) []battleengine.EventKind {
	kinds := make([]battleengine.EventKind, len(events))
	for index, event := range events {
		kinds[index] = event.Kind()
	}
	return kinds
}
