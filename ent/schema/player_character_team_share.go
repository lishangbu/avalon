package schema

import (
	"encoding/json"
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// PlayerCharacterTeamShare 定义 player_character_team_share 表的持久化结构。
type PlayerCharacterTeamShare struct {
	ent.Schema
}

// Fields 返回 player_character_team_share 表全部字段及其数据库约束。
func (PlayerCharacterTeamShare) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("不可变 Team 分享记录的稳定 Identifier。"),
		field.Int64("source_team_id").GoType(snowflake.ID(0)).Positive().Comment("生成该快照时来源 Team 的稳定 Identifier；不作为导入后的持续关联。"),
		field.Int64("owner_player_character_id").GoType(snowflake.ID(0)).Positive().Comment("创建并可撤销该分享的 PlayerCharacter 稳定 Identifier。"),
		field.Int64("source_team_version").Comment("生成分享时来源 Team 的精确乐观并发版本。"),
		field.Bytes("code_digest").Comment("不可猜测分享码的 SHA-256 摘要；明文只在创建响应和客户端请求中出现。"),
		field.Int32("schema_version").Comment("冻结快照编码使用的 Team 分享结构版本。"),
		field.JSON("snapshot", json.RawMessage{}).Comment("由服务端生成且按 schema_version 解码的完整不可变 Team 快照。"),
		field.Int64("version").Annotations(entsql.DefaultExpr("1")).Comment("分享撤销转换使用的乐观并发控制版本。"),
		field.Time("expires_at").Comment("分享码不再允许解析或首次导入的到期时间。"),
		field.Time("revoked_at").Optional().Nillable().Comment("分享被拥有者永久撤销的时间；空值表示尚未撤销。"),
		field.Time("created_at").Comment("不可变分享快照生成的时间。"),
		field.Time("updated_at").Comment("分享唯一允许的撤销生命周期转换完成时间。"),
	}
}

// Annotations 固定 player_character_team_share 的表名、注释、复合主键和检查约束。
func (PlayerCharacterTeamShare) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("冻结 Team 与来源后续变更隔离的不可变分享快照；导入后不持续同步。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "player_character_team_share", Checks: map[string]string{
			"player_character_team_share_check":                     "expires_at > created_at",
			"player_character_team_share_check1":                    "revoked_at IS NULL OR revoked_at >= created_at",
			"player_character_team_share_check2":                    "updated_at >= created_at",
			"player_character_team_share_code_digest_check":         "octet_length(code_digest) = 32",
			"player_character_team_share_schema_version_check":      "schema_version >= 1",
			"player_character_team_share_snapshot_check":            "jsonb_typeof(snapshot) = 'object'::text",
			"player_character_team_share_source_team_version_check": "source_team_version >= 1",
			"player_character_team_share_version_check":             "version >= 1",
		}},
	}
}
