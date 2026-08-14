package api

import (
	"context"
	"errors"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	adminv1 "github.com/lishangbu/avalon/api/gen/go/avalon/admin/v1"
	"github.com/lishangbu/avalon/internal/admin"
	"github.com/lishangbu/avalon/internal/platform/httpapi"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/security/authentication"
	"google.golang.org/protobuf/types/known/timestamppb"
)

// ManagementService 是管理员账号维护和审计查询 RPC 适配器。
type ManagementService struct {
	query      admin.ManagementQuery
	repository admin.ManagementRepository
}

// NewManagementService 创建管理员维护 RPC 服务。
func NewManagementService(query admin.ManagementQuery, repository admin.ManagementRepository) *ManagementService {
	return &ManagementService{query: query, repository: repository}
}

// ListAdminAccounts 返回不含密码资料的管理员账号列表。
func (s *ManagementService) ListAdminAccounts(ctx context.Context, request *adminv1.ListAdminAccountsRequest) (*adminv1.ListAdminAccountsResponse, error) {
	rows, err := s.query.ListAccounts(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, managementAPIError(err)
	}
	response := &adminv1.ListAdminAccountsResponse{Accounts: make([]*adminv1.AdminAccountItem, 0, len(rows))}
	for _, row := range rows {
		response.Accounts = append(response.Accounts, accountMessage(row))
	}
	return response, nil
}

// CreateAdminAccount 创建使用固定 Argon2id 策略的管理员账号。
func (s *ManagementService) CreateAdminAccount(ctx context.Context, request *adminv1.CreateAdminAccountRequest) (*adminv1.CreateAdminAccountResponse, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	value, err := s.repository.CreateAccount(ctx, admin.CreateManagedAccountCommand{ActorAccountID: principal.AccountID, Username: request.GetUsername(), DisplayName: request.GetDisplayName(), Password: request.GetPassword(), IdempotencyKey: request.GetIdempotencyKey(), RequestID: httpapi.RequestIDFromContext(ctx)})
	if err != nil {
		return nil, managementAPIError(err)
	}
	return &adminv1.CreateAdminAccountResponse{Account: accountMessage(value)}, nil
}

// SetAdminAccountEnabled 使用乐观版本启用或停用管理员账号。
func (s *ManagementService) SetAdminAccountEnabled(ctx context.Context, request *adminv1.SetAdminAccountEnabledRequest) (*adminv1.SetAdminAccountEnabledResponse, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok {
		return nil, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	accountID, err := snowflake.Parse(request.GetAccountId())
	if err != nil {
		return nil, kratoserrors.BadRequest("INVALID_ADMIN_ACCOUNT_ID", "管理员账号标识无效")
	}
	value, err := s.repository.SetAccountEnabled(ctx, admin.SetManagedAccountEnabledCommand{ActorAccountID: principal.AccountID, AccountID: accountID, Enabled: request.GetEnabled(), ExpectedVersion: request.GetExpectedVersion(), IdempotencyKey: request.GetIdempotencyKey(), RequestID: httpapi.RequestIDFromContext(ctx)})
	if err != nil {
		return nil, managementAPIError(err)
	}
	return &adminv1.SetAdminAccountEnabledResponse{Account: accountMessage(value)}, nil
}

// ListAdminAuditLogs 返回隐藏哈希链字节的管理员安全审计。
func (s *ManagementService) ListAdminAuditLogs(ctx context.Context, request *adminv1.ListAdminAuditLogsRequest) (*adminv1.ListAdminAuditLogsResponse, error) {
	rows, err := s.query.ListAuditLogs(ctx, int(request.GetPageSize()))
	if err != nil {
		return nil, managementAPIError(err)
	}
	response := &adminv1.ListAdminAuditLogsResponse{Logs: make([]*adminv1.AdminAuditLogItem, 0, len(rows))}
	for _, row := range rows {
		actorID := ""
		if row.ActorAccountID.IsValid() {
			actorID = row.ActorAccountID.String()
		}
		response.Logs = append(response.Logs, &adminv1.AdminAuditLogItem{Id: row.ID.String(), Sequence: row.Sequence, ActorAccountId: actorID, ActorKind: row.ActorKind, ActorIdentifier: row.ActorIdentifier, ActionCode: row.ActionCode, ObjectType: row.ObjectType, ObjectId: row.ObjectID, RequestId: row.RequestID, Reason: row.Reason, CreatedAt: timestamppb.New(row.CreatedAt)})
	}
	return response, nil
}

func accountMessage(value admin.ManagedAccount) *adminv1.AdminAccountItem {
	return &adminv1.AdminAccountItem{Id: value.ID.String(), Username: value.Username, DisplayName: value.DisplayName, Status: value.Status, Version: value.Version, CreatedAt: timestamppb.New(value.CreatedAt), UpdatedAt: timestamppb.New(value.UpdatedAt)}
}
func managementAPIError(err error) error {
	switch {
	case errors.Is(err, admin.ErrInvalidManagementCommand):
		return kratoserrors.BadRequest("INVALID_ADMIN_ACCOUNT", "管理员账号字段无效")
	case errors.Is(err, admin.ErrCannotDisableSelf):
		return kratoserrors.BadRequest("CANNOT_DISABLE_SELF", "不能停用当前管理员账号")
	case errors.Is(err, admin.ErrAdminAccountNotFound):
		return kratoserrors.NotFound("ADMIN_ACCOUNT_NOT_FOUND", "管理员账号不存在")
	case errors.Is(err, admin.ErrAdminAccountConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("ADMIN_ACCOUNT_CONFLICT", "管理员账号用户名、版本或幂等请求冲突")
	default:
		return kratoserrors.InternalServer("ADMIN_MANAGEMENT_FAILED", "管理员维护请求失败")
	}
}
