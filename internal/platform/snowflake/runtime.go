package snowflake

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"errors"
	"fmt"
	"io"
	"sync"
	"sync/atomic"
	"time"
)

const (
	leaseDuration    = 30 * time.Second
	renewalInterval  = 10 * time.Second
	leaseSafety      = 2 * time.Second
	ownerTokenLength = 32
)

// LeaseGrant 是 PostgreSQL 适配器返回的节点、fencing 和数据库时钟事实。
type LeaseGrant struct {
	Node           uint8
	FencingToken   int64
	LeaseExpiresAt time.Time
	DatabaseTime   time.Time
}

// LeaseStore 是 Runtime 获取和续租雪花节点所需的最小持久化接口。
type LeaseStore interface {
	Acquire(context.Context, string, time.Duration) (LeaseGrant, error)
	Renew(context.Context, string, uint8, int64, time.Duration) (LeaseGrant, error)
}

// Runtime 把 PostgreSQL 节点租约、续租、readiness 和 Generator 收敛为单一发号模块。
type Runtime struct {
	store      LeaseStore
	ownerToken string
	node       uint8
	fencing    int64
	generator  *Generator
	valid      atomic.Bool
	deadline   atomic.Pointer[time.Time]
	cancel     context.CancelFunc
	done       chan struct{}
	closeOnce  sync.Once
}

// AcquireRuntime 获取唯一节点租约并启动续租；失败时不会创建可降级的 Generator。
func AcquireRuntime(ctx context.Context, store LeaseStore) (*Runtime, error) {
	return acquireRuntime(ctx, store, rand.Reader)
}

func acquireRuntime(ctx context.Context, store LeaseStore, random io.Reader) (*Runtime, error) {
	if store == nil || random == nil {
		return nil, errors.New("雪花 Runtime 缺少租约存储或随机源")
	}
	ownerBytes := make([]byte, ownerTokenLength)
	if _, err := io.ReadFull(random, ownerBytes); err != nil {
		return nil, fmt.Errorf("生成雪花租约 owner token: %w", err)
	}
	ownerToken := hex.EncodeToString(ownerBytes)
	acquisitionStartedAt := time.Now()
	grant, err := store.Acquire(ctx, ownerToken, leaseDuration)
	if err != nil {
		return nil, fmt.Errorf("获取雪花节点租约: %w", err)
	}
	if err := validateGrant(grant); err != nil {
		return nil, err
	}
	runtime := &Runtime{
		store:      store,
		ownerToken: ownerToken,
		node:       grant.Node,
		fencing:    grant.FencingToken,
		done:       make(chan struct{}),
	}
	runtime.valid.Store(true)
	runtime.setDeadline(grant, acquisitionStartedAt)
	generator, err := NewGenerator(grant.Node, runtime.leaseValid)
	if err != nil {
		return nil, err
	}
	runtime.generator = generator
	renewalCtx, cancel := context.WithCancel(context.Background())
	runtime.cancel = cancel
	go runtime.renew(renewalCtx)
	return runtime, nil
}

// Next 返回一个租约保护的全局雪花 ID。
func (runtime *Runtime) Next(ctx context.Context) (ID, error) {
	if runtime == nil || runtime.generator == nil {
		return 0, ErrLeaseInvalid
	}
	return runtime.generator.Next(ctx)
}

// NextN 返回一组租约保护的全局雪花 ID。
func (runtime *Runtime) NextN(ctx context.Context, count int) ([]ID, error) {
	if runtime == nil || runtime.generator == nil {
		return nil, ErrLeaseInvalid
	}
	return runtime.generator.NextN(ctx, count)
}

// Node 返回当前租约节点号，仅用于日志和低基数指标。
func (runtime *Runtime) Node() uint8 {
	if runtime == nil {
		return 0
	}
	return runtime.node
}

// Ready 确认租约未失效且尚未进入两秒安全窗口。
func (runtime *Runtime) Ready(context.Context) error {
	if !runtime.leaseValid() {
		return ErrLeaseInvalid
	}
	return nil
}

// Close 停止续租和发号，但不提前释放节点；节点只能在数据库租约自然过期后重用。
func (runtime *Runtime) Close() {
	if runtime == nil {
		return
	}
	runtime.closeOnce.Do(func() {
		runtime.valid.Store(false)
		if runtime.generator != nil {
			runtime.generator.Close()
		}
		if runtime.cancel != nil {
			runtime.cancel()
			<-runtime.done
		}
	})
}

func (runtime *Runtime) renew(ctx context.Context) {
	defer close(runtime.done)
	ticker := time.NewTicker(renewalInterval)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			renewalStartedAt := time.Now()
			grant, err := runtime.store.Renew(ctx, runtime.ownerToken, runtime.node, runtime.fencing, leaseDuration)
			if err != nil || validateGrant(grant) != nil || grant.Node != runtime.node || grant.FencingToken != runtime.fencing {
				runtime.valid.Store(false)
				if runtime.generator != nil {
					runtime.generator.Close()
				}
				return
			}
			runtime.setDeadline(grant, renewalStartedAt)
		}
	}
}

func (runtime *Runtime) leaseValid() bool {
	if runtime == nil || !runtime.valid.Load() {
		return false
	}
	deadline := runtime.deadline.Load()
	return deadline != nil && time.Now().Before(*deadline)
}

// setDeadline 从数据库调用开始时刻保守计算本地截止时间，保留单调时钟并把网络往返计入安全窗。
func (runtime *Runtime) setDeadline(grant LeaseGrant, requestStartedAt time.Time) {
	remaining := grant.LeaseExpiresAt.Sub(grant.DatabaseTime) - leaseSafety
	if remaining < 0 {
		remaining = 0
	}
	deadline := requestStartedAt.Add(remaining)
	runtime.deadline.Store(&deadline)
}

func validateGrant(grant LeaseGrant) error {
	if grant.Node == 0 || grant.Node == 255 || grant.FencingToken <= 0 || !grant.LeaseExpiresAt.After(grant.DatabaseTime) {
		return errors.New("PostgreSQL 返回无效雪花节点租约")
	}
	if grant.LeaseExpiresAt.Sub(grant.DatabaseTime) <= leaseSafety {
		return errors.New("PostgreSQL 返回的雪花节点租约已进入安全窗口")
	}
	clockSkew := time.Now().Sub(grant.DatabaseTime)
	if clockSkew > maximumClockRollback || clockSkew < -maximumClockRollback {
		return ErrClockRollback
	}
	return nil
}
