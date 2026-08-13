package schema

import (
	"encoding/json"

	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/edge"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// BackgroundJob 定义由 PostgreSQL 掌握权威状态的后台任务。
type BackgroundJob struct {
	ent.Schema
}

// Fields 返回后台任务的生命周期、受控参数和最终结果字段。
func (BackgroundJob) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("后台任务在 API、Outbox、PostgreSQL 与 Asynq 之间共享的 Snowflake Identifier。"),
		field.String("kind").MaxLen(120).Comment("由代码白名单注册的稳定任务类型。"),
		field.String("queue").MaxLen(32).Comment("critical、default 或 low 执行队列。"),
		field.String("state").MaxLen(32).Comment("后台任务的稳定业务生命周期状态。"),
		field.JSON("parameters", json.RawMessage{}).Comment("由任务类型解释且不向通用管理接口公开的强类型参数。"),
		field.String("result_summary").MaxLen(500).Optional().Nillable().Comment("可安全展示且不包含堆栈或敏感参数的执行摘要。"),
		field.JSON("result", json.RawMessage{}).Optional().Comment("由任务类型解释的强类型执行结果。"),
		field.Int32("attempt_count").Default(0).Comment("已经开始的执行尝试次数。"),
		field.Int32("max_attempts").Default(10).Comment("自动执行尝试的最大次数。"),
		field.Int64("schedule_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("生成周期任务的动态调度 Identifier；人工任务为空。"),
		field.Time("scheduled_for").Optional().Nillable().Comment("周期任务本次理论触发时间；人工任务为空。"),
		field.Time("next_attempt_at").Optional().Nillable().Comment("处于 scheduled 或 retry_wait 时下一次允许入队的时间。"),
		field.Time("cancellation_requested_at").Optional().Nillable().Comment("管理员首次请求取消任务的 UTC 时间。"),
		field.Time("completed_at").Optional().Nillable().Comment("任务首次进入 completed、cancelled 或 failed 终态的 UTC 时间。"),
		field.Int64("version").Default(1).Comment("任务状态条件更新使用的正整数乐观并发版本。"),
		field.Time("created_at").Comment("任务事实首次写入 PostgreSQL 的 UTC 时间。"),
		field.Time("updated_at").Comment("任务业务状态最近一次更新的 UTC 时间。"),
	}
}

// Edges 返回后台任务与可选动态调度的关联。
func (BackgroundJob) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("schedule", BackgroundSchedule.Type).
			Field("schedule_id").
			Unique().
			StorageKey(edge.Symbol("fk_background_job_schedule_id_id")),
		edge.To("attempts", BackgroundJobAttempt.Type),
	}
}

// Indexes 返回任务查询、领取和周期幂等所需索引。
func (BackgroundJob) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("state", "next_attempt_at").StorageKey("idx_background_job_state_next_attempt_at"),
		index.Fields("kind", "created_at").StorageKey("idx_background_job_kind_created_at"),
		index.Fields("schedule_id", "scheduled_for").Unique().
			StorageKey("uk_background_job_schedule_id_scheduled_for").
			Annotations(entsql.IndexWhere("schedule_id IS NOT NULL")),
	}
}

// Annotations 固定后台任务表名、注释和跨字段约束。
func (BackgroundJob) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PostgreSQL 权威后台任务；Valkey 仅保存可恢复的执行副本。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "background_job", Checks: map[string]string{
			"background_job_attempt_count_check": "attempt_count >= 0 AND max_attempts >= 1",
			"background_job_queue_check":         "queue IN ('critical', 'default', 'low')",
			"background_job_schedule_check":      "(schedule_id IS NULL) = (scheduled_for IS NULL)",
			"background_job_state_check":         "state IN ('pending', 'queued', 'scheduled', 'running', 'retry_wait', 'cancellation_requested', 'retry_requested', 'completed', 'cancelled', 'failed')",
			"background_job_version_check":       "version >= 1",
		}},
	}
}
