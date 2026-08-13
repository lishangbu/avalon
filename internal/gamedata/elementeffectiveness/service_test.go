package elementeffectiveness_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/gamedata/elementeffectiveness"
)

func TestServiceCreateAcceptsOnlyCanonicalNonNeutralMultiplier(t *testing.T) {
	store := &effectivenessStoreStub{}
	id, attackID, defenseID, accountID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	service := elementeffectiveness.NewService(store, snowflake.TestSource(func() snowflake.ID { return id }), func() time.Time { return time.Unix(10, 0) })
	created, err := service.Create(context.Background(), elementeffectiveness.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(accountID, "effectiveness-create", "request-1"),
		AttackElementID:      attackID, DefenseElementID: defenseID, Numerator: 2, Denominator: 1, Enabled: true,
	})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if created.ID != id || created.Numerator != 2 || created.Denominator != 1 || created.Version != 1 {
		t.Fatalf("Create() = %+v", created)
	}
	_, err = service.Create(context.Background(), elementeffectiveness.CreateCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(accountID, "effectiveness-invalid", "request-2"),
		AttackElementID:      attackID, DefenseElementID: defenseID, Numerator: 1, Denominator: 1, Enabled: true,
	})
	if err != elementeffectiveness.ErrInvalidEffectiveness {
		t.Fatalf("Create(neutral) error = %v", err)
	}
}

type effectivenessStoreStub struct {
	value elementeffectiveness.Effectiveness
}

func (s *effectivenessStoreStub) WithinElementEffectiveness(_ context.Context, work func(elementeffectiveness.Writer) error) error {
	return work(s)
}
func (s *effectivenessStoreStub) Create(_ context.Context, record elementeffectiveness.CreateRecord) (elementeffectiveness.Effectiveness, error) {
	s.value = record.Effectiveness
	return s.value, nil
}
func (s *effectivenessStoreStub) Update(_ context.Context, record elementeffectiveness.UpdateRecord) (elementeffectiveness.Effectiveness, error) {
	s.value = record.Effectiveness
	return s.value, nil
}
func (s *effectivenessStoreStub) Get(context.Context, snowflake.ID) (elementeffectiveness.Effectiveness, error) {
	return s.value, nil
}
func (s *effectivenessStoreStub) List(context.Context, elementeffectiveness.ListQuery) (elementeffectiveness.Page, error) {
	return elementeffectiveness.Page{}, nil
}
func (s *effectivenessStoreStub) ListEnabled(context.Context) ([]elementeffectiveness.Effectiveness, error) {
	return nil, nil
}
