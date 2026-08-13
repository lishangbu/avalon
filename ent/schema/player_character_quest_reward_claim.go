package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/edge"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterQuestRewardClaim 保存一个任务完成轮次的一次性奖励领取事实。
type PlayerCharacterQuestRewardClaim struct{ ent.Schema }

// Fields 返回领取归属、完成轮次和共同操作身份。
func (PlayerCharacterQuestRewardClaim) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("任务奖励领取事实的稳定 Snowflake Identifier，同时作为资产获取来源引用。"),
		field.Int64("operation_id").GoType(snowflake.ID(0)).Positive().Comment("本轮全部奖励流水与 Outbox 共同使用的 Operation Identifier。"),
		field.Int64("player_character_quest_id").GoType(snowflake.ID(0)).Positive().Comment("被领取奖励的 PlayerCharacter Quest 进度 Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("领取本轮奖励的 PlayerCharacter Identifier。"),
		field.Int64("quest_id").GoType(snowflake.ID(0)).Positive().Comment("领取时冻结的 Quest Identifier。"),
		field.Int32("completion_count").Positive().Comment("本次领取对应的正整数任务完成轮次。"),
		field.Time("claimed_at").Comment("奖励事务提交的 UTC 时间。"),
	}
}

// Edges 返回领取事实与任务进度、角色和任务资料的权威关系。
func (PlayerCharacterQuestRewardClaim) Edges() []ent.Edge {
	return []ent.Edge{
		edge.To("player_character_quest", PlayerCharacterQuest.Type).Field("player_character_quest_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_quest_reward_claim_player_character_quest_id_id")),
		edge.To("player_character", PlayerCharacter.Type).Field("player_character_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_quest_reward_claim_player_character_id_id")),
		edge.To("quest", RpgQuest.Type).Field("quest_id").Unique().Required().StorageKey(edge.Symbol("fk_player_character_quest_reward_claim_quest_id_id")),
	}
}

// Indexes 保证同一任务进度的同一完成轮次只能领取一次。
func (PlayerCharacterQuestRewardClaim) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_quest_id", "completion_count").Unique().StorageKey("uk_player_character_quest_reward_claim_player_character_quest_id_completion_count"),
		index.Fields("operation_id").Unique().StorageKey("uk_player_character_quest_reward_claim_operation_id"),
	}
}

// Annotations 固定任务奖励领取表名与说明。
func (PlayerCharacterQuestRewardClaim) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("PlayerCharacter 对一个 Quest 完成轮次的一次性不可变奖励领取事实。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_quest_reward_claim"}}
}
