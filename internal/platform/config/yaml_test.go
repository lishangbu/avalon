package config_test

import (
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/config"
)

const validServerYAML = `server:
  grpcAddress: ":9080"
  connectAddress: ":9081"
valkey:
  address: "localhost:6379"
  username: ""
  password: ""
  database: 0
database:
  url: "postgres://avalon:secret@localhost:5432/avalon?sslmode=disable"
  schemaMode: DATABASE_SCHEMA_MODE_CREATE
  debugSql: false
  pool:
    maxOpenConnections: 20
    maxIdleConnections: 10
    connectionMaxLifetimeSeconds: 1800
    connectionMaxIdleSeconds: 300
security:
  idleSessionSeconds: 86400
  absoluteSessionSeconds: 2592000
log:
  format: LOG_FORMAT_JSON
  level: info
lifecycle:
  shutdownTimeoutSeconds: 30
`

// TestLoadServerYAML 验证 Server 只从严格 YAML 文件建立完整的强类型启动配置。
func TestLoadServerYAML(t *testing.T) {
	t.Parallel()

	path := filepath.Join(t.TempDir(), "server.yaml")
	if err := os.WriteFile(path, []byte(validServerYAML), 0o600); err != nil {
		t.Fatalf("写入测试配置失败: %v", err)
	}

	loaded, err := config.LoadServer(path)
	if err != nil {
		t.Fatalf("加载 Server YAML 配置失败: %v", err)
	}
	if loaded.GetServer().GetGrpcAddress() != ":9080" {
		t.Fatalf("gRPC 监听地址 = %q，期望 :9080", loaded.GetServer().GetGrpcAddress())
	}
	if loaded.GetLifecycle().GetShutdownTimeoutSeconds() != 30 {
		t.Fatalf("优雅关闭超时 = %d，期望 30 秒", loaded.GetLifecycle().GetShutdownTimeoutSeconds())
	}
}

// TestLoadServerYAMLRejectsInvalidListener 验证结构正确但不可监听的地址不会进入运行期。
func TestLoadServerYAMLRejectsInvalidListener(t *testing.T) {
	t.Parallel()

	path := filepath.Join(t.TempDir(), "server.yaml")
	document := strings.Replace(validServerYAML, `grpcAddress: ":9080"`, `grpcAddress: "invalid"`, 1)
	if err := os.WriteFile(path, []byte(document), 0o600); err != nil {
		t.Fatalf("写入测试配置失败: %v", err)
	}

	if _, err := config.LoadServer(path); err == nil {
		t.Fatal("无效监听地址应当阻止 Server 配置加载")
	}
}

// TestCommittedExampleConfigurationsLoad 验证四个可执行程序提交的示例配置始终是完整、严格且可加载的，
// 避免字段新增后示例仍然看似可复制、实际却会阻止应用启动。
func TestCommittedExampleConfigurationsLoad(t *testing.T) {
	t.Parallel()

	_, sourceFile, _, ok := runtime.Caller(0)
	if !ok {
		t.Fatal("无法定位配置测试源码")
	}
	repositoryRoot := filepath.Clean(filepath.Join(filepath.Dir(sourceFile), "..", "..", ".."))
	tests := []struct {
		// name 是失败时标识目标应用的稳定测试名称。
		name string
		// relativePath 是相对仓库根目录的已提交示例配置路径。
		relativePath string
		// load 通过对应应用的严格配置类型加载示例文件。
		load func(string) error
	}{
		{name: "server", relativePath: "config/server/development.example.yaml", load: func(path string) error {
			_, err := config.LoadServer(path)
			return err
		}},
		{name: "admin-server", relativePath: "config/admin-server/development.example.yaml", load: func(path string) error {
			_, err := config.LoadAdminServer(path)
			return err
		}},
		{name: "worker", relativePath: "config/worker/development.example.yaml", load: func(path string) error {
			_, err := config.LoadWorker(path)
			return err
		}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			path := filepath.Join(repositoryRoot, filepath.FromSlash(test.relativePath))
			if err := test.load(path); err != nil {
				t.Fatalf("加载示例配置 %q 失败：%v", test.relativePath, err)
			}
		})
	}
}
