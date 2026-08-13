package schema

import (
	"encoding/json"
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// AdminAuditLog 定义 admin_audit_log 表的持久化结构。
type AdminAuditLog struct {
	ent.Schema
}

// Fields 返回 admin_audit_log 表全部字段及其数据库约束。
func (AdminAuditLog) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("admin_audit_log 表的 id 字段。"),
		field.Int64("sequence").Comment("admin_audit_log 表的 sequence 字段。"),
		field.Int64("actor_account_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("admin_audit_log 表的 actor_account_id 字段。"),
		field.String("actor_kind").MaxLen(32).Comment("admin_audit_log 表的 actor_kind 字段。"),
		field.String("actor_identifier").MaxLen(128).Optional().Nillable().Comment("admin_audit_log 表的 actor_identifier 字段。"),
		field.String("action_code").MaxLen(128).Comment("admin_audit_log 表的 action_code 字段。"),
		field.String("object_type").MaxLen(64).Comment("admin_audit_log 表的 object_type 字段。"),
		field.String("object_id").MaxLen(128).Optional().Nillable().Comment("admin_audit_log 表的 object_id 字段。"),
		field.String("request_id").MaxLen(64).Comment("admin_audit_log 表的 request_id 字段。"),
		field.String("reason").MaxLen(512).Optional().Nillable().Comment("admin_audit_log 表的 reason 字段。"),
		field.JSON("changes", json.RawMessage{}).Optional().Comment("admin_audit_log 表的 changes 字段。"),
		field.Time("created_at").Comment("admin_audit_log 表的 created_at 字段。"),
		field.Bytes("previous_hash").Comment("admin_audit_log 表的 previous_hash 字段。"),
		field.Bytes("entry_hash").Comment("admin_audit_log 表的 entry_hash 字段。"),
	}
}

// Annotations 固定 admin_audit_log 的表名、注释、复合主键和检查约束。
func (AdminAuditLog) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 admin_audit_log 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "admin_audit_log", Checks: map[string]string{
			"admin_audit_log_actor_kind_check":    "actor_kind::text = ANY (ARRAY['admin'::character varying::text, 'anonymous'::character varying::text, 'operator'::character varying::text, 'system'::character varying::text])",
			"admin_audit_log_entry_hash_check":    "octet_length(entry_hash) = 32",
			"admin_audit_log_previous_hash_check": "octet_length(previous_hash) = ANY (ARRAY[0, 32])",
		}},
	}
}
