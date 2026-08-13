package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemMultiHitRule 定义道具连续命中范围覆盖规则。
type GameItemMultiHitRule struct{ ent.Schema }

func (GameItemMultiHitRule) Fields() []ent.Field {
	return []ent.Field{field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()), field.Int64("item_id").GoType(snowflake.ID(0)).Positive(), field.Int32("count_minimum"), field.Int32("count_maximum"), field.Int32("required_minimum"), field.Int32("required_maximum"), field.Int64("version"), field.Time("created_at"), field.Time("updated_at")}
}
func (GameItemMultiHitRule) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id").Unique().StorageKey("uk_game_item_multi_hit_rule_item_id")}
}
func (GameItemMultiHitRule) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具连续命中规则。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_multi_hit_rule", Checks: map[string]string{"game_item_multi_hit_rule_version_check": "version > 0"}}}
}
