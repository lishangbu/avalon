package schema

import (
	"entgo.io/ent/dialect/entsql"
	entschema "entgo.io/ent/schema"
)

// nonIncrementalSnowflakeIdentifier 关闭 Ent 对数值主键默认启用的数据库自增行为。
// 所有业务实体必须由租约保护的 snowflake.Source 显式提供主键。
func nonIncrementalSnowflakeIdentifier() entschema.Annotation {
	incremental := false
	return entsql.Annotation{Incremental: &incremental}
}
