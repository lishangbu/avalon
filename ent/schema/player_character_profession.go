package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterProfession 定义 player_character_profession 表的持久化结构。
type PlayerCharacterProfession struct {
	ent.Schema
}

// Fields 返回 player_character_profession 表全部字段及其数据库约束。
func (PlayerCharacterProfession) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("玩家职业进度所属 PlayerCharacter 的稳定 Identifier。"),
		field.Int64("profession_id").GoType(snowflake.ID(0)).Positive().Comment("玩家职业进度所属或引用的职业稳定 Identifier。"),
		field.Int32("level").Comment("玩家职业进度当前的正整数成长等级。"),
		field.Int64("experience").Comment("玩家职业进度累计获得的非负经验值。"),
		field.Bool("active").Default(true).Comment("该职业当前是否参与 PlayerCharacter 装备资格与职业规则判定；停用不删除成长进度。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("玩家职业进度写入使用的正整数乐观并发版本。"),
		field.Time("updated_at").Comment("玩家职业进度最近一次业务更新的 UTC 时间。"),
	}
}

// Indexes 返回 player_character_profession 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterProfession) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_id", "profession_id").Unique().StorageKey("uk_player_character_profession_player_character_id_pro_99b276e1"),
	}
}

// Annotations 固定 player_character_profession 的表名、注释、复合主键和检查约束。
func (PlayerCharacterProfession) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 在一个职业中的等级与累计经验。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_profession", Checks: map[string]string{
			"player_character_profession_experience_check": "experience >= 0",
			"player_character_profession_level_check":      "level > 0",
			"player_character_profession_version_check":    "version > 0",
		}},
	}
}
