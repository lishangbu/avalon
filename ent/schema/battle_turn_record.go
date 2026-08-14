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

// BattleTurnRecord 定义 battle_turn_record 表的持久化结构。
type BattleTurnRecord struct {
	ent.Schema
}

// Fields 返回 battle_turn_record 表全部字段及其数据库约束。
func (BattleTurnRecord) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("battle_id").GoType(snowflake.ID(0)).Positive().Comment("所属 Battle 稳定 Identifier。"),
		field.Int64("state_version").Comment("提交本记录后产生的连续权威状态版本。"),
		field.Int32("turn_number").Comment("纯 Battle Engine 已完成结算的连续回合号。"),
		field.Int32("schema_version").Comment("Turn Record JSON 契约版本。"),
		field.JSON("command", json.RawMessage{}).Comment("双方锁定选择组合后的完整可重放命令 JSON。"),
		field.JSON("events", json.RawMessage{}).Comment("按实际发生顺序编码的版本化事件 JSON 数组。"),
		field.JSON("random_trace", json.RawMessage{}).Comment("本回合实际消耗的确定性随机轨迹 JSON 数组。"),
		field.JSON("state_summary", json.RawMessage{}).Comment("本回合提交后的可审计权威状态摘要 JSON。"),
		field.Time("created_at").Comment("权威回合事务提交时的 UTC 时间。"),
	}
}

// Indexes 返回 battle_turn_record 原复合主键对应的稳定业务唯一约束。
func (BattleTurnRecord) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("battle_id", "state_version").Unique().StorageKey("uk_battle_turn_record_battle_id_state_version"),
	}
}

// Annotations 固定 battle_turn_record 的表名、注释、复合主键和检查约束。
func (BattleTurnRecord) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("每回合的权威命令、事件、随机轨迹和状态摘要，供历史、重放和分析重建。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_turn_record", Checks: map[string]string{
			"battle_turn_record_command_check":        "jsonb_typeof(command) = 'object'::text",
			"battle_turn_record_events_check":         "jsonb_typeof(events) = 'array'::text",
			"battle_turn_record_random_trace_check":   "jsonb_typeof(random_trace) = 'array'::text",
			"battle_turn_record_schema_version_check": "schema_version >= 1",
			"battle_turn_record_state_summary_check":  "jsonb_typeof(state_summary) = 'object'::text",
			"battle_turn_record_state_version_check":  "state_version >= 1",
			"battle_turn_record_turn_number_check":    "turn_number >= 1",
		}},
	}
}
