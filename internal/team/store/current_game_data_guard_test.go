package store

import (
	"context"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/team"
)

// TestStoreRejectsWritesWithoutCurrentGameDataValidation 验证导出的 PostgreSQL adapter 不能被直接构造的
// Record 绕过；只有 Team 应用服务在可用资料事务内生成的事实才允许进入写入路径。
func TestStoreRejectsWritesWithoutCurrentGameDataValidation(t *testing.T) {
	t.Parallel()

	repository := New(nil, snowflake.NewTestID)
	ctx := context.Background()
	if _, err := repository.Create(ctx, team.CreateRecord{}); !errors.Is(err, team.ErrTeamCatalogUnavailable) {
		t.Fatalf("Create() error = %v, want ErrTeamCatalogUnavailable", err)
	}
	if _, err := repository.Update(ctx, team.UpdateRecord{}); !errors.Is(err, team.ErrTeamCatalogUnavailable) {
		t.Fatalf("Update() error = %v, want ErrTeamCatalogUnavailable", err)
	}
	if _, err := repository.ImportShare(ctx, team.ImportShareRecord{}); !errors.Is(err, team.ErrTeamCatalogUnavailable) {
		t.Fatalf("ImportShare() error = %v, want ErrTeamCatalogUnavailable", err)
	}
}
