package idempotency

import (
	"context"
	"database/sql"
	"encoding/json/jsontext"
	"errors"

	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/administrationidempotencyrecord"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// EntRecords 使用共享的 administration_idempotency_record 表保存跨领域命令幂等事实。
//
// 该实现只依赖 Ent Client；调用方应把事务绑定的 Client 传入，确保认领、业务写入和响应完成
// 在同一个数据库事务中提交或回滚。
type EntRecords struct {
	client *avalonent.Client
	newID  snowflake.Source
}

// NewEntRecords 创建基于 Ent 的幂等记录存储。
func NewEntRecords(client *avalonent.Client, newID snowflake.Source) *EntRecords {
	return &EntRecords{client: client, newID: newID}
}

// TryClaim 尝试创建幂等记录；复合唯一键冲突表示已有请求，不覆盖原始摘要。
func (records *EntRecords) TryClaim(ctx context.Context, request Request) (bool, error) {
	if records == nil || records.client == nil || records.newID == nil {
		return false, errors.New("Ent 幂等记录存储不可用")
	}
	id, err := records.newID.Next(ctx)
	if err != nil {
		return false, err
	}
	_, err = records.client.AdministrationIdempotencyRecord.Create().
		SetID(id).
		SetActorAccountID(request.ActorAccountID).
		SetOperationID(request.OperationID).
		SetIdempotencyKey(request.Key).
		SetRequestDigest(request.RequestDigest).
		SetCreatedAt(request.CreatedAt.UTC()).
		OnConflictColumns(
			administrationidempotencyrecord.FieldActorAccountID,
			administrationidempotencyrecord.FieldOperationID,
			administrationidempotencyrecord.FieldIdempotencyKey,
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

// FindForUpdate 读取已认领记录；事务 Client 会保留调用方的锁语义。
func (records *EntRecords) FindForUpdate(ctx context.Context, request Request) (StoredRecord, error) {
	value, err := records.client.AdministrationIdempotencyRecord.Query().Where(
		administrationidempotencyrecord.ActorAccountIDEQ(request.ActorAccountID),
		administrationidempotencyrecord.OperationIDEQ(request.OperationID),
		administrationidempotencyrecord.IdempotencyKeyEQ(request.Key),
	).Only(ctx)
	if err != nil {
		return StoredRecord{}, err
	}
	return StoredRecord{RequestDigest: append([]byte(nil), value.RequestDigest...), Response: append([]byte(nil), value.Response...)}, nil
}

// CompleteRecord 写入幂等响应，并返回更新行数。
func (records *EntRecords) CompleteRecord(ctx context.Context, request Request, response []byte) (int64, error) {
	count, err := records.client.AdministrationIdempotencyRecord.Update().Where(
		administrationidempotencyrecord.ActorAccountIDEQ(request.ActorAccountID),
		administrationidempotencyrecord.OperationIDEQ(request.OperationID),
		administrationidempotencyrecord.IdempotencyKeyEQ(request.Key),
		administrationidempotencyrecord.RequestDigestEQ(request.RequestDigest),
	).SetResponse(jsontext.Value(append([]byte(nil), response...))).Save(ctx)
	return int64(count), err
}

var _ RecordStore = (*EntRecords)(nil)
