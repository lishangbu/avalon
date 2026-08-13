package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgShop 定义 rpg_shop 表的持久化结构。
type RpgShop struct {
	ent.Schema
}

// Fields 返回 rpg_shop 表全部字段及其数据库约束。
func (RpgShop) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 商店记录的稳定 Identifier。"),
		field.Int64("npc_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("RPG 商店关联的可选 NPC 稳定 Identifier。"),
		field.Int64("location_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 商店所属或引用的 RPG 地点稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("RPG 商店的全局唯一英文机器编码。"),
		field.String("name").MaxLen(120).Comment("RPG 商店的简体中文展示名称。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 商店是否可被新的 RPG 进度引用。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("RPG 商店写入使用的正整数乐观并发版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 商店首次创建的 UTC 时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 商店最近一次业务更新的 UTC 时间。"),
	}
}

// Annotations 固定 rpg_shop 的表名、注释、复合主键和检查约束。
func (RpgShop) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("固定地点中可由 NPC 主持的商品目录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_shop", Checks: map[string]string{
			"rpg_shop_code_check":    "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_shop_name_check":    "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_shop_time_check":    "updated_at >= created_at",
			"rpg_shop_version_check": "version > 0",
		}},
	}
}
