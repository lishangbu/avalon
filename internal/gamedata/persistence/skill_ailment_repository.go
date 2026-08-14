package persistence

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameskillailment"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/skillailment"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createSkillAilmentOperationID = "game-data.skill-ailment.create"
	updateSkillAilmentOperationID = "game-data.skill-ailment.update"
	deleteSkillAilmentOperationID = "game-data.skill-ailment.delete"
)

// skillAilmentTransactionRepository 隔离技能异常 Writer 的方法集合。
type skillAilmentTransactionRepository struct {
	parent   *Adapters
	client   *avalonent.Client
	executor database.Transaction
}

// Create 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呭垱寤虹ǔ瀹氳韩浠姐€佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillAilmentTransactionRepository) Create(
	ctx context.Context,
	record skillailment.CreateRecord,
) (skillailment.Ailment, error) {
	// 鎽樿鍙寘鍚鎴风鍙噸澶嶆彁浜ょ殑涓氬姟浜嬪疄锛涙湇鍔＄鐢熸垚鐨?Identifier 鍜屽垵濮嬬増鏈笉鑳藉弬涓庤姹傝韩浠姐€?
	digest, err := idempotency.Digest(struct {
		Code    string
		Name    string
		Enabled bool
	}{
		Code:    record.Ailment.Code,
		Name:    record.Ailment.Name,
		Enabled: record.Ailment.Enabled,
	})
	if err != nil {
		return skillailment.Ailment{}, fmt.Errorf("璁＄畻鎶€鑳藉紓甯稿垱寤哄箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: createSkillAilmentOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt,
	}
	created := record.Ailment
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return skillailment.Ailment{}, fmt.Errorf("认领技能异常创建幂等键: %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := w.client.GameSkillAilment.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if err != nil {
		return skillailment.Ailment{}, mapSkillAilmentWriteError(err, "鎻掑叆鎶€鑳藉紓甯?瀹炴椂璧勬枡淇")
	}
	created = skillailment.Ailment{ID: row.ID, Code: row.Code, Name: row.Name, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(
		ctx, executor, record.ActorAccountID, "game-data.skill-ailment.created", "game_skill_ailment",
		created.ID, record.RequestID, record.CreatedAt, nil, &created,
	); err != nil {
		return skillailment.Ailment{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return skillailment.Ailment{}, fmt.Errorf("淇濆瓨鎶€鑳藉紓甯稿垱寤哄箓绛夌粨鏋? %w", err)
	}
	return created, nil
}

// GetSkillAilment 通过稳定 Identifier 读取当前实时技能异常资料。
func (s *Adapters) GetSkillAilment(ctx context.Context, ailmentID snowflake.ID) (skillailment.Ailment, error) {
	row, err := s.pool.Client(ctx).GameSkillAilment.Query().Where(gameskillailment.IDEQ(ailmentID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skillailment.Ailment{}, skillailment.ErrSkillAilmentNotFound
	}
	if err != nil {
		return skillailment.Ailment{}, fmt.Errorf("鏌ヨ鎶€鑳藉紓甯歌祫鏂? %w", err)
	}
	return skillAilmentFromValues(databaseIdentifier(row.ID), row.Code, row.Name, row.Enabled, row.Version), nil
}

// ListSkillAilments 使用 Ent 谓词、稳定排序和页码分页返回技能异常资料。
func (s *Adapters) ListSkillAilments(ctx context.Context, query skillailment.ListQuery) (skillailment.Page, error) {
	filters := skillAilmentPredicates(query)
	client := s.pool.Client(ctx)
	total, err := client.GameSkillAilment.Query().Where(filters...).Count(ctx)
	if err != nil {
		return skillailment.Page{}, fmt.Errorf("缁熻鎶€鑳藉紓甯歌祫鏂? %w", err)
	}
	rows, err := client.GameSkillAilment.Query().Where(filters...).Order(skillAilmentOrder(query.Sort)...).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return skillailment.Page{}, fmt.Errorf("查询技能异常资料页: %w", err)
	}
	items := make([]skillailment.Ailment, len(rows))
	for index, row := range rows {
		items[index] = skillAilmentFromValues(databaseIdentifier(row.ID), row.Code, row.Name, row.Enabled, row.Version)
	}
	return skillailment.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// skillAilmentPredicates 将技能异常列表条件转换为 Ent 谓词。
func skillAilmentPredicates(query skillailment.ListQuery) []predicate.GameSkillAilment {
	filters := make([]predicate.GameSkillAilment, 0, 4)
	if query.Q != "" {
		filters = append(filters, gameskillailment.Or(gameskillailment.CodeContainsFold(query.Q), gameskillailment.NameContainsFold(query.Q)))
	}
	if query.Code != "" {
		filters = append(filters, gameskillailment.CodeContainsFold(query.Code))
	}
	if query.Name != "" {
		filters = append(filters, gameskillailment.NameContainsFold(query.Name))
	}
	if query.Enabled != nil {
		filters = append(filters, gameskillailment.EnabledEQ(*query.Enabled))
	}
	return filters
}

// skillAilmentOrder 返回稳定排序，并使用 Identifier 打破同值记录的平局。
func skillAilmentOrder(value skillailment.Sort) []gameskillailment.OrderOption {
	id := gameskillailment.ByID(sql.OrderAsc())
	switch value {
	case skillailment.SortCodeDescending:
		return []gameskillailment.OrderOption{gameskillailment.ByCode(sql.OrderDesc()), id}
	case skillailment.SortNameAscending:
		return []gameskillailment.OrderOption{gameskillailment.ByName(sql.OrderAsc()), id}
	case skillailment.SortNameDescending:
		return []gameskillailment.OrderOption{gameskillailment.ByName(sql.OrderDesc()), id}
	default:
		return []gameskillailment.OrderOption{gameskillailment.ByCode(sql.OrderAsc()), id}
	}
}

// Update 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呮洿鏂?瀹炴椂璧勬枡淇銆佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *skillAilmentTransactionRepository) Update(
	ctx context.Context,
	record skillailment.UpdateRecord,
) (skillailment.Ailment, error) {
	digest, err := idempotency.Digest(struct {
		Ailment         skillailment.Ailment
		ExpectedVersion int64
	}{record.Ailment, record.ExpectedVersion})
	if err != nil {
		return skillailment.Ailment{}, fmt.Errorf("璁＄畻鎶€鑳藉紓甯告洿鏂板箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: updateSkillAilmentOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt,
	}
	updated := record.Ailment
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return skillailment.Ailment{}, fmt.Errorf("认领技能异常更新幂等键: %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameSkillAilment.Query().Where(gameskillailment.IDEQ(updated.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skillailment.Ailment{}, skillailment.ErrSkillAilmentNotFound
	}
	if err != nil {
		return skillailment.Ailment{}, fmt.Errorf("閿佸畾寰呮洿鏂版妧鑳藉紓甯歌祫鏂? %w", err)
	}
	current := skillailment.Ailment{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return skillailment.Ailment{}, skillailment.ErrSkillAilmentVersionConflict
	}
	row, err := w.client.GameSkillAilment.UpdateOne(currentRow).Where(gameskillailment.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) {
		return skillailment.Ailment{}, skillailment.ErrSkillAilmentVersionConflict
	}
	if err != nil {
		return skillailment.Ailment{}, mapSkillAilmentWriteError(err, "鏇存柊鎶€鑳藉紓甯?瀹炴椂璧勬枡淇")
	}
	updated = skillailment.Ailment{ID: row.ID, Code: row.Code, Name: row.Name, Enabled: row.Enabled, Version: row.Version}
	if err := w.parent.recordGameDataAudit(
		ctx, executor, record.ActorAccountID, "game-data.skill-ailment.updated", "game_skill_ailment",
		updated.ID, record.RequestID, record.UpdatedAt, &current, &updated,
	); err != nil {
		return skillailment.Ailment{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return skillailment.Ailment{}, fmt.Errorf("淇濆瓨鎶€鑳藉紓甯告洿鏂板箓绛夌粨鏋? %w", err)
	}
	return updated, nil
}

// Delete 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呯鐢?瀹炴椂璧勬枡淇銆佽褰曞璁″苟淇濆瓨骞傜瓑鍝嶅簲銆?
func (w *skillAilmentTransactionRepository) Disable(ctx context.Context, record skillailment.DisableRecord) error {
	digest, err := idempotency.Digest(struct {
		AilmentID       snowflake.ID
		ExpectedVersion int64
	}{record.AilmentID, record.ExpectedVersion})
	if err != nil {
		return fmt.Errorf("璁＄畻鎶€鑳藉紓甯哥鐢ㄥ箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: deleteSkillAilmentOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt,
	}
	executor := w.executor
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	response := struct {
		Disabled bool `json:"disabled"`
	}{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil {
		return fmt.Errorf("认领技能异常禁用幂等键: %w", err)
	}
	if replay {
		return nil
	}
	currentRow, err := w.client.GameSkillAilment.Query().Where(gameskillailment.IDEQ(record.AilmentID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return skillailment.ErrSkillAilmentNotFound
	}
	if err != nil {
		return fmt.Errorf("閿佸畾寰呯鐢ㄦ妧鑳藉紓甯歌祫鏂? %w", err)
	}
	current := skillailment.Ailment{ID: currentRow.ID, Code: currentRow.Code, Name: currentRow.Name, Enabled: currentRow.Enabled, Version: currentRow.Version}
	if current.Version != record.ExpectedVersion {
		return skillailment.ErrSkillAilmentVersionConflict
	}
	if _, err := w.client.GameSkillAilment.UpdateOne(currentRow).Where(gameskillailment.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return skillailment.ErrSkillAilmentVersionConflict
	} else if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23503" {
			return skillailment.ErrSkillAilmentReferenced
		}
		return fmt.Errorf("绂佺敤鎶€鑳藉紓甯?瀹炴椂璧勬枡淇: %w", err)
	}
	if err := w.parent.recordGameDataAudit(
		ctx, executor, record.ActorAccountID, "game-data.skill-ailment.disabled", "game_skill_ailment",
		current.ID, record.RequestID, record.DisabledAt, &current, nil,
	); err != nil {
		return err
	}
	response.Disabled = true
	if err := idempotency.Complete(ctx, writer, request, response); err != nil {
		return fmt.Errorf("淇濆瓨鎶€鑳藉紓甯哥鐢ㄥ箓绛夌粨鏋? %w", err)
	}
	return nil
}

func mapSkillAilmentWriteError(err error, action string) error {
	var databaseError *pgconn.PgError
	if errors.As(err, &databaseError) && databaseError.Code == "23505" {
		return skillailment.ErrSkillAilmentCodeConflict
	}
	return fmt.Errorf("%s: %w", action, err)
}
