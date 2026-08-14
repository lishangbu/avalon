package persistence

import (
	"context"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/team"
)

// TestRepositoryRejectsWritesWithoutCurrentGameDataValidation 验证导出的 PostgreSQL adapter 不能被直接构造的
// Record 绕过；只有 Team 应用服务在可用资料事务内生成的事实才允许进入写入路径。
func TestRepositoryRejectsWritesWithoutCurrentGameDataValidation(t *testing.T) {
	t.Parallel()

	adapters := NewAdapters(nil, snowflake.NewTestID)
	ctx := context.Background()
	if _, err := adapters.Create(ctx, team.CreateRecord{}); !errors.Is(err, team.ErrTeamCatalogUnavailable) {
		t.Fatalf("Create() error = %v, want ErrTeamCatalogUnavailable", err)
	}
	if _, err := adapters.Update(ctx, team.UpdateRecord{}); !errors.Is(err, team.ErrTeamCatalogUnavailable) {
		t.Fatalf("Update() error = %v, want ErrTeamCatalogUnavailable", err)
	}
	if _, err := adapters.ImportShare(ctx, team.ImportShareRecord{}); !errors.Is(err, team.ErrTeamCatalogUnavailable) {
		t.Fatalf("ImportShare() error = %v, want ErrTeamCatalogUnavailable", err)
	}
}
