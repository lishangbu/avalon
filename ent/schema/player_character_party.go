package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterParty 是探索和遭遇使用的玩家 Party 根聚合。
type PlayerCharacterParty struct{ ent.Schema }

// Fields 返回 Party 的稳定身份和版本字段。
func (PlayerCharacterParty) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("Party 所属 PlayerCharacter 稳定 Identifier。"),
		field.String("name").MaxLen(120).Comment("Party 的简体中文名称。"),
		field.Int64("version").Default(1).Comment("Party 的乐观并发版本。"),
		field.Time("created_at").Comment("Party 创建时间。"),
		field.Time("updated_at").Comment("Party 最近更新时间。"),
	}
}

// Indexes 保证每个角色只有一个当前 Party。
func (PlayerCharacterParty) Indexes() []ent.Index {
	return []ent.Index{index.Fields("player_character_id").Unique().StorageKey("uk_player_character_party_player_character_id")}
}

// Annotations 固定 Party 的名称和版本约束。
func (PlayerCharacterParty) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("用于 RPG 探索和遭遇的 PlayerCharacter Party。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_party", Checks: map[string]string{
		"player_character_party_name_check":    "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
		"player_character_party_version_check": "version > 0",
	}}}
}
