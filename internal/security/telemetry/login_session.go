// Package telemetry 提供认证应用事件的结构化日志和 Prometheus 指标适配器。
package telemetry

import (
	"context"
	"fmt"
	"log/slog"

	"github.com/lishangbu/avalon/internal/security/authentication"
	"github.com/prometheus/client_golang/prometheus"
)

var loginSessionFailures = prometheus.NewCounterVec(prometheus.CounterOpts{
	Namespace: "avalon",
	Subsystem: "authentication",
	Name:      "login_session_failures_total",
	Help:      "按安全域和建立阶段统计登录会话失败次数。",
}, []string{"domain", "stage"})

func init() {
	prometheus.MustRegister(loginSessionFailures)
}

// LoginSessionObserver 把登录会话阶段故障写入 Kratos 结构化日志和 Prometheus Counter。
type LoginSessionObserver struct {
	logger *slog.Logger
	domain string
}

// NewLoginSessionObserver 创建绑定管理员或玩家安全域的登录会话观测器。
func NewLoginSessionObserver(logger *slog.Logger, domain string) *LoginSessionObserver {
	if logger == nil {
		logger = slog.Default()
	}
	return &LoginSessionObserver{logger: logger, domain: domain}
}

// RecordFailure 记录不包含凭据、token 或底层错误正文的稳定故障事件。
func (o *LoginSessionObserver) RecordFailure(ctx context.Context, stage authentication.LoginSessionStage, requestID string, cause error) {
	loginSessionFailures.WithLabelValues(o.domain, string(stage)).Inc()
	o.logger.WarnContext(ctx, "登录会话建立失败",
		"event.name", "authentication.login_session.failure",
		"auth.domain", o.domain,
		"auth.stage", string(stage),
		"request.id", requestID,
		"error.type", fmt.Sprintf("%T", cause),
	)
}
