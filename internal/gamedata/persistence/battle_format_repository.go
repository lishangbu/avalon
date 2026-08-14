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
	"github.com/lishangbu/avalon/ent/gamebattleformat"
	"github.com/lishangbu/avalon/ent/predicate"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
)

// battleRuleTransactionRepository 隔离战斗规则 Writer 的事务方法集合。
type battleRuleTransactionRepository struct {
	parent   *Adapters
	client   *avalonent.Client
	executor database.Transaction
}

// CreateFormat 鍘熷瓙鍒涘缓绋冲畾 BattleFormat 韬唤銆佸疄鏃惰祫鏂欎慨璁€佸璁″拰骞傜瓑鍝嶅簲銆?
func (w *battleRuleTransactionRepository) CreateFormat(ctx context.Context, record battleformat.CreateFormatRecord) (battleformat.Format, error) {
	digest, err := idempotency.Digest(struct {
		Format battleformat.Format
	}{record.Format})
	if err != nil {
		return battleformat.Format{}, fmt.Errorf("计算 BattleFormat 创建幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: "game-data.battle-format.create",
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.CreatedAt}
	created := record.Format
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &created)
	if err != nil {
		return battleformat.Format{}, fmt.Errorf("璁ら BattleFormat 鍒涘缓骞傜瓑閿? %w", err)
	}
	if replay {
		return created, nil
	}
	builder := w.client.GameBattleFormat.Create().SetID(created.ID).SetCode(created.Code).SetName(created.Name).SetDescription(created.Description).SetMode(string(created.Mode)).SetRosterCount(created.RosterCount).SetSelectCount(created.SelectCount).SetActiveParticipantsPerSide(created.ActiveParticipantsPerSide).SetLevelRule(string(created.LevelRule.Mode)).SetNillableNormalizedLevel(created.LevelRule.Level).SetPreviewSeconds(created.Deadlines.PreviewSeconds).SetTurnSeconds(created.Deadlines.TurnSeconds).SetBattleSeconds(created.Deadlines.BattleSeconds).SetChallengeAvailable(created.Availability.Challenge).SetTrainingAvailable(created.Availability.Training).SetEncounterAvailable(created.Availability.Encounter).SetAdminPreviewAvailable(created.Availability.AdminPreview).SetClauseIds(created.ClauseIDs).SetRestrictionIds(created.RestrictionIDs).SetMechanicIds(created.MechanicIDs).SetIsDefault(created.Default).SetEnabled(created.Enabled).SetVersion(created.Version).SetCreatedAt(record.CreatedAt.UTC()).SetUpdatedAt(record.CreatedAt.UTC())
	row, err := builder.Save(ctx)
	if isUniqueViolation(err) {
		return battleformat.Format{}, battleformat.ErrFormatConflict
	}
	if err != nil {
		return battleformat.Format{}, fmt.Errorf("插入 BattleFormat 实时资料修订: %w", err)
	}
	created = formatFromEnt(row)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.battle-format.created",
		"game_battle_format", created.ID, record.RequestID, record.CreatedAt, nil, &created); err != nil {
		return battleformat.Format{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, created); err != nil {
		return battleformat.Format{}, fmt.Errorf("保存 BattleFormat 创建幂等结果: %w", err)
	}
	return created, nil
}

// UpdateFormat 鍘熷瓙鏇存柊 BattleFormat銆佹帹杩?瀹炴椂璧勬枡 鐗堟湰骞惰褰曞璁°€?
func (w *battleRuleTransactionRepository) UpdateFormat(ctx context.Context, record battleformat.UpdateFormatRecord) (battleformat.Format, error) {
	digest, err := idempotency.Digest(struct {
		ExpectedVersion int64
		Format          battleformat.Format
	}{record.ExpectedVersion, record.Format})
	if err != nil {
		return battleformat.Format{}, fmt.Errorf("计算 BattleFormat 更新幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: "game-data.battle-format.update",
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt}
	updated := record.Format
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &updated)
	if err != nil {
		return battleformat.Format{}, fmt.Errorf("璁ら BattleFormat 鏇存柊骞傜瓑閿? %w", err)
	}
	if replay {
		return updated, nil
	}
	currentRow, err := w.client.GameBattleFormat.Query().Where(gamebattleformat.IDEQ(updated.ID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.Format{}, battleformat.ErrFormatNotFound
	}
	if err != nil {
		return battleformat.Format{}, err
	}
	before := formatFromEnt(currentRow)
	row, err := w.client.GameBattleFormat.UpdateOne(currentRow).Where(gamebattleformat.VersionEQ(record.ExpectedVersion)).SetCode(updated.Code).SetName(updated.Name).SetDescription(updated.Description).SetMode(string(updated.Mode)).SetNillableNormalizedLevel(updated.LevelRule.Level).SetRosterCount(updated.RosterCount).SetSelectCount(updated.SelectCount).SetActiveParticipantsPerSide(updated.ActiveParticipantsPerSide).SetLevelRule(string(updated.LevelRule.Mode)).SetPreviewSeconds(updated.Deadlines.PreviewSeconds).SetTurnSeconds(updated.Deadlines.TurnSeconds).SetBattleSeconds(updated.Deadlines.BattleSeconds).SetChallengeAvailable(updated.Availability.Challenge).SetTrainingAvailable(updated.Availability.Training).SetEncounterAvailable(updated.Availability.Encounter).SetAdminPreviewAvailable(updated.Availability.AdminPreview).SetClauseIds(updated.ClauseIDs).SetRestrictionIds(updated.RestrictionIDs).SetMechanicIds(updated.MechanicIDs).SetIsDefault(updated.Default).SetEnabled(updated.Enabled).SetVersion(updated.Version).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) || isUniqueViolation(err) {
		return battleformat.Format{}, battleformat.ErrFormatConflict
	}
	if err != nil {
		return battleformat.Format{}, fmt.Errorf("更新 BattleFormat 实时资料修订: %w", err)
	}
	updated = formatFromEnt(row)
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.battle-format.updated",
		"game_battle_format", updated.ID, record.RequestID, record.UpdatedAt, &before, &updated); err != nil {
		return battleformat.Format{}, err
	}
	if err := idempotency.Complete(ctx, writer, request, updated); err != nil {
		return battleformat.Format{}, fmt.Errorf("保存 BattleFormat 更新幂等结果: %w", err)
	}
	return updated, nil
}

// DisableFormat 绂佺敤瀹炴椂璧勬枡涓殑 BattleFormat锛屽苟淇濈暀宸叉湁绋冲畾韬唤銆?
func (w *battleRuleTransactionRepository) DisableFormat(ctx context.Context, record battleformat.DisableFormatRecord) error {
	digest, err := idempotency.Digest(struct {
		ExpectedVersion int64
		FormatID        snowflake.ID
	}{record.ExpectedVersion, record.FormatID})
	if err != nil {
		return fmt.Errorf("计算 BattleFormat 禁用幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.ActorAccountID, OperationID: "game-data.battle-format.delete",
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.DisabledAt}
	writer := idempotency.NewPersistentWriter(newEntAdministrationRecords(w.client, w.parent.newID))
	var response struct{}
	replay, err := idempotency.ClaimResponse(ctx, writer, request, &response)
	if err != nil || replay {
		return err
	}
	currentRow, err := w.client.GameBattleFormat.Query().Where(gamebattleformat.IDEQ(record.FormatID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.ErrFormatNotFound
	}
	if err != nil {
		return err
	}
	before := formatFromEnt(currentRow)
	_, err = w.client.GameBattleFormat.UpdateOne(currentRow).Where(gamebattleformat.VersionEQ(record.ExpectedVersion)).SetEnabled(false).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.DisabledAt.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.ErrFormatConflict
	}
	if err != nil {
		return fmt.Errorf("禁用 BattleFormat 实时资料修订: %w", err)
	}
	if err := w.parent.recordGameDataAudit(ctx, w.executor, record.ActorAccountID, "game-data.battle-format.disabled",
		"game_battle_format", record.FormatID, record.RequestID, record.DisabledAt, &before, nil); err != nil {
		return err
	}
	return idempotency.Complete(ctx, writer, request, response)
}

// GetFormat 璇诲彇褰撳墠瀹炴椂璧勬枡鐨勫崟涓?BattleFormat銆?
func (s *Adapters) GetFormat(ctx context.Context, id snowflake.ID) (battleformat.Format, error) {
	row, err := s.pool.Client(ctx).GameBattleFormat.Query().Where(gamebattleformat.IDEQ(id)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battleformat.Format{}, battleformat.ErrFormatNotFound
	}
	if err != nil {
		return battleformat.Format{}, err
	}
	return formatFromEnt(row), nil
}

// ListFormats 杩斿洖褰撳墠瀹炴椂璧勬枡鐨?BattleFormat 鍒嗛〉缁撴灉銆?
func (s *Adapters) ListFormats(ctx context.Context, query battleformat.FormatListQuery) (battleformat.FormatPage, error) {
	filters := make([]predicate.GameBattleFormat, 0, 5)
	if query.Q != "" {
		filters = append(filters, gamebattleformat.Or(gamebattleformat.CodeContainsFold(query.Q), gamebattleformat.NameContainsFold(query.Q)))
	}
	if query.Mode != "" {
		filters = append(filters, gamebattleformat.ModeEQ(string(query.Mode)))
	}
	if query.Enabled != nil {
		filters = append(filters, gamebattleformat.EnabledEQ(*query.Enabled))
	}
	if query.Challenge != nil {
		filters = append(filters, gamebattleformat.ChallengeAvailableEQ(*query.Challenge))
	}
	if query.Training != nil {
		filters = append(filters, gamebattleformat.TrainingAvailableEQ(*query.Training))
	}
	if query.Encounter != nil {
		filters = append(filters, gamebattleformat.EncounterAvailableEQ(*query.Encounter))
	}
	if query.AdminPreview != nil {
		filters = append(filters, gamebattleformat.AdminPreviewAvailableEQ(*query.AdminPreview))
	}
	client := s.pool.Client(ctx)
	total, err := client.GameBattleFormat.Query().Where(filters...).Count(ctx)
	if err != nil {
		return battleformat.FormatPage{}, fmt.Errorf("统计 BattleFormat: %w", err)
	}
	rows, err := client.GameBattleFormat.Query().Where(filters...).Order(gamebattleformat.ByCode(sql.OrderAsc()), gamebattleformat.ByID(sql.OrderAsc())).Limit(int(query.PageSize)).Offset(int(query.Page-1) * int(query.PageSize)).All(ctx)
	if err != nil {
		return battleformat.FormatPage{}, fmt.Errorf("查询 BattleFormat 列表: %w", err)
	}
	items := make([]battleformat.Format, len(rows))
	for index, row := range rows {
		items[index] = formatFromEnt(row)
	}
	return battleformat.FormatPage{Items: items, Total: int64(total), Page: query.Page, PageSize: query.PageSize}, nil
}

func formatFromValues(id pgtype.Int8, code, name, description, mode string, rosterCount, selectCount,
	activeCount int32, levelRule string, normalizedLevel pgtype.Int4, previewSeconds, turnSeconds, battleSeconds int32,
	challenge, training, encounter, adminPreview bool, clauseIDs, restrictionIDs, mechanicIDs []pgtype.Int8,
	isDefault, enabled bool, version int64) battleformat.Format {
	var level *int32
	if normalizedLevel.Valid {
		value := normalizedLevel.Int32
		level = &value
	}
	return battleformat.Format{ID: domainIdentifier(id), Code: code, Name: name, Description: description,
		Mode: battleformat.Mode(mode), RosterCount: rosterCount, SelectCount: selectCount,
		ActiveParticipantsPerSide: activeCount, LevelRule: battleformat.LevelRule{Mode: battleformat.LevelRuleMode(levelRule), Level: level},
		Deadlines:    battleformat.Deadlines{PreviewSeconds: previewSeconds, TurnSeconds: turnSeconds, BattleSeconds: battleSeconds},
		Availability: battleformat.Availability{Challenge: challenge, Training: training, Encounter: encounter, AdminPreview: adminPreview},
		ClauseIDs:    domainIdentifierSlice(clauseIDs), RestrictionIDs: domainIdentifierSlice(restrictionIDs), MechanicIDs: domainIdentifierSlice(mechanicIDs),
		Default: isDefault, Enabled: enabled, Version: version}
}

// formatFromEnt 将 Ent 赛制实体转换为领域赛制，集中处理可空等级和 Identifier 数组边界。
func formatFromEnt(row *avalonent.GameBattleFormat) battleformat.Format {
	clauseIDs := make([]pgtype.Int8, len(row.ClauseIds))
	restrictionIDs := make([]pgtype.Int8, len(row.RestrictionIds))
	mechanicIDs := make([]pgtype.Int8, len(row.MechanicIds))
	for i, value := range row.ClauseIds {
		clauseIDs[i] = databaseIdentifier(value)
	}
	for i, value := range row.RestrictionIds {
		restrictionIDs[i] = databaseIdentifier(value)
	}
	for i, value := range row.MechanicIds {
		mechanicIDs[i] = databaseIdentifier(value)
	}
	return formatFromValues(pgIdentifier(row.ID), row.Code, row.Name, row.Description, row.Mode, row.RosterCount, row.SelectCount, row.ActiveParticipantsPerSide, row.LevelRule, databaseInt32(row.NormalizedLevel), row.PreviewSeconds, row.TurnSeconds, row.BattleSeconds, row.ChallengeAvailable, row.TrainingAvailable, row.EncounterAvailable, row.AdminPreviewAvailable, clauseIDs, restrictionIDs, mechanicIDs, row.IsDefault, row.Enabled, row.Version)
}

func isUniqueViolation(err error) bool {
	var databaseError *pgconn.PgError
	return errors.As(err, &databaseError) && databaseError.Code == "23505"
}

func domainIdentifierSlice(values []pgtype.Int8) []snowflake.ID {
	result := make([]snowflake.ID, len(values))
	for index, value := range values {
		result[index] = domainIdentifier(value)
	}
	return result
}
