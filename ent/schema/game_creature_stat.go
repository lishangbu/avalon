package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameCreatureStat 定义 game_creature_stat 表的持久化结构。
type GameCreatureStat struct {
	ent.Schema
}

// Fields 返回 game_creature_stat 表全部字段及其数据库约束。
func (GameCreatureStat) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.Int64("creature_id").GoType(snowflake.ID(0)).Positive().Comment("该资料记录的 creature id 业务属性。"),
		field.Int64("stat_id").GoType(snowflake.ID(0)).Positive().Comment("关联战斗属性资料的稳定 Identifier。"),
		field.Int32("base_value").Comment("该资料记录的 base value 业务属性。"),
		field.Int16("effort").Comment("该资料记录的 effort 业务属性。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("是否允许新的业务数据引用该实时资料。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_creature_stat 的表名、注释、复合主键和检查约束。
func (GameCreatureStat) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料表。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_creature_stat", Checks: map[string]string{
			"game_creature_stat_base_value_check": "base_value > 0",
			"game_creature_stat_effort_check":     "effort >= 0 AND effort <= 3",
			"game_creature_stat_version_check":    "version > 0",
		}},
	}
}
