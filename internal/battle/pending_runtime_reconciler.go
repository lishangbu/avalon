package battle

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PendingRuntimeBattleSource 读取由 Worker 自动补齐 Preview 后等待本机启动的 Battle。
type PendingRuntimeBattleSource interface {
	// ListPendingRuntimeBattleIDs 返回仍在等待 Runtime 承载的稳定 Battle Identifier。
	ListPendingRuntimeBattleIDs(context.Context) ([]snowflake.ID, error)
	// Get 返回包含冻结 Participant、Preview 和赛制事实的完整 Battle 快照。
	Get(context.Context, snowflake.ID) (Battle, error)
}

// PendingRuntimeBattleStarter 将一份经过状态确认的 Battle 编译并启动为本机 Runtime。
type PendingRuntimeBattleStarter interface {
	// Start 原子持久化初始状态并在当前 Server 的 Runtime Registry 中激活 Battle。
	Start(context.Context, Battle) (Battle, error)
}

// PendingRuntimeBattleReconciler 连接 Worker 的持久化 Preview 补选与单实例 Server 的内存 Runtime 生命周期。
//
// Worker 进程只允许把到期 Preview 推进到等待承载状态，绝不持有 Server 的 Runtime。Server 通过本协调器在受控
// 监控循环中读取权威状态并启动 Runtime，从而在跨进程边界保持清晰的所有权和失败语义。
type PendingRuntimeReconciler struct {
	// source 读取可能由 Worker 或同步 RPC 路径写入的待承载 Battle。
	source PendingRuntimeBattleSource
	// starter 在当前单实例 Server 中编译初始状态并公开唯一 Runtime。
	starter PendingRuntimeBattleStarter
}

// NewPendingRuntimeReconciler 使用显式读取与启动边界创建待承载 Battle 协调器。
func NewPendingRuntimeReconciler(source PendingRuntimeBattleSource, starter PendingRuntimeBattleStarter) *PendingRuntimeReconciler {
	return &PendingRuntimeReconciler{source: source, starter: starter}
}

// StartPending 启动扫描时仍在等待 Runtime 承载的全部 Battle，并返回实际成功启动的稳定 Identifier。
//
// 同步 RPC 启动与本循环并发时，Runtime Registry 的预留会让落后的调用得到
// ErrRuntimeAlreadyRegistered；这表示另一个调用已经负责启动，不能把健康 Battle 中断为 startup_failed。
func (reconciler *PendingRuntimeReconciler) StartPending(ctx context.Context) ([]snowflake.ID, error) {
	if reconciler == nil || reconciler.source == nil || reconciler.starter == nil {
		return nil, ErrInvalidBattle
	}
	ids, err := reconciler.source.ListPendingRuntimeBattleIDs(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询待启动 Battle: %w", err)
	}
	started := make([]snowflake.ID, 0, len(ids))
	for _, battleID := range ids {
		if battleID == snowflake.ID(0) {
			return started, ErrInvalidBattle
		}
		session, getErr := reconciler.source.Get(ctx, battleID)
		if getErr != nil {
			return started, fmt.Errorf("读取待启动 Battle %s: %w", battleID, getErr)
		}
		if session.Status != StatusRunning || !session.StartedAt.IsZero() {
			continue
		}
		if _, startErr := reconciler.starter.Start(ctx, session); startErr != nil {
			if errors.Is(startErr, ErrRuntimeAlreadyRegistered) || errors.Is(startErr, ErrBattleNotPendingRuntime) {
				continue
			}
			return started, fmt.Errorf("启动 Battle %s: %w", battleID, startErr)
		}
		started = append(started, battleID)
	}
	return started, nil
}
