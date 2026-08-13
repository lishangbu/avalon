package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/edge"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterShopPurchase 保存一次已经支付并完成交付的商店购买事实。
type PlayerCharacterShopPurchase struct{ ent.Schema }

// Fields 返回购买归属、商品与成交价格快照。
func (PlayerCharacterShopPurchase) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("商店购买事实的稳定 Snowflake Identifier，同时作为资产获取来源引用。"),
		field.Int64("operation_id").GoType(snowflake.ID(0)).Positive().Comment("购买产生的钱包、背包、装备流水与 Outbox 共同使用的 Operation Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("完成本次购买的 PlayerCharacter Identifier。"),
		field.Int64("shop_item_id").GoType(snowflake.ID(0)).Positive().Comment("成交时读取的 Shop Item Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("成交时冻结的 Item Catalog Entry Identifier。"),
		field.Int64("currency_id").GoType(snowflake.ID(0)).Positive().Comment("成交时冻结的支付 Currency Identifier。"),
		field.Int32("quantity").Positive().Comment("一次购买的正整数商品数量。"),
		field.Int64("unit_price").NonNegative().Comment("成交时冻结的单件非负价格。"),
		field.Int64("total_price").NonNegative().Comment("成交时冻结且经过溢出校验的总价。"),
		field.Int64("balance_after").NonNegative().Comment("支付完成后的对应钱包余额。"),
		field.Time("created_at").Comment("购买事务提交的 UTC 时间。"),
	}
}

// Edges 返回购买事实与角色、商品、道具和货币的权威关系。
func (PlayerCharacterShopPurchase) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_shop_purchase_player_character_id_id")),
		edge.To("shop_item", RpgShopItem.Type).Field("shop_item_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_shop_purchase_shop_item_id_id")),
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_shop_purchase_item_id_id")),
		edge.To("currency", GameCurrency.Type).Field("currency_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_shop_purchase_currency_id_id")),
	}
}

// Indexes 支持角色购买历史和 Operation 诊断。
func (PlayerCharacterShopPurchase) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_id", "created_at", "id").StorageKey("idx_player_character_shop_purchase_player_character_id_created_at_id"),
		index.Fields("operation_id").Unique().StorageKey("uk_player_character_shop_purchase_operation_id"),
	}
}

// Annotations 固定表名并校验成交金额恒等式。
func (PlayerCharacterShopPurchase) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("PlayerCharacter 已支付并完成交付的不可变商店购买事实。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_shop_purchase", Checks: map[string]string{
		"player_character_shop_purchase_total_check": "total_price = unit_price * quantity",
	}}}
}
