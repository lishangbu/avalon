package ability_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/gamedata/administration"
)

func TestServiceCreatesNormalizedAbilityInLive(t *testing.T) {
	t.Parallel()

	abilityID := snowflake.MustParse("1048576013")
	actorID := snowflake.MustParse("1048576014")
	now := time.Date(2026, time.July, 27, 7, 0, 0, 0, time.UTC)
	repository := &abilityRepositoryStub{}
	service := ability.NewService(repository, snowflake.TestSource(func() snowflake.ID { return abilityID }), func() time.Time { return now })

	created, err := service.Create(context.Background(), ability.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-overgrow-ability", "create-overgrow-ability-request"),
		Code:                 "overgrow",
		Name:                 "  茂盛  ",
		MainSeries:           true,
		Enabled:              true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != abilityID || created.Code != "overgrow" || created.Name != "茂盛" ||
		!created.MainSeries || !created.Enabled || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}
	if repository.created.Ability != created || repository.created.ActorAccountID != actorID || repository.created.IdempotencyKey != "create-overgrow-ability" ||
		repository.created.RequestID != "create-overgrow-ability-request" || !repository.created.CreatedAt.Equal(now) {
		t.Fatalf("Create record = %+v", repository.created)
	}
}

func TestServiceUpdatesAbilityWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	abilityID := snowflake.MustParse("1048576013")
	actorID := snowflake.MustParse("1048576014")
	now := time.Date(2026, time.July, 27, 7, 30, 0, 0, time.UTC)
	repository := &abilityRepositoryStub{}
	service := ability.NewService(repository, snowflake.NewTestID, func() time.Time { return now })

	updated, err := service.Update(context.Background(), ability.UpdateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "update-overgrow-ability", "update-overgrow-ability-request"),
		AbilityID:            abilityID,
		ExpectedVersion:      3,
		Code:                 "overgrow",
		Name:                 "  茂盛强化  ",
		MainSeries:           false,
		Enabled:              true,
	})
	if err != nil {
		t.Fatalf("Update() error = %v", err)
	}
	if updated.ID != abilityID || updated.Code != "overgrow" || updated.Name != "茂盛强化" ||
		updated.MainSeries || !updated.Enabled || updated.Version != 4 {
		t.Fatalf("Update() = %+v", updated)
	}
	if repository.updated.Ability != updated || repository.updated.ExpectedVersion != 3 || repository.updated.ActorAccountID != actorID ||
		repository.updated.IdempotencyKey != "update-overgrow-ability" ||
		repository.updated.RequestID != "update-overgrow-ability-request" || !repository.updated.UpdatedAt.Equal(now) {
		t.Fatalf("Update record = %+v", repository.updated)
	}
}

func TestServiceGetsAbilityFromLive(t *testing.T) {
	t.Parallel()

	abilityID := snowflake.MustParse("1048576013")
	want := ability.Ability{ID: abilityID, Code: "overgrow", Name: "茂盛", MainSeries: true, Enabled: true, Version: 2}
	repository := &abilityRepositoryStub{found: want}
	service := ability.NewService(repository, snowflake.NewTestID, time.Now)

	got, err := service.Get(context.Background(), abilityID)
	if err != nil {
		t.Fatalf("Get() error = %v", err)
	}
	if got != want || repository.getID != abilityID {
		t.Fatalf("Get() = %+v, queried ID = %s", got, repository.getID)
	}
}

func TestServiceListsAbilitiesWithNormalizedPageAndFilters(t *testing.T) {
	t.Parallel()

	want := ability.Page{
		Items: []ability.Ability{{Code: "overgrow", Name: "茂盛", MainSeries: true, Enabled: true, Version: 1}},
		Total: 1, Page: 1, PageSize: 20,
	}
	repository := &abilityRepositoryStub{page: want}
	service := ability.NewService(repository, snowflake.NewTestID, time.Now)

	got, err := service.List(context.Background(), ability.ListQuery{Q: "  茂盛  "})
	if err != nil {
		t.Fatalf("List() error = %v", err)
	}
	if got.Total != want.Total || got.Page != want.Page || got.PageSize != want.PageSize || len(got.Items) != 1 {
		t.Fatalf("List() = %+v", got)
	}
	if repository.listQuery.Page != 1 || repository.listQuery.PageSize != 20 || repository.listQuery.Q != "茂盛" ||
		repository.listQuery.Sort != ability.SortCodeAscending {
		t.Fatalf("List query = %+v", repository.listQuery)
	}
}

func TestServiceDeletesAbilityWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	abilityID := snowflake.MustParse("1048576013")
	actorID := snowflake.MustParse("1048576014")
	now := time.Date(2026, time.July, 27, 8, 0, 0, 0, time.UTC)
	repository := &abilityRepositoryStub{}
	service := ability.NewService(repository, snowflake.NewTestID, func() time.Time { return now })

	err := service.Disable(context.Background(), ability.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-overgrow-ability", "delete-overgrow-ability-request"),
		AbilityID:            abilityID,
		ExpectedVersion:      4,
	})
	if err != nil {
		t.Fatalf("Delete() error = %v", err)
	}
	if repository.disabled.AbilityID != abilityID || repository.disabled.ExpectedVersion != 4 || repository.disabled.ActorAccountID != actorID ||
		repository.disabled.IdempotencyKey != "delete-overgrow-ability" ||
		repository.disabled.RequestID != "delete-overgrow-ability-request" || !repository.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v", repository.disabled)
	}
}

type abilityRepositoryStub struct {
	created   ability.CreateRecord
	updated   ability.UpdateRecord
	found     ability.Ability
	getID     snowflake.ID
	page      ability.Page
	listQuery ability.ListQuery
	disabled  ability.DisableRecord
}

func (s *abilityRepositoryStub) Create(_ context.Context, record ability.CreateRecord) (ability.Ability, error) {
	s.created = record
	return record.Ability, nil
}

func (s *abilityRepositoryStub) Update(_ context.Context, record ability.UpdateRecord) (ability.Ability, error) {
	s.updated = record
	return record.Ability, nil
}

func (s *abilityRepositoryStub) GetAbility(_ context.Context, abilityID snowflake.ID) (ability.Ability, error) {
	s.getID = abilityID
	return s.found, nil
}

func (s *abilityRepositoryStub) ListAbilities(_ context.Context, query ability.ListQuery) (ability.Page, error) {
	s.listQuery = query
	return s.page, nil
}

func (s *abilityRepositoryStub) Disable(_ context.Context, record ability.DisableRecord) error {
	s.disabled = record
	return nil
}

func (s *abilityRepositoryStub) WithinAbility(_ context.Context, work func(ability.Writer) error) error {
	return work(s)
}
