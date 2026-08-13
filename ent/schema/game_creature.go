package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameCreature 定义 game_creature 表的持久化结构。
type GameCreature struct {
	ent.Schema
}

// Fields 返回 game_creature 表全部字段及其数据库约束。
func (GameCreature) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("该资料记录的全局唯一稳定编码。"),
		field.String("name").MaxLen(120).Comment("该资料记录的简体中文显示名称。"),
		field.Int64("species_id").GoType(snowflake.ID(0)).Positive().Comment("该资料记录的 species id 业务属性。"),
		field.Int64("inherits_from_creature_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("该资料记录的 inherits from creature id 业务属性。"),
		field.Int32("height").Optional().Nillable().Comment("该资料记录的 height 业务属性。"),
		field.Int32("weight").Optional().Nillable().Comment("该资料记录的 weight 业务属性。"),
		field.Int32("base_experience").Optional().Nillable().Comment("该资料记录的 base experience 业务属性。"),
		field.Int32("capture_rate").Optional().Nillable().Comment("该资料记录的 capture rate 业务属性。"),
		field.Int32("hatch_cycles").Optional().Nillable().Comment("该资料记录的 hatch cycles 业务属性。"),
		field.Int16("male_eighths").Comment("该资料记录的 male eighths 业务属性。"),
		field.Int16("female_eighths").Comment("该资料记录的 female eighths 业务属性。"),
		field.Bool("default_form").Comment("该资料记录的 default form 业务属性。"),
		field.Bool("enabled").Comment("是否允许新的业务数据引用该实时资料。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_creature 的表名、注释、复合主键和检查约束。
func (GameCreature) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料表。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_creature", Checks: map[string]string{
			"game_creature_capture_rate_check": "capture_rate IS NULL OR capture_rate >= 0 AND capture_rate <= 255",
			"game_creature_code_check":         "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"game_creature_gender_ratio_check": "(male_eighths = 0 AND female_eighths = 0 OR (male_eighths + female_eighths) = 8) AND male_eighths >= 0 AND male_eighths <= 8 AND female_eighths >= 0 AND female_eighths <= 8",
			"game_creature_hatch_cycles_check": "hatch_cycles IS NULL OR hatch_cycles >= 0",
			"game_creature_inheritance_check":  "inherits_from_creature_id IS NULL OR inherits_from_creature_id <> id",
			"game_creature_name_check":         "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"game_creature_size_check":         "(height IS NULL OR height >= 0) AND (weight IS NULL OR weight >= 0) AND (base_experience IS NULL OR base_experience >= 0)",
			"game_creature_version_check":      "version > 0",
		}},
	}
}
