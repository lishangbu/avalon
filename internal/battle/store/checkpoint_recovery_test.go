package store

import (
	"encoding/json"
	"testing"
	"time"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// TestEncounterTerminalCommand 验证正常 Encounter 终局生成生命写回命令且仅明确败局标记恢复。
func TestEncounterTerminalCommand(t *testing.T) {
	t.Parallel()

	completedAt := time.Date(2026, time.August, 13, 12, 0, 0, 0, time.UTC)
	playerID := snowflake.MustParse("1048576001")
	creatureID := snowflake.MustParse("1048576002")
	reserveCreatureID := snowflake.MustParse("1048576004")
	base := battle.Battle{
		ID:          snowflake.MustParse("1048576003"),
		Mode:        battle.BattleModePvE,
		SourceType:  battle.BattleSourceEncounter,
		Status:      battle.StatusCompleted,
		CompletedAt: completedAt,
		Participants: []battle.Participant{
			{
				Side:              battle.ParticipantSideOne,
				PlayerCharacterID: playerID,
				Party: &battle.PartyBattleSnapshot{Members: []battle.PartyBattleSnapshotMember{
					{Position: 1, PlayerCharacterCreatureID: creatureID, CurrentHP: 60, MaximumHP: 87},
					{Position: 2, PlayerCharacterCreatureID: reserveCreatureID, CurrentHP: 44, MaximumHP: 70},
				}},
			},
			{Side: battle.ParticipantSideTwo, IsBot: true},
		},
	}
	withResult := func(value battle.Battle, winner battle.ParticipantSide, reason battle.TerminalReason) battle.Battle {
		value.Result, _ = json.Marshal(battle.Result{WinnerSide: winner, Reason: reason})
		return value
	}

	tests := []struct {
		name     string
		mutate   func(battle.Battle) battle.Battle
		want     bool
		defeated bool
	}{
		{name: "Bot 正常获胜", mutate: func(value battle.Battle) battle.Battle {
			return withResult(value, battle.ParticipantSideTwo, battle.TerminalReasonBattleEnded)
		}, want: true, defeated: true},
		{name: "玩家认输", mutate: func(value battle.Battle) battle.Battle {
			return withResult(value, battle.ParticipantSideTwo, battle.TerminalReasonSurrender)
		}, want: true, defeated: true},
		{name: "玩家获胜", mutate: func(value battle.Battle) battle.Battle {
			return withResult(value, battle.ParticipantSideOne, battle.TerminalReasonBattleEnded)
		}, want: true},
		{name: "平局", mutate: func(value battle.Battle) battle.Battle {
			return withResult(value, 0, battle.TerminalReasonDraw)
		}, want: true},
		{name: "No Contest", mutate: func(value battle.Battle) battle.Battle {
			return withResult(value, 0, battle.TerminalReasonNoContest)
		}, want: true},
		{name: "异常终局即使携带胜方", mutate: func(value battle.Battle) battle.Battle {
			return withResult(value, battle.ParticipantSideTwo, battle.TerminalReasonRuntimeFailed)
		}},
		{name: "Training", mutate: func(value battle.Battle) battle.Battle {
			value.SourceType = battle.BattleSourceTraining
			return withResult(value, battle.ParticipantSideTwo, battle.TerminalReasonBattleEnded)
		}},
		{name: "PvP", mutate: func(value battle.Battle) battle.Battle {
			value.Mode = battle.BattleModePvP
			return withResult(value, battle.ParticipantSideTwo, battle.TerminalReasonBattleEnded)
		}},
		{name: "中断", mutate: func(value battle.Battle) battle.Battle {
			value.Status = battle.StatusInterrupted
			return withResult(value, battle.ParticipantSideTwo, battle.TerminalReasonBattleEnded)
		}},
		{name: "取消", mutate: func(value battle.Battle) battle.Battle {
			value.Status = battle.StatusCanceled
			return withResult(value, battle.ParticipantSideTwo, battle.TerminalReasonBattleEnded)
		}},
		{name: "成员快照无效", mutate: func(value battle.Battle) battle.Battle {
			value.Participants[0].Party.Members[0].MaximumHP = 0
			return withResult(value, battle.ParticipantSideTwo, battle.TerminalReasonBattleEnded)
		}},
	}

	for _, test := range tests {
		test := test
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			value := base
			value.Participants = append([]battle.Participant(nil), base.Participants...)
			party := *base.Participants[0].Party
			party.Members = append([]battle.PartyBattleSnapshotMember(nil), base.Participants[0].Party.Members...)
			value.Participants[0].Party = &party
			summary := battleengine.StateSummary{Members: []battleengine.MemberStateSummary{{Side: battleengine.SideOne, MemberPosition: 1, MaxHP: 174, CurrentHP: 44}}}
			command, ok, err := encounterTerminalCommand(test.mutate(value), summary)
			if err != nil {
				t.Fatalf("encounterTerminalCommand() error = %v", err)
			}
			if ok != test.want {
				t.Fatalf("encounterTerminalCommand() ok = %v, want %v", ok, test.want)
			}
			if !test.want {
				return
			}
			if command.BattleID != base.ID || command.PlayerCharacterID != playerID || !command.CompletedAt.Equal(completedAt) ||
				command.Defeated != test.defeated || len(command.Members) != 2 || command.Members[0].PlayerCharacterCreatureID != creatureID || command.Members[0].CurrentHP != 22 || command.Members[0].MaximumHP != 87 ||
				command.Members[1].PlayerCharacterCreatureID != reserveCreatureID || command.Members[1].CurrentHP != 44 || command.Members[1].MaximumHP != 70 {
				t.Fatalf("encounterTerminalCommand() = %+v", command)
			}
		})
	}
}
