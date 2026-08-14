package battleformat_test

import (
	"context"
	"encoding/json"
	"reflect"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
)

func TestServiceCreatesValidatedClauseInLive(t *testing.T) {
	t.Parallel()

	clauseID := snowflake.MustParse("1048576028")
	actorID := snowflake.MustParse("1048576029")
	now := time.Date(2026, time.July, 28, 10, 0, 0, 0, time.UTC)
	repository := &battleRuleRepositoryStub{}
	registry, err := effect.NewDefaultRegistry()
	if err != nil {
		t.Fatalf("创建效果注册表失败: %v", err)
	}
	service := battleformat.NewService(repository, repository, repository, registry, snowflake.TestSource(func() snowflake.ID { return clauseID }), func() time.Time { return now })

	created, err := service.CreateClause(context.Background(), battleformat.CreateClauseCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "b3c6130f-97b0-46b9-ab4b-df948946140b",
			"fe58129b-12d3-461c-b088-60f1d678ce85",
		),
		Code: "species-clause", Name: "  物种条款  ",
		Description: "  同队物种不能重复。  ", Enabled: true,
		Definition: effect.Definition{Kind: effect.KindUniqueSpeciesClause, SchemaVersion: 1, Parameters: json.RawMessage(`{}`)},
	})
	if err != nil {
		t.Fatalf("CreateClause() error = %v", err)
	}
	if created.ID != clauseID || created.Name != "物种条款" || created.Description != "同队物种不能重复。" || created.Version != 1 {
		t.Fatalf("CreateClause() = %+v", created)
	}
	if !reflect.DeepEqual(repository.createdClause.Clause, created) || !repository.createdClause.CreatedAt.Equal(now) {
		t.Fatalf("CreateClause record = %+v", repository.createdClause)
	}
}

func TestServiceCreatesStandardSingleBattleFormat(t *testing.T) {
	t.Parallel()

	formatID := snowflake.MustParse("1048576030")
	clauseID := snowflake.MustParse("1048576031")
	actorID := snowflake.MustParse("1048576032")
	now := time.Date(2026, time.July, 28, 10, 30, 0, 0, time.UTC)
	repository := &battleRuleRepositoryStub{}
	registry, _ := effect.NewDefaultRegistry()
	service := battleformat.NewService(repository, repository, repository, registry, snowflake.TestSource(func() snowflake.ID { return formatID }), func() time.Time { return now })

	created, err := service.CreateFormat(context.Background(), battleformat.CreateFormatCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-standard-single", "request-2"),
		Code:                 "standard-single", Name: "标准单打", Mode: battleformat.ModeSingle,
		RosterCount: 6, SelectCount: 3, ActiveParticipantsPerSide: 1,
		LevelRule:    battleformat.LevelRule{Mode: battleformat.LevelRulePreserve},
		Deadlines:    battleformat.Deadlines{PreviewSeconds: 60, TurnSeconds: 90, BattleSeconds: 1800},
		Availability: battleformat.Availability{Challenge: true, Training: true, AdminPreview: true},
		ClauseIDs:    []snowflake.ID{clauseID}, Default: true, Enabled: true,
	})
	if err != nil {
		t.Fatalf("CreateFormat() error = %v", err)
	}
	if created.ID != formatID || created.Code != "standard-single" || !created.Default || created.Version != 1 {
		t.Fatalf("CreateFormat() = %+v", created)
	}
	if repository.createdFormat.Format.ID != formatID || !repository.createdFormat.CreatedAt.Equal(now) {
		t.Fatalf("CreateFormat record = %+v", repository.createdFormat)
	}
}

type battleRuleRepositoryStub struct {
	createdClause battleformat.CreateClauseRecord
	createdFormat battleformat.CreateFormatRecord
}

func (s *battleRuleRepositoryStub) WithinBattleRules(_ context.Context, work func(battleformat.Writer) error) error {
	return work(s)
}

func (s *battleRuleRepositoryStub) CreateClause(_ context.Context, record battleformat.CreateClauseRecord) (battleformat.Clause, error) {
	s.createdClause = record
	return record.Clause, nil
}

func (s *battleRuleRepositoryStub) CreateFormat(_ context.Context, record battleformat.CreateFormatRecord) (battleformat.Format, error) {
	s.createdFormat = record
	return record.Format, nil
}

func (s *battleRuleRepositoryStub) UpdateClause(context.Context, battleformat.UpdateClauseRecord) (battleformat.Clause, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) DisableClause(context.Context, battleformat.DisableClauseRecord) error {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) CreateRestriction(context.Context, battleformat.CreateRestrictionRecord) (battleformat.Restriction, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) UpdateRestriction(context.Context, battleformat.UpdateRestrictionRecord) (battleformat.Restriction, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) DisableRestriction(context.Context, battleformat.DisableRestrictionRecord) error {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) CreateMechanic(context.Context, battleformat.CreateMechanicRecord) (battleformat.Mechanic, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) UpdateMechanic(context.Context, battleformat.UpdateMechanicRecord) (battleformat.Mechanic, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) DisableMechanic(context.Context, battleformat.DisableMechanicRecord) error {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) UpdateFormat(context.Context, battleformat.UpdateFormatRecord) (battleformat.Format, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) DisableFormat(context.Context, battleformat.DisableFormatRecord) error {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) GetClause(context.Context, snowflake.ID) (battleformat.Clause, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) ListClauses(context.Context, battleformat.ClauseListQuery) (battleformat.ClausePage, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) GetRestriction(context.Context, snowflake.ID) (battleformat.Restriction, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) ListRestrictions(context.Context, battleformat.RestrictionListQuery) (battleformat.RestrictionPage, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) GetMechanic(context.Context, snowflake.ID) (battleformat.Mechanic, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) ListMechanics(context.Context, battleformat.MechanicListQuery) (battleformat.MechanicPage, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) GetFormat(context.Context, snowflake.ID) (battleformat.Format, error) {
	panic("unexpected call")
}

func (s *battleRuleRepositoryStub) ListFormats(context.Context, battleformat.FormatListQuery) (battleformat.FormatPage, error) {
	panic("unexpected call")
}
