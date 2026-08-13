package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgQuestObjective 定义 rpg_quest_objective 表的持久化结构。
type RpgQuestObjective struct {
	ent.Schema
}

// Fields 返回 rpg_quest_objective 表全部字段及其数据库约束。
func (RpgQuestObjective) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 任务目标记录的稳定 Identifier。"),
		field.Int64("quest_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 任务目标所属或引用的任务稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("任务内唯一的稳定英文机器编码。"),
		field.Int16("position").Comment("RPG 任务目标中从一开始的固定顺序位置。"),
		field.String("objective_type").MaxLen(32).Comment("RPG 任务目标采用的收集、捕获、击败、交谈、探索、战斗或制作类型。"),
		field.Int64("target_creature_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 任务目标目标 Creature 的可选稳定 Identifier。"),
		field.Int64("target_item_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 任务目标目标道具的可选稳定 Identifier。"),
		field.Int64("target_location_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 任务目标目标地点的可选稳定 Identifier。"),
		field.Int64("target_npc_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 任务目标目标 NPC 的可选稳定 Identifier。"),
		field.Int32("required_count").Comment("RPG 任务目标判定完成所需的正整数累计次数。"),
		field.String("description").Comment("RPG 任务目标面向玩家或管理者的可选简体中文说明。"),
	}
}

// Indexes 保证同一 Quest 内目标 Stable Code 唯一。
func (RpgQuestObjective) Indexes() []ent.Index {
	return []ent.Index{index.Fields("quest_id", "code").Unique().StorageKey("uk_rpg_quest_objective_quest_id_code")}
}

// Annotations 固定 rpg_quest_objective 的表名、注释、复合主键和检查约束。
func (RpgQuestObjective) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("任务中按顺序维护的结构化进度目标。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_quest_objective", Checks: map[string]string{
			"rpg_quest_objective_description_check":    "char_length(description) >= 1 AND char_length(description) <= 1000 AND description = btrim(description)",
			"rpg_quest_objective_code_check":           "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_quest_objective_position_check":       "\"position\" > 0",
			"rpg_quest_objective_required_count_check": "required_count > 0",
			"rpg_quest_objective_target_check":         "num_nonnulls(target_creature_id, target_item_id, target_location_id, target_npc_id) <= 1",
			"rpg_quest_objective_type_check":           "objective_type::text = ANY (ARRAY['collect'::character varying, 'capture'::character varying, 'defeat'::character varying, 'talk'::character varying, 'explore'::character varying, 'battle'::character varying, 'craft'::character varying]::text[])",
		}},
	}
}
