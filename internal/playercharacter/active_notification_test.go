package playercharacter_test

import (
	"context"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/playercharacter"
)

func TestActiveBindingHubCoalescesSlowSubscriberToLatestAccountBinding(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576082")
	hub := playercharacter.NewActiveBindingHub()
	updates, cancel := hub.Subscribe(accountID)
	defer cancel()
	first := playercharacter.ActiveBinding{AccountID: accountID, PlayerCharacterID: snowflake.NewTestID(), Version: 1}
	second := playercharacter.ActiveBinding{AccountID: accountID, PlayerCharacterID: snowflake.NewTestID(), Version: 2}

	hub.ActivePlayerCharacterChanged(context.Background(), first)
	hub.ActivePlayerCharacterChanged(context.Background(), second)
	if received := <-updates; received != second {
		t.Fatalf("received = %+v, want latest %+v", received, second)
	}
}
