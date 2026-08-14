package creaturemetadata_test

import (
	"context"
	"errors"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"strings"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/creaturemetadata"
)

func TestAdministrationCleansAndBoundsSpeciesReferenceCopy(t *testing.T) {
	repository := &managementRepositoryStub{}
	service := creaturemetadata.NewAdministrationService(repository, snowflake.TestSource(func() snowflake.ID { return snowflake.NewTestID() }), time.Now)
	writeContext := administration.GameDataWriteContext{ActorAccountID: snowflake.NewTestID(), RequestID: "species-copy", IdempotencyKey: "species-copy-1"}

	created, err := service.CreateSpecies(context.Background(), creaturemetadata.CreateSpeciesCommand{
		GameDataWriteContext: writeContext,
		Species:              creaturemetadata.Species{NationalDexNumber: 1, Code: "bulbasaur", Name: "妙蛙种子", Description: stringPointer("  基础介绍  "), Profile: stringPointer("外形与生态"), DesignOrigin: stringPointer("设计原型"), Trivia: stringPointer("补充知识"), Enabled: true},
	})
	if err != nil {
		t.Fatalf("CreateSpecies() error = %v", err)
	}
	if created.Description == nil || *created.Description != "基础介绍" {
		t.Fatalf("CreateSpecies() description = %v", created.Description)
	}

	_, err = service.CreateSpecies(context.Background(), creaturemetadata.CreateSpeciesCommand{
		GameDataWriteContext: writeContext,
		Species:              creaturemetadata.Species{NationalDexNumber: 2, Code: "ivysaur", Name: "妙蛙草", Description: stringPointer(strings.Repeat("文", 4001)), Enabled: true},
	})
	if !errors.Is(err, creaturemetadata.ErrInvalidCreatureMetadata) {
		t.Fatalf("CreateSpecies() oversized reference copy error = %v", err)
	}
}

func stringPointer(value string) *string { return &value }

func TestAdministrationListsSpeciesWithBoundedPagination(t *testing.T) {
	repository := &managementRepositoryStub{speciesPage: creaturemetadata.SpeciesPage{Page: 2, PageSize: 25, Total: 26}}
	service := creaturemetadata.NewAdministrationService(repository, snowflake.TestSource(func() snowflake.ID { return snowflake.NewTestID() }), time.Now)

	page, err := service.ListSpecies(context.Background(), creaturemetadata.SpeciesListQuery{Page: 2, PageSize: 25, Q: "妙蛙"})
	if err != nil {
		t.Fatalf("ListSpecies() error = %v", err)
	}
	if page.Total != 26 || repository.speciesQuery.Q != "妙蛙" {
		t.Fatalf("ListSpecies() = %+v, query = %+v", page, repository.speciesQuery)
	}
}

func TestAdministrationUpdatesOneCreatureWithoutReplacingTheGlobalDataSet(t *testing.T) {
	id := snowflake.NewTestID()
	speciesID := snowflake.NewTestID()
	repository := &managementRepositoryStub{}
	service := creaturemetadata.NewAdministrationService(repository, snowflake.TestSource(func() snowflake.ID { return snowflake.NewTestID() }), func() time.Time {
		return time.Date(2026, 8, 4, 9, 0, 0, 0, time.UTC)
	})

	updated, err := service.UpdateCreature(context.Background(), creaturemetadata.UpdateCreatureCommand{
		GameDataWriteContext: administration.GameDataWriteContext{ActorAccountID: snowflake.NewTestID(), RequestID: "request-1", IdempotencyKey: "key-1"},
		ID:                   id, ExpectedVersion: 3, Code: "bulbasaur", Name: "妙蛙种子", SpeciesID: speciesID,
		GenderRatio: creaturemetadata.GenderRatio{MaleEighths: 7, FemaleEighths: 1}, DefaultForm: true, Enabled: true,
	})
	if err != nil {
		t.Fatalf("UpdateCreature() error = %v", err)
	}
	if updated.ID != id || updated.Version != 4 || repository.updatedCreature.Version != 4 {
		t.Fatalf("UpdateCreature() = %+v, stored = %+v", updated, repository.updatedCreature)
	}
}

func TestAdministrationReplacesOnlyOneCreaturesRelations(t *testing.T) {
	creatureID := snowflake.NewTestID()
	formID := snowflake.NewTestID()
	repository := &managementRepositoryStub{}
	service := creaturemetadata.NewAdministrationService(repository, snowflake.TestSource(func() snowflake.ID { return formID }), time.Now)

	result, err := service.ReplaceRelations(context.Background(), creaturemetadata.ReplaceRelationsCommand{
		GameDataWriteContext: administration.GameDataWriteContext{ActorAccountID: snowflake.NewTestID(), RequestID: "request-relations", IdempotencyKey: "relations-1"},
		CreatureID:           creatureID,
		Relations: creaturemetadata.CreatureRelations{
			Forms:      []creaturemetadata.Form{{Code: "bulbasaur", Name: "默认形态", CreatureID: creatureID, DefaultForm: true, Enabled: true, ElementIDs: []snowflake.ID{snowflake.NewTestID()}}},
			Evolutions: []creaturemetadata.Evolution{{FromCreatureID: creatureID, ToCreatureID: snowflake.NewTestID(), TriggerType: creaturemetadata.EvolutionTriggerLevel, MinimumLevel: int32Pointer(16), ConditionText: "等级16以上", Enabled: true}},
		},
	})
	if err != nil {
		t.Fatalf("ReplaceRelations() error = %v", err)
	}
	if len(result.Forms) != 1 || result.Forms[0].ID != formID || result.Forms[0].Version != 1 || len(result.Evolutions) != 1 || result.Evolutions[0].ID == snowflake.ID(0) {
		t.Fatalf("ReplaceRelations() = %+v", result)
	}
}

func int32Pointer(value int32) *int32 { return &value }

type managementRepositoryStub struct {
	speciesQuery    creaturemetadata.SpeciesListQuery
	speciesPage     creaturemetadata.SpeciesPage
	updatedCreature creaturemetadata.ManagedCreature
	relations       creaturemetadata.CreatureRelations
}

func (s *managementRepositoryStub) GetReferenceOptions(context.Context) (creaturemetadata.ReferenceOptions, error) {
	return creaturemetadata.ReferenceOptions{}, nil
}

func (s *managementRepositoryStub) ListSpecies(_ context.Context, query creaturemetadata.SpeciesListQuery) (creaturemetadata.SpeciesPage, error) {
	s.speciesQuery = query
	return s.speciesPage, nil
}

func (s *managementRepositoryStub) GetSpecies(context.Context, snowflake.ID) (creaturemetadata.ManagedSpecies, error) {
	return creaturemetadata.ManagedSpecies{}, nil
}

func (s *managementRepositoryStub) ListCreatures(context.Context, creaturemetadata.CreatureListQuery) (creaturemetadata.CreaturePage, error) {
	return creaturemetadata.CreaturePage{}, nil
}

func (s *managementRepositoryStub) GetCreature(context.Context, snowflake.ID) (creaturemetadata.ManagedCreature, error) {
	return creaturemetadata.ManagedCreature{}, nil
}
func (s *managementRepositoryStub) GetCreatureRelations(context.Context, snowflake.ID) (creaturemetadata.CreatureRelations, error) {
	return s.relations, nil
}

func (s *managementRepositoryStub) WithinCreatureData(_ context.Context, work func(creaturemetadata.ManagementWriter) error) error {
	return work(s)
}

func (s *managementRepositoryStub) CreateSpecies(_ context.Context, record creaturemetadata.CreateSpeciesRecord) (creaturemetadata.ManagedSpecies, error) {
	return record.Species, nil
}

func (s *managementRepositoryStub) UpdateSpecies(_ context.Context, record creaturemetadata.UpdateSpeciesRecord) (creaturemetadata.ManagedSpecies, error) {
	return record.Species, nil
}

func (s *managementRepositoryStub) CreateCreature(_ context.Context, record creaturemetadata.CreateCreatureRecord) (creaturemetadata.ManagedCreature, error) {
	return record.Creature, nil
}

func (s *managementRepositoryStub) UpdateCreature(_ context.Context, record creaturemetadata.UpdateCreatureRecord) (creaturemetadata.ManagedCreature, error) {
	s.updatedCreature = record.Creature
	return record.Creature, nil
}
func (s *managementRepositoryStub) ReplaceCreatureRelations(_ context.Context, record creaturemetadata.ReplaceRelationsRecord) (creaturemetadata.CreatureRelations, error) {
	s.relations = record.Relations
	for index := range s.relations.Forms {
		if s.relations.Forms[index].Version == 0 {
			s.relations.Forms[index].Version = 1
		}
	}
	for index := range s.relations.Evolutions {
		if s.relations.Evolutions[index].Version == 0 {
			s.relations.Evolutions[index].Version = 1
		}
	}
	return s.relations, nil
}
