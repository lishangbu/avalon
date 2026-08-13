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

// BackgroundSchedule 定义管理端可编辑的受限动态调度实例。
type BackgroundSchedule struct {
	ent.Schema
}

// Fields 返回调度类型、表达式、受控参数和下一次执行时间。
func (BackgroundSchedule) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("动态调度实例的稳定 Snowflake Identifier。"),
		field.String("code").MaxLen(120).Comment("调度实例的全局唯一稳定机器编码。"),
		field.String("name").MaxLen(120).Comment("调度实例的简体中文显示名称。"),
		field.String("task_kind").MaxLen(120).Comment("由代码白名单注册的任务类型。"),
		field.String("schedule_kind").MaxLen(16).Comment("cron 或 interval 调度表达方式。"),
		field.String("cron_expression").MaxLen(120).Optional().Nillable().Comment("Asia/Shanghai 时区的标准五段 Cron 表达式。"),
		field.Int32("interval_seconds").Optional().Nillable().Comment("固定间隔秒数，最短十秒。"),
		field.String("missed_run_policy").MaxLen(16).Default("coalesce").Comment("停机错过周期时采用 skip、coalesce 或 catch_up。"),
		field.JSON("parameters", json.RawMessage{}).Comment("由任务类型白名单解释的强类型受控参数。"),
		field.Bool("enabled").Comment("调度派发器是否继续生成未来任务。"),
		field.Time("next_run_at").Optional().Nillable().Comment("启用调度下一次理论触发的 UTC 时间。"),
		field.Time("last_scheduled_at").Optional().Nillable().Comment("最近一次成功生成任务的理论触发时间。"),
		field.Int64("version").Default(1).Comment("管理写入使用的正整数乐观并发版本。"),
		field.Time("created_at").Comment("调度实例首次创建的 UTC 时间。"),
		field.Time("updated_at").Comment("调度实例最近一次业务更新的 UTC 时间。"),
	}
}

// Edges 返回调度已经生成的后台任务。
func (BackgroundSchedule) Edges() []ent.Edge {
	return []ent.Edge{edge.From("jobs", BackgroundJob.Type).Ref("schedule")}
}

// Indexes 返回调度编码、任务类型和到期领取索引。
func (BackgroundSchedule) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("code").Unique().StorageKey("uk_background_schedule_code"),
		index.Fields("task_kind").StorageKey("idx_background_schedule_task_kind"),
		index.Fields("enabled", "next_run_at").StorageKey("idx_background_schedule_enabled_next_run_at"),
	}
}

// Annotations 固定动态调度表名、注释及表达式互斥约束。
func (BackgroundSchedule) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("由代码白名单限制能力、由管理端维护实例的 PostgreSQL 动态调度。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "background_schedule", Checks: map[string]string{
			"background_schedule_expression_check":        "(schedule_kind = 'cron' AND cron_expression IS NOT NULL AND interval_seconds IS NULL) OR (schedule_kind = 'interval' AND cron_expression IS NULL AND interval_seconds >= 10)",
			"background_schedule_missed_run_policy_check": "missed_run_policy IN ('skip', 'coalesce', 'catch_up')",
			"background_schedule_version_check":           "version >= 1",
		}},
	}
}
