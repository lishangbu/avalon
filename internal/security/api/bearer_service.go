package api

import (
	"context"
	"errors"
	"strings"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	securityv1 "github.com/lishangbu/avalon/api/gen/go/avalon/security/v1"
	adminauth "github.com/lishangbu/avalon/internal/admin/auth"
	"github.com/lishangbu/avalon/internal/platform/requestid"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"google.golang.org/grpc/metadata"
	"google.golang.org/protobuf/types/known/timestamppb"
)

const maximumDeviceSummaryLength = 256

// BearerLoginService 是 Proto 传输层使用的用户名密码登录应用边界。
type BearerLoginService interface {
	Login(context.Context, authentication.LoginCommand) (authentication.LoginResult, error)
}

// BearerLogoutService 是 Proto 传输层使用的当前会话撤销边界。
type BearerLogoutService interface {
	Logout(context.Context, authentication.Principal) error
}

// BearerCurrentSessionQuery 是 Proto 传输层使用的当前账号授权快照边界。
type BearerCurrentSessionQuery interface {
	Get(context.Context, snowflake.ID) (authentication.Identity, error)
}

// BearerSessionManager 查询和撤销账号自己的设备会话。
type BearerSessionManager interface {
	List(context.Context, authentication.Principal) ([]authentication.SessionFamily, error)
	Revoke(context.Context, authentication.Principal, snowflake.ID, string) error
}

// PlayerRefreshService 原子轮换玩家 refresh token。
type PlayerRefreshService interface {
	Refresh(context.Context, string) (authentication.RefreshResult, error)
}

// BearerService 实现显式 Bearer/refresh token 协议。
type BearerService struct {
	login        BearerLoginService
	logout       BearerLogoutService
	current      BearerCurrentSessionQuery
	sessions     BearerSessionManager
	accessTokens *adminauth.AccessTokenIssuer
	refresh      PlayerRefreshService
}

// NewBearerService 创建玩家 Bearer 服务。
func NewBearerService(
	login BearerLoginService,
	logout BearerLogoutService,
	current BearerCurrentSessionQuery,
	sessions BearerSessionManager,
	accessTokens *adminauth.AccessTokenIssuer,
) *BearerService {
	return &BearerService{login: login, logout: logout, current: current, sessions: sessions, accessTokens: accessTokens}
}

// SetRefreshService 注入玩家 refresh token 轮换边界。
func (s *BearerService) SetRefreshService(refresh PlayerRefreshService) { s.refresh = refresh }

// Login 建立会话并返回短期 Bearer access token 与可轮换 refresh token。
func (s *BearerService) Login(ctx context.Context, request *securityv1.LoginRequest) (*securityv1.LoginResponse, error) {
	result, err := s.login.Login(ctx, authentication.LoginCommand{
		Username: request.GetUsername(), Password: request.GetPassword(),
		RequestID: requestIDFromContext(ctx), DeviceSummary: deviceSummaryFromContext(ctx),
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
	return &securityv1.LoginResponse{
		ExpiresAt: timestamppb.New(result.ExpiresAt), AccessToken: accessToken,
		AccessTokenExpiresAt: timestamppb.New(accessExpiresAt), RefreshToken: result.SessionToken,
		RefreshTokenExpiresAt: timestamppb.New(result.ExpiresAt),
	}, nil
}

// Refresh 原子轮换显式提交的 refresh token，并签发新的 access token。
func (s *BearerService) Refresh(ctx context.Context, request *securityv1.RefreshRequest) (*securityv1.RefreshResponse, error) {
	if s.refresh == nil || s.accessTokens == nil || request == nil || request.GetRefreshToken() == "" {
		return nil, kratoserrors.Unauthorized("REFRESH_TOKEN_INVALID", "登录续期凭据无效")
	}
	result, err := s.refresh.Refresh(ctx, request.GetRefreshToken())
	if err != nil {
		return nil, kratoserrors.Unauthorized("REFRESH_TOKEN_INVALID", "登录续期凭据无效")
	}
	accessToken, accessExpiresAt, err := s.accessTokens.Issue(result.Principal)
	if err != nil {
		return nil, kratoserrors.InternalServer("ACCESS_TOKEN_ISSUE_FAILED", "服务端无法完成续期")
	}
	return &securityv1.RefreshResponse{
		AccessToken: accessToken, AccessTokenExpiresAt: timestamppb.New(accessExpiresAt),
		RefreshToken: result.RefreshToken, RefreshTokenExpiresAt: timestamppb.New(result.ExpiresAt),
	}, nil
}

// Logout 撤销当前设备会话；重复执行保持幂等。
func (s *BearerService) Logout(ctx context.Context, _ *securityv1.LogoutRequest) (*securityv1.PlayerSecurityServiceLogoutResponse, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	if err := s.logout.Logout(ctx, principal); err != nil && !errors.Is(err, authentication.ErrInvalidSession) {
		return nil, kratoserrors.InternalServer("LOGOUT_FAILED", "服务端无法完成注销")
	}
	return &securityv1.PlayerSecurityServiceLogoutResponse{Body: &securityv1.PlayerLogoutResult{LoggedOut: true}}, nil
}

// GetCurrentSession 返回当前玩家账号的最小身份快照。
func (s *BearerService) GetCurrentSession(ctx context.Context, _ *securityv1.GetCurrentSessionRequest) (*securityv1.GetCurrentSessionResponse, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	current, err := s.current.Get(ctx, principal.AccountID)
	if err != nil {
		return nil, kratoserrors.InternalServer("SESSION_QUERY_FAILED", "服务端无法读取登录状态")
	}
	return &securityv1.GetCurrentSessionResponse{User: &securityv1.SessionUser{Id: current.ID.String(), Username: current.Username, DisplayName: current.DisplayName}}, nil
}

// ListSessions 返回当前账号仍有效的全部设备会话。
func (s *BearerService) ListSessions(ctx context.Context, _ *securityv1.ListSessionsRequest) (*securityv1.ListSessionsResponse, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	families, err := s.sessions.List(ctx, principal)
	if err != nil {
		return nil, kratoserrors.InternalServer("SESSION_LIST_FAILED", "服务端无法读取设备会话")
	}
	result := make([]*securityv1.Session, len(families))
	for index, family := range families {
		result[index] = &securityv1.Session{Id: family.FamilyID.String(), Current: family.Current, DeviceSummary: family.DeviceSummary, CreatedAt: timestamppb.New(family.CreatedAt), LastActivityAt: timestamppb.New(family.LastActivityAt), IdleExpiresAt: timestamppb.New(family.IdleExpiresAt), ExpiresAt: timestamppb.New(family.ExpiresAt)}
	}
	return &securityv1.ListSessionsResponse{Sessions: result}, nil
}

// RevokeSession 幂等撤销当前账号拥有的指定设备会话。
func (s *BearerService) RevokeSession(ctx context.Context, request *securityv1.RevokeSessionRequest) (*securityv1.PlayerSecurityServiceRevokeSessionResponse, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	sessionID, err := snowflake.Parse(request.GetSessionId())
	if err != nil {
		return nil, kratoserrors.BadRequest("INVALID_SESSION_ID", "设备会话标识无效")
	}
	if err := s.sessions.Revoke(ctx, principal, sessionID, requestIDFromContext(ctx)); err != nil {
		return nil, kratoserrors.InternalServer("SESSION_REVOKE_FAILED", "服务端无法撤销设备会话")
	}
	return &securityv1.PlayerSecurityServiceRevokeSessionResponse{Body: &securityv1.PlayerSessionRevoked{Revoked: true}}, nil
}

func requestIDFromContext(ctx context.Context) string {
	if incoming, ok := metadata.FromIncomingContext(ctx); ok {
		if values := incoming.Get("x-request-id"); len(values) > 0 && strings.TrimSpace(values[0]) != "" {
			return strings.TrimSpace(values[0])
		}
	}
	return requestid.New()
}

func deviceSummaryFromContext(ctx context.Context) string {
	if incoming, ok := metadata.FromIncomingContext(ctx); ok {
		if values := incoming.Get("user-agent"); len(values) > 0 {
			value := strings.Map(func(character rune) rune {
				if character < 0x20 || character == 0x7f {
					return -1
				}
				return character
			}, strings.TrimSpace(values[0]))
			characters := []rune(value)
			if len(characters) > maximumDeviceSummaryLength {
				characters = characters[:maximumDeviceSummaryLength]
			}
			return string(characters)
		}
	}
	return "unknown"
}
