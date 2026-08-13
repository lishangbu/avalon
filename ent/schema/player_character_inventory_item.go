package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterInventoryItem 定义 player_character_inventory_item 表的持久化结构。
type PlayerCharacterInventoryItem struct {
	ent.Schema
}

// Fields 返回 player_character_inventory_item 表全部字段及其数据库约束。
func (PlayerCharacterInventoryItem) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("玩家背包道具所属 PlayerCharacter 的稳定 Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("玩家背包道具引用的道具稳定 Identifier。"),
		field.Int64("quantity").Comment("玩家背包道具保存的非负或正整数道具数量。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("玩家背包道具写入使用的正整数乐观并发版本。"),
		field.Time("updated_at").Comment("玩家背包道具最近一次业务更新的 UTC 时间。"),
	}
}

// Indexes 返回 player_character_inventory_item 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterInventoryItem) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_id", "item_id").Unique().StorageKey("uk_player_character_inventory_item_player_character_id_item_id"),
	}
}

// Annotations 固定 player_character_inventory_item 的表名、注释、复合主键和检查约束。
func (PlayerCharacterInventoryItem) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 背包中按道具聚合的当前非负数量。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_inventory_item", Checks: map[string]string{
			"player_character_inventory_item_quantity_check": "quantity >= 0",
			"player_character_inventory_item_version_check":  "version > 0",
		}},
	}
}
