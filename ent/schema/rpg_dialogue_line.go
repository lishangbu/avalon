package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgDialogueLine 定义 rpg_dialogue_line 表的持久化结构。
type RpgDialogueLine struct {
	ent.Schema
}

// Fields 返回 rpg_dialogue_line 表全部字段及其数据库约束。
func (RpgDialogueLine) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("dialogue_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 对话行所属对话稳定 Identifier。"),
		field.Int32("position").Comment("RPG 对话行中从一开始的固定顺序位置。"),
		field.String("speaker_name").MaxLen(120).Comment("RPG 对话行面向玩家展示的发言者名称。"),
		field.String("content").Comment("RPG 对话行面向玩家展示的完整简体中文正文。"),
	}
}

// Indexes 返回 rpg_dialogue_line 原复合主键对应的稳定业务唯一约束。
func (RpgDialogueLine) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("dialogue_id", "position").Unique().StorageKey("uk_rpg_dialogue_line_dialogue_id_position"),
	}
}

// Annotations 固定 rpg_dialogue_line 的表名、注释、复合主键和检查约束。
func (RpgDialogueLine) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("对话中按固定位置排列的一句发言。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_dialogue_line", Checks: map[string]string{
			"rpg_dialogue_line_content_check":  "char_length(content) >= 1 AND char_length(content) <= 8000 AND content = btrim(content)",
			"rpg_dialogue_line_position_check": "\"position\" > 0",
			"rpg_dialogue_line_speaker_check":  "char_length(speaker_name::text) >= 1 AND char_length(speaker_name::text) <= 120 AND speaker_name::text = btrim(speaker_name::text)",
		}},
	}
}
