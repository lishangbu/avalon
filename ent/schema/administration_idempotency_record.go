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

// AdministrationIdempotencyRecord 定义 administration_idempotency_record 表的持久化结构。
type AdministrationIdempotencyRecord struct {
	ent.Schema
}

// Fields 返回 administration_idempotency_record 表全部字段及其数据库约束。
func (AdministrationIdempotencyRecord) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("actor_account_id").GoType(snowflake.ID(0)).Positive().Comment("administration_idempotency_record 表的 actor_account_id 字段。"),
		field.String("operation_id").MaxLen(64).Comment("administration_idempotency_record 表的 operation_id 字段。"),
		field.String("idempotency_key").MaxLen(128).Comment("administration_idempotency_record 表的 idempotency_key 字段。"),
		field.Bytes("request_digest").Comment("administration_idempotency_record 表的 request_digest 字段。"),
		field.JSON("response", json.RawMessage{}).Optional().Comment("administration_idempotency_record 表的 response 字段。"),
		field.Time("created_at").Comment("administration_idempotency_record 表的 created_at 字段。"),
	}
}

// Indexes 返回 administration_idempotency_record 原复合主键对应的稳定业务唯一约束。
func (AdministrationIdempotencyRecord) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("actor_account_id", "operation_id", "idempotency_key").Unique().StorageKey("uk_administration_idempotency_record_actor_account_id__52d7ef83"),
	}
}

// Annotations 固定 administration_idempotency_record 的表名、注释、复合主键和检查约束。
func (AdministrationIdempotencyRecord) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 administration_idempotency_record 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "administration_idempotency_record", Checks: map[string]string{
			"security_idempotency_record_request_digest_check": "octet_length(request_digest) = 32",
		}},
	}
}
