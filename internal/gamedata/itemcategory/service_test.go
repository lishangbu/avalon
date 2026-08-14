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
	repository := &itemCategoryAdaptersStub{}
	service := itemcategory.NewService(repository, repository, repository, snowflake.TestSource(func() snowflake.ID { return categoryID }), func() time.Time { return now })

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
	if repository.created.Category != created || repository.created.ActorAccountID != actorID || repository.created.IdempotencyKey != "create-held-item-category" ||
		repository.created.RequestID != "create-held-item-category-request" || !repository.created.CreatedAt.Equal(now) {
		t.Fatalf("Create record = %+v", repository.created)
	}
}

func TestServiceUpdatesItemCategoryWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	categoryID := snowflake.MustParse("1048576015")
	actorID := snowflake.MustParse("1048576016")
	now := time.Date(2026, time.July, 27, 9, 30, 0, 0, time.UTC)
	repository := &itemCategoryAdaptersStub{}
	service := itemcategory.NewService(repository, repository, repository, snowflake.NewTestID, func() time.Time { return now })

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
	if repository.updated.Category != updated || repository.updated.ExpectedVersion != 3 ||
		!repository.updated.UpdatedAt.Equal(now) {
		t.Fatalf("Update record = %+v", repository.updated)
	}
}

func TestServiceGetsItemCategoryFromLive(t *testing.T) {
	t.Parallel()

	categoryID := snowflake.MustParse("1048576015")
	want := itemcategory.Category{
		ID: categoryID, Code: "held-items", Name: "携带道具", SortOrder: 10, Enabled: true, Version: 2,
	}
	repository := &itemCategoryAdaptersStub{found: want}
	service := itemcategory.NewService(repository, repository, repository, snowflake.NewTestID, time.Now)

	got, err := service.Get(context.Background(), categoryID)
	if err != nil {
		t.Fatalf("Get() error = %v", err)
	}
	if got != want || repository.getID != categoryID {
		t.Fatalf("Get() = %+v, queried ID = %s", got, repository.getID)
	}
}

func TestServiceListsItemCategoriesWithNormalizedPageAndFilters(t *testing.T) {
	t.Parallel()

	want := itemcategory.Page{
		Items: []itemcategory.Category{{Code: "held-items", Name: "携带道具", Enabled: true, Version: 1}},
		Total: 1, Page: 1, PageSize: 20,
	}
	repository := &itemCategoryAdaptersStub{page: want}
	service := itemcategory.NewService(repository, repository, repository, snowflake.NewTestID, time.Now)

	got, err := service.List(context.Background(), itemcategory.ListQuery{Q: "  携带  "})
	if err != nil {
		t.Fatalf("List() error = %v", err)
	}
	if got.Total != 1 || len(got.Items) != 1 || repository.listQuery.Q != "携带" ||
		repository.listQuery.Page != 1 || repository.listQuery.PageSize != 20 || repository.listQuery.Sort != itemcategory.SortCodeAscending {
		t.Fatalf("List() = %+v, query = %+v", got, repository.listQuery)
	}
}

func TestServiceDeletesItemCategoryWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	categoryID := snowflake.MustParse("1048576015")
	actorID := snowflake.MustParse("1048576016")
	now := time.Date(2026, time.July, 27, 10, 0, 0, 0, time.UTC)
	repository := &itemCategoryAdaptersStub{}
	service := itemcategory.NewService(repository, repository, repository, snowflake.NewTestID, func() time.Time { return now })

	err := service.Disable(context.Background(), itemcategory.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-held-item-category", "delete-held-item-category-request"),
		CategoryID:           categoryID, ExpectedVersion: 4,
	})
	if err != nil {
		t.Fatalf("Delete() error = %v", err)
	}
	if repository.disabled.CategoryID != categoryID || repository.disabled.ExpectedVersion != 4 ||
		!repository.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v", repository.disabled)
	}
}

type itemCategoryAdaptersStub struct {
	created   itemcategory.CreateRecord
	updated   itemcategory.UpdateRecord
	found     itemcategory.Category
	getID     snowflake.ID
	page      itemcategory.Page
	listQuery itemcategory.ListQuery
	disabled  itemcategory.DisableRecord
}

func (s *itemCategoryAdaptersStub) Create(_ context.Context, record itemcategory.CreateRecord) (itemcategory.Category, error) {
	s.created = record
	return record.Category, nil
}

func (s *itemCategoryAdaptersStub) Update(_ context.Context, record itemcategory.UpdateRecord) (itemcategory.Category, error) {
	s.updated = record
	return record.Category, nil
}

func (s *itemCategoryAdaptersStub) GetItemCategory(_ context.Context, categoryID snowflake.ID) (itemcategory.Category, error) {
	s.getID = categoryID
	return s.found, nil
}

func (s *itemCategoryAdaptersStub) ListItemCategories(_ context.Context, query itemcategory.ListQuery) (itemcategory.Page, error) {
	s.listQuery = query
	return s.page, nil
}

func (s *itemCategoryAdaptersStub) Disable(_ context.Context, record itemcategory.DisableRecord) error {
	s.disabled = record
	return nil
}

func (s *itemCategoryAdaptersStub) WithinItemCategory(_ context.Context, work func(itemcategory.Writer) error) error {
	return work(s)
}
