package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterEncounterUsage 保存遭遇表冷却和次数使用事实。
type PlayerCharacterEncounterUsage struct{ ent.Schema }

// Fields 返回遭遇使用字段。
func (PlayerCharacterEncounterUsage) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("使用遭遇表的 PlayerCharacter 稳定 Identifier。"),
		field.Int64("encounter_table_id").GoType(snowflake.ID(0)).Positive().Comment("遭遇表稳定 Identifier。"),
		field.Int32("use_count").Default(0).Comment("已使用次数。"),
		field.Int64("last_trigger_move_sequence").Optional().Nillable().Comment("最近一次触发遭遇时的位置移动序号。"),
		field.Time("cooldown_until").Optional().Nillable().Comment("下一次允许抽样的 UTC 时间。"),
		field.Time("last_used_at").Optional().Nillable().Comment("最近一次使用 UTC 时间。"),
		field.Int64("version").Default(1).Comment("使用事实的乐观并发版本。"),
	}
}

// Indexes 保证每个角色和遭遇表只有一条使用事实。
func (PlayerCharacterEncounterUsage) Indexes() []ent.Index {
	return []ent.Index{index.Fields("player_character_id", "encounter_table_id").Unique().StorageKey("uk_player_character_encounter_usage_player_character_id_encounter_table_id")}
}

// Annotations 固定次数和版本约束。
func (PlayerCharacterEncounterUsage) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("PlayerCharacter 对遭遇表的冷却、次数和版本事实。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_encounter_usage", Checks: map[string]string{
		"player_character_encounter_usage_count_check":    "use_count >= 0",
		"player_character_encounter_usage_sequence_check": "last_trigger_move_sequence IS NULL OR last_trigger_move_sequence >= 0",
		"player_character_encounter_usage_version_check":  "version > 0",
	}}}
}
