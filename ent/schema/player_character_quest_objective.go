package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterQuestObjective 定义 player_character_quest_objective 表的持久化结构。
type PlayerCharacterQuestObjective struct {
	ent.Schema
}

// Fields 返回 player_character_quest_objective 表全部字段及其数据库约束。
func (PlayerCharacterQuestObjective) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("玩家任务目标进度所属 PlayerCharacter 的稳定 Identifier。"),
		field.Int64("quest_id").GoType(snowflake.ID(0)).Positive().Comment("玩家任务目标进度所属或引用的任务稳定 Identifier。"),
		field.Int64("objective_id").GoType(snowflake.ID(0)).Positive().Comment("玩家任务目标进度引用的任务目标稳定 Identifier。"),
		field.Int32("current_count").Comment("玩家任务目标进度已经累计的非负目标次数。"),
		field.Time("completed_at").Optional().Nillable().Comment("玩家任务目标进度最近完成时的可选 UTC 时间。"),
	}
}

// Indexes 返回 player_character_quest_objective 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterQuestObjective) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_id", "quest_id", "objective_id").Unique().StorageKey("uk_player_character_quest_objective_player_character_i_ecd2956c"),
	}
}

// Annotations 固定 player_character_quest_objective 的表名、注释、复合主键和检查约束。
func (PlayerCharacterQuestObjective) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 对单个任务目标的当前进度。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_quest_objective", Checks: map[string]string{
			"player_character_quest_objective_count_check": "current_count >= 0",
		}},
	}
}
