package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterCreature 定义 player_character_creature 表的持久化结构。
type PlayerCharacterCreature struct {
	ent.Schema
}

// Fields 返回 player_character_creature 表全部字段及其数据库约束。
func (PlayerCharacterCreature) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("玩家拥有 Creature记录的稳定 Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("玩家拥有 Creature所属 PlayerCharacter 的稳定 Identifier。"),
		field.Int64("creature_id").GoType(snowflake.ID(0)).Positive().Comment("玩家拥有 Creature引用的 Creature 资料稳定 Identifier。"),
		field.Int64("form_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("玩家拥有 Creature引用的可选 Creature Form 稳定 Identifier。"),
		field.Int64("gender_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("玩家拥有 Creature引用的可选性别资料稳定 Identifier。"),
		field.Int64("skin_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("玩家拥有 Creature引用的可选 Creature 皮肤稳定 Identifier。"),
		field.Int64("ability_id").GoType(snowflake.ID(0)).Positive().Comment("玩家拥有 Creature引用的特性稳定 Identifier。"),
		field.Int64("nature_id").GoType(snowflake.ID(0)).Positive().Comment("玩家拥有 Creature引用的 Nature 稳定 Identifier。"),
		field.Int64("held_item_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("玩家拥有 Creature当前携带的可选道具稳定 Identifier。"),
		field.Int64("origin_location_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("玩家拥有 Creature首次获得时记录的可选 RPG 地点稳定 Identifier。"),
		field.Int64("captured_with_item_id").GoType(snowflake.ID(0)).Positive().Optional().Nillable().Comment("玩家拥有 Creature捕获时使用的可选捕获道具稳定 Identifier。"),
		field.String("nickname").MaxLen(120).Optional().Nillable().Comment("玩家拥有 Creature由玩家设置的可选昵称。"),
		field.String("origin_type").MaxLen(16).Comment("玩家拥有 Creature捕获、孵化、奖励、交易或管理授予的稳定来源类型。"),
		field.Int16("level").Comment("玩家拥有 Creature当前的正整数成长等级。"),
		field.Int64("experience").Comment("玩家拥有 Creature累计获得的非负经验值。"),
		field.Int32("current_hp").Comment("玩家拥有 Creature离开战斗后持久化的非负当前生命值。"),
		field.Int16("friendship").Comment("玩家拥有 Creature零至二百五十五的亲密度。"),
		field.Bool("is_egg").Annotations(entsql.DefaultExpr("false")).Comment("玩家拥有 Creature当前是否仍处于未孵化蛋状态。"),
		field.Int32("hatch_progress").Annotations(entsql.DefaultExpr("0")).Comment("玩家拥有 Creature已累计的非负孵化进度。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("玩家拥有 Creature写入使用的正整数乐观并发版本。"),
		field.Time("acquired_at").Comment("玩家拥有 Creature归属当前 PlayerCharacter 的 UTC 时间。"),
		field.Time("updated_at").Comment("玩家拥有 Creature最近一次业务更新的 UTC 时间。"),
	}
}

// Annotations 固定 player_character_creature 的表名、注释、复合主键和检查约束。
func (PlayerCharacterCreature) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 实际拥有且持续成长的 Creature 实例。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_creature", Checks: map[string]string{
			"player_character_creature_experience_check":     "experience >= 0",
			"player_character_creature_friendship_check":     "friendship >= 0 AND friendship <= 255",
			"player_character_creature_hatch_progress_check": "hatch_progress >= 0",
			"player_character_creature_hp_check":             "current_hp >= 0",
			"player_character_creature_level_check":          "level >= 1 AND level <= 100",
			"player_character_creature_nickname_check":       "nickname IS NULL OR char_length(nickname::text) >= 1 AND char_length(nickname::text) <= 120 AND nickname::text = btrim(nickname::text)",
			"player_character_creature_origin_type_check":    "origin_type::text = ANY (ARRAY['capture'::character varying, 'hatch'::character varying, 'reward'::character varying, 'trade'::character varying, 'admin'::character varying]::text[])",
			"player_character_creature_time_check":           "updated_at >= acquired_at",
			"player_character_creature_version_check":        "version > 0",
		}},
	}
}
