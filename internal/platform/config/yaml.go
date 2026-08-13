package config

import (
	"errors"
	"fmt"

	"buf.build/go/protovalidate"
	kratosfile "github.com/go-kratos/kratos/v3/config/file"
	configv1 "github.com/lishangbu/avalon/api/gen/go/avalon/config/v1"
	"google.golang.org/protobuf/encoding/protojson"
	"google.golang.org/protobuf/proto"
	"sigs.k8s.io/yaml"
)

const (
	// DefaultServerPath 是 avalon-server 未指定 --config 时读取的开发配置路径。
	DefaultServerPath = "config/server/development.yaml"
	// DefaultAdminServerPath 是 avalon-admin-server 未指定 --config 时读取的开发配置路径。
	DefaultAdminServerPath = "config/admin-server/development.yaml"
	// DefaultWorkerPath 是 avalon-worker 未指定 --config 时读取的开发配置路径。
	DefaultWorkerPath = "config/worker/development.yaml"
)

// LoadServer 从单个严格 YAML 文件加载 avalon-server 的强类型配置。
//
// 文件读取由 Kratos Config 文件源负责；未知字段、重复字段、类型错误和 Protobuf
// 约束错误都会阻止进程启动。错误信息只包含文件与字段边界，不回显任何敏感配置值。
func LoadServer(path string) (*configv1.ServerAppConfig, error) {
	result := &configv1.ServerAppConfig{}
	if err := loadYAML(path, result); err != nil {
		return nil, err
	}
	issues := &ValidationError{}
	server := result.GetServer()
	validateListenerAddress(issues, "server.grpcAddress", server.GetGrpcAddress())
	validateListenerAddress(issues, "server.connectAddress", server.GetConnectAddress())
	validateDatabaseConfig(issues, result.GetDatabase())
	validateValkeyAddress(issues, result.GetValkey().GetAddress())
	security := result.GetSecurity()
	if security.GetIdleSessionSeconds() >= security.GetAbsoluteSessionSeconds() {
		issues.add("security.idleSessionSeconds", "必须小于绝对会话期限")
	}
	if len(issues.Problems) > 0 {
		return nil, *issues
	}
	return result, nil
}

// LoadAdminServer 从单个严格 YAML 文件加载 avalon-admin-server 的强类型配置。
//
// 管理服务使用独立配置类型和默认路径，避免部署时误把玩家监听地址或数据库权限注入
// 管理进程。字段约束与 Server 保持一致。
func LoadAdminServer(path string) (*configv1.AdminServerAppConfig, error) {
	result := &configv1.AdminServerAppConfig{}
	if err := loadYAML(path, result); err != nil {
		return nil, err
	}
	issues := &ValidationError{}
	server := result.GetServer()
	validateListenerAddress(issues, "server.grpcAddress", server.GetGrpcAddress())
	validateListenerAddress(issues, "server.connectAddress", server.GetConnectAddress())
	validateDatabaseConfig(issues, result.GetDatabase())
	validateValkeyAddress(issues, result.GetValkey().GetAddress())
	storage := result.GetObjectStorage()
	validateEndpoint(issues, "objectStorage.endpoint", storage.GetEndpoint())
	requireString(issues, "objectStorage.region", storage.GetRegion())
	validateBucket(issues, storage.GetBucket())
	security := result.GetSecurity()
	if security.GetIdleSessionSeconds() >= security.GetAbsoluteSessionSeconds() {
		issues.add("security.idleSessionSeconds", "必须小于绝对会话期限")
	}
	if len(issues.Problems) > 0 {
		return nil, *issues
	}
	return result, nil
}

// LoadWorker 从单个严格 YAML 文件加载 avalon-worker 的强类型配置。
//
// Worker 只持有数据库和日志配置，不加载浏览器监听、安全密钥或管理对象存储凭据，从配置边界保持
// 后台任务进程的最小权限与最小故障面。
func LoadWorker(path string) (*configv1.WorkerAppConfig, error) {
	result := &configv1.WorkerAppConfig{}
	if err := loadYAML(path, result); err != nil {
		return nil, err
	}
	issues := &ValidationError{}
	validateDatabaseConfig(issues, result.GetDatabase())
	validateValkeyAddress(issues, result.GetValkey().GetAddress())
	if len(issues.Problems) > 0 {
		return nil, *issues
	}
	return result, nil
}

// loadYAML 使用同一条严格边界把 Kratos 文件源转换成指定的 Protobuf 配置消息。
func loadYAML(path string, target proto.Message) error {
	values, err := kratosfile.NewSource(path).Load()
	if err != nil {
		return fmt.Errorf("无法读取配置文件 %q", path)
	}
	if len(values) != 1 || len(values[0].Value) == 0 {
		return fmt.Errorf("配置文件 %q 不是非空普通文件", path)
	}

	encodedJSON, err := yaml.YAMLToJSONStrict(values[0].Value)
	if err != nil {
		return fmt.Errorf("配置文件 %q 包含重复字段、非字符串键或无效 YAML", path)
	}
	if err := (protojson.UnmarshalOptions{DiscardUnknown: false}).Unmarshal(encodedJSON, target); err != nil {
		return fmt.Errorf("配置文件 %q 包含未知字段或无效字段类型", path)
	}
	if err := protovalidate.Validate(target); err != nil {
		var validationError *protovalidate.ValidationError
		if errors.As(err, &validationError) {
			return fmt.Errorf("配置文件 %q 未满足字段约束", path)
		}
		return fmt.Errorf("配置文件 %q 无法完成约束校验", path)
	}
	return nil
}
