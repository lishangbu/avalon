package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgDialogue 定义 rpg_dialogue 表的持久化结构。
type RpgDialogue struct {
	ent.Schema
}

// Fields 返回 rpg_dialogue 表全部字段及其数据库约束。
func (RpgDialogue) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 对话记录的稳定 Identifier。"),
		field.Int64("npc_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 对话关联的可选 NPC 稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("RPG 对话的全局唯一英文机器编码。"),
		field.String("name").MaxLen(120).Comment("RPG 对话的简体中文展示名称。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 对话是否可被新的 RPG 进度引用。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("RPG 对话写入使用的正整数乐观并发版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 对话首次创建的 UTC 时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 对话最近一次业务更新的 UTC 时间。"),
	}
}

// Annotations 固定 rpg_dialogue 的表名、注释、复合主键和检查约束。
func (RpgDialogue) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("由一个 NPC 发起的有序简体中文对话定义。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_dialogue", Checks: map[string]string{
			"rpg_dialogue_code_check":    "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_dialogue_name_check":    "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_dialogue_time_check":    "updated_at >= created_at",
			"rpg_dialogue_version_check": "version > 0",
		}},
	}
}
