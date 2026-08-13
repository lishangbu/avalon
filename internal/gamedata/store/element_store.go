package store

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameelement"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/element"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createElementOperationID = "game-data.element.create"
	updateElementOperationID = "game-data.element.update"
	deleteElementOperationID = "game-data.element.delete"
)

// Create 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呭垱寤虹ǔ瀹氳祫鏂欒韩浠姐€佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *transactionStore) Create(ctx context.Context, record element.CreateRecord) (element.Element, error) {
	digest, err := idempotency.Digest(struct {
		Code      string
		Name      string
		SortOrder int32
		Enabled   bool
	}{record.Element.Code, record.Element.Name, record.Element.SortOrder, record.Element.Enabled})
	if err != nil {
		return element.Element{}, fmt.Errorf("璁＄畻灞炴€ц祫鏂欏垱寤哄箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: createElementOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt,
	}
	created := record.Element
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return element.Element{}, fmt.Errorf("认领属性资料创建幂等键: %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := w.client.GameElement.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetSortOrder(created.SortOrder).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23505" {
			return element.Element{}, element.ErrElementCodeConflict
		}
		return element.Element{}, fmt.Errorf("鎻掑叆灞炴€ц祫鏂?瀹炴椂璧勬枡淇: %w", err)
	}
	created = element.Element{ID: row.ID, Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.element.created",
		"game_element", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return element.Element{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return element.Element{}, fmt.Errorf("淇濆瓨灞炴€ц祫鏂欏垱寤哄箓绛夌粨鏋? %w", err)
	}
	return created, nil
}

// Get 通过稳定 Identifier 读取当前实时属性资料。
func (s *Store) Get(ctx context.Context, elementID snowflake.ID) (element.Element, error) {
	row, err := s.pool.Client(ctx).GameElement.Query().Where(gameelement.IDEQ(elementID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return element.Element{}, element.ErrElementNotFound
	}
	if err != nil {
		return element.Element{}, fmt.Errorf("鏌ヨ灞炴€ц祫鏂? %w", err)
	}
	return elementFromValues(databaseIdentifier(row.ID), row.Code, row.Name, row.SortOrder, row.Enabled, row.Version), nil
}

// List 使用 Ent 谓词、稳定排序和页码分页返回属性资料。
func (s *Store) List(ctx context.Context, query element.ListQuery) (element.Page, error) {
	filters := elementPredicates(query)
	client := s.pool.Client(ctx)
	total, err := client.GameElement.Query().Where(filters...).Count(ctx)
	if err != nil {
		return element.Page{}, fmt.Errorf("缁熻灞炴€ц祫鏂? %w", err)
	}
	rows, err := client.GameElement.Query().Where(filters...).Order(elementOrder(query.Sort)...).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return element.Page{}, fmt.Errorf("查询属性资料页: %w", err)
	}
	items := make([]element.Element, len(rows))
	for index, row := range rows {
		items[index] = elementFromValues(databaseIdentifier(row.ID), row.Code, row.Name, row.SortOrder, row.Enabled, row.Version)
	}
	return element.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// elementPredicates 将管理端筛选条件转换为 Ent 谓词，确保列表查询与写入使用同一实时资料表。
func elementPredicates(query element.ListQuery) []predicate.GameElement {
	filters := make([]predicate.GameElement, 0, 5)
	if query.Q != "" {
		filters = append(filters, gameelement.Or(gameelement.CodeContainsFold(query.Q), gameelement.NameContainsFold(query.Q)))
	}
	if query.Code != "" {
		filters = append(filters, gameelement.CodeContainsFold(query.Code))
	}
	if query.Name != "" {
		filters = append(filters, gameelement.NameContainsFold(query.Name))
	}
	if query.SortOrder != nil {
		filters = append(filters, gameelement.SortOrderEQ(*query.SortOrder))
	}
	if query.Enabled != nil {
		filters = append(filters, gameelement.EnabledEQ(*query.Enabled))
	}
	return filters
}

// elementOrder 返回稳定排序，并以 Identifier 作为平局时的确定性次序。
func elementOrder(value element.Sort) []gameelement.OrderOption {
	id := gameelement.ByID(sql.OrderAsc())
	switch value {
	case element.SortCodeDescending:
		return []gameelement.OrderOption{gameelement.ByCode(sql.OrderDesc()), id}
	case element.SortNameAscending:
		return []gameelement.OrderOption{gameelement.ByName(sql.OrderAsc()), id}
	case element.SortNameDescending:
		return []gameelement.OrderOption{gameelement.ByName(sql.OrderDesc()), id}
	case element.SortOrderAscending:
		return []gameelement.OrderOption{gameelement.BySortOrder(sql.OrderAsc()), id}
	case element.SortOrderDescending:
		return []gameelement.OrderOption{gameelement.BySortOrder(sql.OrderDesc()), id}
	default:
		return []gameelement.OrderOption{gameelement.ByCode(sql.OrderAsc()), id}
	}
}

// Update 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呮洿鏂?瀹炴椂璧勬枡淇銆佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *transactionStore) Update(ctx context.Context, record element.UpdateRecord) (element.Element, error) {
	digest, err := idempotency.Digest(struct {
		ElementID       snowflake.ID
		ExpectedVersion int64
		Code            string
		Name            string
		SortOrder       int32
		Enabled         bool
	}{record.Element.ID, record.ExpectedVersion, record.Element.Code, record.Element.Name,
		record.Element.SortOrder, record.Element.Enabled})
	if err != nil {
		return element.Element{}, fmt.Errorf("璁＄畻灞炴€ц祫鏂欐洿鏂板箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: updateElementOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt,
	}
	updated := record.Element
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return element.Element{}, fmt.Errorf("认领属性资料更新幂等键: %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameElement.Query().Where(gameelement.IDEQ(record.Element.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return element.Element{}, element.ErrElementNotFound
	}
	if err != nil {
		return element.Element{}, fmt.Errorf("閿佸畾寰呮洿鏂板睘鎬ц祫鏂? %w", err)
	}
	current := element.Element{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, SortOrder: currentRow.SortOrder, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return element.Element{}, element.ErrElementVersionConflict
	}
	row, err := w.client.GameElement.UpdateOne(currentRow).Where(gameelement.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetSortOrder(updated.SortOrder).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) {
		return element.Element{}, element.ErrElementVersionConflict
	}
	if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23505" {
			return element.Element{}, element.ErrElementCodeConflict
		}
		return element.Element{}, fmt.Errorf("鏇存柊灞炴€ц祫鏂?瀹炴椂璧勬枡淇: %w", err)
	}
	updated = element.Element{ID: row.ID, Code: row.Code, Name: row.Name, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.element.updated",
		"game_element", updated.ID, record.RequestID, record.UpdatedAt, &current, &updated); err != nil {
		return element.Element{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return element.Element{}, fmt.Errorf("淇濆瓨灞炴€ц祫鏂欐洿鏂板箓绛夌粨鏋? %w", err)
	}
	return updated, nil
}

// Delete 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呯鐢?瀹炴椂璧勬枡淇銆佽褰曞璁″苟淇濆瓨骞傜瓑鍝嶅簲銆?
func (w *transactionStore) Disable(ctx context.Context, record element.DisableRecord) error {
	digest, err := idempotency.Digest(struct {
		ElementID       snowflake.ID
		ExpectedVersion int64
	}{record.ElementID, record.ExpectedVersion})
	if err != nil {
		return fmt.Errorf("璁＄畻灞炴€ц祫鏂欑鐢ㄥ箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: deleteElementOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt,
	}
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	response := struct {
		Disabled bool `json:"disabled"`
	}{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil {
		return fmt.Errorf("认领属性资料禁用幂等键: %w", err)
	}
	if replay {
		return nil
	}
	currentRow, err := w.client.GameElement.Query().Where(gameelement.IDEQ(record.ElementID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return element.ErrElementNotFound
	}
	if err != nil {
		return fmt.Errorf("閿佸畾寰呯鐢ㄥ睘鎬ц祫鏂? %w", err)
	}
	current := element.Element{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, SortOrder: currentRow.SortOrder, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return element.ErrElementVersionConflict
	}
	if _, err := w.client.GameElement.UpdateOne(currentRow).Where(gameelement.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return element.ErrElementVersionConflict
	} else if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23503" {
			return element.ErrElementReferenced
		}
		return fmt.Errorf("绂佺敤灞炴€ц祫鏂?瀹炴椂璧勬枡淇: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.element.disabled",
		"game_element", current.ID, record.RequestID, record.DisabledAt, &current, nil); err != nil {
		return err
	}
	response.Disabled = true
	if err := idempotency.Complete(ctx, writer, request, response); err != nil {
		return fmt.Errorf("淇濆瓨灞炴€ц祫鏂欑鐢ㄥ箓绛夌粨鏋? %w", err)
	}
	return nil
}
