package health

import (
	"context"
	"errors"
	"sync/atomic"
)

// ErrDraining 表示服务正在排空且不应继续接收业务流量。
var ErrDraining = errors.New("服务正在排空")

// Gate 组合关键依赖状态和进程 drain 状态。
type Gate struct {
	draining atomic.Bool
	checks   []ReadinessChecker
}

// NewGate 创建需要全部检查通过的就绪门禁。
func NewGate(checks ...ReadinessChecker) *Gate {
	return &Gate{checks: checks}
}

// BeginDrain 立即阻止新的业务流量进入实例。
func (g *Gate) BeginDrain() {
	g.draining.Store(true)
}

// Ready 依次确认 drain 状态和全部关键依赖。
func (g *Gate) Ready(ctx context.Context) error {
	if g.draining.Load() {
		return ErrDraining
	}
	for _, check := range g.checks {
		if err := check.Ready(ctx); err != nil {
			return err
		}
	}
	return nil
}
