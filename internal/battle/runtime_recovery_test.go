package battle

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// TestRuntimeRecoveryKeepsHealthyRuntimeLease 验证恢复扫描命中本机健康 Runtime 时只完成尝试，
// 不会重复领取或释放它正在使用的 Lease。
func TestRuntimeRecoveryKeepsHealthyRuntimeLease(t *testing.T) {
	t.Parallel()
	runtime, _, committer := newGoldenActor(t)
	registry := newRuntimeRegistry(1, nil)
	if err := registry.Register(runtime); err != nil {
		t.Fatalf("Register() error = %v", err)
	}
	attempt := RuntimeRecoveryAttempt{ID: snowflake.NewTestID(), BattleID: runtime.Battle().ID, AttemptNumber: 1}
	store := &runtimeRecoveryRepositoryStub{battle: runtime.Battle(), attempt: attempt}
	leases := &runtimeLeaseCoordinatorStub{}
	registry.leaseCoordinator = leases
	reconciler := NewRuntimeRecoveryReconciler(store, registry, &StartService{repository: &runtimeStartRepositoryStub{committer: committer}}, "server-1", fixedRecoveryClock)

	recovered, err := reconciler.RecoverDue(context.Background())
	if err != nil {
		t.Fatalf("RecoverDue() error = %v", err)
	}
	if len(recovered) != 1 || recovered[0] != attempt.BattleID || !store.completedSucceeded {
		t.Fatalf("RecoverDue() = %v, completedSucceeded = %v", recovered, store.completedSucceeded)
	}
	if leases.acquired != 0 || leases.released != 0 {
		t.Fatalf("健康 Runtime Lease 调用 acquire=%d release=%d", leases.acquired, leases.released)
	}
}

// TestRuntimeRecoveryRestoresStartedBattle 验证已经写入 StartedAt 的 Running Battle 会从持久快照恢复，
// 并在持有新 Lease 的情况下激活唯一 Runtime。
func TestRuntimeRecoveryRestoresStartedBattle(t *testing.T) {
	t.Parallel()
	source, _, committer := newGoldenActor(t)
	attempt := RuntimeRecoveryAttempt{ID: snowflake.NewTestID(), BattleID: source.Battle().ID, AttemptNumber: 2}
	store := &runtimeRecoveryRepositoryStub{
		battle: source.Battle(), attempt: attempt,
		snapshot: RuntimeSnapshot{Battle: source.Battle(), State: source.state, Random: source.random, LastCommittedAt: source.Battle().StartedAt},
	}
	leases := &runtimeLeaseCoordinatorStub{}
	registry := NewRuntimeRegistryWithRuntimeLeases(1, nil, leases, "server-1")
	reconciler := NewRuntimeRecoveryReconciler(store, registry, &StartService{repository: &runtimeStartRepositoryStub{committer: committer}}, "server-1", fixedRecoveryClock)

	recovered, err := reconciler.RecoverDue(context.Background())
	if err != nil {
		t.Fatalf("RecoverDue() error = %v", err)
	}
	if len(recovered) != 1 || recovered[0] != attempt.BattleID || registry.Count() != 1 {
		t.Fatalf("RecoverDue() = %v, registry.Count() = %d", recovered, registry.Count())
	}
	if leases.acquired != 1 || leases.released != 0 || !store.completedSucceeded {
		t.Fatalf("恢复结果 acquire=%d release=%d completed=%v", leases.acquired, leases.released, store.completedSucceeded)
	}
}

// TestRuntimeRecoveryInterruptsAfterFifthFailure 验证第五次恢复仍无法读取快照时以
// recovery_exhausted 中断 Battle，并释放本次尚未激活的 Lease。
func TestRuntimeRecoveryInterruptsAfterFifthFailure(t *testing.T) {
	t.Parallel()
	source, _, committer := newGoldenActor(t)
	attempt := RuntimeRecoveryAttempt{ID: snowflake.NewTestID(), BattleID: source.Battle().ID, AttemptNumber: 5}
	store := &runtimeRecoveryRepositoryStub{battle: source.Battle(), attempt: attempt, snapshotErr: errors.New("测试快照损坏")}
	leases := &runtimeLeaseCoordinatorStub{}
	registry := NewRuntimeRegistryWithRuntimeLeases(1, nil, leases, "server-1")
	reconciler := NewRuntimeRecoveryReconciler(store, registry, &StartService{repository: &runtimeStartRepositoryStub{committer: committer, recoveryRepository: store}}, "server-1", fixedRecoveryClock)

	recovered, err := reconciler.RecoverDue(context.Background())
	if err != nil {
		t.Fatalf("RecoverDue() error = %v", err)
	}
	if len(recovered) != 0 || store.completedSucceeded || store.failureReason != "runtime_recovery_failed" {
		t.Fatalf("RecoverDue() = %v, completed=%v, failureReason=%q", recovered, store.completedSucceeded, store.failureReason)
	}
	if store.interruptReason != TerminalReasonRecoveryExhausted || leases.acquired != 2 || leases.released != 2 || registry.Count() != 0 {
		t.Fatalf("中断原因=%q acquire=%d release=%d registry.Count()=%d", store.interruptReason, leases.acquired, leases.released, registry.Count())
	}
}

// TestRuntimeRecoveryKeepsFifthAttemptClaimedWhenInterruptFails 验证最终中断暂时失败时不结束尝试，
// 使另一 Server 能在 claim 超时后继续完成同一恢复工作。
func TestRuntimeRecoveryKeepsFifthAttemptClaimedWhenInterruptFails(t *testing.T) {
	t.Parallel()
	source, _, committer := newGoldenActor(t)
	attempt := RuntimeRecoveryAttempt{ID: snowflake.NewTestID(), BattleID: source.Battle().ID, AttemptNumber: 5}
	store := &runtimeRecoveryRepositoryStub{battle: source.Battle(), attempt: attempt, snapshotErr: errors.New("测试快照损坏")}
	leases := &runtimeLeaseCoordinatorStub{}
	registry := NewRuntimeRegistryWithRuntimeLeases(1, nil, leases, "server-1")
	startRepository := &runtimeStartRepositoryStub{committer: committer, recoveryRepository: store, interruptErr: errors.New("测试中断失败")}
	reconciler := NewRuntimeRecoveryReconciler(store, registry, &StartService{repository: startRepository}, "server-1", fixedRecoveryClock)

	if recovered, err := reconciler.RecoverDue(context.Background()); err != nil || len(recovered) != 0 {
		t.Fatalf("RecoverDue() = %v, error = %v", recovered, err)
	}
	if store.completedCalled || store.interruptReason != TerminalReasonRecoveryExhausted {
		t.Fatalf("completed=%v interruptReason=%q", store.completedCalled, store.interruptReason)
	}
}

// TestRuntimeRecoveryCompletesReclaimedAttemptAfterPriorInterrupt 验证最终中断已提交但尝试完成写失败后，
// 新领取者只补齐尝试终态，不会再次中断已经终局的 Battle。
func TestRuntimeRecoveryCompletesReclaimedAttemptAfterPriorInterrupt(t *testing.T) {
	t.Parallel()
	source, _, committer := newGoldenActor(t)
	interrupted := source.Battle()
	interrupted.Status = StatusInterrupted
	interrupted.TerminalReason = string(TerminalReasonRecoveryExhausted)
	attempt := RuntimeRecoveryAttempt{ID: snowflake.NewTestID(), BattleID: interrupted.ID, AttemptNumber: 5}
	store := &runtimeRecoveryRepositoryStub{battle: interrupted, attempt: attempt}
	leases := &runtimeLeaseCoordinatorStub{}
	registry := NewRuntimeRegistryWithRuntimeLeases(1, nil, leases, "server-2")
	startRepository := &runtimeStartRepositoryStub{committer: committer, recoveryRepository: store}
	reconciler := NewRuntimeRecoveryReconciler(store, registry, &StartService{repository: startRepository}, "server-2", fixedRecoveryClock)

	if recovered, err := reconciler.RecoverDue(context.Background()); err != nil || len(recovered) != 0 {
		t.Fatalf("RecoverDue() = %v, error = %v", recovered, err)
	}
	if !store.completedCalled || store.interruptReason != "" || leases.acquired != 0 {
		t.Fatalf("completed=%v interruptReason=%q leaseAcquired=%d", store.completedCalled, store.interruptReason, leases.acquired)
	}
}

func fixedRecoveryClock() time.Time {
	return time.Date(2026, 8, 12, 12, 0, 0, 0, time.UTC)
}

// runtimeRecoveryRepositoryStub 记录恢复尝试完成结果，并提供测试指定的 Battle 与 Runtime 快照。
type runtimeRecoveryRepositoryStub struct {
	battle             Battle
	attempt            RuntimeRecoveryAttempt
	snapshot           RuntimeSnapshot
	snapshotErr        error
	completedSucceeded bool
	completedCalled    bool
	failureReason      string
	interruptReason    TerminalReason
}

func (store *runtimeRecoveryRepositoryStub) ListDueRecoveryAttempts(context.Context, time.Time, int) ([]snowflake.ID, error) {
	return []snowflake.ID{store.attempt.ID}, nil
}

func (store *runtimeRecoveryRepositoryStub) ClaimRecoveryAttempt(context.Context, snowflake.ID, string, time.Time) (RuntimeRecoveryAttempt, error) {
	return store.attempt, nil
}

func (store *runtimeRecoveryRepositoryStub) CompleteRecoveryAttempt(_ context.Context, _ snowflake.ID, _ string, succeeded bool, failureReason string, _ time.Time) error {
	store.completedCalled = true
	store.completedSucceeded = succeeded
	store.failureReason = failureReason
	return nil
}

func (store *runtimeRecoveryRepositoryStub) LoadRuntimeSnapshot(context.Context, snowflake.ID) (RuntimeSnapshot, error) {
	return store.snapshot, store.snapshotErr
}

func (store *runtimeRecoveryRepositoryStub) Get(context.Context, snowflake.ID) (Battle, error) {
	return store.battle, nil
}

// runtimeLeaseCoordinatorStub 统计恢复期间 Lease 的领取与释放，续期不属于这些测试的行为范围。
type runtimeLeaseCoordinatorStub struct {
	acquired int
	released int
}

func (coordinator *runtimeLeaseCoordinatorStub) AcquireRuntimeLease(_ context.Context, battleID snowflake.ID, holderID string) (RuntimeLease, error) {
	coordinator.acquired++
	return RuntimeLease{BattleID: battleID, HolderID: holderID, FencingToken: 1, ExpiresAt: fixedRecoveryClock().Add(30 * time.Second)}, nil
}

func (*runtimeLeaseCoordinatorStub) RenewRuntimeLease(_ context.Context, lease RuntimeLease) (RuntimeLease, error) {
	return lease, nil
}

func (coordinator *runtimeLeaseCoordinatorStub) ReleaseRuntimeLease(context.Context, RuntimeLease) error {
	coordinator.released++
	return nil
}

// runtimeStartRepositoryStub 为恢复出的 Runtime 提供回合提交器和超时完成边界。
type runtimeStartRepositoryStub struct {
	committer          TurnCommitter
	recoveryRepository *runtimeRecoveryRepositoryStub
	interruptErr       error
}

func (*runtimeStartRepositoryStub) Start(context.Context, RuntimeLease, battleengine.InitialState, battleengine.RandomSourceSnapshot, time.Time) (Battle, error) {
	return Battle{}, errors.New("测试不应重新启动已有 StartedAt 的 Battle")
}

func (repository *runtimeStartRepositoryStub) InterruptRuntime(_ context.Context, _ RuntimeLease, reason TerminalReason, _ time.Time) (Battle, error) {
	if repository.recoveryRepository != nil {
		repository.recoveryRepository.interruptReason = reason
	}
	return Battle{}, repository.interruptErr
}

func (repository *runtimeStartRepositoryStub) TurnCommitter(RuntimeLease) TurnCommitter {
	return repository.committer
}

func (store *runtimeStartRepositoryStub) TurnTimeoutCompleter(RuntimeLease) TurnTimeoutCompleter {
	return runtimeTimeoutCompleterStub{}
}

type runtimeTimeoutCompleterStub struct{}

func (runtimeTimeoutCompleterStub) Complete(context.Context, snowflake.ID, Result, time.Time) (Battle, error) {
	return Battle{}, nil
}
