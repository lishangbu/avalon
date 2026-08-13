package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterEquipmentInstance 定义 PlayerCharacter 实际拥有的一件不可堆叠装备资产。
type PlayerCharacterEquipmentInstance struct{ ent.Schema }

// Fields 返回装备实例归属、资料来源、幂等来源引用和乐观版本事实。
func (PlayerCharacterEquipmentInstance) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("Equipment Instance 的稳定 Snowflake Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("拥有该 Equipment Instance 的 PlayerCharacter Identifier。"),
		field.Int64("equipment_id").GoType(snowflake.ID(0)).Positive().Comment("该实例引用的 Equipment Catalog Entry Identifier。"),
		field.String("source_type").MaxLen(16).Comment("shop、quest、loot 或 admin 的闭集获取来源。"),
		field.Int64("source_reference_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("支付、奖励、掉落或管理操作事实的可选稳定 Identifier。"),
		field.Int64("version").Default(1).Comment("实例出售等资产写入使用的乐观版本。"),
		field.Time("sold_at").Optional().Nillable().Comment("实例被权威出售的 UTC 终态时间；非空后不再属于可用装备库存。"),
		field.Time("acquired_at").Comment("该实例归属当前 PlayerCharacter 的 UTC 时间。"),
		field.Time("updated_at").Comment("该实例最近一次业务更新的 UTC 时间。"),
	}
}

// Indexes 支持按角色、装备和来源诊断实例。
func (PlayerCharacterEquipmentInstance) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_id", "equipment_id").StorageKey("idx_player_character_equipment_instance_player_character_id_equipment_id"),
		index.Fields("source_type", "source_reference_id").StorageKey("idx_player_character_equipment_instance_source_type_source_reference_id"),
	}
}

// Annotations 固定装备实例来源、版本与时间约束。
func (PlayerCharacterEquipmentInstance) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("PlayerCharacter 实际拥有且不可堆叠的一件 Equipment Instance。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_equipment_instance", Checks: map[string]string{
		"player_character_equipment_instance_source_check":  "source_type IN ('shop','quest','loot','admin')",
		"player_character_equipment_instance_version_check": "version > 0",
		"player_character_equipment_instance_time_check":    "updated_at >= acquired_at AND (sold_at IS NULL OR sold_at >= acquired_at AND updated_at >= sold_at)",
	}}}
}
