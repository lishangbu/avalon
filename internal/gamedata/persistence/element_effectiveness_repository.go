package persistence

import (
	"context"
	"errors"
	"fmt"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgtype"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/gameelementeffectiveness"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/elementeffectiveness"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

const (
	createElementEffectivenessOperationID = "game-data.element-effectiveness.create"
	updateElementEffectivenessOperationID = "game-data.element-effectiveness.update"
)

type elementEffectivenessTransactionRepository struct {
	parent   *Adapters
	client   *avalonent.Client
	executor database.Transaction
}

// Create 在维护事务中创建属性克制资料、推进全局修订并记录审计事实。
func (w *elementEffectivenessTransactionRepository) Create(ctx context.Context, record elementeffectiveness.CreateRecord) (elementeffectiveness.Effectiveness, error) {
	digest, err := idempotency.Digest(record.Effectiveness)
	if err != nil {
		return elementeffectiveness.Effectiveness{}, fmt.Errorf("计算属性克制创建幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: createElementEffectivenessOperationID, Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.At}
	value := record.Effectiveness
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &value)
	if err != nil {
		return elementeffectiveness.Effectiveness{}, err
	}
	if replay {
		return value, nil
	}
	row, err := w.client.GameElementEffectiveness.Create().SetID(value.ID).SetAttackElementID(value.AttackElementID).SetDefenseElementID(value.DefenseElementID).SetNumerator(int16(value.Numerator)).SetDenominator(int16(value.Denominator)).SetEnabled(value.Enabled).SetVersion(value.Version).SetCreatedAt(record.At.UTC()).SetUpdatedAt(record.At.UTC()).Save(ctx)
	if err != nil {
		return elementeffectiveness.Effectiveness{}, effectivenessDatabaseError("创建", err)
	}
	value = effectivenessFromValues(pgIdentifier(row.ID), pgIdentifier(row.AttackElementID), pgIdentifier(row.DefenseElementID), row.Numerator, row.Denominator, row.Enabled, row.Version)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.element-effectiveness.created", "game_element_effectiveness", value.ID, record.RequestID, record.At, nil, &value); err != nil {
		return elementeffectiveness.Effectiveness{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, value); err != nil {
		return elementeffectiveness.Effectiveness{}, err
	}
	return value, nil
}

// Update 在维护事务中完整替换属性克制资料并推进全局修订。
func (w *elementEffectivenessTransactionRepository) Update(ctx context.Context, record elementeffectiveness.UpdateRecord) (elementeffectiveness.Effectiveness, error) {
	digest, err := idempotency.Digest(record.Effectiveness)
	if err != nil {
		return elementeffectiveness.Effectiveness{}, fmt.Errorf("计算属性克制更新幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: updateElementEffectivenessOperationID, Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.At}
	value := record.Effectiveness
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &value)
	if err != nil {
		return elementeffectiveness.Effectiveness{}, err
	}
	if replay {
		return value, nil
	}
	currentRow, err := w.client.GameElementEffectiveness.Query().Where(gameelementeffectiveness.IDEQ(value.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return elementeffectiveness.Effectiveness{}, elementeffectiveness.ErrEffectivenessNotFound
	}
	if err != nil {
		return elementeffectiveness.Effectiveness{}, fmt.Errorf("锁定属性克制资料: %w", err)
	}
	current := effectivenessFromValues(pgIdentifier(currentRow.ID), pgIdentifier(currentRow.AttackElementID), pgIdentifier(currentRow.DefenseElementID), currentRow.Numerator, currentRow.Denominator, currentRow.Enabled, currentRow.Version)
	if current.Version != record.ExpectedVersion {
		return elementeffectiveness.Effectiveness{}, elementeffectiveness.ErrEffectivenessConflict
	}
	row, err := w.client.GameElementEffectiveness.UpdateOne(currentRow).Where(gameelementeffectiveness.VersionEQ(record.ExpectedVersion)).SetAttackElementID(value.AttackElementID).SetDefenseElementID(value.DefenseElementID).SetNumerator(int16(value.Numerator)).SetDenominator(int16(value.Denominator)).SetEnabled(value.Enabled).SetVersion(value.Version).SetUpdatedAt(record.At.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) {
		return elementeffectiveness.Effectiveness{}, elementeffectiveness.ErrEffectivenessConflict
	}
	if err != nil {
		return elementeffectiveness.Effectiveness{}, effectivenessDatabaseError("更新", err)
	}
	value = effectivenessFromValues(pgIdentifier(row.ID), pgIdentifier(row.AttackElementID), pgIdentifier(row.DefenseElementID), row.Numerator, row.Denominator, row.Enabled, row.Version)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.element-effectiveness.updated", "game_element_effectiveness", value.ID, record.RequestID, record.At, &current, &value); err != nil {
		return elementeffectiveness.Effectiveness{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, value); err != nil {
		return elementeffectiveness.Effectiveness{}, err
	}
	return value, nil
}

// Get 查询指定属性克制资料。
func (s *Adapters) GetElementEffectiveness(ctx context.Context, id snowflake.ID) (elementeffectiveness.Effectiveness, error) {
	row, err := s.pool.Client(ctx).GameElementEffectiveness.Query().Where(gameelementeffectiveness.IDEQ(id)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return elementeffectiveness.Effectiveness{}, elementeffectiveness.ErrEffectivenessNotFound
	}
	if err != nil {
		return elementeffectiveness.Effectiveness{}, fmt.Errorf("查询属性克制资料: %w", err)
	}
	return effectivenessFromValues(pgIdentifier(row.ID), pgIdentifier(row.AttackElementID), pgIdentifier(row.DefenseElementID), row.Numerator, row.Denominator, row.Enabled, row.Version), nil
}

// ListElementEffectiveness 返回属性克制资料页。
func (s *Adapters) ListElementEffectiveness(ctx context.Context, query elementeffectiveness.ListQuery) (elementeffectiveness.Page, error) {
	client := s.pool.Client(ctx)
	filters := make([]predicate.GameElementEffectiveness, 0, 3)
	if query.AttackElementID != nil {
		filters = append(filters, gameelementeffectiveness.AttackElementIDEQ(*query.AttackElementID))
	}
	if query.DefenseElementID != nil {
		filters = append(filters, gameelementeffectiveness.DefenseElementIDEQ(*query.DefenseElementID))
	}
	if query.Enabled != nil {
		filters = append(filters, gameelementeffectiveness.EnabledEQ(*query.Enabled))
	}
	total, err := client.GameElementEffectiveness.Query().Where(filters...).Count(ctx)
	if err != nil {
		return elementeffectiveness.Page{}, fmt.Errorf("统计属性克制资料: %w", err)
	}
	rows, err := client.GameElementEffectiveness.Query().Where(filters...).Order(gameelementeffectiveness.ByID(sql.OrderAsc())).Offset(int(query.Page-1) * int(query.PageSize)).Limit(int(query.PageSize)).All(ctx)
	if err != nil {
		return elementeffectiveness.Page{}, fmt.Errorf("查询属性克制资料页: %w", err)
	}
	items := make([]elementeffectiveness.Effectiveness, len(rows))
	for index, row := range rows {
		items[index] = effectivenessFromValues(pgIdentifier(row.ID), pgIdentifier(row.AttackElementID), pgIdentifier(row.DefenseElementID), row.Numerator, row.Denominator, row.Enabled, row.Version)
	}
	return elementeffectiveness.Page{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

// ListEnabledElementEffectiveness 返回全部启用倍率，供 Battle 冻结规则快照。
func (s *Adapters) ListEnabledElementEffectiveness(ctx context.Context) ([]elementeffectiveness.Effectiveness, error) {
	rows, err := s.pool.Client(ctx).GameElementEffectiveness.Query().Where(gameelementeffectiveness.EnabledEQ(true)).Order(gameelementeffectiveness.ByID(sql.OrderAsc())).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询启用属性克制资料: %w", err)
	}
	items := make([]elementeffectiveness.Effectiveness, len(rows))
	for index, row := range rows {
		items[index] = effectivenessFromValues(pgIdentifier(row.ID), pgIdentifier(row.AttackElementID), pgIdentifier(row.DefenseElementID), row.Numerator, row.Denominator, row.Enabled, row.Version)
	}
	return items, nil
}

func effectivenessFromValues(id, attackID, defenseID pgtype.Int8, numerator, denominator int16, enabled bool, version int64) elementeffectiveness.Effectiveness {
	return elementeffectiveness.Effectiveness{ID: domainIdentifier(id), AttackElementID: domainIdentifier(attackID), DefenseElementID: domainIdentifier(defenseID), Numerator: uint16(numerator), Denominator: uint16(denominator), Enabled: enabled, Version: version}
}

// pgIdentifier 将 Ent Identifier 转换为领域转换函数使用的 pgtype Identifier。
func pgIdentifier(value snowflake.ID) pgtype.Int8 {
	return pgtype.Int8{Int64: int64(value), Valid: true}
}
func effectivenessDatabaseError(action string, err error) error {
	var databaseError *pgconn.PgError
	if errors.As(err, &databaseError) && (databaseError.Code == "23505" || databaseError.Code == "23503") {
		return elementeffectiveness.ErrEffectivenessConflict
	}
	return fmt.Errorf("%s属性克制资料: %w", action, err)
}
