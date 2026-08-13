package store

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameskillstatchange"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/skillstatchange"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createSkillStatChangeOperationID  = "game-data.skill-stat-change.create"
	updateSkillStatChangeOperationID  = "game-data.skill-stat-change.update"
	disableSkillStatChangeOperationID = "game-data.skill-stat-change.disable"
)

type skillStatChangeTransactionStore struct {
	parent   *Store
	client   *avalonent.Client
	executor database.Transaction
}

// Create 鍦ㄥ悓涓€浜嬪姟涓啓鍏ョǔ瀹氳韩浠姐€佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillStatChangeTransactionStore) Create(ctx context.Context, record skillstatchange.CreateRecord) (skillstatchange.Change, error) {
	digest, err := idempotency.Digest(struct {
		SkillID     snowflake.ID
		StatID      snowflake.ID
		ChangeValue int32
	}{record.Change.SkillID, record.Change.StatID, record.Change.ChangeValue})
	if err != nil {
		return skillstatchange.Change{}, fmt.Errorf("璁＄畻鎶€鑳芥暟鍊煎彉鍖栧垱寤哄箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: createSkillStatChangeOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt}
	created := record.Change
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return skillstatchange.Change{}, fmt.Errorf("认领技能数值变化创建幂等键: %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := w.client.GameSkillStatChange.Create().SetID(created.ID).SetSkillID(created.SkillID).SetStatID(created.StatID).SetChangeValue(created.ChangeValue).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if err != nil {
		return skillstatchange.Change{}, mapSkillStatChangeWriteError(err, "鎻掑叆鎶€鑳芥暟鍊煎彉鍖?瀹炴椂璧勬枡淇")
	}
	created = skillstatchange.Change{ID: row.ID, SkillID: snowflake.ID(row.SkillID), StatID: snowflake.ID(row.StatID), ChangeValue: row.ChangeValue, Version: row.Version}
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.skill-stat-change.created",
		"game_skill_stat_change", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return skillstatchange.Change{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return skillstatchange.Change{}, fmt.Errorf("淇濆瓨鎶€鑳芥暟鍊煎彉鍖栧垱寤哄箓绛夌粨鏋? %w", err)
	}
	return created, nil
}

// GetSkillStatChange 璇诲彇瀹炴椂璧勬枡涓寚瀹氳褰曘€?
func (s *Store) GetSkillStatChange(ctx context.Context, changeID snowflake.ID) (skillstatchange.Change, error) {
	row, err := s.pool.Client(ctx).GameSkillStatChange.Query().Where(gameskillstatchange.IDEQ(changeID), gameskillstatchange.DisabledAtIsNil()).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skillstatchange.Change{}, skillstatchange.ErrSkillStatChangeNotFound
	}
	if err != nil {
		return skillstatchange.Change{}, fmt.Errorf("鏌ヨ鎶€鑳芥暟鍊煎彉鍖? %w", err)
	}
	return skillstatchange.Change{ID: row.ID, SkillID: snowflake.ID(row.SkillID), StatID: snowflake.ID(row.StatID), ChangeValue: row.ChangeValue, Version: row.Version}, nil
}

// ListSkillStatChanges 杩斿洖瀹炴椂璧勬枡涓鍚堟樉寮忕瓫閫夋潯浠剁殑璁板綍椤点€?
func (s *Store) ListSkillStatChanges(ctx context.Context, query skillstatchange.ListQuery) (skillstatchange.Page, error) {
	client := s.pool.Client(ctx)
	filters := []predicate.GameSkillStatChange{gameskillstatchange.DisabledAtIsNil()}
	if query.SkillID != nil {
		filters = append(filters, gameskillstatchange.SkillIDEQ(*query.SkillID))
	}
	if query.StatID != nil {
		filters = append(filters, gameskillstatchange.StatIDEQ(*query.StatID))
	}
	if query.ChangeValue != nil {
		filters = append(filters, gameskillstatchange.ChangeValueEQ(*query.ChangeValue))
	}
	total, err := client.GameSkillStatChange.Query().Where(filters...).Count(ctx)
	if err != nil {
		return skillstatchange.Page{}, fmt.Errorf("缁熻鎶€鑳芥暟鍊煎彉鍖? %w", err)
	}
	order := gameskillstatchange.BySkillID(sql.OrderAsc())
	if query.Sort == skillstatchange.SortSkillDescending {
		order = gameskillstatchange.BySkillID(sql.OrderDesc())
	}
	rows, err := client.GameSkillStatChange.Query().Where(filters...).Order(order, gameskillstatchange.ByID(sql.OrderAsc())).Offset(int(query.Page-1) * int(query.PageSize)).Limit(int(query.PageSize)).All(ctx)
	if err != nil {
		return skillstatchange.Page{}, fmt.Errorf("查询技能数值变化页: %w", err)
	}
	items := make([]skillstatchange.Change, len(rows))
	for index, row := range rows {
		items[index] = skillstatchange.Change{ID: row.ID, SkillID: snowflake.ID(row.SkillID), StatID: snowflake.ID(row.StatID), ChangeValue: row.ChangeValue, Version: row.Version}
	}
	return skillstatchange.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// Update 鍦ㄥ悓涓€浜嬪姟涓墽琛屼箰瑙傛洿鏂般€佸璁″拰骞傜瓑鍝嶅簲淇濆瓨銆?
func (w *skillStatChangeTransactionStore) Update(ctx context.Context, record skillstatchange.UpdateRecord) (skillstatchange.Change, error) {
	digest, err := idempotency.Digest(struct {
		Change          skillstatchange.Change
		ExpectedVersion int64
	}{record.Change, record.ExpectedVersion})
	if err != nil {
		return skillstatchange.Change{}, fmt.Errorf("璁＄畻鎶€鑳芥暟鍊煎彉鍖栨洿鏂板箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: updateSkillStatChangeOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt}
	updated := record.Change
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return skillstatchange.Change{}, fmt.Errorf("认领技能数值变化更新幂等键: %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameSkillStatChange.Query().Where(gameskillstatchange.IDEQ(updated.ID), gameskillstatchange.DisabledAtIsNil()).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skillstatchange.Change{}, skillstatchange.ErrSkillStatChangeNotFound
	}
	if err != nil {
		return skillstatchange.Change{}, fmt.Errorf("閿佸畾鎶€鑳芥暟鍊煎彉鍖? %w", err)
	}
	current := skillstatchange.Change{ID: currentRow.ID, SkillID: snowflake.ID(currentRow.SkillID), StatID: snowflake.ID(currentRow.StatID), ChangeValue: currentRow.ChangeValue, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return skillstatchange.Change{}, skillstatchange.ErrSkillStatChangeVersionConflict
	}
	row, err := w.client.GameSkillStatChange.UpdateOne(currentRow).Where(gameskillstatchange.VersionEQ(record.ExpectedVersion), gameskillstatchange.DisabledAtIsNil()).SetSkillID(updated.SkillID).SetStatID(updated.StatID).SetChangeValue(updated.ChangeValue).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) {
		return skillstatchange.Change{}, skillstatchange.ErrSkillStatChangeVersionConflict
	}
	if err != nil {
		return skillstatchange.Change{}, mapSkillStatChangeWriteError(err, "鏇存柊鎶€鑳芥暟鍊煎彉鍖?瀹炴椂璧勬枡淇")
	}
	updated = skillstatchange.Change{ID: row.ID, SkillID: snowflake.ID(row.SkillID), StatID: snowflake.ID(row.StatID), ChangeValue: row.ChangeValue, Version: row.Version}
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.skill-stat-change.updated",
		"game_skill_stat_change", updated.ID, record.RequestID, record.UpdatedAt, &current, &updated); err != nil {
		return skillstatchange.Change{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return skillstatchange.Change{}, fmt.Errorf("淇濆瓨鎶€鑳芥暟鍊煎彉鍖栨洿鏂板箓绛夌粨鏋? %w", err)
	}
	return updated, nil
}

// Disable 鍦ㄥ悓涓€浜嬪姟涓鐢ㄤ慨璁€佽褰曞璁″苟淇濆瓨骞傜瓑鍝嶅簲銆?
func (w *skillStatChangeTransactionStore) Disable(ctx context.Context, record skillstatchange.DisableRecord) error {
	digest, err := idempotency.Digest(struct {
		ChangeID        snowflake.ID
		ExpectedVersion int64
	}{record.ChangeID, record.ExpectedVersion})
	if err != nil {
		return fmt.Errorf("璁＄畻鎶€鑳芥暟鍊煎彉鍖栫鐢ㄥ箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: disableSkillStatChangeOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt}
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	response := struct {
		Disabled bool `json:"disabled"`
	}{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil {
		return fmt.Errorf("认领技能数值变化禁用幂等键: %w", err)
	}
	if replay {
		return nil
	}
	currentRow, err := w.client.GameSkillStatChange.Query().Where(gameskillstatchange.IDEQ(record.ChangeID), gameskillstatchange.DisabledAtIsNil()).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skillstatchange.ErrSkillStatChangeNotFound
	}
	if err != nil {
		return fmt.Errorf("閿佸畾寰呯鐢ㄦ妧鑳芥暟鍊煎彉鍖? %w", err)
	}
	current := skillstatchange.Change{ID: currentRow.ID, SkillID: snowflake.ID(currentRow.SkillID), StatID: snowflake.ID(currentRow.StatID), ChangeValue: currentRow.ChangeValue, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return skillstatchange.ErrSkillStatChangeVersionConflict
	}
	if _, err := w.client.GameSkillStatChange.UpdateOne(currentRow).Where(gameskillstatchange.VersionEQ(record.ExpectedVersion), gameskillstatchange.DisabledAtIsNil()).SetDisabledAt(record.DisabledAt.UTC()).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("绂佺敤鎶€鑳芥暟鍊煎彉鍖?瀹炴椂璧勬枡淇: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.skill-stat-change.disabled",
		"game_skill_stat_change", current.ID, record.RequestID, record.DisabledAt, &current, nil); err != nil {
		return err
	}
	response.Disabled = true
	return idempotency.Complete(ctx, writer, request, response)
}

func mapSkillStatChangeWriteError(err error, action string) error {
	var databaseError *pgconn.PgError
	if errors.As(err, &databaseError) {
		switch databaseError.Code {
		case "23505":
			return skillstatchange.ErrSkillStatChangeConflict
		case "23503":
			return skillstatchange.ErrSkillStatChangeDependencyNotFound
		case "23514":
			return skillstatchange.ErrInvalidSkillStatChange
		}
	}
	return fmt.Errorf("%s: %w", action, err)
}
