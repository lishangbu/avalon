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

// BattlePreviewSubmission 定义 battle_preview_submission 表的持久化结构。
type BattlePreviewSubmission struct {
	ent.Schema
}

// Fields 返回 battle_preview_submission 表全部字段及其数据库约束。
func (BattlePreviewSubmission) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("battle_id").GoType(snowflake.ID(0)).Positive().Comment("所属 Battle 稳定 Identifier。"),
		field.Int16("side").Comment("提交选择的固定参赛方位置。"),
		field.JSON("member_positions", json.RawMessage{}).Comment("按玩家确认顺序选择的参赛成员位置 JSON 数组。"),
		field.JSON("active_positions", json.RawMessage{}).Comment("按场上槽位顺序选择的初始上场成员位置 JSON 数组。"),
		field.Time("submitted_at").Comment("服务端锁定选择的 UTC 时间。"),
		field.JSON("random_trace", json.RawMessage{}).Comment("到期自动 Preview 补选使用的可重放随机轨迹；真人和固定 Bot 选择为空数组。"),
	}
}

// Indexes 返回 battle_preview_submission 原复合主键对应的稳定业务唯一约束。
func (BattlePreviewSubmission) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("battle_id", "side").Unique().StorageKey("uk_battle_preview_submission_battle_id_side"),
	}
}

// Annotations 固定 battle_preview_submission 的表名、注释、复合主键和检查约束。
func (BattlePreviewSubmission) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("双方不可覆盖的 Team Preview 秘密选择。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_preview_submission", Checks: map[string]string{
			"battle_preview_submission_active_positions_check": "jsonb_typeof(active_positions) = 'array'::text",
			"battle_preview_submission_member_positions_check": "jsonb_typeof(member_positions) = 'array'::text",
			"battle_preview_submission_random_trace_check":     "jsonb_typeof(random_trace) = 'array'::text",
			"battle_preview_submission_side_check":             "side = ANY (ARRAY[1, 2])",
		}},
	}
}
