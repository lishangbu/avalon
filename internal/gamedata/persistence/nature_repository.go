package persistence

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgtype"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamenature"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/nature"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createNatureOperationID = "game-data.nature.create"
	updateNatureOperationID = "game-data.nature.update"
)

type natureTransactionRepository struct {
	parent   *Adapters
	client   *avalonent.Client
	executor database.Transaction
}

// Create 在维护事务内创建 Nature、推进全局修订并记录审计事实。
func (w *natureTransactionRepository) Create(ctx context.Context, record nature.CreateRecord) (nature.Nature, error) {
	digest, err := idempotency.Digest(record.Nature)
	if err != nil {
		return nature.Nature{}, fmt.Errorf("计算 Nature 创建幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: createNatureOperationID, Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.At}
	value := record.Nature
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &value)
	if err != nil {
		return nature.Nature{}, err
	}
	if replay {
		return value, nil
	}
	builder := w.client.GameNature.Create().SetID(value.ID).SetCode(value.Code).SetName(value.Name).SetEnabled(value.Enabled).SetVersion(value.Version).SetCreatedAt(record.At.UTC()).SetUpdatedAt(record.At.UTC())
	if value.IncreasedStatID != nil {
		builder.SetIncreasedStatID(*value.IncreasedStatID)
	}
	if value.DecreasedStatID != nil {
		builder.SetDecreasedStatID(*value.DecreasedStatID)
	}
	row, err := builder.Save(ctx)
	if err != nil {
		return nature.Nature{}, natureDatabaseError("创建", err)
	}
	value = natureFromValues(naturePGIdentifier(&row.ID), row.Code, row.Name, naturePGIdentifier(row.IncreasedStatID), naturePGIdentifier(row.DecreasedStatID), row.Enabled, row.Version)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.nature.created", "game_nature", value.ID, record.RequestID, record.At, nil, &value); err != nil {
		return nature.Nature{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, value); err != nil {
		return nature.Nature{}, err
	}
	return value, nil
}

// Update 在维护事务内完整替换 Nature 并推进全局修订。
func (w *natureTransactionRepository) Update(ctx context.Context, record nature.UpdateRecord) (nature.Nature, error) {
	digest, err := idempotency.Digest(record.Nature)
	if err != nil {
		return nature.Nature{}, fmt.Errorf("计算 Nature 更新幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: updateNatureOperationID, Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.At}
	value := record.Nature
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &value)
	if err != nil {
		return nature.Nature{}, err
	}
	if replay {
		return value, nil
	}
	currentRow, err := w.client.GameNature.Query().Where(gamenature.IDEQ(value.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return nature.Nature{}, nature.ErrNatureNotFound
	}
	if err != nil {
		return nature.Nature{}, fmt.Errorf("锁定 Nature: %w", err)
	}
	current := natureFromValues(naturePGIdentifier(&currentRow.ID), currentRow.Code, currentRow.Name, naturePGIdentifier(currentRow.IncreasedStatID), naturePGIdentifier(currentRow.DecreasedStatID), currentRow.Enabled, currentRow.Version)
	if current.Version != record.ExpectedVersion {
		return nature.Nature{}, nature.ErrNatureConflict
	}
	update := w.client.GameNature.UpdateOne(currentRow).Where(gamenature.VersionEQ(record.ExpectedVersion)).SetCode(value.Code).SetName(value.Name).SetEnabled(value.Enabled).SetVersion(value.Version).SetUpdatedAt(record.At.UTC())
	if value.IncreasedStatID == nil {
		update.ClearIncreasedStatID()
	} else {
		update.SetIncreasedStatID(*value.IncreasedStatID)
	}
	if value.DecreasedStatID == nil {
		update.ClearDecreasedStatID()
	} else {
		update.SetDecreasedStatID(*value.DecreasedStatID)
	}
	row, err := update.Save(ctx)
	if avalonent.IsNotFound(err) {
		return nature.Nature{}, nature.ErrNatureConflict
	}
	if err != nil {
		return nature.Nature{}, natureDatabaseError("更新", err)
	}
	value = natureFromValues(naturePGIdentifier(&row.ID), row.Code, row.Name, naturePGIdentifier(row.IncreasedStatID), naturePGIdentifier(row.DecreasedStatID), row.Enabled, row.Version)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.nature.updated", "game_nature", value.ID, record.RequestID, record.At, &current, &value); err != nil {
		return nature.Nature{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, value); err != nil {
		return nature.Nature{}, err
	}
	return value, nil
}

// NatureAdapters 避免与其它实时资料同名的 Get、List 方法发生冲突。
type NatureAdapters struct {
	// Adapters 是共享连接池、事务和审计实现。
	*Adapters
}

// NewNatureAdapters 创建 Nature 应用服务使用的窄关系型持久化适配器。
func NewNatureAdapters(adapters *Adapters) *NatureAdapters {
	return &NatureAdapters{Adapters: adapters}
}

// Get 查询指定 Nature。
func (s *NatureAdapters) Get(ctx context.Context, id snowflake.ID) (nature.Nature, error) {
	row, err := s.pool.Client(ctx).GameNature.Query().Where(gamenature.IDEQ(id)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return nature.Nature{}, nature.ErrNatureNotFound
	}
	if err != nil {
		return nature.Nature{}, fmt.Errorf("查询 Nature: %w", err)
	}
	return natureFromValues(naturePGIdentifier(&row.ID), row.Code, row.Name, naturePGIdentifier(row.IncreasedStatID), naturePGIdentifier(row.DecreasedStatID), row.Enabled, row.Version), nil
}

// List 返回 Nature 资料页。
func (s *NatureAdapters) List(ctx context.Context, query nature.ListQuery) (nature.Page, error) {
	client := s.pool.Client(ctx)
	filters := make([]predicate.GameNature, 0, 4)
	if query.Q != "" {
		filters = append(filters, gamenature.Or(gamenature.CodeContainsFold(query.Q), gamenature.NameContainsFold(query.Q)))
	}
	if query.Code != "" {
		filters = append(filters, gamenature.CodeContainsFold(query.Code))
	}
	if query.Name != "" {
		filters = append(filters, gamenature.NameContainsFold(query.Name))
	}
	if query.Enabled != nil {
		filters = append(filters, gamenature.EnabledEQ(*query.Enabled))
	}
	total, err := client.GameNature.Query().Where(filters...).Count(ctx)
	if err != nil {
		return nature.Page{}, fmt.Errorf("统计 Nature: %w", err)
	}
	rows, err := client.GameNature.Query().Where(filters...).Order(gamenature.ByCode(sql.OrderAsc()), gamenature.ByID(sql.OrderAsc())).Offset(int(query.Page-1) * int(query.PageSize)).Limit(int(query.PageSize)).All(ctx)
	if err != nil {
		return nature.Page{}, fmt.Errorf("查询 Nature 页: %w", err)
	}
	items := make([]nature.Nature, len(rows))
	for index, row := range rows {
		items[index] = natureFromValues(naturePGIdentifier(&row.ID), row.Code, row.Name, naturePGIdentifier(row.IncreasedStatID), naturePGIdentifier(row.DecreasedStatID), row.Enabled, row.Version)
	}
	return nature.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

func natureFromValues(id pgtype.Int8, code, name string, increased, decreased pgtype.Int8, enabled bool, version int64) nature.Nature {
	return nature.Nature{ID: domainIdentifier(id), Code: code, Name: name, IncreasedStatID: nullableDomainIdentifier(increased), DecreasedStatID: nullableDomainIdentifier(decreased), Enabled: enabled, Version: version}
}

// naturePGIdentifier 将 Ent 可空 Identifier 转成资料层沿用的 pgtype 表示。
func naturePGIdentifier(value *snowflake.ID) pgtype.Int8 {
	if value == nil {
		return pgtype.Int8{}
	}
	return pgtype.Int8{Int64: int64(*value), Valid: true}
}
func natureDatabaseError(action string, err error) error {
	var databaseError *pgconn.PgError
	if errors.As(err, &databaseError) && (databaseError.Code == "23505" || databaseError.Code == "23503") {
		return nature.ErrNatureConflict
	}
	return fmt.Errorf("%s Nature: %w", action, err)
}
