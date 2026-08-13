package battle

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// TestRuntimeRegistryReconcilerPrunesOnlyTerminalRuntimes 验证独立 Worker 结算后，Server 可以仅移除
// 已进入终态的内存 Runtime，而不会干扰仍在进行的对局。
func TestRuntimeRegistryReconcilerPrunesOnlyTerminalRuntimes(t *testing.T) {
	activeID := snowflake.MustParse("1048576178")
	completedID := snowflake.MustParse("1048576179")
	registry := newRuntimeRegistry(2, nil)
	if err := registry.Register(&Runtime{session: Battle{ID: activeID}}); err != nil {
		t.Fatalf("Register(active) error = %v", err)
	}
	if err := registry.Register(&Runtime{session: Battle{ID: completedID}}); err != nil {
		t.Fatalf("Register(completed) error = %v", err)
	}
	reconciler := NewRuntimeRegistryReconciler(registry, registryBattleStoreStub{sessions: map[snowflake.ID]Battle{
		activeID:    {ID: activeID, Status: StatusRunning, StartedAt: time.Now().UTC()},
		completedID: {ID: completedID, Status: StatusCompleted},
	}})

	removed, err := reconciler.PruneTerminal(context.Background())
	if err != nil {
		t.Fatalf("PruneTerminal() error = %v", err)
	}
	if removed != 1 {
		t.Fatalf("PruneTerminal() removed = %d, want 1", removed)
	}
	if _, found := registry.Get(activeID); !found {
		t.Fatal("active Actor was removed")
	}
	if _, found := registry.Get(completedID); found {
		t.Fatal("completed Actor remains registered")
	}
}

// TestRuntimeRegistryReconcilerStopsOnStoreError 验证状态读取失败时不会猜测性释放 Runtime 容量。
func TestRuntimeRegistryReconcilerStopsOnStoreError(t *testing.T) {
	battleID := snowflake.MustParse("1048576180")
	registry := newRuntimeRegistry(1, nil)
	if err := registry.Register(&Runtime{session: Battle{ID: battleID}}); err != nil {
		t.Fatalf("Register() error = %v", err)
	}
	reconciler := NewRuntimeRegistryReconciler(registry, registryBattleStoreStub{err: errors.New("数据库暂时不可用")})

	if _, err := reconciler.PruneTerminal(context.Background()); err == nil {
		t.Fatal("PruneTerminal() error = nil, want store error")
	}
	if _, found := registry.Get(battleID); !found {
		t.Fatal("Actor was removed after store error")
	}
}

// registryBattleStoreStub 是 Runtime Registry 状态同步测试使用的确定性 Battle 读取器。
type registryBattleStoreStub struct {
	// sessions 按 Battle Identifier 保存应返回的权威 Session。
	sessions map[snowflake.ID]Battle
	// err 是读取任意 Session 时返回的基础设施错误。
	err error
}

// Get 返回预设 Session 或预设错误。
func (stub registryBattleStoreStub) Get(_ context.Context, battleID snowflake.ID) (Battle, error) {
	if stub.err != nil {
		return Battle{}, stub.err
	}
	session, found := stub.sessions[battleID]
	if !found {
		return Battle{}, ErrInvalidBattle
	}
	return session, nil
}
