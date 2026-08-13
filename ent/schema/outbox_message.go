package schema

import (
	"encoding/json"

	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// OutboxMessage 定义所有 Asynq 任务进入 Valkey 前必须经过的 PostgreSQL 投递事实。
type OutboxMessage struct {
	ent.Schema
}

// Fields 返回 Outbox 主题、聚合身份、租约和有限重试字段。
func (OutboxMessage) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("Outbox 消息的稳定 Snowflake Identifier。"),
		field.String("topic").MaxLen(120).Comment("由代码注册的稳定投递主题。"),
		field.Int64("aggregate_id").GoType(snowflake.ID(0)).Positive().Comment("消息关联的后台任务或其它聚合 Identifier。"),
		field.JSON("payload", json.RawMessage{}).Comment("投递到 Asynq 的最小载荷；后台任务只包含任务 Identifier。"),
		field.String("state").MaxLen(16).Default("pending").Comment("pending、processing、delivered 或 dead 投递状态。"),
		field.Int32("attempt_count").Default(0).Comment("已经发生的投递失败次数。"),
		field.Time("available_at").Comment("当前消息下一次允许领取的 UTC 时间。"),
		field.Time("lease_expires_at").Optional().Nillable().Comment("processing 消息的三十秒领取租约截止时间。"),
		field.Time("delivered_at").Optional().Nillable().Comment("消息成功写入 Asynq 的 UTC 时间。"),
		field.String("last_error").MaxLen(1000).Optional().Nillable().Comment("最近一次投递失败的受控错误摘要。"),
		field.Time("created_at").Comment("消息与业务事务共同创建的 UTC 时间。"),
		field.Time("updated_at").Comment("消息投递状态最近一次更新的 UTC 时间。"),
	}
}

// Indexes 返回 Outbox 幂等、领取与清理所需索引。
func (OutboxMessage) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("topic", "aggregate_id").Unique().StorageKey("uk_outbox_message_topic_aggregate_id"),
		index.Fields("state", "available_at").StorageKey("idx_outbox_message_state_available_at"),
		index.Fields("state", "lease_expires_at").StorageKey("idx_outbox_message_state_lease_expires_at"),
		index.Fields("state", "delivered_at").StorageKey("idx_outbox_message_state_delivered_at"),
	}
}

// Annotations 固定 Outbox 表名、注释和有限重试约束。
func (OutboxMessage) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PostgreSQL 到 Asynq 的可靠投递日志；成功记录七天后清理。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "outbox_message", Checks: map[string]string{
			"outbox_message_attempt_count_check": "attempt_count >= 0 AND attempt_count <= 20",
			"outbox_message_state_check":         "state IN ('pending', 'processing', 'delivered', 'dead')",
		}},
	}
}
