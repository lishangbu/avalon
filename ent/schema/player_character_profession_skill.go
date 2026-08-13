package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterProfessionSkill 定义 player_character_profession_skill 表的持久化结构。
type PlayerCharacterProfessionSkill struct {
	ent.Schema
}

// Fields 返回 player_character_profession_skill 表全部字段及其数据库约束。
func (PlayerCharacterProfessionSkill) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("玩家已解锁职业技能所属 PlayerCharacter 的稳定 Identifier。"),
		field.Int64("profession_id").GoType(snowflake.ID(0)).Positive().Comment("玩家已解锁职业技能所属或引用的职业稳定 Identifier。"),
		field.Int64("profession_skill_id").GoType(snowflake.ID(0)).Positive().Comment("玩家已解锁职业技能引用的职业技能稳定 Identifier。"),
		field.Time("unlocked_at").Comment("玩家已解锁职业技能实际解锁并成为玩家事实的 UTC 时间。"),
	}
}

// Indexes 返回 player_character_profession_skill 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterProfessionSkill) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_id", "profession_id", "profession_skill_id").Unique().StorageKey("uk_player_character_profession_skill_player_character__ca6b7fca"),
	}
}

// Annotations 固定 player_character_profession_skill 的表名、注释、复合主键和检查约束。
func (PlayerCharacterProfessionSkill) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 已实际解锁的职业技能事实。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_profession_skill"},
	}
}
