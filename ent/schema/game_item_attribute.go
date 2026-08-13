package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemAttribute 定义道具 Attribute 字典。
type GameItemAttribute struct{ ent.Schema }

// Fields 返回 Attribute 的规范化字段。
func (GameItemAttribute) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("Attribute 稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("Attribute 稳定编码。"),
		field.String("name").MaxLen(120).Comment("Attribute 简体中文名称。"),
		field.String("description").Optional().Nillable().Comment("Attribute 说明。"),
		field.Bool("enabled").Comment("是否启用。"),
		field.Int64("version").Comment("乐观并发版本。"),
		field.Time("created_at").Comment("创建时间。"),
		field.Time("updated_at").Comment("更新时间。"),
	}
}

// Annotations 固定 Attribute 表名及约束。
func (GameItemAttribute) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具 Attribute。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_attribute", Checks: map[string]string{
		"game_item_attribute_code_check":    "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
		"game_item_attribute_version_check": "version > 0",
	}}}
}
