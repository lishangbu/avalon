package persistence

import (
	"context"
	"errors"

	"entgo.io/ent"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

var errExplicitSnowflakeIdentifierRequired = errors.New("Ent 创建缺少显式 Snowflake Identifier")

type snowflakeIdentifierMutation interface {
	ID() (snowflake.ID, bool)
}

// requireExplicitSnowflakeIdentifiers 在 SQL 执行前阻止任何数值实体依赖数据库默认主键。
func requireExplicitSnowflakeIdentifiers() ent.Hook {
	return func(next ent.Mutator) ent.Mutator {
		return ent.MutateFunc(func(ctx context.Context, mutation ent.Mutation) (ent.Value, error) {
			if mutation.Op().Is(ent.OpCreate) {
				if snowflakeMutation, ok := mutation.(snowflakeIdentifierMutation); ok {
					id, exists := snowflakeMutation.ID()
					if !exists || !id.IsValid() {
						return nil, errExplicitSnowflakeIdentifierRequired
					}
				}
			}
			return next.Mutate(ctx, mutation)
		})
	}
}
