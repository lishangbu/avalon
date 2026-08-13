package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// BattleTurnSubmission 定义 battle_turn_submission 表的持久化结构。
type BattleTurnSubmission struct {
	ent.Schema
}

// Fields 返回 battle_turn_submission 表全部字段及其数据库约束。
func (BattleTurnSubmission) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("battle_id").GoType(snowflake.ID(0)).Positive().Comment("battle_turn_submission 表的 battle_id 字段。"),
		field.String("idempotency_key").MaxLen(128).Comment("battle_turn_submission 表的 idempotency_key 字段。"),
		field.Int16("side").Comment("battle_turn_submission 表的 side 字段。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("battle_turn_submission 表的 player_character_id 字段。"),
		field.String("bot_code").MaxLen(64).Optional().Nillable().Comment("battle_turn_submission 表的 bot_code 字段。"),
		field.Int32("bot_strategy_version").Optional().Nillable().Comment("battle_turn_submission 表的 bot_strategy_version 字段。"),
		field.Int64("state_version").Comment("battle_turn_submission 表的 state_version 字段。"),
		field.Bytes("request_digest").Comment("battle_turn_submission 表的 request_digest 字段。"),
		field.Time("created_at").Comment("battle_turn_submission 表的 created_at 字段。"),
	}
}

// Indexes 返回 battle_turn_submission 原复合主键对应的稳定业务唯一约束。
func (BattleTurnSubmission) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("battle_id", "idempotency_key").Unique().StorageKey("uk_battle_turn_submission_battle_id_idempotency_key"),
	}
}

// Annotations 固定 battle_turn_submission 的表名、注释、复合主键和检查约束。
func (BattleTurnSubmission) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("每名真人 Participant 已提交回合选择的幂等键、摘要和对应权威 Turn Record。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_turn_submission", Checks: map[string]string{
			"battle_turn_submission_key_check":            "char_length(idempotency_key::text) >= 1 AND char_length(idempotency_key::text) <= 128",
			"battle_turn_submission_participant_check":    "player_character_id IS NOT NULL AND bot_code IS NULL AND bot_strategy_version IS NULL OR player_character_id IS NULL AND bot_code IS NOT NULL AND bot_strategy_version >= 1",
			"battle_turn_submission_request_digest_check": "octet_length(request_digest) = 32",
			"battle_turn_submission_side_check":           "side = ANY (ARRAY[1, 2])",
			"battle_turn_submission_state_version_check":  "state_version >= 1",
		}},
	}
}
