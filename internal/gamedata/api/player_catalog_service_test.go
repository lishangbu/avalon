package api

import (
	"context"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
)

func TestPlayerCatalogServiceBuildsEnabledLiveBattleContent(t *testing.T) {
	t.Parallel()

	clauseID := snowflake.MustParse("1048576157")
	formatID := snowflake.MustParse("1048576158")
	rules := &playerBattleCatalogStub{
		clauses: []battleformat.Clause{{
			ID: clauseID, Code: "species-unique", Name: "种类唯一",
			Definition: effect.Definition{Kind: effect.KindUniqueSpeciesClause, SchemaVersion: 1, Parameters: []byte(`{}`)},
			Enabled:    true, Version: 1,
		}},
		formats: []battleformat.Format{{
			ID: formatID, Code: "standard-single", Name: "标准单打", Mode: battleformat.ModeSingle,
			RosterCount: 6, SelectCount: 3, ActiveParticipantsPerSide: 1,
			LevelRule:    battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)},
			Deadlines:    battleformat.Deadlines{PreviewSeconds: 90, TurnSeconds: 60, BattleSeconds: 1200},
			Availability: battleformat.Availability{Challenge: true, Training: true, AdminPreview: true},
			Default:      true, Enabled: true, Version: 1,
		}},
	}
	service := NewPlayerCatalogService(rules, nil)

	response, err := service.GetPlayerCatalog(context.Background(), &domainv1.GetPlayerCatalogRequest{})
	if err != nil {
		t.Fatalf("GetPlayerCatalog() error = %v", err)
	}
	body := response.GetBody()
	if body.GetSchemaVersion() != playerCatalogSchemaVersion {
		t.Fatalf("玩家目录状态 = %+v", body)
	}
	if len(body.GetPayload().GetBattleClauses()) != 1 || body.GetPayload().GetBattleClauses()[0].GetId() != clauseID.String() ||
		len(body.GetPayload().GetBattleFormats()) != 1 || body.GetPayload().GetBattleFormats()[0].GetId() != formatID.String() {
		t.Fatalf("玩家目录未完整投影: %+v", body.GetPayload())
	}
	if !rules.enabledOnly {
		t.Fatal("玩家目录查询未限制为启用资料")
	}
}

type playerBattleCatalogStub struct {
	clauses     []battleformat.Clause
	formats     []battleformat.Format
	enabledOnly bool
}

func (stub *playerBattleCatalogStub) ListClauses(_ context.Context, query battleformat.ClauseListQuery) (battleformat.ClausePage, error) {
	stub.enabledOnly = query.Enabled != nil && *query.Enabled
	return battleformat.ClausePage{Items: stub.clauses, Total: int64(len(stub.clauses)), Page: query.Page, PageSize: query.PageSize}, nil
}

func (stub *playerBattleCatalogStub) ListRestrictions(_ context.Context, query battleformat.RestrictionListQuery) (battleformat.RestrictionPage, error) {
	stub.enabledOnly = stub.enabledOnly && query.Enabled != nil && *query.Enabled
	return battleformat.RestrictionPage{Items: []battleformat.Restriction{}, Page: query.Page, PageSize: query.PageSize}, nil
}

func (stub *playerBattleCatalogStub) ListMechanics(_ context.Context, query battleformat.MechanicListQuery) (battleformat.MechanicPage, error) {
	stub.enabledOnly = stub.enabledOnly && query.Enabled != nil && *query.Enabled
	return battleformat.MechanicPage{Items: []battleformat.Mechanic{}, Page: query.Page, PageSize: query.PageSize}, nil
}

func (stub *playerBattleCatalogStub) ListFormats(_ context.Context, query battleformat.FormatListQuery) (battleformat.FormatPage, error) {
	stub.enabledOnly = stub.enabledOnly && query.Enabled != nil && *query.Enabled
	return battleformat.FormatPage{Items: stub.formats, Total: int64(len(stub.formats)), Page: query.Page, PageSize: query.PageSize}, nil
}

func int32Pointer(value int32) *int32 { return &value }
