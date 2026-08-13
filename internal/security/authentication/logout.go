package authentication

import (
	"context"
	"errors"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// ErrInvalidSession 表示服务端会话凭证不存在、过期或所属会话已撤销。
var ErrInvalidSession = errors.New("会话无效")

// ErrSessionNotFound 是持久化适配器报告的 会话凭证 不匹配错误。
var ErrSessionNotFound = errors.New("access 会话不存在")

// LogoutStore 根据已经认证的会话族标识撤销整个会话族。
type LogoutStore interface {
	RevokeSessionFamily(context.Context, snowflake.ID, time.Time) error
}

// LogoutService 使用当前 Bearer access token 对应的会话族执行服务端撤销。
type LogoutService struct {
	store LogoutStore
	now   func() time.Time
}

// NewLogoutService 使用显式依赖创建登出服务。
func NewLogoutService(
	store LogoutStore,
	now func() time.Time,
) *LogoutService {
	return &LogoutService{store: store, now: now}
}

// Logout 撤销已经认证 Principal 所属的整个会话族。
func (s *LogoutService) Logout(ctx context.Context, principal Principal) error {
	if principal.SessionFamilyID == snowflake.ID(0) {
		return ErrInvalidSession
	}
	err := s.store.RevokeSessionFamily(ctx, principal.SessionFamilyID, s.now().UTC())
	if errors.Is(err, ErrSessionNotFound) {
		return ErrInvalidSession
	}
	return err
}
