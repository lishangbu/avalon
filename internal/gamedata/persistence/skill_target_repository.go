package persistence

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameskilltarget"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/skilltarget"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createSkillTargetOperationID = "game-data.skill-target.create"
	updateSkillTargetOperationID = "game-data.skill-target.update"
	deleteSkillTargetOperationID = "game-data.skill-target.delete"
)

type skillTargetTransactionRepository struct {
	parent   *Adapters
	client   *avalonent.Client
	executor database.Transaction
}

// Create 鍦ㄥ悓涓€浜嬪姟涓啓鍏ユ妧鑳界洰鏍囪韩浠姐€佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillTargetTransactionRepository) Create(ctx context.Context, record skilltarget.CreateRecord) (skilltarget.Target, error) {
	// 鎽樿鍙寘鍚鎴风鍙噸澶嶆彁浜ょ殑浜嬪疄锛涙湇鍔＄姣忔灏濊瘯鐢熸垚鐨勬柊 Identifier 涓嶅緱鏀瑰彉骞傜瓑璇锋眰韬唤銆?
	digest, err := idempotency.Digest(struct {
		Code        string
		Name        string
		Description *string
		Enabled     bool
	}{
		Code:        record.Target.Code,
		Name:        record.Target.Name,
		Description: record.Target.Description,
		Enabled:     record.Target.Enabled,
	})
	if err != nil {
		return skilltarget.Target{}, fmt.Errorf("璁＄畻鎶€鑳界洰鏍囧垱寤哄箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: createSkillTargetOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt}
	created := record.Target
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return skilltarget.Target{}, fmt.Errorf("认领技能目标创建幂等键: %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := w.client.GameSkillTarget.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetNillableDescription(created.Description).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if err != nil {
		return skilltarget.Target{}, mapSkillTargetWriteError(err, "鎻掑叆鎶€鑳界洰鏍?瀹炴椂璧勬枡淇")
	}
	created = skilltarget.Target{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.skill-target.created",
		"game_skill_target", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return skilltarget.Target{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return skilltarget.Target{}, fmt.Errorf("淇濆瓨鎶€鑳界洰鏍囧垱寤哄箓绛夌粨鏋? %w", err)
	}
	return created, nil
}

// GetSkillTarget 通过稳定 Identifier 读取当前实时技能目标资料。
func (s *Adapters) GetSkillTarget(ctx context.Context, targetID snowflake.ID) (skilltarget.Target, error) {
	row, err := s.pool.Client(ctx).GameSkillTarget.Query().Where(gameskilltarget.IDEQ(targetID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skilltarget.Target{}, skilltarget.ErrSkillTargetNotFound
	}
	if err != nil {
		return skilltarget.Target{}, fmt.Errorf("鏌ヨ鎶€鑳界洰鏍? %w", err)
	}
	return skillTargetFromEnt(row), nil
}

// ListSkillTargets 使用 Ent 谓词、稳定排序和页码分页返回技能目标资料。
func (s *Adapters) ListSkillTargets(ctx context.Context, query skilltarget.ListQuery) (skilltarget.Page, error) {
	filters := skillTargetPredicates(query)
	client := s.pool.Client(ctx)
	total, err := client.GameSkillTarget.Query().Where(filters...).Count(ctx)
	if err != nil {
		return skilltarget.Page{}, fmt.Errorf("缁熻鎶€鑳界洰鏍? %w", err)
	}
	rows, err := client.GameSkillTarget.Query().Where(filters...).Order(skillTargetOrder(query.Sort)...).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return skilltarget.Page{}, fmt.Errorf("查询技能目标页: %w", err)
	}
	items := make([]skilltarget.Target, len(rows))
	for index, row := range rows {
		items[index] = skillTargetFromEnt(row)
	}
	return skilltarget.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// skillTargetFromEnt 将 Ent 生成实体转换为领域资料，保留可空说明字段语义。
func skillTargetFromEnt(row *avalonent.GameSkillTarget) skilltarget.Target {
	return skilltarget.Target{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
}

// skillTargetPredicates 将技能目标列表筛选条件转换为 Ent 谓词。
func skillTargetPredicates(query skilltarget.ListQuery) []predicate.GameSkillTarget {
	filters := make([]predicate.GameSkillTarget, 0, 5)
	if query.Q != "" {
		filters = append(filters, gameskilltarget.Or(gameskilltarget.CodeContainsFold(query.Q), gameskilltarget.NameContainsFold(query.Q), gameskilltarget.DescriptionContainsFold(query.Q)))
	}
	if query.Code != "" {
		filters = append(filters, gameskilltarget.CodeContainsFold(query.Code))
	}
	if query.Name != "" {
		filters = append(filters, gameskilltarget.NameContainsFold(query.Name))
	}
	if query.Description != "" {
		filters = append(filters, gameskilltarget.DescriptionContainsFold(query.Description))
	}
	if query.Enabled != nil {
		filters = append(filters, gameskilltarget.EnabledEQ(*query.Enabled))
	}
	return filters
}

// skillTargetOrder 返回技能目标的确定性排序。
func skillTargetOrder(value skilltarget.Sort) []gameskilltarget.OrderOption {
	id := gameskilltarget.ByID(sql.OrderAsc())
	switch value {
	case skilltarget.SortCodeDescending:
		return []gameskilltarget.OrderOption{gameskilltarget.ByCode(sql.OrderDesc()), id}
	case skilltarget.SortNameAscending:
		return []gameskilltarget.OrderOption{gameskilltarget.ByName(sql.OrderAsc()), id}
	case skilltarget.SortNameDescending:
		return []gameskilltarget.OrderOption{gameskilltarget.ByName(sql.OrderDesc()), id}
	default:
		return []gameskilltarget.OrderOption{gameskilltarget.ByCode(sql.OrderAsc()), id}
	}
}

// Update 鍦ㄥ悓涓€浜嬪姟涓墽琛屼箰瑙傛洿鏂般€佸疄鏃惰祫鏂?鐗堟湰鎺ㄨ繘銆佸璁″拰骞傜瓑鍝嶅簲淇濆瓨銆?
func (w *skillTargetTransactionRepository) Update(ctx context.Context, record skilltarget.UpdateRecord) (skilltarget.Target, error) {
	digest, err := idempotency.Digest(struct {
		Target          skilltarget.Target
		Description     skilltarget.DescriptionChange
		ExpectedVersion int64
	}{record.Target, record.Description, record.ExpectedVersion})
	if err != nil {
		return skilltarget.Target{}, fmt.Errorf("璁＄畻鎶€鑳界洰鏍囨洿鏂板箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: updateSkillTargetOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt}
	updated := record.Target
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return skilltarget.Target{}, fmt.Errorf("认领技能目标更新幂等键: %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameSkillTarget.Query().Where(gameskilltarget.IDEQ(updated.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skilltarget.Target{}, skilltarget.ErrSkillTargetNotFound
	}
	if err != nil {
		return skilltarget.Target{}, fmt.Errorf("閿佸畾寰呮洿鏂版妧鑳界洰鏍? %w", err)
	}
	current := skilltarget.Target{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, Description: currentRow.Description, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return skilltarget.Target{}, skilltarget.ErrSkillTargetVersionConflict
	}
	builder := w.client.GameSkillTarget.UpdateOne(currentRow).Where(gameskilltarget.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC())
	if record.Description.Specified {
		if record.Description.Value == nil {
			builder.ClearDescription()
		} else {
			builder.SetDescription(*record.Description.Value)
		}
	}
	row, err := builder.Save(ctx)
	if avalonent.IsNotFound(err) {
		return skilltarget.Target{}, skilltarget.ErrSkillTargetVersionConflict
	}
	if err != nil {
		return skilltarget.Target{}, mapSkillTargetWriteError(err, "鏇存柊鎶€鑳界洰鏍?瀹炴椂璧勬枡淇")
	}
	updated = skilltarget.Target{ID: row.ID, Code: row.Code, Name: row.Name, Description: row.Description, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.skill-target.updated",
		"game_skill_target", updated.ID, record.RequestID, record.UpdatedAt, &current, &updated); err != nil {
		return skilltarget.Target{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return skilltarget.Target{}, fmt.Errorf("淇濆瓨鎶€鑳界洰鏍囨洿鏂板箓绛夌粨鏋? %w", err)
	}
	return updated, nil
}

// Delete 鍦ㄥ悓涓€浜嬪姟涓鐢ㄦ湭琚紩鐢ㄧ殑 瀹炴椂璧勬枡淇骞朵繚瀛樺璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillTargetTransactionRepository) Disable(ctx context.Context, record skilltarget.DisableRecord) error {
	digest, err := idempotency.Digest(struct {
		TargetID        snowflake.ID
		ExpectedVersion int64
	}{record.TargetID, record.ExpectedVersion})
	if err != nil {
		return fmt.Errorf("璁＄畻鎶€鑳界洰鏍囩鐢ㄥ箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: deleteSkillTargetOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt}
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	response := struct {
		Disabled bool `json:"disabled"`
	}{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil {
		return fmt.Errorf("认领技能目标禁用幂等键: %w", err)
	}
	if replay {
		return nil
	}
	currentRow, err := w.client.GameSkillTarget.Query().Where(gameskilltarget.IDEQ(record.TargetID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skilltarget.ErrSkillTargetNotFound
	}
	if err != nil {
		return fmt.Errorf("閿佸畾寰呯鐢ㄦ妧鑳界洰鏍? %w", err)
	}
	current := skilltarget.Target{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, Description: currentRow.Description, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return skilltarget.ErrSkillTargetVersionConflict
	}
	if _, err := w.client.GameSkillTarget.UpdateOne(currentRow).Where(gameskilltarget.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return skilltarget.ErrSkillTargetVersionConflict
	} else if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23503" {
			return skilltarget.ErrSkillTargetReferenced
		}
		return fmt.Errorf("绂佺敤鎶€鑳界洰鏍?瀹炴椂璧勬枡淇: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, executor, record.ActorAccountID, "game-data.skill-target.disabled",
		"game_skill_target", current.ID, record.RequestID, record.DisabledAt, &current, nil); err != nil {
		return err
	}
	response.Disabled = true
	return idempotency.Complete(ctx, writer, request, response)
}

func mapSkillTargetWriteError(err error, action string) error {
	var databaseError *pgconn.PgError
	if errors.As(err, &databaseError) && databaseError.Code == "23505" {
		return skilltarget.ErrSkillTargetCodeConflict
	}
	return fmt.Errorf("%s: %w", action, err)
}
