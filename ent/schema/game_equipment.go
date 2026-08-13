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

// GameEquipment 定义以 Item Catalog Entry 为展示身份的 PlayerCharacter 装备资料。
type GameEquipment struct{ ent.Schema }

// Fields 返回装备槽位、手持规则、资格、售价与规范化被动规则字段。
func (GameEquipment) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("Equipment Catalog Entry 的稳定 Snowflake Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("负责装备展示、目录和获取入口的 Item Catalog Entry Identifier。"),
		field.String("slot_type").MaxLen(16).Comment("装备允许进入的闭集资料槽位类型。"),
		field.String("handedness").MaxLen(16).Optional().Nillable().Comment("手部装备的一手、双手或副手闭集身份；非手部装备必须为空。"),
		field.Int32("minimum_level").Default(1).Comment("PlayerCharacter 穿戴该装备所需的最低等级。"),
		field.Int64("sell_currency_id").GoType(snowflake.ID(0)).Positive().Comment("出售该 Equipment Instance 时入账的 Currency Identifier。"),
		field.Int64("sell_price").Comment("未穿戴 Equipment Instance 的基础出售价格。"),
		field.JSON("rules", json.RawMessage{}).Default(json.RawMessage(`{}`)).Comment("由 Equipment Rules 编译器规范化的强类型被动规则文档。"),
		field.Bool("enabled").Default(false).Comment("是否允许新的获取与穿戴；停用不强制卸下既有实例。"),
		field.Int64("version").Default(1).Comment("装备资料及全部关系原子保存使用的乐观版本。"),
		field.Time("created_at").Comment("装备资料首次创建的 UTC 时间。"),
		field.Time("updated_at").Comment("装备资料最近一次业务更新的 UTC 时间。"),
	}
}

// Indexes 固定一个 Item Catalog Entry 至多对应一条装备资料。
func (GameEquipment) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id").Unique().StorageKey("uk_game_equipment_item_id")}
}

// Annotations 固定装备资料的数据库闭集与首期规则启用边界。
func (GameEquipment) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("PlayerCharacter 多槽装备的规范实时资料。"), entsql.WithComments(true), entsql.Annotation{Table: "game_equipment", Checks: map[string]string{
		"game_equipment_slot_type_check":  "slot_type IN ('main_hand','off_hand','head','body','hands','feet','accessory')",
		"game_equipment_handedness_check": "(slot_type IN ('main_hand','off_hand') AND handedness IN ('one_handed','two_handed','off_hand')) OR (slot_type NOT IN ('main_hand','off_hand') AND handedness IS NULL)",
		"game_equipment_level_check":      "minimum_level > 0",
		"game_equipment_sell_price_check": "sell_price >= 0",
		"game_equipment_rules_check":      "jsonb_typeof(rules) = 'object' AND (NOT enabled OR rules = '{}'::jsonb)",
		"game_equipment_version_check":    "version > 0",
		"game_equipment_time_check":       "updated_at >= created_at",
	}}}
}
