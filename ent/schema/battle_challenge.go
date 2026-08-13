package schema

import (
	"encoding/json"
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// BattleChallenge 定义 battle_challenge 表的持久化结构。
type BattleChallenge struct {
	ent.Schema
}

// Fields 返回 battle_challenge 表全部字段及其数据库约束。
func (BattleChallenge) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("battle_challenge 表的 id 字段。"),
		field.Int64("challenger_account_id").GoType(snowflake.ID(0)).Positive().Comment("battle_challenge 表的 challenger_account_id 字段。"),
		field.Int64("challenger_player_character_id").GoType(snowflake.ID(0)).Positive().Comment("battle_challenge 表的 challenger_player_character_id 字段。"),
		field.String("challenger_display_name").MaxLen(64).Comment("battle_challenge 表的 challenger_display_name 字段。"),
		field.Int64("challenger_team_id").GoType(snowflake.ID(0)).Positive().Comment("battle_challenge 表的 challenger_team_id 字段。"),
		field.Int64("challenger_team_version").Comment("battle_challenge 表的 challenger_team_version 字段。"),
		field.JSON("challenger_team_snapshot", json.RawMessage{}).Comment("battle_challenge 表的 challenger_team_snapshot 字段。"),
		field.Int64("target_account_id").GoType(snowflake.ID(0)).Positive().Comment("battle_challenge 表的 target_account_id 字段。"),
		field.Int64("target_player_character_id").GoType(snowflake.ID(0)).Positive().Comment("battle_challenge 表的 target_player_character_id 字段。"),
		field.String("target_display_name").MaxLen(64).Comment("battle_challenge 表的 target_display_name 字段。"),
		field.Int64("battle_format_id").GoType(snowflake.ID(0)).Positive().Comment("battle_challenge 表的 battle_format_id 字段。"),
		field.JSON("battle_format_snapshot", json.RawMessage{}).Comment("battle_challenge 表的 battle_format_snapshot 字段。"),
		field.String("status").MaxLen(16).Comment("battle_challenge 表的 status 字段。"),
		field.String("terminal_reason").MaxLen(64).Optional().Nillable().Comment("battle_challenge 表的 terminal_reason 字段。"),
		field.Time("expires_at").Comment("battle_challenge 表的 expires_at 字段。"),
		field.Time("resolved_at").Optional().Nillable().Comment("battle_challenge 表的 resolved_at 字段。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("battle_challenge 表的 version 字段。"),
		field.Time("created_at").Comment("battle_challenge 表的 created_at 字段。"),
		field.Time("updated_at").Comment("battle_challenge 表的 updated_at 字段。"),
	}
}

// Annotations 固定 battle_challenge 的表名、注释、复合主键和检查约束。
func (BattleChallenge) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 battle_challenge 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_challenge", Checks: map[string]string{
			"battle_challenge_challenger_team_version_check": "challenger_team_version >= 1",
			"battle_challenge_check":                         "challenger_account_id <> target_account_id",
			"battle_challenge_check1":                        "challenger_player_character_id <> target_player_character_id",
			"battle_challenge_check2":                        "status::text = 'pending'::text AND resolved_at IS NULL AND terminal_reason IS NULL OR status::text <> 'pending'::text AND resolved_at IS NOT NULL AND terminal_reason IS NOT NULL",
			"battle_challenge_check3":                        "updated_at >= created_at",
			"battle_challenge_check4":                        "expires_at > created_at",
			"battle_challenge_status_check":                  "status::text = ANY (ARRAY['pending'::character varying::text, 'accepted'::character varying::text, 'rejected'::character varying::text, 'withdrawn'::character varying::text, 'expired'::character varying::text, 'superseded'::character varying::text, 'cancelled'::character varying::text])",
			"battle_challenge_version_check":                 "version >= 1",
		}},
	}
}
