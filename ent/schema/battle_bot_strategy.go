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

// BattleBotStrategy 定义 battle_bot_strategy 表的持久化结构。
type BattleBotStrategy struct {
	ent.Schema
}

// Fields 返回 battle_bot_strategy 表全部字段及其数据库约束。
func (BattleBotStrategy) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.String("code").MaxLen(64).Comment("battle_bot_strategy 表的 code 字段。"),
		field.Int32("version").Comment("battle_bot_strategy 表的 version 字段。"),
		field.Bool("enabled").Annotations(entsql.DefaultExpr("true")).Comment("battle_bot_strategy 表的 enabled 字段。"),
		field.JSON("definition", json.RawMessage{}).Comment("battle_bot_strategy 表的 definition 字段。"),
		field.Time("created_at").Comment("battle_bot_strategy 表的 created_at 字段。"),
	}
}

// Indexes 返回 battle_bot_strategy 原复合主键对应的稳定业务唯一约束。
func (BattleBotStrategy) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("code", "version").Unique().StorageKey("uk_battle_bot_strategy_code_version"),
	}
}

// Annotations 固定 battle_bot_strategy 的表名、注释、复合主键和检查约束。
func (BattleBotStrategy) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("按代码和版本冻结的 Bot 策略配置，不影响已创建 Battle 的策略快照。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "battle_bot_strategy", Checks: map[string]string{
			"battle_bot_strategy_code_check":       "code::text ~ '^[a-z][a-z0-9-]{1,63}$'::text",
			"battle_bot_strategy_definition_check": "jsonb_typeof(definition) = 'object'::text",
			"battle_bot_strategy_version_check":    "version >= 1",
		}},
	}
}
