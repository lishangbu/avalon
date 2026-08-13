package battle

import (
	"context"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// TestRuntimeRegistryBoundsCapacityAndIsolatesPanics 验证 Registry 不会为了腾出容量淘汰活跃对局，
// 并且单个 Actor 的 panic 只移除该 Battle、通知持久化中断协调器。
func TestRuntimeRegistryBoundsCapacityAndIsolatesPanics(t *testing.T) {
	t.Parallel()

	actor, replay, committer := newGoldenActor(t)
	var interrupted []RuntimePanic
	registry := newRuntimeRegistry(1, func(_ context.Context, failure RuntimePanic) {
		interrupted = append(interrupted, failure)
	})
	if err := registry.Register(actor); err != nil {
		t.Fatalf("Register() error = %v", err)
	}
	second, _, _ := newGoldenActor(t)
	second.session.ID = snowflake.NewTestID()
	if err := registry.Register(second); !errors.Is(err, ErrRuntimeCapacityExceeded) {
		t.Fatalf("Register(capacity) error = %v, want ErrActorCapacityExceeded", err)
	}
	if err := registry.Register(actor); !errors.Is(err, ErrRuntimeAlreadyRegistered) {
		t.Fatalf("Register(duplicate) error = %v, want ErrActorAlreadyRegistered", err)
	}

	committer.panicOnCommit = true
	_, err := registry.Submit(context.Background(), actor.Battle().ID, TurnSubmission{
		PlayerCharacterID: actor.Battle().Participants[0].PlayerCharacterID, ExpectedStateVersion: 0,
		IdempotencyKey: "panic-side-one", Actions: replay.Turns[0].Command.Actions[:1],
	})
	if err != nil {
		t.Fatalf("first registry Submit() error = %v", err)
	}
	_, err = registry.Submit(context.Background(), actor.Battle().ID, TurnSubmission{
		PlayerCharacterID: actor.Battle().Participants[1].PlayerCharacterID, ExpectedStateVersion: 0,
		IdempotencyKey: "panic-side-two", Actions: replay.Turns[0].Command.Actions[1:],
	})
	if !errors.Is(err, ErrRuntimePanicked) {
		t.Fatalf("second registry Submit() error = %v, want ErrActorPanicked", err)
	}
	if registry.Count() != 0 || len(interrupted) != 1 || interrupted[0].BattleID != actor.Battle().ID {
		t.Fatalf("registry after panic: count=%d interrupted=%+v", registry.Count(), interrupted)
	}
	_, err = registry.Submit(context.Background(), actor.Battle().ID, TurnSubmission{})
	if !errors.Is(err, ErrRuntimeNotFound) {
		t.Fatalf("Submit(removed) error = %v, want ErrActorNotFound", err)
	}
}
