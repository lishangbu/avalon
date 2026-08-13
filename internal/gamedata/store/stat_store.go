package store

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	entsql "entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamestat"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/stat"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createStatOperationID = "game-data.stat.create"
	updateStatOperationID = "game-data.stat.update"
	deleteStatOperationID = "game-data.stat.delete"
)

// statTransactionStore 在调用方开启的 Ent 事务中实现数值项资料写入边界。
type statTransactionStore struct {
	parent   *Store
	client   *avalonent.Client
	executor database.Transaction
}

// Create 原子创建数值项、管理幂等记录和审计事实。
func (w *statTransactionStore) Create(ctx context.Context, record stat.CreateRecord) (stat.Stat, error) {
	digest, err := idempotency.Digest(struct {
		Code       string
		Name       string
		SortOrder  int32
		BattleOnly bool
		Enabled    bool
	}{record.Stat.Code, record.Stat.Name, record.Stat.SortOrder,
		record.Stat.BattleOnly, record.Stat.Enabled})
	if err != nil {
		return stat.Stat{}, fmt.Errorf("计算数值项资料创建幂等摘要: %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: createStatOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt,
	}
	created := record.Stat
	client := w.client
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return stat.Stat{}, fmt.Errorf("认领数值项资料创建幂等键: %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := client.GameStat.Create().
		SetID(newEntID(created.ID)).
		SetCode(created.Code).
		SetName(created.Name).
		SetSortOrder(created.SortOrder).
		SetBattleOnly(created.BattleOnly).
		SetEnabled(created.Enabled).
		SetVersion(created.Version).
		SetCreatedAt(record.CreatedAt).
		SetUpdatedAt(record.CreatedAt).
		Save(ctx)
	if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23505" {
			return stat.Stat{}, stat.ErrStatCodeConflict
		}
		return stat.Stat{}, fmt.Errorf("插入数值项资料: %w", err)
	}
	created = statFromValues(databaseIdentifier(row.ID), row.Code, row.Name, row.SortOrder, row.BattleOnly, row.Enabled, row.Version)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.stat.created",
		"game_stat", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return stat.Stat{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return stat.Stat{}, fmt.Errorf("保存数值项资料创建幂等结果: %w", err)
	}
	return created, nil
}

// GetStat 通过稳定 Identifier 读取当前数值项资料。
func (s *Store) GetStat(ctx context.Context, statID snowflake.ID) (stat.Stat, error) {
	row, err := s.pool.Client(ctx).GameStat.Query().
		Where(gamestat.IDEQ(statID)).
		Only(ctx)
	if avalonent.IsNotFound(err) {
		return stat.Stat{}, stat.ErrStatNotFound
	}
	if err != nil {
		return stat.Stat{}, fmt.Errorf("查询数值项资料: %w", err)
	}
	return statFromValues(databaseIdentifier(row.ID), row.Code, row.Name, row.SortOrder, row.BattleOnly, row.Enabled, row.Version), nil
}

// ListStats 使用 Ent 谓词、稳定排序和有界页码返回数值项资料。
func (s *Store) ListStats(ctx context.Context, query stat.ListQuery) (stat.Page, error) {
	filters := statPredicates(query)
	client := s.pool.Client(ctx)
	total, err := client.GameStat.Query().Where(filters...).Count(ctx)
	if err != nil {
		return stat.Page{}, fmt.Errorf("统计数值项资料: %w", err)
	}
	rows, err := client.GameStat.Query().
		Where(filters...).
		Order(statOrder(query.Sort)...).
		Limit(int(query.PageSize)).
		Offset(int(query.Page-1) * int(query.PageSize)).
		All(ctx)
	if err != nil {
		return stat.Page{}, fmt.Errorf("查询数值项资料页: %w", err)
	}
	items := make([]stat.Stat, len(rows))
	for index, row := range rows {
		items[index] = statFromValues(databaseIdentifier(row.ID), row.Code, row.Name, row.SortOrder, row.BattleOnly, row.Enabled, row.Version)
	}
	return stat.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

func statPredicates(query stat.ListQuery) []predicate.GameStat {
	filters := make([]predicate.GameStat, 0, 7)
	if query.Q != "" {
		filters = append(filters, gamestat.Or(gamestat.CodeContainsFold(query.Q), gamestat.NameContainsFold(query.Q)))
	}
	if query.Code != "" {
		filters = append(filters, gamestat.CodeContainsFold(query.Code))
	}
	if query.Name != "" {
		filters = append(filters, gamestat.NameContainsFold(query.Name))
	}
	if query.SortOrder != nil {
		filters = append(filters, gamestat.SortOrderEQ(*query.SortOrder))
	}
	if query.BattleOnly != nil {
		filters = append(filters, gamestat.BattleOnlyEQ(*query.BattleOnly))
	}
	if query.Enabled != nil {
		filters = append(filters, gamestat.EnabledEQ(*query.Enabled))
	}
	return filters
}

func statOrder(value stat.Sort) []gamestat.OrderOption {
	id := gamestat.ByID(entsql.OrderAsc())
	switch value {
	case stat.SortCodeDescending:
		return []gamestat.OrderOption{gamestat.ByCode(entsql.OrderDesc()), id}
	case stat.SortNameAscending:
		return []gamestat.OrderOption{gamestat.ByName(entsql.OrderAsc()), id}
	case stat.SortNameDescending:
		return []gamestat.OrderOption{gamestat.ByName(entsql.OrderDesc()), id}
	case stat.SortOrderAscending:
		return []gamestat.OrderOption{gamestat.BySortOrder(entsql.OrderAsc()), id}
	case stat.SortOrderDescending:
		return []gamestat.OrderOption{gamestat.BySortOrder(entsql.OrderDesc()), id}
	default:
		return []gamestat.OrderOption{gamestat.ByCode(entsql.OrderAsc()), id}
	}
}

// Update 使用预期版本原子更新数值项，并保存幂等响应和审计前后值。
func (w *statTransactionStore) Update(ctx context.Context, record stat.UpdateRecord) (stat.Stat, error) {
	digest, err := idempotency.Digest(struct {
		StatID          snowflake.ID
		ExpectedVersion int64
		Code            string
		Name            string
		SortOrder       int32
		BattleOnly      bool
		Enabled         bool
	}{record.Stat.ID, record.ExpectedVersion, record.Stat.Code,
		record.Stat.Name, record.Stat.SortOrder, record.Stat.BattleOnly, record.Stat.Enabled})
	if err != nil {
		return stat.Stat{}, fmt.Errorf("计算数值项资料更新幂等摘要: %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: updateStatOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt,
	}
	updated := record.Stat
	client := w.client
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return stat.Stat{}, fmt.Errorf("认领数值项资料更新幂等键: %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := client.GameStat.Query().
		Where(gamestat.IDEQ(record.Stat.ID)).
		Only(ctx)
	if avalonent.IsNotFound(err) {
		return stat.Stat{}, stat.ErrStatNotFound
	}
	if err != nil {
		return stat.Stat{}, fmt.Errorf("锁定待更新数值项资料: %w", err)
	}
	current := statFromValues(databaseIdentifier(currentRow.ID), currentRow.Code, currentRow.Name, currentRow.SortOrder,
		currentRow.BattleOnly, currentRow.Enabled, currentRow.Version)
	if current.Version != record.ExpectedVersion {
		return stat.Stat{}, stat.ErrStatVersionConflict
	}
	row, err := client.GameStat.UpdateOneID(updated.ID).
		Where(gamestat.VersionEQ(record.ExpectedVersion)).
		SetCode(updated.Code).
		SetName(updated.Name).
		SetSortOrder(updated.SortOrder).
		SetBattleOnly(updated.BattleOnly).
		SetEnabled(updated.Enabled).
		SetVersion(updated.Version).
		SetUpdatedAt(record.UpdatedAt).
		Save(ctx)
	if avalonent.IsNotFound(err) {
		return stat.Stat{}, stat.ErrStatVersionConflict
	}
	if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23505" {
			return stat.Stat{}, stat.ErrStatCodeConflict
		}
		return stat.Stat{}, fmt.Errorf("更新数值项资料: %w", err)
	}
	updated = statFromValues(databaseIdentifier(row.ID), row.Code, row.Name, row.SortOrder, row.BattleOnly, row.Enabled, row.Version)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.stat.updated",
		"game_stat", updated.ID, record.RequestID, record.UpdatedAt, &current, &updated); err != nil {
		return stat.Stat{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return stat.Stat{}, fmt.Errorf("保存数值项资料更新幂等结果: %w", err)
	}
	return updated, nil
}

// Disable 使用预期版本禁用数值项，并保存幂等响应和审计事实。
func (w *statTransactionStore) Disable(ctx context.Context, record stat.DisableRecord) error {
	digest, err := idempotency.Digest(struct {
		StatID          snowflake.ID
		ExpectedVersion int64
	}{record.StatID, record.ExpectedVersion})
	if err != nil {
		return fmt.Errorf("计算数值项资料禁用幂等摘要: %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: deleteStatOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt,
	}
	client := w.client
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(client, w.parent.newID))
	response := struct {
		Disabled bool `json:"disabled"`
	}{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil {
		return fmt.Errorf("认领数值项资料禁用幂等键: %w", err)
	}
	if replay {
		return nil
	}
	currentRow, err := client.GameStat.Query().
		Where(gamestat.IDEQ(record.StatID)).
		Only(ctx)
	if avalonent.IsNotFound(err) {
		return stat.ErrStatNotFound
	}
	if err != nil {
		return fmt.Errorf("锁定待禁用数值项资料: %w", err)
	}
	current := statFromValues(databaseIdentifier(currentRow.ID), currentRow.Code, currentRow.Name, currentRow.SortOrder,
		currentRow.BattleOnly, currentRow.Enabled, currentRow.Version)
	if current.Version != record.ExpectedVersion {
		return stat.ErrStatVersionConflict
	}
	if _, err := client.GameStat.UpdateOneID(record.StatID).
		Where(gamestat.VersionEQ(record.ExpectedVersion)).
		SetEnabled(false).
		AddVersion(1).
		SetUpdatedAt(record.DisabledAt).
		Save(ctx); avalonent.IsNotFound(err) {
		return stat.ErrStatVersionConflict
	} else if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23503" {
			return stat.ErrStatReferenced
		}
		return fmt.Errorf("禁用数值项资料: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.stat.disabled",
		"game_stat", current.ID, record.RequestID, record.DisabledAt, &current, nil); err != nil {
		return err
	}
	response.Disabled = true
	if err := idempotency.Complete(ctx, writer, request, response); err != nil {
		return fmt.Errorf("保存数值项资料禁用幂等结果: %w", err)
	}
	return nil
}
