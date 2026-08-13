package battle

import (
	"context"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// TestActorExpireTurnAwardsOnlyLockedSide 验证一方按时锁定而另一方超时未提交时，Actor 只把胜利授予
// 已锁定一方，并将当前回合的 Actor 容量从 Registry 释放。
func TestActorExpireTurnAwardsOnlyLockedSide(t *testing.T) {
	startedAt := time.Date(2026, time.July, 31, 12, 0, 0, 0, time.UTC)
	battleID := snowflake.MustParse("1048576185")
	completer := &turnTimeoutCompleterStub{}
	actor := timeoutActor(battleID, startedAt, completer)
	actor.pending[ParticipantSideOne] = pendingTurnSubmission{}
	registry := newRuntimeRegistry(1, nil)
	if err := registry.Register(actor); err != nil {
		t.Fatalf("Register() error = %v", err)

	}
	expired, err := registry.ExpireTurnDeadlines(context.Background(), startedAt.Add(30*time.Second))
	if err != nil {
		t.Fatalf("ExpireTurnDeadlines() error = %v", err)
	}
	if len(expired) != 1 || expired[0] != battleID {
		t.Fatalf("ExpireTurnDeadlines() = %v, want [%s]", expired, battleID)
	}
	if completer.result.WinnerSide != ParticipantSideOne || completer.result.Reason != TerminalReasonTurnTimeout {
		t.Fatalf("timeout result = %+v", completer.result)
	}
	if registry.Count() != 0 {
		t.Fatalf("Registry Count() = %d, want 0", registry.Count())
	}
}

// TestActorExpireTurnCreatesNoContestWhenNobodyLocks 验证双方均未提交时回合超时明确产生 No Contest，
// 不把网络中断或未开始回合误判为任一方失败。
func TestActorExpireTurnCreatesNoContestWhenNobodyLocks(t *testing.T) {
	startedAt := time.Date(2026, time.July, 31, 12, 0, 0, 0, time.UTC)
	completer := &turnTimeoutCompleterStub{}
	actor := timeoutActor(snowflake.NewTestID(), startedAt, completer)

	completed, err := actor.ExpireTurn(context.Background(), startedAt.Add(30*time.Second))
	if err != nil {
		t.Fatalf("ExpireTurn() error = %v", err)
	}
	if !completed || completer.result.WinnerSide != 0 || completer.result.Reason != TerminalReasonNoContest {
		t.Fatalf("ExpireTurn() completed=%t result=%+v", completed, completer.result)
	}
}

// timeoutActor 构造仅用于回合超时裁定测试的最小 active Actor，不依赖随机源和回合解析器。
func timeoutActor(battleID snowflake.ID, startedAt time.Time, completer TurnTimeoutCompleter) *Runtime {
	return &Runtime{
		session: Battle{
			ID: battleID, Status: StatusRunning, StartedAt: startedAt,
			BattleDeadlineAt: startedAt.Add(30 * time.Minute), Format: Format{TurnDuration: 30 * time.Second},
		},
		turnDeadlineAt:   startedAt.Add(30 * time.Second),
		pending:          make(map[ParticipantSide]pendingTurnSubmission),
		timeoutCompleter: completer,
	}
}

// turnTimeoutCompleterStub 捕获 Actor 传给持久化边界的超时终局结果。
type turnTimeoutCompleterStub struct {
	// result 是最近一次 Complete 收到的结构化终局结果。
	result Result
}

// Complete 记录回合超时结果，并返回完成状态模拟持久化事务成功。
func (stub *turnTimeoutCompleterStub) Complete(
	_ context.Context,
	_ snowflake.ID,
	result Result,
	_ time.Time,
) (Battle, error) {
	stub.result = result
	return Battle{Status: StatusCompleted}, nil
}
