package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgProfessionSkill 定义 rpg_profession_skill 表的持久化结构。
type RpgProfessionSkill struct {
	ent.Schema
}

// Fields 返回 rpg_profession_skill 表全部字段及其数据库约束。
func (RpgProfessionSkill) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 职业技能记录的稳定 Identifier。"),
		field.Int64("profession_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 职业技能所属或引用的职业稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("RPG 职业技能的全局唯一英文机器编码。"),
		field.String("name").MaxLen(120).Comment("RPG 职业技能的简体中文展示名称。"),
		field.Int32("required_level").Comment("RPG 职业技能解锁所需的最低职业等级。"),
		field.String("description").Optional().Nillable().Comment("RPG 职业技能面向玩家或管理者的可选简体中文说明。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 职业技能是否可被新的 RPG 进度引用。"),
	}
}

// Annotations 固定 rpg_profession_skill 的表名、注释、复合主键和检查约束。
func (RpgProfessionSkill) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("职业在指定等级后可解锁的一项技能资料。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_profession_skill", Checks: map[string]string{
			"rpg_profession_skill_code_check":           "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_profession_skill_description_check":    "description IS NULL OR char_length(description) >= 1 AND char_length(description) <= 4000 AND description = btrim(description)",
			"rpg_profession_skill_name_check":           "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_profession_skill_required_level_check": "required_level > 0",
		}},
	}
}
