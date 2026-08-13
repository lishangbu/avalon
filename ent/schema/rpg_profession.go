package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgProfession 定义 rpg_profession 表的持久化结构。
type RpgProfession struct {
	ent.Schema
}

// Fields 返回 rpg_profession 表全部字段及其数据库约束。
func (RpgProfession) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 职业记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("RPG 职业的全局唯一英文机器编码。"),
		field.String("name").MaxLen(120).Comment("RPG 职业的简体中文展示名称。"),
		field.String("description").Optional().Nillable().Comment("RPG 职业面向玩家或管理者的可选简体中文说明。"),
		field.Int32("maximum_level").Comment("RPG 职业允许生成或使用的最高等级。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 职业是否可被新的 RPG 进度引用。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("RPG 职业写入使用的正整数乐观并发版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 职业首次创建的 UTC 时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 职业最近一次业务更新的 UTC 时间。"),
	}
}

// Annotations 固定 rpg_profession 的表名、注释、复合主键和检查约束。
func (RpgProfession) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 可独立成长的一种 RPG 职业资料。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_profession", Checks: map[string]string{
			"rpg_profession_code_check":          "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_profession_description_check":   "description IS NULL OR char_length(description) >= 1 AND char_length(description) <= 4000 AND description = btrim(description)",
			"rpg_profession_maximum_level_check": "maximum_level > 0",
			"rpg_profession_name_check":          "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_profession_time_check":          "updated_at >= created_at",
			"rpg_profession_version_check":       "version > 0",
		}},
	}
}
