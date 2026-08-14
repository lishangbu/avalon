package item_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/gamedata/itemrules"
)

func TestServiceCreatesNormalizedItemInLive(t *testing.T) {
	t.Parallel()

	itemID := snowflake.MustParse("1048576017")
	categoryID := snowflake.MustParse("1048576018")
	actorID := snowflake.MustParse("1048576019")
	flingPower := int32(30)
	now := time.Date(2026, time.July, 27, 11, 0, 0, 0, time.UTC)
	repository := &itemRepositoryStub{}
	service := item.NewService(repository, repository, repository, snowflake.TestSource(func() snowflake.ID { return itemID }), func() time.Time { return now })

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
	if repository.created.Item != created || repository.created.ActorAccountID != actorID || !repository.created.CreatedAt.Equal(now) {
		t.Fatalf("Create record = %+v", repository.created)
	}
}

func TestServiceUpdatesItemWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	itemID := snowflake.MustParse("1048576017")
	actorID := snowflake.MustParse("1048576019")
	flingPower := int32(40)
	now := time.Date(2026, time.July, 27, 11, 30, 0, 0, time.UTC)
	repository := &itemRepositoryStub{}
	service := item.NewService(repository, repository, repository, snowflake.NewTestID, func() time.Time { return now })

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
	if repository.updated.Item != updated || repository.updated.ExpectedVersion != 3 ||
		!repository.updated.UpdatedAt.Equal(now) {
		t.Fatalf("Update record = %+v", repository.updated)
	}
}

func TestServiceGetsItemFromLive(t *testing.T) {
	t.Parallel()

	itemID := snowflake.MustParse("1048576017")
	want := item.Item{ID: itemID, Code: "leftovers", Name: "剩饭", UsageType: item.UsageHeld, Version: 2}
	repository := &itemRepositoryStub{got: want}
	service := item.NewService(repository, repository, repository, snowflake.NewTestID, time.Now)

	got, err := service.Get(context.Background(), itemID)
	if err != nil {
		t.Fatalf("Get() error = %v", err)
	}
	if got != want || repository.gotID != itemID {
		t.Fatalf("Get() = %+v, queried ID = %s", got, repository.gotID)
	}
}

func TestServiceListsItemsWithNormalizedDefaults(t *testing.T) {
	t.Parallel()

	want := item.Page{Items: []item.Item{{Code: "leftovers"}}, Total: 1, Page: 1, PageSize: 20}
	repository := &itemRepositoryStub{listed: want}
	service := item.NewService(repository, repository, repository, snowflake.NewTestID, time.Now)

	got, err := service.List(context.Background(), item.ListQuery{Q: "  剩饭  "})
	if err != nil {
		t.Fatalf("List() error = %v", err)
	}
	if got.Total != want.Total || len(got.Items) != 1 || got.Items[0].Code != "leftovers" {
		t.Fatalf("List() = %+v", got)
	}
	if repository.listQuery.Page != 1 || repository.listQuery.PageSize != 20 || repository.listQuery.Q != "剩饭" ||
		repository.listQuery.Sort != item.SortCodeAscending {
		t.Fatalf("List query = %+v", repository.listQuery)
	}
}

func TestServiceDeletesItemWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	itemID := snowflake.MustParse("1048576017")
	actorID := snowflake.MustParse("1048576019")
	now := time.Date(2026, time.July, 27, 12, 0, 0, 0, time.UTC)
	repository := &itemRepositoryStub{}
	service := item.NewService(repository, repository, repository, snowflake.NewTestID, func() time.Time { return now })

	err := service.Disable(context.Background(), item.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-leftovers-item", "delete-leftovers-item-request"),
		ItemID:               itemID, ExpectedVersion: 4,
	})
	if err != nil {
		t.Fatalf("Delete() error = %v", err)
	}
	if repository.disabled.ItemID != itemID || repository.disabled.ExpectedVersion != 4 ||
		!repository.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v", repository.disabled)
	}
}

// TestServiceReplacesItemRulesAsOneVersionedAggregate 验证规范化规则表通过道具版本形成单一管理写入边界。
func TestServiceReplacesItemRulesAsOneVersionedAggregate(t *testing.T) {
	t.Parallel()

	itemID := snowflake.MustParse("1048576017")
	actorID := snowflake.MustParse("1048576019")
	now := time.Date(2026, time.August, 13, 10, 0, 0, 0, time.UTC)
	repository := &itemRepositoryStub{}
	service := item.NewService(repository, repository, repository, snowflake.NewTestID, func() time.Time { return now })
	rules := itemrules.Detail{ItemID: itemID, EndTurnHealDenominator: 16, CuresPoison: true}

	updated, err := service.ReplaceRules(context.Background(), item.ReplaceRulesCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "replace-leftovers-rules", "replace-leftovers-rules-request"),
		ItemID:               itemID, ExpectedVersion: 4, Rules: rules,
	})
	if err != nil {
		t.Fatalf("ReplaceRules() error = %v", err)
	}
	if updated.ItemID != itemID || updated.Version != 5 || updated.Rules.EndTurnHealDenominator != 16 || !updated.Rules.CuresPoison {
		t.Fatalf("ReplaceRules() = %+v", updated)
	}
	if repository.replaced.ExpectedVersion != 4 || !repository.replaced.UpdatedAt.Equal(now) {
		t.Fatalf("Replace rules record = %+v", repository.replaced)
	}
}

type itemRepositoryStub struct {
	created   item.CreateRecord
	updated   item.UpdateRecord
	got       item.Item
	gotID     snowflake.ID
	listed    item.Page
	listQuery item.ListQuery
	disabled  item.DisableRecord
	replaced  item.ReplaceRulesRecord
}

func (s *itemRepositoryStub) GetItem(_ context.Context, itemID snowflake.ID) (item.Item, error) {
	s.gotID = itemID
	return s.got, nil
}

func (s *itemRepositoryStub) ListItems(_ context.Context, query item.ListQuery) (item.Page, error) {
	s.listQuery = query
	return s.listed, nil
}

func (s *itemRepositoryStub) Create(_ context.Context, record item.CreateRecord) (item.Item, error) {
	s.created = record
	return record.Item, nil
}

func (s *itemRepositoryStub) Update(_ context.Context, record item.UpdateRecord) (item.Item, error) {
	s.updated = record
	return record.Item, nil
}

func (s *itemRepositoryStub) Disable(_ context.Context, record item.DisableRecord) error {
	s.disabled = record
	return nil
}

func (s *itemRepositoryStub) GetManagedItemRules(_ context.Context, itemID snowflake.ID) (item.Rules, error) {
	return item.Rules{ItemID: itemID, Version: 1}, nil
}

func (s *itemRepositoryStub) ReplaceItemRules(_ context.Context, record item.ReplaceRulesRecord) (item.Rules, error) {
	s.replaced = record
	return item.Rules{ItemID: record.ItemID, Version: record.ExpectedVersion + 1, Rules: record.Rules}, nil
}

func (s *itemRepositoryStub) WithinItem(_ context.Context, work func(item.Writer) error) error {
	return work(s)
}
