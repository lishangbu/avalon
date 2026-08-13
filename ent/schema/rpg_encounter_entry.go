package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgEncounterEntry 定义 rpg_encounter_entry 表的持久化结构。
type RpgEncounterEntry struct {
	ent.Schema
}

// Fields 返回 rpg_encounter_entry 表全部字段及其数据库约束。
func (RpgEncounterEntry) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 遭遇候选记录的稳定 Identifier。"),
		field.Int64("encounter_table_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 遭遇候选所属遭遇表稳定 Identifier。"),
		field.Int64("creature_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 遭遇候选引用的 Creature 资料稳定 Identifier。"),
		field.Int64("form_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 遭遇候选引用的可选 Creature Form 稳定 Identifier。"),
		field.Int64("loot_table_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("战胜该遭遇候选后使用的可选权威 Loot Table Identifier。"),
		field.Int16("minimum_level").Comment("RPG 遭遇候选允许生成或使用的最低等级。"),
		field.Int16("maximum_level").Comment("RPG 遭遇候选允许生成或使用的最高等级。"),
		field.Int32("weight").Comment("RPG 遭遇候选参与同表加权随机选择的正整数权重。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 遭遇候选是否可被新的 RPG 进度引用。"),
	}
}

// Annotations 固定 rpg_encounter_entry 的表名、注释、复合主键和检查约束。
func (RpgEncounterEntry) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("遭遇表中的 Creature、等级区间和正整数权重。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_encounter_entry", Checks: map[string]string{
			"rpg_encounter_entry_level_check":  "minimum_level >= 1 AND minimum_level <= 100 AND maximum_level >= minimum_level AND maximum_level <= 100",
			"rpg_encounter_entry_weight_check": "weight > 0",
		}},
	}
}
