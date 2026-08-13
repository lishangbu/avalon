package item_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/item"
)

func TestServiceCreatesNormalizedItemInLive(t *testing.T) {
	t.Parallel()

	itemID := snowflake.MustParse("1048576017")
	categoryID := snowflake.MustParse("1048576018")
	actorID := snowflake.MustParse("1048576019")
	flingPower := int32(30)
	now := time.Date(2026, time.July, 27, 11, 0, 0, 0, time.UTC)
	store := &itemStoreStub{}
	service := item.NewService(store, snowflake.TestSource(func() snowflake.ID { return itemID }), func() time.Time { return now })

	created, err := service.Create(context.Background(), item.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-leftovers-item", "create-leftovers-item-request"),
		Code:                 "leftovers", Name: "  剩饭  ", UsageType: item.UsageHeld, CategoryID: &categoryID,
		Cost: 20000, FlingPower: &flingPower, Enabled: true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != itemID || created.Code != "leftovers" || created.Name != "剩饭" ||
		created.UsageType != item.UsageHeld || created.CategoryID == nil || *created.CategoryID != categoryID ||
		created.Cost != 20000 || created.FlingPower == nil || *created.FlingPower != 30 || !created.Enabled || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}
	if store.created.Item != created || store.created.ActorAccountID != actorID || !store.created.CreatedAt.Equal(now) {
		t.Fatalf("Create record = %+v", store.created)
	}
}

func TestServiceUpdatesItemWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	itemID := snowflake.MustParse("1048576017")
	actorID := snowflake.MustParse("1048576019")
	flingPower := int32(40)
	now := time.Date(2026, time.July, 27, 11, 30, 0, 0, time.UTC)
	store := &itemStoreStub{}
	service := item.NewService(store, snowflake.NewTestID, func() time.Time { return now })

	updated, err := service.Update(context.Background(), item.UpdateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "update-leftovers-item", "update-leftovers-item-request"),
		ItemID:               itemID, ExpectedVersion: 3, Code: "leftovers", Name: "剩饭强化",
		UsageType: item.UsageHeld, Cost: 21000, FlingPower: &flingPower, Enabled: false,
	})
	if err != nil {
		t.Fatalf("Update() error = %v", err)
	}
	if updated.ID != itemID || updated.Name != "剩饭强化" || updated.Cost != 21000 ||
		updated.FlingPower == nil || *updated.FlingPower != 40 || updated.Enabled || updated.Version != 4 {
		t.Fatalf("Update() = %+v", updated)
	}
	if store.updated.Item != updated || store.updated.ExpectedVersion != 3 ||
		!store.updated.UpdatedAt.Equal(now) {
		t.Fatalf("Update record = %+v", store.updated)
	}
}

func TestServiceGetsItemFromLive(t *testing.T) {
	t.Parallel()

	itemID := snowflake.MustParse("1048576017")
	want := item.Item{ID: itemID, Code: "leftovers", Name: "剩饭", UsageType: item.UsageHeld, Version: 2}
	store := &itemStoreStub{got: want}
	service := item.NewService(store, snowflake.NewTestID, time.Now)

	got, err := service.Get(context.Background(), itemID)
	if err != nil {
		t.Fatalf("Get() error = %v", err)
	}
	if got != want || store.gotID != itemID {
		t.Fatalf("Get() = %+v, queried ID = %s", got, store.gotID)
	}
}

func TestServiceListsItemsWithNormalizedDefaults(t *testing.T) {
	t.Parallel()

	want := item.Page{Items: []item.Item{{Code: "leftovers"}}, Total: 1, Page: 1, PageSize: 20}
	store := &itemStoreStub{listed: want}
	service := item.NewService(store, snowflake.NewTestID, time.Now)

	got, err := service.List(context.Background(), item.ListQuery{Q: "  剩饭  "})
	if err != nil {
		t.Fatalf("List() error = %v", err)
	}
	if got.Total != want.Total || len(got.Items) != 1 || got.Items[0].Code != "leftovers" {
		t.Fatalf("List() = %+v", got)
	}
	if store.listQuery.Page != 1 || store.listQuery.PageSize != 20 || store.listQuery.Q != "剩饭" ||
		store.listQuery.Sort != item.SortCodeAscending {
		t.Fatalf("List query = %+v", store.listQuery)
	}
}

func TestServiceDeletesItemWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	itemID := snowflake.MustParse("1048576017")
	actorID := snowflake.MustParse("1048576019")
	now := time.Date(2026, time.July, 27, 12, 0, 0, 0, time.UTC)
	store := &itemStoreStub{}
	service := item.NewService(store, snowflake.NewTestID, func() time.Time { return now })

	err := service.Disable(context.Background(), item.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-leftovers-item", "delete-leftovers-item-request"),
		ItemID:               itemID, ExpectedVersion: 4,
	})
	if err != nil {
		t.Fatalf("Delete() error = %v", err)
	}
	if store.disabled.ItemID != itemID || store.disabled.ExpectedVersion != 4 ||
		!store.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v", store.disabled)
	}
}

type itemStoreStub struct {
	created   item.CreateRecord
	updated   item.UpdateRecord
	got       item.Item
	gotID     snowflake.ID
	listed    item.Page
	listQuery item.ListQuery
	disabled  item.DisableRecord
}

func (s *itemStoreStub) GetItem(_ context.Context, itemID snowflake.ID) (item.Item, error) {
	s.gotID = itemID
	return s.got, nil
}

func (s *itemStoreStub) ListItems(_ context.Context, query item.ListQuery) (item.Page, error) {
	s.listQuery = query
	return s.listed, nil
}

func (s *itemStoreStub) Create(_ context.Context, record item.CreateRecord) (item.Item, error) {
	s.created = record
	return record.Item, nil
}

func (s *itemStoreStub) Update(_ context.Context, record item.UpdateRecord) (item.Item, error) {
	s.updated = record
	return record.Item, nil
}

func (s *itemStoreStub) Disable(_ context.Context, record item.DisableRecord) error {
	s.disabled = record
	return nil
}

func (s *itemStoreStub) WithinItem(_ context.Context, work func(item.Writer) error) error {
	return work(s)
}
