package database

import (
	"context"

	"github.com/lishangbu/avalon/internal/platform/persistence"
)

// SchemaChecker 验证数据库结构与当前 Ent Schema 完全一致。
type SchemaChecker struct {
	pool *Pool
}

// NewSchemaChecker 创建固定期望版本的 Schema 就绪检查器。
func NewSchemaChecker(pool *Pool, expected int64) *SchemaChecker {
	_ = expected
	return &SchemaChecker{pool: pool}
}

// Ready 以只读模式验证当前数据库结构，不在健康检查阶段执行结构变更。
func (c *SchemaChecker) Ready(ctx context.Context) error {
	return c.pool.database.ApplySchema(ctx, persistence.SchemaModeValidate)
}
