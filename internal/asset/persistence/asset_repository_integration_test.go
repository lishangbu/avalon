//go:build integration

package persistence_test

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"image"
	"image/color"
	"image/png"
	"io"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/asset"
	assetpersistence "github.com/lishangbu/avalon/internal/asset/persistence"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

const postgresImage = "postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7"

func TestAssetRepositoryPersistsLifecycleAtomically(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()

	container, err := postgres.Run(
		ctx,
		postgresImage,
		postgres.WithDatabase("avalon_asset_test"),
		postgres.WithUsername("avalon"),
		postgres.WithPassword("avalon"),
		postgres.BasicWaitStrategies(),
	)
	if err != nil {
		t.Fatalf("启动 PostgreSQL: %v", err)
	}
	t.Cleanup(func() {
		if err := container.Terminate(context.Background()); err != nil {
			t.Errorf("停止 PostgreSQL: %v", err)
		}
	})
	databaseURL, err := container.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		t.Fatalf("取得 PostgreSQL 连接串: %v", err)
	}
	pool, err := database.Open(database.Config{URL: databaseURL, MaxOpenConnections: 20, MaxIdleConnections: 10})
	if err != nil {
		t.Fatalf("连接 PostgreSQL: %v", err)
	}
	t.Cleanup(pool.Close)
	if err := pool.Persistence().ApplySchema(ctx, persistence.SchemaModeCreate); err != nil {
		t.Fatalf("创建 Ent Schema: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO audit_hash_chain_state (id, ledger, latest_hash, updated_at) VALUES (1048577, 'admin_audit_log', ''::bytea, now())`); err != nil {
		t.Fatalf("初始化 Asset 审计哈希链: %v", err)
	}

	now := time.Date(2026, time.July, 28, 11, 0, 0, 0, time.UTC)
	ownerID := snowflake.MustParse("1048576057")
	otherID := snowflake.MustParse("1048576058")
	insertAccount(t, ctx, pool, ownerID, "asset-owner", now)
	insertAccount(t, ctx, pool, otherID, "other-owner", now)

	raw := integrationPNG(t, 4, 3)
	digest := sha256.Sum256(raw)
	blobs := &memoryBlobStore{raw: raw, mediaType: "image/png"}
	repository := assetpersistence.NewRepository(pool, snowflake.NewTestID)
	service := asset.NewService(repository, blobs, snowflake.NewTestID, func() time.Time { return now })
	begin := asset.BeginUploadCommand{
		CommandContext: asset.CommandContext{
			ActorAccountID: ownerID, IdempotencyKey: "asset-begin-persisted", RequestID: "asset-begin-request",
		},
		MediaType: "image/png", ExpectedSize: int64(len(raw)), ExpectedSHA256: hex.EncodeToString(digest[:]),
	}

	pending, err := service.BeginUpload(ctx, begin)
	if err != nil || pending.Asset.Status != asset.StatusPending || pending.Asset.Version != 1 {
		t.Fatalf("BeginUpload() = %+v, error = %v", pending, err)
	}
	// 新服务实例会先生成不同 Identifier，但持久幂等记录必须返回第一次提交的 Asset。
	replayService := asset.NewService(repository, blobs, snowflake.NewTestID, func() time.Time { return now.Add(time.Minute) })
	replayed, err := replayService.BeginUpload(ctx, begin)
	if err != nil || replayed.Asset.ID != pending.Asset.ID || replayed.Asset.ObjectKey != pending.Asset.ObjectKey {
		t.Fatalf("重放 BeginUpload() = %+v, error = %v", replayed, err)
	}
	conflictingBegin := begin
	conflictingBegin.ExpectedSize++
	if _, err := replayService.BeginUpload(ctx, conflictingBegin); !errors.Is(err, idempotency.ErrConflict) {
		t.Fatalf("冲突 BeginUpload() error = %v, want ErrConflict", err)
	}
	if _, err := repository.GetOwned(ctx, otherID, pending.Asset.ID); !errors.Is(err, asset.ErrAssetNotFound) {
		t.Fatalf("跨账号 GetOwned() error = %v, want ErrAssetNotFound", err)
	}

	confirm := asset.ConfirmCommand{
		CommandContext: asset.CommandContext{
			ActorAccountID: ownerID, IdempotencyKey: "asset-confirm-persisted", RequestID: "asset-confirm-request",
		},
		AssetID: pending.Asset.ID, ExpectedVersion: 1,
	}
	ready, err := service.Confirm(ctx, confirm)
	if err != nil || ready.Status != asset.StatusReady || ready.Version != 2 ||
		ready.Width == nil || *ready.Width != 4 || ready.Height == nil || *ready.Height != 3 {
		t.Fatalf("Confirm() = %+v, error = %v", ready, err)
	}
	readsAfterConfirmation := blobs.getCallCount()
	replayedReady, err := replayService.Confirm(ctx, confirm)
	if err != nil || replayedReady.ID != ready.ID || replayedReady.Version != ready.Version {
		t.Fatalf("重放 Confirm() = %+v, error = %v", replayedReady, err)
	}
	if blobs.getCallCount() != readsAfterConfirmation {
		t.Fatalf("确认重放再次读取 RustFS: before=%d after=%d", readsAfterConfirmation, blobs.getCallCount())
	}
	conflictingConfirm := confirm
	conflictingConfirm.IdempotencyKey = "asset-confirm-new-key"
	if _, err := replayService.Confirm(ctx, conflictingConfirm); !errors.Is(err, asset.ErrAssetConflict) {
		t.Fatalf("重复确认 error = %v, want ErrAssetConflict", err)
	}
	download, err := replayService.Download(ctx, ownerID, ready.ID)
	if err != nil || download.URL == "" {
		t.Fatalf("Download() = %+v, error = %v", download, err)
	}
	if _, err := replayService.Download(ctx, otherID, ready.ID); !errors.Is(err, asset.ErrAssetNotFound) {
		t.Fatalf("跨账号 Download() error = %v, want ErrAssetNotFound", err)
	}
	page, err := replayService.List(ctx, ownerID, asset.ListQuery{Page: 1, PageSize: 10, Status: asset.StatusReady})
	if err != nil || page.Total != 1 || len(page.Items) != 1 || page.Items[0].ID != ready.ID {
		t.Fatalf("List(ready) = %+v, error = %v", page, err)
	}
	otherPage, err := replayService.List(ctx, otherID, asset.ListQuery{Page: 1, PageSize: 10})
	if err != nil || otherPage.Total != 0 || len(otherPage.Items) != 0 {
		t.Fatalf("List(other owner) = %+v, error = %v", otherPage, err)
	}

	assertLifecycleRows(t, ctx, pool, ready.ID)
	assertAuditDoesNotPersistSignedURL(t, ctx, pool, ready.ID)
	assertFailedAuditRollsBack(t, ctx, pool, blobs, ownerID, digest, raw, now)
}

func insertAccount(t *testing.T, ctx context.Context, pool *database.Pool, id snowflake.ID, username string, now time.Time) {
	t.Helper()
	if _, err := pool.Exec(ctx, `
		INSERT INTO admin_account (
			id, username, username_key, display_name, password_hash, password_algorithm,
			password_parameters, status, created_at, updated_at
		) VALUES ($1, $2, $2, $2, 'unused', 'argon2id', '{}', 'active', $3, $3)
	`, id, username, now); err != nil {
		t.Fatalf("写入测试账号: %v", err)
	}
}

func assertLifecycleRows(t *testing.T, ctx context.Context, pool *database.Pool, assetID snowflake.ID) {
	t.Helper()
	var status string
	var version int64
	if err := pool.QueryRow(ctx, `SELECT status, version FROM asset WHERE id = $1`, assetID).Scan(&status, &version); err != nil {
		t.Fatalf("查询 Asset: %v", err)
	}
	if status != "ready" || version != 2 {
		t.Fatalf("Asset state = %s/%d, want ready/2", status, version)
	}
	var assetCount, auditCount, idempotencyCount int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM asset`).Scan(&assetCount); err != nil {
		t.Fatalf("统计 Asset: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM admin_audit_log WHERE object_type = 'asset'`).Scan(&auditCount); err != nil {
		t.Fatalf("统计 Asset 审计: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM admin_idempotency_record WHERE operation_id LIKE 'asset.%'`).Scan(&idempotencyCount); err != nil {
		t.Fatalf("统计 Asset 幂等记录: %v", err)
	}
	if assetCount != 1 || auditCount != 2 || idempotencyCount != 2 {
		t.Fatalf("row counts = asset %d, audit %d, idempotency %d", assetCount, auditCount, idempotencyCount)
	}
}

func assertAuditDoesNotPersistSignedURL(t *testing.T, ctx context.Context, pool *database.Pool, assetID snowflake.ID) {
	t.Helper()
	rows, err := pool.Query(ctx, `
		SELECT changes::text
		FROM admin_audit_log
		WHERE object_type = 'asset' AND object_id = $1
	`, assetID.String())
	if err != nil {
		t.Fatalf("查询 Asset 审计: %v", err)
	}
	defer rows.Close()
	for rows.Next() {
		var changes string
		if err := rows.Scan(&changes); err != nil {
			t.Fatalf("读取 Asset 审计: %v", err)
		}
		lower := strings.ToLower(changes)
		if strings.Contains(lower, "signature=") || strings.Contains(lower, "x-amz-signature") ||
			strings.Contains(lower, "rustfs.invalid") {
			t.Fatalf("Asset 审计包含签名 URL: %s", changes)
		}
	}
	if err := rows.Err(); err != nil {
		t.Fatalf("遍历 Asset 审计: %v", err)
	}
}

func assertFailedAuditRollsBack(
	t *testing.T,
	ctx context.Context,
	pool *database.Pool,
	blobs *memoryBlobStore,
	ownerID snowflake.ID,
	digest [sha256.Size]byte,
	raw []byte,
	now time.Time,
) {
	t.Helper()
	// 当前 Snowflake Identifier 生成器不再返回错误。复用一条已经提交的审计 Identifier
	// 触发主键冲突，继续验证审计失败会回滚 Asset 与幂等记录，而不保留旧生成器兼容层。
	var existingAuditIDText string
	if err := pool.QueryRow(ctx, `SELECT id::text FROM admin_audit_log ORDER BY sequence LIMIT 1`).Scan(&existingAuditIDText); err != nil {
		t.Fatalf("读取既有审计 ID: %v", err)
	}
	existingAuditID := snowflake.MustParse(existingAuditIDText)
	failingRepository := assetpersistence.NewRepository(pool, snowflake.TestSource(func() snowflake.ID { return existingAuditID }))
	failingService := asset.NewService(failingRepository, blobs, snowflake.NewTestID, func() time.Time { return now.Add(2 * time.Minute) })
	_, err := failingService.BeginUpload(ctx, asset.BeginUploadCommand{
		CommandContext: asset.CommandContext{
			ActorAccountID: ownerID, IdempotencyKey: "asset-audit-rollback", RequestID: "asset-audit-rollback-request",
		},
		MediaType: "image/png", ExpectedSize: int64(len(raw)), ExpectedSHA256: hex.EncodeToString(digest[:]),
	})
	if err == nil {
		t.Fatal("审计 Identifier 冲突时 BeginUpload() 应失败")
	}
	var assetCount, idempotencyCount int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM asset`).Scan(&assetCount); err != nil {
		t.Fatalf("回滚后统计 Asset: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM admin_idempotency_record WHERE operation_id LIKE 'asset.%'`).Scan(&idempotencyCount); err != nil {
		t.Fatalf("回滚后统计幂等记录: %v", err)
	}
	if assetCount != 1 || idempotencyCount != 2 {
		t.Fatalf("审计失败未完整回滚: asset=%d idempotency=%d", assetCount, idempotencyCount)
	}
}

type memoryBlobStore struct {
	mu        sync.Mutex
	raw       []byte
	mediaType string
	getCalls  int
}

func (s *memoryBlobStore) PresignUpload(
	context.Context,
	string,
	string,
	int64,
	[]byte,
	time.Duration,
) (string, map[string]string, error) {
	return "https://rustfs.invalid/upload?signature=secret", map[string]string{"Content-Type": s.mediaType}, nil
}

func (s *memoryBlobStore) Get(context.Context, string) (asset.BlobObject, error) {
	s.mu.Lock()
	s.getCalls++
	s.mu.Unlock()
	return asset.BlobObject{
		Body: io.NopCloser(bytes.NewReader(s.raw)), Size: int64(len(s.raw)), MediaType: s.mediaType,
	}, nil
}

func (s *memoryBlobStore) PublicURL(objectKey string) (string, error) {
	return "https://rustfs.invalid/" + objectKey, nil
}

func (s *memoryBlobStore) getCallCount() int {
	s.mu.Lock()
	defer s.mu.Unlock()
	return s.getCalls
}

func integrationPNG(t *testing.T, width, height int) []byte {
	t.Helper()
	value := image.NewNRGBA(image.Rect(0, 0, width, height))
	value.Set(0, 0, color.NRGBA{R: 255, G: 64, B: 32, A: 255})
	var output bytes.Buffer
	if err := png.Encode(&output, value); err != nil {
		t.Fatalf("编码 PNG: %v", err)
	}
	return output.Bytes()
}
