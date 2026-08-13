package database

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"sync/atomic"
)

// ServerLeaseKey 是 Avalon server 单活租约使用的稳定 PostgreSQL advisory lock key。
const ServerLeaseKey int64 = 7022902007378177536

// ErrLeaseHeld 表示另一个进程已经持有指定租约。
var ErrLeaseHeld = errors.New("PostgreSQL 租约已被占用")

// ErrLeaseClosed 表示租约连接已经释放。
var ErrLeaseClosed = errors.New("PostgreSQL 租约已经关闭")

// Lease 通过独占连接持有一个 session-scoped PostgreSQL advisory lock。
type Lease struct {
	connection *sql.Conn
	key        int64
	closed     atomic.Bool
}

// AcquireLease 尝试在独占连接上取得租约，不等待其他持有者释放。
func (p *Pool) AcquireLease(ctx context.Context, key int64) (*Lease, error) {
	connection, err := p.database.RawDB().Conn(ctx)
	if err != nil {
		return nil, fmt.Errorf("取得 PostgreSQL 租约连接: %w", err)
	}
	var acquired bool
	if err := connection.QueryRowContext(ctx, "SELECT pg_try_advisory_lock($1)", key).Scan(&acquired); err != nil {
		_ = connection.Close()
		return nil, fmt.Errorf("取得 PostgreSQL advisory lock: %w", err)
	}
	if !acquired {
		_ = connection.Close()
		return nil, ErrLeaseHeld
	}
	return &Lease{connection: connection, key: key}, nil
}

// Ready 验证持有租约的独占 PostgreSQL 连接仍然可用。
func (l *Lease) Ready(ctx context.Context) error {
	if l.closed.Load() {
		return ErrLeaseClosed
	}
	var value int
	if err := l.connection.QueryRowContext(ctx, "SELECT 1").Scan(&value); err != nil {
		return fmt.Errorf("PostgreSQL 租约连接失效: %w", err)
	}
	return nil
}

// Close 释放 advisory lock 和持有它的独占连接。
func (l *Lease) Close(ctx context.Context) error {
	if !l.closed.CompareAndSwap(false, true) {
		return nil
	}
	defer l.connection.Close()
	var released bool
	if err := l.connection.QueryRowContext(ctx, "SELECT pg_advisory_unlock($1)", l.key).Scan(&released); err != nil {
		return fmt.Errorf("释放 PostgreSQL advisory lock: %w", err)
	}
	if !released {
		return errors.New("PostgreSQL advisory lock 未由当前连接持有")
	}
	return nil
}
