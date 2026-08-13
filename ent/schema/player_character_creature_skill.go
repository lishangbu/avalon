package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterCreatureSkill 定义 player_character_creature_skill 表的持久化结构。
type PlayerCharacterCreatureSkill struct {
	ent.Schema
}

// Fields 返回 player_character_creature_skill 表全部字段及其数据库约束。
func (PlayerCharacterCreatureSkill) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("player_character_creature_id").GoType(snowflake.ID(0)).Positive().Comment("玩家拥有 Creature 技能引用的玩家拥有 Creature 实例稳定 Identifier。"),
		field.Int16("position").Comment("玩家拥有 Creature 技能中从一开始的固定顺序位置。"),
		field.Int64("skill_id").GoType(snowflake.ID(0)).Positive().Comment("玩家拥有 Creature 技能引用的技能资料稳定 Identifier。"),
		field.Int32("current_pp").Comment("玩家拥有 Creature 技能当前剩余的非负技能使用次数。"),
	}
}

// Indexes 返回 player_character_creature_skill 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterCreatureSkill) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_creature_id", "position").Unique().StorageKey("uk_player_character_creature_skill_player_character_cr_ac6f169e"),
	}
}

// Annotations 固定 player_character_creature_skill 的表名、注释、复合主键和检查约束。
func (PlayerCharacterCreatureSkill) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 player_character_creature_skill 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_creature_skill", Checks: map[string]string{
			"player_character_creature_skill_position_check": "\"position\" >= 1 AND \"position\" <= 4",
			"player_character_creature_skill_pp_check":       "current_pp >= 0",
		}},
	}
}
