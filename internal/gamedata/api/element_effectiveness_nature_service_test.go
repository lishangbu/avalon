package api_test

import (
	"context"
	"io"
	"log/slog"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	gameapi "github.com/lishangbu/avalon/internal/gamedata/api"
	"github.com/lishangbu/avalon/internal/gamedata/elementeffectiveness"
	"github.com/lishangbu/avalon/internal/gamedata/nature"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

func TestKratosServiceCreatesElementEffectivenessAndNatureThroughIndependentContracts(t *testing.T) {
	t.Parallel()
	accountID, attackID, defenseID, effectivenessID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	natureID, increasedID, decreasedID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	effectivenessService := &elementEffectivenessServiceStub{created: elementeffectiveness.Effectiveness{ID: effectivenessID, AttackElementID: attackID, DefenseElementID: defenseID, Numerator: 2, Denominator: 1, Enabled: true, Version: 1}}
	natureService := &natureServiceStub{created: nature.Nature{ID: natureID, Code: "brave", Name: "勇敢", IncreasedStatID: &increasedID, DecreasedStatID: &decreasedID, Enabled: true, Version: 1}}
	service := gameapi.NewKratosService(gameapi.NativeServices{ElementEffectiveness: effectivenessService, Natures: natureService}, slog.New(slog.NewTextHandler(io.Discard, nil)))
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	effectivenessResponse, err := service.CreateGameElementEffectiveness(ctx, &domainv1.CreateGameElementEffectivenessRequest{HeaderIdempotencyKey: "create-effectiveness", Body: &domainv1.CreateGameElementEffectivenessBody{AttackElementId: attackID.String(), DefenseElementId: defenseID.String(), Numerator: 2, Denominator: 1, Enabled: true}})
	if err != nil || effectivenessResponse.GetBody().GetId() != effectivenessID.String() || effectivenessService.command.ActorAccountID != accountID {
		t.Fatalf("CreateGameElementEffectiveness() = %+v, command = %+v, error = %v", effectivenessResponse, effectivenessService.command, err)
	}
	natureResponse, err := service.CreateGameNature(ctx, &domainv1.CreateGameNatureRequest{HeaderIdempotencyKey: "create-nature", Body: &domainv1.CreateGameNatureBody{Code: "brave", Name: "勇敢", IncreasedStatId: increasedID.String(), DecreasedStatId: decreasedID.String(), Enabled: true}})
	if err != nil || natureResponse.GetBody().GetId() != natureID.String() || natureService.command.IncreasedStatID == nil || *natureService.command.IncreasedStatID != increasedID {
		t.Fatalf("CreateGameNature() = %+v, command = %+v, error = %v", natureResponse, natureService.command, err)
	}
}

type elementEffectivenessServiceStub struct {
	created elementeffectiveness.Effectiveness
	command elementeffectiveness.CreateCommand
}

func (s *elementEffectivenessServiceStub) Create(_ context.Context, command elementeffectiveness.CreateCommand) (elementeffectiveness.Effectiveness, error) {
	s.command = command
	return s.created, nil
}
func (*elementEffectivenessServiceStub) Get(context.Context, snowflake.ID) (elementeffectiveness.Effectiveness, error) {
	return elementeffectiveness.Effectiveness{}, nil
}
func (*elementEffectivenessServiceStub) List(context.Context, elementeffectiveness.ListQuery) (elementeffectiveness.Page, error) {
	return elementeffectiveness.Page{}, nil
}
func (*elementEffectivenessServiceStub) Update(context.Context, elementeffectiveness.UpdateCommand) (elementeffectiveness.Effectiveness, error) {
	return elementeffectiveness.Effectiveness{}, nil
}
func (*elementEffectivenessServiceStub) ListEnabled(context.Context) ([]elementeffectiveness.Effectiveness, error) {
	return nil, nil
}

type natureServiceStub struct {
	created nature.Nature
	command nature.CreateCommand
}

func (s *natureServiceStub) Create(_ context.Context, command nature.CreateCommand) (nature.Nature, error) {
	s.command = command
	return s.created, nil
}
func (*natureServiceStub) Get(context.Context, snowflake.ID) (nature.Nature, error) {
	return nature.Nature{}, nil
}
func (*natureServiceStub) List(context.Context, nature.ListQuery) (nature.Page, error) {
	return nature.Page{}, nil
}
func (*natureServiceStub) Update(context.Context, nature.UpdateCommand) (nature.Nature, error) {
	return nature.Nature{}, nil
}
