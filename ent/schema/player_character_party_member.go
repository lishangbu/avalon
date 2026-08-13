package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterPartyMember 定义 player_character_party_member 表的持久化结构。
type PlayerCharacterPartyMember struct {
	ent.Schema
}

// Fields 返回 player_character_party_member 表全部字段及其数据库约束。
func (PlayerCharacterPartyMember) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("party_id").GoType(snowflake.ID(0)).Positive().Comment("所属 RPG Party 的稳定 Identifier。"),
		field.Int16("position").Comment("玩家当前队伍成员中从一开始的固定顺序位置。"),
		field.Int64("player_character_creature_id").GoType(snowflake.ID(0)).Positive().Comment("玩家当前队伍成员引用的玩家拥有 Creature 实例稳定 Identifier。"),
	}
}

// Indexes 返回 player_character_party_member 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterPartyMember) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("party_id", "position").Unique().StorageKey("uk_player_character_party_member_party_id_position"),
	}
}

// Annotations 固定 player_character_party_member 的表名、注释、复合主键和检查约束。
func (PlayerCharacterPartyMember) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 player_character_party_member 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_party_member", Checks: map[string]string{
			"player_character_party_member_position_check": "\"position\" >= 1 AND \"position\" <= 6",
		}},
	}
}
