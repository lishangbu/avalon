// Package store 提供 Asset 生命周期的 Ent 持久化实现。
package store

import (
	"context"
	"database/sql"
	"encoding/json"
	"encoding/json/jsontext"
	entsql "entgo.io/ent/dialect/sql"
	"errors"
	"fmt"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/adminidempotencyrecord"
	"github.com/lishangbu/avalon/ent/asset"
	domain "github.com/lishangbu/avalon/internal/asset"
	"github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"time"
)

// Store 使用 Ent 访问 Asset 与管理幂等记录。
type Store struct {
	pool  *database.Pool
	newID snowflake.Source
}
type transactionStore struct {
	parent *Store
	client *avalonent.Client
	ctx    context.Context
}

// New 创建 Asset 存储。
func New(pool *database.Pool, newID snowflake.Source) *Store {
	return &Store{pool: pool, newID: newID}
}

// ListOwned 按账号、状态和页码查询 Asset。
func (s *Store) ListOwned(ctx context.Context, ownerID snowflake.ID, q domain.ListQuery) (domain.Page, error) {
	query := s.pool.Client(ctx).Asset.Query().Where(asset.OwnerAccountIDEQ(ownerID))
	if q.Status != "" {
		query = query.Where(asset.StatusEQ(string(q.Status)))
	}
	rows, err := query.Order( /* 创建时间、Identifier 倒序 */ asset.ByCreatedAt(entsql.OrderDesc()), asset.ByID(entsql.OrderDesc())).Offset(int((q.Page - 1) * q.PageSize)).Limit(int(q.PageSize)).All(ctx)
	if err != nil {
		return domain.Page{}, fmt.Errorf("分页查询 Asset: %w", err)
	}
	total, err := query.Clone().Count(ctx)
	if err != nil {
		return domain.Page{}, fmt.Errorf("统计 Asset: %w", err)
	}
	items := make([]domain.Asset, len(rows))
	for i, row := range rows {
		items[i] = fromEnt(row)
	}
	return domain.Page{Items: items, Page: q.Page, PageSize: q.PageSize, Total: int64(total)}, nil
}

// GetOwned 按账号读取 Asset。
func (s *Store) GetOwned(ctx context.Context, ownerID, id snowflake.ID) (domain.Asset, error) {
	row, err := s.pool.Client(ctx).Asset.Query().Where(asset.IDEQ(id), asset.OwnerAccountIDEQ(ownerID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return domain.Asset{}, domain.ErrAssetNotFound
	}
	if err != nil {
		return domain.Asset{}, fmt.Errorf("查询 Asset: %w", err)
	}
	return fromEnt(row), nil
}

// WithinAsset 在同一 Ent 事务内执行 Asset 状态写入。
func (s *Store) WithinAsset(ctx context.Context, work func(domain.Writer) error) error {
	return s.pool.WithinTransaction(ctx, func(txctx context.Context) error {
		return work(&transactionStore{parent: s, client: s.pool.Client(txctx), ctx: txctx})
	})
}

// Reserve 创建 Pending 记录并保存幂等响应。
func (w *transactionStore) Reserve(ctx context.Context, r domain.ReserveRecord) (domain.Asset, error) {
	ctx = w.ctx
	digest, err := idempotency.Digest(struct {
		MediaType      string
		ExpectedSize   int64
		ExpectedSHA256 []byte
	}{r.MediaType, r.ExpectedSize, r.ExpectedSHA256})
	if err != nil {
		return domain.Asset{}, fmt.Errorf("计算 Asset 上传幂等摘要: %w", err)
	}
	req := idempotency.Request{ActorAccountID: r.ActorAccountID, OperationID: "asset.upload.begin", Key: r.IdempotencyKey, RequestDigest: digest, CreatedAt: r.CreatedAt}
	result := domain.Asset{}
	records := idempotency.NewPersistentWriter(&entRecords{client: w.client, newID: w.parent.newID})
	replay, err := idempotency.ClaimResponse(ctx, records, req, &result)
	if err != nil {
		return domain.Asset{}, fmt.Errorf("认领 Asset 上传幂等键: %w", err)
	}
	if replay {
		return result, nil
	}
	row, err := w.client.Asset.Create().SetID(r.ID).SetOwnerAccountID(r.ActorAccountID).SetObjectKey(r.ObjectKey).SetStatus(string(domain.StatusPending)).SetMediaType(r.MediaType).SetExpectedSize(r.ExpectedSize).SetExpectedSha256(r.ExpectedSHA256).SetVersion(1).SetCreatedAt(r.CreatedAt).Save(ctx)
	if err != nil {
		return domain.Asset{}, fmt.Errorf("创建 Pending Asset: %w", err)
	}
	result = fromEnt(row)
	if err = w.audit(ctx, r.ActorAccountID, "asset.upload.pending", r.ID, r.RequestID, r.CreatedAt, nil, result); err != nil {
		return domain.Asset{}, err
	}
	if err = idempotency.Complete(ctx, records, req, result); err != nil {
		return domain.Asset{}, fmt.Errorf("保存 Asset 上传幂等结果: %w", err)
	}
	return result, nil
}

// MarkReady 以状态和版本条件把 Pending 记录转换为 Ready。
func (w *transactionStore) MarkReady(ctx context.Context, r domain.ReadyRecord) (domain.Asset, error) {
	ctx = w.ctx
	digest, err := idempotency.Digest(struct {
		AssetID         snowflake.ID
		ExpectedVersion int64
		ActualSize      int64
		ActualSHA256    []byte
		Width           int32
		Height          int32
	}{r.AssetID, r.ExpectedVersion, r.ActualSize, r.ActualSHA256, r.Width, r.Height})
	if err != nil {
		return domain.Asset{}, err
	}
	req := idempotency.Request{ActorAccountID: r.ActorAccountID, OperationID: "asset.upload.confirm", Key: r.IdempotencyKey, RequestDigest: digest, CreatedAt: r.ReadyAt}
	result := domain.Asset{}
	records := idempotency.NewPersistentWriter(&entRecords{client: w.client, newID: w.parent.newID})
	replay, err := idempotency.ClaimResponse(ctx, records, req, &result)
	if err != nil {
		return domain.Asset{}, err
	}
	if replay {
		return result, nil
	}
	before, err := w.client.Asset.Query().Where(asset.IDEQ(r.AssetID), asset.OwnerAccountIDEQ(r.ActorAccountID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return domain.Asset{}, domain.ErrAssetNotFound
	}
	if err != nil {
		return domain.Asset{}, fmt.Errorf("锁定 Pending Asset: %w", err)
	}
	b := fromEnt(before)
	if b.Status != domain.StatusPending || b.Version != r.ExpectedVersion {
		return domain.Asset{}, domain.ErrAssetConflict
	}
	update := w.client.Asset.UpdateOne(before).SetActualSize(r.ActualSize).SetActualSha256(r.ActualSHA256).SetWidth(r.Width).SetHeight(r.Height).SetVersion(r.ExpectedVersion + 1).SetReadyAt(r.ReadyAt).SetStatus(string(domain.StatusReady))
	row, err := update.Save(ctx)
	if err != nil {
		return domain.Asset{}, domain.ErrAssetConflict
	}
	result = fromEnt(row)
	if err = w.audit(ctx, r.ActorAccountID, "asset.upload.ready", r.AssetID, r.RequestID, r.ReadyAt, b, result); err != nil {
		return domain.Asset{}, err
	}
	if err = idempotency.Complete(ctx, records, req, result); err != nil {
		return domain.Asset{}, err
	}
	return result, nil
}

func (w *transactionStore) audit(ctx context.Context, actor snowflake.ID, action string, id snowflake.ID, request string, at time.Time, before, after any) error {
	changes, err := json.Marshal(struct {
		Before any `json:"before,omitempty"`
		After  any `json:"after,omitempty"`
	}{before, after})
	if err != nil {
		return err
	}
	aid, err := w.parent.newID.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成 Asset 审计标识: %w", err)
	}
	objectText := id.String()
	return audit.Append(ctx, database.Executor(ctx, nil), audit.AdminLedger, audit.Entry{ID: aid, ActorAccountID: &actor, ActorKind: "admin", ActionCode: action, ObjectType: "asset", ObjectID: &objectText, RequestID: request, Reason: func() *string { v := "administrative_change"; return &v }(), Changes: changes, CreatedAt: at})
}

type entRecords struct {
	client *avalonent.Client
	newID  snowflake.Source
}

func (r *entRecords) TryClaim(ctx context.Context, q idempotency.Request) (bool, error) {
	id, err := r.newID.Next(ctx)
	if err != nil {
		return false, err
	}
	_, err = r.client.AdminIdempotencyRecord.Create().SetID(id).SetActorAccountID(q.ActorAccountID).SetOperationID(q.OperationID).SetIdempotencyKey(q.Key).SetRequestDigest(q.RequestDigest).SetCreatedAt(q.CreatedAt).
		OnConflictColumns(adminidempotencyrecord.FieldActorAccountID, adminidempotencyrecord.FieldOperationID, adminidempotencyrecord.FieldIdempotencyKey).
		DoNothing().ID(ctx)
	if err == nil {
		return true, nil
	}
	if errors.Is(err, sql.ErrNoRows) {
		return false, nil
	}
	return false, err
}
func (r *entRecords) FindForUpdate(ctx context.Context, q idempotency.Request) (idempotency.StoredRecord, error) {
	v, e := r.client.AdminIdempotencyRecord.Query().Where(adminidempotencyrecord.ActorAccountIDEQ(q.ActorAccountID), adminidempotencyrecord.OperationIDEQ(q.OperationID), adminidempotencyrecord.IdempotencyKeyEQ(q.Key)).Only(ctx)
	if e != nil {
		return idempotency.StoredRecord{}, e
	}
	return idempotency.StoredRecord{RequestDigest: v.RequestDigest, Response: v.Response}, nil
}
func (r *entRecords) CompleteRecord(ctx context.Context, q idempotency.Request, response []byte) (int64, error) {
	n, e := r.client.AdminIdempotencyRecord.Update().Where(adminidempotencyrecord.ActorAccountIDEQ(q.ActorAccountID), adminidempotencyrecord.OperationIDEQ(q.OperationID), adminidempotencyrecord.IdempotencyKeyEQ(q.Key), adminidempotencyrecord.RequestDigestEQ(q.RequestDigest)).SetResponse(jsontext.Value(response)).Save(ctx)
	return int64(n), e
}
func fromEnt(v *avalonent.Asset) domain.Asset {
	actual := []byte(nil)
	if v.ActualSha256 != nil {
		actual = append([]byte(nil), (*v.ActualSha256)...)
	}
	r := domain.Asset{ID: v.ID, OwnerAccountID: v.OwnerAccountID, ObjectKey: v.ObjectKey, Status: domain.Status(v.Status), MediaType: v.MediaType, ExpectedSize: v.ExpectedSize, ExpectedSHA256: append([]byte(nil), v.ExpectedSha256...), ActualSHA256: actual, Version: v.Version, CreatedAt: v.CreatedAt.UTC()}
	r.ActualSize = v.ActualSize
	r.Width = v.Width
	r.Height = v.Height
	if v.ReadyAt != nil {
		x := v.ReadyAt.UTC()
		r.ReadyAt = &x
	}
	return r
}

var _ domain.Store = (*Store)(nil)
