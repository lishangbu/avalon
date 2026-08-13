package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// Asset 定义 asset 表的持久化结构。
type Asset struct {
	ent.Schema
}

// Fields 返回 asset 表全部字段及其数据库约束。
func (Asset) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("asset 表的 id 字段。"),
		field.Int64("owner_account_id").GoType(snowflake.ID(0)).Positive().Comment("asset 表的 owner_account_id 字段。"),
		field.String("object_key").MaxLen(200).Comment("asset 表的 object_key 字段。"),
		field.String("status").MaxLen(20).Comment("asset 表的 status 字段。"),
		field.String("media_type").MaxLen(100).Comment("asset 表的 media_type 字段。"),
		field.Int64("expected_size").Comment("asset 表的 expected_size 字段。"),
		field.Bytes("expected_sha256").Comment("asset 表的 expected_sha256 字段。"),
		field.Int64("actual_size").Optional().Nillable().Comment("asset 表的 actual_size 字段。"),
		field.Bytes("actual_sha256").Optional().Nillable().Comment("asset 表的 actual_sha256 字段。"),
		field.Int32("width").Optional().Nillable().Comment("asset 表的 width 字段。"),
		field.Int32("height").Optional().Nillable().Comment("asset 表的 height 字段。"),
		field.Int64("version").Comment("asset 表的 version 字段。"),
		field.Time("created_at").Comment("asset 表的 created_at 字段。"),
		field.Time("ready_at").Optional().Nillable().Comment("asset 表的 ready_at 字段。"),
	}
}

// Annotations 固定 asset 的表名、注释、复合主键和检查约束。
func (Asset) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 asset 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "asset", Checks: map[string]string{
			"asset_actual_sha256_check":   "actual_sha256 IS NULL OR octet_length(actual_sha256) = 32",
			"asset_actual_size_check":     "actual_size IS NULL OR actual_size > 0 AND actual_size <= 10485760",
			"asset_check":                 "status::text = 'pending'::text AND actual_size IS NULL AND actual_sha256 IS NULL AND width IS NULL AND height IS NULL AND ready_at IS NULL OR status::text = 'ready'::text AND actual_size IS NOT NULL AND actual_sha256 IS NOT NULL AND width IS NOT NULL AND height IS NOT NULL AND ready_at IS NOT NULL",
			"asset_check1":                "actual_size IS NULL OR actual_size = expected_size",
			"asset_check2":                "actual_sha256 IS NULL OR actual_sha256 = expected_sha256",
			"asset_check3":                "width IS NULL OR height IS NULL OR (width::bigint * height::bigint) <= 16000000",
			"asset_check4":                "ready_at IS NULL OR ready_at >= created_at",
			"asset_expected_sha256_check": "octet_length(expected_sha256) = 32",
			"asset_expected_size_check":   "expected_size > 0 AND expected_size <= 10485760",
			"asset_height_check":          "height IS NULL OR height > 0 AND height <= 8192",
			"asset_media_type_check":      "media_type::text = ANY (ARRAY['image/jpeg'::character varying::text, 'image/png'::character varying::text, 'image/webp'::character varying::text])",
			"asset_status_check":          "status::text = ANY (ARRAY['pending'::character varying::text, 'ready'::character varying::text])",
			"asset_version_check":         "version > 0",
			"asset_width_check":           "width IS NULL OR width > 0 AND width <= 8192",
		}},
	}
}
