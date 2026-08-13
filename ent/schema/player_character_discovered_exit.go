package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterDiscoveredExit 保存玩家单调增长的出口发现事实。
type PlayerCharacterDiscoveredExit struct{ ent.Schema }

// Fields 返回出口发现记录字段。
func (PlayerCharacterDiscoveredExit) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("发现出口的 PlayerCharacter 稳定 Identifier。"),
		field.Int64("location_exit_id").GoType(snowflake.ID(0)).Positive().Comment("被发现有向出口的稳定 Identifier。"),
		field.Time("discovered_at").Comment("首次发现时间；记录不可回退。"),
	}
}

// Indexes 保证同一角色不会重复记录同一出口。
func (PlayerCharacterDiscoveredExit) Indexes() []ent.Index {
	return []ent.Index{index.Fields("player_character_id", "location_exit_id").Unique().StorageKey("uk_player_character_discovered_exit_player_character_id_location_exit_id")}
}

// Annotations 固定发现表名。
func (PlayerCharacterDiscoveredExit) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("PlayerCharacter 已发现的有向 Location Exit 单调事实。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_discovered_exit"}}
}
