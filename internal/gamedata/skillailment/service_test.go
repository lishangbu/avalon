package skillailment_test

import (
	"context"
	"errors"
	"strings"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/skillailment"
)

func TestServiceCreatesNormalizedSkillAilmentInLive(t *testing.T) {
	t.Parallel()

	ailmentID := snowflake.MustParse("1048576045")
	actorID := snowflake.MustParse("1048576046")
	now := time.Date(2026, time.July, 27, 23, 0, 0, 0, time.UTC)
	store := &skillAilmentStoreStub{}
	service := skillailment.NewService(
		store,
		snowflake.TestSource(func() snowflake.ID { return ailmentID }),
		func() time.Time { return now },
	)

	created, err := service.Create(context.Background(), skillailment.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-paralysis-ailment", "create-paralysis-ailment-request"),
		Code:                 " paralysis ", Name: "  麻痹  ", Enabled: true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != ailmentID || created.Code != "paralysis" || created.Name != "麻痹" ||
		!created.Enabled || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}
	if store.created.Ailment.ID != created.ID || store.created.ActorAccountID != actorID || !store.created.CreatedAt.Equal(now) {
		t.Fatalf("Create record = %+v", store.created)
	}
}

func TestServiceUpdatesGetsListsAndDeletesSkillAilmentThroughPublicBoundaries(t *testing.T) {
	t.Parallel()

	ailmentID := snowflake.MustParse("1048576045")
	actorID := snowflake.MustParse("1048576046")
	updatedResult := skillailment.Ailment{
		ID: ailmentID, Code: "paralysis", Name: "麻痹状态", Enabled: false, Version: 2,
	}
	store := &skillAilmentStoreStub{
		found:         updatedResult,
		page:          skillailment.Page{Items: []skillailment.Ailment{updatedResult}, Total: 1, Page: 1, PageSize: 20},
		updatedResult: updatedResult,
	}
	now := time.Date(2026, time.July, 27, 23, 30, 0, 0, time.UTC)
	service := skillailment.NewService(store, snowflake.NewTestID, func() time.Time { return now })

	updated, err := service.Update(context.Background(), skillailment.UpdateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "update-paralysis-ailment", "update-paralysis-ailment-request"),
		AilmentID:            ailmentID, ExpectedVersion: 1,
		Code: "paralysis", Name: "  麻痹状态  ", Enabled: false,
	})
	if err != nil || updated != updatedResult || store.updated.Ailment.Name != "麻痹状态" ||
		store.updated.ExpectedVersion != 1 || !store.updated.UpdatedAt.Equal(now) {
		t.Fatalf("Update() = %+v, record = %+v, error = %v", updated, store.updated, err)
	}

	got, err := service.Get(context.Background(), ailmentID)
	if err != nil || got != updatedResult || store.getID != ailmentID {
		t.Fatalf("Get() = %+v, error = %v, queried ID = %s", got, err, store.getID)
	}
	page, err := service.List(context.Background(), skillailment.ListQuery{
		Q: "  麻痹  ", Enabled: boolPointer(true), Sort: skillailment.SortNameDescending,
	})
	if err != nil || page.Total != 1 || len(page.Items) != 1 || page.Items[0].ID != ailmentID {
		t.Fatalf("List() = %+v, error = %v", page, err)
	}
	if store.listQuery.Page != 1 || store.listQuery.PageSize != 20 || store.listQuery.Q != "麻痹" ||
		store.listQuery.Enabled == nil || !*store.listQuery.Enabled ||
		store.listQuery.Sort != skillailment.SortNameDescending {
		t.Fatalf("List query = %+v", store.listQuery)
	}

	err = service.Disable(context.Background(), skillailment.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-paralysis-ailment", "delete-paralysis-ailment-request"),
		AilmentID:            ailmentID, ExpectedVersion: 2,
	})
	if err != nil || store.disabled.AilmentID != ailmentID || store.disabled.ExpectedVersion != 2 ||
		!store.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v, error = %v", store.disabled, err)
	}
}

func TestServiceRejectsInvalidSkillAilmentDomainValues(t *testing.T) {
	t.Parallel()

	actorID := snowflake.MustParse("1048576046")
	base := skillailment.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "invalid-skill-ailment", "invalid-skill-ailment-request"),
		Code:                 "valid-ailment", Name: "有效异常", Enabled: true,
	}
	tests := []struct {
		name   string
		mutate func(*skillailment.CreateCommand)
	}{
		{name: "稳定编码无效", mutate: func(command *skillailment.CreateCommand) { command.Code = "INVALID" }},
		{name: "名称为空", mutate: func(command *skillailment.CreateCommand) { command.Name = "  " }},
		{name: "名称超过上限", mutate: func(command *skillailment.CreateCommand) { command.Name = strings.Repeat("异", 121) }},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()

			command := base
			test.mutate(&command)
			store := &skillAilmentStoreStub{}
			service := skillailment.NewService(store, snowflake.NewTestID, time.Now)
			if _, err := service.Create(context.Background(), command); !errors.Is(err, skillailment.ErrInvalidSkillAilment) {
				t.Fatalf("Create() error = %v, want ErrInvalidSkillAilment", err)
			}
			if store.created.Ailment.ID != snowflake.ID(0) {
				t.Fatalf("invalid command reached Store.Create(): %+v", store.created)
			}
		})
	}
}

func boolPointer(value bool) *bool { return &value }

type skillAilmentStoreStub struct {
	created       skillailment.CreateRecord
	updated       skillailment.UpdateRecord
	updatedResult skillailment.Ailment
	found         skillailment.Ailment
	getID         snowflake.ID
	page          skillailment.Page
	listQuery     skillailment.ListQuery
	disabled      skillailment.DisableRecord
}

func (s *skillAilmentStoreStub) GetSkillAilment(
	_ context.Context,
	ailmentID snowflake.ID,
) (skillailment.Ailment, error) {
	s.getID = ailmentID
	return s.found, nil
}

func (s *skillAilmentStoreStub) ListSkillAilments(
	_ context.Context,
	query skillailment.ListQuery,
) (skillailment.Page, error) {
	s.listQuery = query
	return s.page, nil
}

func (s *skillAilmentStoreStub) Create(
	_ context.Context,
	record skillailment.CreateRecord,
) (skillailment.Ailment, error) {
	s.created = record
	return record.Ailment, nil
}

func (s *skillAilmentStoreStub) Update(
	_ context.Context,
	record skillailment.UpdateRecord,
) (skillailment.Ailment, error) {
	s.updated = record
	return s.updatedResult, nil
}

func (s *skillAilmentStoreStub) Disable(_ context.Context, record skillailment.DisableRecord) error {
	s.disabled = record
	return nil
}

func (s *skillAilmentStoreStub) WithinSkillAilment(
	_ context.Context,
	work func(skillailment.Writer) error,
) error {
	return work(s)
}
