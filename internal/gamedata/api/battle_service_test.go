package api_test

import (
	"context"
	"log/slog"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	gameapi "github.com/lishangbu/avalon/internal/gamedata/api"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// TestKratosServiceCreatesBattleRestriction 验证名单限制保持独立强类型参数，
// 并把管理员、全局修订和幂等键完整映射到领域命令。
func TestKratosServiceCreatesBattleRestriction(t *testing.T) {
	t.Parallel()
	accountID := snowflake.MustParse("1048576126")
	restrictionID := snowflake.MustParse("1048576127")
	battles := &battleRuleStub{restriction: battleformat.Restriction{
		ID: restrictionID, Code: "allow-creatures", Name: "允许生物", Enabled: true, Version: 1,
		Definition: effect.Definition{Kind: effect.KindStableCodeListRestriction, SchemaVersion: 1},
	}}
	service := gameapi.NewKratosService(gameapi.NativeServices{BattleRules: battles}, slog.Default())
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.CreateBattleRestriction(ctx, &domainv1.CreateBattleRestrictionRequest{
		HeaderIdempotencyKey: "create-restriction",
		Body: &domainv1.CreateGameBattleRestrictionRequest{
			Code: "allow-creatures", Name: "允许生物", Enabled: true, Definition: &domainv1.BattleRestrictionEffectDefinition{
				Kind: effect.KindStableCodeListRestriction, SchemaVersion: 1,
				Parameters: &domainv1.StableCodeListParameters{Mode: "allow", ResourceType: "creature", StableCodes: []string{"bulbasaur"}},
			},
		},
	})
	if err != nil {
		t.Fatalf("CreateBattleRestriction() error = %v", err)
	}
	if response.GetHttpStatusCode() != 201 || response.GetBody().GetId() != restrictionID.String() {
		t.Fatalf("CreateBattleRestriction() = %#v", response)
	}
	if battles.createRestriction.ActorAccountID != accountID || battles.createRestriction.IdempotencyKey != "create-restriction" {
		t.Fatalf("CreateRestriction command = %+v", battles.createRestriction)
	}
	if string(battles.createRestriction.Definition.Parameters) != `{"mode":"allow","resourceType":"creature","stableCodes":["bulbasaur"]}` {
		t.Fatalf("Definition parameters = %s", battles.createRestriction.Definition.Parameters)
	}
}

// battleRuleStub 实现四类对战资料的独立应用边界，仅记录本测试关心的限制命令。
type battleRuleStub struct {
	createRestriction battleformat.CreateRestrictionCommand
	restriction       battleformat.Restriction
}

func (*battleRuleStub) CreateClause(context.Context, battleformat.CreateClauseCommand) (battleformat.Clause, error) {
	return battleformat.Clause{}, nil
}
func (*battleRuleStub) UpdateClause(context.Context, battleformat.UpdateClauseCommand) (battleformat.Clause, error) {
	return battleformat.Clause{}, nil
}
func (*battleRuleStub) GetClause(context.Context, snowflake.ID) (battleformat.Clause, error) {
	return battleformat.Clause{}, nil
}
func (*battleRuleStub) ListClauses(context.Context, battleformat.ClauseListQuery) (battleformat.ClausePage, error) {
	return battleformat.ClausePage{}, nil
}
func (stub *battleRuleStub) CreateRestriction(_ context.Context, command battleformat.CreateRestrictionCommand) (battleformat.Restriction, error) {
	stub.createRestriction = command
	return stub.restriction, nil
}
func (*battleRuleStub) UpdateRestriction(context.Context, battleformat.UpdateRestrictionCommand) (battleformat.Restriction, error) {
	return battleformat.Restriction{}, nil
}
func (*battleRuleStub) GetRestriction(context.Context, snowflake.ID) (battleformat.Restriction, error) {
	return battleformat.Restriction{}, nil
}
func (*battleRuleStub) ListRestrictions(context.Context, battleformat.RestrictionListQuery) (battleformat.RestrictionPage, error) {
	return battleformat.RestrictionPage{}, nil
}
func (*battleRuleStub) CreateMechanic(context.Context, battleformat.CreateMechanicCommand) (battleformat.Mechanic, error) {
	return battleformat.Mechanic{}, nil
}
func (*battleRuleStub) UpdateMechanic(context.Context, battleformat.UpdateMechanicCommand) (battleformat.Mechanic, error) {
	return battleformat.Mechanic{}, nil
}
func (*battleRuleStub) GetMechanic(context.Context, snowflake.ID) (battleformat.Mechanic, error) {
	return battleformat.Mechanic{}, nil
}
func (*battleRuleStub) ListMechanics(context.Context, battleformat.MechanicListQuery) (battleformat.MechanicPage, error) {
	return battleformat.MechanicPage{}, nil
}
func (*battleRuleStub) CreateFormat(context.Context, battleformat.CreateFormatCommand) (battleformat.Format, error) {
	return battleformat.Format{}, nil
}
func (*battleRuleStub) UpdateFormat(context.Context, battleformat.UpdateFormatCommand) (battleformat.Format, error) {
	return battleformat.Format{}, nil
}
func (*battleRuleStub) GetFormat(context.Context, snowflake.ID) (battleformat.Format, error) {
	return battleformat.Format{}, nil
}
func (*battleRuleStub) ListFormats(context.Context, battleformat.FormatListQuery) (battleformat.FormatPage, error) {
	return battleformat.FormatPage{}, nil
}
