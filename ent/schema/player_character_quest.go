package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterQuest 定义 player_character_quest 表的持久化结构。
type PlayerCharacterQuest struct {
	ent.Schema
}

// Fields 返回 player_character_quest 表全部字段及其数据库约束。
func (PlayerCharacterQuest) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("玩家任务进度所属 PlayerCharacter 的稳定 Identifier。"),
		field.Int64("quest_id").GoType(snowflake.ID(0)).Positive().Comment("玩家任务进度所属或引用的任务稳定 Identifier。"),
		field.String("status").MaxLen(16).Comment("玩家任务进度当前的进行中、完成、失败或放弃状态。"),
		field.Time("started_at").Comment("玩家任务进度首次或本轮开始的 UTC 时间。"),
		field.Time("completed_at").Optional().Nillable().Comment("玩家任务进度最近完成时的可选 UTC 时间。"),
		field.Int32("completion_count").Annotations(entsql.DefaultExpr("0")).Comment("玩家任务进度已经完整完成的非负次数。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("玩家任务进度写入使用的正整数乐观并发版本。"),
	}
}

// Indexes 返回 player_character_quest 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterQuest) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_id", "quest_id").Unique().StorageKey("uk_player_character_quest_player_character_id_quest_id"),
	}
}

// Annotations 固定 player_character_quest 的表名、注释、复合主键和检查约束。
func (PlayerCharacterQuest) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 对任务的生命周期与重复完成次数。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_quest", Checks: map[string]string{
			"player_character_quest_completion_check": "completion_count >= 0 AND (completed_at IS NULL OR completed_at >= started_at)",
			"player_character_quest_status_check":     "status::text = ANY (ARRAY['active'::character varying, 'completed'::character varying, 'failed'::character varying, 'abandoned'::character varying]::text[])",
			"player_character_quest_version_check":    "version > 0",
		}},
	}
}
