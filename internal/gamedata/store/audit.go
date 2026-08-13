package store

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	platformaudit "github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
)

// recordGameDataAudit 将不同资料类型的领域前后值写入统一管理审计基础设施。
func (s *Store) recordGameDataAudit(
	ctx context.Context,
	executor database.Transaction,
	actorID snowflake.ID,
	actionCode string,
	objectType string,
	objectID snowflake.ID,
	requestID string,
	occurredAt time.Time,
	before any,
	after any,
) error {
	changes, err := json.Marshal(struct {
		Before any `json:"before,omitempty"`
		After  any `json:"after,omitempty"`
	}{Before: before, After: after})
	if err != nil {
		return fmt.Errorf("编码游戏资料审计摘要: %w", err)
	}
	auditID, err := s.newID.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成游戏资料审计标识: %w", err)
	}
	reason := "administrative_change"
	if err := platformaudit.Append(ctx, executor, platformaudit.AdminLedger, platformaudit.Entry{
		ID: auditID, ActorAccountID: &actorID, ActorKind: "admin",
		ActionCode: actionCode, ObjectType: objectType, ObjectID: stringPointer(objectID.String()), RequestID: requestID,
		Reason: &reason, Changes: changes, CreatedAt: occurredAt.UTC(),
	}); err != nil {
		return fmt.Errorf("记录游戏资料管理审计: %w", err)
	}
	return nil
}

// stringPointer 返回审计条目使用的稳定可选字符串。
func stringPointer(value string) *string { return &value }
