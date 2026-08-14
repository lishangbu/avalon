package stat_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
)

func TestServiceCreatesNormalizedStatInLive(t *testing.T) {
	t.Parallel()

	statID := snowflake.MustParse("1048576020")
	actorID := snowflake.MustParse("1048576021")
	now := time.Date(2026, time.July, 27, 17, 0, 0, 0, time.UTC)
	repository := &statRepositoryStub{}
	service := stat.NewService(repository, snowflake.TestSource(func() snowflake.ID { return statID }), func() time.Time { return now })

	created, err := service.Create(context.Background(), stat.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-hp-stat", "create-hp-stat-request"),
		Code:                 "hp", Name: "  体力  ", SortOrder: 1, BattleOnly: false, Enabled: true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != statID || created.Code != "hp" || created.Name != "体力" || created.SortOrder != 1 ||
		created.BattleOnly || !created.Enabled || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}
	if repository.created.Stat != created || repository.created.ActorAccountID != actorID || !repository.created.CreatedAt.Equal(now) {
		t.Fatalf("Create record = %+v", repository.created)
	}
}

func TestServiceUpdatesStatWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	statID := snowflake.MustParse("1048576020")
	actorID := snowflake.MustParse("1048576021")
	now := time.Date(2026, time.July, 27, 17, 30, 0, 0, time.UTC)
	repository := &statRepositoryStub{}
	service := stat.NewService(repository, snowflake.NewTestID, func() time.Time { return now })

	updated, err := service.Update(context.Background(), stat.UpdateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "update-hp-stat", "update-hp-stat-request"),
		StatID:               statID, ExpectedVersion: 2, Code: "hp", Name: "生命值", SortOrder: 2,
		BattleOnly: true, Enabled: false,
	})
	if err != nil {
		t.Fatalf("Update() error = %v", err)
	}
	if updated.ID != statID || updated.Name != "生命值" || updated.SortOrder != 2 ||
		!updated.BattleOnly || updated.Enabled || updated.Version != 3 {
		t.Fatalf("Update() = %+v", updated)
	}
	if repository.updated.Stat != updated || repository.updated.ExpectedVersion != 2 ||
		!repository.updated.UpdatedAt.Equal(now) {
		t.Fatalf("Update record = %+v", repository.updated)
	}
}

func TestServiceGetsStatFromLive(t *testing.T) {
	t.Parallel()

	statID := snowflake.MustParse("1048576020")
	want := stat.Stat{ID: statID, Code: "hp", Name: "体力", SortOrder: 1, Enabled: true, Version: 2}
	repository := &statRepositoryStub{found: want}
	service := stat.NewService(repository, snowflake.NewTestID, time.Now)

	got, err := service.Get(context.Background(), statID)
	if err != nil {
		t.Fatalf("Get() error = %v", err)
	}
	if got != want || repository.getID != statID {
		t.Fatalf("Get() = %+v, queried ID = %s", got, repository.getID)
	}
}

func TestServiceListsStatsWithNormalizedDefaults(t *testing.T) {
	t.Parallel()

	want := stat.Page{Items: []stat.Stat{{Code: "hp"}}, Total: 1, Page: 1, PageSize: 20}
	repository := &statRepositoryStub{page: want}
	service := stat.NewService(repository, snowflake.NewTestID, time.Now)

	got, err := service.List(context.Background(), stat.ListQuery{Q: "  体力  "})
	if err != nil {
		t.Fatalf("List() error = %v", err)
	}
	if got.Total != want.Total || len(got.Items) != 1 || got.Items[0].Code != "hp" {
		t.Fatalf("List() = %+v", got)
	}
	if repository.listQuery.Page != 1 || repository.listQuery.PageSize != 20 || repository.listQuery.Q != "体力" ||
		repository.listQuery.Sort != stat.SortCodeAscending {
		t.Fatalf("List query = %+v", repository.listQuery)
	}
}

func TestServiceDeletesStatWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	statID := snowflake.MustParse("1048576020")
	actorID := snowflake.MustParse("1048576021")
	now := time.Date(2026, time.July, 27, 18, 0, 0, 0, time.UTC)
	repository := &statRepositoryStub{}
	service := stat.NewService(repository, snowflake.NewTestID, func() time.Time { return now })

	err := service.Disable(context.Background(), stat.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-hp-stat", "delete-hp-stat-request"),
		StatID:               statID, ExpectedVersion: 3,
	})
	if err != nil {
		t.Fatalf("Delete() error = %v", err)
	}
	if repository.disabled.StatID != statID || repository.disabled.ExpectedVersion != 3 ||
		!repository.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v", repository.disabled)
	}
}

type statRepositoryStub struct {
	created   stat.CreateRecord
	updated   stat.UpdateRecord
	found     stat.Stat
	getID     snowflake.ID
	page      stat.Page
	listQuery stat.ListQuery
	disabled  stat.DisableRecord
}

func (s *statRepositoryStub) Disable(_ context.Context, record stat.DisableRecord) error {
	s.disabled = record
	return nil
}

func (s *statRepositoryStub) ListStats(_ context.Context, query stat.ListQuery) (stat.Page, error) {
	s.listQuery = query
	return s.page, nil
}

func (s *statRepositoryStub) GetStat(_ context.Context, statID snowflake.ID) (stat.Stat, error) {
	s.getID = statID
	return s.found, nil
}

func (s *statRepositoryStub) Update(_ context.Context, record stat.UpdateRecord) (stat.Stat, error) {
	s.updated = record
	return record.Stat, nil
}

func (s *statRepositoryStub) Create(_ context.Context, record stat.CreateRecord) (stat.Stat, error) {
	s.created = record
	return record.Stat, nil
}

func (s *statRepositoryStub) WithinStat(_ context.Context, work func(stat.Writer) error) error {
	return work(s)
}
