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

// PlayerCharacterIdempotencyRecord 保存非 Traversal 玩家命令的首次响应。
type PlayerCharacterIdempotencyRecord struct{ ent.Schema }

// Fields 返回玩家命令幂等记录字段。
func (PlayerCharacterIdempotencyRecord) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()),
		field.Int64("player_character_id").GoType(snowflake.ID(0)).Positive().Comment("命令作用域所属 PlayerCharacter 稳定 Identifier。"),
		field.String("operation_id").MaxLen(64).Comment("服务端注册的稳定操作标识。"),
		field.String("idempotency_key").MaxLen(128).Comment("客户端显式幂等键。"),
		field.Bytes("request_digest").Comment("规范请求 SHA-256 摘要。"),
		field.JSON("response", json.RawMessage{}).Comment("首次提交响应的规范 JSON。"),
		field.Time("created_at").Comment("首次提交时间。"),
	}
}

// Indexes 保证操作作用域内幂等键唯一。
func (PlayerCharacterIdempotencyRecord) Indexes() []ent.Index {
	return []ent.Index{index.Fields("player_character_id", "operation_id", "idempotency_key").Unique().StorageKey("uk_player_character_idempotency_record_player_character_id_operation_id_idempotency_key")}
}

// Annotations 固定摘要和响应结构约束。
func (PlayerCharacterIdempotencyRecord) Annotations() []schema.Annotation {
	return []schema.Annotation{schema.Comment("玩家写命令的确定性幂等响应记录。"), entsql.WithComments(true), entsql.Annotation{Table: "player_character_idempotency_record", Checks: map[string]string{
		"player_character_idempotency_record_digest_check":   "octet_length(request_digest) = 32",
		"player_character_idempotency_record_response_check": "jsonb_typeof(response) = 'object'::text",
	}}}
}
