package persistence

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameskilldamageclass"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/skilldamageclass"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createSkillDamageClassOperationID = "game-data.skill-damage-class.create"
	updateSkillDamageClassOperationID = "game-data.skill-damage-class.update"
	deleteSkillDamageClassOperationID = "game-data.skill-damage-class.delete"
)

// skillDamageClassTransactionRepository 隔离技能伤害分类 Writer 的方法集合。
type skillDamageClassTransactionRepository struct {
	parent   *Adapters
	client   *avalonent.Client
	executor database.Transaction
}

// Create 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呭垱寤虹ǔ瀹氳韩浠姐€佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillDamageClassTransactionRepository) Create(
	ctx context.Context,
	record skilldamageclass.CreateRecord,
) (skilldamageclass.DamageClass, error) {
	digest, err := idempotency.Digest(struct {
		Code        string
		Name        string
		Description *string
		SortOrder   int32
		Enabled     bool
	}{
		record.DamageClass.Code,
		record.DamageClass.Name,
		record.DamageClass.Description,
		record.DamageClass.SortOrder,
		record.DamageClass.Enabled,
	})
	if err != nil {
		return skilldamageclass.DamageClass{}, fmt.Errorf("璁＄畻鎶€鑳戒激瀹冲垎绫诲垱寤哄箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID,
		OperationID:    createSkillDamageClassOperationID,
		Key:            record.IdempotencyKey,
		RequestDigest:  digest,
		CreatedAt:      record.CreatedAt,
	}
	created := record.DamageClass
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return skilldamageclass.DamageClass{}, fmt.Errorf("认领技能伤害分类创建幂等键: %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := w.client.GameSkillDamageClass.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetNillableDescription(created.Description).SetSortOrder(created.SortOrder).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23505" {
			return skilldamageclass.DamageClass{}, skilldamageclass.ErrSkillDamageClassCodeConflict
		}
		return skilldamageclass.DamageClass{}, fmt.Errorf("鎻掑叆鎶€鑳戒激瀹冲垎绫?瀹炴椂璧勬枡淇: %w", err)
	}
	created = skilldamageclass.DamageClass{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(
		ctx, executor, record.ActorAccountID, "game-data.skill-damage-class.created",
		"game_skill_damage_class", created.ID, record.RequestID, record.CreatedAt, nil, &created,
	); err != nil {
		return skilldamageclass.DamageClass{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return skilldamageclass.DamageClass{}, fmt.Errorf("淇濆瓨鎶€鑳戒激瀹冲垎绫诲垱寤哄箓绛夌粨鏋? %w", err)
	}
	return created, nil
}

// GetSkillDamageClass 璇诲彇褰撳墠瀹炴椂璧勬枡涓寚瀹氱ǔ瀹氳韩浠界殑鎶€鑳戒激瀹冲垎绫汇€?
func (s *Adapters) GetSkillDamageClass(
	ctx context.Context,
	damageClassID snowflake.ID,
) (skilldamageclass.DamageClass, error) {
	row, err := s.pool.Client(ctx).GameSkillDamageClass.Query().Where(gameskilldamageclass.IDEQ(damageClassID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skilldamageclass.DamageClass{}, skilldamageclass.ErrSkillDamageClassNotFound
	}
	if err != nil {
		return skilldamageclass.DamageClass{}, fmt.Errorf("鏌ヨ鎶€鑳戒激瀹冲垎绫? %w", err)
	}
	var description *string
	if row.Description != nil {
		description = row.Description
	}
	return skillDamageClassFromValues(databaseIdentifier(row.ID), row.Code, row.Name, databaseText(description), row.SortOrder, row.Enabled, row.Version), nil
}

// ListSkillDamageClasses 杩斿洖褰撳墠瀹炴椂璧勬枡涓鍚堟樉寮忕瓫閫夋潯浠剁殑鎶€鑳戒激瀹冲垎绫婚〉銆?
func (s *Adapters) ListSkillDamageClasses(
	ctx context.Context,
	query skilldamageclass.ListQuery,
) (skilldamageclass.Page, error) {
	filters := make([]predicate.GameSkillDamageClass, 0, 5)
	if query.Q != "" {
		filters = append(filters, gameskilldamageclass.Or(gameskilldamageclass.CodeContainsFold(query.Q), gameskilldamageclass.NameContainsFold(query.Q), gameskilldamageclass.DescriptionContainsFold(query.Q)))
	}
	if query.Code != "" {
		filters = append(filters, gameskilldamageclass.CodeContainsFold(query.Code))
	}
	if query.Name != "" {
		filters = append(filters, gameskilldamageclass.NameContainsFold(query.Name))
	}
	if query.Description != "" {
		filters = append(filters, gameskilldamageclass.DescriptionContainsFold(query.Description))
	}
	if query.SortOrder != nil {
		filters = append(filters, gameskilldamageclass.SortOrderEQ(*query.SortOrder))
	}
	if query.Enabled != nil {
		filters = append(filters, gameskilldamageclass.EnabledEQ(*query.Enabled))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameSkillDamageClass.Query().Where(filters...).Count(ctx)
	if err != nil {
		return skilldamageclass.Page{}, fmt.Errorf("缁熻鎶€鑳戒激瀹冲垎绫? %w", err)
	}
	order := []gameskilldamageclass.OrderOption{gameskilldamageclass.BySortOrder(), gameskilldamageclass.ByCode()}
	switch query.Sort {
	case skilldamageclass.SortCodeDescending:
		order = []gameskilldamageclass.OrderOption{gameskilldamageclass.ByCode(sql.OrderDesc()), gameskilldamageclass.ByID(sql.OrderDesc())}
	case skilldamageclass.SortNameAscending:
		order = []gameskilldamageclass.OrderOption{gameskilldamageclass.ByName(), gameskilldamageclass.ByID()}
	case skilldamageclass.SortNameDescending:
		order = []gameskilldamageclass.OrderOption{gameskilldamageclass.ByName(sql.OrderDesc()), gameskilldamageclass.ByID(sql.OrderDesc())}
	case skilldamageclass.SortOrderDescending:
		order = []gameskilldamageclass.OrderOption{gameskilldamageclass.BySortOrder(sql.OrderDesc()), gameskilldamageclass.ByID(sql.OrderDesc())}
	case skilldamageclass.SortOrderAscending:
		order = []gameskilldamageclass.OrderOption{gameskilldamageclass.BySortOrder(), gameskilldamageclass.ByID()}
	}
	rows, err := client.GameSkillDamageClass.Query().Where(filters...).Order(order...).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return skilldamageclass.Page{}, fmt.Errorf("查询技能伤害分类页: %w", err)
	}
	items := make([]skilldamageclass.DamageClass, len(rows))
	for index, row := range rows {
		var description *string
		if row.Description != nil {
			description = row.Description
		}
		items[index] = skillDamageClassFromValues(databaseIdentifier(row.ID), row.Code, row.Name, databaseText(description), row.SortOrder, row.Enabled, row.Version)
	}
	return skilldamageclass.Page{
		Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize,
	}, nil
}

// Update 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呮洿鏂?瀹炴椂璧勬枡淇銆佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillDamageClassTransactionRepository) Update(
	ctx context.Context,
	record skilldamageclass.UpdateRecord,
) (skilldamageclass.DamageClass, error) {
	digest, err := idempotency.Digest(struct {
		DamageClassID        snowflake.ID
		ExpectedVersion      int64
		Code                 string
		Name                 string
		DescriptionSpecified bool
		Description          *string
		SortOrder            int32
		Enabled              bool
	}{
		record.DamageClass.ID,
		record.ExpectedVersion,
		record.DamageClass.Code,
		record.DamageClass.Name,
		record.Description.Specified,
		record.Description.Value,
		record.DamageClass.SortOrder,
		record.DamageClass.Enabled,
	})
	if err != nil {
		return skilldamageclass.DamageClass{}, fmt.Errorf("璁＄畻鎶€鑳戒激瀹冲垎绫绘洿鏂板箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID,
		OperationID:    updateSkillDamageClassOperationID,
		Key:            record.IdempotencyKey,
		RequestDigest:  digest,
		CreatedAt:      record.UpdatedAt,
	}
	updated := record.DamageClass
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return skilldamageclass.DamageClass{}, fmt.Errorf("认领技能伤害分类更新幂等键: %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameSkillDamageClass.Query().Where(gameskilldamageclass.IDEQ(record.DamageClass.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skilldamageclass.DamageClass{}, skilldamageclass.ErrSkillDamageClassNotFound
	}
	if err != nil {
		return skilldamageclass.DamageClass{}, fmt.Errorf("閿佸畾寰呮洿鏂版妧鑳戒激瀹冲垎绫? %w", err)
	}
	current := skilldamageclass.DamageClass{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, Description: currentRow.Description, SortOrder: currentRow.SortOrder, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return skilldamageclass.DamageClass{}, skilldamageclass.ErrSkillDamageClassVersionConflict
	}
	builder := w.client.GameSkillDamageClass.UpdateOne(currentRow).Where(gameskilldamageclass.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetSortOrder(updated.SortOrder).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC())
	if record.Description.Specified {
		if record.Description.Value == nil {
			builder.ClearDescription()
		} else {
			builder.SetDescription(*record.Description.Value)
		}
	}
	row, err := builder.Save(ctx)
	if avalonent.IsNotFound(err) {
		return skilldamageclass.DamageClass{}, skilldamageclass.ErrSkillDamageClassVersionConflict
	}
	if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23505" {
			return skilldamageclass.DamageClass{}, skilldamageclass.ErrSkillDamageClassCodeConflict
		}
		return skilldamageclass.DamageClass{}, fmt.Errorf("鏇存柊鎶€鑳戒激瀹冲垎绫?瀹炴椂璧勬枡淇: %w", err)
	}
	updated = skilldamageclass.DamageClass{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, SortOrder: row.SortOrder, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(
		ctx, executor, record.ActorAccountID, "game-data.skill-damage-class.updated",
		"game_skill_damage_class", updated.ID, record.RequestID, record.UpdatedAt, &current, &updated,
	); err != nil {
		return skilldamageclass.DamageClass{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return skilldamageclass.DamageClass{}, fmt.Errorf("淇濆瓨鎶€鑳戒激瀹冲垎绫绘洿鏂板箓绛夌粨鏋? %w", err)
	}
	return updated, nil
}

// Delete 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呯鐢?瀹炴椂璧勬枡淇銆佽褰曞璁″苟淇濆瓨骞傜瓑鍝嶅簲銆?
func (w *skillDamageClassTransactionRepository) Disable(
	ctx context.Context,
	record skilldamageclass.DisableRecord,
) error {
	digest, err := idempotency.Digest(struct {
		DamageClassID   snowflake.ID
		ExpectedVersion int64
	}{record.DamageClassID, record.ExpectedVersion})
	if err != nil {
		return fmt.Errorf("璁＄畻鎶€鑳戒激瀹冲垎绫荤鐢ㄥ箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID,
		OperationID:    deleteSkillDamageClassOperationID,
		Key:            record.IdempotencyKey,
		RequestDigest:  digest,
		CreatedAt:      record.DisabledAt,
	}
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	response := struct {
		Disabled bool `json:"disabled"`
	}{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil {
		return fmt.Errorf("认领技能伤害分类禁用幂等键: %w", err)
	}
	if replay {
		return nil
	}
	currentRow, err := w.client.GameSkillDamageClass.Query().Where(gameskilldamageclass.IDEQ(record.DamageClassID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skilldamageclass.ErrSkillDamageClassNotFound
	}
	if err != nil {
		return fmt.Errorf("閿佸畾寰呯鐢ㄦ妧鑳戒激瀹冲垎绫? %w", err)
	}
	current := skilldamageclass.DamageClass{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, Description: currentRow.Description, SortOrder: currentRow.SortOrder, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return skilldamageclass.ErrSkillDamageClassVersionConflict
	}
	if _, err := w.client.GameSkillDamageClass.UpdateOne(currentRow).Where(gameskilldamageclass.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return skilldamageclass.ErrSkillDamageClassVersionConflict
	} else if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23503" {
			return skilldamageclass.ErrSkillDamageClassReferenced
		}
		return fmt.Errorf("绂佺敤鎶€鑳戒激瀹冲垎绫?瀹炴椂璧勬枡淇: %w", err)
	}
	if err := w.parent.recordGameDataAudit(
		ctx, executor, record.ActorAccountID, "game-data.skill-damage-class.disabled",
		"game_skill_damage_class", current.ID, record.RequestID, record.DisabledAt, &current, nil,
	); err != nil {
		return err
	}
	response.Disabled = true
	if err := idempotency.Complete(ctx, writer, request, response); err != nil {
		return fmt.Errorf("淇濆瓨鎶€鑳戒激瀹冲垎绫荤鐢ㄥ箓绛夌粨鏋? %w", err)
	}
	return nil
}
