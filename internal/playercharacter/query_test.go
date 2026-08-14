package playercharacter_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/playercharacter"
)

func TestQueryServiceOnlyExposesPublicCharacterAfterCallerHasActiveBinding(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576089")
	callerID := snowflake.MustParse("1048576090")
	targetID := snowflake.MustParse("1048576091")
	now := time.Date(2026, time.July, 29, 4, 30, 0, 0, time.UTC)
	repository := &queryStub{
		active: playercharacter.ActiveBinding{AccountID: accountID, PlayerCharacterID: callerID, Version: 1},
		found:  playercharacter.PlayerCharacter{ID: targetID, DisplayName: "星界旅人", DisplayNameKey: "星界旅人"},
	}
	presence := playercharacter.NewPresenceRegistry(time.Minute)
	presence.Open(targetID, snowflake.NewTestID(), now)
	service := playercharacter.NewQueryService(repository, presence, func() time.Time { return now })

	result, err := service.FindPublicByDisplayName(context.Background(), accountID, "  星界旅人 ")
	if err != nil {
		t.Fatalf("FindPublicByDisplayName() error = %v", err)
	}
	if result.DisplayName != "星界旅人" || !result.Online || !result.Challengeable {
		t.Fatalf("FindPublicByDisplayName() = %+v", result)
	}
	if repository.displayNameKey != "星界旅人" {
		t.Fatalf("displayNameKey = %q", repository.displayNameKey)
	}
}

type queryStub struct {
	active         playercharacter.ActiveBinding
	found          playercharacter.PlayerCharacter
	displayNameKey string
}

func (s *queryStub) GetOwned(context.Context, snowflake.ID, snowflake.ID) (playercharacter.PlayerCharacter, error) {
	return playercharacter.PlayerCharacter{}, nil
}

func (s *queryStub) ListOwned(context.Context, snowflake.ID, bool) ([]playercharacter.PlayerCharacter, error) {
	return nil, nil
}

func (s *queryStub) GetActive(context.Context, snowflake.ID) (playercharacter.ActiveBinding, error) {
	return s.active, nil
}

func (s *queryStub) FindActiveByDisplayNameKey(_ context.Context, key string) (playercharacter.PlayerCharacter, error) {
	s.displayNameKey = key
	return s.found, nil
}
