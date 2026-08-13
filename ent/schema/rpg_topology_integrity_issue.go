package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgTopologyIntegrityIssue 保存报告中的一个稳定问题明细。
type RpgTopologyIntegrityIssue struct{ ent.Schema }

// Fields 返回问题代码、资源路径和中文说明。
func (RpgTopologyIntegrityIssue) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()),
		field.Int64("report_id").GoType(snowflake.ID(0)).Positive().Comment("所属完整性报告稳定 Identifier。"),
		field.Int32("position").Comment("报告内从一开始的稳定顺序。"),
		field.String("reason_code").MaxLen(80).Comment("机器可读稳定原因码。"),
		field.String("resource_type").MaxLen(40).Comment("发生问题的资源类型。"),
		field.Int64("resource_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("可选问题资源 Identifier。"),
		field.String("reference_path").MaxLen(1000).Optional().Nillable().Comment("可选引用路径。"),
		field.String("message").MaxLen(1000).Comment("管理员可读简体中文说明。"),
	}
}

// Indexes 保证报告内问题位置唯一。
func (RpgTopologyIntegrityIssue) Indexes() []ent.Index {
	return []ent.Index{index.Fields("report_id", "position").Unique().StorageKey("uk_rpg_topology_integrity_issue_report_id_position")}
}

// Annotations 固定原因码和位置约束。
func (RpgTopologyIntegrityIssue) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("RPG 拓扑校验报告的稳定问题明细。"), entsql.WithComments(true), entsql.Annotation{Table: "rpg_topology_integrity_issue", Checks: map[string]string{
		"rpg_topology_integrity_issue_position_check": "position >= 1",
		"rpg_topology_integrity_issue_reason_check":   "reason_code::text ~ '^[a-z][a-z0-9_]{1,79}$'::text",
	}}}
}
