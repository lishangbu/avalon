package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameElementEffectiveness 定义 game_element_effectiveness 表的持久化结构。
type GameElementEffectiveness struct {
	ent.Schema
}

// Fields 返回 game_element_effectiveness 表全部字段及其数据库约束。
func (GameElementEffectiveness) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.Int64("attack_element_id").GoType(snowflake.ID(0)).Positive().Comment("该资料记录的 attack element id 业务属性。"),
		field.Int64("defense_element_id").GoType(snowflake.ID(0)).Positive().Comment("该资料记录的 defense element id 业务属性。"),
		field.Int16("numerator").Comment("该资料记录的 numerator 业务属性。"),
		field.Int16("denominator").Comment("该资料记录的 denominator 业务属性。"),
		field.Bool("enabled").Comment("是否允许新的业务数据引用该实时资料。"),
		field.Int64("version").Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_element_effectiveness 的表名、注释、复合主键和检查约束。
func (GameElementEffectiveness) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料表。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_element_effectiveness", Checks: map[string]string{
			"game_element_effectiveness_fraction_check": "numerator = 0 AND denominator = 1 OR numerator = 1 AND denominator = 2 OR numerator = 2 AND denominator = 1",
			"game_element_effectiveness_version_check":  "version > 0",
		}},
	}
}
