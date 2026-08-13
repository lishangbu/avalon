// Package logging 提供 Kratos V3 与 Go 结构化日志共享的进程级日志边界。
package logging

import (
	"fmt"
	"io"
	"log/slog"
	"strings"

	kratoslog "github.com/go-kratos/kratos/v3/log"
	configv1 "github.com/lishangbu/avalon/api/gen/go/avalon/config/v1"
)

const redactedRequestArguments = "<redacted>"

// NewSlog 根据经过 Protobuf 校验的日志配置创建进程级 slog Logger。
// output 由组合根显式传入，便于测试捕获输出，也避免日志基础设施直接依赖全局 os.Stdout。
func NewSlog(config *configv1.LogConfig, output io.Writer) (*slog.Logger, error) {
	if config == nil {
		return nil, fmt.Errorf("日志配置不能为空")
	}
	level, err := parseLevel(config.GetLevel())
	if err != nil {
		return nil, err
	}
	options := &slog.HandlerOptions{Level: level, ReplaceAttr: redactSensitiveAttribute}
	var handler slog.Handler
	switch config.GetFormat() {
	case configv1.LogFormat_LOG_FORMAT_JSON:
		handler = slog.NewJSONHandler(output, options)
	case configv1.LogFormat_LOG_FORMAT_CONSOLE:
		handler = slog.NewTextHandler(output, options)
	default:
		return nil, fmt.Errorf("不支持的日志格式 %s", config.GetFormat())
	}
	return kratoslog.NewLogger(handler), nil
}

// parseLevel 把配置中的稳定小写级别映射为 slog.Level。
func parseLevel(value string) (slog.Level, error) {
	switch strings.ToLower(strings.TrimSpace(value)) {
	case "debug":
		return slog.LevelDebug, nil
	case "info":
		return slog.LevelInfo, nil
	case "warn":
		return slog.LevelWarn, nil
	case "error":
		return slog.LevelError, nil
	default:
		return 0, fmt.Errorf("不支持的日志级别 %q", value)
	}
}

// redactSensitiveAttribute 在最终编码前移除可能携带凭据或请求正文的日志属性。
//
// Kratos V3 的请求日志会把完整请求写入 args，并在失败时同时记录 error 与 stack。
// Avalon 只保留 operation、code、reason、latency 和 Trace 标识等诊断字段，避免密码、
// refresh token、Team 分享码或对象存储凭据随错误链进入日志后端。
func redactSensitiveAttribute(_ []string, attribute slog.Attr) slog.Attr {
	switch attribute.Key {
	case "args":
		return slog.String(attribute.Key, redactedRequestArguments)
	case "error", "stack":
		return slog.String(attribute.Key, redactedRequestArguments)
	default:
		return attribute
	}
}
