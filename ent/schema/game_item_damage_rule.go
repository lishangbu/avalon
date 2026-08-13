package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemDamageRule 定义道具伤害修正规则的一对一关系记录。
type GameItemDamageRule struct{ ent.Schema }

// Fields 返回伤害规则的强类型字段。
func (GameItemDamageRule) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("规则稳定 Identifier。"), field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("道具稳定 Identifier。"),
		field.Bool("physical_damage_power_boost").Comment("是否强化物理伤害。"), field.Bool("special_damage_power_boost").Comment("是否强化特殊伤害。"), field.Bool("physical_damage_power_boost_50").Comment("是否强化物理伤害百分之五十。"), field.Bool("special_damage_power_boost_50").Comment("是否强化特殊伤害百分之五十。"), field.Bool("super_effective_damage_boost").Comment("是否强化效果拔群伤害。"), field.Bool("damage_boost_with_recoil").Comment("是否以反作用代价强化伤害。"), field.Bool("damage_dealt_heal").Comment("造成伤害时是否回复。"), field.Bool("drain_healing_boost").Comment("是否强化吸取回复。"), field.Bool("weakness_policy").Comment("是否触发弱点策略。"), field.Bool("consecutive_skill_damage_boost").Comment("是否强化连续技能伤害。"),
		field.Int64("element_boost_element_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("持续属性伤害强化匹配的 Element Identifier。"),
		field.Int64("consumable_boost_element_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("一次性属性伤害强化匹配的 Element Identifier。"),
		field.Int32("consumable_boost_numerator").Comment("一次性属性伤害强化倍率分子。"),
		field.Int32("consumable_boost_denominator").Comment("一次性属性伤害强化倍率分母。"),
		field.Int64("reduction_element_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("一次性属性伤害减免匹配的 Element Identifier。"),
		field.Bool("reduction_requires_super_effective").Comment("减免是否要求技能严格克制。"),
		field.Int64("version").Comment("乐观并发版本。"), field.Time("created_at").Comment("创建时间。"), field.Time("updated_at").Comment("更新时间。"),
	}
}

// Indexes 返回每个道具最多一条伤害规则的唯一约束。
func (GameItemDamageRule) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id").Unique().StorageKey("uk_game_item_damage_rule_item_id")}
}

// Annotations 固定伤害规则表及版本约束。
func (GameItemDamageRule) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具伤害规则。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_damage_rule", Checks: map[string]string{"game_item_damage_rule_version_check": "version > 0", "game_item_damage_rule_consumable_boost_check": "(consumable_boost_element_id IS NULL AND consumable_boost_numerator = 0 AND consumable_boost_denominator = 0) OR (consumable_boost_element_id IS NOT NULL AND consumable_boost_numerator > 0 AND consumable_boost_denominator > 0)"}}}
}
