package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgMapProjection 定义 World Topology 的独立展示布局版本。
type RpgMapProjection struct {
	ent.Schema
}

// Fields 返回 Projection 的稳定身份、版本和启用状态。
func (RpgMapProjection) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("地图展示 Projection 的稳定 Identifier。"),
		field.String("code").MaxLen(64).Comment("Projection 的全局稳定机器编码。"),
		field.String("name").MaxLen(120).Comment("Projection 的简体中文名称。"),
		field.Int64("layout_version").Annotations(entsql.DefaultExpr("1")).Comment("展示布局的正整数版本。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("false")).Comment("该布局版本是否为当前启用展示版本。"),
		field.Time("created_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("Projection 首次创建时间。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("Projection 最近更新时间。"),
	}
}

// Indexes 返回 Projection 编码和启用查询约束。
func (RpgMapProjection) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("code", "layout_version").Unique().StorageKey("uk_rpg_map_projection_code_layout_version"),
		index.Fields("enabled").StorageKey("idx_rpg_map_projection_enabled"),
	}
}

// Annotations 固定 Projection 表名和版本约束。
func (RpgMapProjection) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("World Topology 的纯展示布局，不参与可达性和距离规则。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_map_projection", Checks: map[string]string{
			"rpg_map_projection_code_check":    "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"rpg_map_projection_name_check":    "char_length(name::text) >= 1 AND char_length(name::text) <= 120 AND name::text = btrim(name::text)",
			"rpg_map_projection_time_check":    "updated_at >= created_at",
			"rpg_map_projection_version_check": "layout_version > 0",
		}},
	}
}
