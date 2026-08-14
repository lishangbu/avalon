package schema

import (
	"encoding/json"

	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameBattleClause 定义 game_battle_clause 表的持久化结构。
type GameBattleClause struct {
	ent.Schema
}

// Fields 返回 game_battle_clause 表全部字段及其数据库约束。
func (GameBattleClause) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("该资料记录的全局唯一稳定编码。"),
		field.String("name").MaxLen(80).Comment("该资料记录的简体中文显示名称。"),
		field.String("description").MaxLen(500).Comment("该资料记录的简体中文说明。"),
		field.String("effect_kind").MaxLen(120).Comment("效果定义的稳定类别标识。"),
		field.Int32("effect_schema_version").Comment("效果参数 JSON 使用的结构版本。"),
		field.JSON("effect_parameters", json.RawMessage{}).Comment("按效果类别解释的 JSON 参数。"),
		field.Bool("enabled").Comment("是否允许新的业务数据引用该实时资料。"),
		field.Int64("version").Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_battle_clause 的表名、注释、复合主键和检查约束。
func (GameBattleClause) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料：赛制条款。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_battle_clause", Checks: map[string]string{
			"game_battle_clause_code_check":                  "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"game_battle_clause_description_check":           "description::text = btrim(description::text)",
			"game_battle_clause_effect_kind_check":           "effect_kind::text = btrim(effect_kind::text) AND effect_kind::text <> ''::text",
			"game_battle_clause_effect_parameters_check":     "jsonb_typeof(effect_parameters) = 'object'::text",
			"game_battle_clause_effect_schema_version_check": "effect_schema_version > 0",
			"game_battle_clause_name_check":                  "char_length(name::text) >= 1 AND char_length(name::text) <= 80 AND name::text = btrim(name::text)",
			"game_battle_clause_version_check":               "version > 0",
		}},
	}
}
