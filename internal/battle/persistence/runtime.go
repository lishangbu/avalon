package persistence

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/lishangbu/avalon/ent/battlerecoveryattempt"
	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

const (
	// RuntimeLeaseDuration 是 Server 承载单场 Battle 的固定租约时长。
	RuntimeLeaseDuration = 30 * time.Second
	// RuntimeLeaseRenewInterval 是健康 Runtime 主动续期的固定周期。
	RuntimeLeaseRenewInterval = 10 * time.Second
	// MaximumRecoveryAttempts 是一场 Battle 进入 recovery_exhausted 前的最大恢复次数。
	MaximumRecoveryAttempts int32 = 5
	// RecoveryClaimTimeout 是 Server 崩溃后已领取恢复尝试允许被另一实例重新领取的期限。
	RecoveryClaimTimeout = time.Minute
)

var (
	// ErrRuntimeLeaseHeld 表示 Battle 的未过期租约由另一 Server 持有。
	ErrRuntimeLeaseHeld = errors.New("Battle Runtime Lease 已被占用")
	// ErrRuntimeLeaseLost 表示 holder 或 fencing token 已过期，当前 Runtime 必须立即停止写入。
	ErrRuntimeLeaseLost = errors.New("Battle Runtime Lease 已丢失")
	// ErrRecoveryExhausted 表示 Battle 已用完全部有界恢复机会。
	ErrRecoveryExhausted = errors.New("Battle Recovery 已耗尽")
)

// AcquireRuntimeLease 原子领取或续领一场 Battle；过期后的新 holder 会递增 fencing token。
func (store *Adapters) AcquireRuntimeLease(ctx context.Context, battleID snowflake.ID, holderID string) (battle.RuntimeLease, error) {
	holderID = strings.TrimSpace(holderID)
	if store == nil || store.pool == nil || battleID == snowflake.ID(0) || holderID == "" {
		return battle.RuntimeLease{}, ErrRuntimeLeaseHeld
	}
	var lease battle.RuntimeLease
	err := store.pool.QueryRow(ctx, `
		INSERT INTO battle_runtime_lease (battle_id, holder_id, fencing_token, lease_expires_at, acquired_at, renewed_at)
		VALUES ($1, $2, 1, CURRENT_TIMESTAMP + INTERVAL '30 seconds', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
		ON CONFLICT (battle_id) DO UPDATE SET
			holder_id = EXCLUDED.holder_id,
			fencing_token = CASE WHEN battle_runtime_lease.holder_id = EXCLUDED.holder_id THEN battle_runtime_lease.fencing_token ELSE battle_runtime_lease.fencing_token + 1 END,
			lease_expires_at = CURRENT_TIMESTAMP + INTERVAL '30 seconds',
			acquired_at = CASE WHEN battle_runtime_lease.holder_id = EXCLUDED.holder_id THEN battle_runtime_lease.acquired_at ELSE CURRENT_TIMESTAMP END,
			renewed_at = CURRENT_TIMESTAMP
		WHERE battle_runtime_lease.holder_id = EXCLUDED.holder_id OR battle_runtime_lease.lease_expires_at <= CURRENT_TIMESTAMP
		RETURNING battle_id, holder_id, fencing_token, lease_expires_at
	`, battleID, holderID).Scan(&lease.BattleID, &lease.HolderID, &lease.FencingToken, &lease.ExpiresAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return battle.RuntimeLease{}, ErrRuntimeLeaseHeld
	}
	if err != nil {
		return battle.RuntimeLease{}, fmt.Errorf("领取 Battle Runtime Lease: %w", err)
	}
	return lease, nil
}

// RenewRuntimeLease 仅在 holder 和 fencing token 都仍有效时延长租约。
func (store *Adapters) RenewRuntimeLease(ctx context.Context, lease battle.RuntimeLease) (battle.RuntimeLease, error) {
	if store == nil || store.pool == nil || lease.BattleID == snowflake.ID(0) || strings.TrimSpace(lease.HolderID) == "" || lease.FencingToken < 1 {
		return battle.RuntimeLease{}, ErrRuntimeLeaseLost
	}
	var renewed battle.RuntimeLease
	err := store.pool.QueryRow(ctx, `
		UPDATE battle_runtime_lease
		SET lease_expires_at = CURRENT_TIMESTAMP + INTERVAL '30 seconds', renewed_at = CURRENT_TIMESTAMP
		WHERE battle_id = $1 AND holder_id = $2 AND fencing_token = $3 AND lease_expires_at > CURRENT_TIMESTAMP
		RETURNING battle_id, holder_id, fencing_token, lease_expires_at
	`, lease.BattleID, lease.HolderID, lease.FencingToken).Scan(&renewed.BattleID, &renewed.HolderID, &renewed.FencingToken, &renewed.ExpiresAt)
	if errors.Is(err, pgx.ErrNoRows) {
		return battle.RuntimeLease{}, ErrRuntimeLeaseLost
	}
	if err != nil {
		return battle.RuntimeLease{}, fmt.Errorf("续期 Battle Runtime Lease: %w", err)
	}
	return renewed, nil
}

// ReleaseRuntimeLease 仅删除当前 holder 与 fencing token 仍匹配的租约。
func (store *Adapters) ReleaseRuntimeLease(ctx context.Context, lease battle.RuntimeLease) error {
	if store == nil || store.pool == nil || lease.BattleID == snowflake.ID(0) || strings.TrimSpace(lease.HolderID) == "" || lease.FencingToken < 1 {
		return ErrRuntimeLeaseLost
	}
	tag, err := store.pool.Exec(ctx, `DELETE FROM battle_runtime_lease WHERE battle_id = $1 AND holder_id = $2 AND fencing_token = $3`, lease.BattleID, lease.HolderID, lease.FencingToken)
	if err != nil {
		return fmt.Errorf("释放 Battle Runtime Lease: %w", err)
	}
	if tag.RowsAffected() != 1 {
		return ErrRuntimeLeaseLost
	}
	return nil
}

// validateRuntimeLease 在当前事务中锁定并校验 Runtime 写入携带的 holder 与 fencing token。
//
// PostgreSQL 时间是有效期的唯一权威；校验锁会与租约接管的 UPDATE 互斥，保证旧 Runtime 不能在新
// holder 取得更高 token 后提交状态、终局或中断。
func (store *Adapters) validateRuntimeLease(ctx context.Context, lease battle.RuntimeLease) error {
	if store == nil || store.pool == nil || lease.BattleID == snowflake.ID(0) || strings.TrimSpace(lease.HolderID) == "" || lease.FencingToken < 1 {
		return ErrRuntimeLeaseLost
	}
	var valid bool
	err := database.Executor(ctx, store.pool).QueryRow(ctx, `
		SELECT holder_id = $2 AND fencing_token = $3 AND lease_expires_at > CURRENT_TIMESTAMP
		FROM battle_runtime_lease
		WHERE battle_id = $1
		FOR UPDATE
	`, lease.BattleID, lease.HolderID, lease.FencingToken).Scan(&valid)
	if errors.Is(err, pgx.ErrNoRows) || err == nil && !valid {
		return ErrRuntimeLeaseLost
	}
	if err != nil {
		return fmt.Errorf("校验 Battle Runtime Lease: %w", err)
	}
	return nil
}

// RecoveryBackoff 返回指定恢复尝试序号对应的固定有界退避。
func RecoveryBackoff(attemptNumber int32) (time.Duration, error) {
	delays := [...]time.Duration{5 * time.Second, 15 * time.Second, 45 * time.Second, 2 * time.Minute, 5 * time.Minute}
	if attemptNumber < 1 || attemptNumber > int32(len(delays)) {
		return 0, ErrRecoveryExhausted
	}
	return delays[attemptNumber-1], nil
}

// ScheduleRecoveryAttempt 为 Running Battle 创建下一条不可变恢复尝试。
func (store *Adapters) ScheduleRecoveryAttempt(ctx context.Context, battleID snowflake.ID, observedAt time.Time) (int32, time.Time, error) {
	if store == nil || store.pool == nil || store.newID == nil || battleID == snowflake.ID(0) || observedAt.IsZero() {
		return 0, time.Time{}, ErrRecoveryExhausted
	}
	var attemptNumber int32
	err := store.pool.QueryRow(ctx, `SELECT COALESCE(MAX(attempt_number), 0) + 1 FROM battle_recovery_attempt WHERE battle_id = $1`, battleID).Scan(&attemptNumber)
	if err != nil {
		return 0, time.Time{}, fmt.Errorf("读取 Battle Recovery 序号: %w", err)
	}
	delay, err := RecoveryBackoff(attemptNumber)
	if err != nil {
		return 0, time.Time{}, err
	}
	id, err := store.newID.Next(ctx)
	if err != nil {
		return 0, time.Time{}, fmt.Errorf("生成 Battle Recovery Attempt Identifier: %w", err)
	}
	availableAt := observedAt.UTC().Add(delay)
	if _, err := store.pool.Client(ctx).BattleRecoveryAttempt.Create().SetID(id).SetBattleID(battleID).SetAttemptNumber(attemptNumber).SetState("pending").SetAvailableAt(availableAt).SetCreatedAt(observedAt.UTC()).Save(ctx); err != nil {
		return 0, time.Time{}, fmt.Errorf("创建 Battle Recovery Attempt: %w", err)
	}
	return attemptNumber, availableAt, nil
}

// ScheduleMissingRuntimeRecoveries 为没有有效 Lease 和未完成尝试的 Running Battle 排队恢复。
func (store *Adapters) ScheduleMissingRuntimeRecoveries(ctx context.Context, observedAt time.Time, maximum int) (int, error) {
	if store == nil || store.pool == nil || observedAt.IsZero() || maximum < 1 || maximum > 1000 {
		return 0, ErrRecoveryExhausted
	}
	rows, err := store.pool.Query(ctx, `
		SELECT b.id
		FROM battle b
		WHERE b.status = 'running'
		  AND NOT EXISTS (SELECT 1 FROM battle_runtime_lease l WHERE l.battle_id = b.id AND l.lease_expires_at > $1)
		  AND NOT EXISTS (SELECT 1 FROM battle_recovery_attempt a WHERE a.battle_id = b.id AND a.state IN ('pending', 'claimed'))
		  AND (SELECT COUNT(*) FROM battle_recovery_attempt a WHERE a.battle_id = b.id) < $2
		ORDER BY b.id
		LIMIT $3
	`, observedAt.UTC(), MaximumRecoveryAttempts, maximum)
	if err != nil {
		return 0, fmt.Errorf("扫描缺失 Battle Runtime: %w", err)
	}
	defer rows.Close()
	ids := make([]snowflake.ID, 0, maximum)
	for rows.Next() {
		var id snowflake.ID
		if err := rows.Scan(&id); err != nil {
			return 0, fmt.Errorf("读取缺失 Battle Runtime: %w", err)
		}
		ids = append(ids, id)
	}
	if err := rows.Err(); err != nil {
		return 0, fmt.Errorf("遍历缺失 Battle Runtime: %w", err)
	}
	scheduled := 0
	for _, id := range ids {
		if _, _, err := store.ScheduleRecoveryAttempt(ctx, id, observedAt); err != nil {
			if errors.Is(err, ErrRecoveryExhausted) {
				continue
			}
			return scheduled, err
		}
		scheduled++
	}
	return scheduled, nil
}

// ListDueRecoveryAttempts 返回到期的 pending 或领取超时的 claimed 恢复尝试 Identifier。
func (store *Adapters) ListDueRecoveryAttempts(ctx context.Context, observedAt time.Time, maximum int) ([]snowflake.ID, error) {
	if store == nil || store.pool == nil || observedAt.IsZero() || maximum < 1 || maximum > 1000 {
		return nil, ErrRecoveryExhausted
	}
	claimExpiredAt := observedAt.UTC().Add(-RecoveryClaimTimeout)
	rows, err := store.pool.Query(ctx, `
		SELECT id
		FROM battle_recovery_attempt
		WHERE (state = 'pending' AND available_at <= $1)
		   OR (state = 'claimed' AND claimed_at <= $2)
		ORDER BY available_at, id
		LIMIT $3
	`, observedAt.UTC(), claimExpiredAt, maximum)
	if err != nil {
		return nil, fmt.Errorf("查询到期 Battle Recovery Attempt: %w", err)
	}
	defer rows.Close()
	result := make([]snowflake.ID, 0, maximum)
	for rows.Next() {
		var id snowflake.ID
		if err := rows.Scan(&id); err != nil {
			return nil, fmt.Errorf("读取到期 Battle Recovery Attempt: %w", err)
		}
		result = append(result, id)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("遍历到期 Battle Recovery Attempt: %w", err)
	}
	return result, nil
}

// ClaimRecoveryAttempt 原子领取一条到期恢复尝试，并返回目标 Battle。
func (store *Adapters) ClaimRecoveryAttempt(ctx context.Context, attemptID snowflake.ID, holderID string, observedAt time.Time) (battle.RuntimeRecoveryAttempt, error) {
	holderID = strings.TrimSpace(holderID)
	if store == nil || store.pool == nil || attemptID == 0 || holderID == "" || observedAt.IsZero() {
		return battle.RuntimeRecoveryAttempt{}, ErrRuntimeLeaseHeld
	}
	var result battle.RuntimeRecoveryAttempt
	claimExpiredAt := observedAt.UTC().Add(-RecoveryClaimTimeout)
	err := store.pool.QueryRow(ctx, `
		UPDATE battle_recovery_attempt
		SET state = 'claimed', claimed_by = $2, claimed_at = $3
		WHERE id = $1
		  AND ((state = 'pending' AND available_at <= $3)
		       OR (state = 'claimed' AND claimed_at <= $4))
		RETURNING id, battle_id, attempt_number
	`, attemptID, holderID, observedAt.UTC(), claimExpiredAt).Scan(&result.ID, &result.BattleID, &result.AttemptNumber)
	if errors.Is(err, pgx.ErrNoRows) {
		return battle.RuntimeRecoveryAttempt{}, ErrRuntimeLeaseHeld
	}
	if err != nil {
		return battle.RuntimeRecoveryAttempt{}, fmt.Errorf("领取 Battle Recovery Attempt: %w", err)
	}
	return result, nil
}

// CompleteRecoveryAttempt 把已领取尝试推进为成功或失败终态。
func (store *Adapters) CompleteRecoveryAttempt(ctx context.Context, attemptID snowflake.ID, holderID string, succeeded bool, failureReason string, observedAt time.Time) error {
	holderID = strings.TrimSpace(holderID)
	if store == nil || store.pool == nil || attemptID == snowflake.ID(0) || holderID == "" || observedAt.IsZero() {
		return ErrRuntimeLeaseLost
	}
	state := "succeeded"
	failureReason = strings.TrimSpace(failureReason)
	if !succeeded {
		state = "failed"
		if failureReason == "" {
			failureReason = "runtime_recovery_failed"
		}
	}
	update := store.pool.Client(ctx).BattleRecoveryAttempt.Update().Where(
		battlerecoveryattempt.IDEQ(attemptID),
		battlerecoveryattempt.StateEQ("claimed"),
		battlerecoveryattempt.ClaimedByEQ(holderID),
	).SetState(state).SetCompletedAt(observedAt.UTC())
	if succeeded {
		update.ClearFailureReason()
	} else {
		update.SetFailureReason(failureReason)
	}
	rows, err := update.Save(ctx)
	if err != nil {
		return fmt.Errorf("完成 Battle Recovery Attempt: %w", err)
	}
	if rows != 1 {
		return ErrRuntimeLeaseLost
	}
	return nil
}
