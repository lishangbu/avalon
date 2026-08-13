package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItem 定义 game_item 表的持久化结构。
type GameItem struct {
	ent.Schema
}

// Fields 返回 game_item 表全部字段及其数据库约束。
func (GameItem) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该资料记录的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("该资料记录的全局唯一稳定编码。"),
		field.String("name").MaxLen(120).Comment("该资料记录的简体中文显示名称。"),
		field.String("name_ja").MaxLen(120).Optional().Nillable().Comment("该资料记录的 name ja 业务属性。"),
		field.String("name_en").MaxLen(120).Optional().Nillable().Comment("该资料记录的 name en 业务属性。"),
		field.String("description").Optional().Nillable().Comment("该资料记录的简体中文说明。"),
		field.String("effect").Optional().Nillable().Comment("该道具投掷或使用时的完整效果说明。"),
		field.String("short_effect").Optional().Nillable().Comment("该道具投掷或使用时的简短效果说明。"),
		field.String("icon_filename").MaxLen(255).Optional().Nillable().Comment("该资料记录的 icon filename 业务属性。"),
		field.Int64("asset_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("该资料记录的 asset id 业务属性。"),
		field.String("usage_type").MaxLen(32).Comment("该资料记录的 usage type 业务属性。"),
		field.Int64("category_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("关联分类资料的稳定 Identifier。"),
		field.Int64("fling_effect_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("该道具可选的投掷效果稳定 Identifier。"),
		field.Int32("cost").Comment("该资料记录的 cost 业务属性。"),
		field.Int32("fling_power").Optional().Nillable().Comment("该资料记录的 fling power 业务属性。"),
		field.Bool("enabled").Comment("是否允许新的业务数据引用该实时资料。"),
		field.Int64("version").Comment("该资料记录的乐观并发控制版本。"),
		field.Time("created_at").Comment("该资料记录的创建时间。"),
		field.Time("updated_at").Comment("该资料记录的最后更新时间。"),
	}
}

// Annotations 固定 game_item 的表名、注释、复合主键和检查约束。
func (GameItem) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("实时游戏资料：道具。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "game_item", Checks: map[string]string{
			"game_item_code_check":          "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"game_item_cost_check":          "cost >= 0",
			"game_item_description_check":   "description IS NULL OR char_length(description) >= 1 AND char_length(description) <= 4000 AND description = btrim(description)",
			"game_item_effect_check":        "effect IS NULL OR char_length(effect) >= 1 AND char_length(effect) <= 4000 AND effect = btrim(effect)",
			"game_item_short_effect_check":  "short_effect IS NULL OR char_length(short_effect) >= 1 AND char_length(short_effect) <= 500 AND short_effect = btrim(short_effect)",
			"game_item_fling_power_check":   "fling_power >= 0",
			"game_item_icon_filename_check": "icon_filename IS NULL OR char_length(icon_filename::text) >= 1 AND char_length(icon_filename::text) <= 255 AND icon_filename::text = btrim(icon_filename::text)",
			"game_item_name_check":          "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"game_item_name_en_check":       "name_en IS NULL OR char_length(name_en::text) >= 1 AND char_length(name_en::text) <= 120 AND name_en::text = btrim(name_en::text)",
			"game_item_name_ja_check":       "name_ja IS NULL OR char_length(name_ja::text) >= 1 AND char_length(name_ja::text) <= 120 AND name_ja::text = btrim(name_ja::text)",
			"game_item_usage_type_check":    "usage_type::text = ANY (ARRAY['held'::character varying::text, 'battle_consumable'::character varying::text, 'capture'::character varying::text, 'evolution'::character varying::text, 'training'::character varying::text, 'key'::character varying::text, 'material'::character varying::text, 'catalog'::character varying::text])",
			"game_item_version_check":       "version > 0",
		}},
	}
}
