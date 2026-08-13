package idempotency

import (
	"context"
	"database/sql"
	"encoding/json/jsontext"
	"errors"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/adminidempotencyrecord"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// AdminEntRecords 使用 admin_idempotency_record 保存管理员命令的幂等事实。
//
// 调用方必须传入绑定当前事务的 Ent Client，确保幂等认领、业务写入和响应完成原子提交。
type AdminEntRecords struct {
	client *avalonent.Client
	newID  snowflake.Source
}

// NewAdminEntRecords 创建基于管理员账号域的 Ent 幂等记录存储。
func NewAdminEntRecords(client *avalonent.Client, newID snowflake.Source) *AdminEntRecords {
	return &AdminEntRecords{client: client, newID: newID}
}

// TryClaim 尝试创建管理员幂等记录；复合唯一键冲突表示已有请求。
func (records *AdminEntRecords) TryClaim(ctx context.Context, request Request) (bool, error) {
	if records == nil || records.client == nil || records.newID == nil {
		return false, errors.New("管理员 Ent 幂等记录存储不可用")
	}
	id, err := records.newID.Next(ctx)
	if err != nil {
		return false, err
	}
	_, err = records.client.AdminIdempotencyRecord.Create().
		SetID(id).
		SetActorAccountID(request.ActorAccountID).
		SetOperationID(request.OperationID).
		SetIdempotencyKey(request.Key).
		SetRequestDigest(request.RequestDigest).
		SetCreatedAt(request.CreatedAt.UTC()).
		OnConflictColumns(
			adminidempotencyrecord.FieldActorAccountID,
			adminidempotencyrecord.FieldOperationID,
			adminidempotencyrecord.FieldIdempotencyKey,
		).
		DoNothing().
		ID(ctx)
	if errors.Is(err, sql.ErrNoRows) {
		return false, nil
	}
	if err == nil {
		return true, nil
	}
	if avalonent.IsConstraintError(err) {
		return false, nil
	}
	return false, err
}

// FindForUpdate 读取当前事务中已认领的管理员幂等记录。
func (records *AdminEntRecords) FindForUpdate(ctx context.Context, request Request) (StoredRecord, error) {
	value, err := records.client.AdminIdempotencyRecord.Query().Where(
		adminidempotencyrecord.ActorAccountIDEQ(request.ActorAccountID),
		adminidempotencyrecord.OperationIDEQ(request.OperationID),
		adminidempotencyrecord.IdempotencyKeyEQ(request.Key),
	).Only(ctx)
	if err != nil {
		return StoredRecord{}, err
	}
	return StoredRecord{RequestDigest: append([]byte(nil), value.RequestDigest...), Response: append([]byte(nil), value.Response...)}, nil
}

// CompleteRecord 写入管理员幂等响应，并返回更新行数。
func (records *AdminEntRecords) CompleteRecord(ctx context.Context, request Request, response []byte) (int64, error) {
	count, err := records.client.AdminIdempotencyRecord.Update().Where(
		adminidempotencyrecord.ActorAccountIDEQ(request.ActorAccountID),
		adminidempotencyrecord.OperationIDEQ(request.OperationID),
		adminidempotencyrecord.IdempotencyKeyEQ(request.Key),
		adminidempotencyrecord.RequestDigestEQ(request.RequestDigest),
	).SetResponse(jsontext.Value(append([]byte(nil), response...))).Save(ctx)
	return int64(count), err
}

var _ RecordStore = (*AdminEntRecords)(nil)
