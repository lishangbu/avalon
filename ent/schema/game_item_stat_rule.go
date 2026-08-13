package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemStatRule 定义道具能力阶级与能力修正规则。
type GameItemStatRule struct{ ent.Schema }

// Fields 返回能力规则的强类型字段。
func (GameItemStatRule) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("规则稳定 Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("道具稳定 Identifier。"),
		field.Bool("special_defense_boost").Comment("是否提高参与特殊伤害公式的特防。"),
		field.Bool("speed_half").Comment("是否把有效速度减半。"),
		field.Bool("speed_boost_50").Comment("是否把有效速度提高百分之五十。"),
		field.Bool("accuracy_boost").Comment("是否提高持有者命中率。"),
		field.Bool("opponent_accuracy_reduction").Comment("是否降低对手针对持有者的命中率。"),
		field.Bool("critical_hit_stage_boost").Comment("是否提高要害等级。"),
		field.Bool("opponent_stat_stage_reduction_immunity").Comment("是否免疫对手造成的能力降阶。"),
		field.Bool("negative_stat_stage_reset").Comment("是否在降阶后清除负能力阶级。"),
		field.Bool("ability_stat_reduction_speed_boost").Comment("是否在特性降阶后提高速度。"),
		field.Bool("opponent_positive_stat_stage_copy").Comment("是否复制对手获得的正能力阶级。"),
		field.String("accuracy_miss_stat_stage_boost_stat").MaxLen(32).Optional().Nillable().Comment("因命中判定落空后提高的能力项。"),
		field.Int32("accuracy_miss_stat_stage_boost_delta").Comment("因命中判定落空后提高的能力阶级。"),
		field.Int64("water_spa_element_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("触发水属性伤害特攻强化的 Element Identifier。"),
		field.Int64("electric_atk_element_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("触发电属性伤害攻击强化的 Element Identifier。"),
		field.Int64("water_spd_element_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("触发水属性伤害特防强化的 Element Identifier。"),
		field.Int64("ice_atk_element_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("触发冰属性伤害攻击强化的 Element Identifier。"),
		field.Int64("version").Comment("乐观并发版本。"),
		field.Time("created_at").Comment("创建时间。"),
		field.Time("updated_at").Comment("更新时间。"),
	}
}

// Indexes 返回每个道具最多一条能力规则的唯一约束。
func (GameItemStatRule) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id").Unique().StorageKey("uk_game_item_stat_rule_item_id")}
}

// Annotations 固定能力规则表及字段组合约束。
func (GameItemStatRule) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料：道具能力规则。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_item_stat_rule", Checks: map[string]string{
			"game_item_stat_rule_version_check":       "version > 0",
			"game_item_stat_rule_accuracy_miss_check": "(accuracy_miss_stat_stage_boost_stat IS NULL AND accuracy_miss_stat_stage_boost_delta = 0) OR (accuracy_miss_stat_stage_boost_stat IS NOT NULL AND accuracy_miss_stat_stage_boost_delta > 0)",
		}},
	}
}
