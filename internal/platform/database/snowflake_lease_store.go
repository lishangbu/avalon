package database

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var errNoSnowflakeNodeAvailable = errors.New("没有可用雪花节点租约")

// SnowflakeLeaseStore 使用 PostgreSQL 行锁和数据库时钟分配运行时节点号。
type SnowflakeLeaseStore struct{ pool *Pool }

// NewSnowflakeLeaseStore 创建节点租约 PostgreSQL 适配器。
func NewSnowflakeLeaseStore(pool *Pool) *SnowflakeLeaseStore {
	return &SnowflakeLeaseStore{pool: pool}
}

// Acquire 领取最小的已过期节点，并单调增加 fencing token。
func (store *SnowflakeLeaseStore) Acquire(ctx context.Context, ownerToken string, duration time.Duration) (snowflake.LeaseGrant, error) {
	if store == nil || store.pool == nil || len(ownerToken) != 64 || duration <= 0 {
		return snowflake.LeaseGrant{}, errors.New("雪花节点领取参数无效")
	}
	var grant snowflake.LeaseGrant
	err := store.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		row := store.pool.QueryRow(txctx, `
WITH candidate AS (
    SELECT node_id
    FROM snowflake_node_lease
    WHERE lease_expires_at <= clock_timestamp()
    ORDER BY node_id
    FOR UPDATE SKIP LOCKED
    LIMIT 1
)
UPDATE snowflake_node_lease AS lease
SET owner_token = $1,
    fencing_token = lease.fencing_token + 1,
    last_renewed_at = clock_timestamp(),
    lease_expires_at = clock_timestamp() + $2 * INTERVAL '1 millisecond',
    updated_at = clock_timestamp()
FROM candidate
WHERE lease.node_id = candidate.node_id
RETURNING lease.node_id, lease.fencing_token, lease.lease_expires_at, clock_timestamp()`, ownerToken, duration.Milliseconds())
		var node int16
		if err := row.Scan(&node, &grant.FencingToken, &grant.LeaseExpiresAt, &grant.DatabaseTime); err != nil {
			if errors.Is(err, pgx.ErrNoRows) {
				return errNoSnowflakeNodeAvailable
			}
			return err
		}
		grant.Node = uint8(node)
		return nil
	})
	return grant, err
}

// Renew 只有 owner、节点和 fencing token 全部匹配时才延长原租约。
func (store *SnowflakeLeaseStore) Renew(ctx context.Context, ownerToken string, node uint8, fencingToken int64, duration time.Duration) (snowflake.LeaseGrant, error) {
	if store == nil || store.pool == nil || len(ownerToken) != 64 || node == 0 || node == 255 || fencingToken <= 0 || duration <= 0 {
		return snowflake.LeaseGrant{}, errors.New("雪花节点续租参数无效")
	}
	var grant snowflake.LeaseGrant
	row := store.pool.QueryRow(ctx, `
UPDATE snowflake_node_lease
SET last_renewed_at = clock_timestamp(),
    lease_expires_at = clock_timestamp() + $4 * INTERVAL '1 millisecond',
    updated_at = clock_timestamp()
WHERE node_id = $1
  AND owner_token = $2
  AND fencing_token = $3
  AND lease_expires_at > clock_timestamp()
RETURNING node_id, fencing_token, lease_expires_at, clock_timestamp()`, int16(node), ownerToken, fencingToken, duration.Milliseconds())
	var renewedNode int16
	if err := row.Scan(&renewedNode, &grant.FencingToken, &grant.LeaseExpiresAt, &grant.DatabaseTime); err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return snowflake.LeaseGrant{}, snowflake.ErrLeaseInvalid
		}
		return snowflake.LeaseGrant{}, fmt.Errorf("续租雪花节点: %w", err)
	}
	grant.Node = uint8(renewedNode)
	return grant, nil
}
