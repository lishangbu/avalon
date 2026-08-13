package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgQuestReward 定义 rpg_quest_reward 表的持久化结构。
type RpgQuestReward struct {
	ent.Schema
}

// Fields 返回 rpg_quest_reward 表全部字段及其数据库约束。
func (RpgQuestReward) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 任务奖励记录的稳定 Identifier。"),
		field.Int64("quest_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 任务奖励所属或引用的任务稳定 Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 任务奖励引用的道具稳定 Identifier。"),
		field.Int64("currency_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 任务奖励使用或变更的游戏货币稳定 Identifier。"),
		field.Int64("creature_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 任务奖励引用的 Creature 资料稳定 Identifier。"),
		field.Int64("quantity").Comment("RPG 任务奖励保存的非负或正整数道具数量。"),
	}
}

// Annotations 固定 rpg_quest_reward 的表名、注释、复合主键和检查约束。
func (RpgQuestReward) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("任务完成后授予的一种道具、货币或 Creature 奖励。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_quest_reward", Checks: map[string]string{
			"rpg_quest_reward_quantity_check": "quantity > 0",
			"rpg_quest_reward_target_check":   "num_nonnulls(item_id, currency_id, creature_id) = 1",
		}},
	}
}
