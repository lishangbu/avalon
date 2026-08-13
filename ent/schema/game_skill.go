package schema

import (
	"encoding/json"

	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameSkill 定义 game_skill 表的持久化结构。
type GameSkill struct {
	ent.Schema
}

// Fields 返回 game_skill 表全部字段及其数据库约束。
func (GameSkill) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("该资料记录的全局唯一稳定编码。"),
		field.String("name").MaxLen(120).Comment("该资料记录的简体中文显示名称。"),
		field.Int64("element_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("关联属性资料的稳定 Identifier。"),
		field.Int64("damage_class_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("关联伤害分类资料的稳定 Identifier。"),
		field.Int32("accuracy").Optional().Nillable().Comment("该资料记录的 accuracy 业务属性。"),
		field.Int32("power").Optional().Nillable().Comment("该资料记录的 power 业务属性。"),
		field.Int32("pp").Optional().Nillable().Comment("该资料记录的 pp 业务属性。"),
		field.Int32("priority").Comment("该资料记录的 priority 业务属性。"),
		field.Int32("effect_chance").Optional().Nillable().Comment("该资料记录的 effect chance 业务属性。"),
		field.String("effect").Optional().Nillable().Comment("技能的完整机制说明，不作为结构化战斗规则的替代。"),
		field.String("short_effect").MaxLen(500).Optional().Nillable().Comment("技能机制的简短说明。"),
		field.String("description").MaxLen(500).Optional().Nillable().Comment("技能面向玩家展示的简体中文说明。"),
		field.JSON("rules", json.RawMessage{}).Annotations(entsql.DefaultExpr("'{}'::jsonb")).Comment("按 Battle Engine 执行时机组织的强类型战斗规则。"),
		field.Bool("enabled").Comment("是否允许新的业务数据引用该实时资料。"),
		field.Int64("version").Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_skill 的表名、注释、复合主键和检查约束。
func (GameSkill) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料：技能。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_skill", Checks: map[string]string{
			"game_skill_accuracy_check":      "accuracy >= 1 AND accuracy <= 100",
			"game_skill_code_check":          "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"game_skill_effect_chance_check": "effect_chance >= 0 AND effect_chance <= 100",
			"game_skill_effect_check":        "effect IS NULL OR char_length(effect) >= 1 AND effect = btrim(effect)",
			"game_skill_short_effect_check":  "short_effect IS NULL OR char_length(short_effect) >= 1 AND short_effect = btrim(short_effect)",
			"game_skill_description_check":   "description IS NULL OR char_length(description) >= 1 AND description = btrim(description)",
			"game_skill_name_check":          "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"game_skill_power_check":         "power >= 0",
			"game_skill_pp_check":            "pp > 0",
			"game_skill_rules_check":         "jsonb_typeof(rules) = 'object'::text",
			"game_skill_version_check":       "version > 0",
		}},
	}
}
