package battle_test

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
)

// TestLifecycleServiceExpiresEveryDueLifecycleKind 验证周期扫描会分别结算到期邀请、自动补齐 Preview 与活跃 Battle，
// 并且所有持久化动作使用同一个权威观测时间。
func TestLifecycleServiceExpiresEveryDueLifecycleKind(t *testing.T) {
	t.Parallel()

	observedAt := time.Date(2026, time.July, 31, 12, 0, 0, 0, time.UTC)
	store := &lifecycleStoreStub{
		expiredChallenges: []snowflake.ID{snowflake.NewTestID(), snowflake.NewTestID()},
		expiredPreviews:   []snowflake.ID{snowflake.NewTestID()},
		expiredBattlees:   []snowflake.ID{snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()},
	}
	service := battle.NewLifecycleService(store, func() time.Time { return observedAt })

	result, err := service.ExpireDue(context.Background())
	if err != nil {
		t.Fatalf("ExpireDue() error = %v", err)
	}
	if result != (battle.LifecycleRunResult{ExpiredChallenges: 2, AutoCompletedPreviews: 1, TimedOutBattlees: 3}) {
		t.Fatalf("ExpireDue() = %+v", result)
	}
	if !store.allCallsAt(observedAt) {
		t.Fatalf("生命周期调用时间 = %+v，期望均为 %s", store.calledAt, observedAt)
	}
}

type lifecycleStoreStub struct {
	expiredChallenges []snowflake.ID
	expiredPreviews   []snowflake.ID
	expiredBattlees   []snowflake.ID
	calledAt          []time.Time
}

func (store *lifecycleStoreStub) ListExpiredChallengeIDs(context.Context, time.Time) ([]snowflake.ID, error) {
	return append([]snowflake.ID(nil), store.expiredChallenges...), nil
}

func (store *lifecycleStoreStub) ExpireChallenge(_ context.Context, _ snowflake.ID, observedAt time.Time) (battle.Challenge, error) {
	store.calledAt = append(store.calledAt, observedAt)
	return battle.Challenge{}, nil
}

func (store *lifecycleStoreStub) ListExpiredPreviewBattleIDs(context.Context, time.Time) ([]snowflake.ID, error) {
	return append([]snowflake.ID(nil), store.expiredPreviews...), nil
}

func (store *lifecycleStoreStub) CompleteExpiredPreview(_ context.Context, _ snowflake.ID, observedAt time.Time) (battle.Battle, error) {
	store.calledAt = append(store.calledAt, observedAt)
	return battle.Battle{}, nil
}

func (store *lifecycleStoreStub) ListExpiredRunningBattleIDs(context.Context, time.Time) ([]snowflake.ID, error) {
	return append([]snowflake.ID(nil), store.expiredBattlees...), nil
}

func (store *lifecycleStoreStub) CompleteBattleTimeout(_ context.Context, _ snowflake.ID, observedAt time.Time) (battle.Battle, error) {
	store.calledAt = append(store.calledAt, observedAt)
	return battle.Battle{}, nil
}

func (store *lifecycleStoreStub) ScheduleMissingRuntimeRecoveries(_ context.Context, observedAt time.Time, maximum int) (int, error) {
	store.calledAt = append(store.calledAt, observedAt)
	if maximum != 100 {
		return 0, battle.ErrInvalidBattle
	}
	return 0, nil
}

func (store *lifecycleStoreStub) allCallsAt(expected time.Time) bool {
	return len(store.calledAt) == 7 && allTimesEqual(store.calledAt, expected)
}

func allTimesEqual(values []time.Time, expected time.Time) bool {
	for _, value := range values {
		if !value.Equal(expected) {
			return false
		}
	}
	return true
}
