package skillstatchange_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/skillstatchange"
)

func TestServiceManagesValidatedSkillStatChange(t *testing.T) {
	t.Parallel()
	changeID := snowflake.MustParse("1048576047")
	skillID := snowflake.MustParse("1048576048")
	statID := snowflake.MustParse("1048576049")
	store := &storeStub{}
	service := skillstatchange.NewService(store, snowflake.TestSource(func() snowflake.ID { return changeID }),
		func() time.Time { return time.Date(2026, time.July, 28, 1, 0, 0, 0, time.UTC) })
	writeContext := administration.NewGameDataWriteContext(snowflake.NewTestID(), "skill-stat-change-key", "request-id")
	created, err := service.Create(context.Background(), skillstatchange.CreateCommand{
		GameDataWriteContext: writeContext, SkillID: skillID, StatID: statID, ChangeValue: -2,
	})
	if err != nil || created.ID != changeID || created.Version != 1 || store.created.ChangeValue != -2 {
		t.Fatalf("Create() = %+v, stored = %+v, error = %v", created, store.created, err)
	}
	updated, err := service.Update(context.Background(), skillstatchange.UpdateCommand{
		GameDataWriteContext: writeContext, ChangeID: changeID, SkillID: skillID, StatID: statID,
		ChangeValue: 3, ExpectedVersion: 1,
	})
	if err != nil || updated.Version != 2 || store.updated.ChangeValue != 3 {
		t.Fatalf("Update() = %+v, stored = %+v, error = %v", updated, store.updated, err)
	}
	if err := service.Disable(context.Background(), skillstatchange.DisableCommand{
		GameDataWriteContext: writeContext, ChangeID: changeID, ExpectedVersion: 2,
	}); err != nil || store.disabled.ChangeID != changeID {
		t.Fatalf("Disable() error = %v, record = %+v", err, store.disabled)
	}
}

func TestServiceRejectsZeroAndOutOfRangeChanges(t *testing.T) {
	t.Parallel()
	service := skillstatchange.NewService(&storeStub{}, snowflake.NewTestID, time.Now)
	for _, value := range []int32{-7, 0, 7} {
		_, err := service.Create(context.Background(), skillstatchange.CreateCommand{
			GameDataWriteContext: administration.NewGameDataWriteContext(snowflake.NewTestID(), "key", "request"),
			SkillID:              snowflake.NewTestID(), StatID: snowflake.NewTestID(), ChangeValue: value,
		})
		if !errors.Is(err, skillstatchange.ErrInvalidSkillStatChange) {
			t.Fatalf("Create(changeValue=%d) error = %v", value, err)
		}
	}
}

type storeStub struct {
	created  skillstatchange.Change
	updated  skillstatchange.Change
	disabled skillstatchange.DisableRecord
}

func (s *storeStub) GetSkillStatChange(context.Context, snowflake.ID) (skillstatchange.Change, error) {
	return s.created, nil
}

func (s *storeStub) ListSkillStatChanges(context.Context, skillstatchange.ListQuery) (skillstatchange.Page, error) {
	return skillstatchange.Page{}, nil
}

func (s *storeStub) WithinSkillStatChange(ctx context.Context, work func(skillstatchange.Writer) error) error {
	return work(s)
}

func (s *storeStub) Create(_ context.Context, record skillstatchange.CreateRecord) (skillstatchange.Change, error) {
	s.created = record.Change
	return record.Change, nil
}

func (s *storeStub) Update(_ context.Context, record skillstatchange.UpdateRecord) (skillstatchange.Change, error) {
	s.updated = record.Change
	return record.Change, nil
}

func (s *storeStub) Disable(_ context.Context, record skillstatchange.DisableRecord) error {
	s.disabled = record
	return nil
}
