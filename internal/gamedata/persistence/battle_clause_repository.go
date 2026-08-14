package persistence

import (
	"context"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgtype"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamebattleclause"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// CreateClause 鍘熷瓙鍒涘缓 Clause 绋冲畾韬唤銆佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *battleRuleTransactionRepository) CreateClause(ctx context.Context, record battleformat.CreateClauseRecord) (battleformat.Clause, error) {
	const operationID = "game-data.battle-clause.create"
	digest, err := idempotency.Digest(struct {
		Clause battleformat.Clause
	}{record.Clause})
	if err != nil {
		return battleformat.Clause{}, fmt.Errorf("计算 Battle Clause 创建幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: operationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt}
	created := record.Clause
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return battleformat.Clause{}, fmt.Errorf("璁ら Battle Clause 鍒涘缓骞傜瓑閿? %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := w.client.GameBattleClause.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetDescription(created.Description).SetEffectKind(created.Definition.Kind).SetEffectSchemaVersion(created.Definition.SchemaVersion).SetEffectParameters(created.Definition.Parameters).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if isUniqueViolation(err) {
		return battleformat.Clause{}, battleformat.ErrClauseConflict
	}
	if err != nil {
		return battleformat.Clause{}, fmt.Errorf("插入 Battle Clause 实时资料修订: %w", err)
	}
	created = clauseFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.EffectKind,
		row.EffectSchemaVersion, row.EffectParameters, row.Enabled, row.Version)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, operationID,
		"game_battle_clause", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return battleformat.Clause{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return battleformat.Clause{}, fmt.Errorf("保存 Battle Clause 创建幂等结果: %w", err)
	}
	return created, nil
}

// UpdateClause 鍘熷瓙鏇存柊瀹炴椂璧勬枡 Clause銆佹帹杩?瀹炴椂璧勬枡 鐗堟湰骞惰褰曞璁°€?
func (w *battleRuleTransactionRepository) UpdateClause(ctx context.Context, record battleformat.UpdateClauseRecord) (battleformat.Clause, error) {
	const operationID = "game-data.battle-clause.update"
	digest, err := idempotency.Digest(struct {
		ExpectedVersion int64
		Clause          battleformat.Clause
	}{record.ExpectedVersion, record.Clause})
	if err != nil {
		return battleformat.Clause{}, fmt.Errorf("计算 Battle Clause 更新幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: operationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt}
	updated := record.Clause
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return battleformat.Clause{}, fmt.Errorf("璁ら Battle Clause 鏇存柊骞傜瓑閿? %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameBattleClause.Query().Where(gamebattleclause.IDEQ(updated.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.Clause{}, battleformat.ErrClauseNotFound
	}
	if err != nil {
		return battleformat.Clause{}, fmt.Errorf("查询 Battle Clause: %w", err)
	}
	before := clauseFromValues(pgIdentifier(currentRow.ID), currentRow.Code, currentRow.Name, currentRow.Description, currentRow.EffectKind, currentRow.EffectSchemaVersion, currentRow.EffectParameters, currentRow.Enabled, currentRow.Version)
	row, err := w.client.GameBattleClause.UpdateOne(currentRow).Where(gamebattleclause.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetDescription(updated.Description).SetEffectKind(updated.Definition.Kind).SetEffectSchemaVersion(updated.Definition.SchemaVersion).SetEffectParameters(updated.Definition.Parameters).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) || isUniqueViolation(err) {
		return battleformat.Clause{}, battleformat.ErrClauseConflict
	}
	if err != nil {
		return battleformat.Clause{}, fmt.Errorf("更新 Battle Clause 实时资料修订: %w", err)
	}
	updated = clauseFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.EffectKind,
		row.EffectSchemaVersion, row.EffectParameters, row.Enabled, row.Version)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, operationID,
		"game_battle_clause", updated.ID, record.RequestID, record.UpdatedAt, &before, &updated); err != nil {
		return battleformat.Clause{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return battleformat.Clause{}, fmt.Errorf("保存 Battle Clause 更新幂等结果: %w", err)
	}
	return updated, nil
}

// DisableClause 绂佺敤鏈 BattleFormat 寮曠敤鐨?瀹炴椂璧勬枡 Clause锛屽苟淇濈暀宸插彂甯冪ǔ瀹氳韩浠姐€?
func (w *battleRuleTransactionRepository) DisableClause(ctx context.Context, record battleformat.DisableClauseRecord) error {
	const operationID = "game-data.battle-clause.delete"
	digest, err := idempotency.Digest(struct {
		ExpectedVersion int64
		ClauseID        snowflake.ID
	}{record.ExpectedVersion, record.ClauseID})
	if err != nil {
		return fmt.Errorf("计算 Battle Clause 禁用幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: operationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt}
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	var response struct{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil || replay {
		return err
	}
	currentRow, err := w.client.GameBattleClause.Query().Where(gamebattleclause.IDEQ(record.ClauseID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.ErrClauseNotFound
	}
	if err != nil {
		return fmt.Errorf("查询 Battle Clause: %w", err)
	}
	before := clauseFromValues(pgIdentifier(currentRow.ID), currentRow.Code, currentRow.Name, currentRow.Description, currentRow.EffectKind, currentRow.EffectSchemaVersion, currentRow.EffectParameters, currentRow.Enabled, currentRow.Version)
	if _, err = w.client.GameBattleClause.UpdateOne(currentRow).Where(gamebattleclause.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return battleformat.ErrClauseConflict
	} else if err != nil {
		return fmt.Errorf("禁用 Battle Clause 实时资料修订: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, operationID,
		"game_battle_clause", record.ClauseID, record.RequestID, record.DisabledAt, &before, nil); err != nil {
		return err
	}
	return idempotency.Complete(ctx, writer, request, response)
}

// GetClause 璇诲彇瀹炴椂璧勬枡鐨勫崟涓?Clause銆?
func (s *Adapters) GetClause(ctx context.Context, id snowflake.ID) (battleformat.Clause, error) {
	row, err := s.pool.Client(ctx).GameBattleClause.Query().Where(gamebattleclause.IDEQ(id)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.Clause{}, battleformat.ErrClauseNotFound
	}
	if err != nil {
		return battleformat.Clause{}, fmt.Errorf("查询 Battle Clause: %w", err)
	}
	return clauseFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.EffectKind, row.EffectSchemaVersion, row.EffectParameters, row.Enabled, row.Version), nil
}

// ListClauses 杩斿洖瀹炴椂璧勬枡鐨?Clause 鏈夌晫鍒嗛〉缁撴灉銆?
func (s *Adapters) ListClauses(ctx context.Context, query battleformat.ClauseListQuery) (battleformat.ClausePage, error) {
	filters := make([]predicate.GameBattleClause, 0, 2)
	if query.Q != "" {
		filters = append(filters, gamebattleclause.Or(gamebattleclause.CodeContainsFold(query.Q), gamebattleclause.NameContainsFold(query.Q)))
	}
	if query.Enabled != nil {
		filters = append(filters, gamebattleclause.EnabledEQ(*query.Enabled))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameBattleClause.Query().Where(filters...).Count(ctx)
	if err != nil {
		return battleformat.ClausePage{}, fmt.Errorf("统计 Battle Clause: %w", err)
	}
	rows, err := client.GameBattleClause.Query().Where(filters...).Order(gamebattleclause.ByCode(sql.OrderAsc()), gamebattleclause.ByID(sql.OrderAsc())).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return battleformat.ClausePage{}, fmt.Errorf("查询 Battle Clause 列表: %w", err)
	}
	items := make([]battleformat.Clause, len(rows))
	for index, row := range rows {
		items[index] = clauseFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.EffectKind,
			row.EffectSchemaVersion, row.EffectParameters, row.Enabled, row.Version)
	}
	return battleformat.ClausePage{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

func clauseFromValues(id pgtype.Int8, code, name, description, kind string, schemaVersion int32,
	parameters []byte, enabled bool, version int64) battleformat.Clause {
	return battleformat.Clause{ID: domainIdentifier(id), Code: code, Name: name, Description: description,
		Definition: effect.Definition{Kind: kind, SchemaVersion: schemaVersion, Parameters: append([]byte(nil), parameters...)},
		Enabled:    enabled, Version: version}
}
