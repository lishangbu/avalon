package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemCatalogCategoryBinding 定义 game_item_catalog_category_binding 表的持久化结构。
type GameItemCatalogCategoryBinding struct {
	ent.Schema
}

// Fields 返回 game_item_catalog_category_binding 表全部字段及其数据库约束。
func (GameItemCatalogCategoryBinding) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("该资料记录的 item id 业务属性。"),
		field.Int64("category_id").GoType(snowflake.ID(0)).Positive().Comment("关联分类资料的稳定 Identifier。"),
	}
}

// Indexes 返回 game_item_catalog_category_binding 原复合主键对应的稳定业务唯一约束。
func (GameItemCatalogCategoryBinding) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("item_id", "category_id").Unique().StorageKey("uk_game_item_catalog_category_binding_item_id_category_id"),
	}
}

// Annotations 固定 game_item_catalog_category_binding 的表名、注释、复合主键和检查约束。
func (GameItemCatalogCategoryBinding) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料表。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_item_catalog_category_binding"},
	}
}
