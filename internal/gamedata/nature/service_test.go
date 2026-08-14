package nature_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/nature"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
)

func TestServiceCreatesNeutralAndNonNeutralNatureButRejectsHalfSpecifiedModifier(t *testing.T) {
	repository := &natureAdaptersStub{}
	service := nature.NewService(repository, repository, repository, natureStatReaderStub{}, snowflake.NewTestID, time.Now)
	contextValue := administration.NewGameDataWriteContext(snowflake.NewTestID(), "nature-create", "request")
	neutral, err := service.Create(context.Background(), nature.CreateCommand{GameDataWriteContext: contextValue, Code: "hardy", Name: "勤奋", Enabled: true})
	if err != nil || neutral.IncreasedStatID != nil || neutral.DecreasedStatID != nil {
		t.Fatalf("Create(neutral) = %+v, %v", neutral, err)
	}
	attackID := snowflake.NewTestID()
	_, err = service.Create(context.Background(), nature.CreateCommand{GameDataWriteContext: contextValue, Code: "invalid", Name: "无效", IncreasedStatID: &attackID, Enabled: true})
	if err != nature.ErrInvalidNature {
		t.Fatalf("Create(half specified) error = %v", err)
	}
}

type natureStatReaderStub struct{}

func (natureStatReaderStub) Get(_ context.Context, id snowflake.ID) (stat.Stat, error) {
	return stat.Stat{ID: id, Code: "attack", Enabled: true}, nil
}

type natureAdaptersStub struct{ value nature.Nature }

func (s *natureAdaptersStub) WithinNature(_ context.Context, work func(nature.Writer) error) error {
	return work(s)
}
func (s *natureAdaptersStub) Create(_ context.Context, record nature.CreateRecord) (nature.Nature, error) {
	s.value = record.Nature
	return s.value, nil
}
func (s *natureAdaptersStub) Update(_ context.Context, record nature.UpdateRecord) (nature.Nature, error) {
	s.value = record.Nature
	return s.value, nil
}
func (s *natureAdaptersStub) GetNature(context.Context, snowflake.ID) (nature.Nature, error) {
	return s.value, nil
}
func (s *natureAdaptersStub) ListNatures(context.Context, nature.ListQuery) (nature.Page, error) {
	return nature.Page{}, nil
}
