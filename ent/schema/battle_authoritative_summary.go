package schema

import (
	"encoding/json"
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// BattleAuthoritativeSummary 定义 battle_authoritative_summary 表的持久化结构。
type BattleAuthoritativeSummary struct {
	ent.Schema
}

// Fields 返回 battle_authoritative_summary 表全部字段及其数据库约束。
func (BattleAuthoritativeSummary) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).StorageKey("battle_id").Comment("battle_authoritative_summary 表的 battle_id 字段。"),
		field.String("mode").MaxLen(8).Comment("终局 Battle 的 pvp 或 pve 模式。"),
		field.String("source_type").MaxLen(16).Comment("终局 Battle 的 challenge、training 或 encounter 来源。"),
		field.Int16("winner_side").Optional().Nillable().Comment("battle_authoritative_summary 表的 winner_side 字段。"),
		field.String("terminal_reason").MaxLen(64).Comment("battle_authoritative_summary 表的 terminal_reason 字段。"),
		field.Int32("turn_count").Comment("battle_authoritative_summary 表的 turn_count 字段。"),
		field.JSON("summary", json.RawMessage{}).Comment("battle_authoritative_summary 表的 summary 字段。"),
		field.Time("completed_at").Comment("battle_authoritative_summary 表的 completed_at 字段。"),
	}
}

// Annotations 固定 battle_authoritative_summary 的表名、注释、复合主键和检查约束。
func (BattleAuthoritativeSummary) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("Battle 正常完成后同事务写入的历史和分析最小权威摘要。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_authoritative_summary", Checks: map[string]string{
			"battle_authoritative_summary_mode_check":        "mode::text = ANY (ARRAY['pvp'::text, 'pve'::text])",
			"battle_authoritative_summary_source_type_check": "source_type::text = ANY (ARRAY['challenge'::text, 'training'::text, 'encounter'::text])",
			"battle_authoritative_summary_summary_check":     "jsonb_typeof(summary) = 'object'::text",
			"battle_authoritative_summary_turn_count_check":  "turn_count >= 0",
			"battle_authoritative_summary_winner_side_check": "winner_side IS NULL OR (winner_side = ANY (ARRAY[1, 2]))",
		}},
	}
}
