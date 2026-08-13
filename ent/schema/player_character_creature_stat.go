package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterCreatureStat 定义 player_character_creature_stat 表的持久化结构。
type PlayerCharacterCreatureStat struct {
	ent.Schema
}

// Fields 返回 player_character_creature_stat 表全部字段及其数据库约束。
func (PlayerCharacterCreatureStat) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("player_character_creature_id").GoType(snowflake.ID(0)).Positive().Comment("玩家拥有 Creature 培养能力引用的玩家拥有 Creature 实例稳定 Identifier。"),
		field.Int64("stat_id").GoType(snowflake.ID(0)).Positive().Comment("玩家拥有 Creature 培养能力引用的能力项资料稳定 Identifier。"),
		field.Int16("individual_value").Comment("玩家拥有 Creature 培养能力零至三十一的个体值。"),
		field.Int16("effort_value").Comment("玩家拥有 Creature 培养能力零至二百五十二的已训练努力值。"),
	}
}

// Indexes 返回 player_character_creature_stat 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterCreatureStat) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_creature_id", "stat_id").Unique().StorageKey("uk_player_character_creature_stat_player_character_cre_ba2d923d"),
	}
}

// Annotations 固定 player_character_creature_stat 的表名、注释、复合主键和检查约束。
func (PlayerCharacterCreatureStat) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 player_character_creature_stat 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_creature_stat", Checks: map[string]string{
			"player_character_creature_stat_ev_check": "effort_value >= 0 AND effort_value <= 252",
			"player_character_creature_stat_iv_check": "individual_value >= 0 AND individual_value <= 31",
		}},
	}
}
