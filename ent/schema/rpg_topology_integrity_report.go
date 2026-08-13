package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgTopologyIntegrityReport 保存一次不可变地图完整性校验结果。
type RpgTopologyIntegrityReport struct{ ent.Schema }

// Fields 返回校验状态和执行时间字段。
func (RpgTopologyIntegrityReport) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()),
		field.String("state").MaxLen(16).Comment("passed 或 failed 校验终态。"),
		field.Int32("issue_count").Comment("校验发现的问题总数。"),
		field.Time("checked_at").Comment("校验完成时间。"),
		field.Int64("duration_milliseconds").Comment("校验耗时毫秒数。"),
	}
}

// Indexes 支持按校验时间倒序读取历史。
func (RpgTopologyIntegrityReport) Indexes() []ent.Index {
	return []ent.Index{index.Fields("checked_at", "id").StorageKey("idx_rpg_topology_integrity_report_checked_at_id")}
}

// Annotations 固定终态和计数约束。
func (RpgTopologyIntegrityReport) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("RPG 地图拓扑的一次不可变完整性校验报告。"), entsql.WithComments(true), entsql.Annotation{Table: "rpg_topology_integrity_report", Checks: map[string]string{
		"rpg_topology_integrity_report_state_check":    "state IN ('passed', 'failed')",
		"rpg_topology_integrity_report_count_check":    "issue_count >= 0",
		"rpg_topology_integrity_report_duration_check": "duration_milliseconds >= 0",
	}}}
}
