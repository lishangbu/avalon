package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgLootEntry 定义 rpg_loot_entry 表的持久化结构。
type RpgLootEntry struct {
	ent.Schema
}

// Fields 返回 rpg_loot_entry 表全部字段及其数据库约束。
func (RpgLootEntry) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 掉落候选记录的稳定 Identifier。"),
		field.Int64("loot_table_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 掉落候选所属掉落表稳定 Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 掉落候选引用的道具稳定 Identifier。"),
		field.Int32("minimum_quantity").Comment("RPG 掉落候选一次产生的最小正整数道具数量。"),
		field.Int32("maximum_quantity").Comment("RPG 掉落候选一次产生且不低于最小值的最大道具数量。"),
		field.Int32("weight").Comment("RPG 掉落候选参与同表加权随机选择的正整数权重。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("掉落候选是否参与新的权威抽样。"),
	}
}

// Annotations 固定 rpg_loot_entry 的表名、注释、复合主键和检查约束。
func (RpgLootEntry) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("掉落表中道具数量区间与正整数权重。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_loot_entry", Checks: map[string]string{
			"rpg_loot_entry_quantity_check": "minimum_quantity > 0 AND maximum_quantity >= minimum_quantity",
			"rpg_loot_entry_weight_check":   "weight > 0",
		}},
	}
}
