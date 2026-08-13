package battle_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestStartServiceInterruptsSessionWhenFactsReadFails 验证启动资料无法编译时不会遗留 starting Session。
func TestStartServiceInterruptsSessionWhenFactsReadFails(t *testing.T) {
	t.Parallel()
	session := battle.Battle{ID: snowflake.NewTestID(), Status: battle.StatusRunning}
	store := &startStoreStub{}
	leases := &startLeaseCoordinatorStub{}
	service := battle.NewStartService(
		store,
		battle.NewRuntimeRegistryWithRuntimeLeases(1, nil, leases, "test-server"),
		failedFactsReader{},
		func() (battleengine.RandomSource, error) { return battleengine.RandomSource{}, nil },
		nil,
		func() time.Time { return time.Date(2026, 7, 30, 12, 0, 0, 0, time.UTC) },
	)

	_, err := service.Start(context.Background(), session)
	if err == nil {
		t.Fatal("Start() error = nil，期望资料读取失败")
	}
	if !store.interrupted || store.interruptReason != battle.TerminalReasonStartupFailed {
		t.Fatalf("Interrupt() 调用 = %+v，期望以 startup_failed 中断", store)
	}
	if store.started {
		t.Fatal("资料读取失败时不应写入 active Battle")
	}
}

// TestStartServiceDuplicateReservationDoesNotTouchLease 验证本机已有同场启动工作时，
// 落后调用在领取数据库 Lease 前结束，不会释放首个 Runtime 的承载权。
func TestStartServiceDuplicateReservationDoesNotTouchLease(t *testing.T) {
	t.Parallel()
	battleID := snowflake.NewTestID()
	leases := &startLeaseCoordinatorStub{}
	registry := battle.NewRuntimeRegistryWithRuntimeLeases(1, nil, leases, "test-server")
	if err := registry.Reserve(battleID); err != nil {
		t.Fatalf("Reserve() error = %v", err)
	}
	service := battle.NewStartService(
		&startStoreStub{}, registry, failedFactsReader{},
		func() (battleengine.RandomSource, error) { return battleengine.RandomSource{}, nil }, nil, time.Now,
	)

	_, err := service.Start(context.Background(), battle.Battle{ID: battleID, Status: battle.StatusRunning})
	if !errors.Is(err, battle.ErrRuntimeAlreadyRegistered) {
		t.Fatalf("Start() error = %v, want ErrRuntimeAlreadyRegistered", err)
	}
	if leases.acquired != 0 || leases.released != 0 {
		t.Fatalf("重复启动 Lease 调用 acquire=%d release=%d", leases.acquired, leases.released)
	}
}

// failedFactsReader 让启动服务在持久化前稳定进入资料读取失败分支。
type failedFactsReader struct{}

func (failedFactsReader) ReadInitialStateFacts(context.Context, battle.Battle) (battle.InitialStateFacts, error) {
	return battle.InitialStateFacts{}, errors.New("测试资料读取失败")
}

// startStoreStub 记录 StartService 与 Battle 持久化边界之间的调用顺序。
type startStoreStub struct {
	started         bool
	interrupted     bool
	interruptReason battle.TerminalReason
}

func (store *startStoreStub) Start(context.Context, battle.RuntimeLease, battleengine.InitialState, battleengine.RandomSourceSnapshot, time.Time) (battle.Battle, error) {
	store.started = true
	return battle.Battle{}, nil
}

func (store *startStoreStub) InterruptRuntime(
	_ context.Context,
	_ battle.RuntimeLease,
	reason battle.TerminalReason,
	_ time.Time,
) (battle.Battle, error) {
	store.interrupted = true
	store.interruptReason = reason
	return battle.Battle{}, nil
}

func (store *startStoreStub) TurnCommitter(battle.RuntimeLease) battle.TurnCommitter { return nil }

func (store *startStoreStub) TurnTimeoutCompleter(battle.RuntimeLease) battle.TurnTimeoutCompleter {
	return startTimeoutCompleterStub{}
}

type startTimeoutCompleterStub struct{}

func (startTimeoutCompleterStub) Complete(context.Context, snowflake.ID, battle.Result, time.Time) (battle.Battle, error) {
	return battle.Battle{}, nil
}

type startLeaseCoordinatorStub struct {
	acquired int
	released int
}

func (coordinator *startLeaseCoordinatorStub) AcquireRuntimeLease(_ context.Context, battleID snowflake.ID, holderID string) (battle.RuntimeLease, error) {
	coordinator.acquired++
	return battle.RuntimeLease{BattleID: battleID, HolderID: holderID, FencingToken: 1, ExpiresAt: time.Now().Add(time.Minute)}, nil
}

func (*startLeaseCoordinatorStub) RenewRuntimeLease(_ context.Context, lease battle.RuntimeLease) (battle.RuntimeLease, error) {
	return lease, nil
}

func (coordinator *startLeaseCoordinatorStub) ReleaseRuntimeLease(context.Context, battle.RuntimeLease) error {
	coordinator.released++
	return nil
}
