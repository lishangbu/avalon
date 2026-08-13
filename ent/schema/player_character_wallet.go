package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterWallet 定义 player_character_wallet 表的持久化结构。
type PlayerCharacterWallet struct {
	ent.Schema
}

// Fields 返回 player_character_wallet 表全部字段及其数据库约束。
func (PlayerCharacterWallet) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("该关联记录的稳定 Snowflake Identifier。"),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("玩家钱包余额所属 PlayerCharacter 的稳定 Identifier。"),
		field.Int64("currency_id").GoType(snowflake.ID(0)).Positive().Comment("玩家钱包余额使用或变更的游戏货币稳定 Identifier。"),
		field.Int64("balance").Comment("玩家钱包余额事务完成后的当前非负余额。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("玩家钱包余额写入使用的正整数乐观并发版本。"),
		field.Time("updated_at").Comment("玩家钱包余额最近一次业务更新的 UTC 时间。"),
	}
}

// Indexes 返回 player_character_wallet 原复合主键对应的稳定业务唯一约束。
func (PlayerCharacterWallet) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_id", "currency_id").Unique().StorageKey("uk_player_character_wallet_player_character_id_currency_id"),
	}
}

// Annotations 固定 player_character_wallet 的表名、注释、复合主键和检查约束。
func (PlayerCharacterWallet) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("PlayerCharacter 按货币保存的当前非负余额。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_wallet", Checks: map[string]string{
			"player_character_wallet_balance_check": "balance >= 0",
			"player_character_wallet_version_check": "version > 0",
		}},
	}
}
