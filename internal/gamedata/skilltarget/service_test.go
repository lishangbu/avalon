package skilltarget_test

import (
	"context"
	"errors"
	"strings"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/skilltarget"
)

func TestServiceCreatesNormalizedSkillTargetInLive(t *testing.T) {
	t.Parallel()

	targetID := snowflake.MustParse("1048576052")
	actorID := snowflake.MustParse("1048576053")
	description := "  造成伤害并可能附加状态。  "
	now := time.Date(2026, time.July, 28, 2, 30, 0, 0, time.UTC)
	repository := &skillTargetRepositoryStub{}
	service := skilltarget.NewService(
		repository,
		snowflake.TestSource(func() snowflake.ID { return targetID }),
		func() time.Time { return now },
	)

	created, err := service.Create(context.Background(), skilltarget.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-damage-target", "create-damage-target-request"),
		Code:                 " damage ", Name: "  伤害类  ", Description: &description, Enabled: true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != targetID || created.Code != "damage" || created.Name != "伤害类" ||
		created.Description == nil || *created.Description != "造成伤害并可能附加状态。" ||
		!created.Enabled || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}
}

func TestServicePreservesClearsAndReplacesSkillTargetDescription(t *testing.T) {
	t.Parallel()

	targetID := snowflake.MustParse("1048576052")
	actorID := snowflake.MustParse("1048576053")
	replacement := "  替换后的说明。  "
	tests := []struct {
		name      string
		change    skilltarget.DescriptionChange
		want      *string
		specified bool
	}{
		{name: "省略时保留", change: skilltarget.DescriptionChange{}},
		{name: "null 时清空", change: skilltarget.DescriptionChange{Specified: true}, specified: true},
		{name: "新值时规范化并替换", change: skilltarget.DescriptionChange{Specified: true, Value: &replacement}, want: stringPointer("替换后的说明。"), specified: true},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()

			repository := &skillTargetRepositoryStub{}
			service := skilltarget.NewService(repository, snowflake.NewTestID, time.Now)
			_, err := service.Update(context.Background(), skilltarget.UpdateCommand{
				GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "update-damage-target", "update-damage-target-request"),
				TargetID:             targetID, ExpectedVersion: 1,
				Code: "damage", Name: "伤害类", Description: test.change, Enabled: false,
			})
			if err != nil {
				t.Fatalf("Update() error = %v", err)
			}
			got := repository.updated.Description
			if got.Specified != test.specified || !stringPointersEqual(got.Value, test.want) {
				t.Fatalf("Description = %+v, want specified=%v value=%v", got, test.specified, test.want)
			}
		})
	}
}

func TestServiceGetsListsAndDeletesSkillTargetThroughPublicBoundaries(t *testing.T) {
	t.Parallel()

	targetID := snowflake.MustParse("1048576052")
	actorID := snowflake.MustParse("1048576053")
	want := skilltarget.Target{ID: targetID, Code: "damage", Name: "伤害类", Enabled: true, Version: 2}
	repository := &skillTargetRepositoryStub{
		found: want, page: skilltarget.Page{Items: []skilltarget.Target{want}, Total: 1, Page: 1, PageSize: 20},
	}
	now := time.Date(2026, time.July, 28, 3, 0, 0, 0, time.UTC)
	service := skilltarget.NewService(repository, snowflake.NewTestID, func() time.Time { return now })

	got, err := service.Get(context.Background(), targetID)
	if err != nil || got != want || repository.getID != targetID {
		t.Fatalf("Get() = %+v, error = %v", got, err)
	}
	page, err := service.List(context.Background(), skilltarget.ListQuery{Q: "  伤害  ", Sort: skilltarget.SortNameDescending})
	if err != nil || page.Total != 1 || repository.listQuery.Q != "伤害" || repository.listQuery.Page != 1 ||
		repository.listQuery.PageSize != 20 || repository.listQuery.Sort != skilltarget.SortNameDescending {
		t.Fatalf("List() = %+v, query = %+v, error = %v", page, repository.listQuery, err)
	}
	err = service.Disable(context.Background(), skilltarget.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-damage-target", "delete-damage-target-request"),
		TargetID:             targetID, ExpectedVersion: 2,
	})
	if err != nil || repository.disabled.TargetID != targetID || repository.disabled.ExpectedVersion != 2 ||
		!repository.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v, error = %v", repository.disabled, err)
	}
}

func TestServiceRejectsInvalidSkillTargetDomainValues(t *testing.T) {
	t.Parallel()

	actorID := snowflake.MustParse("1048576053")
	validDescription := "有效说明"
	base := skilltarget.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "invalid-target", "invalid-target-request"),
		Code:                 "valid-target", Name: "有效分类", Description: &validDescription, Enabled: true,
	}
	tests := []struct {
		name   string
		mutate func(*skilltarget.CreateCommand)
	}{
		{name: "稳定编码无效", mutate: func(command *skilltarget.CreateCommand) { command.Code = "INVALID" }},
		{name: "名称为空", mutate: func(command *skilltarget.CreateCommand) { command.Name = "  " }},
		{name: "名称超过上限", mutate: func(command *skilltarget.CreateCommand) { command.Name = strings.Repeat("类", 121) }},
		{name: "说明超过上限", mutate: func(command *skilltarget.CreateCommand) {
			command.Description = stringPointer(strings.Repeat("说", 501))
		}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			command := base
			test.mutate(&command)
			repository := &skillTargetRepositoryStub{}
			service := skilltarget.NewService(repository, snowflake.NewTestID, time.Now)
			if _, err := service.Create(context.Background(), command); !errors.Is(err, skilltarget.ErrInvalidSkillTarget) {
				t.Fatalf("Create() error = %v, want ErrInvalidSkillTarget", err)
			}
		})
	}
}

func stringPointer(value string) *string { return &value }

func stringPointersEqual(left, right *string) bool {
	return left == nil && right == nil || left != nil && right != nil && *left == *right
}

type skillTargetRepositoryStub struct {
	created   skilltarget.CreateRecord
	updated   skilltarget.UpdateRecord
	found     skilltarget.Target
	getID     snowflake.ID
	page      skilltarget.Page
	listQuery skilltarget.ListQuery
	disabled  skilltarget.DisableRecord
}

func (s *skillTargetRepositoryStub) GetSkillTarget(_ context.Context, id snowflake.ID) (skilltarget.Target, error) {
	s.getID = id
	return s.found, nil
}
func (s *skillTargetRepositoryStub) ListSkillTargets(_ context.Context, query skilltarget.ListQuery) (skilltarget.Page, error) {
	s.listQuery = query
	return s.page, nil
}
func (s *skillTargetRepositoryStub) Create(_ context.Context, record skilltarget.CreateRecord) (skilltarget.Target, error) {
	s.created = record
	return record.Target, nil
}
func (s *skillTargetRepositoryStub) Update(_ context.Context, record skilltarget.UpdateRecord) (skilltarget.Target, error) {
	s.updated = record
	return record.Target, nil
}
func (s *skillTargetRepositoryStub) Disable(_ context.Context, record skilltarget.DisableRecord) error {
	s.disabled = record
	return nil
}
func (s *skillTargetRepositoryStub) WithinSkillTarget(_ context.Context, work func(skilltarget.Writer) error) error {
	return work(s)
}
