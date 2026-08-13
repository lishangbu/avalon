package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// AdminLoginAttempt 定义 admin_login_attempt 表的持久化结构。
type AdminLoginAttempt struct {
	ent.Schema
}

// Fields 返回 admin_login_attempt 表全部字段及其数据库约束。
func (AdminLoginAttempt) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("admin_login_attempt 表的 id 字段。"),
		field.Int64("account_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("admin_login_attempt 表的 account_id 字段。"),
		field.Bytes("username_digest").Comment("admin_login_attempt 表的 username_digest 字段。"),
		field.Bool("succeeded").Comment("admin_login_attempt 表的 succeeded 字段。"),
		field.String("failure_reason").MaxLen(64).Optional().Nillable().Comment("admin_login_attempt 表的 failure_reason 字段。"),
		field.String("request_id").MaxLen(64).Comment("admin_login_attempt 表的 request_id 字段。"),
		field.Bytes("remote_address_digest").Optional().Nillable().Comment("admin_login_attempt 表的 remote_address_digest 字段。"),
		field.Time("occurred_at").Comment("admin_login_attempt 表的 occurred_at 字段。"),
	}
}

// Annotations 固定 admin_login_attempt 的表名、注释、复合主键和检查约束。
func (AdminLoginAttempt) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 admin_login_attempt 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "admin_login_attempt", Checks: map[string]string{
			"admin_login_attempt_check":                       "succeeded AND failure_reason IS NULL OR NOT succeeded AND failure_reason IS NOT NULL",
			"admin_login_attempt_remote_address_digest_check": "remote_address_digest IS NULL OR octet_length(remote_address_digest) = 32",
			"admin_login_attempt_username_digest_check":       "octet_length(username_digest) = 32",
		}},
	}
}
