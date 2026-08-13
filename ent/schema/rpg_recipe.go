package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgRecipe 定义 rpg_recipe 表的持久化结构。
type RpgRecipe struct {
	ent.Schema
}

// Fields 返回 rpg_recipe 表全部字段及其数据库约束。
func (RpgRecipe) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 制作配方记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("RPG 制作配方的全局唯一英文机器编码。"),
		field.String("name").MaxLen(120).Comment("RPG 制作配方的简体中文展示名称。"),
		field.String("required_profession_code").MaxLen(64).Optional().Nillable().Comment("RPG 制作配方制作所需的可选职业机器编码。"),
		field.Int32("required_profession_level").Optional().Nillable().Comment("RPG 制作配方制作所需的可选最低职业等级。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 制作配方是否可被新的 RPG 进度引用。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("RPG 制作配方写入使用的正整数乐观并发版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 制作配方首次创建的 UTC 时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 制作配方最近一次业务更新的 UTC 时间。"),
	}
}

// Annotations 固定 rpg_recipe 的表名、注释、复合主键和检查约束。
func (RpgRecipe) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("把一组道具材料转换为一组道具产物的制作配方。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_recipe", Checks: map[string]string{
			"rpg_recipe_code_check":       "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_recipe_name_check":       "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_recipe_profession_check": "required_profession_code IS NULL AND required_profession_level IS NULL OR required_profession_code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text AND required_profession_level > 0",
			"rpg_recipe_time_check":       "updated_at >= created_at",
			"rpg_recipe_version_check":    "version > 0",
		}},
	}
}
