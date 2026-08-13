package logging_test

import (
	"bytes"
	"strings"
	"testing"

	configv1 "github.com/lishangbu/avalon/api/gen/go/avalon/config/v1"
	platformlogging "github.com/lishangbu/avalon/internal/platform/logging"
)

// TestNewSlogHonorsFormatAndLevel 验证配置同时控制编码格式和最低日志级别。
func TestNewSlogHonorsFormatAndLevel(t *testing.T) {
	t.Parallel()

	var output bytes.Buffer
	logger, err := platformlogging.NewSlog(&configv1.LogConfig{
		Format: configv1.LogFormat_LOG_FORMAT_JSON,
		Level:  "warn",
	}, &output)
	if err != nil {
		t.Fatalf("NewSlog() error = %v", err)
	}
	logger.Info("不应输出")
	logger.Warn("应输出", "component", "test")
	contents := output.String()
	if strings.Contains(contents, "不应输出") {
		t.Fatalf("info 日志没有被 warn 级别过滤: %s", contents)
	}
	if !strings.Contains(contents, `"component":"test"`) || !strings.Contains(contents, "应输出") {
		t.Fatalf("JSON 日志缺少结构化内容: %s", contents)
	}
}

// TestKratosLoggerRedactsRequestArguments 验证 V3 slog Handler 不会把 RPC 凭据写入结构化日志。
func TestKratosLoggerRedactsRequestArguments(t *testing.T) {
	t.Parallel()

	var output bytes.Buffer
	target, err := platformlogging.NewSlog(&configv1.LogConfig{
		Format: configv1.LogFormat_LOG_FORMAT_JSON,
		Level:  "info",
	}, &output)
	if err != nil {
		t.Fatalf("NewSlog() error = %v", err)
	}
	target.Info(
		"server request",
		"operation", "/avalon.admin.v1.AdminSecurityService/Login",
		"args", `username:"admin" password:"secret-password" csrf_token:"secret-csrf"`,
		"error", "secret-password",
		"stack", "secret-csrf",
	)

	contents := output.String()
	if strings.Contains(contents, "secret-password") || strings.Contains(contents, "secret-csrf") || strings.Contains(contents, "123456") {
		t.Fatalf("敏感 RPC 参数泄漏到日志: %s", contents)
	}
	if !strings.Contains(contents, `"args":"<redacted>"`) ||
		!strings.Contains(contents, "AdminSecurityService/Login") {
		t.Fatalf("脱敏后应保留诊断字段: %s", contents)
	}
}
