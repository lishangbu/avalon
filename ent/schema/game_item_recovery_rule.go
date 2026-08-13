package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemRecoveryRule 定义道具回合末恢复与伤害规则。
type GameItemRecoveryRule struct{ ent.Schema }

func (GameItemRecoveryRule) Fields() []ent.Field {
	return []ent.Field{field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()), field.Int64("item_id").GoType(snowflake.ID(0)).Positive(), field.Int32("end_turn_heal_denominator"), field.Int32("end_turn_damage_denominator"), field.Int32("end_turn_heal_for_element_denominator"), field.Int64("end_turn_heal_for_element_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable(), field.Int32("end_turn_damage_without_element_denominator"), field.Int64("end_turn_damage_without_element_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable(), field.Bool("damage_dealt_heal"), field.Int64("version"), field.Time("created_at"), field.Time("updated_at")}
}
func (GameItemRecoveryRule) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id").Unique().StorageKey("uk_game_item_recovery_rule_item_id")}
}
func (GameItemRecoveryRule) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具恢复规则。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_recovery_rule", Checks: map[string]string{"game_item_recovery_rule_version_check": "version > 0"}}}
}
