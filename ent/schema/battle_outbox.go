package schema

import (
	"encoding/json"

	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// BattleOutbox 定义 battle_outbox 表的持久化结构。
type BattleOutbox struct {
	ent.Schema
}

// Fields 返回 battle_outbox 表全部字段及其数据库约束。
func (BattleOutbox) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("battle_outbox 表的 id 字段。"),
		field.Int64("battle_id").GoType(snowflake.ID(0)).Positive().Comment("battle_outbox 表的 battle_id 字段。"),
		field.String("topic").MaxLen(128).Comment("battle_outbox 表的 topic 字段。"),
		field.JSON("payload", json.RawMessage{}).Comment("battle_outbox 表的 payload 字段。"),
		field.Time("created_at").Comment("battle_outbox 表的 created_at 字段。"),
		field.Time("published_at").Optional().Nillable().Comment("battle_outbox 表的 published_at 字段。"),
		field.Int32("attempts").Annotations(entsql.DefaultExpr("0")).Comment("battle_outbox 表的 attempts 字段。"),
		field.String("last_error").MaxLen(512).Optional().Nillable().Comment("battle_outbox 表的 last_error 字段。"),
	}
}

// Annotations 固定 battle_outbox 的表名、注释、复合主键和检查约束。
func (BattleOutbox) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("Battle 事务提交后由 Asynq Worker 异步消费的可靠领域事件 Outbox。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_outbox", Checks: map[string]string{
			"battle_outbox_attempts_check": "attempts >= 0",
			"battle_outbox_payload_check":  "jsonb_typeof(payload) = 'object'::text",
			"battle_outbox_topic_check":    "char_length(topic::text) >= 1 AND char_length(topic::text) <= 128",
		}},
	}
}
