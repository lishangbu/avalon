package persistence

import (
	"context"
	"encoding/json/jsontext"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameability"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/ability"
	"github.com/lishangbu/avalon/internal/gamedata/battlerules"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createAbilityOperationID = "game-data.ability.create"
	updateAbilityOperationID = "game-data.ability.update"
	deleteAbilityOperationID = "game-data.ability.delete"
)

// abilityTransactionRepository 隔离特性资料 Writer 的方法集合，避免不同资料类型的命令签名互相耦合。
type abilityTransactionRepository struct {
	parent   *Adapters
	client   *avalonent.Client
	executor database.Transaction
}

// Create 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呭垱寤虹ǔ瀹氳祫鏂欒韩浠姐€佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *abilityTransactionRepository) Create(ctx context.Context, record ability.CreateRecord) (ability.Ability, error) {
	digest, err := idempotency.Digest(struct {
		Code       string
		Name       string
		MainSeries bool
		Enabled    bool
	}{record.Ability.Code, record.Ability.Name, record.Ability.MainSeries, record.Ability.Enabled})
	if err != nil {
		return ability.Ability{}, fmt.Errorf("璁＄畻鐗规€ц祫鏂欏垱寤哄箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: createAbilityOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt,
	}
	created := record.Ability
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return ability.Ability{}, fmt.Errorf("认领特性资料创建幂等键: %w", err)
	}
	if replay {
		return created, nil
	}
	rulesPayload, err := battlerules.AbilityJSON(created.Rules)
	if err != nil {
		return ability.Ability{}, ability.ErrInvalidAbility
	}
	row, err := w.client.GameAbility.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetMainSeries(created.MainSeries).SetNillableEffect(created.Effect).SetNillableShortEffect(created.ShortEffect).SetNillableIntroduction(created.Introduction).SetRules(jsontext.Value(rulesPayload)).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23505" {
			return ability.Ability{}, ability.ErrAbilityCodeConflict
		}
		return ability.Ability{}, fmt.Errorf("鎻掑叆鐗规€ц祫鏂?瀹炴椂璧勬枡淇: %w", err)
	}
	created, err = abilityFromEnt(row)
	if err != nil {
		return ability.Ability{}, fmt.Errorf("读取新建特性规则: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.ability.created",
		"game_ability", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return ability.Ability{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return ability.Ability{}, fmt.Errorf("淇濆瓨鐗规€ц祫鏂欏垱寤哄箓绛夌粨鏋? %w", err)
	}
	return created, nil
}

// GetAbility 璇诲彇褰撳墠瀹炴椂璧勬枡涓寚瀹氱ǔ瀹氳韩浠界殑鐗规€ц祫鏂欍€?
func (s *Adapters) GetAbility(ctx context.Context, abilityID snowflake.ID) (ability.Ability, error) {
	row, err := s.pool.Client(ctx).GameAbility.Query().Where(gameability.IDEQ(abilityID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return ability.Ability{}, ability.ErrAbilityNotFound
	}
	if err != nil {
		return ability.Ability{}, fmt.Errorf("鏌ヨ鐗规€ц祫鏂? %w", err)
	}
	value, err := abilityFromEnt(row)
	if err != nil {
		return ability.Ability{}, fmt.Errorf("解析特性战斗规则: %w", err)
	}
	return value, nil
}

// ListAbilities 杩斿洖褰撳墠瀹炴椂璧勬枡涓鍚堟樉寮忕瓫閫夋潯浠剁殑绋冲畾璧勬枡椤点€?
func (s *Adapters) ListAbilities(ctx context.Context, query ability.ListQuery) (ability.Page, error) {
	filters := make([]predicate.GameAbility, 0, 5)
	if query.Q != "" {
		filters = append(filters, gameability.Or(gameability.CodeContainsFold(query.Q), gameability.NameContainsFold(query.Q)))
	}
	if query.Code != "" {
		filters = append(filters, gameability.CodeContainsFold(query.Code))
	}
	if query.Name != "" {
		filters = append(filters, gameability.NameContainsFold(query.Name))
	}
	if query.MainSeries != nil {
		filters = append(filters, gameability.MainSeriesEQ(*query.MainSeries))
	}
	if query.Enabled != nil {
		filters = append(filters, gameability.EnabledEQ(*query.Enabled))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameAbility.Query().Where(filters...).Count(ctx)
	if err != nil {
		return ability.Page{}, fmt.Errorf("缁熻鐗规€ц祫鏂? %w", err)
	}
	order := []gameability.OrderOption{gameability.ByCode(), gameability.ByID()}
	switch query.Sort {
	case ability.SortCodeDescending:
		order = []gameability.OrderOption{gameability.ByCode(sql.OrderDesc()), gameability.ByID(sql.OrderDesc())}
	case ability.SortNameAscending:
		order = []gameability.OrderOption{gameability.ByName(), gameability.ByID()}
	case ability.SortNameDescending:
		order = []gameability.OrderOption{gameability.ByName(sql.OrderDesc()), gameability.ByID(sql.OrderDesc())}
	}
	rows, err := client.GameAbility.Query().Where(filters...).Order(order...).Offset(int(query.Page-1) * int(query.PageSize)).Limit(int(query.PageSize)).All(ctx)
	if err != nil {
		return ability.Page{}, fmt.Errorf("查询特性资料页: %w", err)
	}
	items := make([]ability.Ability, len(rows))
	for index, row := range rows {
		items[index], err = abilityFromEnt(row)
		if err != nil {
			return ability.Page{}, fmt.Errorf("解析特性战斗规则: %w", err)
		}
	}
	return ability.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// Update 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呮洿鏂?瀹炴椂璧勬枡淇銆佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *abilityTransactionRepository) Update(ctx context.Context, record ability.UpdateRecord) (ability.Ability, error) {
	digest, err := idempotency.Digest(struct {
		AbilityID       snowflake.ID
		ExpectedVersion int64
		Code            string
		Name            string
		MainSeries      bool
		Enabled         bool
	}{record.Ability.ID, record.ExpectedVersion, record.Ability.Code,
		record.Ability.Name, record.Ability.MainSeries, record.Ability.Enabled})
	if err != nil {
		return ability.Ability{}, fmt.Errorf("璁＄畻鐗规€ц祫鏂欐洿鏂板箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: updateAbilityOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt,
	}
	updated := record.Ability
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return ability.Ability{}, fmt.Errorf("认领特性资料更新幂等键: %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameAbility.Query().Where(gameability.IDEQ(record.Ability.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return ability.Ability{}, ability.ErrAbilityNotFound
	}
	if err != nil {
		return ability.Ability{}, fmt.Errorf("閿佸畾寰呮洿鏂扮壒鎬ц祫鏂? %w", err)
	}
	current, err := abilityFromEnt(currentRow)
	if err != nil {
		return ability.Ability{}, fmt.Errorf("解析待更新特性战斗规则: %w", err)
	}
	if current.Version != record.ExpectedVersion {
		return ability.Ability{}, ability.ErrAbilityVersionConflict
	}
	rulesPayload, err := battlerules.AbilityJSON(updated.Rules)
	if err != nil {
		return ability.Ability{}, ability.ErrInvalidAbility
	}
	builder := w.client.GameAbility.UpdateOne(currentRow).Where(gameability.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetMainSeries(updated.MainSeries).SetRules(jsontext.Value(rulesPayload)).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC())
	if updated.Effect == nil {
		builder.ClearEffect()
	} else {
		builder.SetEffect(*updated.Effect)
	}
	if updated.ShortEffect == nil {
		builder.ClearShortEffect()
	} else {
		builder.SetShortEffect(*updated.ShortEffect)
	}
	if updated.Introduction == nil {
		builder.ClearIntroduction()
	} else {
		builder.SetIntroduction(*updated.Introduction)
	}
	row, err := builder.Save(ctx)
	if avalonent.IsNotFound(err) {
		return ability.Ability{}, ability.ErrAbilityVersionConflict
	}
	if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23505" {
			return ability.Ability{}, ability.ErrAbilityCodeConflict
		}
		return ability.Ability{}, fmt.Errorf("鏇存柊鐗规€ц祫鏂?瀹炴椂璧勬枡淇: %w", err)
	}
	updated, err = abilityFromEnt(row)
	if err != nil {
		return ability.Ability{}, fmt.Errorf("解析已更新特性战斗规则: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.ability.updated",
		"game_ability", updated.ID, record.RequestID, record.UpdatedAt, &current, &updated); err != nil {
		return ability.Ability{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return ability.Ability{}, fmt.Errorf("淇濆瓨鐗规€ц祫鏂欐洿鏂板箓绛夌粨鏋? %w", err)
	}
	return updated, nil
}

// Delete 鍦ㄥ簲鐢ㄦ湇鍔″凡寮€鍚殑浜嬪姟鍐呯鐢?瀹炴椂璧勬枡淇銆佽褰曞璁″苟淇濆瓨骞傜瓑鍝嶅簲銆?
func (w *abilityTransactionRepository) Disable(ctx context.Context, record ability.DisableRecord) error {
	digest, err := idempotency.Digest(struct {
		AbilityID       snowflake.ID
		ExpectedVersion int64
	}{record.AbilityID, record.ExpectedVersion})
	if err != nil {
		return fmt.Errorf("璁＄畻鐗规€ц祫鏂欑鐢ㄥ箓绛夋憳瑕? %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.ActorAccountID, OperationID: deleteAbilityOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt,
	}
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	response := struct {
		Disabled bool `json:"disabled"`
	}{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil {
		return fmt.Errorf("认领特性资料禁用幂等键: %w", err)
	}
	if replay {
		return nil
	}
	currentRow, err := w.client.GameAbility.Query().Where(gameability.IDEQ(record.AbilityID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return ability.ErrAbilityNotFound
	}
	if err != nil {
		return fmt.Errorf("閿佸畾寰呯鐢ㄧ壒鎬ц祫鏂? %w", err)
	}
	current, err := abilityFromEnt(currentRow)
	if err != nil {
		return fmt.Errorf("解析待禁用特性战斗规则: %w", err)
	}
	if current.Version != record.ExpectedVersion {
		return ability.ErrAbilityVersionConflict
	}
	if _, err := w.client.GameAbility.UpdateOne(currentRow).Where(gameability.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return ability.ErrAbilityVersionConflict
	} else if err != nil {
		var databaseError *pgconn.PgError
		if errors.As(err, &databaseError) && databaseError.Code == "23503" {
			return ability.ErrAbilityReferenced
		}
		return fmt.Errorf("绂佺敤鐗规€ц祫鏂?瀹炴椂璧勬枡淇: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.ability.disabled",
		"game_ability", current.ID, record.RequestID, record.DisabledAt, &current, nil); err != nil {
		return err
	}
	response.Disabled = true
	if err := idempotency.Complete(ctx, writer, request, response); err != nil {
		return fmt.Errorf("淇濆瓨鐗规€ц祫鏂欑鐢ㄥ箓绛夌粨鏋? %w", err)
	}
	return nil
}
