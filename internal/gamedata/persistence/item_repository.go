package persistence

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameitem"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/item"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createItemOperationID = "game-data.item.create"
	updateItemOperationID = "game-data.item.update"
	deleteItemOperationID = "game-data.item.delete"
)

// itemTransactionRepository 隔离道具资料 Writer 的方法集合。
type itemTransactionRepository struct {
	parent   *Adapters
	client   *avalonent.Client
	executor database.Transaction
}

// Create 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呭垱寤虹ǔ瀹氶亾鍏疯韩浠姐€佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *itemTransactionRepository) Create(ctx context.Context, record item.CreateRecord) (item.Item, error) {
	digest, err := idempotency.Digest(struct {
		Code       string
		Name       string
		UsageType  item.UsageType
		CategoryID *snowflake.ID
		Cost       int32
		FlingPower *int32
		Enabled    bool
	}{record.Item.Code, record.Item.Name, record.Item.UsageType,
		record.Item.CategoryID, record.Item.Cost, record.Item.FlingPower, record.Item.Enabled})
	if err != nil {
		return item.Item{}, fmt.Errorf("计算道具资料创建幂等摘要: %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: createItemOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt,
	}
	created := record.Item
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return item.Item{}, fmt.Errorf("璁ら閬撳叿璧勬枡鍒涘缓骞傜瓑閿? %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := w.client.GameItem.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetNillableDescription(created.Description).SetNillableEffect(created.Effect).SetNillableShortEffect(created.ShortEffect).SetUsageType(string(created.UsageType)).SetNillableCategoryID(optionalEntIdentifier(created.CategoryID)).SetNillableFlingEffectID(optionalEntIdentifier(created.FlingEffectID)).SetCost(created.Cost).SetNillableFlingPower(created.FlingPower).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if err != nil {
		if domainErr := itemConstraintError(err); domainErr != nil {
			return item.Item{}, domainErr
		}
		return item.Item{}, fmt.Errorf("插入道具资料 实时资料修订: %w", err)
	}
	created = itemFromEnt(row)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.item.created",
		"game_item", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return item.Item{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return item.Item{}, fmt.Errorf("保存道具资料创建幂等结果: %w", err)
	}
	return created, nil
}

// GetItem 璇诲彇褰撳墠瀹炴椂璧勬枡涓寚瀹氱ǔ瀹氳韩浠界殑閬撳叿璧勬枡銆?
func (s *Adapters) GetItem(ctx context.Context, itemID snowflake.ID) (item.Item, error) {
	row, err := s.pool.Client(ctx).GameItem.Query().Where(gameitem.IDEQ(itemID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return item.Item{}, item.ErrItemNotFound
	}
	if err != nil {
		return item.Item{}, fmt.Errorf("查询道具资料: %w", err)
	}
	return itemFromEnt(row), nil
}

// ListItems 杩斿洖褰撳墠瀹炴椂璧勬枡涓鍚堟樉寮忕瓫閫夋潯浠剁殑閬撳叿璧勬枡椤点€?
func (s *Adapters) ListItems(ctx context.Context, query item.ListQuery) (item.Page, error) {
	filters := make([]predicate.GameItem, 0, 8)
	if query.Q != "" {
		filters = append(filters, gameitem.Or(gameitem.CodeContainsFold(query.Q), gameitem.NameContainsFold(query.Q)))
	}
	if query.Code != "" {
		filters = append(filters, gameitem.CodeContainsFold(query.Code))
	}
	if query.Name != "" {
		filters = append(filters, gameitem.NameContainsFold(query.Name))
	}
	if query.UsageType != nil {
		filters = append(filters, gameitem.UsageTypeEQ(string(*query.UsageType)))
	}
	if query.CategoryID != nil {
		filters = append(filters, gameitem.CategoryIDEQ(*query.CategoryID))
	}
	if query.Cost != nil {
		filters = append(filters, gameitem.CostEQ(*query.Cost))
	}
	if query.Enabled != nil {
		filters = append(filters, gameitem.EnabledEQ(*query.Enabled))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameItem.Query().Where(filters...).Count(ctx)
	if err != nil {
		return item.Page{}, fmt.Errorf("统计道具资料: %w", err)
	}
	order := []gameitem.OrderOption{gameitem.ByCode(), gameitem.ByID()}
	if query.Sort == item.SortCodeDescending {
		order = []gameitem.OrderOption{gameitem.ByCode(sql.OrderDesc()), gameitem.ByID(sql.OrderDesc())}
	}
	if query.Sort == item.SortNameAscending {
		order = []gameitem.OrderOption{gameitem.ByName(), gameitem.ByID()}
	}
	if query.Sort == item.SortNameDescending {
		order = []gameitem.OrderOption{gameitem.ByName(sql.OrderDesc()), gameitem.ByID(sql.OrderDesc())}
	}
	rows, err := client.GameItem.Query().Where(filters...).Order(order...).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return item.Page{}, fmt.Errorf("鏌ヨ閬撳叿璧勬枡椤? %w", err)
	}
	items := make([]item.Item, len(rows))
	for index, row := range rows {
		items[index] = itemFromEnt(row)
	}
	return item.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// Update 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呮洿鏂?瀹炴椂璧勬枡淇銆佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *itemTransactionRepository) Update(ctx context.Context, record item.UpdateRecord) (item.Item, error) {
	digest, err := idempotency.Digest(struct {
		ItemID          snowflake.ID
		ExpectedVersion int64
		Code            string
		Name            string
		UsageType       item.UsageType
		CategoryID      *snowflake.ID
		Cost            int32
		FlingPower      *int32
		Enabled         bool
	}{record.Item.ID, record.ExpectedVersion, record.Item.Code,
		record.Item.Name, record.Item.UsageType, record.Item.CategoryID, record.Item.Cost,
		record.Item.FlingPower, record.Item.Enabled})
	if err != nil {
		return item.Item{}, fmt.Errorf("计算道具资料更新幂等摘要: %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: updateItemOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt,
	}
	updated := record.Item
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return item.Item{}, fmt.Errorf("璁ら閬撳叿璧勬枡鏇存柊骞傜瓑閿? %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameItem.Query().Where(gameitem.IDEQ(record.Item.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return item.Item{}, item.ErrItemNotFound
	}
	if err != nil {
		return item.Item{}, fmt.Errorf("閿佸畾寰呮洿鏂伴亾鍏疯祫鏂? %w", err)
	}
	current := itemFromEnt(currentRow)
	if current.Version != record.ExpectedVersion {
		return item.Item{}, item.ErrItemVersionConflict
	}
	row, err := w.client.GameItem.UpdateOne(currentRow).Where(gameitem.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetNillableDescription(updated.Description).SetNillableEffect(updated.Effect).SetNillableShortEffect(updated.ShortEffect).SetUsageType(string(updated.UsageType)).SetNillableCategoryID(optionalEntIdentifier(updated.CategoryID)).SetNillableFlingEffectID(optionalEntIdentifier(updated.FlingEffectID)).SetCost(updated.Cost).SetNillableFlingPower(updated.FlingPower).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) {
		return item.Item{}, item.ErrItemVersionConflict
	}
	if err != nil {
		if domainErr := itemConstraintError(err); domainErr != nil {
			return item.Item{}, domainErr
		}
		return item.Item{}, fmt.Errorf("更新道具资料 实时资料修订: %w", err)
	}
	updated = itemFromEnt(row)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.item.updated",
		"game_item", updated.ID, record.RequestID, record.UpdatedAt, &current, &updated); err != nil {
		return item.Item{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return item.Item{}, fmt.Errorf("保存道具资料更新幂等结果: %w", err)
	}
	return updated, nil
}

// Delete 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呯鐢?瀹炴椂璧勬枡淇銆佽褰曞璁″苟淇濆瓨骞傜瓑鍝嶅簲銆?
func (w *itemTransactionRepository) Disable(ctx context.Context, record item.DisableRecord) error {
	digest, err := idempotency.Digest(struct {
		ItemID          snowflake.ID
		ExpectedVersion int64
	}{record.ItemID, record.ExpectedVersion})
	if err != nil {
		return fmt.Errorf("计算道具资料禁用幂等摘要: %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: deleteItemOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt,
	}
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	response := struct {
		Disabled bool `json:"disabled"`
	}{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil {
		return fmt.Errorf("璁ら閬撳叿璧勬枡绂佺敤骞傜瓑閿? %w", err)
	}
	if replay {
		return nil
	}
	currentRow, err := w.client.GameItem.Query().Where(gameitem.IDEQ(record.ItemID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return item.ErrItemNotFound
	}
	if err != nil {
		return fmt.Errorf("閿佸畾寰呯鐢ㄩ亾鍏疯祫鏂? %w", err)
	}
	current := itemFromEnt(currentRow)
	if current.Version != record.ExpectedVersion {
		return item.ErrItemVersionConflict
	}
	if _, err := w.client.GameItem.UpdateOne(currentRow).Where(gameitem.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return item.ErrItemVersionConflict
	} else if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23503" {
			return item.ErrItemReferenced
		}
		return fmt.Errorf("禁用道具资料 实时资料修订: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.item.disabled",
		"game_item", current.ID, record.RequestID, record.DisabledAt, &current, nil); err != nil {
		return err
	}
	response.Disabled = true
	if err := idempotency.Complete(ctx, writer, request, response); err != nil {
		return fmt.Errorf("保存道具资料禁用幂等结果: %w", err)
	}
	return nil
}

func itemConstraintError(err error) error {
	var databaseError *pgconn.PgError
	if !errors.As(err, &databaseError) {
		return nil
	}
	switch databaseError.Code {
	case "23505":
		return item.ErrItemCodeConflict
	case "23503":
		return item.ErrItemCategoryNotFound
	default:
		return nil
	}
}

// itemFromEnt 将 Ent 生成实体转换为道具领域对象，集中处理可空字段。
func itemFromEnt(row *avalonent.GameItem) item.Item {
	return itemFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.UsageType, pgIdentifierPointer(row.CategoryID), row.Cost, databaseInt32(row.FlingPower), row.Enabled, row.Version, pgIdentifierPointer(row.AssetID), nullableText(row.Description), nullableText(row.Effect), nullableText(row.ShortEffect), pgIdentifierPointer(row.FlingEffectID))
}
