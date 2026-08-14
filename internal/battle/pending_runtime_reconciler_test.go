package battle_test

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
)

// TestPendingRuntimeBattleReconcilerStartsOnlyCurrentPendingRuntimeBattles 验证 Server 协调器只会启动仍等待 Actor 的
// 权威 Session，扫描后已经变化的记录不会被错误处理。
func TestPendingRuntimeBattleReconcilerStartsOnlyCurrentPendingRuntimeBattles(t *testing.T) {
	t.Parallel()

	startingID := snowflake.NewTestID()
	staleID := snowflake.NewTestID()
	source := &pendingRuntimeBattleSourceStub{
		ids: []snowflake.ID{startingID, staleID},
		sessions: map[snowflake.ID]battle.Battle{
			startingID: {ID: startingID, Status: battle.StatusRunning},
			staleID:    {ID: staleID, Status: battle.StatusRunning, StartedAt: time.Now().UTC()},
		},
	}
	starter := &pendingRuntimeBattleStarterStub{}
	reconciler := battle.NewPendingRuntimeReconciler(source, starter)

	started, err := reconciler.StartPending(context.Background())
	if err != nil {
		t.Fatalf("StartPending() error = %v", err)
	}
	if len(started) != 1 || started[0] != startingID || len(starter.sessions) != 1 || starter.sessions[0].ID != startingID {
		t.Fatalf("StartPending() started=%+v sessions=%+v", started, starter.sessions)
	}
}

// TestPendingRuntimeBattleReconcilerTreatsDuplicateRuntimeAsConcurrentSuccess 验证同步 RPC 启动已经预留 Runtime 时，
// 受控监控循环不会将同一健康 Battle 当成启动失败。
func TestPendingRuntimeBattleReconcilerTreatsDuplicateActorAsConcurrentSuccess(t *testing.T) {
	t.Parallel()

	battleID := snowflake.NewTestID()
	source := &pendingRuntimeBattleSourceStub{
		ids: []snowflake.ID{battleID},
		sessions: map[snowflake.ID]battle.Battle{
			battleID: {ID: battleID, Status: battle.StatusRunning},
		},
	}
	starter := &pendingRuntimeBattleStarterStub{err: battle.ErrRuntimeAlreadyRegistered}
	started, err := battle.NewPendingRuntimeReconciler(source, starter).StartPending(context.Background())
	if err != nil {
		t.Fatalf("StartPending() error = %v", err)
	}
	if len(started) != 0 || len(starter.sessions) != 1 {
		t.Fatalf("StartPending() started=%+v sessions=%+v", started, starter.sessions)
	}
}

// pendingRuntimeBattleSourceStub 提供没有 PostgreSQL 的待承载 Battle 权威读取替身。
type pendingRuntimeBattleSourceStub struct {
	// ids 是本次扫描返回的 Starting Battle 候选 Identifier。
	ids []snowflake.ID
	// sessions 按 Identifier 保存读取完整 Session 时应返回的当前权威状态。
	sessions map[snowflake.ID]battle.Battle
}

// ListPendingRuntimeBattleIDs 返回复制后的候选 ID，避免调用方修改测试固定输入。
func (stub *pendingRuntimeBattleSourceStub) ListPendingRuntimeBattleIDs(context.Context) ([]snowflake.ID, error) {
	return append([]snowflake.ID(nil), stub.ids...), nil
}

// Get 返回指定 Session；不存在时模拟 Reader 的领域未找到错误。
func (stub *pendingRuntimeBattleSourceStub) Get(_ context.Context, battleID snowflake.ID) (battle.Battle, error) {
	session, found := stub.sessions[battleID]
	if !found {
		return battle.Battle{}, errors.New("测试 Battle 不存在")
	}
	return session, nil
}

// pendingRuntimeBattleStarterStub 捕获协调器交给启动边界的 Battle 并可注入启动结果。
type pendingRuntimeBattleStarterStub struct {
	// sessions 按调用顺序保存已经尝试启动的 Session。
	sessions []battle.Battle
	// err 是每次启动返回的可控错误。
	err error
}

// Start 记录启动请求，并在没有注入错误时返回一个最小 Active Session。
func (stub *pendingRuntimeBattleStarterStub) Start(_ context.Context, session battle.Battle) (battle.Battle, error) {
	stub.sessions = append(stub.sessions, session)
	if stub.err != nil {
		return battle.Battle{}, stub.err
	}
	return battle.Battle{ID: session.ID, Status: battle.StatusRunning, StartedAt: time.Now().UTC()}, nil
}
