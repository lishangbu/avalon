package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameSpeciesEggGroup 定义 game_species_egg_group 表的持久化结构。
type GameSpeciesEggGroup struct {
	ent.Schema
}

// Fields 返回 game_species_egg_group 表全部字段及其数据库约束。
func (GameSpeciesEggGroup) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("species_id").GoType(snowflake.ID(0)).Positive().Comment("该资料记录的 species id 业务属性。"),
		field.Int64("egg_group_id").GoType(snowflake.ID(0)).Positive().Comment("该资料记录的 egg group id 业务属性。"),
	}
}

// Indexes 返回 game_species_egg_group 原复合主键对应的稳定业务唯一约束。
func (GameSpeciesEggGroup) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("species_id", "egg_group_id").Unique().StorageKey("uk_game_species_egg_group_species_id_egg_group_id"),
	}
}

// Annotations 固定 game_species_egg_group 的表名、注释、复合主键和检查约束。
func (GameSpeciesEggGroup) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料表。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_species_egg_group"},
	}
}
