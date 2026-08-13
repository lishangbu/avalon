package snowflake

import (
	"context"
	"errors"
	"fmt"
	"sync"
	"time"
)

const leaseMonitorInterval = time.Second

type readinessChecker interface {
	Ready(context.Context) error
}

// LeaseMonitor 将 Snowflake Runtime 就绪状态接入 Kratos Server 生命周期。
//
// 租约失效时 Start 返回错误，Kratos 会停止同一应用中的全部 Transport 和 Worker；正常关闭时
// Stop 只结束监控，不会把进程排空误报为租约故障。
type LeaseMonitor struct {
	checker  readinessChecker
	interval time.Duration
	stopped  chan struct{}
	stopOnce sync.Once
}

// NewLeaseMonitor 创建使用固定低频本地检查的运行时租约守卫。
func NewLeaseMonitor(runtime *Runtime) (*LeaseMonitor, error) {
	return newLeaseMonitor(runtime, leaseMonitorInterval)
}

func newLeaseMonitor(checker readinessChecker, interval time.Duration) (*LeaseMonitor, error) {
	if checker == nil || interval <= 0 {
		return nil, errors.New("雪花节点租约监控参数无效")
	}
	return &LeaseMonitor{checker: checker, interval: interval, stopped: make(chan struct{})}, nil
}

// Start 持续检查 Runtime；一旦租约不再有效便终止应用生命周期。
func (monitor *LeaseMonitor) Start(ctx context.Context) error {
	if monitor == nil || monitor.checker == nil || monitor.interval <= 0 {
		return errors.New("雪花节点租约监控不可用")
	}
	if err := monitor.checker.Ready(ctx); err != nil {
		return fmt.Errorf("雪花节点租约失效: %w", err)
	}
	ticker := time.NewTicker(monitor.interval)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return context.Cause(ctx)
		case <-monitor.stopped:
			return nil
		case <-ticker.C:
			if err := monitor.checker.Ready(ctx); err != nil {
				return fmt.Errorf("雪花节点租约失效: %w", err)
			}
		}
	}
}

// Stop 结束健康状态下的租约监控，允许 Kratos 正常关闭应用。
func (monitor *LeaseMonitor) Stop(context.Context) error {
	if monitor != nil {
		monitor.stopOnce.Do(func() { close(monitor.stopped) })
	}
	return nil
}
