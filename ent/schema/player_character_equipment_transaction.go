package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterEquipmentTransaction 定义一件 Equipment Instance 的不可变资产动作流水。
type PlayerCharacterEquipmentTransaction struct{ ent.Schema }

// Fields 返回实例、动作、来源、槽位、共同 Operation Identifier 和提交时间。
func (PlayerCharacterEquipmentTransaction) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("Equipment Transaction 的稳定 Snowflake Identifier。"),
		field.Int64("operation_id").GoType(snowflake.ID(0)).Positive().Comment("同一获取、换装或出售命令共享的 Operation Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("资产流水所属 PlayerCharacter Identifier。"),
		field.Int64("equipment_instance_id").GoType(snowflake.ID(0)).Positive().Comment("发生动作的 Equipment Instance Identifier。"),
		field.String("action").MaxLen(16).Comment("acquire、equip、unequip 或 sell 的闭集资产动作。"),
		field.String("source_type").MaxLen(16).Optional().Nillable().Comment("acquire 动作的 shop、quest、loot 或 admin 来源。"),
		field.String("slot").MaxLen(16).Optional().Nillable().Comment("equip 或 unequip 动作涉及的固定 Loadout 槽位。"),
		field.Time("created_at").Comment("该命令统一的 UTC 提交时间。"),
	}
}

// Indexes 支持按角色时间、实例和 Operation 查询不可变流水。
func (PlayerCharacterEquipmentTransaction) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_id", "created_at", "id").StorageKey("idx_player_character_equipment_transaction_player_character_id_created_at_id"),
		index.Fields("equipment_instance_id", "created_at").StorageKey("idx_player_character_equipment_transaction_equipment_instance_id_created_at"),
		index.Fields("operation_id", "id").StorageKey("idx_player_character_equipment_transaction_operation_id_id"),
	}
}

// Annotations 固定动作字段之间的语义组合，阻止不完整流水。
func (PlayerCharacterEquipmentTransaction) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("Equipment Instance 获取、穿戴、卸下与出售的不可变资产流水。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_equipment_transaction", Checks: map[string]string{
		"player_character_equipment_transaction_action_check": "action IN ('acquire','equip','unequip','sell')",
		"player_character_equipment_transaction_source_check": "(action = 'acquire' AND source_type IN ('shop','quest','loot','admin')) OR (action <> 'acquire' AND source_type IS NULL)",
		"player_character_equipment_transaction_slot_check":   "(action IN ('equip','unequip') AND slot IN ('main_hand','off_hand','head','body','hands','feet','accessory_1','accessory_2')) OR (action NOT IN ('equip','unequip') AND slot IS NULL)",
	}}}
}
