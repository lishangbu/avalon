package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterTeamMemberSkill 定义 player_character_team_member_skill 表的持久化结构。
type PlayerCharacterTeamMemberSkill struct {
	ent.Schema
}

// Fields 返回 player_character_team_member_skill 表全部字段及其数据库约束。
func (PlayerCharacterTeamMemberSkill) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("team_id").GoType(snowflake.ID(0)).Positive().Comment("所属 Team 的稳定 Identifier。"),
		field.Int16("member_position").Comment("技能所属成员在 Team 中的固定位置。"),
		field.Int16("position").Comment("技能在该成员技能栏中从一开始的固定位置。"),
		field.Int64("skill_id").GoType(snowflake.ID(0)).Positive().Comment("成员选择的技能资料稳定 Identifier。"),
	}
}

// Indexes 返回 player_character_team_member_skill 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterTeamMemberSkill) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("team_id", "member_position", "position").Unique().StorageKey("uk_player_character_team_member_skill_team_id_member_p_99de905c"),
	}
}

// Annotations 固定 player_character_team_member_skill 的表名、注释、复合主键和检查约束。
func (PlayerCharacterTeamMemberSkill) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("Team 成员技能栏的固定位置资料引用。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_team_member_skill", Checks: map[string]string{
			"player_character_team_member_skill_position_check": "\"position\" >= 1 AND \"position\" <= 4",
		}},
	}
}
