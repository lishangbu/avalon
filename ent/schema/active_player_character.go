package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// ActivePlayerCharacter 定义 active_player_character 表的持久化结构。
type ActivePlayerCharacter struct {
	ent.Schema
}

// Fields 返回 active_player_character 表全部字段及其数据库约束。
func (ActivePlayerCharacter) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).StorageKey("account_id").Comment("active_player_character 表的 account_id 字段。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("active_player_character 表的 player_character_id 字段。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("active_player_character 表的 version 字段。"),
		field.Time("updated_at").Comment("active_player_character 表的 updated_at 字段。"),
	}
}

// Annotations 固定 active_player_character 的表名、注释、复合主键和检查约束。
func (ActivePlayerCharacter) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 active_player_character 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "active_player_character", Checks: map[string]string{
			"active_player_character_version_check": "version >= 1",
		}},
	}
}
