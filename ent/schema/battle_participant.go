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

// BattleParticipant 定义 battle_participant 表的持久化结构。
type BattleParticipant struct {
	ent.Schema
}

// Fields 返回 battle_participant 表全部字段及其数据库约束。
func (BattleParticipant) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("battle_id").GoType(snowflake.ID(0)).Positive().Comment("battle_participant 表的 battle_id 字段。"),
		field.Int16("side").Comment("Participant 在 Battle 内不可变的阵营位置。"),
		field.String("participant_type").MaxLen(24).Comment("参赛主体类型：player_character 或 bot。"),
		field.String("input_type").MaxLen(16).Comment("冻结参战输入类型：team、party 或 generated。"),
		field.Int64("account_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("真人 Participant 所属 Account。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("真人 Participant 对应的 PlayerCharacter。"),
		field.String("display_name").MaxLen(64).Comment("Battle 创建时冻结的展示名称。"),
		field.Int64("source_team_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("Team 输入的来源 Team。"),
		field.Int64("source_team_version").Optional().Nillable().Comment("Team 输入的来源版本。"),
		field.Int64("source_party_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("Party 输入的来源 Party。"),
		field.Int64("source_party_version").Optional().Nillable().Comment("Party 输入的来源版本。"),
		field.JSON("input_snapshot", json.RawMessage{}).Comment("创建 Battle 时冻结的 Team、Party 或生成对手输入。"),
		field.String("bot_code").MaxLen(64).Optional().Nillable().Comment("Bot Participant 使用的稳定策略代码。"),
		field.Int32("bot_strategy_version").Optional().Nillable().Comment("Bot Participant 使用的策略版本。"),
		field.JSON("bot_definition", json.RawMessage{}).Optional().Comment("创建 Training Battle 时冻结的完整 Bot 策略定义。"),
	}
}

// Indexes 返回 battle_participant 原复合主键对应的稳定业务唯一约束。
func (BattleParticipant) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("battle_id", "side").Unique().StorageKey("uk_battle_participant_battle_id_side"),
	}
}

// Annotations 固定 battle_participant 的表名、注释、复合主键和检查约束。
func (BattleParticipant) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("Battle 创建时冻结的参赛主体和 Team、Party 或生成输入。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_participant", Checks: map[string]string{
			"battle_participant_identity_check":             "(participant_type::text = 'player_character'::text AND account_id IS NOT NULL AND player_character_id IS NOT NULL) OR (participant_type::text = 'bot'::text AND account_id IS NULL AND player_character_id IS NULL)",
			"battle_participant_input_check":                "(input_type::text = 'team'::text AND source_team_id IS NOT NULL AND source_team_version >= 1 AND source_party_id IS NULL AND source_party_version IS NULL) OR (input_type::text = 'party'::text AND source_team_id IS NULL AND source_team_version IS NULL AND source_party_id IS NOT NULL AND source_party_version >= 1) OR (input_type::text = 'generated'::text AND source_team_id IS NULL AND source_team_version IS NULL AND source_party_id IS NULL AND source_party_version IS NULL)",
			"battle_participant_bot_check":                  "(participant_type::text = 'bot'::text AND bot_code IS NOT NULL AND bot_strategy_version >= 1) OR (participant_type::text = 'player_character'::text AND bot_code IS NULL AND bot_strategy_version IS NULL AND bot_definition IS NULL)",
			"battle_participant_snapshot_check":             "jsonb_typeof(input_snapshot) = 'object'::text",
			"battle_participant_side_check":                 "side = ANY (ARRAY[1, 2])",
			"battle_participant_source_team_version_check":  "source_team_version >= 1",
			"battle_participant_source_party_version_check": "source_party_version >= 1",
		}},
	}
}
