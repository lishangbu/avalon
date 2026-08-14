package asset_test

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/binary"
	"encoding/hex"
	"errors"
	"hash/crc32"
	"image"
	"image/color"
	"image/png"
	"io"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/asset"
)

func TestServiceKeepsBlobCallsOutsidePostgreSQLTransactions(t *testing.T) {
	t.Parallel()
	now := time.Date(2026, time.July, 28, 10, 0, 0, 0, time.UTC)
	actorID := snowflake.MustParse("1048576054")
	assetID := snowflake.MustParse("1048576055")
	objectID := snowflake.MustParse("1048576056")
	raw := pngBytes(t, 2, 3)
	digest := sha256.Sum256(raw)
	store := &repositoryStub{}
	blobs := &blobStoreStub{repository: store, raw: raw, mediaType: "image/png"}
	ids := []snowflake.ID{assetID, objectID}
	service := asset.NewService(store, blobs, snowflake.TestSource(func() snowflake.ID {
		value := ids[0]
		ids = ids[1:]
		return value
	}), func() time.Time { return now })

	grant, err := service.BeginUpload(context.Background(), asset.BeginUploadCommand{
		CommandContext: asset.CommandContext{ActorAccountID: actorID, IdempotencyKey: "begin-upload", RequestID: "request-begin"},
		MediaType:      "image/png", ExpectedSize: int64(len(raw)), ExpectedSHA256: hex.EncodeToString(digest[:]),
	})
	if err != nil || grant.Asset.ID != assetID || grant.Asset.Status != asset.StatusPending || grant.URL == "" {
		t.Fatalf("BeginUpload() = %+v, error = %v", grant, err)
	}
	ready, err := service.Confirm(context.Background(), asset.ConfirmCommand{
		CommandContext: asset.CommandContext{ActorAccountID: actorID, IdempotencyKey: "confirm-upload", RequestID: "request-confirm"},
		AssetID:        assetID, ExpectedVersion: 1,
	})
	if err != nil || ready.Status != asset.StatusReady || ready.Width == nil || *ready.Width != 2 || ready.Height == nil || *ready.Height != 3 {
		t.Fatalf("Confirm() = %+v, error = %v", ready, err)
	}
	if blobs.calledInsideTransaction {
		t.Fatal("RustFS call occurred inside PostgreSQL transaction")
	}
}

func TestServiceRejectsSpoofedImageBytes(t *testing.T) {
	t.Parallel()
	actorID, assetID := snowflake.NewTestID(), snowflake.NewTestID()
	raw := []byte("<svg xmlns='http://www.w3.org/2000/svg'></svg>")
	digest := sha256.Sum256(raw)
	pending := asset.Asset{
		ID: assetID, OwnerAccountID: actorID, ObjectKey: "assets/private/object", Status: asset.StatusPending,
		MediaType: "image/png", ExpectedSize: int64(len(raw)), ExpectedSHA256: digest[:], Version: 1,
	}
	store := &repositoryStub{value: pending}
	service := asset.NewService(store, &blobStoreStub{repository: store, raw: raw, mediaType: "image/png"}, snowflake.NewTestID, time.Now)
	_, err := service.Confirm(context.Background(), asset.ConfirmCommand{
		CommandContext: asset.CommandContext{ActorAccountID: actorID, IdempotencyKey: "confirm-spoof", RequestID: "request-spoof"},
		AssetID:        assetID, ExpectedVersion: 1,
	})
	if !errors.Is(err, asset.ErrAssetContentInvalid) || store.readyCalled {
		t.Fatalf("Confirm() error = %v, readyCalled = %v", err, store.readyCalled)
	}
}

func TestServiceRejectsCompactImageDecodeBomb(t *testing.T) {
	t.Parallel()
	actorID, assetID := snowflake.NewTestID(), snowflake.NewTestID()
	// 仅几十字节的 PNG IHDR 声明超过 1600 万像素，模拟高压缩比解码炸弹。
	raw := compactPNGHeader(4001, 4000)
	digest := sha256.Sum256(raw)
	pending := asset.Asset{
		ID: assetID, OwnerAccountID: actorID, ObjectKey: "assets/private/decode-bomb", Status: asset.StatusPending,
		MediaType: "image/png", ExpectedSize: int64(len(raw)), ExpectedSHA256: digest[:], Version: 1,
	}
	store := &repositoryStub{value: pending}
	service := asset.NewService(store, &blobStoreStub{repository: store, raw: raw, mediaType: "image/png"}, snowflake.NewTestID, time.Now)
	_, err := service.Confirm(context.Background(), asset.ConfirmCommand{
		CommandContext: asset.CommandContext{
			ActorAccountID: actorID, IdempotencyKey: "confirm-decode-bomb", RequestID: "request-decode-bomb",
		},
		AssetID: assetID, ExpectedVersion: 1,
	})
	if !errors.Is(err, asset.ErrAssetContentInvalid) || store.readyCalled {
		t.Fatalf("Confirm() error = %v, readyCalled = %v", err, store.readyCalled)
	}
}

func TestServiceReplaysCompletedConfirmationWithoutReadingBlobAgain(t *testing.T) {
	t.Parallel()
	actorID, assetID := snowflake.NewTestID(), snowflake.NewTestID()
	actualSize, width, height := int64(128), int32(2), int32(3)
	digest := sha256.Sum256([]byte("verified"))
	readyAt := time.Date(2026, time.July, 28, 10, 30, 0, 0, time.UTC)
	store := &repositoryStub{value: asset.Asset{
		ID: assetID, OwnerAccountID: actorID, Status: asset.StatusReady, Version: 2,
		ActualSize: &actualSize, ActualSHA256: digest[:], Width: &width, Height: &height, ReadyAt: &readyAt,
	}}
	blobs := &blobStoreStub{repository: store}
	service := asset.NewService(store, blobs, snowflake.NewTestID, time.Now)

	result, err := service.Confirm(context.Background(), asset.ConfirmCommand{
		CommandContext: asset.CommandContext{ActorAccountID: actorID, IdempotencyKey: "confirm-replay", RequestID: "request-replay"},
		AssetID:        assetID, ExpectedVersion: 1,
	})
	if err != nil || result.Status != asset.StatusReady {
		t.Fatalf("Confirm() = %+v, error = %v", result, err)
	}
	if blobs.getCalls != 0 {
		t.Fatalf("blob Get calls = %d, want 0", blobs.getCalls)
	}
}

// TestServiceReturnsStablePublicReadURL 验证 Ready Asset 经过数据库归属检查后返回稳定公开地址，
// 地址不包含签名查询参数，也不伪造一个并不存在的读取过期时间。
func TestServiceReturnsStablePublicReadURL(t *testing.T) {
	t.Parallel()

	actorID, assetID := snowflake.NewTestID(), snowflake.NewTestID()
	store := &repositoryStub{value: asset.Asset{
		ID: assetID, OwnerAccountID: actorID, ObjectKey: "assets/public/asset.png",
		Status: asset.StatusReady, Version: 2,
	}}
	service := asset.NewService(store, &blobStoreStub{repository: store}, snowflake.NewTestID, time.Now)

	grant, err := service.Download(context.Background(), actorID, assetID)
	if err != nil {
		t.Fatalf("Download() error = %v", err)
	}
	if grant.URL != "https://rustfs.invalid/assets/public/asset.png" {
		t.Fatalf("Download() URL = %q", grant.URL)
	}
}

// TestServiceListsOwnedAssets 验证 Asset 管理列表保留调用账号、分页与状态筛选，
// 并把持久层返回的总条数原样暴露给管理端。
func TestServiceListsOwnedAssets(t *testing.T) {
	t.Parallel()

	actorID := snowflake.MustParse("1048576112")
	readyID := snowflake.MustParse("1048576113")
	store := &repositoryStub{page: asset.Page{
		Items: []asset.Asset{{ID: readyID, OwnerAccountID: actorID, Status: asset.StatusReady}},
		Page:  2, PageSize: 20, Total: 21,
	}}
	service := asset.NewService(store, &blobStoreStub{repository: store}, snowflake.NewTestID, time.Now)

	page, err := service.List(context.Background(), actorID, asset.ListQuery{Page: 2, PageSize: 20, Status: asset.StatusReady})
	if err != nil {
		t.Fatalf("List() error = %v", err)
	}
	if len(page.Items) != 1 || page.Items[0].ID != readyID || page.Total != 21 {
		t.Fatalf("List() = %+v", page)
	}
	if store.listOwnerID != actorID || store.listQuery != (asset.ListQuery{Page: 2, PageSize: 20, Status: asset.StatusReady}) {
		t.Fatalf("ListOwned() owner/query = %s/%+v", store.listOwnerID, store.listQuery)
	}
}

// TestServiceRejectsInvalidAssetList 验证非法页码、每页条数和未知状态不会进入持久层。
func TestServiceRejectsInvalidAssetList(t *testing.T) {
	t.Parallel()

	store := &repositoryStub{}
	service := asset.NewService(store, &blobStoreStub{repository: store}, snowflake.NewTestID, time.Now)
	tests := []asset.ListQuery{
		{Page: 0, PageSize: 20},
		{Page: 1, PageSize: 0},
		{Page: 1, PageSize: 101},
		{Page: 1, PageSize: 20, Status: asset.Status("deleted")},
	}
	for _, query := range tests {
		if _, err := service.List(context.Background(), snowflake.NewTestID(), query); !errors.Is(err, asset.ErrInvalidAsset) {
			t.Fatalf("List(%+v) error = %v, want ErrInvalidAsset", query, err)
		}
	}
	if store.listCalls != 0 {
		t.Fatalf("ListOwned() calls = %d, want 0", store.listCalls)
	}
}

type repositoryStub struct {
	inTransaction bool
	readyCalled   bool
	value         asset.Asset
	page          asset.Page
	listOwnerID   snowflake.ID
	listQuery     asset.ListQuery
	listCalls     int
}

func (s *repositoryStub) ListOwned(_ context.Context, ownerID snowflake.ID, query asset.ListQuery) (asset.Page, error) {
	s.listCalls++
	s.listOwnerID = ownerID
	s.listQuery = query
	return s.page, nil
}

func (s *repositoryStub) GetOwned(_ context.Context, _, _ snowflake.ID) (asset.Asset, error) {
	if s.value.ID == snowflake.ID(0) {
		return asset.Asset{}, asset.ErrAssetNotFound
	}
	return s.value, nil
}

func (s *repositoryStub) WithinAsset(ctx context.Context, work func(asset.Writer) error) error {
	s.inTransaction = true
	err := work((*writerStub)(s))
	s.inTransaction = false
	return err
}

type writerStub repositoryStub

func (w *writerStub) Reserve(_ context.Context, record asset.ReserveRecord) (asset.Asset, error) {
	value := asset.Asset{
		ID: record.ID, OwnerAccountID: record.ActorAccountID, ObjectKey: record.ObjectKey,
		Status: asset.StatusPending, MediaType: record.MediaType, ExpectedSize: record.ExpectedSize,
		ExpectedSHA256: record.ExpectedSHA256, Version: 1, CreatedAt: record.CreatedAt,
	}
	(*repositoryStub)(w).value = value
	return value, nil
}

func (w *writerStub) MarkReady(_ context.Context, record asset.ReadyRecord) (asset.Asset, error) {
	repository := (*repositoryStub)(w)
	repository.readyCalled = true
	value := repository.value
	value.Status, value.Version = asset.StatusReady, value.Version+1
	value.ActualSize, value.ActualSHA256 = &record.ActualSize, record.ActualSHA256
	value.Width, value.Height, value.ReadyAt = &record.Width, &record.Height, &record.ReadyAt
	repository.value = value
	return value, nil
}

type blobStoreStub struct {
	repository              *repositoryStub
	raw                     []byte
	mediaType               string
	calledInsideTransaction bool
	getCalls                int
}

func (s *blobStoreStub) PresignUpload(context.Context, string, string, int64, []byte, time.Duration) (string, map[string]string, error) {
	s.calledInsideTransaction = s.calledInsideTransaction || s.repository.inTransaction
	return "https://rustfs.invalid/upload?signature=secret", map[string]string{"Content-Type": s.mediaType}, nil
}

func (s *blobStoreStub) Get(context.Context, string) (asset.BlobObject, error) {
	s.getCalls++
	s.calledInsideTransaction = s.calledInsideTransaction || s.repository.inTransaction
	return asset.BlobObject{Body: io.NopCloser(bytes.NewReader(s.raw)), Size: int64(len(s.raw)), MediaType: s.mediaType}, nil
}

func (s *blobStoreStub) PublicURL(objectKey string) (string, error) {
	s.calledInsideTransaction = s.calledInsideTransaction || s.repository.inTransaction
	return "https://rustfs.invalid/" + objectKey, nil
}

func pngBytes(t *testing.T, width, height int) []byte {
	t.Helper()
	value := image.NewNRGBA(image.Rect(0, 0, width, height))
	value.Set(0, 0, color.NRGBA{R: 255, A: 255})
	var output bytes.Buffer
	if err := png.Encode(&output, value); err != nil {
		t.Fatalf("png.Encode() error = %v", err)
	}
	return output.Bytes()
}

func compactPNGHeader(width, height uint32) []byte {
	result := append([]byte(nil), []byte("\x89PNG\r\n\x1a\n")...)
	data := make([]byte, 13)
	binary.BigEndian.PutUint32(data[0:4], width)
	binary.BigEndian.PutUint32(data[4:8], height)
	data[8], data[9], data[10], data[11], data[12] = 8, 6, 0, 0, 0
	chunkType := []byte("IHDR")
	length := make([]byte, 4)
	binary.BigEndian.PutUint32(length, uint32(len(data)))
	result = append(result, length...)
	result = append(result, chunkType...)
	result = append(result, data...)
	checksumInput := append(append([]byte(nil), chunkType...), data...)
	checksum := make([]byte, 4)
	binary.BigEndian.PutUint32(checksum, crc32.ChecksumIEEE(checksumInput))
	return append(result, checksum...)
}
