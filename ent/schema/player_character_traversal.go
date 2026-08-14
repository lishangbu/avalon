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

// PlayerCharacterTraversal 保存一次幂等移动的不可变提交事实。
type PlayerCharacterTraversal struct{ ent.Schema }

// Fields 返回 Traversal 的输入、位置版本和响应摘要。
func (PlayerCharacterTraversal) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("执行移动的 PlayerCharacter 稳定 Identifier。"),
		field.Int64("location_exit_id").GoType(snowflake.ID(0)).Positive().Comment("执行的有向出口稳定 Identifier。"),
		field.Int64("source_location_id").GoType(snowflake.ID(0)).Positive().Comment("提交前 Location 稳定 Identifier。"),
		field.Int64("target_location_id").GoType(snowflake.ID(0)).Positive().Comment("提交后 Location 稳定 Identifier。"),
		field.Int64("position_version_before").Comment("命令验证的位置版本。"),
		field.Int64("position_version_after").Comment("成功提交后的位置版本。"),
		field.String("idempotency_key").MaxLen(128).Comment("角色与命令作用域内的幂等键。"),
		field.Bytes("request_digest").Comment("规范请求的 SHA-256 摘要。"),
		field.JSON("response", json.RawMessage{}).Comment("首次成功提交的确定性响应摘要。"),
		field.Time("created_at").Comment("Traversal 提交时间。"),
	}
}

// Indexes 保证角色幂等键只能绑定一个请求。
func (PlayerCharacterTraversal) Indexes() []ent.Index {
	return []ent.Index{
		index.Fields("player_character_id", "idempotency_key").Unique().StorageKey("uk_player_character_traversal_player_character_id_idempotency_key"),
		index.Fields("player_character_id", "created_at").StorageKey("idx_player_character_traversal_player_character_id_created_at"),
	}
}

// Annotations 固定摘要和位置版本约束。
func (PlayerCharacterTraversal) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("一次成功 Location Traversal 的幂等不可变事实。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_traversal", Checks: map[string]string{
		"player_character_traversal_digest_check":   "octet_length(request_digest) = 32",
		"player_character_traversal_response_check": "jsonb_typeof(response) = 'object'::text",
		"player_character_traversal_version_check":  "position_version_before > 0 AND position_version_after = position_version_before + 1",
	}}}
}
