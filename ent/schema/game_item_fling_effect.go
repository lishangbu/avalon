package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemFlingEffect 定义投掷效果字典。
type GameItemFlingEffect struct{ ent.Schema }

// Fields 返回投掷效果的规范化字段。
func (GameItemFlingEffect) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("投掷效果稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("投掷效果稳定编码。"),
		field.String("name").MaxLen(120).Comment("投掷效果名称。"),
		field.String("effect").Optional().Nillable().Comment("投掷效果说明。"),
		field.Bool("enabled").Comment("是否启用。"),
		field.Int64("version").Comment("乐观并发版本。"),
		field.Time("created_at").Comment("创建时间。"),
		field.Time("updated_at").Comment("更新时间。"),
	}
}

// Annotations 固定投掷效果表名及约束。
func (GameItemFlingEffect) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具投掷效果。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_fling_effect", Checks: map[string]string{
		"game_item_fling_effect_code_check":    "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
		"game_item_fling_effect_version_check": "version > 0",
	}}}
}
