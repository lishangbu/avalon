package store

import (
	"context"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgtype"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamebattlerestriction"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// CreateRestriction 鍘熷瓙鍒涘缓 Restriction 绋冲畾韬唤銆佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *battleRuleTransactionStore) CreateRestriction(ctx context.Context, record battleformat.CreateRestrictionRecord) (battleformat.Restriction, error) {
	const operationID = "game-data.battle-restriction.create"
	digest, err := idempotency.Digest(struct {
		Restriction battleformat.Restriction
	}{record.Restriction})
	if err != nil {
		return battleformat.Restriction{}, fmt.Errorf("计算 Battle Restriction 创建幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: operationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt}
	created := record.Restriction
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return battleformat.Restriction{}, fmt.Errorf("璁ら Battle Restriction 鍒涘缓骞傜瓑閿? %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := w.client.GameBattleRestriction.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetDescription(created.Description).SetEffectKind(created.Definition.Kind).SetEffectSchemaVersion(created.Definition.SchemaVersion).SetEffectParameters(created.Definition.Parameters).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if isUniqueViolation(err) {
		return battleformat.Restriction{}, battleformat.ErrRestrictionConflict
	}
	if err != nil {
		return battleformat.Restriction{}, fmt.Errorf("插入 Battle Restriction 实时资料修订: %w", err)
	}
	created = restrictionFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.EffectKind,
		row.EffectSchemaVersion, row.EffectParameters, row.Enabled, row.Version)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, operationID,
		"game_battle_restriction", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return battleformat.Restriction{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return battleformat.Restriction{}, fmt.Errorf("保存 Battle Restriction 创建幂等结果: %w", err)
	}
	return created, nil
}

// UpdateRestriction 鍘熷瓙鏇存柊瀹炴椂璧勬枡 Restriction銆佹帹杩?瀹炴椂璧勬枡 鐗堟湰骞惰褰曞璁°€?
func (w *battleRuleTransactionStore) UpdateRestriction(ctx context.Context, record battleformat.UpdateRestrictionRecord) (battleformat.Restriction, error) {
	const operationID = "game-data.battle-restriction.update"
	digest, err := idempotency.Digest(struct {
		ExpectedVersion int64
		Restriction     battleformat.Restriction
	}{record.ExpectedVersion, record.Restriction})
	if err != nil {
		return battleformat.Restriction{}, fmt.Errorf("计算 Battle Restriction 更新幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: operationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt}
	updated := record.Restriction
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return battleformat.Restriction{}, fmt.Errorf("璁ら Battle Restriction 鏇存柊骞傜瓑閿? %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameBattleRestriction.Query().Where(gamebattlerestriction.IDEQ(updated.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.Restriction{}, battleformat.ErrRestrictionNotFound
	}
	if err != nil {
		return battleformat.Restriction{}, err
	}
	before := restrictionFromValues(pgIdentifier(currentRow.ID), currentRow.Code, currentRow.Name, currentRow.Description, currentRow.EffectKind, currentRow.EffectSchemaVersion, currentRow.EffectParameters, currentRow.Enabled, currentRow.Version)
	row, err := w.client.GameBattleRestriction.UpdateOne(currentRow).Where(gamebattlerestriction.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetDescription(updated.Description).SetEffectKind(updated.Definition.Kind).SetEffectSchemaVersion(updated.Definition.SchemaVersion).SetEffectParameters(updated.Definition.Parameters).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) || isUniqueViolation(err) {
		return battleformat.Restriction{}, battleformat.ErrRestrictionConflict
	}
	if err != nil {
		return battleformat.Restriction{}, fmt.Errorf("更新 Battle Restriction 实时资料修订: %w", err)
	}
	updated = restrictionFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.EffectKind,
		row.EffectSchemaVersion, row.EffectParameters, row.Enabled, row.Version)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, operationID,
		"game_battle_restriction", updated.ID, record.RequestID, record.UpdatedAt, &before, &updated); err != nil {
		return battleformat.Restriction{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return battleformat.Restriction{}, fmt.Errorf("保存 Battle Restriction 更新幂等结果: %w", err)
	}
	return updated, nil
}

// DisableRestriction 绂佺敤鏈 BattleFormat 寮曠敤鐨?瀹炴椂璧勬枡 Restriction锛屽苟淇濈暀宸插彂甯冪ǔ瀹氳韩浠姐€?
func (w *battleRuleTransactionStore) DisableRestriction(ctx context.Context, record battleformat.DisableRestrictionRecord) error {
	const operationID = "game-data.battle-restriction.delete"
	digest, err := idempotency.Digest(struct {
		ExpectedVersion int64
		RestrictionID   snowflake.ID
	}{record.ExpectedVersion, record.RestrictionID})
	if err != nil {
		return fmt.Errorf("计算 Battle Restriction 禁用幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: operationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt}
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	var response struct{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil || replay {
		return err
	}
	currentRow, err := w.client.GameBattleRestriction.Query().Where(gamebattlerestriction.IDEQ(record.RestrictionID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.ErrRestrictionNotFound
	}
	if err != nil {
		return err
	}
	before := restrictionFromValues(pgIdentifier(currentRow.ID), currentRow.Code, currentRow.Name, currentRow.Description, currentRow.EffectKind, currentRow.EffectSchemaVersion, currentRow.EffectParameters, currentRow.Enabled, currentRow.Version)
	if _, err = w.client.GameBattleRestriction.UpdateOne(currentRow).Where(gamebattlerestriction.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return battleformat.ErrRestrictionConflict
	} else if err != nil {
		return fmt.Errorf("禁用 Battle Restriction 实时资料修订: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, operationID,
		"game_battle_restriction", record.RestrictionID, record.RequestID, record.DisabledAt, &before, nil); err != nil {
		return err
	}
	return idempotency.Complete(ctx, writer, request, response)
}

// GetRestriction 璇诲彇瀹炴椂璧勬枡鐨勫崟涓?Restriction銆?
func (s *Store) GetRestriction(ctx context.Context, id snowflake.ID) (battleformat.Restriction, error) {
	row, err := s.pool.Client(ctx).GameBattleRestriction.Query().Where(gamebattlerestriction.IDEQ(id)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.Restriction{}, battleformat.ErrRestrictionNotFound
	}
	if err != nil {
		return battleformat.Restriction{}, err
	}
	return restrictionFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.EffectKind, row.EffectSchemaVersion, row.EffectParameters, row.Enabled, row.Version), nil
}

// ListRestrictions 杩斿洖瀹炴椂璧勬枡鐨?Restriction 鏈夌晫鍒嗛〉缁撴灉銆?
func (s *Store) ListRestrictions(ctx context.Context, query battleformat.RestrictionListQuery) (battleformat.RestrictionPage, error) {
	filters := make([]predicate.GameBattleRestriction, 0, 2)
	if query.Q != "" {
		filters = append(filters, gamebattlerestriction.Or(gamebattlerestriction.CodeContainsFold(query.Q), gamebattlerestriction.NameContainsFold(query.Q)))
	}
	if query.Enabled != nil {
		filters = append(filters, gamebattlerestriction.EnabledEQ(*query.Enabled))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameBattleRestriction.Query().Where(filters...).Count(ctx)
	if err != nil {
		return battleformat.RestrictionPage{}, fmt.Errorf("统计 Battle Restriction: %w", err)
	}
	rows, err := client.GameBattleRestriction.Query().Where(filters...).Order(gamebattlerestriction.ByCode(sql.OrderAsc()), gamebattlerestriction.ByID(sql.OrderAsc())).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return battleformat.RestrictionPage{}, fmt.Errorf("查询 Battle Restriction 列表: %w", err)
	}
	items := make([]battleformat.Restriction, len(rows))
	for index, row := range rows {
		items[index] = restrictionFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.EffectKind,
			row.EffectSchemaVersion, row.EffectParameters, row.Enabled, row.Version)
	}
	return battleformat.RestrictionPage{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

func restrictionFromValues(id pgtype.Int8, code, name, description, kind string, schemaVersion int32,
	parameters []byte, enabled bool, version int64) battleformat.Restriction {
	return battleformat.Restriction{ID: domainIdentifier(id), Code: code, Name: name, Description: description,
		Definition: effect.Definition{Kind: kind, SchemaVersion: schemaVersion, Parameters: append([]byte(nil), parameters...)},
		Enabled:    enabled, Version: version}
}
