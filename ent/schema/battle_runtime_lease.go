package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// BattleRuntimeLease 定义 Server 对单场 Running Battle 的短期独占承载租约。
type BattleRuntimeLease struct {
	ent.Schema
}

// Fields 返回 battle_runtime_lease 的全部字段与约束。
func (BattleRuntimeLease) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).StorageKey("battle_id").Comment("被承载的 Battle Identifier。"),
		field.String("holder_id").MaxLen(128).Comment("当前承载 Server 实例的稳定进程标识。"),
		field.Int64("fencing_token").Positive().Comment("每次重新领取严格递增的 fencing token。"),
		field.Time("lease_expires_at").Comment("按 PostgreSQL 时间计算的租约过期时间。"),
		field.Time("acquired_at").Comment("当前 holder 首次取得本轮租约的 UTC 时间。"),
		field.Time("renewed_at").Comment("当前租约最近续期的 UTC 时间。"),
	}
}

// Annotations 固定 battle_runtime_lease 的表名、注释和时间约束。
func (BattleRuntimeLease) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("Server 承载 Running Battle 的 PostgreSQL Lease 与 fencing token。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_runtime_lease", Checks: map[string]string{
			"battle_runtime_lease_time_check": "lease_expires_at > renewed_at AND renewed_at >= acquired_at",
		}},
	}
}
