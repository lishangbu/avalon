package schema

import (
	"encoding/json"
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/edge"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterLootSettlement 是 Battle 或世界交互预先建立的权威掉落结算。
type PlayerCharacterLootSettlement struct{ ent.Schema }

// Fields 返回结算归属、不可伪造来源和领取终态。
func (PlayerCharacterLootSettlement) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("掉落结算的稳定 Snowflake Identifier，同时作为资产获取来源引用。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("有权领取该结算的 PlayerCharacter Identifier。"),
		field.Int64("loot_table_id").GoType(snowflake.ID(0)).Positive().Comment("抽样时使用的 Loot Table Identifier。"),
		field.String("source_type").MaxLen(16).Comment("建立结算的 battle 或 world 闭集来源类型。"),
		field.Int64("source_reference_id").GoType(snowflake.ID(0)).Positive().Comment("Battle 或世界交互的权威来源 Identifier。"),
		field.Int64("operation_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("领取后全部资产流水使用的可选 Operation Identifier。"),
		field.String("state").MaxLen(16).Comment("结算当前 pending 或 claimed 闭集状态。"),
		field.String("random_algorithm").MaxLen(32).Comment("建立结算时使用的版本化随机算法。"),
		field.JSON("random_trace", json.RawMessage{}).Comment("不含 seed 的用途、draw 序号、命中条目与数量轨迹。"),
		field.Time("created_at").Comment("服务端建立结算的 UTC 时间。"),
		field.Time("claimed_at").Optional().Nillable().Comment("完成原子领取的可选 UTC 时间。"),
	}
}

// Edges 返回结算与所属角色、掉落表及冻结条目的权威关系。
func (PlayerCharacterLootSettlement) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_loot_settlement_player_character_id_id")),
		edge.To("loot_table", RpgLootTable.Type).Field("loot_table_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_loot_settlement_loot_table_id_id")),
		edge.From("entries", PlayerCharacterLootSettlementEntry.Type).Ref("loot_settlement"),
	}
}

// Indexes 保证同一权威来源只建立一个掉落结算。
func (PlayerCharacterLootSettlement) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("source_type", "source_reference_id").Unique().StorageKey("uk_player_character_loot_settlement_source_type_source_reference_id"),
		index.Fields("player_character_id", "state", "created_at").StorageKey("idx_player_character_loot_settlement_player_character_id_state_created_at"),
	}
}

// Annotations 固定掉落来源与领取终态组合。
func (PlayerCharacterLootSettlement) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("由服务端权威抽样并等待所属 PlayerCharacter 一次性领取的掉落结算。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_loot_settlement", Checks: map[string]string{
		"player_character_loot_settlement_source_check": "source_type IN ('battle','world')",
		"player_character_loot_settlement_random_check": "random_algorithm = 'hmac-sha256-v1' AND jsonb_typeof(random_trace) = 'object'",
		"player_character_loot_settlement_state_check":  "(state = 'pending' AND operation_id IS NULL AND claimed_at IS NULL) OR (state = 'claimed' AND operation_id IS NOT NULL AND claimed_at IS NOT NULL)",
	}}}
}
