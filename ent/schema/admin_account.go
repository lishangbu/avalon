package schema

import (
	"encoding/json"
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// AdminAccount 定义 admin_account 表的持久化结构。
type AdminAccount struct {
	ent.Schema
}

// Fields 返回 admin_account 表全部字段及其数据库约束。
func (AdminAccount) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("admin_account 表的 id 字段。"),
		field.String("username").MaxLen(64).Comment("admin_account 表的 username 字段。"),
		field.String("username_key").MaxLen(64).Comment("admin_account 表的 username_key 字段。"),
		field.String("display_name").MaxLen(64).Comment("admin_account 表的 display_name 字段。"),
		field.String("password_hash").Comment("admin_account 表的 password_hash 字段。"),
		field.String("password_algorithm").MaxLen(32).Comment("admin_account 表的 password_algorithm 字段。"),
		field.JSON("password_parameters", json.RawMessage{}).Comment("admin_account 表的 password_parameters 字段。"),
		field.String("status").MaxLen(32).Comment("admin_account 表的 status 字段。"),
		field.Int32("failed_login_attempts").Annotations(entsql.DefaultExpr("0")).Comment("admin_account 表的 failed_login_attempts 字段。"),
		field.Time("locked_until").Optional().Nillable().Comment("admin_account 表的 locked_until 字段。"),
		field.Time("created_at").Comment("admin_account 表的 created_at 字段。"),
		field.Time("updated_at").Comment("admin_account 表的 updated_at 字段。"),
	}
}

// Annotations 固定 admin_account 的表名、注释、复合主键和检查约束。
func (AdminAccount) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("与玩家账号完全隔离、仅供 avalon-admin-server 使用的管理员身份。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "admin_account", Checks: map[string]string{
			"admin_account_failed_login_attempts_check": "failed_login_attempts >= 0",
			"admin_account_status_check":                "status::text = ANY (ARRAY['active'::character varying::text, 'locked'::character varying::text, 'disabled'::character varying::text])",
		}},
	}
}
