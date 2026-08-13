package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// BattleRecoveryAttempt 定义 Worker 请求 Server 恢复 Battle 的不可变尝试记录。
type BattleRecoveryAttempt struct {
	ent.Schema
}

// Fields 返回 battle_recovery_attempt 的全部字段与约束。
func (BattleRecoveryAttempt) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("恢复尝试的稳定 Identifier。"),
		field.Int64("battle_id").GoType(snowflake.ID(0)).Positive().Comment("需要恢复的 Running Battle。"),
		field.Int32("attempt_number").Positive().Comment("该 Battle 从 1 开始连续递增的恢复尝试序号。"),
		field.String("state").MaxLen(16).Comment("pending、claimed、succeeded 或 failed。"),
		field.Time("available_at").Comment("Worker 允许 Server 领取本次尝试的 UTC 时间。"),
		field.String("claimed_by").MaxLen(128).Optional().Nillable().Comment("领取本次尝试的 Server holder 标识。"),
		field.Time("claimed_at").Optional().Nillable().Comment("Server 领取本次尝试的 UTC 时间。"),
		field.Time("completed_at").Optional().Nillable().Comment("恢复成功或失败的 UTC 时间。"),
		field.String("failure_reason").MaxLen(64).Optional().Nillable().Comment("失败时写入的稳定脱敏原因代码。"),
		field.Time("created_at").Comment("恢复尝试创建的 UTC 时间。"),
	}
}

// Indexes 返回 Battle 内恢复序号唯一约束和待领取扫描索引。
func (BattleRecoveryAttempt) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("battle_id", "attempt_number").Unique().StorageKey("uk_battle_recovery_attempt_battle_id_attempt_number"),
		index.Fields("state", "available_at").StorageKey("idx_battle_recovery_attempt_state_available_at"),
	}
}

// Annotations 固定 battle_recovery_attempt 的表名、注释和状态约束。
func (BattleRecoveryAttempt) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("Worker 扫描生成、Server 领取并完成的 Battle 恢复尝试审计事实。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_recovery_attempt", Checks: map[string]string{
			"battle_recovery_attempt_state_check": "state::text = ANY (ARRAY['pending'::text, 'claimed'::text, 'succeeded'::text, 'failed'::text])",
		}},
	}
}
