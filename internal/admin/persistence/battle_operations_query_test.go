package persistence

import (
	"encoding/json"
	"testing"

	"github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/team"
)

// TestFrozenParticipantMembers 验证运维投影只读取不可变快照，并对损坏或未知输入明确失败。
func TestFrozenParticipantMembers(t *testing.T) {
	creatureID := snowflake.NewTestID()
	ownedID := snowflake.NewTestID()
	teamSnapshot := battle.TeamSnapshot{SourceTeamID: snowflake.NewTestID(), SourceTeamVersion: 4, Members: []team.Member{{
		Position: 1, CreatureID: creatureID, AbilityID: snowflake.NewTestID(),
		TeraElementID: snowflake.NewTestID(), NatureID: snowflake.NewTestID(), Level: 17,
		Skills: []team.MemberSkill{{Position: 1, SkillID: snowflake.NewTestID()}},
	}}}
	partySnapshot := battle.PartyBattleSnapshot{
		PartyID: snowflake.NewTestID(), Version: 4, Team: teamSnapshot,
		Members: []battle.PartyBattleSnapshotMember{{
			Position: 1, PlayerCharacterCreatureID: ownedID, CurrentHP: 23, MaximumHP: 51,
		}},
	}
	teamPayload, err := json.Marshal(teamSnapshot)
	if err != nil {
		t.Fatalf("编码 Team 快照: %v", err)
	}
	partyPayload, err := json.Marshal(partySnapshot)
	if err != nil {
		t.Fatalf("编码 Party 快照: %v", err)
	}
	tests := []struct {
		name      string
		inputType string
		payload   json.RawMessage
		wantOwned snowflake.ID
		wantHP    int32
		wantErr   bool
	}{
		{name: "Party 映射 Owned Creature 生命", inputType: "party", payload: partyPayload, wantOwned: ownedID, wantHP: 23},
		{name: "Generated 只暴露冻结 Team", inputType: "generated", payload: teamPayload},
		{name: "损坏 JSON", inputType: "party", payload: json.RawMessage(`{"team":`), wantErr: true},
		{name: "未知输入类型", inputType: "legacy", payload: teamPayload, wantErr: true},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			members, err := frozenParticipantMembers(test.inputType, test.payload)
			if (err != nil) != test.wantErr {
				t.Fatalf("frozenParticipantMembers() error = %v, wantErr %v", err, test.wantErr)
			}
			if test.wantErr {
				return
			}
			if len(members) != 1 || members[0].CreatureID != creatureID || members[0].Level != 17 ||
				members[0].PlayerCharacterCreatureID != test.wantOwned || members[0].CurrentHP != test.wantHP {
				t.Fatalf("frozenParticipantMembers() = %+v", members)
			}
		})
	}
}
