package api

import (
	"context"
	"errors"
	"strings"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	"github.com/go-kratos/kratos/v3/middleware"
	"github.com/go-kratos/kratos/v3/transport"
	adminauth "github.com/lishangbu/avalon/internal/admin/auth"
	"github.com/lishangbu/avalon/internal/security/access"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// AccessTokenVerifier 验证 Bearer access token 并返回最小会话身份。
type AccessTokenVerifier interface {
	Verify(string) (authentication.Principal, error)
}

// NewBearerSecurityMiddleware 将 Protobuf 公开策略和 Bearer 身份认证接入 Kratos Middleware。
func NewBearerSecurityMiddleware(catalog access.OperationCatalog, verifier AccessTokenVerifier) middleware.Middleware {
	return func(next middleware.Handler) middleware.Handler {
		return func(ctx context.Context, request any) (any, error) {
			tr, ok := transport.FromServerContext(ctx)
			if !ok {
				return nil, kratoserrors.InternalServer("TRANSPORT_CONTEXT_MISSING", "服务端无法完成请求")
			}
			policy, declared := catalog[tr.Operation()]
			if !declared {
				return nil, kratoserrors.InternalServer("ACCESS_POLICY_MISSING", "服务端安全策略不完整")
			}
			if policy.Public {
				return next(ctx, request)
			}
			parts := strings.Fields(strings.TrimSpace(tr.RequestHeader().Get("Authorization")))
			if len(parts) != 2 || !strings.EqualFold(parts[0], "Bearer") {
				return nil, kratoserrors.Unauthorized("ACCESS_TOKEN_INVALID", "访问令牌无效或已过期")
			}
			principal, err := verifier.Verify(parts[1])
			if err != nil {
				if errors.Is(err, adminauth.ErrInvalidAccessToken) {
					return nil, kratoserrors.Unauthorized("ACCESS_TOKEN_INVALID", "访问令牌无效或已过期")
				}
				return nil, kratoserrors.InternalServer("ACCESS_TOKEN_VERIFICATION_FAILED", "服务端无法完成认证")
			}
			return next(authentication.WithPrincipal(ctx, principal), request)
		}
	}
}
