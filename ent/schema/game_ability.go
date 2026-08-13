package schema

import (
	"encoding/json"

	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameAbility 定义 game_ability 表的持久化结构。
type GameAbility struct {
	ent.Schema
}

// Fields 返回 game_ability 表全部字段及其数据库约束。
func (GameAbility) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("该资料记录的全局唯一稳定编码。"),
		field.String("name").MaxLen(120).Comment("该资料记录的简体中文显示名称。"),
		field.Bool("main_series").Comment("是否属于主系列资料。"),
		field.String("effect").Optional().Nillable().Comment("特性的完整机制说明，不作为结构化战斗规则的替代。"),
		field.String("short_effect").MaxLen(500).Optional().Nillable().Comment("特性机制的简短说明。"),
		field.String("introduction").MaxLen(500).Optional().Nillable().Comment("特性面向玩家展示的简体中文介绍。"),
		field.JSON("rules", json.RawMessage{}).Annotations(entsql.DefaultExpr("'{}'::jsonb")).Comment("按 Battle Engine 执行时机组织的强类型战斗规则。"),
		field.Bool("enabled").Comment("是否允许新的业务数据引用该实时资料。"),
		field.Int64("version").Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_ability 的表名、注释、复合主键和检查约束。
func (GameAbility) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料：能力。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_ability", Checks: map[string]string{
			"game_ability_code_check":         "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"game_ability_effect_check":       "effect IS NULL OR char_length(effect) >= 1 AND effect = btrim(effect)",
			"game_ability_short_effect_check": "short_effect IS NULL OR char_length(short_effect) >= 1 AND short_effect = btrim(short_effect)",
			"game_ability_introduction_check": "introduction IS NULL OR char_length(introduction) >= 1 AND introduction = btrim(introduction)",
			"game_ability_name_check":         "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"game_ability_rules_check":        "jsonb_typeof(rules) = 'object'::text",
			"game_ability_version_check":      "version > 0",
		}},
	}
}
