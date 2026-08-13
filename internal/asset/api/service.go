package api

import (
	"context"
	"encoding/hex"
	"errors"
	"log/slog"
	"sort"
	"strconv"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/asset"
	"github.com/lishangbu/avalon/internal/platform/httpapi"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// KratosService 直接实现 GameDataService 中的 Asset 上传、确认和下载 RPC。
//
// 对象存储签名只存在于返回值中，日志边界不会记录 URL、Header 或底层 SDK 错误文本。
type KratosService struct {
	// application 提供不依赖 HTTP 的 Asset 上传、确认和下载应用用例。
	application Service
	// logger 只记录稳定错误上下文，不记录对象签名信息。
	logger *slog.Logger
}

// NewKratosService 使用 Asset 应用边界创建原生 Kratos 服务。
func NewKratosService(application Service, logger *slog.Logger) *KratosService {
	if logger == nil {
		logger = slog.Default()
	}
	return &KratosService{application: application, logger: logger}
}

// ListAssets 分页返回当前认证账号拥有的 Asset，不泄露其他账号对象是否存在。
func (service *KratosService) ListAssets(
	ctx context.Context,
	request *domainv1.ListAssetsRequest,
) (*domainv1.ListAssetsResponse, error) {
	principal, err := assetPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	page, listErr := service.application.List(ctx, principal.AccountID, asset.ListQuery{
		Page: request.GetPage(), PageSize: request.GetPageSize(), Status: asset.Status(request.GetStatus()),
	})
	if listErr != nil {
		return nil, service.assetError(ctx, "ASSET_LIST_FAILED", listErr)
	}
	items := make([]*domainv1.Asset, 0, len(page.Items))
	for _, item := range page.Items {
		items = append(items, assetMessage(item))
	}
	return &domainv1.ListAssetsResponse{HttpStatusCode: 200, Body: &domainv1.AssetPage{
		Items: items, Page: page.Page, PageSize: page.PageSize, Total: page.Total,
	}}, nil
}

// CreateAssetUpload 创建 Pending Asset 并签发短期上传请求。
func (service *KratosService) CreateAssetUpload(
	ctx context.Context,
	request *domainv1.CreateAssetUploadRequest,
) (*domainv1.CreateAssetUploadResponse, error) {
	principal, err := assetPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	grant, beginErr := service.application.BeginUpload(ctx, asset.BeginUploadCommand{
		CommandContext: asset.CommandContext{
			ActorAccountID: principal.AccountID, IdempotencyKey: request.GetHeaderIdempotencyKey(),
			RequestID: httpapi.RequestIDFromContext(ctx),
		},
		MediaType: request.GetBody().GetMediaType(), ExpectedSize: request.GetBody().GetExpectedSize(),
		ExpectedSHA256: request.GetBody().GetExpectedSha256(),
	})
	if beginErr != nil {
		return nil, service.assetError(ctx, "ASSET_UPLOAD_CREATE_FAILED", beginErr)
	}
	return &domainv1.CreateAssetUploadResponse{HttpStatusCode: 201, Body: assetUploadGrantMessage(grant)}, nil
}

// ConfirmAssetUpload 校验 RustFS 对象并把 Pending Asset 原子转换为 Ready。
func (service *KratosService) ConfirmAssetUpload(
	ctx context.Context,
	request *domainv1.ConfirmAssetUploadRequest,
) (*domainv1.ConfirmAssetUploadResponse, error) {
	principal, err := assetPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	assetID, err := snowflake.Parse(request.GetAssetId())
	version, versionErr := strconv.ParseInt(request.GetBody().GetExpectedVersion(), 10, 64)
	if err != nil || assetID == snowflake.ID(0) || versionErr != nil || version < 1 {
		return nil, kratoserrors.BadRequest("INVALID_REQUEST", "请求字段无效")
	}
	value, confirmErr := service.application.Confirm(ctx, asset.ConfirmCommand{
		CommandContext: asset.CommandContext{
			ActorAccountID: principal.AccountID, IdempotencyKey: request.GetHeaderIdempotencyKey(),
			RequestID: httpapi.RequestIDFromContext(ctx),
		},
		AssetID: assetID, ExpectedVersion: version,
	})
	if confirmErr != nil {
		return nil, service.assetError(ctx, "ASSET_UPLOAD_CONFIRM_FAILED", confirmErr)
	}
	return &domainv1.ConfirmAssetUploadResponse{HttpStatusCode: 200, Body: assetMessage(value)}, nil
}

// CreateAssetDownload 对 Ready Asset 执行数据库归属检查并返回稳定公开读取位置。
func (service *KratosService) CreateAssetDownload(
	ctx context.Context,
	request *domainv1.CreateAssetDownloadRequest,
) (*domainv1.CreateAssetDownloadResponse, error) {
	principal, err := assetPrincipal(ctx)
	if err != nil {
		return nil, err
	}
	assetID, err := snowflake.Parse(request.GetAssetId())
	if err != nil || assetID == snowflake.ID(0) {
		return nil, kratoserrors.BadRequest("INVALID_ASSET_ID", "Asset 标识格式无效")
	}
	grant, downloadErr := service.application.Download(ctx, principal.AccountID, assetID)
	if downloadErr != nil {
		return nil, service.assetError(ctx, "ASSET_DOWNLOAD_CREATE_FAILED", downloadErr)
	}
	return &domainv1.CreateAssetDownloadResponse{
		HttpStatusCode: 200,
		Body:           &domainv1.AssetReadLocation{Url: grant.URL},
	}, nil
}

func assetPrincipal(ctx context.Context) (authentication.Principal, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok || principal.AccountID == snowflake.ID(0) {
		return authentication.Principal{}, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	return principal, nil
}

func assetUploadGrantMessage(value asset.UploadGrant) *domainv1.AssetUploadGrant {
	names := make([]string, 0, len(value.Headers))
	for name := range value.Headers {
		names = append(names, name)
	}
	sort.Strings(names)
	headers := make([]*domainv1.SignedHeader, 0, len(names))
	for _, name := range names {
		headers = append(headers, &domainv1.SignedHeader{Name: name, Value: value.Headers[name]})
	}
	return &domainv1.AssetUploadGrant{
		Asset: assetMessage(value.Asset), Method: "PUT", Url: value.URL, Headers: headers,
		ExpiresAt: value.ExpiresAt.UTC().Format(time.RFC3339Nano),
	}
}

func assetMessage(value asset.Asset) *domainv1.Asset {
	actualSize := int64(0)
	if value.ActualSize != nil {
		actualSize = *value.ActualSize
	}
	width := int32(0)
	if value.Width != nil {
		width = *value.Width
	}
	height := int32(0)
	if value.Height != nil {
		height = *value.Height
	}
	readyAt := ""
	if value.ReadyAt != nil {
		readyAt = value.ReadyAt.UTC().Format(time.RFC3339Nano)
	}
	return &domainv1.Asset{
		Id: value.ID.String(), Status: string(value.Status), MediaType: value.MediaType, ExpectedSize: value.ExpectedSize,
		ExpectedSha256: hex.EncodeToString(value.ExpectedSHA256), ActualSize: actualSize,
		ActualSha256: hex.EncodeToString(value.ActualSHA256), Width: width, Height: height,
		Version: strconv.FormatInt(value.Version, 10), CreatedAt: value.CreatedAt.UTC().Format(time.RFC3339Nano), ReadyAt: readyAt,
	}
}

func (service *KratosService) assetError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, asset.ErrInvalidAsset):
		return kratoserrors.BadRequest("INVALID_ASSET", "Asset 请求字段无效")
	case errors.Is(err, asset.ErrAssetContentInvalid):
		return kratoserrors.BadRequest("ASSET_CONTENT_INVALID", "上传对象未通过校验")
	case errors.Is(err, asset.ErrAssetNotFound):
		return kratoserrors.NotFound("ASSET_NOT_FOUND", "Asset 不存在")
	case errors.Is(err, asset.ErrAssetConflict), errors.Is(err, idempotency.ErrConflict):
		return kratoserrors.Conflict("ASSET_CONFLICT", "Asset 状态、版本或幂等请求冲突")
	default:
		// 对象存储 SDK 错误可能包含签名 URL，因此仅记录稳定事件和追踪标识。
		service.logger.ErrorContext(ctx, "Asset Kratos 服务调用失败", "reason", reason,
			"requestId", httpapi.RequestIDFromContext(ctx))
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
