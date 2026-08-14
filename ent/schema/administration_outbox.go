package schema

import (
	"encoding/json"

	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// AdministrationOutbox 定义 administration_outbox 表的持久化结构。
type AdministrationOutbox struct {
	ent.Schema
}

// Fields 返回 administration_outbox 表全部字段及其数据库约束。
func (AdministrationOutbox) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("administration_outbox 表的 id 字段。"),
		field.String("topic").MaxLen(120).Comment("administration_outbox 表的 topic 字段。"),
		field.Int64("aggregate_id").GoType(snowflake.ID(0)).Positive().Comment("administration_outbox 表的 aggregate_id 字段。"),
		field.JSON("payload", json.RawMessage{}).Comment("administration_outbox 表的 payload 字段。"),
		field.Time("created_at").Comment("administration_outbox 表的 created_at 字段。"),
		field.Time("published_at").Optional().Nillable().Comment("administration_outbox 表的 published_at 字段。"),
	}
}

// Annotations 固定 administration_outbox 的表名、注释、复合主键和检查约束。
func (AdministrationOutbox) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 administration_outbox 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "administration_outbox", Checks: map[string]string{
			"administration_outbox_payload_check": "jsonb_typeof(payload) = 'object'::text",
			"administration_outbox_topic_check":   "topic::text = btrim(topic::text) AND topic::text <> ''::text",
		}},
	}
}
