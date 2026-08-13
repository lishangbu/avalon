package battleengine_test

import (
	"encoding/json"
	"strings"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

func TestNewStateFreezesPositionBasedInitialSnapshot(t *testing.T) {
	t.Parallel()

	initial := battleengine.InitialState{
		Format: battleengine.FormatSnapshot{
			Code:               "standard-single",
			ActiveSlotsPerSide: 1,
			TeamSize:           2,
		},
		Rules: battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{
				Side:          battleengine.SideOne,
				ActiveMembers: []battleengine.MemberPosition{1},
				Members: []battleengine.MemberSnapshot{
					newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100),
					newMember(2, "00000000-0000-0000-0000-000000000002", 100, 80),
				},
			},
			{
				Side:          battleengine.SideTwo,
				ActiveMembers: []battleengine.MemberPosition{1},
				Members: []battleengine.MemberSnapshot{
					newMember(1, "00000000-0000-0000-0000-000000000003", 120, 120),
					newMember(2, "00000000-0000-0000-0000-000000000004", 120, 90),
				},
			},
		},
	}

	state, err := battleengine.NewState(initial)
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	initial.Sides[0].ActiveMembers[0] = 2
	initial.Sides[0].Members[0].CurrentHP = 1

	active, ok := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !ok {
		t.Fatal("ActiveMember() ok = false")
	}
	if active.Position != 1 || active.CurrentHP != 100 {
		t.Fatalf("ActiveMember() = %+v, want member position 1 with 100 HP", active)
	}
	if state.TurnNumber() != 0 {
		t.Fatalf("TurnNumber() = %d, want 0", state.TurnNumber())
	}
}

func TestStateSnapshotIsLanguageIndependentAndDetached(t *testing.T) {
	t.Parallel()

	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{
				Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1},
				Members: []battleengine.MemberSnapshot{newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)},
			},
			{
				Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1},
				Members: []battleengine.MemberSnapshot{newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100)},
			},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}

	snapshot := state.Snapshot()
	encoded, err := json.Marshal(snapshot)
	if err != nil {
		t.Fatalf("json.Marshal() error = %v", err)
	}
	jsonText := string(encoded)
	if !strings.Contains(jsonText, `"turnNumber":0`) || !strings.Contains(jsonText, `"result":null`) ||
		!strings.Contains(jsonText, `"memberPosition"`) {
		t.Fatalf("snapshot JSON = %s, want explicit language-independent fields", jsonText)
	}
	snapshot.Sides[0].ActiveMembers[0] = 2
	snapshot.Sides[0].Members[0].CurrentHP = 1
	active, _ := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if active.CurrentHP != 100 {
		t.Fatalf("state HP = %d after snapshot mutation, want 100", active.CurrentHP)
	}
}

func TestNewStateRejectsNegativeBadPoisonCounter(t *testing.T) {
	t.Parallel()

	poisoned := newMember(1, "00000000-0000-0000-0000-000000000001", 100, 100)
	poisoned.MajorStatus = battleengine.MajorStatusBadPoison
	poisoned.BadPoisonCounter = -1
	_, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "standard-single", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{
				Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1},
				Members: []battleengine.MemberSnapshot{poisoned},
			},
			{
				Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1},
				Members: []battleengine.MemberSnapshot{
					newMember(1, "00000000-0000-0000-0000-000000000002", 100, 100),
				},
			},
		},
	})
	if err == nil || !strings.Contains(err.Error(), "剧毒状态必须携带正数伤害计数") {
		t.Fatalf("NewState() error = %v, want positive bad poison counter validation error", err)
	}
}

func newMember(position battleengine.MemberPosition, creatureID string, maxHP, currentHP uint32) battleengine.MemberSnapshot {
	return battleengine.MemberSnapshot{
		Position:   position,
		CreatureID: testID(creatureID),
		Level:      50,
		MaxHP:      maxHP,
		CurrentHP:  currentHP,
		Stats: battleengine.StatBlock{
			Attack:         100,
			Defense:        100,
			SpecialAttack:  100,
			SpecialDefense: 100,
			Speed:          100,
		},
		ElementIDs: testIDs("00000000-0000-0000-0000-000000000010"),
		Skills: []battleengine.SkillSnapshot{
			{TargetScope: battleengine.SkillTargetScopeSelectedTarget, DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1,
				SkillID:     testID("00000000-0000-0000-0000-000000000020"),
				Name:        "撞击",
				ElementID:   testID("00000000-0000-0000-0000-000000000010"),
				DamageClass: battleengine.DamageClassPhysical,
				Power:       40,
				Accuracy:    100,
				RemainingPP: 35,
				MaxPP:       35,
			},
		},
	}
}
