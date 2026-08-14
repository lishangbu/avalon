package itemcategory_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/itemcategory"
)

func TestServiceCreatesNormalizedItemCategoryInLive(t *testing.T) {
	t.Parallel()

	categoryID := snowflake.MustParse("1048576015")
	actorID := snowflake.MustParse("1048576016")
	now := time.Date(2026, time.July, 27, 9, 0, 0, 0, time.UTC)
	store := &itemCategoryRepositoryStub{}
	service := itemcategory.NewService(store, snowflake.TestSource(func() snowflake.ID { return categoryID }), func() time.Time { return now })

	created, err := service.Create(context.Background(), itemcategory.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-held-item-category", "create-held-item-category-request"),
		Code:                 "held-items", Name: "  携带道具  ", SortOrder: 10, Enabled: true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != categoryID || created.Code != "held-items" || created.Name != "携带道具" ||
		created.SortOrder != 10 || !created.Enabled || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}
	if store.created.Category != created || store.created.ActorAccountID != actorID || store.created.IdempotencyKey != "create-held-item-category" ||
		store.created.RequestID != "create-held-item-category-request" || !store.created.CreatedAt.Equal(now) {
		t.Fatalf("Create record = %+v", store.created)
	}
}

func TestServiceUpdatesItemCategoryWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	categoryID := snowflake.MustParse("1048576015")
	actorID := snowflake.MustParse("1048576016")
	now := time.Date(2026, time.July, 27, 9, 30, 0, 0, time.UTC)
	store := &itemCategoryRepositoryStub{}
	service := itemcategory.NewService(store, snowflake.NewTestID, func() time.Time { return now })

	updated, err := service.Update(context.Background(), itemcategory.UpdateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "update-held-item-category", "update-held-item-category-request"),
		CategoryID:           categoryID, ExpectedVersion: 3, Code: "held-items", Name: "携带物品",
		SortOrder: 11, Enabled: false,
	})
	if err != nil {
		t.Fatalf("Update() error = %v", err)
	}
	if updated.ID != categoryID || updated.Name != "携带物品" || updated.SortOrder != 11 ||
		updated.Enabled || updated.Version != 4 {
		t.Fatalf("Update() = %+v", updated)
	}
	if store.updated.Category != updated || store.updated.ExpectedVersion != 3 ||
		!store.updated.UpdatedAt.Equal(now) {
		t.Fatalf("Update record = %+v", store.updated)
	}
}

func TestServiceGetsItemCategoryFromLive(t *testing.T) {
	t.Parallel()

	categoryID := snowflake.MustParse("1048576015")
	want := itemcategory.Category{
		ID: categoryID, Code: "held-items", Name: "携带道具", SortOrder: 10, Enabled: true, Version: 2,
	}
	store := &itemCategoryRepositoryStub{found: want}
	service := itemcategory.NewService(store, snowflake.NewTestID, time.Now)

	got, err := service.Get(context.Background(), categoryID)
	if err != nil {
		t.Fatalf("Get() error = %v", err)
	}
	if got != want || store.getID != categoryID {
		t.Fatalf("Get() = %+v, queried ID = %s", got, store.getID)
	}
}

func TestServiceListsItemCategoriesWithNormalizedPageAndFilters(t *testing.T) {
	t.Parallel()

	want := itemcategory.Page{
		Items: []itemcategory.Category{{Code: "held-items", Name: "携带道具", Enabled: true, Version: 1}},
		Total: 1, Page: 1, PageSize: 20,
	}
	store := &itemCategoryRepositoryStub{page: want}
	service := itemcategory.NewService(store, snowflake.NewTestID, time.Now)

	got, err := service.List(context.Background(), itemcategory.ListQuery{Q: "  携带  "})
	if err != nil {
		t.Fatalf("List() error = %v", err)
	}
	if got.Total != 1 || len(got.Items) != 1 || store.listQuery.Q != "携带" ||
		store.listQuery.Page != 1 || store.listQuery.PageSize != 20 || store.listQuery.Sort != itemcategory.SortCodeAscending {
		t.Fatalf("List() = %+v, query = %+v", got, store.listQuery)
	}
}

func TestServiceDeletesItemCategoryWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	categoryID := snowflake.MustParse("1048576015")
	actorID := snowflake.MustParse("1048576016")
	now := time.Date(2026, time.July, 27, 10, 0, 0, 0, time.UTC)
	store := &itemCategoryRepositoryStub{}
	service := itemcategory.NewService(store, snowflake.NewTestID, func() time.Time { return now })

	err := service.Disable(context.Background(), itemcategory.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-held-item-category", "delete-held-item-category-request"),
		CategoryID:           categoryID, ExpectedVersion: 4,
	})
	if err != nil {
		t.Fatalf("Delete() error = %v", err)
	}
	if store.disabled.CategoryID != categoryID || store.disabled.ExpectedVersion != 4 ||
		!store.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v", store.disabled)
	}
}

type itemCategoryRepositoryStub struct {
	created   itemcategory.CreateRecord
	updated   itemcategory.UpdateRecord
	found     itemcategory.Category
	getID     snowflake.ID
	page      itemcategory.Page
	listQuery itemcategory.ListQuery
	disabled  itemcategory.DisableRecord
}

func (s *itemCategoryRepositoryStub) Create(_ context.Context, record itemcategory.CreateRecord) (itemcategory.Category, error) {
	s.created = record
	return record.Category, nil
}

func (s *itemCategoryRepositoryStub) Update(_ context.Context, record itemcategory.UpdateRecord) (itemcategory.Category, error) {
	s.updated = record
	return record.Category, nil
}

func (s *itemCategoryRepositoryStub) GetItemCategory(_ context.Context, categoryID snowflake.ID) (itemcategory.Category, error) {
	s.getID = categoryID
	return s.found, nil
}

func (s *itemCategoryRepositoryStub) ListItemCategories(_ context.Context, query itemcategory.ListQuery) (itemcategory.Page, error) {
	s.listQuery = query
	return s.page, nil
}

func (s *itemCategoryRepositoryStub) Disable(_ context.Context, record itemcategory.DisableRecord) error {
	s.disabled = record
	return nil
}

func (s *itemCategoryRepositoryStub) WithinItemCategory(_ context.Context, work func(itemcategory.Writer) error) error {
	return work(s)
}
