package authentication

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/security/session"
)

var (
	// ErrRefreshReplay 表示已经轮换或撤销的 refresh token 被再次使用。
	ErrRefreshReplay = errors.New("refresh token 重放")
)

// RefreshRotationStore 原子轮换 refresh token；发现重放时必须撤销整个会话族。
type RefreshRotationStore interface {
	RotateRefreshSession(context.Context, []byte, []byte, snowflake.ID, time.Time, time.Duration) (Principal, time.Time, error)
}

// RefreshResult 是一次成功轮换后返回给传输层的最小结果。
type RefreshResult struct {
	// Principal 是新 access token 使用的账号和会话身份。
	Principal Principal
	// RefreshToken 是只返回一次的新 refresh token 明文。
	RefreshToken string
	// ExpiresAt 是 refresh 会话族的绝对失效时间。
	ExpiresAt time.Time
}

// RefreshService 负责单次使用 refresh token 的旋转和重放处置。
type RefreshService struct {
	store   RefreshRotationStore
	tokens  *session.TokenIssuer
	idleTTL time.Duration
	newID   snowflake.Source
	now     func() time.Time
}

// NewRefreshService 使用与登录相同的领域隔离 token 发行器创建轮换服务。
func NewRefreshService(store RefreshRotationStore, tokens *session.TokenIssuer, idleTTL time.Duration, newID snowflake.Source, now func() time.Time) *RefreshService {
	return &RefreshService{store: store, tokens: tokens, idleTTL: idleTTL, newID: newID, now: now}
}

// Refresh 原子消费当前 refresh token，并签发下一枚 token。
func (s *RefreshService) Refresh(ctx context.Context, plaintext string) (RefreshResult, error) {
	if plaintext == "" {
		return RefreshResult{}, ErrInvalidSession
	}
	next, err := s.tokens.Issue()
	if err != nil {
		return RefreshResult{}, fmt.Errorf("签发 refresh token: %w", err)
	}
	rotationID, err := s.newID.Next(ctx)
	if err != nil {
		return RefreshResult{}, fmt.Errorf("生成 refresh 轮换标识: %w", err)
	}
	principal, expiresAt, err := s.store.RotateRefreshSession(
		ctx, s.tokens.Digest(plaintext), next.Digest, rotationID, s.now().UTC(), s.idleTTL,
	)
	if err != nil {
		return RefreshResult{}, err
	}
	return RefreshResult{Principal: principal, RefreshToken: next.Plaintext, ExpiresAt: expiresAt}, nil
}
