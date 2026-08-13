package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// GameItemWeatherRule 定义道具天气与场地持续规则的一对一关系记录。
type GameItemWeatherRule struct{ ent.Schema }

// Fields 返回天气规则的强类型字段。
func (GameItemWeatherRule) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("规则稳定 Identifier。"), field.Int64("item_id").GoType(snowflake.ID(0)).Positive().Comment("道具稳定 Identifier。"),
		field.Int32("reflect_turns_remaining").Comment("反射壁持续回合。"), field.Int32("light_screen_turns_remaining").Comment("光墙持续回合。"), field.Int32("aurora_veil_turns_remaining").Comment("极光幕持续回合。"), field.Int32("rain_turns_remaining").Comment("降雨持续回合。"), field.Int32("sandstorm_turns_remaining").Comment("沙暴持续回合。"), field.Int32("snow_turns_remaining").Comment("降雪持续回合。"), field.Int32("sun_turns_remaining").Comment("日照持续回合。"), field.Int32("terrain_turns_remaining").Comment("场地持续回合。"), field.Bool("sandstorm_damage_immunity").Comment("是否免疫沙暴伤害。"),
		field.Int64("version").Comment("乐观并发版本。"), field.Time("created_at").Comment("创建时间。"), field.Time("updated_at").Comment("更新时间。"),
	}
}

// Indexes 返回每个道具最多一条天气规则的唯一约束。
func (GameItemWeatherRule) Indexes() []ent.Index {
	return []ent.Index{index.Fields("item_id").Unique().StorageKey("uk_game_item_weather_rule_item_id")}
}

// Annotations 固定天气规则表及版本约束。
func (GameItemWeatherRule) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("实时游戏资料：道具天气规则。"), entsql.WithComments(true), entsql.Annotation{Table: "game_item_weather_rule", Checks: map[string]string{"game_item_weather_rule_version_check": "version > 0", "game_item_weather_rule_turns_check": "reflect_turns_remaining >= 0 AND light_screen_turns_remaining >= 0 AND aurora_veil_turns_remaining >= 0 AND rain_turns_remaining >= 0 AND sandstorm_turns_remaining >= 0 AND snow_turns_remaining >= 0 AND sun_turns_remaining >= 0 AND terrain_turns_remaining >= 0"}}}
}
