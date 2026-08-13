package schema

import (
	"encoding/json"

	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// Account 定义玩家账号 account 表的持久化结构。
type Account struct {
	ent.Schema
}

// Fields 返回 account 表全部字段及其数据库约束。
func (Account) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("account 表的稳定 Identifier。"),
		field.String("username").MaxLen(64).Comment("玩家登录用户名。"),
		field.String("username_key").MaxLen(64).Comment("用于唯一比较的规范化用户名。"),
		field.String("password_hash").Comment("玩家密码的受控哈希结果。"),
		field.String("password_algorithm").MaxLen(32).Comment("密码哈希算法标识。"),
		field.JSON("password_parameters", json.RawMessage{}).Comment("密码哈希算法使用的参数。"),
		field.String("status").MaxLen(32).Comment("账号当前生命周期状态。"),
		field.Int64("security_version").Annotations(entsql.DefaultExpr("1")).Comment("撤销既有会话使用的安全版本。"),
		field.Time("locked_until").Optional().Nillable().Comment("账号临时锁定的 UTC 截止时间。"),
		field.Time("created_at").Comment("账号创建的 UTC 时间。"),
		field.Time("updated_at").Comment("账号最近修改的 UTC 时间。"),
		field.Int32("failed_login_attempts").Annotations(entsql.DefaultExpr("0")).Comment("连续登录失败次数。"),
		field.String("display_name").MaxLen(64).Comment("玩家账号的展示名称。"),
	}
}

// Annotations 固定 account 的表名、注释和检查约束。
func (Account) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("仅供玩家业务使用的账号身份；不得作为管理员身份使用。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "account", Checks: map[string]string{
			"account_security_version_check":      "security_version > 0",
			"account_failed_login_attempts_check": "failed_login_attempts >= 0",
			"account_status_check":                "status::text = ANY (ARRAY['active'::character varying::text, 'locked'::character varying::text, 'disabled'::character varying::text])",
		}},
	}
}
