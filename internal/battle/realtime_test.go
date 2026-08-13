package battle_test

import (
	"context"
	"sync"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
)

func TestRealtimeHubSeparatesParticipantViewsAndDisconnectsSlowSubscriber(t *testing.T) {
	t.Parallel()
	battleID := snowflake.MustParse("1048576175")
	firstCharacterID := snowflake.MustParse("1048576176")
	secondCharacterID := snowflake.MustParse("1048576177")
	reader := &disclosureReaderStub{views: map[snowflake.ID]battle.DisclosureView{
		firstCharacterID:  {SchemaVersion: 1, StateVersion: 1},
		secondCharacterID: {SchemaVersion: 1, StateVersion: 1},
	}}
	hub := battle.NewRealtimeHub(reader, 1)
	first, err := hub.Subscribe(context.Background(), battleID, firstCharacterID)
	if err != nil {
		t.Fatalf("Subscribe(first) error = %v", err)
	}
	second, err := hub.Subscribe(context.Background(), battleID, secondCharacterID)
	if err != nil {
		t.Fatalf("Subscribe(second) error = %v", err)
	}
	defer second.Close()

	if view := <-first.Views; view.StateVersion != 1 {
		t.Fatalf("first initial view = %+v, want version 1", view)
	}
	if view := <-second.Views; view.StateVersion != 1 {
		t.Fatalf("second initial view = %+v, want version 1", view)
	}

	reader.set(firstCharacterID, battle.DisclosureView{SchemaVersion: 1, StateVersion: 2})
	reader.set(secondCharacterID, battle.DisclosureView{SchemaVersion: 1, StateVersion: 2})
	hub.Publish(context.Background(), battleID)
	if view := <-first.Views; view.StateVersion != 2 {
		t.Fatalf("first published view = %+v, want version 2", view)
	}
	if view := <-second.Views; view.StateVersion != 2 {
		t.Fatalf("second published view = %+v, want version 2", view)
	}

	reader.set(firstCharacterID, battle.DisclosureView{SchemaVersion: 1, StateVersion: 3})
	reader.set(secondCharacterID, battle.DisclosureView{SchemaVersion: 1, StateVersion: 3})
	hub.Publish(context.Background(), battleID)
	if view := <-first.Views; view.StateVersion != 3 {
		t.Fatalf("first buffered view = %+v, want version 3", view)
	}
	reader.set(firstCharacterID, battle.DisclosureView{SchemaVersion: 1, StateVersion: 4})
	reader.set(secondCharacterID, battle.DisclosureView{SchemaVersion: 1, StateVersion: 4})
	hub.Publish(context.Background(), battleID)

	if view, open := <-second.Views; !open || view.StateVersion != 3 {
		t.Fatalf("slow second subscriber must retain its final queued view before close, view=%+v open=%t", view, open)
	}
	if _, open := <-second.Views; open {
		t.Fatal("slow second subscriber must be disconnected when its bounded queue is full")
	}
	if view := <-first.Views; view.StateVersion != 4 {
		t.Fatalf("first newest view = %+v, want version 4", view)
	}
}

type disclosureReaderStub struct {
	mu    sync.RWMutex
	views map[snowflake.ID]battle.DisclosureView
}

func (stub *disclosureReaderStub) GetParticipantDisclosure(
	_ context.Context,
	_ snowflake.ID,
	playerCharacterID snowflake.ID,
) (battle.DisclosureView, error) {
	stub.mu.RLock()
	defer stub.mu.RUnlock()
	return stub.views[playerCharacterID], nil
}

func (stub *disclosureReaderStub) set(playerCharacterID snowflake.ID, view battle.DisclosureView) {
	stub.mu.Lock()
	defer stub.mu.Unlock()
	stub.views[playerCharacterID] = view
}
