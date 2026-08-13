package systemapi_test

import (
	"context"
	"testing"

	systemv1 "github.com/lishangbu/avalon/api/gen/go/avalon/system/v1"
	"github.com/lishangbu/avalon/internal/systemapi"
)

// TestServiceGetVersion 验证版本契约只暴露构建系统允许公开的稳定字段。
func TestServiceGetVersion(t *testing.T) {
	t.Parallel()

	service := systemapi.NewService(systemapi.BuildInfo{
		Version: "1.2.3", Commit: "abc123", APIMajorVersion: "v1",
	})
	response, err := service.GetVersion(context.Background(), &systemv1.GetVersionRequest{})
	if err != nil {
		t.Fatalf("读取版本失败: %v", err)
	}
	if response.GetVersion() != "1.2.3" || response.GetApiMajorVersion() != "v1" {
		t.Fatalf("版本响应 = %+v，期望完整构建信息", response)
	}
}
