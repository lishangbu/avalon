package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemPocket 定义道具 Pocket 字典。
type GameItemPocket struct{ ent.Schema }

// Fields 返回 Pocket 的规范化字段。
func (GameItemPocket) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("Pocket 稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("Pocket 稳定编码。"),
		field.String("name").MaxLen(120).Comment("Pocket 简体中文名称。"),
		field.Int32("sort_order").Comment("Pocket 展示顺序。"),
		field.Bool("enabled").Comment("是否启用。"),
		field.Int64("version").Comment("乐观并发版本。"),
		field.Time("created_at").Comment("创建时间。"),
		field.Time("updated_at").Comment("更新时间。"),
	}
}

// Annotations 固定 Pocket 表名及约束。
func (GameItemPocket) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具 Pocket。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_pocket", Checks: map[string]string{
		"game_item_pocket_code_check":    "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
		"game_item_pocket_version_check": "version > 0",
	}}}
}
