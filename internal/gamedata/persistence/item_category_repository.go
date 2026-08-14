package persistence

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameitemcategory"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/itemcategory"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createItemCategoryOperationID = "game-data.item-category.create"
	updateItemCategoryOperationID = "game-data.item-category.update"
	deleteItemCategoryOperationID = "game-data.item-category.delete"
)

// itemCategoryTransactionRepository 隔离道具分类 Writer 的方法集合。
type itemCategoryTransactionRepository struct {
	parent   *Adapters
	client   *avalonent.Client
	executor database.Transaction
}

// Create 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呭垱寤虹ǔ瀹氬垎绫昏韩浠姐€佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *itemCategoryTransactionRepository) Create(
	ctx context.Context,
	record itemcategory.CreateRecord,
) (itemcategory.Category, error) {
	digest, err := idempotency.Digest(struct {
		Code      string
		Name      string
		SortOrder int32
		Enabled   bool
	}{record.Category.Code, record.Category.Name, record.Category.SortOrder, record.Category.Enabled})
	if err != nil {
		return itemcategory.Category{}, fmt.Errorf("计算道具分类创建幂等摘要: %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: createItemCategoryOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt,
	}
	created := record.Category
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return itemcategory.Category{}, fmt.Errorf("璁ら閬撳叿鍒嗙被鍒涘缓骞傜瓑閿? %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := w.client.GameItemCategory.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetPocketID(created.PocketID).SetSortOrder(created.SortOrder).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23505" {
			return itemcategory.Category{}, itemcategory.ErrItemCategoryCodeConflict
		}
		return itemcategory.Category{}, fmt.Errorf("插入道具分类 实时资料修订: %w", err)
	}
	created = itemcategory.Category{ID: row.ID, Code: row.Code, Name: row.Name, PocketID: row.PocketID, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.item-category.created",
		"game_item_category", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return itemcategory.Category{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return itemcategory.Category{}, fmt.Errorf("保存道具分类创建幂等结果: %w", err)
	}
	return created, nil
}

// GetItemCategory 璇诲彇褰撳墠瀹炴椂璧勬枡涓寚瀹氱ǔ瀹氳韩浠界殑閬撳叿鍒嗙被銆?
func (s *Adapters) GetItemCategory(ctx context.Context, categoryID snowflake.ID) (itemcategory.Category, error) {
	row, err := s.pool.Client(ctx).GameItemCategory.Query().Where(gameitemcategory.IDEQ(categoryID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return itemcategory.Category{}, itemcategory.ErrItemCategoryNotFound
	}
	if err != nil {
		return itemcategory.Category{}, fmt.Errorf("查询道具分类: %w", err)
	}
	return itemcategory.Category{ID: row.ID, Code: row.Code, Name: row.Name, PocketID: row.PocketID, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}, nil
}

// ListItemCategories 杩斿洖褰撳墠瀹炴椂璧勬枡涓鍚堟樉寮忕瓫閫夋潯浠剁殑閬撳叿鍒嗙被椤点€?
func (s *Adapters) ListItemCategories(ctx context.Context, query itemcategory.ListQuery) (itemcategory.Page, error) {
	filters := make([]predicate.GameItemCategory, 0, 5)
	if query.Q != "" {
		filters = append(filters, gameitemcategory.Or(gameitemcategory.CodeContainsFold(query.Q), gameitemcategory.NameContainsFold(query.Q)))
	}
	if query.Code != "" {
		filters = append(filters, gameitemcategory.CodeContainsFold(query.Code))
	}
	if query.Name != "" {
		filters = append(filters, gameitemcategory.NameContainsFold(query.Name))
	}
	if query.SortOrder != nil {
		filters = append(filters, gameitemcategory.SortOrderEQ(*query.SortOrder))
	}
	if query.Enabled != nil {
		filters = append(filters, gameitemcategory.EnabledEQ(*query.Enabled))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameItemCategory.Query().Where(filters...).Count(ctx)
	if err != nil {
		return itemcategory.Page{}, fmt.Errorf("统计道具分类: %w", err)
	}
	order := []gameitemcategory.OrderOption{gameitemcategory.ByCode()}
	switch query.Sort {
	case itemcategory.SortCodeDescending:
		order = []gameitemcategory.OrderOption{gameitemcategory.ByCode(sql.OrderDesc()), gameitemcategory.ByID(sql.OrderDesc())}
	case itemcategory.SortNameAscending:
		order = []gameitemcategory.OrderOption{gameitemcategory.ByName(), gameitemcategory.ByID()}
	case itemcategory.SortNameDescending:
		order = []gameitemcategory.OrderOption{gameitemcategory.ByName(sql.OrderDesc()), gameitemcategory.ByID(sql.OrderDesc())}
	case itemcategory.SortOrderAscending:
		order = []gameitemcategory.OrderOption{gameitemcategory.BySortOrder(), gameitemcategory.ByID()}
	case itemcategory.SortOrderDescending:
		order = []gameitemcategory.OrderOption{gameitemcategory.BySortOrder(sql.OrderDesc()), gameitemcategory.ByID(sql.OrderDesc())}
	}
	rows, err := client.GameItemCategory.Query().Where(filters...).Order(order...).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return itemcategory.Page{}, fmt.Errorf("鏌ヨ閬撳叿鍒嗙被椤? %w", err)
	}
	items := make([]itemcategory.Category, len(rows))
	for index, row := range rows {
		items[index] = itemcategory.Category{ID: row.ID, Code: row.Code, Name: row.Name, PocketID: row.PocketID, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
	}
	return itemcategory.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// Update 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呮洿鏂?瀹炴椂璧勬枡淇銆佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *itemCategoryTransactionRepository) Update(
	ctx context.Context,
	record itemcategory.UpdateRecord,
) (itemcategory.Category, error) {
	digest, err := idempotency.Digest(struct {
		CategoryID      snowflake.ID
		ExpectedVersion int64
		Code            string
		Name            string
		SortOrder       int32
		Enabled         bool
	}{record.Category.ID, record.ExpectedVersion, record.Category.Code,
		record.Category.Name, record.Category.SortOrder, record.Category.Enabled})
	if err != nil {
		return itemcategory.Category{}, fmt.Errorf("计算道具分类更新幂等摘要: %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: updateItemCategoryOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt,
	}
	updated := record.Category
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return itemcategory.Category{}, fmt.Errorf("璁ら閬撳叿鍒嗙被鏇存柊骞傜瓑閿? %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameItemCategory.Query().Where(gameitemcategory.IDEQ(record.Category.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return itemcategory.Category{}, itemcategory.ErrItemCategoryNotFound
	}
	if err != nil {
		return itemcategory.Category{}, fmt.Errorf("閿佸畾寰呮洿鏂伴亾鍏峰垎绫? %w", err)
	}
	current := itemcategory.Category{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, PocketID: currentRow.PocketID, SortOrder: currentRow.SortOrder, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return itemcategory.Category{}, itemcategory.ErrItemCategoryVersionConflict
	}
	row, err := w.client.GameItemCategory.UpdateOne(currentRow).Where(gameitemcategory.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetPocketID(updated.PocketID).SetSortOrder(updated.SortOrder).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) {
		return itemcategory.Category{}, itemcategory.ErrItemCategoryVersionConflict
	}
	if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23505" {
			return itemcategory.Category{}, itemcategory.ErrItemCategoryCodeConflict
		}
		return itemcategory.Category{}, fmt.Errorf("更新道具分类 实时资料修订: %w", err)
	}
	updated = itemcategory.Category{ID: row.ID, Code: row.Code, Name: row.Name, PocketID: row.PocketID, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.item-category.updated",
		"game_item_category", updated.ID, record.RequestID, record.UpdatedAt, &current, &updated); err != nil {
		return itemcategory.Category{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return itemcategory.Category{}, fmt.Errorf("保存道具分类更新幂等结果: %w", err)
	}
	return updated, nil
}

// Delete 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呯鐢?瀹炴椂璧勬枡淇銆佽褰曞璁″苟淇濆瓨骞傜瓑鍝嶅簲銆?
func (w *itemCategoryTransactionRepository) Disable(ctx context.Context, record itemcategory.DisableRecord) error {
	digest, err := idempotency.Digest(struct {
		CategoryID      snowflake.ID
		ExpectedVersion int64
	}{record.CategoryID, record.ExpectedVersion})
	if err != nil {
		return fmt.Errorf("计算道具分类禁用幂等摘要: %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: deleteItemCategoryOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt,
	}
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	response := struct {
		Disabled bool `json:"disabled"`
	}{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil {
		return fmt.Errorf("璁ら閬撳叿鍒嗙被绂佺敤骞傜瓑閿? %w", err)
	}
	if replay {
		return nil
	}
	currentRow, err := w.client.GameItemCategory.Query().Where(gameitemcategory.IDEQ(record.CategoryID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return itemcategory.ErrItemCategoryNotFound
	}
	if err != nil {
		return fmt.Errorf("閿佸畾寰呯鐢ㄩ亾鍏峰垎绫? %w", err)
	}
	current := itemcategory.Category{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, SortOrder: currentRow.SortOrder, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return itemcategory.ErrItemCategoryVersionConflict
	}
	if _, err := w.client.GameItemCategory.UpdateOne(currentRow).Where(gameitemcategory.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return itemcategory.ErrItemCategoryVersionConflict
	} else if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23503" {
			return itemcategory.ErrItemCategoryReferenced
		}
		return fmt.Errorf("禁用道具分类 实时资料修订: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.item-category.disabled",
		"game_item_category", current.ID, record.RequestID, record.DisabledAt, &current, nil); err != nil {
		return err
	}
	response.Disabled = true
	if err := idempotency.Complete(ctx, writer, request, response); err != nil {
		return fmt.Errorf("保存道具分类禁用幂等结果: %w", err)
	}
	return nil
}
