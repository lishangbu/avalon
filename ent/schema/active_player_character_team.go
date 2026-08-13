package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// ActivePlayerCharacterTeam 定义 active_player_character_team 表的持久化结构。
type ActivePlayerCharacterTeam struct {
	ent.Schema
}

// Fields 返回 active_player_character_team 表全部字段及其数据库约束。
func (ActivePlayerCharacterTeam) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).StorageKey("player_character_id").Comment("拥有当前默认 Team 绑定的 PlayerCharacter 稳定 Identifier。"),
		field.Int64("team_id").GoType(snowflake.ID(0)).Positive().Comment("当前被选为默认 Team 的稳定 Identifier。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("默认 Team 绑定的乐观并发控制版本。"),
		field.Time("updated_at").Comment("默认 Team 绑定最后一次切换的时间。"),
	}
}

// Annotations 固定 active_player_character_team 的表名、注释、复合主键和检查约束。
func (ActivePlayerCharacterTeam) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 当前默认 Team 的持久化乐观并发绑定。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "active_player_character_team", Checks: map[string]string{
			"active_player_character_team_version_check": "version >= 1",
		}},
	}
}
