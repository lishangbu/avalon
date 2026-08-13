package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemAttributeBinding 定义道具与 Attribute 的多对多关系。
type GameItemAttributeBinding struct{ ent.Schema }

// Fields 返回关系表字段。
func (GameItemAttributeBinding) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("关系稳定 Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("道具稳定 Identifier。"),
		field.Int64("attribute_id").GoType(snowflake.ID(0)).Positive().Comment("Attribute 稳定 Identifier。"),
		field.Int64("version").Comment("乐观并发版本。"),
		field.Time("created_at").Comment("创建时间。"),
		field.Time("updated_at").Comment("更新时间。"),
	}
}

// Indexes 返回关系唯一约束。
func (GameItemAttributeBinding) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id", "attribute_id").Unique().StorageKey("uk_game_item_attribute_binding_item_id_attribute_id")}
}

// Annotations 固定关系表名。
func (GameItemAttributeBinding) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具 Attribute 关系。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_attribute_binding"}}
}
