package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgNpc 定义 rpg_npc 表的持久化结构。
type RpgNpc struct {
	ent.Schema
}

// Fields 返回 rpg_npc 表全部字段及其数据库约束。
func (RpgNpc) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 非玩家角色记录的稳定 Identifier。"),
		field.Int64("location_id").GoType(snowflake.ID(0)).Positive().Comment("RPG 非玩家角色所属或引用的 RPG 地点稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("RPG 非玩家角色的全局唯一英文机器编码。"),
		field.String("name").MaxLen(120).Comment("RPG 非玩家角色的简体中文展示名称。"),
		field.String("npc_type").MaxLen(32).Comment("RPG 非玩家角色承担的剧情、商人、战斗、服务或环境职责。"),
		field.String("description").Optional().Nillable().Comment("RPG 非玩家角色面向玩家或管理者的可选简体中文说明。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 非玩家角色是否可被新的 RPG 进度引用。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("RPG 非玩家角色写入使用的正整数乐观并发版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 非玩家角色首次创建的 UTC 时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 非玩家角色最近一次业务更新的 UTC 时间。"),
	}
}

// Annotations 固定 rpg_npc 的表名、注释、复合主键和检查约束。
func (RpgNpc) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("固定归属地点并承担剧情、商店、战斗或服务职责的 NPC。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_npc", Checks: map[string]string{
			"rpg_npc_code_check":        "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_npc_description_check": "description IS NULL OR char_length(description) >= 1 AND char_length(description) <= 4000 AND description = btrim(description)",
			"rpg_npc_name_check":        "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_npc_time_check":        "updated_at >= created_at",
			"rpg_npc_type_check":        "npc_type::text = ANY (ARRAY['story'::character varying, 'merchant'::character varying, 'trainer'::character varying, 'service'::character varying, 'ambient'::character varying]::text[])",
			"rpg_npc_version_check":     "version > 0",
		}},
	}
}
