package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameCreatureFormElement 定义 game_creature_form_element 表的持久化结构。
type GameCreatureFormElement struct {
	ent.Schema
}

// Fields 返回 game_creature_form_element 表全部字段及其数据库约束。
func (GameCreatureFormElement) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("form_id").GoType(snowflake.ID(0)).Positive().Comment("该资料记录的 form id 业务属性。"),
		field.Int64("element_id").GoType(snowflake.ID(0)).Positive().Comment("关联属性资料的稳定 Identifier。"),
	}
}

// Indexes 返回 game_creature_form_element 原复合主键对应的稳定业务唯一约束。
func (GameCreatureFormElement) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("form_id", "element_id").Unique().StorageKey("uk_game_creature_form_element_form_id_element_id"),
	}
}

// Annotations 固定 game_creature_form_element 的表名、注释、复合主键和检查约束。
func (GameCreatureFormElement) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料表。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_creature_form_element"},
	}
}
