package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterDisplayNameHistory 定义 player_character_display_name_history 表的持久化结构。
type PlayerCharacterDisplayNameHistory struct {
	ent.Schema
}

// Fields 返回 player_character_display_name_history 表全部字段及其数据库约束。
func (PlayerCharacterDisplayNameHistory) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("player_character_display_name_history 表的雪花主键。"),
		field.String("display_name_key").MaxLen(64).Comment("player_character_display_name_history 表的规范化展示名称键。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("player_character_display_name_history 表的 player_character_id 字段。"),
		field.String("display_name").MaxLen(64).Comment("player_character_display_name_history 表的 display_name 字段。"),
		field.Time("claimed_at").Comment("player_character_display_name_history 表的 claimed_at 字段。"),
		field.Time("retired_at").Optional().Nillable().Comment("player_character_display_name_history 表的 retired_at 字段。"),
	}
}

// Indexes 返回规范化展示名称键的唯一索引。
func (PlayerCharacterDisplayNameHistory) Indexes() []ent.Index {
	return []ent.Index{index.Fields("display_name_key").Unique().StorageKey("uk_player_character_display_name_history_display_name_key")}
}

// Annotations 固定 player_character_display_name_history 的表名、注释、复合主键和检查约束。
func (PlayerCharacterDisplayNameHistory) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 player_character_display_name_history 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_display_name_history", Checks: map[string]string{
			"player_character_display_name_history_check": "retired_at IS NULL OR retired_at >= claimed_at",
		}},
	}
}
