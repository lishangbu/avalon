package schema

import (
	"encoding/json"
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// Battle 定义 battle 表的持久化结构。
type Battle struct {
	ent.Schema
}

// Fields 返回 battle 表全部字段及其数据库约束。
func (Battle) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("Battle 的稳定 Identifier。"),
		field.String("mode").MaxLen(8).Comment("Battle 参与关系类型：pvp 或 pve。"),
		field.String("source_type").MaxLen(16).Comment("Battle 来源：challenge、training 或 encounter。"),
		field.Int64("challenge_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("创建 PvP Battle 的已接受 Challenge。"),
		field.Int64("pending_encounter_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("创建 Encounter PvE Battle 的待处理遭遇。"),
		field.String("status").MaxLen(16).Comment("created、preview、running、completed、canceled 或 interrupted 生命周期状态。"),
		field.Int64("battle_format_id").GoType(snowflake.ID(0)).Positive().Comment("使用的实时 BattleFormat 稳定 Identifier。"),
		field.JSON("battle_format_snapshot", json.RawMessage{}).Comment("赛制、条款和机制的完整冻结 JSON 快照。"),
		field.JSON("format", json.RawMessage{}).Comment("Preview 数量、上场人数和期限所需的最小冻结赛制 JSON。"),
		field.Time("preview_deadline_at").Comment("双方必须锁定 Team Preview 的 UTC 截止时间。"),
		field.Time("battle_deadline_at").Comment("整场 Battle 的 UTC 超时裁决时间。"),
		field.JSON("initial_state", json.RawMessage{}).Optional().Comment("Runtime 启动后写入的完整纯 Battle Engine 初始状态 JSON。"),
		field.JSON("random_source", json.RawMessage{}).Optional().Comment("当前 state_version 下一回合使用的确定性随机源快照。"),
		field.JSON("initial_events", json.RawMessage{}).Optional().Comment("初始入场阶段已公开的结构化事件 JSON 数组；不属于任何 Turn Record。"),
		field.JSON("result", json.RawMessage{}).Optional().Comment("Completed Battle 的结构化终局结果；Interrupted 时为空。"),
		field.String("terminal_reason").MaxLen(64).Optional().Nillable().Comment("终局或中断的稳定机器原因。"),
		field.Int64("state_version").Annotations(entsql.DefaultExpr("0")).Comment("已成功提交 Turn Record 的连续权威状态版本。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("生命周期和 Preview 写入使用的乐观并发版本。"),
		field.Time("created_at").Comment("Battle 创建 UTC 时间。"),
		field.Time("updated_at").Comment("最近一次生命周期或 Preview 转换 UTC 时间。"),
		field.Time("started_at").Optional().Nillable().Comment("纯引擎和 Runtime 成功启动并进入 running 的 UTC 时间。"),
		field.Time("completed_at").Optional().Nillable().Comment("Completed 或 Interrupted 终态写入的 UTC 时间。"),
	}
}

// Annotations 固定 battle 的表名、注释和检查约束。
func (Battle) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("一场权威 Battle 的生命周期、冻结赛制和最终结果；不保存运行中的 Runtime。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle", Checks: map[string]string{
			"battle_source_check":         "(source_type::text = 'challenge'::text AND mode::text = 'pvp'::text AND challenge_id IS NOT NULL AND pending_encounter_id IS NULL) OR (source_type::text = 'training'::text AND mode::text = 'pve'::text AND challenge_id IS NULL AND pending_encounter_id IS NULL) OR (source_type::text = 'encounter'::text AND mode::text = 'pve'::text AND challenge_id IS NULL AND pending_encounter_id IS NOT NULL)",
			"battle_deadline_check":       "battle_deadline_at > preview_deadline_at",
			"battle_terminal_check":       "(status::text = ANY (ARRAY['created'::text, 'preview'::text, 'running'::text])) AND completed_at IS NULL AND result IS NULL AND terminal_reason IS NULL OR (status::text = ANY (ARRAY['completed'::text, 'canceled'::text, 'interrupted'::text])) AND completed_at IS NOT NULL AND terminal_reason IS NOT NULL",
			"battle_updated_at_check":     "updated_at >= created_at",
			"battle_preview_check":        "status::text <> 'preview'::text OR initial_state IS NULL AND initial_events IS NULL AND started_at IS NULL AND state_version = 0",
			"battle_initial_events_check": "initial_events IS NULL OR jsonb_typeof(initial_events) = 'array'::text",
			"battle_initial_state_check":  "initial_state IS NULL OR jsonb_typeof(initial_state) = 'object'::text",
			"battle_random_source_check":  "random_source IS NULL OR jsonb_typeof(random_source) = 'object'::text",
			"battle_mode_check":           "mode::text = ANY (ARRAY['pvp'::text, 'pve'::text])",
			"battle_format_check":         "jsonb_typeof(format) = 'object'::text",
			"battle_state_version_check":  "state_version >= 0",
			"battle_status_check":         "status::text = ANY (ARRAY['created'::text, 'preview'::text, 'running'::text, 'completed'::text, 'canceled'::text, 'interrupted'::text])",
			"battle_version_check":        "version >= 1",
		}},
	}
}
