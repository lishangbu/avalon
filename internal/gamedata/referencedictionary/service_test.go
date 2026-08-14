package referencedictionary

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

type testRepository struct{ saved Entry }

func (s *testRepository) ListReferenceDictionary(context.Context, Kind) ([]Entry, error) {
	return []Entry{s.saved}, nil
}
func (s *testRepository) CreateReferenceDictionary(_ context.Context, entry Entry, _ administration.GameDataWriteContext, _ time.Time) (Entry, error) {
	s.saved = entry
	return entry, nil
}
func (s *testRepository) UpdateReferenceDictionary(_ context.Context, entry Entry, _ int64, _ administration.GameDataWriteContext, _ time.Time) (Entry, error) {
	s.saved = entry
	return entry, nil
}

type testIDs struct{}

func (testIDs) Next(context.Context) (snowflake.ID, error) { return snowflake.ID(101), nil }

func TestServiceCreatesSpecializedEntries(t *testing.T) {
	repository := &testRepository{}
	service := NewService(repository, testIDs{}, func() time.Time { return time.Unix(1, 0) })
	formula, description := " n * n ", " 说明 "
	value, err := service.Create(context.Background(), CreateCommand{GameDataWriteContext: administration.GameDataWriteContext{ActorAccountID: 1, IdempotencyKey: "request-key", RequestID: "request-id"}, Entry: Entry{Kind: KindGrowthRate, Code: " medium ", Name: " 中速 ", Formula: &formula, Description: &description, Enabled: true}})
	if err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if value.ID != 101 || value.Version != 1 || value.Code != "medium" || *value.Formula != "n * n" {
		t.Fatalf("Create() = %#v", value)
	}
}

func TestServiceRejectsFieldsFromAnotherResource(t *testing.T) {
	service := NewService(&testRepository{}, testIDs{}, time.Now)
	formula := "n"
	_, err := service.Create(context.Background(), CreateCommand{GameDataWriteContext: administration.GameDataWriteContext{ActorAccountID: 1, IdempotencyKey: "request-key", RequestID: "request-id"}, Entry: Entry{Kind: KindHabitat, Code: "forest", Name: "森林", Formula: &formula}})
	if err != ErrInvalid {
		t.Fatalf("Create() error = %v, want ErrInvalid", err)
	}
}
