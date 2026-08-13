package skillcategory_test

import (
	"context"
	"errors"
	"strings"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/skillcategory"
)

func TestServiceCreatesNormalizedSkillCategoryInLive(t *testing.T) {
	t.Parallel()

	categoryID := snowflake.MustParse("1048576050")
	actorID := snowflake.MustParse("1048576051")
	description := "  造成伤害并可能附加状态。  "
	now := time.Date(2026, time.July, 28, 2, 30, 0, 0, time.UTC)
	store := &skillCategoryStoreStub{}
	service := skillcategory.NewService(
		store,
		snowflake.TestSource(func() snowflake.ID { return categoryID }),
		func() time.Time { return now },
	)

	created, err := service.Create(context.Background(), skillcategory.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-damage-category", "create-damage-category-request"),
		Code:                 " damage ", Name: "  伤害类  ", Description: &description, Enabled: true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != categoryID || created.Code != "damage" || created.Name != "伤害类" ||
		created.Description == nil || *created.Description != "造成伤害并可能附加状态。" ||
		!created.Enabled || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}
}

func TestServicePreservesClearsAndReplacesSkillCategoryDescription(t *testing.T) {
	t.Parallel()

	categoryID := snowflake.MustParse("1048576050")
	actorID := snowflake.MustParse("1048576051")
	replacement := "  替换后的说明。  "
	tests := []struct {
		name      string
		change    skillcategory.DescriptionChange
		want      *string
		specified bool
	}{
		{name: "省略时保留", change: skillcategory.DescriptionChange{}},
		{name: "null 时清空", change: skillcategory.DescriptionChange{Specified: true}, specified: true},
		{name: "新值时规范化并替换", change: skillcategory.DescriptionChange{Specified: true, Value: &replacement}, want: stringPointer("替换后的说明。"), specified: true},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()

			store := &skillCategoryStoreStub{}
			service := skillcategory.NewService(store, snowflake.NewTestID, time.Now)
			_, err := service.Update(context.Background(), skillcategory.UpdateCommand{
				GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "update-damage-category", "update-damage-category-request"),
				CategoryID:           categoryID, ExpectedVersion: 1,
				Code: "damage", Name: "伤害类", Description: test.change, Enabled: false,
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

func TestServiceGetsListsAndDeletesSkillCategoryThroughPublicBoundaries(t *testing.T) {
	t.Parallel()

	categoryID := snowflake.MustParse("1048576050")
	actorID := snowflake.MustParse("1048576051")
	want := skillcategory.Category{ID: categoryID, Code: "damage", Name: "伤害类", Enabled: true, Version: 2}
	store := &skillCategoryStoreStub{
		found: want, page: skillcategory.Page{Items: []skillcategory.Category{want}, Total: 1, Page: 1, PageSize: 20},
	}
	now := time.Date(2026, time.July, 28, 3, 0, 0, 0, time.UTC)
	service := skillcategory.NewService(store, snowflake.NewTestID, func() time.Time { return now })

	got, err := service.Get(context.Background(), categoryID)
	if err != nil || got != want || store.getID != categoryID {
		t.Fatalf("Get() = %+v, error = %v", got, err)
	}
	page, err := service.List(context.Background(), skillcategory.ListQuery{Q: "  伤害  ", Sort: skillcategory.SortNameDescending})
	if err != nil || page.Total != 1 || store.listQuery.Q != "伤害" || store.listQuery.Page != 1 ||
		store.listQuery.PageSize != 20 || store.listQuery.Sort != skillcategory.SortNameDescending {
		t.Fatalf("List() = %+v, query = %+v, error = %v", page, store.listQuery, err)
	}
	err = service.Disable(context.Background(), skillcategory.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-damage-category", "delete-damage-category-request"),
		CategoryID:           categoryID, ExpectedVersion: 2,
	})
	if err != nil || store.disabled.CategoryID != categoryID || store.disabled.ExpectedVersion != 2 ||
		!store.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v, error = %v", store.disabled, err)
	}
}

func TestServiceRejectsInvalidSkillCategoryDomainValues(t *testing.T) {
	t.Parallel()

	actorID := snowflake.MustParse("1048576051")
	validDescription := "有效说明"
	base := skillcategory.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "invalid-category", "invalid-category-request"),
		Code:                 "valid-category", Name: "有效分类", Description: &validDescription, Enabled: true,
	}
	tests := []struct {
		name   string
		mutate func(*skillcategory.CreateCommand)
	}{
		{name: "稳定编码无效", mutate: func(command *skillcategory.CreateCommand) { command.Code = "INVALID" }},
		{name: "名称为空", mutate: func(command *skillcategory.CreateCommand) { command.Name = "  " }},
		{name: "名称超过上限", mutate: func(command *skillcategory.CreateCommand) { command.Name = strings.Repeat("类", 121) }},
		{name: "说明超过上限", mutate: func(command *skillcategory.CreateCommand) {
			command.Description = stringPointer(strings.Repeat("说", 501))
		}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			command := base
			test.mutate(&command)
			store := &skillCategoryStoreStub{}
			service := skillcategory.NewService(store, snowflake.NewTestID, time.Now)
			if _, err := service.Create(context.Background(), command); !errors.Is(err, skillcategory.ErrInvalidSkillCategory) {
				t.Fatalf("Create() error = %v, want ErrInvalidSkillCategory", err)
			}
		})
	}
}

func stringPointer(value string) *string { return &value }

func stringPointersEqual(left, right *string) bool {
	return left == nil && right == nil || left != nil && right != nil && *left == *right
}

type skillCategoryStoreStub struct {
	created   skillcategory.CreateRecord
	updated   skillcategory.UpdateRecord
	found     skillcategory.Category
	getID     snowflake.ID
	page      skillcategory.Page
	listQuery skillcategory.ListQuery
	disabled  skillcategory.DisableRecord
}

func (s *skillCategoryStoreStub) GetSkillCategory(_ context.Context, id snowflake.ID) (skillcategory.Category, error) {
	s.getID = id
	return s.found, nil
}
func (s *skillCategoryStoreStub) ListSkillCategories(_ context.Context, query skillcategory.ListQuery) (skillcategory.Page, error) {
	s.listQuery = query
	return s.page, nil
}
func (s *skillCategoryStoreStub) Create(_ context.Context, record skillcategory.CreateRecord) (skillcategory.Category, error) {
	s.created = record
	return record.Category, nil
}
func (s *skillCategoryStoreStub) Update(_ context.Context, record skillcategory.UpdateRecord) (skillcategory.Category, error) {
	s.updated = record
	return record.Category, nil
}
func (s *skillCategoryStoreStub) Disable(_ context.Context, record skillcategory.DisableRecord) error {
	s.disabled = record
	return nil
}
func (s *skillCategoryStoreStub) WithinSkillCategory(_ context.Context, work func(skillcategory.Writer) error) error {
	return work(s)
}
