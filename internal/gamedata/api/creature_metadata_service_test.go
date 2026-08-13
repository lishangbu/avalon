package api_test

import (
	"context"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"log/slog"
	"testing"

	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	gameapi "github.com/lishangbu/avalon/internal/gamedata/api"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

func TestKratosServiceListsCreatureSpeciesWithoutReturningTheGlobalAggregate(t *testing.T) {
	accountID := snowflake.NewTestID()
	speciesID := snowflake.NewTestID()
	stub := &creatureAdministrationStub{speciesPage: creaturemetadata.SpeciesPage{Items: []creaturemetadata.ManagedSpecies{{Species: creaturemetadata.Species{ID: speciesID, NationalDexNumber: 1, Code: "bulbasaur", Name: "妙蛙种子", Description: creatureStringPointer("基础介绍"), Profile: creatureStringPointer("外形与生态"), DesignOrigin: creatureStringPointer("设计原型"), Trivia: creatureStringPointer("补充知识"), Enabled: true}, Version: 2}}, Total: 1, Page: 1, PageSize: 20}}
	service := gameapi.NewKratosService(gameapi.NativeServices{CreatureAdministration: stub}, slog.Default())
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.ListGameCreatureSpecies(ctx, &domainv1.ListGameCreatureSpeciesRequest{Page: 1, PageSize: 20, Q: "妙蛙"})
	if err != nil {
		t.Fatalf("ListGameCreatureSpecies() error = %v", err)
	}
	species := response.GetBody().GetItems()[0]
	if response.GetBody().GetTotal() != "1" || species.GetNationalDexNumber() != 1 || species.GetVersion() != "2" || species.GetDescription() != "基础介绍" || species.GetProfile() != "外形与生态" || species.GetDesignOrigin() != "设计原型" || species.GetTrivia() != "补充知识" || stub.speciesQuery.Q != "妙蛙" {
		t.Fatalf("ListGameCreatureSpecies() = %+v, query = %+v", response.GetBody(), stub.speciesQuery)
	}
}

func TestKratosServiceMapsSpeciesReferenceCopyFromUpdateContract(t *testing.T) {
	accountID := snowflake.NewTestID()
	speciesID := snowflake.NewTestID()
	stub := &creatureAdministrationStub{}
	service := gameapi.NewKratosService(gameapi.NativeServices{CreatureAdministration: stub}, slog.Default())
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	_, err := service.UpdateGameCreatureSpecies(ctx, &domainv1.UpdateGameCreatureSpeciesRequest{
		SpeciesId:            speciesID.String(),
		HeaderIdempotencyKey: "update-species-copy",
		Body: &domainv1.UpdateGameCreatureSpeciesBody{
			NationalDexNumber: 1, Code: "bulbasaur", Name: "妙蛙种子", Description: "基础介绍", Profile: "外形与生态",
			DesignOrigin: "设计原型", Trivia: "补充知识", ExpectedVersion: "2", Enabled: true,
		},
	})
	if err != nil {
		t.Fatalf("UpdateGameCreatureSpecies() error = %v", err)
	}
	actual := stub.updateSpeciesCommand.Species
	if actual.Description == nil || *actual.Description != "基础介绍" || actual.Profile == nil || *actual.Profile != "外形与生态" || actual.DesignOrigin == nil || *actual.DesignOrigin != "设计原型" || actual.Trivia == nil || *actual.Trivia != "补充知识" {
		t.Fatalf("UpdateGameCreatureSpecies() Species = %+v", actual)
	}
}

func TestKratosServiceListsCreatureCaptureAndHatchFacts(t *testing.T) {
	accountID := snowflake.NewTestID()
	creatureID := snowflake.NewTestID()
	speciesID := snowflake.NewTestID()
	stub := &creatureAdministrationStub{creaturePage: creaturemetadata.CreaturePage{
		Items: []creaturemetadata.ManagedCreature{{Creature: creaturemetadata.Creature{
			ID: creatureID, SpeciesID: speciesID, Code: "bulbasaur", Name: "妙蛙种子",
			CaptureRate: creatureInt32Pointer(45), HatchCycles: creatureInt32Pointer(20), GenderRatio: &creaturemetadata.GenderRatio{MaleEighths: 7, FemaleEighths: 1}, Enabled: true,
		}, Version: 3}},
		Total: 1, Page: 1, PageSize: 20,
	}}
	service := gameapi.NewKratosService(gameapi.NativeServices{CreatureAdministration: stub}, slog.Default())
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.ListGameCreatures(ctx, &domainv1.ListGameCreaturesRequest{Page: 1, PageSize: 20})
	if err != nil {
		t.Fatalf("ListGameCreatures() error = %v", err)
	}
	creature := response.GetBody().GetItems()[0]
	if creature.CaptureRate == nil || creature.GetCaptureRate() != 45 || creature.HatchCycles == nil || creature.GetHatchCycles() != 20 {
		t.Fatalf("ListGameCreatures() creature = %+v", creature)
	}
}

func TestKratosServiceReadsAndReplacesStructuredCreatureEvolution(t *testing.T) {
	accountID := snowflake.NewTestID()
	fromCreatureID := snowflake.NewTestID()
	toCreatureID := snowflake.NewTestID()
	triggerItemID := snowflake.NewTestID()
	requiredSkillID := snowflake.NewTestID()
	evolutionID := snowflake.NewTestID()
	stub := &creatureAdministrationStub{relations: creaturemetadata.CreatureRelations{Evolutions: []creaturemetadata.Evolution{{
		ID: evolutionID, FromCreatureID: fromCreatureID, ToCreatureID: toCreatureID, TriggerType: creaturemetadata.EvolutionTriggerLevel,
		MinimumLevel: creatureInt32Pointer(16), TriggerItemID: &triggerItemID, MinimumFriendship: creatureInt32Pointer(220), RequiredSkillID: &requiredSkillID,
		ConditionText: "等级16以上", Enabled: true, Version: 4,
	}}}}
	service := gameapi.NewKratosService(gameapi.NativeServices{CreatureAdministration: stub}, slog.Default())
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	read, err := service.GetGameCreatureRelations(ctx, &domainv1.GetGameCreatureRelationsRequest{CreatureId: fromCreatureID.String()})
	if err != nil {
		t.Fatalf("GetGameCreatureRelations() error = %v", err)
	}
	evolution := read.GetBody().GetEvolutions()[0]
	if evolution.GetToCreatureId() != toCreatureID.String() || evolution.GetTriggerType() != "level" || evolution.GetMinimumLevel() != 16 || evolution.GetTriggerItemId() != triggerItemID.String() || evolution.GetRequiredSkillId() != requiredSkillID.String() || evolution.GetConditionText() != "等级16以上" {
		t.Fatalf("GetGameCreatureRelations() evolution = %+v", evolution)
	}

	_, err = service.ReplaceGameCreatureRelations(ctx, &domainv1.ReplaceGameCreatureRelationsRequest{
		CreatureId: fromCreatureID.String(), HeaderIdempotencyKey: "replace-evolution",
		Body: &domainv1.ReplaceGameCreatureRelationsBody{Relations: &domainv1.GameCreatureRelations{Evolutions: []*domainv1.GameCreatureEvolution{{
			FromCreatureId: fromCreatureID.String(), ToCreatureId: toCreatureID.String(), TriggerType: "level", MinimumLevel: creatureInt32Pointer(16),
			ConditionText: "等级16以上", Enabled: true,
		}}}},
	})
	if err != nil {
		t.Fatalf("ReplaceGameCreatureRelations() error = %v", err)
	}
	if stub.replaceCommand.CreatureID != fromCreatureID || len(stub.replaceCommand.Relations.Evolutions) != 1 || stub.replaceCommand.Relations.Evolutions[0].FromCreatureID != fromCreatureID || stub.replaceCommand.Relations.Evolutions[0].ToCreatureID != toCreatureID {
		t.Fatalf("ReplaceGameCreatureRelations() command = %+v", stub.replaceCommand)
	}
}

type creatureAdministrationStub struct {
	speciesQuery         creaturemetadata.SpeciesListQuery
	speciesPage          creaturemetadata.SpeciesPage
	creaturePage         creaturemetadata.CreaturePage
	relations            creaturemetadata.CreatureRelations
	replaceCommand       creaturemetadata.ReplaceRelationsCommand
	updateSpeciesCommand creaturemetadata.UpdateSpeciesCommand
}

func (s *creatureAdministrationStub) GetReferenceOptions(context.Context) (creaturemetadata.ReferenceOptions, error) {
	return creaturemetadata.ReferenceOptions{}, nil
}

func (s *creatureAdministrationStub) ListSpecies(_ context.Context, query creaturemetadata.SpeciesListQuery) (creaturemetadata.SpeciesPage, error) {
	s.speciesQuery = query
	return s.speciesPage, nil
}
func (s *creatureAdministrationStub) GetSpecies(context.Context, snowflake.ID) (creaturemetadata.ManagedSpecies, error) {
	return creaturemetadata.ManagedSpecies{}, nil
}
func (s *creatureAdministrationStub) CreateSpecies(context.Context, creaturemetadata.CreateSpeciesCommand) (creaturemetadata.ManagedSpecies, error) {
	return creaturemetadata.ManagedSpecies{}, nil
}
func (s *creatureAdministrationStub) UpdateSpecies(_ context.Context, command creaturemetadata.UpdateSpeciesCommand) (creaturemetadata.ManagedSpecies, error) {
	s.updateSpeciesCommand = command
	return creaturemetadata.ManagedSpecies{Species: command.Species, Version: command.ExpectedVersion + 1}, nil
}
func (s *creatureAdministrationStub) ListCreatures(context.Context, creaturemetadata.CreatureListQuery) (creaturemetadata.CreaturePage, error) {
	return s.creaturePage, nil
}
func (s *creatureAdministrationStub) GetCreature(context.Context, snowflake.ID) (creaturemetadata.ManagedCreature, error) {
	return creaturemetadata.ManagedCreature{}, nil
}
func (s *creatureAdministrationStub) CreateCreature(context.Context, creaturemetadata.CreateCreatureCommand) (creaturemetadata.ManagedCreature, error) {
	return creaturemetadata.ManagedCreature{}, nil
}
func (s *creatureAdministrationStub) UpdateCreature(context.Context, creaturemetadata.UpdateCreatureCommand) (creaturemetadata.ManagedCreature, error) {
	return creaturemetadata.ManagedCreature{}, nil
}
func (s *creatureAdministrationStub) GetCreatureRelations(context.Context, snowflake.ID) (creaturemetadata.CreatureRelations, error) {
	return s.relations, nil
}
func (s *creatureAdministrationStub) ReplaceRelations(_ context.Context, command creaturemetadata.ReplaceRelationsCommand) (creaturemetadata.CreatureRelations, error) {
	s.replaceCommand = command
	return command.Relations, nil
}

func creatureInt32Pointer(value int32) *int32    { return &value }
func creatureStringPointer(value string) *string { return &value }
