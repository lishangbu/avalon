package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemStatusRule 定义道具异常状态与限制规则的一对一关系记录。
type GameItemStatusRule struct{ ent.Schema }

// Fields 返回异常规则的强类型字段。
func (GameItemStatusRule) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("规则稳定 Identifier。"),
		field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("道具稳定 Identifier。"),
		field.Bool("cures_paralysis").Comment("是否治愈麻痹。"), field.Bool("cures_sleep").Comment("是否治愈睡眠。"), field.Bool("cures_poison").Comment("是否治愈中毒。"), field.Bool("cures_burn").Comment("是否治愈灼伤。"), field.Bool("cures_freeze").Comment("是否治愈冰冻。"), field.Bool("cures_all_major_statuses").Comment("是否治愈全部主要异常。"), field.Bool("cures_confusion").Comment("是否治愈混乱。"), field.Bool("powder_skill_immunity").Comment("是否免疫粉末技能。"), field.Bool("status_skill_restriction").Comment("是否限制状态技能。"), field.Bool("damaging_skill_secondary_effect_immunity").Comment("是否免疫伤害技能追加效果。"),
		field.Int64("version").Comment("乐观并发版本。"), field.Time("created_at").Comment("创建时间。"), field.Time("updated_at").Comment("更新时间。"),
	}
}

// Indexes 返回每个道具最多一条状态规则的唯一约束。
func (GameItemStatusRule) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id").Unique().StorageKey("uk_game_item_status_rule_item_id")}
}

// Annotations 固定状态规则表及版本约束。
func (GameItemStatusRule) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具状态规则。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_status_rule", Checks: map[string]string{"game_item_status_rule_version_check": "version > 0"}}}
}
