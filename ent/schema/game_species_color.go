package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameSpeciesColor 定义 game_species_color 表的持久化结构。
type GameSpeciesColor struct {
	ent.Schema
}

// Fields 返回 game_species_color 表全部字段及其数据库约束。
func (GameSpeciesColor) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("该资料记录的全局唯一稳定编码。"),
		field.String("name").MaxLen(80).Comment("该资料记录的简体中文显示名称。"),
		field.Int32("sort_order").Comment("同类资料中的稳定展示顺序。"),
		field.Bool("enabled").Comment("是否允许新的业务数据引用该实时资料。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_species_color 的表名、注释、复合主键和检查约束。
func (GameSpeciesColor) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料表。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_species_color", Checks: map[string]string{
			"game_species_color_code_check":    "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"game_species_color_name_check":    "char_length(name::text) >= 1 AND char_length(name::text) <= 80 AND name::text = btrim(name::text)",
			"game_species_color_version_check": "version > 0",
		}},
	}
}
