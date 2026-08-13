package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgShopItem 定义 rpg_shop_item 表的持久化结构。
type RpgShopItem struct {
	ent.Schema
}

// Fields 返回 rpg_shop_item 表全部字段及其数据库约束。
func (RpgShopItem) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("shop_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 商店商品所属商店稳定 Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 商店商品引用的道具稳定 Identifier。"),
		field.Int64("currency_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 商店商品使用或变更的游戏货币稳定 Identifier。"),
		field.Int64("buy_price").Comment("RPG 商店商品使用指定货币购买一件时的非负价格。"),
		field.Int64("sell_price").Optional().Nillable().Comment("RPG 商店商品出售一件时获得的可选非负价格。"),
		field.Int32("stock_limit").Optional().Nillable().Comment("RPG 商店商品在一个补货周期内的可选正整数库存上限。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 商店商品是否可被新的 RPG 进度引用。"),
	}
}

// Indexes 返回 rpg_shop_item 原复合主键对应的稳定业务唯一约束。
func (RpgShopItem) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("shop_id", "item_id", "currency_id").Unique().StorageKey("uk_rpg_shop_item_shop_id_item_id_currency_id"),
	}
}

// Annotations 固定 rpg_shop_item 的表名、注释、复合主键和检查约束。
func (RpgShopItem) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("商店中使用指定货币定价和限制库存的道具。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_shop_item", Checks: map[string]string{
			"rpg_shop_item_price_check": "buy_price >= 0 AND (sell_price IS NULL OR sell_price >= 0)",
			"rpg_shop_item_stock_check": "stock_limit IS NULL OR stock_limit > 0",
		}},
	}
}
