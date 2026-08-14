package skilldamageclass_test

import (
	"context"
	"errors"
	"strings"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/skilldamageclass"
)

func TestServiceCreatesNormalizedSkillDamageClassInLive(t *testing.T) {
	t.Parallel()

	damageClassID := snowflake.MustParse("1048576022")
	actorID := snowflake.MustParse("1048576023")
	now := time.Date(2026, time.July, 27, 19, 0, 0, 0, time.UTC)
	store := &skillDamageClassRepositoryStub{}
	service := skilldamageclass.NewService(
		store,
		snowflake.TestSource(func() snowflake.ID { return damageClassID }),
		func() time.Time { return now },
	)
	description := "  造成直接伤害的技能分类。  "

	created, err := service.Create(context.Background(), skilldamageclass.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-physical-damage-class",
			"create-physical-damage-class-request",
		),
		Code:        " physical ",
		Name:        "  物理  ",
		Description: &description,
		SortOrder:   1,
		Enabled:     true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != damageClassID || created.Code != "physical" || created.Name != "物理" ||
		created.Description == nil || *created.Description != "造成直接伤害的技能分类。" ||
		created.SortOrder != 1 || !created.Enabled || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}
	if store.created.DamageClass.ID != created.ID || store.created.ActorAccountID != actorID || !store.created.CreatedAt.Equal(now) {
		t.Fatalf("Create record = %+v", store.created)
	}
}

func TestServiceUpdatesSkillDamageClassWithoutClearingOmittedDescription(t *testing.T) {
	t.Parallel()

	damageClassID := snowflake.MustParse("1048576022")
	actorID := snowflake.MustParse("1048576023")
	now := time.Date(2026, time.July, 27, 19, 30, 0, 0, time.UTC)
	preservedDescription := "造成直接伤害的技能分类。"
	store := &skillDamageClassRepositoryStub{updatedResult: skilldamageclass.DamageClass{
		ID: damageClassID, Code: "physical", Name: "物理伤害", Description: &preservedDescription,
		SortOrder: 2, Enabled: false, Version: 3,
	}}
	service := skilldamageclass.NewService(store, snowflake.NewTestID, func() time.Time { return now })

	updated, err := service.Update(context.Background(), skilldamageclass.UpdateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "update-physical-damage-class",
			"update-physical-damage-class-request",
		),
		DamageClassID:   damageClassID,
		ExpectedVersion: 2,
		Code:            "physical",
		Name:            "  物理伤害  ",
		Description:     skilldamageclass.DescriptionChange{},
		SortOrder:       2,
		Enabled:         false,
	})
	if err != nil {
		t.Fatalf("Update() error = %v", err)
	}
	if updated.Description == nil || *updated.Description != preservedDescription || updated.Version != 3 {
		t.Fatalf("Update() = %+v", updated)
	}
	if store.updated.DamageClass.Name != "物理伤害" || store.updated.Description.Specified ||
		store.updated.ExpectedVersion != 2 || !store.updated.UpdatedAt.Equal(now) {
		t.Fatalf("Update record = %+v", store.updated)
	}
}

func TestServiceGetsSkillDamageClassFromLive(t *testing.T) {
	t.Parallel()

	damageClassID := snowflake.MustParse("1048576022")
	want := skilldamageclass.DamageClass{
		ID: damageClassID, Code: "physical", Name: "物理", SortOrder: 1, Enabled: true, Version: 2,
	}
	store := &skillDamageClassRepositoryStub{found: want}
	service := skilldamageclass.NewService(store, snowflake.NewTestID, time.Now)

	got, err := service.Get(context.Background(), damageClassID)
	if err != nil {
		t.Fatalf("Get() error = %v", err)
	}
	if got.ID != want.ID || got.Code != want.Code || got.Version != want.Version || store.getID != damageClassID {
		t.Fatalf("Get() = %+v, queried ID = %s", got, store.getID)
	}
}

func TestServiceListsSkillDamageClassesWithNormalizedDefaults(t *testing.T) {
	t.Parallel()

	want := skilldamageclass.Page{
		Items: []skilldamageclass.DamageClass{{Code: "physical"}}, Total: 1, Page: 1, PageSize: 20,
	}
	store := &skillDamageClassRepositoryStub{page: want}
	service := skilldamageclass.NewService(store, snowflake.NewTestID, time.Now)

	got, err := service.List(context.Background(), skilldamageclass.ListQuery{Q: "  物理  "})
	if err != nil {
		t.Fatalf("List() error = %v", err)
	}
	if got.Total != want.Total || len(got.Items) != 1 || got.Items[0].Code != "physical" {
		t.Fatalf("List() = %+v", got)
	}
	if store.listQuery.Page != 1 || store.listQuery.PageSize != 20 || store.listQuery.Q != "物理" ||
		store.listQuery.Sort != skilldamageclass.SortCodeAscending {
		t.Fatalf("List query = %+v", store.listQuery)
	}
}

func TestServiceDeletesSkillDamageClassWithOptimisticVersion(t *testing.T) {
	t.Parallel()

	damageClassID := snowflake.MustParse("1048576022")
	actorID := snowflake.MustParse("1048576023")
	now := time.Date(2026, time.July, 27, 20, 0, 0, 0, time.UTC)
	store := &skillDamageClassRepositoryStub{}
	service := skilldamageclass.NewService(store, snowflake.NewTestID, func() time.Time { return now })

	err := service.Disable(context.Background(), skilldamageclass.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-physical-damage-class",
			"delete-physical-damage-class-request",
		),
		DamageClassID:   damageClassID,
		ExpectedVersion: 3,
	})
	if err != nil {
		t.Fatalf("Delete() error = %v", err)
	}
	if store.disabled.DamageClassID != damageClassID || store.disabled.ExpectedVersion != 3 ||
		!store.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v", store.disabled)
	}
}

func TestServiceNormalizesExplicitBlankDescriptionToClear(t *testing.T) {
	t.Parallel()

	damageClassID := snowflake.MustParse("1048576022")
	blank := "  \t  "
	store := &skillDamageClassRepositoryStub{updatedResult: skilldamageclass.DamageClass{
		ID: damageClassID, Code: "physical", Name: "物理", Version: 2,
	}}
	service := skilldamageclass.NewService(store, snowflake.NewTestID, time.Now)

	_, err := service.Update(context.Background(), skilldamageclass.UpdateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(snowflake.MustParse("1048576023"), "clear-blank-description",
			"clear-blank-description-request",
		),
		DamageClassID: damageClassID, ExpectedVersion: 1, Code: "physical", Name: "物理",
		Description: skilldamageclass.DescriptionChange{Specified: true, Value: &blank}, Enabled: true,
	})
	if err != nil {
		t.Fatalf("Update() error = %v", err)
	}
	if !store.updated.Description.Specified || store.updated.Description.Value != nil {
		t.Fatalf("Description change = %+v", store.updated.Description)
	}
}

func TestServiceRejectsInvalidSkillDamageClassFields(t *testing.T) {
	t.Parallel()

	longDescription := strings.Repeat("技", 501)
	longName := strings.Repeat("名", 81)
	tests := []struct {
		name   string
		change func(*skilldamageclass.CreateCommand)
	}{
		{name: "rejects one-character stable code", change: func(command *skilldamageclass.CreateCommand) {
			command.Code = "x"
		}},
		{name: "rejects blank name", change: func(command *skilldamageclass.CreateCommand) {
			command.Name = "  "
		}},
		{name: "rejects name over eighty Unicode characters", change: func(command *skilldamageclass.CreateCommand) {
			command.Name = longName
		}},
		{name: "rejects description over five hundred Unicode characters", change: func(command *skilldamageclass.CreateCommand) {
			command.Description = &longDescription
		}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()

			command := skilldamageclass.CreateCommand{
				GameDataWriteContext: administration.NewGameDataWriteContext(snowflake.MustParse("1048576023"), "reject-invalid-damage-class",
					"reject-invalid-damage-class-request",
				),
				Code: "physical", Name: "物理", Enabled: true,
			}
			test.change(&command)
			service := skilldamageclass.NewService(
				&skillDamageClassRepositoryStub{},
				snowflake.TestSource(func() snowflake.ID {
					t.Fatal("invalid command must not generate an ID")
					return snowflake.ID(0)
				}),
				time.Now,
			)

			_, err := service.Create(context.Background(), command)
			if !errors.Is(err, skilldamageclass.ErrInvalidSkillDamageClass) {
				t.Fatalf("Create() error = %v, want ErrInvalidSkillDamageClass", err)
			}
		})
	}
}

type skillDamageClassRepositoryStub struct {
	created       skilldamageclass.CreateRecord
	updated       skilldamageclass.UpdateRecord
	updatedResult skilldamageclass.DamageClass
	found         skilldamageclass.DamageClass
	getID         snowflake.ID
	page          skilldamageclass.Page
	listQuery     skilldamageclass.ListQuery
	disabled      skilldamageclass.DisableRecord
}

func (s *skillDamageClassRepositoryStub) Disable(
	_ context.Context,
	record skilldamageclass.DisableRecord,
) error {
	s.disabled = record
	return nil
}

func (s *skillDamageClassRepositoryStub) ListSkillDamageClasses(
	_ context.Context,
	query skilldamageclass.ListQuery,
) (skilldamageclass.Page, error) {
	s.listQuery = query
	return s.page, nil
}

func (s *skillDamageClassRepositoryStub) GetSkillDamageClass(
	_ context.Context,
	damageClassID snowflake.ID,
) (skilldamageclass.DamageClass, error) {
	s.getID = damageClassID
	return s.found, nil
}

func (s *skillDamageClassRepositoryStub) Update(
	_ context.Context,
	record skilldamageclass.UpdateRecord,
) (skilldamageclass.DamageClass, error) {
	s.updated = record
	return s.updatedResult, nil
}

func (s *skillDamageClassRepositoryStub) Create(
	_ context.Context,
	record skilldamageclass.CreateRecord,
) (skilldamageclass.DamageClass, error) {
	s.created = record
	return record.DamageClass, nil
}

func (s *skillDamageClassRepositoryStub) WithinSkillDamageClass(
	_ context.Context,
	work func(skilldamageclass.Writer) error,
) error {
	return work(s)
}
