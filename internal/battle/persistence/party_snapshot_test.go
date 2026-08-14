package persistence

import (
	"encoding/json"
	"testing"
	"time"

	avalonent "github.com/lishangbu/avalon/ent"
	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/team"
)

// TestBattleFromEntLoadsPartyAsExecutableTeam 验证 Party 输入重载后同时保留恢复事实与完整可执行 Team。
func TestBattleFromEntLoadsPartyAsExecutableTeam(t *testing.T) {
	t.Parallel()

	now := time.Date(2026, time.August, 13, 12, 0, 0, 0, time.UTC)
	partyID := snowflake.MustParse("1048576020")
	creatureID := snowflake.MustParse("1048576021")
	ownedID := snowflake.MustParse("1048576022")
	snapshot := battle.PartyBattleSnapshot{PartyID: partyID, Version: 3, Team: battle.TeamSnapshot{SourceTeamID: partyID, SourceTeamVersion: 3, Members: []team.Member{{Position: 1, CreatureID: creatureID, AbilityID: snowflake.MustParse("1048576027"), TeraElementID: snowflake.MustParse("1048576028"), NatureID: snowflake.MustParse("1048576029"), Level: 5, Skills: []team.MemberSkill{{Position: 1, SkillID: snowflake.MustParse("1048576030")}}}}}, Members: []battle.PartyBattleSnapshotMember{{Position: 1, PlayerCharacterCreatureID: ownedID, CurrentHP: 30, MaximumHP: 55}}}
	raw, err := json.Marshal(snapshot)
	if err != nil {
		t.Fatalf("json.Marshal() error = %v", err)
	}
	accountID := snowflake.MustParse("1048576023")
	playerID := snowflake.MustParse("1048576024")
	format, _ := json.Marshal(battle.Format{RosterCount: 1, SelectCount: 1, ActiveParticipantsPerSide: 1, PreviewDuration: time.Minute, BattleDuration: 10 * time.Minute})
	session, err := battleFromEnt(&avalonent.Battle{ID: snowflake.MustParse("1048576025"), Mode: "pve", SourceType: "encounter", Status: "running", BattleFormatID: snowflake.MustParse("1048576026"), BattleFormatSnapshot: json.RawMessage(`{}`), Format: format, PreviewDeadlineAt: now.Add(time.Minute), BattleDeadlineAt: now.Add(10 * time.Minute), Version: 1, CreatedAt: now, UpdatedAt: now}, []*avalonent.BattleParticipant{{Side: 1, ParticipantType: "player_character", InputType: "party", AccountID: &accountID, PlayerCharacterID: &playerID, DisplayName: "玩家", SourcePartyID: &partyID, InputSnapshot: raw}}, nil)
	if err != nil {
		t.Fatalf("battleFromEnt() error = %v", err)
	}
	participant := session.Participants[0]
	if participant.Party == nil || participant.Party.Members[0].PlayerCharacterCreatureID != ownedID || len(participant.Team.Members) != 1 || participant.Team.Members[0].CreatureID != creatureID {
		t.Fatalf("battleFromEnt() participant = %+v", participant)
	}
}
