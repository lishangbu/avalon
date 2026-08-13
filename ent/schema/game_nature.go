package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameNature 定义 game_nature 表的持久化结构。
type GameNature struct {
	ent.Schema
}

// Fields 返回 game_nature 表全部字段及其数据库约束。
func (GameNature) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("该资料记录的全局唯一稳定编码。"),
		field.String("name").MaxLen(80).Comment("该资料记录的简体中文显示名称。"),
		field.Int64("increased_stat_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("该资料记录的 increased stat id 业务属性。"),
		field.Int64("decreased_stat_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("该资料记录的 decreased stat id 业务属性。"),
		field.Bool("enabled").Comment("是否允许新的业务数据引用该实时资料。"),
		field.Int64("version").Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_nature 的表名、注释、复合主键和检查约束。
func (GameNature) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料表。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_nature", Checks: map[string]string{
			"game_nature_code_check":      "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"game_nature_name_check":      "char_length(name::text) >= 1 AND char_length(name::text) <= 80 AND name::text = btrim(name::text)",
			"game_nature_stat_pair_check": "increased_stat_id IS NULL AND decreased_stat_id IS NULL OR increased_stat_id IS NOT NULL AND decreased_stat_id IS NOT NULL AND increased_stat_id <> decreased_stat_id",
			"game_nature_version_check":   "version > 0",
		}},
	}
}
