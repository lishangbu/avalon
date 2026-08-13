package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterTeam 定义 player_character_team 表的持久化结构。
type PlayerCharacterTeam struct {
	ent.Schema
}

// Fields 返回 player_character_team 表全部字段及其数据库约束。
func (PlayerCharacterTeam) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("Team 的稳定 Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("拥有该 Team 的 PlayerCharacter 稳定 Identifier。"),
		field.String("name").MaxLen(120).Comment("经规范化后展示给角色拥有者的 Team 名称。"),
		field.String("name_key").MaxLen(120).Comment("用于同一 PlayerCharacter 内唯一性比较的规范化 Team 名称键。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("完整 Team 更新时递增的乐观并发控制版本。"),
		field.Time("created_at").Comment("Team 首次创建的时间。"),
		field.Time("updated_at").Comment("Team 最近一次完整替换的时间。"),
	}
}

// Annotations 固定 player_character_team 的表名、注释、复合主键和检查约束。
func (PlayerCharacterTeam) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 在 Match 外维护的可变版本化命名阵容。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_team", Checks: map[string]string{
			"player_character_team_check":          "updated_at >= created_at",
			"player_character_team_name_check":     "char_length(name::text) >= 1 AND char_length(name::text) <= 40 AND name::text = btrim(name::text)",
			"player_character_team_name_key_check": "char_length(name_key::text) >= 1 AND char_length(name_key::text) <= 40 AND name_key::text = btrim(name_key::text)",
			"player_character_team_version_check":  "version >= 1",
		}},
	}
}
