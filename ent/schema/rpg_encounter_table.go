package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgEncounterTable 定义 rpg_encounter_table 表的持久化结构。
type RpgEncounterTable struct {
	ent.Schema
}

// Fields 返回 rpg_encounter_table 表全部字段及其数据库约束。
func (RpgEncounterTable) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 遭遇表记录的稳定 Identifier。"),
		field.Int64("location_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 遭遇表所属或引用的 RPG 地点稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("RPG 遭遇表的全局唯一英文机器编码。"),
		field.String("name").MaxLen(120).Comment("RPG 遭遇表的简体中文展示名称。"),
		field.String("encounter_method").MaxLen(32).Comment("RPG 遭遇表触发野生遭遇的移动方式；首期只允许 walk。"),
		field.Int32("trigger_probability_bps").Comment("每次符合条件移动触发遭遇的整数万分比。"),
		field.Int64("cooldown_moves").Default(0).Comment("同一角色再次使用该表前必须经过的移动步数。"),
		field.Int32("maximum_uses").Optional().Nillable().Comment("该角色可触发该表的可选次数上限。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 遭遇表是否可被新的 RPG 进度引用。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("RPG 遭遇表写入使用的正整数乐观并发版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 遭遇表首次创建的 UTC 时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 遭遇表最近一次业务更新的 UTC 时间。"),
	}
}

// Annotations 固定 rpg_encounter_table 的表名、注释、复合主键和检查约束。
func (RpgEncounterTable) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("某地点与遭遇方式对应的一组加权野生 Creature 候选。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_encounter_table", Checks: map[string]string{
			"rpg_encounter_table_code_check":         "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_encounter_table_method_check":       "encounter_method = 'walk'",
			"rpg_encounter_table_probability_check":  "trigger_probability_bps >= 0 AND trigger_probability_bps <= 10000",
			"rpg_encounter_table_cooldown_check":     "cooldown_moves >= 0",
			"rpg_encounter_table_maximum_uses_check": "maximum_uses IS NULL OR maximum_uses > 0",
			"rpg_encounter_table_name_check":         "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_encounter_table_time_check":         "updated_at >= created_at",
			"rpg_encounter_table_version_check":      "version > 0",
		}},
	}
}
