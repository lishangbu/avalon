package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/edge"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// BackgroundJobAttempt 定义后台任务每次自动或人工执行尝试的追加历史。
type BackgroundJobAttempt struct {
	ent.Schema
}

// Fields 返回执行尝试的编号、触发来源、节点和受控错误字段。
func (BackgroundJobAttempt) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("执行尝试的稳定 Snowflake Identifier。"),
		field.Int64("job_id").GoType(snowflake.ID(0)).Positive().Comment("所属后台任务的稳定 Snowflake Identifier。"),
		field.Int32("attempt_number").Comment("同一任务内从一开始连续递增的执行尝试编号。"),
		field.String("trigger").MaxLen(16).Comment("automatic 或 manual 执行触发来源。"),
		field.String("state").MaxLen(16).Comment("running、completed、cancelled 或 failed 尝试状态。"),
		field.String("worker_id").MaxLen(128).Comment("执行本次尝试的 Worker 实例标识。"),
		field.String("error_summary").MaxLen(1000).Optional().Nillable().Comment("不包含堆栈和敏感载荷的受控失败摘要。"),
		field.Time("started_at").Comment("本次尝试开始执行的 UTC 时间。"),
		field.Time("finished_at").Optional().Nillable().Comment("本次尝试进入终态的 UTC 时间。"),
		field.Time("created_at").Comment("本次尝试记录首次写入 PostgreSQL 的 UTC 时间。"),
	}
}

// Edges 返回执行尝试所属后台任务。
func (BackgroundJobAttempt) Edges() []ent.Edge {
	return []ent.Edge{
		edge.From("job", BackgroundJob.Type).
			Ref("attempts").
			Field("job_id").
			Unique().
			Required(),
	}
}

// Indexes 返回同一任务尝试编号的稳定唯一约束。
func (BackgroundJobAttempt) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("job_id", "attempt_number").Unique().StorageKey("uk_background_job_attempt_job_id_attempt_number"),
	}
}

// Annotations 固定执行尝试表名、注释和状态约束。
func (BackgroundJobAttempt) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("后台任务每次执行尝试的不可覆盖历史。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "background_job_attempt", Checks: map[string]string{
			"background_job_attempt_number_check":  "attempt_number >= 1",
			"background_job_attempt_state_check":   "state IN ('running', 'completed', 'cancelled', 'failed')",
			"background_job_attempt_trigger_check": "trigger IN ('automatic', 'manual')",
		}},
	}
}
