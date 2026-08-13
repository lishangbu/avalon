package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterCheckpoint 定义 player_character_checkpoint 表的持久化结构。
type PlayerCharacterCheckpoint struct {
	ent.Schema
}

// Fields 返回 player_character_checkpoint 表全部字段及其数据库约束。
func (PlayerCharacterCheckpoint) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("玩家检查点绑定的稳定 Snowflake Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("检查点所属 PlayerCharacter 的稳定 Identifier。"),
		field.Int64("checkpoint_id").GoType(snowflake.ID(0)).Positive().Comment("当前选择的 RPG Checkpoint 稳定 Identifier。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("玩家检查点写入使用的正整数乐观并发版本。"),
		field.Time("updated_at").Comment("玩家检查点最近一次业务更新的 UTC 时间。"),
	}
}

// Indexes 保证每个 PlayerCharacter 只有一个当前恢复点。
func (PlayerCharacterCheckpoint) Indexes() []ent.Index {
	return []ent.Index{index.Fields("player_character_id").Unique().StorageKey("uk_player_character_checkpoint_player_character_id")}
}

// Annotations 固定 player_character_checkpoint 的表名、注释、复合主键和检查约束。
func (PlayerCharacterCheckpoint) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 当前唯一的恢复地点和检查点编码。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_checkpoint", Checks: map[string]string{
			"player_character_checkpoint_version_check": "version > 0",
		}},
	}
}
