package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// SnowflakeNodeLease 保存一个运行时发号节点的 owner、fencing 和数据库时间租约。
type SnowflakeNodeLease struct{ ent.Schema }

// Fields 返回节点租约的固定身份和续租状态。
func (SnowflakeNodeLease) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("节点租约行的稳定雪花 ID。"),
		field.Int16("node_id").Comment("雪花协议运行时节点号，范围为 1..254。"),
		field.String("owner_token").MaxLen(64).Comment("当前进程的随机租约持有者令牌。"),
		field.Int64("fencing_token").Comment("每次接管节点时单调递增的 fencing 令牌。"),
		field.Time("lease_expires_at").Comment("按 PostgreSQL 时间计算的租约截止时间。"),
		field.Time("last_renewed_at").Comment("最近一次成功续租的 PostgreSQL 时间。"),
		field.Time("created_at").Comment("租约行首次创建时间。"),
		field.Time("updated_at").Comment("租约行最近变更时间。"),
	}
}

// Indexes 保证节点号唯一并支持过期租约领取。
func (SnowflakeNodeLease) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("node_id").Unique().StorageKey("uk_snowflake_node_lease_node_id"),
		index.Fields("lease_expires_at", "node_id").StorageKey("idx_snowflake_node_lease_expires_at_node_id"),
	}
}

// Annotations 固定租约表约束和中文注释。
func (SnowflakeNodeLease) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PostgreSQL 中央分配的雪花运行时节点租约；失去租约必须停止发号。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "snowflake_node_lease", Checks: map[string]string{
			"snowflake_node_lease_id_check":            "id > 0",
			"snowflake_node_lease_node_id_check":       "node_id BETWEEN 1 AND 254",
			"snowflake_node_lease_fencing_token_check": "fencing_token > 0",
			"snowflake_node_lease_owner_token_check":   "owner_token::text ~ '^[0-9a-f]{64}$'::text",
			"snowflake_node_lease_expiry_check":        "lease_expires_at > last_renewed_at",
		}},
	}
}
