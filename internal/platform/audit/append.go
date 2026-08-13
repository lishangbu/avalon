package audit

import (
	"context"
	"crypto/sha256"
	"encoding/json"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgtype"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

const (
	// AdminLedger 保存管理员安全域和管理端资源写入审计。
	AdminLedger = "admin_audit_log"
	// AdministrationLedger 保存玩家账号及玩家资源管理审计。
	AdministrationLedger = "administration_audit_log"
)

// Executor 是审计追加器需要的事务内原生 PostgreSQL 能力。
type Executor interface {
	Exec(context.Context, string, ...any) (pgconn.CommandTag, error)
	QueryRow(context.Context, string, ...any) pgx.Row
}

// Entry 保存一条待追加审计事实的完整、非哈希字段。
type Entry struct {
	// ID 是审计事实的稳定 Snowflake Identifier。
	ID snowflake.ID
	// ActorAccountID 是可选的管理员或玩家账号 Snowflake Identifier。
	ActorAccountID *snowflake.ID
	// ActorKind 是 admin、account、anonymous、operator 或 system。
	ActorKind string
	// ActorIdentifier 是无账号主体的可选稳定标识。
	ActorIdentifier *string
	// ActionCode 是稳定审计动作编码。
	ActionCode string
	// ObjectType 是受影响对象的稳定类型。
	ObjectType string
	// ObjectID 是受影响对象的可选稳定标识。
	ObjectID *string
	// RequestID 关联入口请求和结构化日志。
	RequestID string
	// Reason 是可选的受控操作原因。
	Reason *string
	// Changes 是不含密钥、令牌或大载荷的 JSON 变更摘要。
	Changes json.RawMessage
	// CreatedAt 是审计事实发生的 UTC 时间。
	CreatedAt time.Time
}

// Append 在调用方事务内串行分配顺序号、计算 SHA-256 链式摘要并写入链尾状态。
func Append(ctx context.Context, executor Executor, ledger string, entry Entry) error {
	if executor == nil || !validLedger(ledger) || !entry.ID.IsValid() || entry.ActorKind == "" ||
		(entry.ActorAccountID != nil && !entry.ActorAccountID.IsValid()) ||
		entry.ActionCode == "" || entry.ObjectType == "" || entry.RequestID == "" || entry.CreatedAt.IsZero() {
		return fmt.Errorf("审计追加参数无效: executor=%t ledger=%q id=%t actor=%t action=%t object=%t request=%t created_at=%t", executor != nil, ledger, entry.ID.IsValid(), entry.ActorKind != "", entry.ActionCode != "", entry.ObjectType != "", entry.RequestID != "", !entry.CreatedAt.IsZero())
	}
	entry.CreatedAt = entry.CreatedAt.UTC().Truncate(time.Microsecond)
	changes, err := normalizeJSON(entry.Changes)
	if err != nil {
		return fmt.Errorf("规范化审计 changes: %w", err)
	}
	entry.Changes = changes
	var previousHash []byte
	if err := executor.QueryRow(ctx, `
SELECT latest_hash FROM audit_hash_chain_state WHERE ledger = $1 FOR UPDATE`, ledger).Scan(&previousHash); err != nil {
		return fmt.Errorf("锁定审计哈希链尾 %s: %w", ledger, err)
	}
	var sequence int64
	if err := executor.QueryRow(ctx, fmt.Sprintf("SELECT COALESCE(max(sequence), 0) + 1 FROM %s", ledger)).Scan(&sequence); err != nil {
		return fmt.Errorf("分配审计顺序号 %s: %w", ledger, err)
	}
	payload, err := canonicalPayload(ledger, sequence, entry)
	if err != nil {
		return err
	}
	digestInput := append(append([]byte(nil), previousHash...), payload...)
	digest := sha256.Sum256(digestInput)
	statement := fmt.Sprintf(`
INSERT INTO %s (
    id, sequence, actor_account_id, actor_kind, actor_identifier, action_code,
    object_type, object_id, request_id, reason, changes, created_at, previous_hash, entry_hash
) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14)`, ledger)
	if _, err := executor.Exec(ctx, statement,
		entry.ID, sequence, nullableID(entry.ActorAccountID), entry.ActorKind,
		nullableString(entry.ActorIdentifier), entry.ActionCode, entry.ObjectType,
		nullableString(entry.ObjectID), entry.RequestID, nullableString(entry.Reason),
		entry.Changes, entry.CreatedAt, previousHash, digest[:],
	); err != nil {
		return fmt.Errorf("追加审计记录 %s: %w", ledger, err)
	}
	if _, err := executor.Exec(ctx, `
UPDATE audit_hash_chain_state SET latest_hash = $2, updated_at = $3 WHERE ledger = $1`,
		ledger, digest[:], entry.CreatedAt); err != nil {
		return fmt.Errorf("更新审计哈希链尾 %s: %w", ledger, err)
	}
	return nil
}

type canonicalEntry struct {
	ID              string          `json:"id"`
	Sequence        int64           `json:"sequence"`
	ActorAccountID  *string         `json:"actor_account_id"`
	ActorKind       string          `json:"actor_kind"`
	ActorIdentifier *string         `json:"actor_identifier"`
	ActionCode      string          `json:"action_code"`
	ObjectType      string          `json:"object_type"`
	ObjectID        *string         `json:"object_id"`
	RequestID       string          `json:"request_id"`
	Reason          *string         `json:"reason"`
	Changes         json.RawMessage `json:"changes"`
	CreatedAt       time.Time       `json:"created_at"`
}

func canonicalPayload(ledger string, sequence int64, entry Entry) ([]byte, error) {
	return json.Marshal(struct {
		Entry    canonicalEntry `json:"entry"`
		Ledger   string         `json:"ledger"`
		Sequence int64          `json:"sequence"`
	}{
		Entry: canonicalEntry{
			ID: entry.ID.String(), Sequence: sequence, ActorAccountID: identifierStringPointer(entry.ActorAccountID),
			ActorKind: entry.ActorKind, ActorIdentifier: entry.ActorIdentifier,
			ActionCode: entry.ActionCode, ObjectType: entry.ObjectType, ObjectID: entry.ObjectID,
			RequestID: entry.RequestID, Reason: entry.Reason, Changes: entry.Changes,
			CreatedAt: entry.CreatedAt.UTC().Truncate(time.Microsecond),
		},
		Ledger: ledger, Sequence: sequence,
	})
}

func normalizeJSON(value json.RawMessage) (json.RawMessage, error) {
	if len(value) == 0 {
		return nil, nil
	}
	var decoded any
	if err := json.Unmarshal(value, &decoded); err != nil {
		return nil, err
	}
	return json.Marshal(decoded)
}

func validLedger(ledger string) bool {
	return ledger == AdminLedger || ledger == AdministrationLedger
}

func nullableString(value *string) any {
	if value == nil {
		return nil
	}
	return *value
}

func nullableID(value *snowflake.ID) any {
	if value == nil {
		return nil
	}
	return *value
}

func identifierStringPointer(value *snowflake.ID) *string {
	if value == nil {
		return nil
	}
	text := value.String()
	return &text
}

func pgIDPointer(value pgtype.Int8) *snowflake.ID {
	if !value.Valid {
		return nil
	}
	id := snowflake.ID(value.Int64)
	return &id
}
