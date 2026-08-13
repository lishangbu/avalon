package battle

import (
	"context"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RuntimeRegistryBattleStore 读取 Server 当前内存 Runtime 对应的权威 Battle 状态。
type RuntimeRegistryBattleStore interface {
	// Get 返回 Battle 的最新持久化状态。
	Get(context.Context, snowflake.ID) (Battle, error)
}

// RuntimeRegistryReconcilerStore 描述状态同步时需要的最小内存 Runtime 操作。
type RuntimeRegistryReconcilerStore interface {
	// IDs 返回当前已激活 Runtime 的 Battle Identifier 快照。
	IDs() []snowflake.ID
	// Remove 注销指定 Battle 的内存 Runtime。
	Remove(snowflake.ID) bool
}

// RuntimeRegistryReconciler 将独立 Worker 写入的终局状态同步到 Server 内存 Runtime Registry。
//
// Asynq Worker 与 Server 必须分进程运行，Worker 不能直接持有或删除 Server 内存中的 Runtime。本协调器由
// Server 受控监控循环调用，以数据库 Battle 为唯一权威源释放已终局 Runtime 的容量；读取失败时宁可保留
// Runtime，也绝不猜测性移除仍可能活跃的对局。
type RuntimeRegistryReconciler struct {
	// registry 保存本进程当前激活的 Battle Runtime。
	registry RuntimeRegistryReconcilerStore
	// sessions 提供每个 Runtime 的权威持久化生命周期状态。
	sessions RuntimeRegistryBattleStore
}

// NewRuntimeRegistryReconciler 使用显式 Registry 与 Battle 查询器创建状态协调器。
func NewRuntimeRegistryReconciler(
	registry RuntimeRegistryReconcilerStore,
	sessions RuntimeRegistryBattleStore,
) *RuntimeRegistryReconciler {
	return &RuntimeRegistryReconciler{registry: registry, sessions: sessions}
}

// PruneTerminal 移除所有已不处于 running 状态的本地 Runtime，并返回实际释放的数量。
func (reconciler *RuntimeRegistryReconciler) PruneTerminal(ctx context.Context) (int, error) {
	if reconciler == nil || reconciler.registry == nil || reconciler.sessions == nil {
		return 0, ErrInvalidRuntimeRegistry
	}
	removed := 0
	for _, battleID := range reconciler.registry.IDs() {
		if battleID == snowflake.ID(0) {
			return removed, ErrInvalidRuntimeRegistry
		}
		session, err := reconciler.sessions.Get(ctx, battleID)
		if err != nil {
			return removed, fmt.Errorf("读取 Battle %s 的权威状态: %w", battleID, err)
		}
		if session.Status == StatusRunning && !session.StartedAt.IsZero() {
			continue
		}
		if reconciler.registry.Remove(battleID) {
			removed++
		}
	}
	return removed, nil
}
