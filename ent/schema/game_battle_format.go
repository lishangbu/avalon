package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameBattleFormat 定义 game_battle_format 表的持久化结构。
type GameBattleFormat struct {
	ent.Schema
}

// Fields 返回 game_battle_format 表全部字段及其数据库约束。
func (GameBattleFormat) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("该资料记录的全局唯一稳定编码。"),
		field.String("name").MaxLen(80).Comment("该资料记录的简体中文显示名称。"),
		field.String("description").MaxLen(500).Comment("该资料记录的简体中文说明。"),
		field.String("mode").MaxLen(16).Comment("该资料记录的 mode 业务属性。"),
		field.Int32("roster_count").Comment("该资料记录的 roster count 业务属性。"),
		field.Int32("select_count").Comment("该资料记录的 select count 业务属性。"),
		field.Int32("active_participants_per_side").Comment("该资料记录的 active participants per side 业务属性。"),
		field.String("level_rule").MaxLen(16).Comment("该资料记录的 level rule 业务属性。"),
		field.Int32("normalized_level").Optional().Nillable().Comment("该资料记录的 normalized level 业务属性。"),
		field.Int32("preview_seconds").Comment("该资料记录的 preview seconds 业务属性。"),
		field.Int32("turn_seconds").Comment("该资料记录的 turn seconds 业务属性。"),
		field.Int32("battle_seconds").Comment("整场 Battle 的最长持续秒数。"),
		field.Bool("challenge_available").Comment("该资料记录的 challenge available 业务属性。"),
		field.Bool("training_available").Comment("该赛制是否允许创建 Training Battle。"),
		field.Bool("encounter_available").Comment("该赛制是否允许创建 Encounter Battle。"),
		field.Bool("admin_preview_available").Comment("该资料记录的 admin preview available 业务属性。"),
		field.JSON("clause_ids", []snowflake.ID{}).SchemaType(map[string]string{dialect.Postgres: "bigint[]"}).Annotations(entsql.DefaultExpr("'{}'::bigint[]")).Comment("赛制启用的条款 Snowflake Identifier 列表。"),
		field.JSON("restriction_ids", []snowflake.ID{}).SchemaType(map[string]string{dialect.Postgres: "bigint[]"}).Annotations(entsql.DefaultExpr("'{}'::bigint[]")).Comment("赛制启用的限制 Snowflake Identifier 列表。"),
		field.JSON("mechanic_ids", []snowflake.ID{}).SchemaType(map[string]string{dialect.Postgres: "bigint[]"}).Annotations(entsql.DefaultExpr("'{}'::bigint[]")).Comment("赛制启用的机制 Snowflake Identifier 列表。"),
		field.Bool("is_default").Comment("是否为默认赛制。"),
		field.Bool("enabled").Comment("是否允许新的业务数据引用该实时资料。"),
		field.Int64("version").Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_battle_format 的表名、注释、复合主键和检查约束。
func (GameBattleFormat) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料：对战赛制。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_battle_format", Checks: map[string]string{
			"game_battle_format_check":                  "select_count >= 1 AND select_count <= roster_count",
			"game_battle_format_check1":                 "active_participants_per_side >= 1 AND active_participants_per_side <= select_count",
			"game_battle_format_check2":                 "mode::text = 'single'::text AND active_participants_per_side = 1 OR mode::text = 'double'::text AND active_participants_per_side = 2",
			"game_battle_format_check3":                 "level_rule::text = 'preserve'::text AND normalized_level IS NULL OR level_rule::text = 'normalize'::text AND normalized_level IS NOT NULL",
			"game_battle_format_check4":                 "challenge_available OR training_available OR encounter_available OR admin_preview_available",
			"game_battle_format_check5":                 "cardinality(clause_ids) <= 50 AND cardinality(restriction_ids) <= 50 AND cardinality(mechanic_ids) <= 50",
			"game_battle_format_check6":                 "is_default = (code::text = 'standard-single'::text)",
			"game_battle_format_code_check":             "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"game_battle_format_description_check":      "description::text = btrim(description::text)",
			"game_battle_format_level_rule_check":       "level_rule::text = ANY (ARRAY['preserve'::character varying::text, 'normalize'::character varying::text])",
			"game_battle_format_battle_seconds_check":   "battle_seconds >= 60 AND battle_seconds <= 14400",
			"game_battle_format_mode_check":             "mode::text = ANY (ARRAY['single'::character varying::text, 'double'::character varying::text])",
			"game_battle_format_name_check":             "char_length(name::text) >= 1 AND char_length(name::text) <= 80 AND name::text = btrim(name::text)",
			"game_battle_format_normalized_level_check": "normalized_level >= 1 AND normalized_level <= 100",
			"game_battle_format_preview_seconds_check":  "preview_seconds >= 10 AND preview_seconds <= 600",
			"game_battle_format_roster_count_check":     "roster_count >= 1 AND roster_count <= 6",
			"game_battle_format_turn_seconds_check":     "turn_seconds >= 10 AND turn_seconds <= 600",
			"game_battle_format_version_check":          "version > 0",
		}},
	}
}
