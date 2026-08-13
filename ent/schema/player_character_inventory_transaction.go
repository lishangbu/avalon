package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterInventoryTransaction 定义 player_character_inventory_transaction 表的持久化结构。
type PlayerCharacterInventoryTransaction struct {
	ent.Schema
}

// Fields 返回 player_character_inventory_transaction 表全部字段及其数据库约束。
func (PlayerCharacterInventoryTransaction) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("玩家背包道具流水记录的稳定 Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("玩家背包道具流水所属 PlayerCharacter 的稳定 Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("玩家背包道具流水引用的道具稳定 Identifier。"),
		field.Int64("quantity_delta").Comment("玩家背包道具流水本次发生的非零道具数量增减。"),
		field.Int64("balance_after").Comment("玩家背包道具流水本次流水提交后的非负余额。"),
		field.String("reason_code").MaxLen(64).Comment("玩家背包道具流水产生原因的稳定英文机器编码。"),
		field.Int64("reference_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("玩家背包道具流水来源命令、任务、交易或掉落事实的可选稳定 Identifier。"),
		field.Time("created_at").Comment("玩家背包道具流水首次创建的 UTC 时间。"),
	}
}

// Annotations 固定 player_character_inventory_transaction 的表名、注释、复合主键和检查约束。
func (PlayerCharacterInventoryTransaction) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 道具获得与消耗后的不可变数量流水。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_inventory_transaction", Checks: map[string]string{
			"player_character_inventory_transaction_balance_check": "balance_after >= 0",
			"player_character_inventory_transaction_delta_check":   "quantity_delta <> 0",
			"player_character_inventory_transaction_reason_check":  "reason_code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
		}},
	}
}
