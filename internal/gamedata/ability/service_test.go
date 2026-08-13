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
	store := &abilityStoreStub{}
	service := ability.NewService(store, snowflake.TestSource(func() snowflake.ID { return abilityID }), func() time.Time { return now })

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
	if store.created.Ability != created || store.created.ActorAccountID != actorID || store.created.IdempotencyKey != "create-overgrow-ability" ||
		store.created.RequestID != "create-overgrow-ability-request" || !store.created.CreatedAt.Equal(now) {
		t.Fatalf("Create record = %+v", store.created)
	}
}

func TestServiceUpdatesAbilityWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	abilityID := snowflake.MustParse("1048576013")
	actorID := snowflake.MustParse("1048576014")
	now := time.Date(2026, time.July, 27, 7, 30, 0, 0, time.UTC)
	store := &abilityStoreStub{}
	service := ability.NewService(store, snowflake.NewTestID, func() time.Time { return now })

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
	if store.updated.Ability != updated || store.updated.ExpectedVersion != 3 || store.updated.ActorAccountID != actorID ||
		store.updated.IdempotencyKey != "update-overgrow-ability" ||
		store.updated.RequestID != "update-overgrow-ability-request" || !store.updated.UpdatedAt.Equal(now) {
		t.Fatalf("Update record = %+v", store.updated)
	}
}

func TestServiceGetsAbilityFromLive(t *testing.T) {
	t.Parallel()

	abilityID := snowflake.MustParse("1048576013")
	want := ability.Ability{ID: abilityID, Code: "overgrow", Name: "茂盛", MainSeries: true, Enabled: true, Version: 2}
	store := &abilityStoreStub{found: want}
	service := ability.NewService(store, snowflake.NewTestID, time.Now)

	got, err := service.Get(context.Background(), abilityID)
	if err != nil {
		t.Fatalf("Get() error = %v", err)
	}
	if got != want || store.getID != abilityID {
		t.Fatalf("Get() = %+v, queried ID = %s", got, store.getID)
	}
}

func TestServiceListsAbilitiesWithNormalizedPageAndFilters(t *testing.T) {
	t.Parallel()

	want := ability.Page{
		Items: []ability.Ability{{Code: "overgrow", Name: "茂盛", MainSeries: true, Enabled: true, Version: 1}},
		Total: 1, Page: 1, PageSize: 20,
	}
	store := &abilityStoreStub{page: want}
	service := ability.NewService(store, snowflake.NewTestID, time.Now)

	got, err := service.List(context.Background(), ability.ListQuery{Q: "  茂盛  "})
	if err != nil {
		t.Fatalf("List() error = %v", err)
	}
	if got.Total != want.Total || got.Page != want.Page || got.PageSize != want.PageSize || len(got.Items) != 1 {
		t.Fatalf("List() = %+v", got)
	}
	if store.listQuery.Page != 1 || store.listQuery.PageSize != 20 || store.listQuery.Q != "茂盛" ||
		store.listQuery.Sort != ability.SortCodeAscending {
		t.Fatalf("List query = %+v", store.listQuery)
	}
}

func TestServiceDeletesAbilityWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	abilityID := snowflake.MustParse("1048576013")
	actorID := snowflake.MustParse("1048576014")
	now := time.Date(2026, time.July, 27, 8, 0, 0, 0, time.UTC)
	store := &abilityStoreStub{}
	service := ability.NewService(store, snowflake.NewTestID, func() time.Time { return now })

	err := service.Disable(context.Background(), ability.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-overgrow-ability", "delete-overgrow-ability-request"),
		AbilityID:            abilityID,
		ExpectedVersion:      4,
	})
	if err != nil {
		t.Fatalf("Delete() error = %v", err)
	}
	if store.disabled.AbilityID != abilityID || store.disabled.ExpectedVersion != 4 || store.disabled.ActorAccountID != actorID ||
		store.disabled.IdempotencyKey != "delete-overgrow-ability" ||
		store.disabled.RequestID != "delete-overgrow-ability-request" || !store.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v", store.disabled)
	}
}

type abilityStoreStub struct {
	created   ability.CreateRecord
	updated   ability.UpdateRecord
	found     ability.Ability
	getID     snowflake.ID
	page      ability.Page
	listQuery ability.ListQuery
	disabled  ability.DisableRecord
}

func (s *abilityStoreStub) Create(_ context.Context, record ability.CreateRecord) (ability.Ability, error) {
	s.created = record
	return record.Ability, nil
}

func (s *abilityStoreStub) Update(_ context.Context, record ability.UpdateRecord) (ability.Ability, error) {
	s.updated = record
	return record.Ability, nil
}

func (s *abilityStoreStub) GetAbility(_ context.Context, abilityID snowflake.ID) (ability.Ability, error) {
	s.getID = abilityID
	return s.found, nil
}

func (s *abilityStoreStub) ListAbilities(_ context.Context, query ability.ListQuery) (ability.Page, error) {
	s.listQuery = query
	return s.page, nil
}

func (s *abilityStoreStub) Disable(_ context.Context, record ability.DisableRecord) error {
	s.disabled = record
	return nil
}

func (s *abilityStoreStub) WithinAbility(_ context.Context, work func(ability.Writer) error) error {
	return work(s)
}
