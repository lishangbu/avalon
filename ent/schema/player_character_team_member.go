package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterTeamMember 定义 player_character_team_member 表的持久化结构。
type PlayerCharacterTeamMember struct {
	ent.Schema
}

// Fields 返回 player_character_team_member 表全部字段及其数据库约束。
func (PlayerCharacterTeamMember) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("team_id").GoType(snowflake.ID(0)).Positive().Comment("所属 Team 的稳定 Identifier。"),
		field.Int16("position").Comment("成员在 Team 中从一开始的固定位置。"),
		field.Int64("creature_id").GoType(snowflake.ID(0)).Positive().Comment("成员选择的生物资料稳定 Identifier。"),
		field.Int64("form_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("成员可选形态资料的稳定 Identifier。"),
		field.Int64("gender_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("成员可选性别资料的稳定 Identifier。"),
		field.Int64("skin_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("成员可选皮肤资料的稳定 Identifier。"),
		field.Int64("ability_id").GoType(snowflake.ID(0)).Positive().Comment("成员选择的特性资料稳定 Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("成员可选持有道具资料的稳定 Identifier。"),
		field.Int64("tera_element_id").GoType(snowflake.ID(0)).Positive().Comment("成员太晶属性资料的稳定 Identifier。"),
		field.Int64("nature_id").GoType(snowflake.ID(0)).Positive().Comment("成员选择的 Nature 实时资料稳定 Identifier。"),
		field.Int16("level").Comment("成员在保留等级赛制下使用的一至一百的固定等级。"),
	}
}

// Indexes 返回 player_character_team_member 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterTeamMember) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("team_id", "position").Unique().StorageKey("uk_player_character_team_member_team_id_position"),
	}
}

// Annotations 固定 player_character_team_member 的表名、注释、复合主键和检查约束。
func (PlayerCharacterTeamMember) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("Team 中按固定位置保存的参战成员。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_team_member", Checks: map[string]string{
			"player_character_team_member_level_check":    "level >= 1 AND level <= 100",
			"player_character_team_member_position_check": "\"position\" >= 1 AND \"position\" <= 6",
		}},
	}
}
