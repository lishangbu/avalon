package store

import (
	"context"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgtype"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gamebattlemechanic"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/effect"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// CreateMechanic 鍘熷瓙鍒涘缓 Mechanic 绋冲畾韬唤銆佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *battleRuleTransactionStore) CreateMechanic(ctx context.Context, record battleformat.CreateMechanicRecord) (battleformat.Mechanic, error) {
	const operationID = "game-data.battle-mechanic.create"
	digest, err := idempotency.Digest(struct {
		Mechanic battleformat.Mechanic
	}{record.Mechanic})
	if err != nil {
		return battleformat.Mechanic{}, fmt.Errorf("计算 Battle Mechanic 创建幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: operationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt}
	created := record.Mechanic
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return battleformat.Mechanic{}, fmt.Errorf("璁ら Battle Mechanic 鍒涘缓骞傜瓑閿? %w", err)
	}
	if replay {
		return created, nil
	}
	row, err := w.client.GameBattleMechanic.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetDescription(created.Description).SetEffectKind(created.Definition.Kind).SetEffectSchemaVersion(created.Definition.SchemaVersion).SetEffectParameters(created.Definition.Parameters).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC()).Save(ctx)
	if isUniqueViolation(err) {
		return battleformat.Mechanic{}, battleformat.ErrMechanicConflict
	}
	if err != nil {
		return battleformat.Mechanic{}, fmt.Errorf("插入 Battle Mechanic 实时资料修订: %w", err)
	}
	created = mechanicFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.EffectKind,
		row.EffectSchemaVersion, row.EffectParameters, row.Enabled, row.Version)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, operationID,
		"game_battle_mechanic", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return battleformat.Mechanic{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return battleformat.Mechanic{}, fmt.Errorf("保存 Battle Mechanic 创建幂等结果: %w", err)
	}
	return created, nil
}

// UpdateMechanic 鍘熷瓙鏇存柊瀹炴椂璧勬枡 Mechanic銆佹帹杩?瀹炴椂璧勬枡 鐗堟湰骞惰褰曞璁°€?
func (w *battleRuleTransactionStore) UpdateMechanic(ctx context.Context, record battleformat.UpdateMechanicRecord) (battleformat.Mechanic, error) {
	const operationID = "game-data.battle-mechanic.update"
	digest, err := idempotency.Digest(struct {
		ExpectedVersion int64
		Mechanic        battleformat.Mechanic
	}{record.ExpectedVersion, record.Mechanic})
	if err != nil {
		return battleformat.Mechanic{}, fmt.Errorf("计算 Battle Mechanic 更新幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: operationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt}
	updated := record.Mechanic
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return battleformat.Mechanic{}, fmt.Errorf("璁ら Battle Mechanic 鏇存柊骞傜瓑閿? %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameBattleMechanic.Query().Where(gamebattlemechanic.IDEQ(updated.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.Mechanic{}, battleformat.ErrMechanicNotFound
	}
	if err != nil {
		return battleformat.Mechanic{}, err
	}
	before := mechanicFromValues(pgIdentifier(currentRow.ID), currentRow.Code, currentRow.Name, currentRow.Description, currentRow.EffectKind, currentRow.EffectSchemaVersion, currentRow.EffectParameters, currentRow.Enabled, currentRow.Version)
	row, err := w.client.GameBattleMechanic.UpdateOne(currentRow).Where(gamebattlemechanic.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetDescription(updated.Description).SetEffectKind(updated.Definition.Kind).SetEffectSchemaVersion(updated.Definition.SchemaVersion).SetEffectParameters(updated.Definition.Parameters).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) || isUniqueViolation(err) {
		return battleformat.Mechanic{}, battleformat.ErrMechanicConflict
	}
	if err != nil {
		return battleformat.Mechanic{}, fmt.Errorf("更新 Battle Mechanic 实时资料修订: %w", err)
	}
	updated = mechanicFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.EffectKind,
		row.EffectSchemaVersion, row.EffectParameters, row.Enabled, row.Version)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, operationID,
		"game_battle_mechanic", updated.ID, record.RequestID, record.UpdatedAt, &before, &updated); err != nil {
		return battleformat.Mechanic{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return battleformat.Mechanic{}, fmt.Errorf("保存 Battle Mechanic 更新幂等结果: %w", err)
	}
	return updated, nil
}

// DisableMechanic 绂佺敤鏈 BattleFormat 寮曠敤鐨?瀹炴椂璧勬枡 Mechanic锛屽苟淇濈暀宸插彂甯冪ǔ瀹氳韩浠姐€?
func (w *battleRuleTransactionStore) DisableMechanic(ctx context.Context, record battleformat.DisableMechanicRecord) error {
	const operationID = "game-data.battle-mechanic.delete"
	digest, err := idempotency.Digest(struct {
		ExpectedVersion int64
		MechanicID      snowflake.ID
	}{record.ExpectedVersion, record.MechanicID})
	if err != nil {
		return fmt.Errorf("计算 Battle Mechanic 禁用幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: operationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt}
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	var response struct{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil || replay {
		return err
	}
	currentRow, err := w.client.GameBattleMechanic.Query().Where(gamebattlemechanic.IDEQ(record.MechanicID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.ErrMechanicNotFound
	}
	if err != nil {
		return err
	}
	before := mechanicFromValues(pgIdentifier(currentRow.ID), currentRow.Code, currentRow.Name, currentRow.Description, currentRow.EffectKind, currentRow.EffectSchemaVersion, currentRow.EffectParameters, currentRow.Enabled, currentRow.Version)
	if _, err = w.client.GameBattleMechanic.UpdateOne(currentRow).Where(gamebattlemechanic.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx); avalonent.IsNotFound(err) {
		return battleformat.ErrMechanicConflict
	} else if err != nil {
		return fmt.Errorf("禁用 Battle Mechanic 实时资料修订: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, operationID,
		"game_battle_mechanic", record.MechanicID, record.RequestID, record.DisabledAt, &before, nil); err != nil {
		return err
	}
	return idempotency.Complete(ctx, writer, request, response)
}

// GetMechanic 璇诲彇瀹炴椂璧勬枡鐨勫崟涓?Mechanic銆?
func (s *Store) GetMechanic(ctx context.Context, id snowflake.ID) (battleformat.Mechanic, error) {
	row, err := s.pool.Client(ctx).GameBattleMechanic.Query().Where(gamebattlemechanic.IDEQ(id)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.Mechanic{}, battleformat.ErrMechanicNotFound
	}
	if err != nil {
		return battleformat.Mechanic{}, err
	}
	return mechanicFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.EffectKind, row.EffectSchemaVersion, row.EffectParameters, row.Enabled, row.Version), nil
}

// ListMechanics 杩斿洖瀹炴椂璧勬枡鐨?Mechanic 鏈夌晫鍒嗛〉缁撴灉銆?
func (s *Store) ListMechanics(ctx context.Context, query battleformat.MechanicListQuery) (battleformat.MechanicPage, error) {
	filters := make([]predicate.GameBattleMechanic, 0, 2)
	if query.Q != "" {
		filters = append(filters, gamebattlemechanic.Or(gamebattlemechanic.CodeContainsFold(query.Q), gamebattlemechanic.NameContainsFold(query.Q)))
	}
	if query.Enabled != nil {
		filters = append(filters, gamebattlemechanic.EnabledEQ(*query.Enabled))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameBattleMechanic.Query().Where(filters...).Count(ctx)
	if err != nil {
		return battleformat.MechanicPage{}, fmt.Errorf("统计 Battle Mechanic: %w", err)
	}
	rows, err := client.GameBattleMechanic.Query().Where(filters...).Order(gamebattlemechanic.ByCode(sql.OrderAsc()), gamebattlemechanic.ByID(sql.OrderAsc())).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return battleformat.MechanicPage{}, fmt.Errorf("查询 Battle Mechanic 列表: %w", err)
	}
	items := make([]battleformat.Mechanic, len(rows))
	for index, row := range rows {
		items[index] = mechanicFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.EffectKind,
			row.EffectSchemaVersion, row.EffectParameters, row.Enabled, row.Version)
	}
	return battleformat.MechanicPage{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

func mechanicFromValues(id pgtype.Int8, code, name, description, kind string, schemaVersion int32,
	parameters []byte, enabled bool, version int64) battleformat.Mechanic {
	return battleformat.Mechanic{ID: domainIdentifier(id), Code: code, Name: name, Description: description,
		Definition: effect.Definition{Kind: kind, SchemaVersion: schemaVersion, Parameters: append([]byte(nil), parameters...)},
		Enabled:    enabled, Version: version}
}
