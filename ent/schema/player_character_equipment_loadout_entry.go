package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterEquipmentLoadoutEntry 定义 Loadout 中一个固定槽位与一个 Equipment Instance 的关系。
type PlayerCharacterEquipmentLoadoutEntry struct{ ent.Schema }

// Fields 返回独立关系身份、角色、固定槽位、实例和穿戴时间。
func (PlayerCharacterEquipmentLoadoutEntry) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("Loadout Entry 的稳定 Snowflake Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("该槽位所属 PlayerCharacter Identifier。"),
		field.String("slot").MaxLen(16).Comment("main_hand、off_hand、head、body、hands、feet、accessory_1 或 accessory_2 固定槽位。"),
		field.Int64("equipment_instance_id").GoType(snowflake.ID(0)).Positive().Comment("当前穿戴在该槽位的 Equipment Instance Identifier。"),
		field.Time("equipped_at").Comment("该实例进入当前槽位的 UTC 时间。"),
	}
}

// Indexes 保证一个角色每槽至多一件装备，且同一实例全局至多穿戴一次。
func (PlayerCharacterEquipmentLoadoutEntry) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_id", "slot").Unique().StorageKey("uk_player_character_equipment_loadout_entry_player_character_id_slot"),
		index.Fields("equipment_instance_id").Unique().StorageKey("uk_player_character_equipment_loadout_entry_equipment_instance_id"),
	}
}

// Annotations 固定 Loadout 的槽位闭集。
func (PlayerCharacterEquipmentLoadoutEntry) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("PlayerCharacter Equipment Loadout 的当前槽位关系。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_equipment_loadout_entry", Checks: map[string]string{"player_character_equipment_loadout_entry_slot_check": "slot IN ('main_hand','off_hand','head','body','hands','feet','accessory_1','accessory_2')"}}}
}
