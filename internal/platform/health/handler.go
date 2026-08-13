// Package health 提供部署探针使用的存活与就绪 HTTP 边界。
package health

import (
	"context"
	"net/http"
	"time"
)

// ReadinessChecker 判断服务是否可以接收业务流量。
type ReadinessChecker interface {
	Ready(ctx context.Context) error
}

// Handler 提供存活与就绪探针。
type Handler struct {
	readiness ReadinessChecker
}

const readinessTimeout = 2 * time.Second

// NewHandler 创建健康检查 Handler。
func NewHandler(readiness ReadinessChecker) *Handler {
	return &Handler{readiness: readiness}
}

// Liveness 仅确认进程 HTTP 循环仍可响应，不访问外部依赖。
func (h *Handler) Liveness(w http.ResponseWriter, _ *http.Request) {
	w.WriteHeader(http.StatusNoContent)
}

// Readiness 仅在全部关键依赖可用时允许部署平台转发业务流量。
func (h *Handler) Readiness(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), readinessTimeout)
	defer cancel()
	if h.readiness == nil || h.readiness.Ready(ctx) != nil {
		w.WriteHeader(http.StatusServiceUnavailable)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
