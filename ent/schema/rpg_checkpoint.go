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

// RpgCheckpoint 定义可设置和恢复的稳定 RPG Checkpoint 资料。
type RpgCheckpoint struct {
	ent.Schema
}

// Fields 返回 Checkpoint 的地点、规则和生命周期字段。
func (RpgCheckpoint) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("Checkpoint 的稳定 Identifier。"),
		field.Int64("location_id").GoType(snowflake.ID(0)).Positive().Comment("Checkpoint 所属 Location 的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("地点内唯一的 Checkpoint 机器编码。"),
		field.String("name").MaxLen(120).Comment("Checkpoint 面向玩家的简体中文名称。"),
		field.String("description").Optional().Nillable().MaxLen(4000).Comment("Checkpoint 的可选简体中文说明。"),
		field.JSON("set_condition", json.RawMessage{}).Optional().Comment("规范化的可设置条件 JSON 对象。"),
		field.JSON("recovery_condition", json.RawMessage{}).Optional().Comment("规范化的恢复条件 JSON 对象。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("Checkpoint 是否可被新命令选择。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("Checkpoint 的正整数乐观版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("Checkpoint 首次创建时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("Checkpoint 最近更新时间。"),
	}
}

// Indexes 返回地点内 Checkpoint 编码唯一约束。
func (RpgCheckpoint) Indexes() []ent.Index {
	return []ent.Index{index.Fields("location_id", "code").Unique().StorageKey("uk_rpg_checkpoint_location_id_code")}
}

// Annotations 固定 Checkpoint 表名和规则 JSON 约束。
func (RpgCheckpoint) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("RPG 世界中声明可设置和恢复条件的稳定 Checkpoint 资料。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_checkpoint", Checks: map[string]string{
			"rpg_checkpoint_code_check":               "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_checkpoint_name_check":               "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_checkpoint_set_condition_check":      "set_condition IS NULL OR jsonb_typeof(set_condition) = 'object'::text",
			"rpg_checkpoint_recovery_condition_check": "recovery_condition IS NULL OR jsonb_typeof(recovery_condition) = 'object'::text",
			"rpg_checkpoint_time_check":               "updated_at >= created_at",
			"rpg_checkpoint_version_check":            "version > 0",
		}},
	}
}
