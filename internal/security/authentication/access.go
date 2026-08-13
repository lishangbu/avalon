package authentication

import (
	"context"
	"errors"
	"fmt"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/security/session"
)

// sessionAuthenticationQueryTimeout 防止数据库阻塞无限占用受保护 HTTP 请求。
const sessionAuthenticationQueryTimeout = 2 * time.Second

// Principal 是通过 会话凭证 认证后可以交给应用层使用的最小账号身份。
//
// 它只包含稳定标识和授权版本，不携带令牌明文、摘要或其他认证秘密。
type Principal struct {
	AccountID       snowflake.ID
	SessionID       snowflake.ID
	SessionFamilyID snowflake.ID
	SecurityVersion int64
}

// SessionAuthenticationStore 根据会话凭证摘要读取仍然有效的权威会话身份。
type SessionAuthenticationStore interface {
	AuthenticateSession(context.Context, []byte, time.Time) (Principal, error)
}

// SessionActivityStore 以节流条件更新当前会话的最近活动和空闲失效时间。
type SessionActivityStore interface {
	TouchSessionActivity(context.Context, snowflake.ID, time.Time, time.Time, time.Time) error
}

// SessionAuthenticator 将 Bearer 会话或 refresh token 认证为服务端权威 Principal。
type SessionAuthenticator struct {
	store SessionAuthenticationStore
	// sessionTokens 计算 opaque 会话或 refresh 凭证的领域隔离摘要。
	sessionTokens *session.TokenIssuer
	now           func() time.Time
	// idleTTL 是每次实际持久化活动后重新计算的空闲有效期。
	idleTTL time.Duration
	// activityWriteInterval 限制高频请求写入同一会话行的最短间隔。
	activityWriteInterval time.Duration
}

// NewSessionAuthenticator 使用显式空闲期限和活动写入节流创建 opaque 会话认证器。
func NewSessionAuthenticator(
	store SessionAuthenticationStore,
	sessionTokens *session.TokenIssuer,
	idleTTL time.Duration,
	activityWriteInterval time.Duration,
	now func() time.Time,
) *SessionAuthenticator {
	return &SessionAuthenticator{
		store: store, sessionTokens: sessionTokens, idleTTL: idleTTL,
		activityWriteInterval: activityWriteInterval, now: now,
	}
}

// Authenticate 验证 opaque 会话或 refresh token，并返回不含任何凭证明文的账号身份。
func (a *SessionAuthenticator) Authenticate(ctx context.Context, plaintext string) (Principal, error) {
	if plaintext == "" {
		return Principal{}, ErrInvalidSession
	}
	queryContext, cancel := context.WithTimeout(ctx, sessionAuthenticationQueryTimeout)
	defer cancel()
	now := a.now().UTC()
	principal, err := a.store.AuthenticateSession(
		queryContext,
		a.sessionTokens.Digest(plaintext),
		now,
	)
	if errors.Is(err, ErrSessionNotFound) {
		return Principal{}, ErrInvalidSession
	}
	if err != nil {
		return Principal{}, fmt.Errorf("认证 会话凭证: %w", err)
	}
	if activityStore, ok := a.store.(SessionActivityStore); ok && a.idleTTL > 0 && a.activityWriteInterval > 0 {
		if err := activityStore.TouchSessionActivity(
			queryContext,
			principal.SessionID,
			now,
			now.Add(a.idleTTL),
			now.Add(-a.activityWriteInterval),
		); err != nil {
			return Principal{}, fmt.Errorf("更新会话活动时间: %w", err)
		}
	}
	return principal, nil
}
