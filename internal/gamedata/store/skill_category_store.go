package store

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameskillcategory"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/skillcategory"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createSkillCategoryOperationID = "game-data.skill-category.create"
	updateSkillCategoryOperationID = "game-data.skill-category.update"
	deleteSkillCategoryOperationID = "game-data.skill-category.delete"
)

type skillCategoryTransactionStore struct {
	parent   *Store
	client   *avalonent.Client
	executor database.Transaction
}

// Create 鍦ㄥ悓涓€浜嬪姟涓啓鍏ユ妧鑳藉厓鍒嗙被韬唤銆佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillCategoryTransactionStore) Create(ctx context.Context, record skillcategory.CreateRecord) (skillcategory.Category, error) {
	// 鎽樿鍙寘鍚鎴风鍙噸澶嶆彁浜ょ殑浜嬪疄锛涙湇鍔＄姣忔灏濊瘯鐢熸垚鐨勬柊 Identifier 涓嶅緱鏀瑰彉骞傜瓑璇锋眰韬唤銆?
	digest, err := idempotency.Digest(struct {
		Code        string
		Name        string
		Description *string
		Enabled     bool
	}{
		Code:        record.Category.Code,
		Name:        record.Category.Name,
		Description: record.Category.Description,
		Enabled:     record.Category.Enabled,
	})
	if err != nil {
		return skillcategory.Category{}, fmt.Errorf("计算技能元分类创建幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: createSkillCategoryOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt}
	created := record.Category
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return skillcategory.Category{}, fmt.Errorf("璁ら鎶€鑳藉厓鍒嗙被鍒涘缓骞傜瓑閿? %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := w.client.GameSkillCategory.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetNillableDescription(created.Description).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if err != nil {
		return skillcategory.Category{}, mapSkillCategoryWriteError(err, "插入技能元分类 实时资料修订")
	}
	created = skillcategory.Category{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.skill-category.created",
		"game_skill_category", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return skillcategory.Category{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return skillcategory.Category{}, fmt.Errorf("保存技能元分类创建幂等结果: %w", err)
	}
	return created, nil
}

// GetSkillCategory 璇诲彇褰撳墠瀹炴椂璧勬枡涓寚瀹氱ǔ瀹氳韩浠界殑鎶€鑳藉厓鍒嗙被銆?
func (s *Store) GetSkillCategory(ctx context.Context, categoryID snowflake.ID) (skillcategory.Category, error) {
	row, err := s.pool.Client(ctx).GameSkillCategory.Query().Where(gameskillcategory.IDEQ(categoryID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skillcategory.Category{}, skillcategory.ErrSkillCategoryNotFound
	}
	if err != nil {
		return skillcategory.Category{}, fmt.Errorf("查询技能元分类: %w", err)
	}
	return skillcategory.Category{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}, nil
}

// ListSkillCategories 杩斿洖褰撳墠瀹炴椂璧勬枡涓鍚堟樉寮忕瓫閫夋潯浠剁殑鎶€鑳藉厓鍒嗙被椤点€?
func (s *Store) ListSkillCategories(ctx context.Context, query skillcategory.ListQuery) (skillcategory.Page, error) {
	filters := make([]predicate.GameSkillCategory, 0, 5)
	if query.Q != "" {
		filters = append(filters, gameskillcategory.Or(gameskillcategory.CodeContainsFold(query.Q), gameskillcategory.NameContainsFold(query.Q), gameskillcategory.DescriptionContainsFold(query.Q)))
	}
	if query.Code != "" {
		filters = append(filters, gameskillcategory.CodeContainsFold(query.Code))
	}
	if query.Name != "" {
		filters = append(filters, gameskillcategory.NameContainsFold(query.Name))
	}
	if query.Description != "" {
		filters = append(filters, gameskillcategory.DescriptionContainsFold(query.Description))
	}
	if query.Enabled != nil {
		filters = append(filters, gameskillcategory.EnabledEQ(*query.Enabled))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameSkillCategory.Query().Where(filters...).Count(ctx)
	if err != nil {
		return skillcategory.Page{}, fmt.Errorf("统计技能元分类: %w", err)
	}
	id := gameskillcategory.ByID(sql.OrderAsc())
	var order []gameskillcategory.OrderOption
	switch query.Sort {
	case skillcategory.SortCodeDescending:
		order = []gameskillcategory.OrderOption{gameskillcategory.ByCode(sql.OrderDesc()), id}
	case skillcategory.SortNameAscending:
		order = []gameskillcategory.OrderOption{gameskillcategory.ByName(sql.OrderAsc()), id}
	case skillcategory.SortNameDescending:
		order = []gameskillcategory.OrderOption{gameskillcategory.ByName(sql.OrderDesc()), id}
	default:
		order = []gameskillcategory.OrderOption{gameskillcategory.ByCode(sql.OrderAsc()), id}
	}
	rows, err := client.GameSkillCategory.Query().Where(filters...).Order(order...).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return skillcategory.Page{}, fmt.Errorf("鏌ヨ鎶€鑳藉厓鍒嗙被椤? %w", err)
	}
	items := make([]skillcategory.Category, len(rows))
	for index, row := range rows {
		items[index] = skillcategory.Category{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
	}
	return skillcategory.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// Update 鍦ㄥ悓涓€浜嬪姟涓墽琛屼箰瑙傛洿鏂般€佸疄鏃惰祫鏂?鐗堟湰鎺ㄨ繘銆佸璁″拰骞傜瓑鍝嶅簲淇濆瓨銆?
func (w *skillCategoryTransactionStore) Update(ctx context.Context, record skillcategory.UpdateRecord) (skillcategory.Category, error) {
	digest, err := idempotency.Digest(struct {
		Category        skillcategory.Category
		Description     skillcategory.DescriptionChange
		ExpectedVersion int64
	}{record.Category, record.Description, record.ExpectedVersion})
	if err != nil {
		return skillcategory.Category{}, fmt.Errorf("计算技能元分类更新幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: updateSkillCategoryOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt}
	updated := record.Category
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return skillcategory.Category{}, fmt.Errorf("璁ら鎶€鑳藉厓鍒嗙被鏇存柊骞傜瓑閿? %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameSkillCategory.Query().Where(gameskillcategory.IDEQ(updated.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skillcategory.Category{}, skillcategory.ErrSkillCategoryNotFound
	}
	if err != nil {
		return skillcategory.Category{}, fmt.Errorf("锁定待更新技能元分类: %w", err)
	}
	current := skillcategory.Category{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, Description: currentRow.Description, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return skillcategory.Category{}, skillcategory.ErrSkillCategoryVersionConflict
	}
	builder := w.client.GameSkillCategory.UpdateOne(currentRow).Where(gameskillcategory.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC())
	if record.Description.Specified {
		if record.Description.Value == nil {
			builder.ClearDescription()
		} else {
			builder.SetDescription(*record.Description.Value)
		}
	}
	row, err := builder.Save(ctx)
	if avalonent.IsNotFound(err) {
		return skillcategory.Category{}, skillcategory.ErrSkillCategoryVersionConflict
	}
	if err != nil {
		return skillcategory.Category{}, mapSkillCategoryWriteError(err, "更新技能元分类 实时资料修订")
	}
	updated = skillcategory.Category{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.skill-category.updated",
		"game_skill_category", updated.ID, record.RequestID, record.UpdatedAt, &current, &updated); err != nil {
		return skillcategory.Category{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return skillcategory.Category{}, fmt.Errorf("保存技能元分类更新幂等结果: %w", err)
	}
	return updated, nil
}

// Delete 鍦ㄥ悓涓€浜嬪姟涓鐢ㄦ湭琚紩鐢ㄧ殑 瀹炴椂璧勬枡淇骞朵繚瀛樺璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillCategoryTransactionStore) Disable(ctx context.Context, record skillcategory.DisableRecord) error {
	digest, err := idempotency.Digest(struct {
		CategoryID      snowflake.ID
		ExpectedVersion int64
	}{record.CategoryID, record.ExpectedVersion})
	if err != nil {
		return fmt.Errorf("计算技能元分类禁用幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: deleteSkillCategoryOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt}
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	response := struct {
		Disabled bool `json:"disabled"`
	}{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil {
		return fmt.Errorf("璁ら鎶€鑳藉厓鍒嗙被绂佺敤骞傜瓑閿? %w", err)
	}
	if replay {
		return nil
	}
	currentRow, err := w.client.GameSkillCategory.Query().Where(gameskillcategory.IDEQ(record.CategoryID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skillcategory.ErrSkillCategoryNotFound
	}
	if err != nil {
		return fmt.Errorf("锁定待禁用技能元分类: %w", err)
	}
	current := skillcategory.Category{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, Description: currentRow.Description, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return skillcategory.ErrSkillCategoryVersionConflict
	}
	if _, err := w.client.GameSkillCategory.UpdateOne(currentRow).Where(gameskillcategory.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return skillcategory.ErrSkillCategoryVersionConflict
	} else if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23503" {
			return skillcategory.ErrSkillCategoryReferenced
		}
		return fmt.Errorf("禁用技能元分类 实时资料修订: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.skill-category.disabled",
		"game_skill_category", current.ID, record.RequestID, record.DisabledAt, &current, nil); err != nil {
		return err
	}
	response.Disabled = true
	return idempotency.Complete(ctx, writer, request, response)
}

func mapSkillCategoryWriteError(err error, action string) error {
	var databaseError *pgconn.PgError
	if errors.As(err, &databaseError) && databaseError.Code == "23505" {
		return skillcategory.ErrSkillCategoryCodeConflict
	}
	return fmt.Errorf("%s: %w", action, err)
}
