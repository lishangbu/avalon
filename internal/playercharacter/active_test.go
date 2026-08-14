package playercharacter_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/playercharacter"
)

func TestActiveServiceSwitchesSharedBindingAndClearsPreviousPresence(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576077")
	previousID := snowflake.MustParse("1048576078")
	nextID := snowflake.MustParse("1048576079")
	now := time.Date(2026, time.July, 29, 4, 0, 0, 0, time.UTC)
	repository := &activeAdaptersStub{switched: playercharacter.SwitchActiveResult{
		Binding: playercharacter.ActiveBinding{
			AccountID: accountID, PlayerCharacterID: nextID, Version: 2, UpdatedAt: now,
		},
		PreviousPlayerCharacterID: previousID,
	}}
	presence := playercharacter.NewPresenceRegistry(time.Minute)
	presence.Open(previousID, snowflake.NewTestID(), now)
	notifier := &activeNotifierStub{}
	service := playercharacter.NewActiveService(repository, presence, notifier, func() time.Time { return now })

	binding, err := service.Switch(context.Background(), playercharacter.SwitchActiveCommand{
		AccountID: accountID, PlayerCharacterID: nextID, ExpectedVersion: 1,
		IdempotencyKey: "switch-active-character", RequestID: "switch-active-character-request",
	})
	if err != nil {
		t.Fatalf("Switch() error = %v", err)
	}
	if binding != repository.switched.Binding {
		t.Fatalf("Switch() = %+v, record = %+v", binding, repository.record)
	}
	if presence.Online(previousID, now) {
		t.Fatal("旧 PlayerCharacter Presence 未在切换后清除")
	}
	if notifier.binding != binding {
		t.Fatalf("published binding = %+v", notifier.binding)
	}
}

func TestActiveServiceDoesNotRepeatPresenceSideEffectsForDelayedIdempotentReplay(t *testing.T) {
	t.Parallel()

	currentID := snowflake.MustParse("1048576080")
	replayedID := snowflake.MustParse("1048576081")
	now := time.Date(2026, time.July, 29, 4, 5, 0, 0, time.UTC)
	repository := &activeAdaptersStub{switched: playercharacter.SwitchActiveResult{
		Binding:                   playercharacter.ActiveBinding{PlayerCharacterID: replayedID, Version: 2},
		PreviousPlayerCharacterID: currentID,
		Replayed:                  true,
	}}
	presence := playercharacter.NewPresenceRegistry(time.Minute)
	presence.Open(currentID, snowflake.NewTestID(), now)
	notifier := &activeNotifierStub{}
	service := playercharacter.NewActiveService(repository, presence, notifier, func() time.Time { return now })

	if _, err := service.Switch(context.Background(), playercharacter.SwitchActiveCommand{
		AccountID: snowflake.NewTestID(), PlayerCharacterID: replayedID, ExpectedVersion: 1,
		IdempotencyKey: "delayed-replay", RequestID: "delayed-replay-request",
	}); err != nil {
		t.Fatalf("Switch() error = %v", err)
	}
	if !presence.Online(currentID, now) {
		t.Fatal("迟到的幂等重放不应清除较新活动角色的 Presence")
	}
	if notifier.binding != (playercharacter.ActiveBinding{}) {
		t.Fatalf("迟到的幂等重放不应广播旧绑定: %+v", notifier.binding)
	}
}

type activeAdaptersStub struct {
	current  playercharacter.ActiveBinding
	switched playercharacter.SwitchActiveResult
	record   playercharacter.SwitchActiveRecord
}

func (s *activeAdaptersStub) GetActive(context.Context, snowflake.ID) (playercharacter.ActiveBinding, error) {
	return s.current, nil
}

func (s *activeAdaptersStub) SwitchActive(_ context.Context, record playercharacter.SwitchActiveRecord) (playercharacter.SwitchActiveResult, error) {
	s.record = record
	return s.switched, nil
}

type activeNotifierStub struct {
	binding playercharacter.ActiveBinding
}

func (n *activeNotifierStub) ActivePlayerCharacterChanged(_ context.Context, binding playercharacter.ActiveBinding) {
	n.binding = binding
}
