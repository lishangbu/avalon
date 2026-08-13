package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterSensitiveNameRule 定义 player_character_sensitive_name_rule 表的持久化结构。
type PlayerCharacterSensitiveNameRule struct {
	ent.Schema
}

// Fields 返回 player_character_sensitive_name_rule 表全部字段及其数据库约束。
func (PlayerCharacterSensitiveNameRule) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("player_character_sensitive_name_rule 表的 id 字段。"),
		field.String("normalized_term").MaxLen(64).Comment("player_character_sensitive_name_rule 表的 normalized_term 字段。"),
		field.String("match_type").MaxLen(16).Comment("player_character_sensitive_name_rule 表的 match_type 字段。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("player_character_sensitive_name_rule 表的 enabled 字段。"),
		field.Time("created_at").Comment("player_character_sensitive_name_rule 表的 created_at 字段。"),
		field.Time("updated_at").Comment("player_character_sensitive_name_rule 表的 updated_at 字段。"),
	}
}

// Annotations 固定 player_character_sensitive_name_rule 的表名、注释、复合主键和检查约束。
func (PlayerCharacterSensitiveNameRule) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 player_character_sensitive_name_rule 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_sensitive_name_rule", Checks: map[string]string{
			"player_character_sensitive_name_rule_check":                 "updated_at >= created_at",
			"player_character_sensitive_name_rule_match_type_check":      "match_type::text = ANY (ARRAY['exact'::character varying::text, 'contains'::character varying::text])",
			"player_character_sensitive_name_rule_normalized_term_check": "normalized_term::text <> ''::text",
		}},
	}
}
