package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemActionRule 定义道具行动顺序与技能限制规则。
type GameItemActionRule struct{ ent.Schema }

func (GameItemActionRule) Fields() []ent.Field {
	return []ent.Field{field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()), field.Int64("item_id").GoType(snowflake.ID(0)).Positive(), field.Bool("charge_skip_once"), field.Bool("choice_skill_lock"), field.Bool("forced_last_action_order"), field.Bool("low_hp_action_order_boost"), field.Bool("field_speed_order_speed_stage_drop"), field.Int32("additional_flinch_chance_percent"), field.Int32("random_action_order_boost_chance_percent"), field.Bool("accuracy_after_target_acted_boost"), field.Bool("survive_fatal_damage_at_full_hp"), field.Int32("binding_turns"), field.Int32("binding_damage_denominator"), field.Int64("version"), field.Time("created_at"), field.Time("updated_at")}
}
func (GameItemActionRule) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id").Unique().StorageKey("uk_game_item_action_rule_item_id")}
}
func (GameItemActionRule) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具行动规则。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_action_rule", Checks: map[string]string{"game_item_action_rule_version_check": "version > 0"}}}
}
