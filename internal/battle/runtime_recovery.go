package battle

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RuntimeRecoveryReconciler 在 Server 内领取到期尝试并恢复 Running Battle Runtime。
type RuntimeRecoveryReconciler struct {
	store    RuntimeRecoveryStore
	registry *RuntimeRegistry
	starter  *StartService
	holderID string
	now      func() time.Time
}

// NewRuntimeRecoveryReconciler 创建显式依赖持久恢复事实的 Server 协调器。
func NewRuntimeRecoveryReconciler(store RuntimeRecoveryStore, registry *RuntimeRegistry, starter *StartService, holderID string, now func() time.Time) *RuntimeRecoveryReconciler {
	if now == nil {
		now = time.Now
	}
	return &RuntimeRecoveryReconciler{store: store, registry: registry, starter: starter, holderID: holderID, now: now}
}

// RecoverDue 领取并处理一批到期恢复尝试，返回成功恢复的 Battle Identifier。
func (reconciler *RuntimeRecoveryReconciler) RecoverDue(ctx context.Context) ([]snowflake.ID, error) {
	if reconciler == nil || reconciler.store == nil || reconciler.registry == nil || reconciler.starter == nil || reconciler.holderID == "" {
		return nil, ErrInvalidRuntimeRegistry
	}
	now := reconciler.now().UTC()
	ids, err := reconciler.store.ListDueRecoveryAttempts(ctx, now, 100)
	if err != nil {
		return nil, err
	}
	recovered := make([]snowflake.ID, 0, len(ids))
	for _, id := range ids {
		attempt, claimErr := reconciler.store.ClaimRecoveryAttempt(ctx, id, reconciler.holderID, now)
		if claimErr != nil {
			continue
		}
		if recoverErr := reconciler.recover(ctx, attempt); recoverErr != nil {
			if attempt.AttemptNumber >= 5 {
				alreadyInterrupted, err := reconciler.recoveryAlreadyExhausted(ctx, attempt.BattleID)
				if err != nil {
					continue
				}
				if !alreadyInterrupted && reconciler.interruptRecoveryExhausted(ctx, attempt.BattleID) != nil {
					continue
				}
			}
			_ = reconciler.store.CompleteRecoveryAttempt(ctx, attempt.ID, reconciler.holderID, false, "runtime_recovery_failed", reconciler.now().UTC())
			continue
		}
		if err := reconciler.store.CompleteRecoveryAttempt(ctx, attempt.ID, reconciler.holderID, true, "", reconciler.now().UTC()); err != nil {
			return recovered, err
		}
		recovered = append(recovered, attempt.BattleID)
	}
	return recovered, nil
}

// recoveryAlreadyExhausted 判断先前领取者是否已提交最终中断、但尚未来得及完成恢复尝试。
func (reconciler *RuntimeRecoveryReconciler) recoveryAlreadyExhausted(ctx context.Context, battleID snowflake.ID) (bool, error) {
	value, err := reconciler.store.Get(ctx, battleID)
	if err != nil {
		return false, err
	}
	return value.Status == StatusInterrupted && value.TerminalReason == string(TerminalReasonRecoveryExhausted), nil
}

func (reconciler *RuntimeRecoveryReconciler) recover(ctx context.Context, attempt RuntimeRecoveryAttempt) error {
	battleValue, err := reconciler.store.Get(ctx, attempt.BattleID)
	if err != nil || battleValue.Status != StatusRunning {
		return ErrBattleNotRunning
	}
	if battleValue.StartedAt.IsZero() {
		_, err = reconciler.starter.Start(ctx, battleValue)
		return err
	}
	if _, exists := reconciler.registry.Get(attempt.BattleID); exists {
		return nil
	}
	if err := reconciler.registry.Reserve(attempt.BattleID); err != nil {
		if errors.Is(err, ErrRuntimeAlreadyRegistered) {
			return nil
		}
		return err
	}
	reserved := true
	defer func() {
		if reserved {
			reconciler.registry.ReleaseReservation(attempt.BattleID)
		}
	}()
	if err := reconciler.registry.AcquireRuntimeLease(ctx, attempt.BattleID); err != nil {
		return err
	}
	lease, ok := reconciler.registry.RuntimeLease(attempt.BattleID)
	if !ok {
		return ErrInvalidRuntimeRegistry
	}
	leaseAcquired := true
	defer func() {
		if leaseAcquired {
			reconciler.registry.ReleaseAcquiredRuntimeLease(ctx, attempt.BattleID)
		}
	}()
	snapshot, err := reconciler.store.LoadRuntimeSnapshot(ctx, attempt.BattleID)
	if err != nil {
		return fmt.Errorf("加载 Runtime 快照: %w", err)
	}
	runtime, err := newBattleRuntime(
		snapshot.Battle,
		snapshot.State,
		snapshot.Random,
		reconciler.starter.store.TurnCommitter(lease),
		reconciler.starter.store.TurnTimeoutCompleter(lease),
		reconciler.now,
	)
	if err != nil {
		return err
	}
	if snapshot.Battle.Format.TurnDuration > 0 {
		runtime.turnDeadlineAt = snapshot.LastCommittedAt.Add(snapshot.Battle.Format.TurnDuration).UTC()
	}
	if err := reconciler.registry.Activate(runtime); err != nil {
		return err
	}
	reserved = false
	leaseAcquired = false
	return nil
}

// interruptRecoveryExhausted 在独占 Lease 保护下把耗尽恢复机会的 Battle 持久化为 interrupted。
func (reconciler *RuntimeRecoveryReconciler) interruptRecoveryExhausted(ctx context.Context, battleID snowflake.ID) error {
	if err := reconciler.registry.Reserve(battleID); err != nil {
		return err
	}
	defer reconciler.registry.ReleaseReservation(battleID)
	if err := reconciler.registry.AcquireRuntimeLease(ctx, battleID); err != nil {
		return err
	}
	defer reconciler.registry.ReleaseAcquiredRuntimeLease(ctx, battleID)
	lease, ok := reconciler.registry.RuntimeLease(battleID)
	if !ok {
		return ErrInvalidRuntimeRegistry
	}
	_, err := reconciler.starter.store.InterruptRuntime(ctx, lease, TerminalReasonRecoveryExhausted, reconciler.now().UTC())
	return err
}
