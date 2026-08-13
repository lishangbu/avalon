package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameSkillStatChange 定义 game_skill_stat_change 表的持久化结构。
type GameSkillStatChange struct {
	ent.Schema
}

// Fields 返回 game_skill_stat_change 表全部字段及其数据库约束。
func (GameSkillStatChange) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.Int64("skill_id").GoType(snowflake.ID(0)).Positive().Comment("关联技能资料的稳定 Identifier。"),
		field.Int64("stat_id").GoType(snowflake.ID(0)).Positive().Comment("关联战斗属性资料的稳定 Identifier。"),
		field.Int32("change_value").Comment("该资料记录的 change value 业务属性。"),
		field.Int64("version").Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Comment("该资料记录的最后更新时间。"),
		field.Time("disabled_at").Optional().Nillable().Comment("该资料记录的 disabled at 业务属性。"),
	}
}

// Annotations 固定 game_skill_stat_change 的表名、注释、复合主键和检查约束。
func (GameSkillStatChange) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料：技能造成的属性变化。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_skill_stat_change", Checks: map[string]string{
			"game_skill_stat_change_change_value_check": "change_value >= '-6'::integer AND change_value <= 6 AND change_value <> 0",
			"game_skill_stat_change_version_check":      "version > 0",
		}},
	}
}
