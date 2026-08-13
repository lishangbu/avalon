package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemSwitchRule 定义道具换人与入场规则。
type GameItemSwitchRule struct{ ent.Schema }

func (GameItemSwitchRule) Fields() []ent.Field {
	return []ent.Field{field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()), field.Int64("item_id").GoType(snowflake.ID(0)).Positive(), field.Bool("damaged_force_self_switch"), field.Bool("damaged_force_attacker_switch"), field.Bool("negative_stat_stage_force_self_switch"), field.Bool("switch_restriction_immunity"), field.Bool("entry_hazard_immunity"), field.Int64("version"), field.Time("created_at"), field.Time("updated_at")}
}
func (GameItemSwitchRule) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id").Unique().StorageKey("uk_game_item_switch_rule_item_id")}
}
func (GameItemSwitchRule) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具换人规则。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_switch_rule", Checks: map[string]string{"game_item_switch_rule_version_check": "version > 0"}}}
}
