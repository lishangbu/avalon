package skilllearnmethod_test

import (
	"context"
	"errors"
	"strings"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/skilllearnmethod"
)

func TestServiceCreatesNormalizedSkillLearnMethodInLive(t *testing.T) {
	t.Parallel()

	methodID := snowflake.MustParse("1048576052")
	actorID := snowflake.MustParse("1048576053")
	description := "  达到指定等级时学习。  "
	now := time.Date(2026, time.July, 28, 2, 30, 0, 0, time.UTC)
	store := &skillLearnMethodStoreStub{}
	service := skilllearnmethod.NewService(
		store,
		snowflake.TestSource(func() snowflake.ID { return methodID }),
		func() time.Time { return now },
	)

	created, err := service.Create(context.Background(), skilllearnmethod.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-level-up-method", "create-level-up-method-request"),
		Code:                 " level-up ", Name: "  升级  ", Description: &description, Enabled: true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != methodID || created.Code != "level-up" || created.Name != "升级" ||
		created.Description == nil || *created.Description != "达到指定等级时学习。" ||
		!created.Enabled || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}
}

func TestServicePreservesClearsAndReplacesSkillLearnMethodDescription(t *testing.T) {
	t.Parallel()

	methodID := snowflake.MustParse("1048576052")
	actorID := snowflake.MustParse("1048576053")
	replacement := "  替换后的说明。  "
	tests := []struct {
		name      string
		change    skilllearnmethod.DescriptionChange
		want      *string
		specified bool
	}{
		{name: "省略时保留", change: skilllearnmethod.DescriptionChange{}},
		{name: "null 时清空", change: skilllearnmethod.DescriptionChange{Specified: true}, specified: true},
		{name: "新值时规范化并替换", change: skilllearnmethod.DescriptionChange{Specified: true, Value: &replacement}, want: stringPointer("替换后的说明。"), specified: true},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()

			store := &skillLearnMethodStoreStub{}
			service := skilllearnmethod.NewService(store, snowflake.NewTestID, time.Now)
			_, err := service.Update(context.Background(), skilllearnmethod.UpdateCommand{
				GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "update-level-up-method", "update-level-up-method-request"),
				MethodID:             methodID, ExpectedVersion: 1,
				Code: "level-up", Name: "升级", Description: test.change, Enabled: false,
			})
			if err != nil {
				t.Fatalf("Update() error = %v", err)
			}
			got := store.updated.Description
			if got.Specified != test.specified || !stringPointersEqual(got.Value, test.want) {
				t.Fatalf("Description = %+v, want specified=%v value=%v", got, test.specified, test.want)
			}
		})
	}
}

func TestServiceGetsListsAndDeletesSkillLearnMethodThroughPublicBoundaries(t *testing.T) {
	t.Parallel()

	methodID := snowflake.MustParse("1048576052")
	actorID := snowflake.MustParse("1048576053")
	want := skilllearnmethod.Method{ID: methodID, Code: "level-up", Name: "升级", Enabled: true, Version: 2}
	store := &skillLearnMethodStoreStub{
		found: want, page: skilllearnmethod.Page{Items: []skilllearnmethod.Method{want}, Total: 1, Page: 1, PageSize: 20},
	}
	now := time.Date(2026, time.July, 28, 3, 0, 0, 0, time.UTC)
	service := skilllearnmethod.NewService(store, snowflake.NewTestID, func() time.Time { return now })

	got, err := service.Get(context.Background(), methodID)
	if err != nil || got != want || store.getID != methodID {
		t.Fatalf("Get() = %+v, error = %v", got, err)
	}
	page, err := service.List(context.Background(), skilllearnmethod.ListQuery{Q: "  升级  ", Sort: skilllearnmethod.SortNameDescending})
	if err != nil || page.Total != 1 || store.listQuery.Q != "升级" || store.listQuery.Page != 1 ||
		store.listQuery.PageSize != 20 || store.listQuery.Sort != skilllearnmethod.SortNameDescending {
		t.Fatalf("List() = %+v, query = %+v, error = %v", page, store.listQuery, err)
	}
	err = service.Disable(context.Background(), skilllearnmethod.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-level-up-method", "delete-level-up-method-request"),
		MethodID:             methodID, ExpectedVersion: 2,
	})
	if err != nil || store.disabled.MethodID != methodID || store.disabled.ExpectedVersion != 2 ||
		!store.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v, error = %v", store.disabled, err)
	}
}

func TestServiceRejectsInvalidSkillLearnMethodDomainValues(t *testing.T) {
	t.Parallel()

	actorID := snowflake.MustParse("1048576053")
	validDescription := "有效说明"
	base := skilllearnmethod.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "invalid-method", "invalid-method-request"),
		Code:                 "valid-method", Name: "有效方式", Description: &validDescription, Enabled: true,
	}
	tests := []struct {
		name   string
		mutate func(*skilllearnmethod.CreateCommand)
	}{
		{name: "稳定编码无效", mutate: func(command *skilllearnmethod.CreateCommand) { command.Code = "INVALID" }},
		{name: "名称为空", mutate: func(command *skilllearnmethod.CreateCommand) { command.Name = "  " }},
		{name: "名称超过上限", mutate: func(command *skilllearnmethod.CreateCommand) { command.Name = strings.Repeat("类", 121) }},
		{name: "说明超过上限", mutate: func(command *skilllearnmethod.CreateCommand) {
			command.Description = stringPointer(strings.Repeat("说", 501))
		}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			command := base
			test.mutate(&command)
			store := &skillLearnMethodStoreStub{}
			service := skilllearnmethod.NewService(store, snowflake.NewTestID, time.Now)
			if _, err := service.Create(context.Background(), command); !errors.Is(err, skilllearnmethod.ErrInvalidSkillLearnMethod) {
				t.Fatalf("Create() error = %v, want ErrInvalidSkillLearnMethod", err)
			}
		})
	}
}

func stringPointer(value string) *string { return &value }

func stringPointersEqual(left, right *string) bool {
	return left == nil && right == nil || left != nil && right != nil && *left == *right
}

type skillLearnMethodStoreStub struct {
	created   skilllearnmethod.CreateRecord
	updated   skilllearnmethod.UpdateRecord
	found     skilllearnmethod.Method
	getID     snowflake.ID
	page      skilllearnmethod.Page
	listQuery skilllearnmethod.ListQuery
	disabled  skilllearnmethod.DisableRecord
}

func (s *skillLearnMethodStoreStub) GetSkillLearnMethod(_ context.Context, id snowflake.ID) (skilllearnmethod.Method, error) {
	s.getID = id
	return s.found, nil
}
func (s *skillLearnMethodStoreStub) ListSkillLearnMethods(_ context.Context, query skilllearnmethod.ListQuery) (skilllearnmethod.Page, error) {
	s.listQuery = query
	return s.page, nil
}
func (s *skillLearnMethodStoreStub) Create(_ context.Context, record skilllearnmethod.CreateRecord) (skilllearnmethod.Method, error) {
	s.created = record
	return record.Method, nil
}
func (s *skillLearnMethodStoreStub) Update(_ context.Context, record skilllearnmethod.UpdateRecord) (skilllearnmethod.Method, error) {
	s.updated = record
	return record.Method, nil
}
func (s *skillLearnMethodStoreStub) Disable(_ context.Context, record skilllearnmethod.DisableRecord) error {
	s.disabled = record
	return nil
}
func (s *skillLearnMethodStoreStub) WithinSkillLearnMethod(_ context.Context, work func(skilllearnmethod.Writer) error) error {
	return work(s)
}
