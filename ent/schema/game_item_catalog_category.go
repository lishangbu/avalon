package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemCatalogCategory 定义 game_item_catalog_category 表的持久化结构。
type GameItemCatalogCategory struct {
	ent.Schema
}

// Fields 返回 game_item_catalog_category 表全部字段及其数据库约束。
func (GameItemCatalogCategory) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.String("code").MaxLen(120).Comment("该资料记录的全局唯一稳定编码。"),
		field.Int64("parent_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("该资料记录的 parent id 业务属性。"),
		field.String("name").MaxLen(120).Comment("该资料记录的简体中文显示名称。"),
		field.Int32("sort_order").Comment("同类资料中的稳定展示顺序。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("是否允许新的业务数据引用该实时资料。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_item_catalog_category 的表名、注释、复合主键和检查约束。
func (GameItemCatalogCategory) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料表。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_item_catalog_category", Checks: map[string]string{
			"game_item_catalog_category_code_check":       "code::text ~ '^[a-z][a-z0-9-]{1,119}$'::text",
			"game_item_catalog_category_name_check":       "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"game_item_catalog_category_parent_check":     "parent_id IS NULL OR parent_id <> id",
			"game_item_catalog_category_sort_order_check": "sort_order > 0",
			"game_item_catalog_category_time_check":       "updated_at >= created_at",
		}},
	}
}
