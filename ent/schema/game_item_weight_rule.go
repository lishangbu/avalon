package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemWeightRule 定义道具体重修正规则。
type GameItemWeightRule struct{ ent.Schema }

func (GameItemWeightRule) Fields() []ent.Field {
	return []ent.Field{field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()), field.Int64("item_id").GoType(snowflake.ID(0)).Positive(), field.Bool("weight_half"), field.Bool("airborne_until_damaged"), field.Bool("force_grounded"), field.Bool("type_immunity_suppression"), field.Int64("version"), field.Time("created_at"), field.Time("updated_at")}
}
func (GameItemWeightRule) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id").Unique().StorageKey("uk_game_item_weight_rule_item_id")}
}
func (GameItemWeightRule) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具体重规则。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_weight_rule", Checks: map[string]string{"game_item_weight_rule_version_check": "version > 0"}}}
}
