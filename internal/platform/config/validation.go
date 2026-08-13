package config

import (
	"net"
	"net/url"
	"strconv"
	"strings"

	configv1 "github.com/lishangbu/avalon/api/gen/go/avalon/config/v1"
)

// ValidationError 汇总一个配置文件中所有可独立发现的字段问题。
type ValidationError struct {
	// Problems 按稳定的配置遍历顺序保存脱敏后的字段问题。
	Problems []string
}

// validateDatabaseConfig 验证 PostgreSQL URL 和单一显式连接池的交叉字段约束。
func validateDatabaseConfig(issues *ValidationError, value *configv1.DatabaseConfig) {
	validateDatabaseURL(issues, value.GetUrl())
	pool := value.GetPool()
	if pool.GetMaxIdleConnections() > pool.GetMaxOpenConnections() {
		issues.add("database.pool.maxIdleConnections", "不得大于最大打开连接数")
	}
}

// validateValkeyAddress 验证 Worker 的 Valkey 地址使用明确 host:port。
func validateValkeyAddress(issues *ValidationError, value string) {
	validateListenerAddress(issues, "valkey.address", value)
}

// Error 返回可一次修复的配置路径列表，但绝不包含对应配置值。
func (e ValidationError) Error() string {
	return "配置校验失败: " + strings.Join(e.Problems, "; ")
}

// add 追加只包含字段路径和约束原因的脱敏问题。
func (e *ValidationError) add(path string, reason string) {
	e.Problems = append(e.Problems, path+": "+reason)
}

// requireString 验证业务语义要求的非空字符串。
func requireString(issues *ValidationError, path string, value string) {
	if strings.TrimSpace(value) == "" {
		issues.add(path, "必填")
	}
}

// validateListenerAddress 验证监听地址包含合法 host 与 TCP 端口。
func validateListenerAddress(issues *ValidationError, path string, value string) {
	_, port, err := net.SplitHostPort(value)
	if err != nil {
		issues.add(path, "必须是 host:port 格式的监听地址")
		return
	}
	parsedPort, err := strconv.Atoi(port)
	if err != nil || parsedPort < 1 || parsedPort > 65535 {
		issues.add(path, "端口必须介于 1 和 65535 之间")
	}
}

// validateDatabaseURL 验证 PostgreSQL URL 的协议和网络地址。
func validateDatabaseURL(issues *ValidationError, value string) {
	if strings.TrimSpace(value) == "" {
		issues.add("database.url", "必填")
		return
	}
	parsed, err := url.Parse(value)
	if err != nil || (parsed.Scheme != "postgres" && parsed.Scheme != "postgresql") || parsed.Host == "" {
		issues.add("database.url", "必须是有效的 PostgreSQL URL")
	}
}

// validateEndpoint 验证对象存储端点不夹带凭据、查询参数或对象路径。
func validateEndpoint(issues *ValidationError, path string, value string) {
	if strings.TrimSpace(value) == "" {
		issues.add(path, "必填")
		return
	}
	parsed, err := url.Parse(value)
	if err != nil || (parsed.Scheme != "http" && parsed.Scheme != "https") || parsed.Host == "" ||
		parsed.User != nil || (parsed.Path != "" && parsed.Path != "/") || parsed.RawQuery != "" ||
		parsed.ForceQuery || parsed.Fragment != "" {
		issues.add(path, "必须是没有凭据、查询或路径的 HTTP(S) 地址")
	}
}

// validateBucket 验证 S3 兼容桶名的基础字符和长度边界。
func validateBucket(issues *ValidationError, value string) {
	trimmed := strings.TrimSpace(value)
	if len(trimmed) < 3 || len(trimmed) > 63 {
		issues.add("objectStorage.bucket", "长度必须介于 3 和 63 个字符之间")
		return
	}
	for _, character := range trimmed {
		if (character < 'a' || character > 'z') && (character < '0' || character > '9') &&
			character != '.' && character != '-' {
			issues.add("objectStorage.bucket", "只能包含小写字母、数字、点和连字符")
			return
		}
	}
}
