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

// AdminIdempotencyRecord 定义 admin_idempotency_record 表的持久化结构。
type AdminIdempotencyRecord struct {
	ent.Schema
}

// Fields 返回 admin_idempotency_record 表全部字段及其数据库约束。
func (AdminIdempotencyRecord) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("actor_account_id").GoType(snowflake.ID(0)).Positive().Comment("admin_idempotency_record 表的 actor_account_id 字段。"),
		field.String("operation_id").MaxLen(64).Comment("admin_idempotency_record 表的 operation_id 字段。"),
		field.String("idempotency_key").MaxLen(128).Comment("admin_idempotency_record 表的 idempotency_key 字段。"),
		field.Bytes("request_digest").Comment("admin_idempotency_record 表的 request_digest 字段。"),
		field.JSON("response", json.RawMessage{}).Optional().Comment("admin_idempotency_record 表的 response 字段。"),
		field.Time("created_at").Comment("admin_idempotency_record 表的 created_at 字段。"),
	}
}

// Indexes 返回 admin_idempotency_record 原复合主键对应的稳定业务唯一约束。
func (AdminIdempotencyRecord) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("actor_account_id", "operation_id", "idempotency_key").Unique().StorageKey("uk_admin_idempotency_record_actor_account_id_operation_a51e7ed2"),
	}
}

// Annotations 固定 admin_idempotency_record 的表名、注释、复合主键和检查约束。
func (AdminIdempotencyRecord) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 admin_idempotency_record 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "admin_idempotency_record", Checks: map[string]string{
			"admin_idempotency_record_request_digest_check": "octet_length(request_digest) = 32",
		}},
	}
}
