package schema

import (
	"encoding/json"
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// BattleAnalyticsProjection 定义 battle_analytics_projection 表的持久化结构。
type BattleAnalyticsProjection struct {
	ent.Schema
}

// Fields 返回 battle_analytics_projection 表全部字段及其数据库约束。
func (BattleAnalyticsProjection) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("battle_analytics_projection 表的雪花主键。"),
		field.String("projection_key").MaxLen(128).Comment("battle_analytics_projection 表的投影稳定键。"),
		field.Int64("projection_version").Comment("battle_analytics_projection 表的 projection_version 字段。"),
		field.JSON("payload", json.RawMessage{}).Comment("battle_analytics_projection 表的 payload 字段。"),
		field.Time("refreshed_at").Comment("battle_analytics_projection 表的 refreshed_at 字段。"),
	}
}

// Indexes 返回投影稳定键的唯一索引。
func (BattleAnalyticsProjection) Indexes() []ent.Index {
	return []ent.Index{index.Fields("projection_key").Unique().StorageKey("uk_battle_analytics_projection_projection_key")}
}

// Annotations 固定 battle_analytics_projection 的表名、注释、复合主键和检查约束。
func (BattleAnalyticsProjection) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("可从权威 Battle 与 Turn Record 幂等重建的只读分析投影。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_analytics_projection", Checks: map[string]string{
			"battle_analytics_projection_key_check":     "char_length(projection_key::text) >= 1 AND char_length(projection_key::text) <= 128",
			"battle_analytics_projection_payload_check": "jsonb_typeof(payload) = 'object'::text",
			"battle_analytics_projection_version_check": "projection_version >= 0",
		}},
	}
}
