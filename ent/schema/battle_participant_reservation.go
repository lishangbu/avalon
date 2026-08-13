package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// BattleParticipantReservation 定义 Battle 期间 PlayerCharacter 的唯一占用事实。
type BattleParticipantReservation struct {
	ent.Schema
}

// Fields 返回 battle_participant_reservation 表全部字段及其数据库约束。
func (BattleParticipantReservation) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).StorageKey("player_character_id").Comment("被 Battle 独占的 PlayerCharacter Identifier。"),
		field.Int64("battle_id").GoType(snowflake.ID(0)).Positive().Comment("占用该 PlayerCharacter 的 Battle Identifier。"),
		field.Time("created_at").Comment("占用事实创建的 UTC 时间。"),
	}
}

// Annotations 固定 battle_participant_reservation 的表名和注释。
func (BattleParticipantReservation) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("未终局 Battle 对真人 PlayerCharacter 的唯一占用事实。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_participant_reservation"},
	}
}
