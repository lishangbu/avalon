package audit

import (
	"context"
	"strings"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// TestCanonicalPayloadUsesStringActorAccountIdentifier 验证审计哈希载荷把强类型账号
// Identifier 固定编码为十进制 JSON 字符串，避免哈希格式随数据库驱动变化。
func TestCanonicalPayloadUsesStringActorAccountIdentifier(t *testing.T) {
	t.Parallel()

	actorID := snowflake.MustParse("1048576215")
	payload, err := canonicalPayload(AdminLedger, 1, Entry{
		ID: snowflake.MustParse("1048576216"), ActorAccountID: &actorID, ActorKind: "admin",
		ActionCode: "admin.test", ObjectType: "test", RequestID: "request-205",
		CreatedAt: time.Date(2026, 8, 11, 10, 0, 0, 0, time.UTC),
	})
	if err != nil {
		t.Fatalf("canonicalPayload() error = %v", err)
	}
	if !strings.Contains(string(payload), `"actor_account_id":"1048576215"`) {
		t.Fatalf("规范审计载荷未使用字符串 Identifier：%s", payload)
	}
}

// TestAppendRejectsZeroActorAccountIdentifier 验证审计追加器在访问数据库前拒绝零值账号
// Identifier，避免匿名主体被错误写成账号零值。
func TestAppendRejectsZeroActorAccountIdentifier(t *testing.T) {
	t.Parallel()

	zero := snowflake.ID(0)
	err := Append(context.Background(), unusedExecutor{}, AdminLedger, Entry{
		ID: snowflake.MustParse("1048576216"), ActorAccountID: &zero, ActorKind: "admin",
		ActionCode: "admin.test", ObjectType: "test", RequestID: "request-205",
		CreatedAt: time.Date(2026, 8, 11, 10, 0, 0, 0, time.UTC),
	})
	if err == nil {
		t.Fatal("零值 ActorAccountID 不应通过审计追加校验")
	}
}

type unusedExecutor struct{}

func (unusedExecutor) Exec(context.Context, string, ...any) (pgconn.CommandTag, error) {
	panic("无效参数不应执行数据库写入")
}

func (unusedExecutor) QueryRow(context.Context, string, ...any) pgx.Row {
	panic("无效参数不应执行数据库查询")
}
