package runtime_test

import (
	"io"
	"log/slog"
	"testing"
	"time"

	appruntime "github.com/lishangbu/avalon/internal/app/runtime"
)

// TestNewApplicationPreservesStableProcessIdentity 验证显式构造的 Layout V3 应用把实例、
// 服务名、版本和非敏感元数据完整交给 Kratos App。
func TestNewApplicationPreservesStableProcessIdentity(t *testing.T) {
	t.Parallel()

	application := appruntime.NewApplication(
		appruntime.ApplicationInfo{
			ID:          "worker-node-1",
			Name:        "avalon-worker",
			Version:     "1.2.3",
			Metadata:    map[string]string{"commit": "abc123"},
			StopTimeout: 30 * time.Second,
		},
		slog.New(slog.NewTextHandler(io.Discard, nil)),
	)
	if application.ID() != "worker-node-1" || application.Name() != "avalon-worker" || application.Version() != "1.2.3" {
		t.Fatalf("Kratos App 身份 = (%q, %q, %q)", application.ID(), application.Name(), application.Version())
	}
	if application.Metadata()["commit"] != "abc123" {
		t.Fatalf("Kratos App metadata = %#v", application.Metadata())
	}
}
