package api

import (
	"context"
	"errors"
	"strings"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	"github.com/go-kratos/kratos/v3/middleware"
	"github.com/go-kratos/kratos/v3/transport"
	"github.com/lishangbu/avalon/internal/admin"
	adminauth "github.com/lishangbu/avalon/internal/admin/auth"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/security/access"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// AccessTokenVerifier 验证管理员 Bearer access token 并返回最小身份。
type AccessTokenVerifier interface {
	Verify(string) (authentication.Principal, error)
}

// AccessPrincipalValidator 校验 access token 对应管理员账号当前仍可用。
type AccessPrincipalValidator interface {
	Get(context.Context, snowflake.ID) (admin.Identity, error)
}

// NewBearerSecurityMiddleware 根据 Protobuf access 注解验证管理员 Bearer token。
// 管理 RPC 只接受显式 Bearer，不执行 Cookie 或 CSRF 校验。
func NewBearerSecurityMiddleware(catalog access.OperationCatalog, verifier AccessTokenVerifier, validators ...AccessPrincipalValidator) middleware.Middleware {
	return func(next middleware.Handler) middleware.Handler {
		return func(ctx context.Context, request any) (any, error) {
			transporter, ok := transport.FromServerContext(ctx)
			if !ok {
				return nil, kratoserrors.InternalServer("TRANSPORT_CONTEXT_MISSING", "服务端无法完成请求")
			}
			policy, declared := catalog[transporter.Operation()]
			if !declared {
				return nil, kratoserrors.InternalServer("ACCESS_POLICY_MISSING", "服务端安全策略不完整")
			}
			if policy.Public {
				return next(ctx, request)
			}
			authorization := strings.TrimSpace(transporter.RequestHeader().Get("Authorization"))
			parts := strings.Fields(authorization)
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
			if len(validators) > 0 && validators[0] != nil {
				if _, err := validators[0].Get(ctx, principal.AccountID); err != nil {
					if errors.Is(err, admin.ErrIdentityNotFound) {
						return nil, kratoserrors.Unauthorized("ACCESS_TOKEN_INVALID", "访问令牌无效或已过期")
					}
					return nil, kratoserrors.InternalServer("ACCESS_IDENTITY_VERIFICATION_FAILED", "服务端无法完成认证")
				}
			}
			return next(authentication.WithPrincipal(ctx, principal), request)
		}
	}
}
