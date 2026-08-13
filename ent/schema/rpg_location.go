package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgLocation 定义 rpg_location 表的持久化结构。
type RpgLocation struct {
	ent.Schema
}

// Fields 返回 rpg_location 表全部字段及其数据库约束。
func (RpgLocation) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 世界地点记录的稳定 Identifier。"),
		field.Int64("region_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 世界地点所属 RPG 世界区域稳定 Identifier。"),
		field.Int64("parent_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 世界地点可选的直接上级记录稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("RPG 世界地点的全局唯一英文机器编码。"),
		field.String("name").MaxLen(120).Comment("RPG 世界地点的简体中文展示名称。"),
		field.String("location_type").MaxLen(32).Comment("RPG 世界地点的世界、聚落、道路、野外、迷宫、室内或竞技场类型。"),
		field.String("description").Optional().Nillable().Comment("RPG 世界地点面向玩家或管理者的可选简体中文说明。"),
		field.Bool("default_spawn").Default(false).Comment("是否为新 PlayerCharacter 唯一的默认出生地点。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 世界地点是否可被新的 RPG 进度引用。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("RPG 世界地点写入使用的正整数乐观并发版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 世界地点首次创建的 UTC 时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 世界地点最近一次业务更新的 UTC 时间。"),
	}
}

// Annotations 固定 rpg_location 的表名、注释、复合主键和检查约束。
func (RpgLocation) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("RPG 世界中可进入、遭遇、交互或保存检查点的层级地点。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_location", Checks: map[string]string{
			"rpg_location_code_check":        "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_location_description_check": "description IS NULL OR char_length(description) >= 1 AND char_length(description) <= 4000 AND description = btrim(description)",
			"rpg_location_name_check":        "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_location_parent_check":      "parent_id IS NULL OR parent_id <> id",
			"rpg_location_spawn_check":       "NOT default_spawn OR enabled",
			"rpg_location_time_check":        "updated_at >= created_at",
			"rpg_location_type_check":        "location_type::text = ANY (ARRAY['world'::character varying, 'settlement'::character varying, 'route'::character varying, 'wild'::character varying, 'dungeon'::character varying, 'interior'::character varying, 'arena'::character varying]::text[])",
			"rpg_location_version_check":     "version > 0",
		}},
	}
}
