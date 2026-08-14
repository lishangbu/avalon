package skill_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/skill"
)

func TestServiceCreatesNormalizedSkillInLive(t *testing.T) {
	t.Parallel()

	skillID := snowflake.MustParse("1048576024")
	elementID := snowflake.MustParse("1048576025")
	damageClassID := snowflake.MustParse("1048576026")
	actorID := snowflake.MustParse("1048576027")
	accuracy, power, pp, effectChance := int32(100), int32(40), int32(35), int32(10)
	now := time.Date(2026, time.July, 27, 21, 0, 0, 0, time.UTC)
	repository := &skillRepositoryStub{}
	service := skill.NewService(
		repository, repository, repository,
		snowflake.TestSource(func() snowflake.ID { return skillID }),
		func() time.Time { return now })

	created, err := service.Create(context.Background(), skill.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "create-tackle-skill", "create-tackle-skill-request"),
		Code:                 " tackle ", Name: "  撞击  ",
		OptionalValues: skill.OptionalValues{
			ElementID: &elementID, DamageClassID: &damageClassID, Accuracy: &accuracy,
			Power: &power, PP: &pp, EffectChance: &effectChance,
		},
		Priority: 0, Enabled: true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != skillID || created.Code != "tackle" || created.Name != "撞击" ||
		created.ElementID == nil || *created.ElementID != elementID ||
		created.DamageClassID == nil || *created.DamageClassID != damageClassID ||
		created.Accuracy == nil || *created.Accuracy != 100 || created.Power == nil || *created.Power != 40 ||
		created.PP == nil || *created.PP != 35 || created.EffectChance == nil || *created.EffectChance != 10 ||
		created.Priority != 0 || !created.Enabled || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}
	if repository.created.Skill.ID != created.ID || repository.created.ActorAccountID != actorID || !repository.created.CreatedAt.Equal(now) {
		t.Fatalf("Create record = %+v", repository.created)
	}
}

func TestServiceUpdatesSkillWithIndependentNullableFieldChanges(t *testing.T) {
	t.Parallel()

	skillID := snowflake.MustParse("1048576024")
	elementID := snowflake.MustParse("1048576025")
	accuracy := int32(95)
	preservedPower := int32(40)
	result := skill.Skill{
		ID: skillID, Code: "tackle", Name: "猛撞", Priority: 1, Enabled: false, Version: 3,
		OptionalValues: skill.OptionalValues{ElementID: &elementID, Accuracy: &accuracy, Power: &preservedPower},
	}
	repository := &skillRepositoryStub{updatedResult: result}
	now := time.Date(2026, time.July, 27, 21, 30, 0, 0, time.UTC)
	service := skill.NewService(repository, repository, repository, snowflake.NewTestID, func() time.Time { return now })

	updated, err := service.Update(context.Background(), skill.UpdateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(snowflake.MustParse("1048576027"), "update-tackle-skill", "update-tackle-skill-request"),
		SkillID:              skillID, ExpectedVersion: 2, Code: "tackle", Name: "  猛撞  ",
		Changes: skill.OptionalChanges{
			ElementID:     skill.Change[snowflake.ID]{Specified: true, Value: &elementID},
			DamageClassID: skill.Change[snowflake.ID]{Specified: true},
			Accuracy:      skill.Change[int32]{Specified: true, Value: &accuracy},
			Power:         skill.Change[int32]{},
		},
		Priority: 1, Enabled: false,
	})
	if err != nil {
		t.Fatalf("Update() error = %v", err)
	}
	if updated.ID != skillID || updated.Version != 3 || updated.Power == nil || *updated.Power != preservedPower {
		t.Fatalf("Update() = %+v", updated)
	}
	if repository.updated.Skill.Name != "猛撞" || !repository.updated.Changes.ElementID.Specified ||
		!repository.updated.Changes.DamageClassID.Specified || repository.updated.Changes.DamageClassID.Value != nil ||
		!repository.updated.Changes.Accuracy.Specified || repository.updated.Changes.Power.Specified ||
		repository.updated.ExpectedVersion != 2 || !repository.updated.UpdatedAt.Equal(now) {
		t.Fatalf("Update record = %+v", repository.updated)
	}
}

func TestServiceGetsListsAndDeletesSkillThroughPublicBoundaries(t *testing.T) {
	t.Parallel()

	skillID := snowflake.MustParse("1048576024")
	actorID := snowflake.MustParse("1048576027")
	want := skill.Skill{ID: skillID, Code: "tackle", Name: "撞击", Enabled: true, Version: 2}
	repository := &skillRepositoryStub{
		found: want,
		page:  skill.Page{Items: []skill.Skill{want}, Total: 1, Page: 1, PageSize: 20},
	}
	now := time.Date(2026, time.July, 27, 22, 0, 0, 0, time.UTC)
	service := skill.NewService(repository, repository, repository, snowflake.NewTestID, func() time.Time { return now })

	got, err := service.Get(context.Background(), skillID)
	if err != nil || got.ID != want.ID || repository.getID != skillID {
		t.Fatalf("Get() = %+v, error = %v, queried ID = %s", got, err, repository.getID)
	}
	page, err := service.List(context.Background(), skill.ListQuery{Q: "  撞击  "})
	if err != nil || page.Total != 1 || len(page.Items) != 1 || page.Items[0].ID != skillID {
		t.Fatalf("List() = %+v, error = %v", page, err)
	}
	if repository.listQuery.Page != 1 || repository.listQuery.PageSize != 20 || repository.listQuery.Q != "撞击" ||
		repository.listQuery.Sort != skill.SortCodeAscending {
		t.Fatalf("List query = %+v", repository.listQuery)
	}
	err = service.Disable(context.Background(), skill.DisableCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "delete-tackle-skill", "delete-tackle-skill-request"),
		SkillID:              skillID, ExpectedVersion: 2,
	})
	if err != nil || repository.disabled.SkillID != skillID || repository.disabled.ExpectedVersion != 2 ||
		!repository.disabled.DisabledAt.Equal(now) {
		t.Fatalf("Delete record = %+v, error = %v", repository.disabled, err)
	}
}

func TestServiceRejectsInvalidSkillDomainValues(t *testing.T) {
	t.Parallel()

	actorID := snowflake.MustParse("1048576027")
	validElementID := snowflake.MustParse("1048576025")
	validDamageClassID := snowflake.MustParse("1048576026")
	validAccuracy, validPower, validPP, validEffectChance := int32(100), int32(0), int32(1), int32(0)
	base := skill.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "invalid-skill", "invalid-skill-request"),
		OptionalValues: skill.OptionalValues{
			ElementID: &validElementID, DamageClassID: &validDamageClassID,
			Accuracy: &validAccuracy, Power: &validPower, PP: &validPP, EffectChance: &validEffectChance,
		},
		Code: "valid-skill", Name: "有效技能", Priority: 0, Enabled: true,
	}
	tests := []struct {
		name   string
		mutate func(*skill.CreateCommand)
	}{
		{name: "稳定编码无效", mutate: func(command *skill.CreateCommand) { command.Code = "INVALID" }},
		{name: "名称为空", mutate: func(command *skill.CreateCommand) { command.Name = "  " }},
		{name: "名称超过上限", mutate: func(command *skill.CreateCommand) { command.Name = string(make([]rune, 121)) }},
		{name: "属性 Identifier 为空", mutate: func(command *skill.CreateCommand) { command.ElementID = identifierPointer(snowflake.ID(0)) }},
		{name: "伤害分类 Identifier 为空", mutate: func(command *skill.CreateCommand) { command.DamageClassID = identifierPointer(snowflake.ID(0)) }},
		{name: "命中率小于下限", mutate: func(command *skill.CreateCommand) { command.Accuracy = int32Pointer(0) }},
		{name: "命中率超过上限", mutate: func(command *skill.CreateCommand) { command.Accuracy = int32Pointer(101) }},
		{name: "威力为负数", mutate: func(command *skill.CreateCommand) { command.Power = int32Pointer(-1) }},
		{name: "PP 不为正数", mutate: func(command *skill.CreateCommand) { command.PP = int32Pointer(0) }},
		{name: "效果概率小于下限", mutate: func(command *skill.CreateCommand) { command.EffectChance = int32Pointer(-1) }},
		{name: "效果概率超过上限", mutate: func(command *skill.CreateCommand) { command.EffectChance = int32Pointer(101) }},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()

			command := base
			test.mutate(&command)
			repository := &skillRepositoryStub{}
			service := skill.NewService(repository, repository, repository, snowflake.NewTestID, time.Now)
			if _, err := service.Create(context.Background(), command); !errors.Is(err, skill.ErrInvalidSkill) {
				t.Fatalf("Create() error = %v, want ErrInvalidSkill", err)
			}
			if repository.created.Skill.ID != snowflake.ID(0) {
				t.Fatalf("invalid command reached Repository.Create(): %+v", repository.created)
			}
		})
	}
}

func identifierPointer(value snowflake.ID) *snowflake.ID {
	return &value
}

func int32Pointer(value int32) *int32 {
	return &value
}

type skillRepositoryStub struct {
	created       skill.CreateRecord
	updated       skill.UpdateRecord
	updatedResult skill.Skill
	found         skill.Skill
	getID         snowflake.ID
	page          skill.Page
	listQuery     skill.ListQuery
	disabled      skill.DisableRecord
}

func (s *skillRepositoryStub) GetSkill(_ context.Context, skillID snowflake.ID) (skill.Skill, error) {
	s.getID = skillID
	return s.found, nil
}

func (s *skillRepositoryStub) ListSkills(_ context.Context, query skill.ListQuery) (skill.Page, error) {
	s.listQuery = query
	return s.page, nil
}

func (s *skillRepositoryStub) Disable(_ context.Context, record skill.DisableRecord) error {
	s.disabled = record
	return nil
}

func (s *skillRepositoryStub) Create(_ context.Context, record skill.CreateRecord) (skill.Skill, error) {
	s.created = record
	return record.Skill, nil
}

func (s *skillRepositoryStub) Update(_ context.Context, record skill.UpdateRecord) (skill.Skill, error) {
	s.updated = record
	return s.updatedResult, nil
}

func (s *skillRepositoryStub) WithinSkill(_ context.Context, work func(skill.Writer) error) error {
	return work(s)
}
