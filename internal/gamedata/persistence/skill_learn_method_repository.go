package persistence

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameskilllearnmethod"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/skilllearnmethod"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createSkillLearnMethodOperationID = "game-data.skill-learn-method.create"
	updateSkillLearnMethodOperationID = "game-data.skill-learn-method.update"
	deleteSkillLearnMethodOperationID = "game-data.skill-learn-method.delete"
)

type skillLearnMethodTransactionRepository struct {
	parent   *Adapters
	client   *avalonent.Client
	executor database.Transaction
}

// Create 鍦ㄥ悓涓€浜嬪姟涓啓鍏ユ妧鑳藉涔犳柟寮忚韩浠姐€佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillLearnMethodTransactionRepository) Create(ctx context.Context, record skilllearnmethod.CreateRecord) (skilllearnmethod.Method, error) {
	// 鎽樿鍙寘鍚鎴风鍙噸澶嶆彁浜ょ殑浜嬪疄锛涙湇鍔＄姣忔灏濊瘯鐢熸垚鐨勬柊 Identifier 涓嶅緱鏀瑰彉骞傜瓑璇锋眰韬唤銆?
	digest, err := idempotency.Digest(struct {
		Code        string
		Name        string
		Description *string
		Enabled     bool
	}{
		Code:        record.Method.Code,
		Name:        record.Method.Name,
		Description: record.Method.Description,
		Enabled:     record.Method.Enabled,
	})
	if err != nil {
		return skilllearnmethod.Method{}, fmt.Errorf("璁＄畻鎶€鑳藉涔犳柟寮忓垱寤哄箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: createSkillLearnMethodOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt}
	created := record.Method
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return skilllearnmethod.Method{}, fmt.Errorf("认领技能学习方式创建幂等键: %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := w.client.GameSkillLearnMethod.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetNillableDescription(created.Description).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if err != nil {
		return skilllearnmethod.Method{}, mapSkillLearnMethodWriteError(err, "鎻掑叆鎶€鑳藉涔犳柟寮?瀹炴椂璧勬枡淇")
	}
	created = skilllearnmethod.Method{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.skill-learn-method.created",
		"game_skill_learn_method", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return skilllearnmethod.Method{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return skilllearnmethod.Method{}, fmt.Errorf("淇濆瓨鎶€鑳藉涔犳柟寮忓垱寤哄箓绛夌粨鏋? %w", err)
	}
	return created, nil
}

// GetSkillLearnMethod 璇诲彇褰撳墠瀹炴椂璧勬枡涓寚瀹氱ǔ瀹氳韩浠界殑鎶€鑳藉涔犳柟寮忋€?
func (s *Adapters) GetSkillLearnMethod(ctx context.Context, methodID snowflake.ID) (skilllearnmethod.Method, error) {
	row, err := s.pool.Client(ctx).GameSkillLearnMethod.Query().Where(gameskilllearnmethod.IDEQ(methodID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skilllearnmethod.Method{}, skilllearnmethod.ErrSkillLearnMethodNotFound
	}
	if err != nil {
		return skilllearnmethod.Method{}, fmt.Errorf("鏌ヨ鎶€鑳藉涔犳柟寮? %w", err)
	}
	return skilllearnmethod.Method{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}, nil
}

// ListSkillLearnMethods 杩斿洖褰撳墠瀹炴椂璧勬枡涓鍚堟樉寮忕瓫閫夋潯浠剁殑鎶€鑳藉涔犳柟寮忛〉銆?
func (s *Adapters) ListSkillLearnMethods(ctx context.Context, query skilllearnmethod.ListQuery) (skilllearnmethod.Page, error) {
	filters := make([]predicate.GameSkillLearnMethod, 0, 4)
	if query.Q != "" {
		filters = append(filters, gameskilllearnmethod.Or(gameskilllearnmethod.CodeContainsFold(query.Q), gameskilllearnmethod.NameContainsFold(query.Q), gameskilllearnmethod.DescriptionContainsFold(query.Q)))
	}
	if query.Code != "" {
		filters = append(filters, gameskilllearnmethod.CodeContainsFold(query.Code))
	}
	if query.Name != "" {
		filters = append(filters, gameskilllearnmethod.NameContainsFold(query.Name))
	}
	if query.Description != "" {
		filters = append(filters, gameskilllearnmethod.DescriptionContainsFold(query.Description))
	}
	if query.Enabled != nil {
		filters = append(filters, gameskilllearnmethod.EnabledEQ(*query.Enabled))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameSkillLearnMethod.Query().Where(filters...).Count(ctx)
	if err != nil {
		return skilllearnmethod.Page{}, fmt.Errorf("缁熻鎶€鑳藉涔犳柟寮? %w", err)
	}
	order := []gameskilllearnmethod.OrderOption{gameskilllearnmethod.ByCode()}
	if query.Sort == skilllearnmethod.SortCodeDescending {
		order = []gameskilllearnmethod.OrderOption{gameskilllearnmethod.ByCode(sql.OrderDesc()), gameskilllearnmethod.ByID(sql.OrderDesc())}
	}
	if query.Sort == skilllearnmethod.SortNameAscending {
		order = []gameskilllearnmethod.OrderOption{gameskilllearnmethod.ByName(), gameskilllearnmethod.ByID()}
	}
	if query.Sort == skilllearnmethod.SortNameDescending {
		order = []gameskilllearnmethod.OrderOption{gameskilllearnmethod.ByName(sql.OrderDesc()), gameskilllearnmethod.ByID(sql.OrderDesc())}
	}
	rows, err := client.GameSkillLearnMethod.Query().Where(filters...).Order(order...).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return skilllearnmethod.Page{}, fmt.Errorf("查询技能学习方式页: %w", err)
	}
	items := make([]skilllearnmethod.Method, len(rows))
	for index, row := range rows {
		items[index] = skilllearnmethod.Method{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
	}
	return skilllearnmethod.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// Update 鍦ㄥ悓涓€浜嬪姟涓墽琛屼箰瑙傛洿鏂般€佸疄鏃惰祫鏂?鐗堟湰鎺ㄨ繘銆佸璁″拰骞傜瓑鍝嶅簲淇濆瓨銆?
func (w *skillLearnMethodTransactionRepository) Update(ctx context.Context, record skilllearnmethod.UpdateRecord) (skilllearnmethod.Method, error) {
	digest, err := idempotency.Digest(struct {
		Method          skilllearnmethod.Method
		Description     skilllearnmethod.DescriptionChange
		ExpectedVersion int64
	}{record.Method, record.Description, record.ExpectedVersion})
	if err != nil {
		return skilllearnmethod.Method{}, fmt.Errorf("璁＄畻鎶€鑳藉涔犳柟寮忔洿鏂板箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: updateSkillLearnMethodOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt}
	updated := record.Method
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return skilllearnmethod.Method{}, fmt.Errorf("认领技能学习方式更新幂等键: %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameSkillLearnMethod.Query().Where(gameskilllearnmethod.IDEQ(updated.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skilllearnmethod.Method{}, skilllearnmethod.ErrSkillLearnMethodNotFound
	}
	if err != nil {
		return skilllearnmethod.Method{}, fmt.Errorf("閿佸畾寰呮洿鏂版妧鑳藉涔犳柟寮? %w", err)
	}
	current := skilllearnmethod.Method{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, Description: currentRow.Description, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return skilllearnmethod.Method{}, skilllearnmethod.ErrSkillLearnMethodVersionConflict
	}
	builder := w.client.GameSkillLearnMethod.UpdateOne(currentRow).Where(gameskilllearnmethod.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC())
	if record.Description.Specified {
		if record.Description.Value == nil {
			builder.ClearDescription()
		} else {
			builder.SetDescription(*record.Description.Value)
		}
	}
	row, err := builder.Save(ctx)
	if avalonent.IsNotFound(err) {
		return skilllearnmethod.Method{}, skilllearnmethod.ErrSkillLearnMethodVersionConflict
	}
	if err != nil {
		return skilllearnmethod.Method{}, mapSkillLearnMethodWriteError(err, "鏇存柊鎶€鑳藉涔犳柟寮?瀹炴椂璧勬枡淇")
	}
	updated = skilllearnmethod.Method{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.skill-learn-method.updated",
		"game_skill_learn_method", updated.ID, record.RequestID, record.UpdatedAt, &current, &updated); err != nil {
		return skilllearnmethod.Method{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return skilllearnmethod.Method{}, fmt.Errorf("淇濆瓨鎶€鑳藉涔犳柟寮忔洿鏂板箓绛夌粨鏋? %w", err)
	}
	return updated, nil
}

// Delete 鍦ㄥ悓涓€浜嬪姟涓鐢ㄦ湭琚紩鐢ㄧ殑 瀹炴椂璧勬枡淇骞朵繚瀛樺璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillLearnMethodTransactionRepository) Disable(ctx context.Context, record skilllearnmethod.DisableRecord) error {
	digest, err := idempotency.Digest(struct {
		MethodID        snowflake.ID
		ExpectedVersion int64
	}{record.MethodID, record.ExpectedVersion})
	if err != nil {
		return fmt.Errorf("璁＄畻鎶€鑳藉涔犳柟寮忕鐢ㄥ箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: deleteSkillLearnMethodOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt}
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	response := struct {
		Disabled bool `json:"disabled"`
	}{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil {
		return fmt.Errorf("认领技能学习方式禁用幂等键: %w", err)
	}
	if replay {
		return nil
	}
	currentRow, err := w.client.GameSkillLearnMethod.Query().Where(gameskilllearnmethod.IDEQ(record.MethodID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skilllearnmethod.ErrSkillLearnMethodNotFound
	}
	if err != nil {
		return fmt.Errorf("閿佸畾寰呯鐢ㄦ妧鑳藉涔犳柟寮? %w", err)
	}
	current := skilllearnmethod.Method{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, Description: currentRow.Description, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return skilllearnmethod.ErrSkillLearnMethodVersionConflict
	}
	if _, err := w.client.GameSkillLearnMethod.UpdateOne(currentRow).Where(gameskilllearnmethod.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return skilllearnmethod.ErrSkillLearnMethodVersionConflict
	} else if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23503" {
			return skilllearnmethod.ErrSkillLearnMethodReferenced
		}
		return fmt.Errorf("绂佺敤鎶€鑳藉涔犳柟寮?瀹炴椂璧勬枡淇: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.skill-learn-method.disabled",
		"game_skill_learn_method", current.ID, record.RequestID, record.DisabledAt, &current, nil); err != nil {
		return err
	}
	response.Disabled = true
	return idempotency.Complete(ctx, writer, request, response)
}

func mapSkillLearnMethodWriteError(err error, action string) error {
	var databaseError *pgconn.PgError
	if errors.As(err, &databaseError) && databaseError.Code == "23505" {
		return skilllearnmethod.ErrSkillLearnMethodCodeConflict
	}
	return fmt.Errorf("%s: %w", action, err)
}
