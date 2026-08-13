package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgLootTable 定义 rpg_loot_table 表的持久化结构。
type RpgLootTable struct {
	ent.Schema
}

// Fields 返回 rpg_loot_table 表全部字段及其数据库约束。
func (RpgLootTable) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("RPG 掉落表记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("RPG 掉落表的全局唯一英文机器编码。"),
		field.String("name").MaxLen(120).Comment("RPG 掉落表的简体中文展示名称。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("RPG 掉落表是否可被新的 RPG 进度引用。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("RPG 掉落表写入使用的正整数乐观并发版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 掉落表首次创建的 UTC 时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("RPG 掉落表最近一次业务更新的 UTC 时间。"),
	}
}

// Annotations 固定 rpg_loot_table 的表名、注释、复合主键和检查约束。
func (RpgLootTable) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("可由战斗、交互或任务引用的一组加权道具掉落候选。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_loot_table", Checks: map[string]string{
			"rpg_loot_table_code_check":    "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_loot_table_name_check":    "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_loot_table_time_check":    "updated_at >= created_at",
			"rpg_loot_table_version_check": "version > 0",
		}},
	}
}
