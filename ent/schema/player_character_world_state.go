package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterWorldState 定义 player_character_world_state 表的持久化结构。
type PlayerCharacterWorldState struct {
	ent.Schema
}

// Fields 返回 player_character_world_state 表全部字段及其数据库约束。
func (PlayerCharacterWorldState) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("玩家世界状态所属 PlayerCharacter 的稳定 Identifier。"),
		field.String("state_key").MaxLen(120).Comment("玩家世界状态在单个 PlayerCharacter 内唯一的层级机器键。"),
		field.Int64("integer_value").Optional().Nillable().Comment("玩家世界状态可选的整数取值。"),
		field.String("text_value").Optional().Nillable().Comment("玩家世界状态可选的非空文本取值。"),
		field.Bool("boolean_value").Optional().Nillable().Comment("玩家世界状态可选的布尔取值。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("玩家世界状态写入使用的正整数乐观并发版本。"),
		field.Time("updated_at").Comment("玩家世界状态最近一次业务更新的 UTC 时间。"),
	}
}

// Indexes 返回 player_character_world_state 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterWorldState) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_id", "state_key").Unique().StorageKey("uk_player_character_world_state_player_character_id_state_key"),
	}
}

// Annotations 固定 player_character_world_state 的表名、注释、复合主键和检查约束。
func (PlayerCharacterWorldState) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 私有世界状态中恰好一种强类型标量值。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_world_state", Checks: map[string]string{
			"player_character_world_state_key_check":     "state_key::text ~ '^[a-z][a-z0-9.-]{1,119}$'::text",
			"player_character_world_state_text_check":    "text_value IS NULL OR char_length(text_value) >= 1 AND char_length(text_value) <= 4000 AND text_value = btrim(text_value)",
			"player_character_world_state_value_check":   "num_nonnulls(integer_value, text_value, boolean_value) = 1",
			"player_character_world_state_version_check": "version > 0",
		}},
	}
}
