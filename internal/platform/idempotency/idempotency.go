// Package idempotency 提供跨领域管理写命令共享的持久幂等契约与规范化摘要。
package idempotency

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/json"
	"errors"
	"fmt"
	"regexp"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// StoredRecord 是 RecordStore 锁定后返回的幂等摘要与已提交响应。
type StoredRecord struct {
	RequestDigest []byte
	Response      []byte
}

// RecordStore 隔离 原生 SQL 生成类型，提供持久幂等算法所需的最小数据库操作。
type RecordStore interface {
	TryClaim(context.Context, Request) (bool, error)
	FindForUpdate(context.Context, Request) (StoredRecord, error)
	CompleteRecord(context.Context, Request, []byte) (int64, error)
}

// PersistentWriter 在具体 原生 SQL 适配器之上共享认领、异载荷判定和完成语义。
type PersistentWriter struct {
	store RecordStore
}

// NewPersistentWriter 创建共享持久幂等写入器。
func NewPersistentWriter(store RecordStore) *PersistentWriter {
	return &PersistentWriter{store: store}
}

// ClaimIdempotency 认领新键，或锁定并重放相同载荷已经提交的响应。
func (w *PersistentWriter) ClaimIdempotency(ctx context.Context, request Request) (Claim, error) {
	claimed, err := w.store.TryClaim(ctx, request)
	if err != nil {
		return Claim{}, err
	}
	if claimed {
		return Claim{}, nil
	}
	existing, err := w.store.FindForUpdate(ctx, request)
	if err != nil {
		return Claim{}, err
	}
	if !bytes.Equal(existing.RequestDigest, request.RequestDigest) {
		return Claim{}, ErrConflict
	}
	if len(existing.Response) == 0 {
		return Claim{}, errors.New("幂等记录缺少已提交响应")
	}
	return Claim{Replay: true, Response: existing.Response}, nil
}

// CompleteIdempotency 保存与业务写入同事务提交的最终响应。
func (w *PersistentWriter) CompleteIdempotency(
	ctx context.Context,
	request Request,
	response []byte,
) error {
	rows, err := w.store.CompleteRecord(ctx, request, response)
	if err != nil {
		return err
	}
	if rows != 1 {
		return fmt.Errorf("完成幂等记录影响 %d 行，预期 1 行", rows)
	}
	return nil
}

var keyPattern = regexp.MustCompile(`^[A-Za-z0-9._:-]{1,128}$`)

var (
	// ErrInvalidKey 表示关键写命令没有提供合法的幂等键。
	ErrInvalidKey = errors.New("幂等键无效")
	// ErrConflict 表示同一调用者和操作重复使用幂等键但命令载荷不同。
	ErrConflict = errors.New("幂等键已用于不同载荷")
)

// Request 是事务内声明一个管理写命令幂等作用域所需的信息。
type Request struct {
	ActorAccountID snowflake.ID
	OperationID    string
	Key            string
	RequestDigest  []byte
	CreatedAt      time.Time
}

// Claim 表示当前事务应执行新命令，或直接重放已经提交的响应。
type Claim struct {
	Replay   bool
	Response []byte
}

// Writer 在业务事务内认领幂等键并保存最终可重放结果。
type Writer interface {
	ClaimIdempotency(context.Context, Request) (Claim, error)
	CompleteIdempotency(context.Context, Request, []byte) error
}

// ValidKey 判断客户端幂等键是否满足稳定的长度与字符边界。
func ValidKey(key string) bool {
	return keyPattern.MatchString(key)
}

// Digest 对规范化命令载荷编码并计算 SHA-256 摘要。
func Digest(value any) ([]byte, error) {
	payload, err := json.Marshal(value)
	if err != nil {
		return nil, err
	}
	digest := sha256.Sum256(payload)
	return digest[:], nil
}

// ClaimResponse 认领幂等作用域，并在已提交命令重放时解码原始响应。
func ClaimResponse(ctx context.Context, writer Writer, request Request, target any) (bool, error) {
	claim, err := writer.ClaimIdempotency(ctx, request)
	if err != nil {
		return false, err
	}
	if !claim.Replay {
		return false, nil
	}
	if err := json.Unmarshal(claim.Response, target); err != nil {
		return false, err
	}
	return true, nil
}

// Complete 编码并保存与业务写入同事务提交的可重放响应。
func Complete(ctx context.Context, writer Writer, request Request, response any) error {
	payload, err := json.Marshal(response)
	if err != nil {
		return err
	}
	return writer.CompleteIdempotency(ctx, request, payload)
}
