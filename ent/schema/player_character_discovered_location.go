package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterDiscoveredLocation 保存玩家单调增长的地点发现事实。
type PlayerCharacterDiscoveredLocation struct{ ent.Schema }

// Fields 返回地点发现记录字段。
func (PlayerCharacterDiscoveredLocation) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("发现地点的 PlayerCharacter 稳定 Identifier。"),
		field.Int64("location_id").GoType(snowflake.ID(0)).Positive().Comment("被发现 Location 的稳定 Identifier。"),
		field.String("source").MaxLen(24).Comment("发现来源，例如 traversal 或 checkpoint。"),
		field.Time("discovered_at").Comment("首次发现时间；记录不可回退。"),
	}
}

// Indexes 保证同一角色不会重复记录同一地点。
func (PlayerCharacterDiscoveredLocation) Indexes() []ent.Index {
	return []ent.Index{index.Fields("player_character_id", "location_id").Unique().StorageKey("uk_player_character_discovered_location_player_character_id_location_id")}
}

// Annotations 固定发现来源约束。
func (PlayerCharacterDiscoveredLocation) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("PlayerCharacter 已发现的 Location 单调事实。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_discovered_location", Checks: map[string]string{
		"player_character_discovered_location_source_check": "source IN ('traversal', 'checkpoint', 'admin')",
	}}}
}
