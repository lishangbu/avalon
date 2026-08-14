package team_test

import (
	"context"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/team"
)

// TestAdmissionServiceReloadsAndValidatesOwnedTeam 验证对战入口不会信任客户端 Team 内容。
func TestAdmissionServiceReloadsAndValidatesOwnedTeam(t *testing.T) {
	t.Parallel()
	accountID, characterID, teamID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	creatureID, abilityID, elementID, skillID, statID, natureID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	member := team.Member{Position: 1, CreatureID: creatureID, AbilityID: abilityID, TeraElementID: elementID, NatureID: natureID,
		Skills: []team.MemberSkill{{Position: 1, SkillID: skillID}}, Stats: []team.MemberStat{{StatID: statID}}}
	queries := team.NewQueryService(&admissionTeamAdaptersStub{value: team.Team{
		ID: teamID, PlayerCharacterID: characterID, Members: []team.Member{member},
	}}, &admissionTeamAdaptersStub{value: team.Team{
		ID: teamID, PlayerCharacterID: characterID, Members: []team.Member{member},
	}})

	validator := team.NewCatalogValidator(&admissionCatalogStub{catalog: team.ReferenceCatalog{
		Elements:  []team.Reference{{ID: elementID, Enabled: true}},
		Abilities: []team.Reference{{ID: abilityID, Enabled: true}},
		Skills:    []team.Reference{{ID: skillID, Enabled: true}},
		Stats:     []team.Reference{{ID: statID, Enabled: true}},
		Natures:   []team.Reference{{ID: natureID, Enabled: true}},
		CreatureMetadata: creaturemetadata.Data{
			Creatures:   []creaturemetadata.Creature{{ID: creatureID, Enabled: true}},
			Abilities:   []creaturemetadata.AbilityBinding{{CreatureID: creatureID, AbilityID: abilityID}},
			SkillLearns: []creaturemetadata.SkillLearn{{CreatureID: creatureID, SkillID: skillID}},
			Stats:       []creaturemetadata.StatBinding{{CreatureID: creatureID, StatID: statID}},
		},
	}})
	value, err := team.NewAdmissionService(queries, validator).ValidateOwned(context.Background(), accountID, characterID, teamID)
	if err != nil || value.ID != teamID || len(value.Members) != 1 {
		t.Fatalf("ValidateOwned() = %+v, error = %v", value, err)
	}
}

type admissionTeamAdaptersStub struct{ value team.Team }

func (stub *admissionTeamAdaptersStub) GetOwned(context.Context, snowflake.ID, snowflake.ID, snowflake.ID) (team.Team, error) {
	return stub.value, nil
}
func (stub *admissionTeamAdaptersStub) ListOwned(context.Context, snowflake.ID, snowflake.ID) ([]team.Team, error) {
	return nil, nil
}
func (stub *admissionTeamAdaptersStub) GetActive(context.Context, snowflake.ID, snowflake.ID) (team.ActiveBinding, error) {
	return team.ActiveBinding{}, nil
}

type admissionCatalogStub struct{ catalog team.ReferenceCatalog }

func (stub *admissionCatalogStub) Current(context.Context) (team.ReferenceCatalog, error) {
	return stub.catalog, nil
}
