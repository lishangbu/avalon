package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameEquipmentProfession 定义 Equipment Catalog Entry 的职业白名单关系。
type GameEquipmentProfession struct{ ent.Schema }

// Fields 返回装备与允许职业的稳定引用。
func (GameEquipmentProfession) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("装备职业白名单关系的稳定 Snowflake Identifier。"),
		field.Int64("equipment_id").GoType(snowflake.ID(0)).Positive().Comment("Equipment Catalog Entry Identifier。"),
		field.Int64("profession_id").GoType(snowflake.ID(0)).Positive().Comment("允许穿戴该装备的 Profession Identifier。"),
	}
}

// Indexes 防止同一装备重复声明同一职业。
func (GameEquipmentProfession) Indexes() []ent.Index {
	return []ent.Index{index.Fields("equipment_id", "profession_id").Unique().StorageKey("uk_game_equipment_profession_equipment_id_profession_id")}
}

// Annotations 固定装备职业白名单表名。
func (GameEquipmentProfession) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("Equipment Catalog Entry 允许穿戴的 Profession 白名单；没有记录表示通用。"), entsql.WithComments(true), entsql.Annotation{Table: "game_equipment_profession"}}
}
