package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemContactRule 定义道具接触与转移规则。
type GameItemContactRule struct{ ent.Schema }

func (GameItemContactRule) Fields() []ent.Field {
	return []ent.Field{field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()), field.Int64("item_id").GoType(snowflake.ID(0)).Positive(), field.Bool("contact_side_effect_immunity"), field.Int32("contact_damage_to_attacker_denominator"), field.Bool("contact_transfer_to_attacker"), field.Bool("punch_based_contact_suppression"), field.Bool("punch_based_skill_power_boost"), field.Int64("version"), field.Time("created_at"), field.Time("updated_at")}
}
func (GameItemContactRule) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id").Unique().StorageKey("uk_game_item_contact_rule_item_id")}
}
func (GameItemContactRule) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具接触规则。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_contact_rule", Checks: map[string]string{"game_item_contact_rule_version_check": "version > 0", "game_item_contact_rule_denominator_check": "contact_damage_to_attacker_denominator >= 0"}}}
}
