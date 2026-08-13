package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameEquipmentStatModifier 定义装备对现有 Game Stat 的平加和万分比修正。
type GameEquipmentStatModifier struct{ ent.Schema }

// Fields 返回装备、Stat 与确定性修正值。
func (GameEquipmentStatModifier) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("装备属性修正关系的稳定 Snowflake Identifier。"),
		field.Int64("equipment_id").GoType(snowflake.ID(0)).Positive().Comment("Equipment Catalog Entry Identifier。"),
		field.Int64("stat_id").GoType(snowflake.ID(0)).Positive().Comment("被修正的现有 Game Stat Identifier。"),
		field.Int64("flat_value").Comment("百分比计算前累加的有符号整数修正。"),
		field.Int32("percentage_bps").Comment("以万分之一为单位、在全部平加后累加应用的百分比修正。"),
	}
}

// Indexes 保证同一装备对一种 Stat 只有一条规范修正。
func (GameEquipmentStatModifier) Indexes() []ent.Index {
	return []ent.Index{index.Fields("equipment_id", "stat_id").Unique().StorageKey("uk_game_equipment_stat_modifier_equipment_id_stat_id")}
}

// Annotations 限制单项百分比修正范围。
func (GameEquipmentStatModifier) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("Equipment Catalog Entry 的确定性属性修正。"), entsql.WithComments(true), entsql.Annotation{Table: "game_equipment_stat_modifier", Checks: map[string]string{"game_equipment_stat_modifier_percentage_check": "percentage_bps BETWEEN -10000 AND 100000"}}}
}
