package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterEquipmentLoadoutState 定义 PlayerCharacter 整套 Loadout 的单一并发版本。
type PlayerCharacterEquipmentLoadoutState struct{ ent.Schema }

// Fields 以 PlayerCharacter Identifier 作为一对一 Loadout 状态身份。
func (PlayerCharacterEquipmentLoadoutState) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("PlayerCharacter Identifier，同时也是唯一 Loadout State 身份。"),
		field.Int64("version").Default(1).Comment("整套 ReplaceLoadout 命令使用的单一乐观版本。"),
		field.Time("updated_at").Comment("Loadout 最近一次整体替换的 UTC 时间。"),
	}
}

// Annotations 固定 Loadout State 表和正版本约束。
func (PlayerCharacterEquipmentLoadoutState) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("PlayerCharacter 当前 Equipment Loadout 的单一版本化状态。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_equipment_loadout_state", Checks: map[string]string{"player_character_equipment_loadout_state_version_check": "version > 0"}}}
}
