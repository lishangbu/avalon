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

// PlayerCharacterLootSettlementEntry 保存掉落抽样后不可变的一种道具及数量。
type PlayerCharacterLootSettlementEntry struct{ ent.Schema }

// Fields 返回结算、原始候选、道具和抽样数量。
func (PlayerCharacterLootSettlementEntry) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("掉落结算条目的稳定 Snowflake Identifier。"),
		field.Int64("loot_settlement_id").GoType(snowflake.ID(0)).Positive().Comment("所属权威 Loot Settlement Identifier。"),
		field.Int64("loot_entry_id").GoType(snowflake.ID(0)).Positive().Comment("抽样命中的 Loot Entry Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("抽样时冻结的 Item Catalog Entry Identifier。"),
		field.Int32("quantity").Positive().Comment("抽样后冻结的正整数道具数量。"),
	}
}

// Edges 返回冻结条目与结算、原始掉落候选和道具的权威关系。
func (PlayerCharacterLootSettlementEntry) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("loot_settlement", PlayerCharacterLootSettlement.Type).Field("loot_settlement_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_loot_settlement_entry_loot_settlement_id_id")),
		edge.To("loot_entry", RpgLootEntry.Type).Field("loot_entry_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_loot_settlement_entry_loot_entry_id_id")),
		edge.To("item", GameItem.Type).Field("item_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_loot_settlement_entry_item_id_id")),
	}
}

// Indexes 防止同一结算重复保存相同候选。
func (PlayerCharacterLootSettlementEntry) Indexes() []ent.Index {
	return []ent.Index{index.Fields("loot_settlement_id", "loot_entry_id").Unique().StorageKey("uk_player_character_loot_settlement_entry_loot_settlement_id_loot_entry_id")}
}

// Annotations 固定掉落结算条目表名与说明。
func (PlayerCharacterLootSettlementEntry) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("Loot Settlement 已抽样并冻结的道具数量事实。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_loot_settlement_entry"}}
}
