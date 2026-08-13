package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameSpecies 定义 game_species 表的持久化结构。
type GameSpecies struct {
	ent.Schema
}

// Fields 返回 game_species 表全部字段及其数据库约束。
func (GameSpecies) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.Int32("national_dex_number").Comment("该资料记录的 national dex number 业务属性。"),
		field.String("code").MaxLen(64).Comment("该资料记录的全局唯一稳定编码。"),
		field.String("name").MaxLen(120).Comment("该资料记录的简体中文显示名称。"),
		field.Int64("growth_rate_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("该资料记录的 growth rate id 业务属性。"),
		field.Int64("habitat_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("该资料记录的 habitat id 业务属性。"),
		field.Int64("color_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("该资料记录的 color id 业务属性。"),
		field.Int64("shape_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("该资料记录的 shape id 业务属性。"),
		field.String("genus").MaxLen(200).Optional().Nillable().Comment("该资料记录的 genus 业务属性。"),
		field.String("pokedex_entry").Optional().Nillable().Comment("该资料记录的 pokedex entry 业务属性。"),
		field.String("description").Optional().Nillable().Comment("该资料记录的简体中文说明。"),
		field.String("profile").Optional().Nillable().Comment("该资料记录的 profile 业务属性。"),
		field.String("design_origin").Optional().Nillable().Comment("该资料记录的 design origin 业务属性。"),
		field.String("trivia").Optional().Nillable().Comment("该资料记录的 trivia 业务属性。"),
		field.Bool("gender_differences").Comment("该资料记录的 gender differences 业务属性。"),
		field.Bool("forms_switchable").Comment("该资料记录的 forms switchable 业务属性。"),
		field.Bool("enabled").Comment("是否允许新的业务数据引用该实时资料。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_species 的表名、注释、复合主键和检查约束。
func (GameSpecies) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料表。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_species", Checks: map[string]string{
			"game_species_code_check":                "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"game_species_description_check":         "char_length(description) >= 1 AND char_length(description) <= 4000 AND description = btrim(description)",
			"game_species_design_origin_check":       "char_length(design_origin) >= 1 AND char_length(design_origin) <= 4000 AND design_origin = btrim(design_origin)",
			"game_species_name_check":                "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"game_species_national_dex_number_check": "national_dex_number > 0",
			"game_species_profile_check":             "char_length(profile) >= 1 AND char_length(profile) <= 4000 AND profile = btrim(profile)",
			"game_species_trivia_check":              "char_length(trivia) >= 1 AND char_length(trivia) <= 4000 AND trivia = btrim(trivia)",
			"game_species_version_check":             "version > 0",
		}},
	}
}
