package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterPosition 保存角色当前位置以及移动序列的并发事实。
type PlayerCharacterPosition struct{ ent.Schema }

// Fields 返回角色位置的显式字段。
func (PlayerCharacterPosition) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("位置记录的稳定 Snowflake Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("所属 PlayerCharacter 的稳定 Identifier。"),
		field.Int64("location_id").GoType(snowflake.ID(0)).Positive().Comment("当前位置 Location 的稳定 Identifier。"),
		field.Int64("move_sequence").Default(0).Comment("角色单调递增的成功移动序号。"),
		field.Int64("version").Default(1).Comment("位置记录的乐观并发版本。"),
		field.Time("updated_at").Comment("位置事实最近更新时间。"),
	}
}

// Indexes 保证每个角色只有一条当前位置事实。
func (PlayerCharacterPosition) Indexes() []ent.Index {
	return []ent.Index{index.Fields("player_character_id").Unique().StorageKey("uk_player_character_position_player_character_id")}
}

// Annotations 固定表名及单调字段约束。
func (PlayerCharacterPosition) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("PlayerCharacter 的当前位置、移动序号和乐观版本。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_position", Checks: map[string]string{
		"player_character_position_move_sequence_check": "move_sequence >= 0",
		"player_character_position_version_check":       "version > 0",
	}}}
}
