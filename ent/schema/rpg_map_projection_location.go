package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// RpgMapProjectionLocation 保存一个 Location 在某个展示布局中的纯展示位置。
type RpgMapProjectionLocation struct {
	ent.Schema
}

// Fields 返回展示坐标和可选 Asset 引用。
func (RpgMapProjectionLocation) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()),
		field.Int64("projection_id").GoType(snowflake.ID(0)).Positive().Comment("所属地图 Projection 的稳定 Identifier。"),
		field.Int64("location_id").GoType(snowflake.ID(0)).Positive().Comment("被展示的 Location 稳定 Identifier。"),
		field.Int32("x").Comment("展示坐标 X，不参与距离计算。"),
		field.Int32("y").Comment("展示坐标 Y，不参与距离计算。"),
		field.Int32("z").Default(0).Comment("展示坐标 Z 层级，不参与可达性计算。"),
		field.Int64("icon_asset_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("可选地点图标 Asset 稳定 Identifier。"),
		field.Int64("background_asset_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("可选地点背景 Asset 稳定 Identifier。"),
	}
}

// Indexes 返回 Projection 与 Location 的唯一布局关系。
func (RpgMapProjectionLocation) Indexes() []ent.Index {
	return []ent.Index{index.Fields("projection_id", "location_id").Unique().StorageKey("uk_rpg_map_projection_location_projection_id_location_id")}
}

// Annotations 固定 Projection Location 关系表和展示坐标约束。
func (RpgMapProjectionLocation) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("Location 在 World Map Projection 中的纯展示坐标和 Asset 引用。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "rpg_map_projection_location"},
	}
}
