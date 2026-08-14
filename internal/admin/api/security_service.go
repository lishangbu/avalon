// Package api 将独立管理员身份域映射到 Kratos 生成的 HTTP 契约。
package api

import (
	"context"
	"errors"
	"net/http"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/requestid"
	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	adminv1 "github.com/lishangbu/avalon/api/gen/go/avalon/admin/v1"
	"github.com/lishangbu/avalon/internal/admin"
	adminauth "github.com/lishangbu/avalon/internal/admin/auth"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"google.golang.org/grpc/metadata"
	"google.golang.org/protobuf/types/known/timestamppb"
)

const maximumDeviceSummaryLength = 256

// LoginService 验证管理员用户名和密码，并创建权威持久会话。
type LoginService interface {
	Login(context.Context, authentication.LoginCommand) (authentication.LoginResult, error)
}

// LogoutService 撤销当前管理员设备会话。
type LogoutService interface {
	Logout(context.Context, authentication.Principal) error
}

// IdentityReader 读取当前管理员最小身份快照。
type IdentityReader interface {
	Get(context.Context, snowflake.ID) (admin.Identity, error)
}

// AccessTokenIssuer 签发短期管理员 access token 并公开当前 JWK。
type AccessTokenIssuer interface {
	Issue(authentication.Principal) (string, time.Time, error)
	JWK() adminauth.JWK
}

// RefreshService 原子轮换管理员 refresh token。
type RefreshService interface {
	Refresh(context.Context, string) (authentication.RefreshResult, error)
}

// RefreshTokenValidator 在轮换前验证 refresh token。
type RefreshTokenValidator interface {
	Authenticate(context.Context, string) (authentication.Principal, error)
}

// SessionManager 查询和撤销管理员自己的持久设备会话。
type SessionManager interface {
	List(context.Context, authentication.Principal) ([]authentication.SessionFamily, error)
	Revoke(context.Context, authentication.Principal, snowflake.ID, string) error
}

// SecurityService 实现独立管理员 Bearer access token 与轮换 refresh token 契约。
type SecurityService struct {
	// login 验证用户名和密码并创建数据库会话。
	login LoginService
	// logout 撤销当前管理员设备会话。
	logout LogoutService
	// identity 读取不包含角色和权限的管理员身份。
	identity IdentityReader
	// accessTokens 签发短期 Ed25519 JWT 并公开验证公钥。
	accessTokens AccessTokenIssuer
	// refresh 原子消费并轮换 refresh token。
	refresh RefreshService
	// refreshValidator 验证 refresh token。
	refreshValidator RefreshTokenValidator
	// sessions 管理当前管理员自己的设备会话。
	sessions SessionManager
}

// NewSecurityService 使用显式应用依赖创建管理员安全服务。
func NewSecurityService(
	login LoginService,
	logout LogoutService,
	identity IdentityReader,
	accessTokens AccessTokenIssuer,
	refresh RefreshService,
	refreshValidator RefreshTokenValidator,
	sessions SessionManager,
) *SecurityService {
	return &SecurityService{
		login: login, logout: logout, identity: identity, accessTokens: accessTokens,
		refresh: refresh, refreshValidator: refreshValidator, sessions: sessions,
	}
}

// Login 建立管理员 refresh 会话，并返回短期 Bearer access token。
func (s *SecurityService) Login(
	ctx context.Context,
	request *adminv1.LoginRequest,
) (*adminv1.LoginResponse, error) {
	result, err := s.login.Login(ctx, authentication.LoginCommand{
		Username: request.GetUsername(), Password: request.GetPassword(),
		RequestID: adminRequestIDFromContext(ctx), DeviceSummary: deviceSummaryFromContext(ctx),
	})
	if errors.Is(err, authentication.ErrInvalidCredentials) {
		return nil, kratoserrors.Unauthorized("INVALID_CREDENTIALS", "用户名或密码无效")
	}
	if err != nil {
		return nil, kratoserrors.InternalServer("LOGIN_FAILED", "服务端无法完成登录")
	}
	principal := authentication.Principal{AccountID: result.AccountID, SessionID: result.SessionID, SessionFamilyID: result.SessionFamilyID, SecurityVersion: result.SecurityVersion}
	accessToken, accessExpiresAt, err := s.accessTokens.Issue(principal)
	if err != nil {
		return nil, kratoserrors.InternalServer("ACCESS_TOKEN_ISSUE_FAILED", "服务端无法完成登录")
	}
	identity, err := s.identity.Get(ctx, result.AccountID)
	if err != nil {
		return nil, kratoserrors.InternalServer("IDENTITY_QUERY_FAILED", "服务端无法完成登录")
	}
	return &adminv1.LoginResponse{
		AccessToken: accessToken, AccessTokenExpiresAt: timestamppb.New(accessExpiresAt),
		RefreshToken:          result.SessionToken,
		RefreshTokenExpiresAt: timestamppb.New(result.ExpiresAt), User: adminSessionUser(identity),
	}, nil
}

// Refresh 原子轮换 refresh token，并签发新的 access token。
func (s *SecurityService) Refresh(ctx context.Context, request *adminv1.RefreshRequest) (*adminv1.RefreshResponse, error) {
	refreshToken := request.GetRefreshToken()
	if refreshToken == "" {
		return nil, kratoserrors.Unauthorized("REFRESH_TOKEN_INVALID", "登录续期凭据无效")
	}
	_, err := s.refreshValidator.Authenticate(ctx, refreshToken)
	if err != nil {
		_, rotationErr := s.refresh.Refresh(ctx, refreshToken)
		if errors.Is(rotationErr, authentication.ErrRefreshReplay) {
			return nil, kratoserrors.Unauthorized("REFRESH_TOKEN_REPLAYED", "登录续期凭据已失效")
		}
		return nil, kratoserrors.Unauthorized("REFRESH_TOKEN_INVALID", "登录续期凭据无效")
	}
	result, err := s.refresh.Refresh(ctx, refreshToken)
	if err != nil {
		return nil, kratoserrors.Unauthorized("REFRESH_TOKEN_INVALID", "登录续期凭据无效")
	}
	accessToken, accessExpiresAt, err := s.accessTokens.Issue(result.Principal)
	if err != nil {
		return nil, kratoserrors.InternalServer("ACCESS_TOKEN_ISSUE_FAILED", "服务端无法完成续期")
	}
	return &adminv1.RefreshResponse{AccessToken: accessToken, AccessTokenExpiresAt: timestamppb.New(accessExpiresAt), RefreshToken: result.RefreshToken, RefreshTokenExpiresAt: timestamppb.New(result.ExpiresAt)}, nil
}

// GetJWKS 返回当前管理员 access token 的 Ed25519 公开验证密钥。
func (s *SecurityService) GetJWKS(context.Context, *adminv1.GetJWKSRequest) (*adminv1.GetJWKSResponse, error) {
	key := s.accessTokens.JWK()
	return &adminv1.GetJWKSResponse{Keys: []*adminv1.JSONWebKey{{Kty: key.KeyType, Crv: key.Curve, Use: key.Use, Alg: key.Algorithm, Kid: key.KeyID, X: key.X}}}, nil
}

// Logout 幂等撤销当前管理员设备会话。
func (s *SecurityService) Logout(
	ctx context.Context,
	_ *adminv1.LogoutRequest,
) (*adminv1.AdminSecurityServiceLogoutResponse, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	if err := s.logout.Logout(ctx, principal); err != nil && !errors.Is(err, authentication.ErrInvalidSession) {
		return nil, kratoserrors.InternalServer("LOGOUT_FAILED", "服务端无法完成注销")
	}
	return &adminv1.AdminSecurityServiceLogoutResponse{Body: &adminv1.AdminLogoutResult{LoggedOut: true}}, nil
}

// GetCurrentSession 返回当前管理员身份，不返回角色或权限集合。
func (s *SecurityService) GetCurrentSession(
	ctx context.Context,
	_ *adminv1.GetCurrentSessionRequest,
) (*adminv1.GetCurrentSessionResponse, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	identity, err := s.identity.Get(ctx, principal.AccountID)
	if err != nil {
		return nil, kratoserrors.InternalServer("SESSION_QUERY_FAILED", "服务端无法读取登录状态")
	}
	return &adminv1.GetCurrentSessionResponse{User: adminSessionUser(identity)}, nil
}

// ListSessions 返回当前管理员仍然有效的设备会话，并标识当前会话。
func (s *SecurityService) ListSessions(
	ctx context.Context,
	_ *adminv1.ListSessionsRequest,
) (*adminv1.ListSessionsResponse, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	families, err := s.sessions.List(ctx, principal)
	if err != nil {
		return nil, kratoserrors.InternalServer("SESSION_LIST_FAILED", "服务端无法读取设备会话")
	}
	result := make([]*adminv1.AdminSession, len(families))
	for index, family := range families {
		result[index] = &adminv1.AdminSession{
			Id: family.FamilyID.String(), Current: family.Current, DeviceSummary: family.DeviceSummary,
			CreatedAt: timestamppb.New(family.CreatedAt), LastActivityAt: timestamppb.New(family.LastActivityAt),
			IdleExpiresAt: timestamppb.New(family.IdleExpiresAt), ExpiresAt: timestamppb.New(family.ExpiresAt),
		}
	}
	return &adminv1.ListSessionsResponse{Sessions: result}, nil
}

// RevokeSession 幂等撤销当前管理员拥有的指定设备会话。
func (s *SecurityService) RevokeSession(
	ctx context.Context,
	request *adminv1.RevokeSessionRequest,
) (*adminv1.AdminSecurityServiceRevokeSessionResponse, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	familyID, err := snowflake.Parse(request.GetSessionId())
	if err != nil {
		return nil, kratoserrors.BadRequest("INVALID_SESSION_ID", "设备会话标识无效")
	}
	if err := s.sessions.Revoke(ctx, principal, familyID, adminRequestIDFromContext(ctx)); err != nil {
		return nil, kratoserrors.InternalServer("SESSION_REVOKE_FAILED", "服务端无法撤销设备会话")
	}
	return &adminv1.AdminSecurityServiceRevokeSessionResponse{Body: &adminv1.AdminSessionRevoked{Revoked: true}}, nil
}

func adminSessionUser(identity admin.Identity) *adminv1.AdminSessionUser {
	return &adminv1.AdminSessionUser{Id: identity.ID.String(), Username: identity.Username, DisplayName: identity.DisplayName}
}

func deviceSummary(userAgent string) string {
	cleaned := strings.Map(func(character rune) rune {
		if character < 0x20 || character == 0x7f {
			return -1
		}
		return character
	}, strings.TrimSpace(userAgent))
	characters := []rune(cleaned)
	if len(characters) > maximumDeviceSummaryLength {
		characters = characters[:maximumDeviceSummaryLength]
	}
	return string(characters)
}

func adminRequestIDFromContext(ctx context.Context) string {
	if incoming, ok := metadata.FromIncomingContext(ctx); ok {
		if values := incoming.Get("x-request-id"); len(values) > 0 && strings.TrimSpace(values[0]) != "" {
			return strings.TrimSpace(values[0])
		}
	}
	return requestid.New()
}

func httpBoundary(ctx context.Context) (*http.Request, http.ResponseWriter, bool) {
	request, err := http.NewRequestWithContext(ctx, http.MethodPost, "grpc://avalon", nil)
	if err != nil {
		return nil, nil, false
	}
	return request, nil, true
}

func requestID(request *http.Request) string {
	if request != nil && strings.TrimSpace(request.Header.Get("X-Request-ID")) != "" {
		return strings.TrimSpace(request.Header.Get("X-Request-ID"))
	}
	return requestid.New()
}

func deviceSummaryFromContext(ctx context.Context) string {
	if incoming, ok := metadata.FromIncomingContext(ctx); ok {
		if values := incoming.Get("user-agent"); len(values) > 0 {
			return deviceSummary(values[0])
		}
	}
	return "unknown"
}
