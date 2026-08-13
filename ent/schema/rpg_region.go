package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgRegion 定义 rpg_region 表的持久化结构。
type RpgRegion struct {
	ent.Schema
}

// Fields 返回 rpg_region 表全部字段及其数据库约束。
func (RpgRegion) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 世界区域记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("RPG 世界区域的全局唯一英文机器编码。"),
		field.String("name").MaxLen(120).Comment("RPG 世界区域的简体中文展示名称。"),
		field.String("description").Optional().Nillable().Comment("RPG 世界区域面向玩家或管理者的可选简体中文说明。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 世界区域是否可被新的 RPG 进度引用。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("RPG 世界区域写入使用的正整数乐观并发版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 世界区域首次创建的 UTC 时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 世界区域最近一次业务更新的 UTC 时间。"),
	}
}

// Annotations 固定 rpg_region 的表名、注释、复合主键和检查约束。
func (RpgRegion) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("RPG 世界中包含多个地点的顶层区域资料。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_region", Checks: map[string]string{
			"rpg_region_code_check":        "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_region_description_check": "description IS NULL OR char_length(description) >= 1 AND char_length(description) <= 4000 AND description = btrim(description)",
			"rpg_region_name_check":        "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_region_time_check":        "updated_at >= created_at",
			"rpg_region_version_check":     "version > 0",
		}},
	}
}
