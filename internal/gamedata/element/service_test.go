package element_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/element"
)

func TestServiceCreatesNormalizedElementInLive(t *testing.T) {
	t.Parallel()

	elementID := snowflake.MustParse("1048576002")
	actorID := snowflake.MustParse("1048576003")
	now := time.Date(2026, time.July, 27, 5, 0, 0, 0, time.UTC)
	repository := &elementAdaptersStub{}
	service := element.NewService(repository, repository, repository, snowflake.TestSource(func() snowflake.ID { return elementID }), func() time.Time { return now })

	created, err := service.Create(context.Background(), element.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-stellar-element", "create-stellar-element-request"),
		Code:                 "stellar",
		Name:                 "  星晶  ",
		SortOrder:            19,
		Enabled:              true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != elementID || created.Code != "stellar" || created.Name != "星晶" ||
		created.SortOrder != 19 || !created.Enabled || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}
	if repository.created.Element != created || repository.created.ActorAccountID != actorID ||
		repository.created.IdempotencyKey != "create-stellar-element" ||
		repository.created.RequestID != "create-stellar-element-request" || !repository.created.CreatedAt.Equal(now) {
		t.Fatalf("Create record = %+v", repository.created)
	}
}

func TestServiceRejectsInvalidElementBeforeRepository(t *testing.T) {
	t.Parallel()

	repository := &elementAdaptersStub{}
	service := element.NewService(repository, repository, repository, snowflake.NewTestID, time.Now)
	_, err := service.Create(context.Background(), element.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(snowflake.MustParse("1048576003"), "invalid-element", "invalid-element-request"),
		Code:                 "Bad Code",
		Name:                 "星晶",
		SortOrder:            19,
		Enabled:              true,
	})
	if !errors.Is(err, element.ErrInvalidElement) {
		t.Fatalf("Create() error = %v, want ErrInvalidElement", err)
	}
	if repository.createCalls != 0 {
		t.Fatalf("Repository.Create() calls = %d, want 0", repository.createCalls)
	}
}

func TestServiceUpdatesElementWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	elementID := snowflake.MustParse("1048576002")
	actorID := snowflake.MustParse("1048576003")
	now := time.Date(2026, time.July, 27, 6, 0, 0, 0, time.UTC)
	repository := &elementAdaptersStub{}
	service := element.NewService(repository, repository, repository, snowflake.NewTestID, func() time.Time { return now })

	updated, err := service.Update(context.Background(), element.UpdateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "update-stellar-element", "update-stellar-element-request"),
		ElementID:            elementID,
		ExpectedVersion:      3,
		Code:                 "stellar",
		Name:                 "  星晶核心  ",
		SortOrder:            20,
		Enabled:              false,
	})
	if err != nil {
		t.Fatalf("Update() error = %v", err)
	}
	if updated.ID != elementID || updated.Code != "stellar" || updated.Name != "星晶核心" ||
		updated.SortOrder != 20 || updated.Enabled || updated.Version != 4 {
		t.Fatalf("Update() = %+v", updated)
	}
	if repository.updated.Element != updated || repository.updated.ExpectedVersion != 3 ||
		repository.updated.ActorAccountID != actorID || repository.updated.IdempotencyKey != "update-stellar-element" ||
		repository.updated.RequestID != "update-stellar-element-request" || !repository.updated.UpdatedAt.Equal(now) {
		t.Fatalf("Update record = %+v", repository.updated)
	}
}

func TestServiceGetsElementFromLive(t *testing.T) {
	t.Parallel()

	elementID := snowflake.MustParse("1048576002")
	want := element.Element{ID: elementID, Code: "stellar", Name: "星晶", SortOrder: 19, Enabled: true, Version: 2}
	repository := &elementAdaptersStub{found: want}
	service := element.NewService(repository, repository, repository, snowflake.NewTestID, time.Now)

	got, err := service.Get(context.Background(), elementID)
	if err != nil {
		t.Fatalf("Get() error = %v", err)
	}
	if got != want || repository.getID != elementID {
		t.Fatalf("Get() = %+v, queried ID = %s", got, repository.getID)
	}
}

func TestServiceListsElementsWithNormalizedPageAndFilters(t *testing.T) {
	t.Parallel()

	want := element.Page{
		Items: []element.Element{{Code: "stellar", Name: "星晶", Enabled: true, Version: 1}},
		Total: 1, Page: 1, PageSize: 20,
	}
	repository := &elementAdaptersStub{page: want}
	service := element.NewService(repository, repository, repository, snowflake.NewTestID, time.Now)

	got, err := service.List(context.Background(), element.ListQuery{Q: "  星晶  "})
	if err != nil {
		t.Fatalf("List() error = %v", err)
	}
	if got.Total != want.Total || got.Page != want.Page || got.PageSize != want.PageSize || len(got.Items) != 1 {
		t.Fatalf("List() = %+v", got)
	}
	if repository.listQuery.Page != 1 || repository.listQuery.PageSize != 20 || repository.listQuery.Q != "星晶" ||
		repository.listQuery.Sort != element.SortCodeAscending {
		t.Fatalf("List query = %+v", repository.listQuery)
	}
}

func TestServiceDeletesElementWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	elementID := snowflake.MustParse("1048576002")
	actorID := snowflake.MustParse("1048576003")
	now := time.Date(2026, time.July, 27, 6, 30, 0, 0, time.UTC)
	repository := &elementAdaptersStub{}
	service := element.NewService(repository, repository, repository, snowflake.NewTestID, func() time.Time { return now })

	err := service.Disable(context.Background(), element.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-stellar-element", "delete-stellar-element-request"),
		ElementID:            elementID,
		ExpectedVersion:      4,
	})
	if err != nil {
		t.Fatalf("Delete() error = %v", err)
	}
	if repository.disabled.ElementID != elementID || repository.disabled.ExpectedVersion != 4 ||
		repository.disabled.ActorAccountID != actorID || repository.disabled.IdempotencyKey != "delete-stellar-element" ||
		repository.disabled.RequestID != "delete-stellar-element-request" || !repository.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v", repository.disabled)
	}
}

type elementAdaptersStub struct {
	created     element.CreateRecord
	updated     element.UpdateRecord
	found       element.Element
	getID       snowflake.ID
	page        element.Page
	listQuery   element.ListQuery
	disabled    element.DisableRecord
	createCalls int
}

func (s *elementAdaptersStub) Create(_ context.Context, record element.CreateRecord) (element.Element, error) {
	s.createCalls++
	s.created = record
	return record.Element, nil
}

func (s *elementAdaptersStub) Update(_ context.Context, record element.UpdateRecord) (element.Element, error) {
	s.updated = record
	return record.Element, nil
}

func (s *elementAdaptersStub) Get(_ context.Context, elementID snowflake.ID) (element.Element, error) {
	s.getID = elementID
	return s.found, nil
}

func (s *elementAdaptersStub) List(_ context.Context, query element.ListQuery) (element.Page, error) {
	s.listQuery = query
	return s.page, nil
}

func (s *elementAdaptersStub) Disable(_ context.Context, record element.DisableRecord) error {
	s.disabled = record
	return nil
}

func (s *elementAdaptersStub) WithinElement(_ context.Context, work func(element.Writer) error) error {
	return work(s)
}
