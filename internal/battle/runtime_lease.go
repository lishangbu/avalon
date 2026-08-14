package battle

import (
	"context"
	"time"

	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RuntimeLease 是 Server 成功取得的 Battle 承载权及其 fencing token。
type RuntimeLease struct {
	// BattleID 是被当前 Server 承载的 Battle。
	BattleID snowflake.ID
	// HolderID 是当前 Server 实例的稳定进程标识。
	HolderID string
	// FencingToken 是所有 Runtime 写入必须携带的单调递增令牌。
	FencingToken int64
	// ExpiresAt 是按 PostgreSQL 时间确定的当前租约期限。
	ExpiresAt time.Time
}

// RuntimeLeaseCoordinator 管理单场 Running Battle 的 PostgreSQL Lease。
type RuntimeLeaseCoordinator interface {
	AcquireRuntimeLease(context.Context, snowflake.ID, string) (RuntimeLease, error)
	RenewRuntimeLease(context.Context, RuntimeLease) (RuntimeLease, error)
	ReleaseRuntimeLease(context.Context, RuntimeLease) error
}

// RuntimeRecoveryAttempt 是 Server 已原子领取的一次 Battle 恢复工作。
type RuntimeRecoveryAttempt struct {
	ID            snowflake.ID
	BattleID      snowflake.ID
	AttemptNumber int32
}

// RuntimeSnapshot 是从同一 state_version 持久事实恢复 Runtime 所需的完整值。
type RuntimeSnapshot struct {
	Battle          Battle
	State           battleengine.State
	Random          battleengine.RandomSource
	LastCommittedAt time.Time
}

// RuntimeRecoveryQuery 返回到期恢复尝试 Identifier 投影。
type RuntimeRecoveryQuery interface {
	ListDueRecoveryAttempts(context.Context, time.Time, int) ([]snowflake.ID, error)
}

// RuntimeRecoveryReader 返回 Battle 与对应 Runtime 持久快照。
type RuntimeRecoveryReader interface {
	LoadRuntimeSnapshot(context.Context, snowflake.ID) (RuntimeSnapshot, error)
	Get(context.Context, snowflake.ID) (Battle, error)
}

// RuntimeRecoveryRepository 定义恢复尝试状态的原子写入能力。
type RuntimeRecoveryRepository interface {
	ClaimRecoveryAttempt(context.Context, snowflake.ID, string, time.Time) (RuntimeRecoveryAttempt, error)
	CompleteRecoveryAttempt(context.Context, snowflake.ID, string, bool, string, time.Time) error
}
