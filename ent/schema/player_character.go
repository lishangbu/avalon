package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacter 定义 player_character 表的持久化结构。
type PlayerCharacter struct {
	ent.Schema
}

// Fields 返回 player_character 表全部字段及其数据库约束。
func (PlayerCharacter) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("player_character 表的 id 字段。"),
		field.Int64("account_id").GoType(snowflake.ID(0)).Positive().Comment("player_character 表的 account_id 字段。"),
		field.String("display_name").MaxLen(64).Comment("player_character 表的 display_name 字段。"),
		field.String("display_name_key").MaxLen(64).Comment("player_character 表的 display_name_key 字段。"),
		field.Int32("level").Default(1).Comment("PlayerCharacter 的正整数 RPG 等级。"),
		field.Int64("experience").Default(0).Comment("PlayerCharacter 的非负累计 RPG 经验。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("player_character 表的 version 字段。"),
		field.Time("archived_at").Optional().Nillable().Comment("player_character 表的 archived_at 字段。"),
		field.Time("created_at").Comment("player_character 表的 created_at 字段。"),
		field.Time("updated_at").Comment("player_character 表的 updated_at 字段。"),
	}
}

// Annotations 固定 player_character 的表名、注释、复合主键和检查约束。
func (PlayerCharacter) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 player_character 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character", Checks: map[string]string{
			"player_character_check":            "updated_at >= created_at",
			"player_character_level_check":      "level > 0",
			"player_character_experience_check": "experience >= 0",
			"player_character_version_check":    "version >= 1",
		}},
	}
}
