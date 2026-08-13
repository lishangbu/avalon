package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemStatBoosterAbility 定义道具最高能力强化规则允许触发的 Ability 关系。
type GameItemStatBoosterAbility struct{ ent.Schema }

// Fields 返回 Item 与 Ability 多对多关系字段。
func (GameItemStatBoosterAbility) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("关系稳定 Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("道具稳定 Identifier。"),
		field.Int64("ability_id").GoType(snowflake.ID(0)).Positive().Comment("允许触发的 Ability Identifier。"),
		field.Int64("version").Comment("乐观并发版本。"), field.Time("created_at").Comment("创建时间。"), field.Time("updated_at").Comment("更新时间。"),
	}
}

// Indexes 保证同一 Item 与 Ability 只绑定一次。
func (GameItemStatBoosterAbility) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id", "ability_id").Unique().StorageKey("uk_game_item_stat_booster_ability_item_id_ability_id")}
}

// Annotations 固定关系表名与版本约束。
func (GameItemStatBoosterAbility) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具最高能力强化 Ability 关系。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_stat_booster_ability", Checks: map[string]string{"game_item_stat_booster_ability_version_check": "version > 0"}}}
}
