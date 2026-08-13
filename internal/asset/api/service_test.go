package api_test

import (
	"context"
	"io"
	"log/slog"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	domainv1 "github.com/lishangbu/avalon/api/gen/go/avalon/domain/v1"
	"github.com/lishangbu/avalon/internal/asset"
	assetapi "github.com/lishangbu/avalon/internal/asset/api"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// TestKratosServiceBeginsAssetUpload 验证对象上传直接通过生成的 Proto 契约进入 Asset 应用服务。
func TestKratosServiceBeginsAssetUpload(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576106")
	assetID := snowflake.MustParse("1048576107")
	now := time.Date(2026, time.July, 30, 3, 0, 0, 0, time.UTC)
	application := &serviceStub{upload: asset.UploadGrant{
		Asset: asset.Asset{ID: assetID, MediaType: "image/png", Version: 1, CreatedAt: now},
		URL:   "https://objects.example/upload", ExpiresAt: now.Add(time.Minute),
	}}
	service := assetapi.NewKratosService(application, slog.New(slog.NewTextHandler(io.Discard, nil)))
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.CreateAssetUpload(ctx, &domainv1.CreateAssetUploadRequest{
		HeaderIdempotencyKey: "create-asset",
		Body: &domainv1.CreateAssetUploadBody{
			MediaType: "image/png", ExpectedSize: 128, ExpectedSha256: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
		},
	})
	if err != nil {
		t.Fatalf("CreateAssetUpload() error = %v", err)
	}
	if response.GetHttpStatusCode() != 201 || response.GetBody().GetAsset().GetId() != assetID.String() {
		t.Fatalf("CreateAssetUpload() = %#v", response)
	}
	if application.beginCommand.ActorAccountID != accountID || application.beginCommand.IdempotencyKey != "create-asset" {
		t.Fatalf("BeginUpload command = %+v", application.beginCommand)
	}
}

// TestKratosServiceReturnsPublicAssetLocation 验证下载契约只返回不会过期的公开对象地址，
// 不再把公开读取伪装成带有效期的预签名授权。
func TestKratosServiceReturnsPublicAssetLocation(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576110")
	assetID := snowflake.MustParse("1048576111")
	application := &serviceStub{download: asset.DownloadGrant{URL: "https://objects.example/avalon-assets/assets/ready.png"}}
	service := assetapi.NewKratosService(application, slog.New(slog.NewTextHandler(io.Discard, nil)))
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.CreateAssetDownload(ctx, &domainv1.CreateAssetDownloadRequest{AssetId: assetID.String()})
	if err != nil {
		t.Fatalf("CreateAssetDownload() error = %v", err)
	}
	if response.GetBody().GetUrl() != application.download.URL {
		t.Fatalf("CreateAssetDownload() = %#v", response.GetBody())
	}
}

// TestKratosServiceListsOwnedAssets 验证认证账号和页码筛选通过生成契约进入应用边界，
// 响应中的 Identifier 与版本继续保持字符串。
func TestKratosServiceListsOwnedAssets(t *testing.T) {
	t.Parallel()

	accountID := snowflake.MustParse("1048576114")
	assetID := snowflake.MustParse("1048576115")
	application := &serviceStub{page: asset.Page{
		Items: []asset.Asset{{ID: assetID, OwnerAccountID: accountID, Status: asset.StatusReady, Version: 2}},
		Page:  2, PageSize: 10, Total: 11,
	}}
	service := assetapi.NewKratosService(application, slog.New(slog.NewTextHandler(io.Discard, nil)))
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.ListAssets(ctx, &domainv1.ListAssetsRequest{Page: 2, PageSize: 10, Status: "ready"})
	if err != nil {
		t.Fatalf("ListAssets() error = %v", err)
	}
	if response.GetBody().GetTotal() != 11 || len(response.GetBody().GetItems()) != 1 ||
		response.GetBody().GetItems()[0].GetId() != assetID.String() || response.GetBody().GetItems()[0].GetVersion() != "2" {
		t.Fatalf("ListAssets() = %#v", response.GetBody())
	}
	if application.listOwnerID != accountID || application.listQuery != (asset.ListQuery{Page: 2, PageSize: 10, Status: asset.StatusReady}) {
		t.Fatalf("List query = %s/%+v", application.listOwnerID, application.listQuery)
	}
}

type serviceStub struct {
	upload       asset.UploadGrant
	download     asset.DownloadGrant
	page         asset.Page
	beginCommand asset.BeginUploadCommand
	listOwnerID  snowflake.ID
	listQuery    asset.ListQuery
}

func (stub *serviceStub) List(_ context.Context, ownerID snowflake.ID, query asset.ListQuery) (asset.Page, error) {
	stub.listOwnerID = ownerID
	stub.listQuery = query
	return stub.page, nil
}

func (stub *serviceStub) BeginUpload(_ context.Context, command asset.BeginUploadCommand) (asset.UploadGrant, error) {
	stub.beginCommand = command
	return stub.upload, nil
}
func (*serviceStub) Confirm(context.Context, asset.ConfirmCommand) (asset.Asset, error) {
	return asset.Asset{}, nil
}
func (stub *serviceStub) Download(context.Context, snowflake.ID, snowflake.ID) (asset.DownloadGrant, error) {
	return stub.download, nil
}
