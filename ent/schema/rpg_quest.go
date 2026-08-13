package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgQuest 定义 rpg_quest 表的持久化结构。
type RpgQuest struct {
	ent.Schema
}

// Fields 返回 rpg_quest 表全部字段及其数据库约束。
func (RpgQuest) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 任务记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("RPG 任务的全局唯一英文机器编码。"),
		field.String("name").MaxLen(120).Comment("RPG 任务的简体中文展示名称。"),
		field.String("quest_type").MaxLen(16).Comment("RPG 任务所属的主线、支线、每日或职业任务类型。"),
		field.Int64("start_npc_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 任务可选的任务发放 NPC 稳定 Identifier。"),
		field.Int64("turn_in_npc_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 任务可选的任务交付 NPC 稳定 Identifier。"),
		field.Int64("prerequisite_quest_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 任务开始前必须完成的可选前置任务稳定 Identifier。"),
		field.String("description").Comment("RPG 任务面向玩家或管理者的可选简体中文说明。"),
		field.Bool("repeatable").Annotations(entsql.DefaultExpr("false")).Comment("RPG 任务完成后是否允许重新开始。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 任务是否可被新的 RPG 进度引用。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("RPG 任务写入使用的正整数乐观并发版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 任务首次创建的 UTC 时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 任务最近一次业务更新的 UTC 时间。"),
	}
}

// Annotations 固定 rpg_quest 的表名、注释、复合主键和检查约束。
func (RpgQuest) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("主线、支线、每日或职业任务的稳定定义。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_quest", Checks: map[string]string{
			"rpg_quest_code_check":         "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_quest_description_check":  "char_length(description) >= 1 AND char_length(description) <= 4000 AND description = btrim(description)",
			"rpg_quest_name_check":         "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_quest_prerequisite_check": "prerequisite_quest_id IS NULL OR prerequisite_quest_id <> id",
			"rpg_quest_time_check":         "updated_at >= created_at",
			"rpg_quest_type_check":         "quest_type::text = ANY (ARRAY['main'::character varying, 'side'::character varying, 'daily'::character varying, 'profession'::character varying]::text[])",
			"rpg_quest_version_check":      "version > 0",
		}},
	}
}
