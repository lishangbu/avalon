package schema

import (
	"encoding/json"

	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterPendingEncounter 保存一次抽样后不可重抽的待处理遭遇。
type PlayerCharacterPendingEncounter struct{ ent.Schema }

// Fields 返回待处理遭遇的冻结输入和生命周期字段。
func (PlayerCharacterPendingEncounter) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("待处理遭遇所属 PlayerCharacter 稳定 Identifier。"),
		field.Int64("traversal_id").GoType(snowflake.ID(0)).Positive().Comment("触发该遭遇的 Traversal 稳定 Identifier。"),
		field.Int64("encounter_table_id").GoType(snowflake.ID(0)).Positive().Comment("抽样使用的遭遇表稳定 Identifier。"),
		field.Int64("encounter_entry_id").GoType(snowflake.ID(0)).Positive().Comment("抽样冻结的遭遇条目稳定 Identifier。"),
		field.Int64("battle_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("接受后创建的 PvE Battle 稳定 Identifier。"),
		field.Int64("encounter_table_version").Comment("Traversal 时冻结的遭遇表版本。"),
		field.Int16("encounter_level").Comment("抽样冻结的 Creature 等级。"),
		field.String("random_algorithm").MaxLen(32).Comment("随机算法标识，例如 hmac-sha256-v1。"),
		field.Bytes("random_seed").Sensitive().Comment("仅服务端保存的 32 字节随机 seed。"),
		field.Int64("random_draw_number").Comment("本次抽样在用途内的单调 draw 序号。"),
		field.JSON("random_result", json.RawMessage{}).Comment("不含 seed 的不可变抽样结果摘要。"),
		field.String("state").MaxLen(16).Default("pending").Comment("pending、accepted、cancelled 或 expired。"),
		field.Time("expires_at").Comment("待处理遭遇固定十分钟期限。"),
		field.Time("created_at").Comment("遭遇抽样提交时间。"),
		field.Time("resolved_at").Optional().Nillable().Comment("遭遇进入终态的时间。"),
	}
}

// Indexes 保证一次 Traversal 至多产生一个待处理遭遇，并支持角色查询。
func (PlayerCharacterPendingEncounter) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("traversal_id").Unique().StorageKey("uk_player_character_pending_encounter_traversal_id"),
		index.Fields("player_character_id", "state", "expires_at").StorageKey("idx_player_character_pending_encounter_player_character_id_state_expires_at"),
	}
}

// Annotations 固定遭遇状态、随机结果和期限约束。
func (PlayerCharacterPendingEncounter) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("一次抽样后不可重抽且有固定期限的 PlayerCharacter 遭遇。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_pending_encounter", Checks: map[string]string{
		"player_character_pending_encounter_algorithm_check":     "random_algorithm = 'hmac-sha256-v1'",
		"player_character_pending_encounter_draw_check":          "random_draw_number >= 0",
		"player_character_pending_encounter_seed_check":          "octet_length(random_seed) = 32",
		"player_character_pending_encounter_level_check":         "encounter_level >= 1 AND encounter_level <= 100",
		"player_character_pending_encounter_table_version_check": "encounter_table_version > 0",
		"player_character_pending_encounter_result_check":        "jsonb_typeof(random_result) = 'object'::text",
		"player_character_pending_encounter_state_check":         "state IN ('pending', 'accepted', 'cancelled', 'expired')",
		"player_character_pending_encounter_expiry_check":        "expires_at > created_at",
	}}}
}
