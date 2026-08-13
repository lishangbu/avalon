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

// BattleDisclosureLedger 定义 battle_disclosure_ledger 表的持久化结构。
type BattleDisclosureLedger struct {
	ent.Schema
}

// Fields 返回 battle_disclosure_ledger 表全部字段及其数据库约束。
func (BattleDisclosureLedger) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("battle_id").GoType(snowflake.ID(0)).Positive().Comment("battle_disclosure_ledger 表的 battle_id 字段。"),
		field.Int16("side").Comment("battle_disclosure_ledger 表的 side 字段。"),
		field.Int64("state_version").Comment("battle_disclosure_ledger 表的 state_version 字段。"),
		field.JSON("view", json.RawMessage{}).Comment("battle_disclosure_ledger 表的 view 字段。"),
		field.Time("updated_at").Comment("battle_disclosure_ledger 表的 updated_at 字段。"),
	}
}

// Indexes 返回 battle_disclosure_ledger 原复合主键对应的稳定业务唯一约束。
func (BattleDisclosureLedger) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("battle_id", "side").Unique().StorageKey("uk_battle_disclosure_ledger_battle_id_side"),
	}
}

// Annotations 固定 battle_disclosure_ledger 的表名、注释、复合主键和检查约束。
func (BattleDisclosureLedger) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("按参与者视角保存最近已披露状态，防止 WebSocket 泄露对方秘密选择。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_disclosure_ledger", Checks: map[string]string{
			"battle_disclosure_ledger_side_check":          "side = ANY (ARRAY[1, 2])",
			"battle_disclosure_ledger_state_version_check": "state_version >= 0",
			"battle_disclosure_ledger_view_check":          "jsonb_typeof(view) = 'object'::text",
		}},
	}
}
