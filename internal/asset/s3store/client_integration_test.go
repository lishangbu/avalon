//go:build integration

package s3store_test

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"errors"
	"fmt"
	"image"
	"image/color"
	"image/png"
	"io"
	"net/http"
	"net/url"
	"sort"
	"strconv"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/aws/aws-sdk-go-v2/aws"
	awsconfig "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/credentials"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/lishangbu/avalon/internal/asset"
	"github.com/lishangbu/avalon/internal/asset/s3store"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/wait"
)

const rustFSImage = "rustfs/rustfs:latest"

func TestRustFSPublicReadAuthenticatedWriteAssetLifecycle(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Minute)
	defer cancel()

	endpoint, bucket, accessKey, secretKey, terminate := startRustFS(t, ctx)
	t.Cleanup(terminate)
	admin := newS3Client(t, ctx, endpoint, accessKey, secretKey)
	createBucket(t, ctx, admin, bucket)
	blobs, err := s3store.New(ctx, s3store.Config{
		Endpoint: endpoint, Region: "us-east-1", Bucket: bucket,
		AccessKeyID: accessKey, SecretAccessKey: secretKey, UsePathStyle: true,
	})
	if err != nil {
		t.Fatalf("创建 RustFS 适配器: %v", err)
	}
	if err := blobs.Ready(ctx); err == nil {
		t.Fatal("RustFS Ready() 应拒绝尚未开放匿名 GetObject 的 Bucket")
	}
	setPublicReadPolicy(t, ctx, admin, bucket)
	if err := blobs.Ready(ctx); err != nil {
		t.Fatalf("RustFS Ready(): %v", err)
	}

	ownerID := snowflake.MustParse("1048576059")
	now := time.Date(2026, time.July, 28, 12, 0, 0, 0, time.UTC)
	raw := rustFSPNG(t, 5, 4)
	digest := sha256.Sum256(raw)
	repository := newMemoryRepository()
	service := asset.NewService(repository, blobs, snowflake.NewTestID, func() time.Time { return now })
	grant, err := service.BeginUpload(ctx, asset.BeginUploadCommand{
		CommandContext: asset.CommandContext{
			ActorAccountID: ownerID, IdempotencyKey: "rustfs-upload", RequestID: "rustfs-upload-request",
		},
		MediaType: "image/png", ExpectedSize: int64(len(raw)), ExpectedSHA256: hex.EncodeToString(digest[:]),
	})
	if err != nil {
		t.Fatalf("BeginUpload(): %v", err)
	}
	if !containsHeader(grant.Headers, "If-None-Battle", "*") ||
		!containsHeader(grant.Headers, "Content-Type", "image/png") ||
		!containsHeader(grant.Headers, "Content-Length", strconv.Itoa(len(raw))) ||
		!containsSignedValue(grant.URL, grant.Headers, "x-amz-checksum-sha256", base64.StdEncoding.EncodeToString(digest[:])) {
		t.Fatalf("上传授权缺少不可变对象约束: headers=%v", grant.Headers)
	}
	tamperedMediaType := cloneHeaders(grant.Headers)
	setHeader(tamperedMediaType, "Content-Type", "image/jpeg")
	if status := signedPut(t, ctx, grant.URL, tamperedMediaType, raw); status != http.StatusForbidden {
		t.Fatalf("篡改 Content-Type status = %d, want %d", status, http.StatusForbidden)
	}
	tamperedChecksum := cloneHeaders(grant.Headers)
	tamperedChecksumURL := grant.URL
	if containsHeaderName(tamperedChecksum, "x-amz-checksum-sha256") {
		setHeader(tamperedChecksum, "x-amz-checksum-sha256", base64.StdEncoding.EncodeToString(make([]byte, sha256.Size)))
	} else {
		tamperedChecksumURL = replaceQueryValue(
			t, grant.URL, "x-amz-checksum-sha256", base64.StdEncoding.EncodeToString(make([]byte, sha256.Size)),
		)
	}
	if status := signedPut(t, ctx, tamperedChecksumURL, tamperedChecksum, raw); status != http.StatusForbidden {
		t.Fatalf("篡改 SHA-256 status = %d, want %d", status, http.StatusForbidden)
	}
	tamperedLength := cloneHeaders(grant.Headers)
	setHeader(tamperedLength, "Content-Length", strconv.Itoa(len(raw)+1))
	if status := signedPut(t, ctx, grant.URL, tamperedLength, append(append([]byte(nil), raw...), 0)); status != http.StatusForbidden {
		t.Fatalf("篡改 Content-Length status = %d, want %d", status, http.StatusForbidden)
	}
	firstStatus := signedPut(t, ctx, grant.URL, grant.Headers, raw)
	if firstStatus < http.StatusOK || firstStatus >= http.StatusMultipleChoices {
		t.Fatalf("首次预签名 PUT status = %d", firstStatus)
	}
	secondStatus := signedPut(t, ctx, grant.URL, grant.Headers, raw)
	if secondStatus != http.StatusPreconditionFailed {
		t.Fatalf("重复预签名 PUT status = %d, want %d", secondStatus, http.StatusPreconditionFailed)
	}
	unsignedWrite, err := http.NewRequestWithContext(
		ctx,
		http.MethodPut,
		endpoint+"/"+bucket+"/assets/anonymous-write-must-fail.png",
		bytes.NewReader(raw),
	)
	if err != nil {
		t.Fatalf("构造匿名 PUT: %v", err)
	}
	unsignedWriteResponse, err := httpClient().Do(unsignedWrite)
	if err != nil {
		t.Fatalf("执行匿名 PUT: %v", err)
	}
	_ = unsignedWriteResponse.Body.Close()
	if unsignedWriteResponse.StatusCode != http.StatusForbidden {
		t.Fatalf("匿名 PUT status = %d, want %d", unsignedWriteResponse.StatusCode, http.StatusForbidden)
	}

	ready, err := service.Confirm(ctx, asset.ConfirmCommand{
		CommandContext: asset.CommandContext{
			ActorAccountID: ownerID, IdempotencyKey: "rustfs-confirm", RequestID: "rustfs-confirm-request",
		},
		AssetID: grant.Asset.ID, ExpectedVersion: 1,
	})
	if err != nil || ready.Status != asset.StatusReady || ready.Width == nil || *ready.Width != 5 ||
		ready.Height == nil || *ready.Height != 4 {
		t.Fatalf("Confirm() = %+v, error = %v", ready, err)
	}
	anonymousResponse, err := httpClient().Get(endpoint + "/" + bucket + "/" + grant.Asset.ObjectKey)
	if err != nil {
		t.Fatalf("匿名 GET: %v", err)
	}
	_ = anonymousResponse.Body.Close()
	if anonymousResponse.StatusCode != http.StatusOK {
		t.Fatalf("匿名 GET status = %d, want %d", anonymousResponse.StatusCode, http.StatusOK)
	}
	download, err := service.Download(ctx, ownerID, ready.ID)
	if err != nil {
		t.Fatalf("Download(): %v", err)
	}
	downloadResponse, err := httpClient().Get(download.URL)
	if err != nil {
		t.Fatalf("公开 GET: %v", err)
	}
	downloaded, readErr := io.ReadAll(downloadResponse.Body)
	_ = downloadResponse.Body.Close()
	if readErr != nil || downloadResponse.StatusCode != http.StatusOK || !bytes.Equal(downloaded, raw) {
		t.Fatalf("公开 GET status=%d bytes=%d readError=%v", downloadResponse.StatusCode, len(downloaded), readErr)
	}

	t.Run("拒绝伪造 MIME", func(t *testing.T) {
		assertInvalidStoredObject(t, ctx, admin, blobs, bucket, invalidObjectCase{
			declared: raw, stored: raw, storedMediaType: "image/jpeg",
		})
	})
	t.Run("拒绝错误魔数", func(t *testing.T) {
		spoofed := []byte("<svg xmlns='http://www.w3.org/2000/svg'></svg>")
		assertInvalidStoredObject(t, ctx, admin, blobs, bucket, invalidObjectCase{
			declared: spoofed, stored: spoofed, storedMediaType: "image/png",
		})
	})
	t.Run("拒绝错误 SHA-256", func(t *testing.T) {
		changed := append([]byte(nil), raw...)
		changed[len(changed)-1] ^= 0xff
		assertInvalidStoredObject(t, ctx, admin, blobs, bucket, invalidObjectCase{
			declared: raw, stored: changed, storedMediaType: "image/png",
		})
	})
	t.Run("拒绝超大图片尺寸", func(t *testing.T) {
		oversized := rustFSPNG(t, 8193, 1)
		assertInvalidStoredObject(t, ctx, admin, blobs, bucket, invalidObjectCase{
			declared: oversized, stored: oversized, storedMediaType: "image/png",
		})
	})
}

// setPublicReadPolicy 只向匿名调用者开放对象读取；列举、上传、覆盖和删除仍要求服务账号认证。
func setPublicReadPolicy(t *testing.T, ctx context.Context, admin *s3.Client, bucket string) {
	t.Helper()
	publicPolicy := fmt.Sprintf(`{
		"Version":"2012-10-17",
		"Statement":[{
			"Effect":"Allow","Principal":"*","Action":"s3:GetObject",
			"Resource":"arn:aws:s3:::%s/assets/*"
		}]
	}`, bucket)
	if _, err := admin.PutBucketPolicy(ctx, &s3.PutBucketPolicyInput{
		Bucket: aws.String(bucket), Policy: aws.String(publicPolicy),
	}); err != nil {
		t.Fatalf("设置 RustFS 公开读取策略: %v", err)
	}
}

type invalidObjectCase struct {
	declared        []byte
	stored          []byte
	storedMediaType string
}

func assertInvalidStoredObject(
	t *testing.T,
	ctx context.Context,
	admin *s3.Client,
	blobs *s3store.Client,
	bucket string,
	testCase invalidObjectCase,
) {
	t.Helper()
	ownerID := snowflake.NewTestID()
	now := time.Date(2026, time.July, 28, 12, 30, 0, 0, time.UTC)
	declaredDigest := sha256.Sum256(testCase.declared)
	repository := newMemoryRepository()
	service := asset.NewService(repository, blobs, snowflake.NewTestID, func() time.Time { return now })
	grant, err := service.BeginUpload(ctx, asset.BeginUploadCommand{
		CommandContext: asset.CommandContext{
			ActorAccountID: ownerID, IdempotencyKey: snowflake.NewTestID().String(), RequestID: snowflake.NewTestID().String(),
		},
		MediaType: "image/png", ExpectedSize: int64(len(testCase.declared)),
		ExpectedSHA256: hex.EncodeToString(declaredDigest[:]),
	})
	if err != nil {
		t.Fatalf("BeginUpload(): %v", err)
	}
	_, err = admin.PutObject(ctx, &s3.PutObjectInput{
		Bucket: aws.String(bucket), Key: aws.String(grant.Asset.ObjectKey),
		Body: bytes.NewReader(testCase.stored), ContentType: aws.String(testCase.storedMediaType),
		ContentLength: aws.Int64(int64(len(testCase.stored))),
	})
	if err != nil {
		t.Fatalf("直接写入无效测试对象: %v", err)
	}
	_, err = service.Confirm(ctx, asset.ConfirmCommand{
		CommandContext: asset.CommandContext{
			ActorAccountID: ownerID, IdempotencyKey: snowflake.NewTestID().String(), RequestID: snowflake.NewTestID().String(),
		},
		AssetID: grant.Asset.ID, ExpectedVersion: 1,
	})
	if !errors.Is(err, asset.ErrAssetContentInvalid) {
		t.Fatalf("Confirm() error = %v, want ErrAssetContentInvalid", err)
	}
	stored, getErr := repository.GetOwned(ctx, ownerID, grant.Asset.ID)
	if getErr != nil || stored.Status != asset.StatusPending || stored.Version != 1 {
		t.Fatalf("校验失败后的 Asset = %+v, error = %v", stored, getErr)
	}
}

func startRustFS(t *testing.T, ctx context.Context) (string, string, string, string, func()) {
	t.Helper()
	accessKey := "avalon-integration-access"
	secretKey := "avalon-integration-secret-key"
	request := testcontainers.ContainerRequest{
		Image:        rustFSImage,
		ExposedPorts: []string{"9000/tcp"},
		Env: map[string]string{
			"RUSTFS_ACCESS_KEY": accessKey,
			"RUSTFS_SECRET_KEY": secretKey,
			"RUSTFS_REGION":     "us-east-1",
		},
		WaitingFor: wait.ForListeningPort("9000/tcp").WithStartupTimeout(time.Minute),
	}
	container, err := testcontainers.GenericContainer(ctx, testcontainers.GenericContainerRequest{
		ContainerRequest: request,
		Started:          true,
	})
	if err != nil {
		t.Fatalf("启动 RustFS: %v", err)
	}
	endpoint, err := container.PortEndpoint(ctx, "9000/tcp", "http")
	if err != nil {
		_ = container.Terminate(context.Background())
		t.Fatalf("取得 RustFS endpoint: %v", err)
	}
	return endpoint, "avalon-assets", accessKey, secretKey, func() {
		if err := container.Terminate(context.Background()); err != nil {
			t.Errorf("停止 RustFS: %v", err)
		}
	}
}

func newS3Client(t *testing.T, ctx context.Context, endpoint, accessKey, secretKey string) *s3.Client {
	t.Helper()
	configuration, err := awsconfig.LoadDefaultConfig(
		ctx,
		awsconfig.WithRegion("us-east-1"),
		awsconfig.WithCredentialsProvider(credentials.NewStaticCredentialsProvider(accessKey, secretKey, "")),
		awsconfig.WithBaseEndpoint(endpoint),
	)
	if err != nil {
		t.Fatalf("加载 RustFS 管理客户端配置: %v", err)
	}
	return s3.NewFromConfig(configuration, func(options *s3.Options) { options.UsePathStyle = true })
}

func createBucket(t *testing.T, ctx context.Context, client *s3.Client, bucket string) {
	t.Helper()
	deadline := time.Now().Add(30 * time.Second)
	for {
		_, err := client.CreateBucket(ctx, &s3.CreateBucketInput{Bucket: aws.String(bucket)})
		if err == nil {
			return
		}
		if time.Now().After(deadline) {
			t.Fatalf("创建 RustFS Bucket: %v", err)
		}
		timer := time.NewTimer(200 * time.Millisecond)
		select {
		case <-ctx.Done():
			timer.Stop()
			t.Fatalf("等待 RustFS 就绪: %v", ctx.Err())
		case <-timer.C:
		}
	}
}

func signedPut(t *testing.T, ctx context.Context, url string, headers map[string]string, body []byte) int {
	t.Helper()
	request, err := http.NewRequestWithContext(ctx, http.MethodPut, url, bytes.NewReader(body))
	if err != nil {
		t.Fatalf("创建预签名 PUT: %v", err)
	}
	request.ContentLength = int64(len(body))
	for name, value := range headers {
		if strings.EqualFold(name, "Content-Length") {
			length, parseErr := strconv.ParseInt(value, 10, 64)
			if parseErr != nil {
				t.Fatalf("解析签名 Content-Length: %v", parseErr)
			}
			request.ContentLength = length
			continue
		}
		request.Header.Set(name, value)
	}
	response, err := httpClient().Do(request)
	if err != nil {
		t.Fatalf("执行预签名 PUT: %v", err)
	}
	_, _ = io.Copy(io.Discard, response.Body)
	_ = response.Body.Close()
	return response.StatusCode
}

func containsHeader(headers map[string]string, name, value string) bool {
	for actualName, actualValue := range headers {
		if strings.EqualFold(actualName, name) && actualValue == value {
			return true
		}
	}
	return false
}

func containsHeaderName(headers map[string]string, name string) bool {
	for actualName := range headers {
		if strings.EqualFold(actualName, name) {
			return true
		}
	}
	return false
}

func containsSignedValue(rawURL string, headers map[string]string, name, value string) bool {
	if containsHeader(headers, name, value) {
		return true
	}
	parsed, err := url.Parse(rawURL)
	if err != nil {
		return false
	}
	for actualName, values := range parsed.Query() {
		if strings.EqualFold(actualName, name) && len(values) == 1 && values[0] == value {
			return true
		}
	}
	return false
}

func replaceQueryValue(t *testing.T, rawURL, name, value string) string {
	t.Helper()
	parsed, err := url.Parse(rawURL)
	if err != nil {
		t.Fatalf("解析预签名 URL: %v", err)
	}
	query := parsed.Query()
	found := false
	for actualName := range query {
		if strings.EqualFold(actualName, name) {
			query.Set(actualName, value)
			found = true
			break
		}
	}
	if !found {
		t.Fatalf("预签名 URL 缺少 %s", name)
	}
	parsed.RawQuery = query.Encode()
	return parsed.String()
}

func cloneHeaders(headers map[string]string) map[string]string {
	result := make(map[string]string, len(headers))
	for name, value := range headers {
		result[name] = value
	}
	return result
}

func setHeader(headers map[string]string, name, value string) {
	for actualName := range headers {
		if strings.EqualFold(actualName, name) {
			headers[actualName] = value
			return
		}
	}
	headers[name] = value
}

func httpClient() *http.Client {
	return &http.Client{Timeout: 15 * time.Second}
}

type memoryRepository struct {
	mu     sync.Mutex
	assets map[snowflake.ID]asset.Asset
}

func newMemoryRepository() *memoryRepository {
	return &memoryRepository{assets: make(map[snowflake.ID]asset.Asset)}
}

// ListOwned 实现集成测试所需的账号隔离、状态筛选、稳定排序和页码分页语义。
func (s *memoryRepository) ListOwned(_ context.Context, ownerID snowflake.ID, query asset.ListQuery) (asset.Page, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	items := make([]asset.Asset, 0, len(s.assets))
	for _, value := range s.assets {
		if value.OwnerAccountID == ownerID && (query.Status == "" || value.Status == query.Status) {
			items = append(items, cloneAsset(value))
		}
	}
	sort.Slice(items, func(left, right int) bool {
		if !items[left].CreatedAt.Equal(items[right].CreatedAt) {
			return items[left].CreatedAt.After(items[right].CreatedAt)
		}
		return items[left].ID.String() > items[right].ID.String()
	})
	total := int64(len(items))
	start := int((query.Page - 1) * query.PageSize)
	if start > len(items) {
		start = len(items)
	}
	end := min(start+int(query.PageSize), len(items))
	return asset.Page{Items: items[start:end], Page: query.Page, PageSize: query.PageSize, Total: total}, nil
}

func (s *memoryRepository) GetOwned(_ context.Context, ownerID, assetID snowflake.ID) (asset.Asset, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	value, exists := s.assets[assetID]
	if !exists || value.OwnerAccountID != ownerID {
		return asset.Asset{}, asset.ErrAssetNotFound
	}
	return cloneAsset(value), nil
}

func (s *memoryRepository) WithinAsset(ctx context.Context, work func(asset.Writer) error) error {
	s.mu.Lock()
	defer s.mu.Unlock()
	return work(memoryWriter{s: s, ctx: ctx})
}

type memoryWriter struct {
	s   *memoryRepository
	ctx context.Context
}

func (w memoryWriter) Reserve(_ context.Context, record asset.ReserveRecord) (asset.Asset, error) {
	value := asset.Asset{
		ID: record.ID, OwnerAccountID: record.ActorAccountID, ObjectKey: record.ObjectKey,
		Status: asset.StatusPending, MediaType: record.MediaType, ExpectedSize: record.ExpectedSize,
		ExpectedSHA256: append([]byte(nil), record.ExpectedSHA256...), Version: 1, CreatedAt: record.CreatedAt,
	}
	w.s.assets[value.ID] = value
	return cloneAsset(value), nil
}

func (w memoryWriter) MarkReady(_ context.Context, record asset.ReadyRecord) (asset.Asset, error) {
	value, exists := w.s.assets[record.AssetID]
	if !exists || value.OwnerAccountID != record.ActorAccountID {
		return asset.Asset{}, asset.ErrAssetNotFound
	}
	if value.Status != asset.StatusPending || value.Version != record.ExpectedVersion {
		return asset.Asset{}, asset.ErrAssetConflict
	}
	value.Status = asset.StatusReady
	value.Version++
	value.ActualSize = aws.Int64(record.ActualSize)
	value.ActualSHA256 = append([]byte(nil), record.ActualSHA256...)
	value.Width = &record.Width
	value.Height = &record.Height
	value.ReadyAt = &record.ReadyAt
	w.s.assets[value.ID] = value
	return cloneAsset(value), nil
}

func cloneAsset(value asset.Asset) asset.Asset {
	value.ExpectedSHA256 = append([]byte(nil), value.ExpectedSHA256...)
	value.ActualSHA256 = append([]byte(nil), value.ActualSHA256...)
	return value
}

func rustFSPNG(t *testing.T, width, height int) []byte {
	t.Helper()
	if width <= 0 || height <= 0 {
		t.Fatal("图片尺寸必须为正数")
	}
	value := image.NewNRGBA(image.Rect(0, 0, width, height))
	value.Set(0, 0, color.NRGBA{R: 32, G: 128, B: 255, A: 255})
	var output bytes.Buffer
	if err := png.Encode(&output, value); err != nil {
		t.Fatalf("编码 PNG: %v", err)
	}
	if output.Len() > 10*1024*1024 {
		t.Fatalf("测试图片过大: %s", fmt.Sprintf("%d bytes", output.Len()))
	}
	return output.Bytes()
}
