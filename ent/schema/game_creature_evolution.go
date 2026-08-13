package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameCreatureEvolution 定义 game_creature_evolution 表的持久化结构。
type GameCreatureEvolution struct {
	ent.Schema
}

// Fields 返回 game_creature_evolution 表全部字段及其数据库约束。
func (GameCreatureEvolution) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.Int64("from_creature_id").GoType(snowflake.ID(0)).Positive().Comment("该资料记录的 from creature id 业务属性。"),
		field.Int64("to_creature_id").GoType(snowflake.ID(0)).Positive().Comment("该资料记录的 to creature id 业务属性。"),
		field.String("trigger_type").MaxLen(32).Comment("该资料记录的 trigger type 业务属性。"),
		field.Int32("minimum_level").Optional().Nillable().Comment("该资料记录的 minimum level 业务属性。"),
		field.Int64("trigger_item_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("该资料记录的 trigger item id 业务属性。"),
		field.Int32("minimum_friendship").Optional().Nillable().Comment("该资料记录的 minimum friendship 业务属性。"),
		field.String("time_of_day").MaxLen(32).Optional().Nillable().Comment("该资料记录的 time of day 业务属性。"),
		field.String("gender").MaxLen(16).Optional().Nillable().Comment("该资料记录的 gender 业务属性。"),
		field.Int64("required_skill_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("该资料记录的 required skill id 业务属性。"),
		field.String("condition_text").Comment("该资料记录的 condition text 业务属性。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("是否允许新的业务数据引用该实时资料。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_creature_evolution 的表名、注释、复合主键和检查约束。
func (GameCreatureEvolution) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料表。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_creature_evolution", Checks: map[string]string{
			"game_creature_evolution_condition_text_check":     "condition_text = btrim(condition_text) AND char_length(condition_text) >= 1 AND char_length(condition_text) <= 2000",
			"game_creature_evolution_distinct_creatures_check": "from_creature_id <> to_creature_id",
			"game_creature_evolution_gender_check":             "gender IS NULL OR (gender::text = ANY (ARRAY['male'::character varying, 'female'::character varying]::text[]))",
			"game_creature_evolution_minimum_friendship_check": "minimum_friendship IS NULL OR minimum_friendship >= 0",
			"game_creature_evolution_minimum_level_check":      "minimum_level IS NULL OR minimum_level > 0",
			"game_creature_evolution_time_of_day_check":        "time_of_day IS NULL OR (time_of_day::text = ANY (ARRAY['day'::character varying, 'night'::character varying, 'dusk'::character varying]::text[]))",
			"game_creature_evolution_trigger_type_check":       "trigger_type::text = ANY (ARRAY['level'::text, 'item'::text, 'trade'::text, 'friendship'::text, 'skill'::text, 'breeding'::text, 'special'::text])",
			"game_creature_evolution_version_check":            "version > 0",
		}},
	}
}
