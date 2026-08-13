package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgRecipeIngredient 定义 rpg_recipe_ingredient 表的持久化结构。
type RpgRecipeIngredient struct {
	ent.Schema
}

// Fields 返回 rpg_recipe_ingredient 表全部字段及其数据库约束。
func (RpgRecipeIngredient) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("recipe_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 配方材料所属制作配方稳定 Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 配方材料引用的道具稳定 Identifier。"),
		field.Int32("quantity").Comment("RPG 配方材料保存的非负或正整数道具数量。"),
	}
}

// Indexes 返回 rpg_recipe_ingredient 原复合主键对应的稳定业务唯一约束。
func (RpgRecipeIngredient) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("recipe_id", "item_id").Unique().StorageKey("uk_rpg_recipe_ingredient_recipe_id_item_id"),
	}
}

// Annotations 固定 rpg_recipe_ingredient 的表名、注释、复合主键和检查约束。
func (RpgRecipeIngredient) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("制作配方需要消耗的道具及正整数数量。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_recipe_ingredient", Checks: map[string]string{
			"rpg_recipe_ingredient_quantity_check": "quantity > 0",
		}},
	}
}
