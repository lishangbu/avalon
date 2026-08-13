package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterTeamMemberStat 定义 player_character_team_member_stat 表的持久化结构。
type PlayerCharacterTeamMemberStat struct {
	ent.Schema
}

// Fields 返回 player_character_team_member_stat 表全部字段及其数据库约束。
func (PlayerCharacterTeamMemberStat) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("team_id").GoType(snowflake.ID(0)).Positive().Comment("所属 Team 的稳定 Identifier。"),
		field.Int16("member_position").Comment("培养数值所属成员在 Team 中的固定位置。"),
		field.Int64("stat_id").GoType(snowflake.ID(0)).Positive().Comment("培养数值对应战斗属性资料的稳定 Identifier。"),
		field.Int16("individual_value").Comment("该属性零至三十一范围内的个体值。"),
		field.Int16("effort_value").Comment("该属性零至二百五十二范围内的努力值。"),
	}
}

// Indexes 返回 player_character_team_member_stat 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterTeamMemberStat) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("team_id", "member_position", "stat_id").Unique().StorageKey("uk_player_character_team_member_stat_team_id_member_po_54d44bb9"),
	}
}

// Annotations 固定 player_character_team_member_stat 的表名、注释、复合主键和检查约束。
func (PlayerCharacterTeamMemberStat) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("Team 成员的个体值与努力值资料引用。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_team_member_stat", Checks: map[string]string{
			"player_character_team_member_stat_effort_value_check":     "effort_value >= 0 AND effort_value <= 252",
			"player_character_team_member_stat_individual_value_check": "individual_value >= 0 AND individual_value <= 31",
		}},
	}
}
