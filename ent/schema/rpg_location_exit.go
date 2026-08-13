package schema

import (
	"encoding/json"

	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgLocationExit 定义从一个 Location 指向另一个 Location 的有向出口资料。
type RpgLocationExit struct {
	ent.Schema
}

// Fields 返回 RPG 有向出口的稳定身份、目标和规则字段。
func (RpgLocationExit) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("有向 Location Exit 的稳定 Identifier。"),
		field.Int64("source_location_id").GoType(snowflake.ID(0)).Positive().Comment("出口来源 Location 的稳定 Identifier。"),
		field.Int64("target_location_id").GoType(snowflake.ID(0)).Positive().Comment("出口目标 Location 的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("出口的全局稳定英文机器编码。"),
		field.String("name").MaxLen(120).Comment("出口面向玩家的简体中文名称。"),
		field.String("description").Optional().Nillable().MaxLen(4000).Comment("出口的可选简体中文说明。"),
		field.Int32("sort_order").Default(0).Comment("来源地点展示出口时的非负排序值。"),
		field.JSON("condition", json.RawMessage{}).Comment("规范化后的 Exit Condition JSON 对象。"),
		field.JSON("effect", json.RawMessage{}).Optional().Comment("规范化后的 Traversal Effect JSON 对象；无副作用时为空。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("出口是否允许新的 Traversal。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("出口资料的正整数乐观版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("出口首次创建时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("出口最近更新时间。"),
	}
}

// Indexes 返回出口编码和来源排序查询所需索引。
func (RpgLocationExit) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("code").Unique().StorageKey("uk_rpg_location_exit_code"),
		index.Fields("source_location_id", "sort_order", "id").StorageKey("idx_rpg_location_exit_source_location_id_sort_order_id"),
	}
}

// Annotations 固定出口表名、注释和服务端规则约束。
func (RpgLocationExit) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("RPG 世界中一条独立的有向 Location Exit 资料。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_location_exit", Checks: map[string]string{
			"rpg_location_exit_code_check":       "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_location_exit_condition_check":  "jsonb_typeof(condition) = 'object'::text",
			"rpg_location_exit_effect_check":     "effect IS NULL OR jsonb_typeof(effect) = 'object'::text",
			"rpg_location_exit_name_check":       "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_location_exit_sort_order_check": "sort_order >= 0",
			"rpg_location_exit_target_check":     "source_location_id <> target_location_id",
			"rpg_location_exit_time_check":       "updated_at >= created_at",
			"rpg_location_exit_version_check":    "version > 0",
		}},
	}
}
