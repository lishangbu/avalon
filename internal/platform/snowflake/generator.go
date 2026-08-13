package snowflake

import (
	"context"
	"errors"
	"fmt"
	"sync"
	"sync/atomic"
	"time"
)

const (
	// Epoch 是 Avalon 雪花协议不可修改的 UTC 起点。
	Epoch                int64 = 1767225600000 // 2026-01-01T00:00:00Z
	timestampBits              = 43
	nodeBits                   = 8
	sequenceBits               = 12
	maximumTimestamp           = int64(1<<timestampBits) - 1
	maximumSequence            = uint16(1<<sequenceBits) - 1
	nodeShift                  = sequenceBits
	timestampShift             = nodeBits + sequenceBits
	maximumClockRollback       = 5 * time.Second
	maximumBatchSize           = 10000
)

var (
	// ErrLeaseInvalid 表示节点租约已失效，Generator 不再允许发号。
	ErrLeaseInvalid = errors.New("雪花节点租约无效")
	// ErrClockRollback 表示系统时间回拨超过协议允许的五秒等待窗口。
	ErrClockRollback = errors.New("系统时钟回拨超过雪花协议上限")
	// ErrTimestampOverflow 表示 43 位相对毫秒时间已经耗尽。
	ErrTimestampOverflow = errors.New("雪花 ID 时间戳已耗尽")
)

type clock interface {
	now() time.Time
	wait(context.Context, time.Duration) error
}

type systemClock struct{}

func (systemClock) now() time.Time { return time.Now() }
func (systemClock) wait(ctx context.Context, duration time.Duration) error {
	timer := time.NewTimer(duration)
	defer timer.Stop()
	select {
	case <-ctx.Done():
		return ctx.Err()
	case <-timer.C:
		return nil
	}
}

// Generator 使用固定 43/8/12 位协议，在有效节点租约内生成全局唯一 ID。
type Generator struct {
	node       uint8
	clock      clock
	leaseValid func() bool
	closed     atomic.Bool
	mu         sync.Mutex
	lastMillis int64
	sequence   uint16
}

// NewGenerator 创建运行时发号器；node 必须由 PostgreSQL 租约分配且只能位于 1..254。
func NewGenerator(node uint8, leaseValid func() bool) (*Generator, error) {
	return newGenerator(node, leaseValid, systemClock{})
}

func newGenerator(node uint8, leaseValid func() bool, source clock) (*Generator, error) {
	if node == 0 || node == 255 || leaseValid == nil || source == nil {
		return nil, errors.New("雪花 Generator 参数无效")
	}
	return &Generator{node: node, leaseValid: leaseValid, clock: source, lastMillis: -1}, nil
}

// Next 返回一个正数雪花 ID；租约失效、严重时钟回拨或 Context 结束时不会降级发号。
func (generator *Generator) Next(ctx context.Context) (ID, error) {
	if generator == nil || generator.closed.Load() || !generator.leaseValid() {
		return 0, ErrLeaseInvalid
	}
	generator.mu.Lock()
	defer generator.mu.Unlock()
	for {
		if generator.closed.Load() || !generator.leaseValid() {
			return 0, ErrLeaseInvalid
		}
		now := generator.clock.now().UTC()
		millis := now.UnixMilli() - Epoch
		if millis < 0 {
			return 0, fmt.Errorf("%w: 当前时间早于 epoch", ErrClockRollback)
		}
		if millis > maximumTimestamp {
			return 0, ErrTimestampOverflow
		}
		if millis < generator.lastMillis {
			rollback := time.Duration(generator.lastMillis-millis) * time.Millisecond
			if rollback > maximumClockRollback {
				return 0, ErrClockRollback
			}
			if err := generator.clock.wait(ctx, rollback); err != nil {
				return 0, err
			}
			continue
		}
		if millis == generator.lastMillis {
			if generator.sequence == maximumSequence {
				if err := generator.clock.wait(ctx, time.Millisecond); err != nil {
					return 0, err
				}
				continue
			}
			generator.sequence++
		} else {
			generator.lastMillis = millis
			generator.sequence = 0
		}
		value := (millis << timestampShift) | (int64(generator.node) << nodeShift) | int64(generator.sequence)
		if value <= 0 {
			return 0, ErrInvalidID
		}
		return ID(value), nil
	}
}

// NextN 批量生成至多一万个 ID，并保持与逐次调用 Next 相同的租约和时钟语义。
func (generator *Generator) NextN(ctx context.Context, count int) ([]ID, error) {
	if count < 1 || count > maximumBatchSize {
		return nil, fmt.Errorf("雪花 ID 批量数量必须位于 1..%d", maximumBatchSize)
	}
	result := make([]ID, 0, count)
	for range count {
		id, err := generator.Next(ctx)
		if err != nil {
			return nil, err
		}
		result = append(result, id)
	}
	return result, nil
}

// Close 原子关闭 Generator；关闭后不能恢复发号。
func (generator *Generator) Close() {
	if generator != nil {
		generator.closed.Store(true)
	}
}
