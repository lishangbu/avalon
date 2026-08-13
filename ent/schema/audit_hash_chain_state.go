package schema

import (
	"entgo.io/ent"
	"entgo.io/ent/dialect/entsql"
	"entgo.io/ent/schema"
	"entgo.io/ent/schema/field"
	"entgo.io/ent/schema/index"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// AuditHashChainState 定义 audit_hash_chain_state 表的持久化结构。
type AuditHashChainState struct {
	ent.Schema
}

// Fields 返回 audit_hash_chain_state 表全部字段及其数据库约束。
func (AuditHashChainState) Fields() []ent.Field {
	return []ent.Field{
		field.Int64("id").GoType(snowflake.ID(0)).Positive().Annotations(nonIncrementalSnowflakeIdentifier()).Comment("audit_hash_chain_state 表的雪花主键。"),
		field.String("ledger").MaxLen(64).Comment("audit_hash_chain_state 表的审计账本稳定键。"),
		field.Bytes("latest_hash").Comment("audit_hash_chain_state 表的 latest_hash 字段。"),
		field.Time("updated_at").Annotations(entsql.DefaultExpr("CURRENT_TIMESTAMP")).Comment("audit_hash_chain_state 表的 updated_at 字段。"),
	}
}

// Indexes 返回审计账本稳定键的唯一索引。
func (AuditHashChainState) Indexes() []ent.Index {
	return []ent.Index{index.Fields("ledger").Unique().StorageKey("uk_audit_hash_chain_state_ledger")}
}

// Annotations 固定 audit_hash_chain_state 的表名、注释、复合主键和检查约束。
func (AuditHashChainState) Annotations() []schema.Annotation {
	return []schema.Annotation{
		schema.Comment("保存 audit_hash_chain_state 的持久化记录。"),
		entsql.WithComments(true),
		entsql.Annotation{Table: "audit_hash_chain_state", Checks: map[string]string{
			"audit_hash_chain_state_hash_check": "octet_length(latest_hash) = ANY (ARRAY[0, 32])",
		}},
	}
}
