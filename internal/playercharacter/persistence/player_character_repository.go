// Package persistence 提供 PlayerCharacter 的 PostgreSQL 持久化适配器。
package persistence

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
	"github.com/jackc/pgx/v5/pgtype"
	avalonent "github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/account"
	"github.com/lishangbu/avalon/ent/activeplayercharacter"
	entpc "github.com/lishangbu/avalon/ent/playercharacter"
	"github.com/lishangbu/avalon/ent/playercharacterdisplaynamehistory"
	"github.com/lishangbu/avalon/ent/playercharactersensitivenamerule"
	"github.com/lishangbu/avalon/ent/rpglocation"
	"github.com/lishangbu/avalon/ent/rpglocationexit"
	platformaudit "github.com/lishangbu/avalon/internal/platform/audit"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/idempotency"
	"github.com/lishangbu/avalon/internal/playercharacter"
)

const (
	createOperationID       = "player-character.create"
	renameOperationID       = "player-character.rename"
	archiveOperationID      = "player-character.archive"
	restoreOperationID      = "player-character.restore"
	switchActiveOperationID = "player-character.switch-active"
)

// repository 使用 Ent 事务和账号行锁串行化角色上限、名称和生命周期写入。
type repository struct {
	pool  *database.Pool
	newID snowflake.Source
}

// NewRepository 创建 PlayerCharacter PostgreSQL 持久化适配器。
func NewRepository(pool *database.Pool, newID snowflake.Source) *repository {
	return &repository{pool: pool, newID: newID}
}

// WithinAccount 开启事务并锁定 Account，防止并发命令突破账号级不变量。
func (s *repository) WithinAccount(
	ctx context.Context,
	accountID snowflake.ID,
	work func(playercharacter.Writer) error,
) error {
	return s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := s.pool.Client(transactionCtx)
		if err := lockAccount(ctx, client, accountID); avalonent.IsNotFound(err) {
			return playercharacter.ErrInvalidCommand
		} else if err != nil {
			return fmt.Errorf("锁定 PlayerCharacter 账号: %w", err)
		}
		return work(&transactionRepository{parent: s, client: client, records: idempotency.NewEntRecords(client, s.newID), executor: database.Executor(transactionCtx, s.pool)})
	})
}

// GetOwned 查询账号拥有的指定 PlayerCharacter，避免通过角色 Identifier 越权读取。
func (s *repository) GetOwned(ctx context.Context, accountID, playerCharacterID snowflake.ID) (playercharacter.PlayerCharacter, error) {
	row, err := s.pool.Client(ctx).PlayerCharacter.Query().Where(
		entpc.IDEQ(playerCharacterID),
		entpc.HasAccountWith(account.IDEQ(accountID)),
	).Only(ctx)
	if avalonent.IsNotFound(err) {
		return playercharacter.PlayerCharacter{}, playercharacter.ErrPlayerCharacterNotFound
	}
	if err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("查询账号 PlayerCharacter: %w", err)
	}
	return playerCharacterFromEnt(row), nil
}

// ListOwned 按稳定创建顺序查询账号拥有的角色。
func (s *repository) ListOwned(ctx context.Context, accountID snowflake.ID, includeArchived bool) ([]playercharacter.PlayerCharacter, error) {
	query := s.pool.Client(ctx).PlayerCharacter.Query().Where(entpc.HasAccountWith(account.IDEQ(accountID))).Order(entpc.ByCreatedAt())
	if !includeArchived {
		query = query.Where(entpc.ArchivedAtIsNil())
	}
	rows, err := query.All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询账号 PlayerCharacter 列表: %w", err)
	}
	result := make([]playercharacter.PlayerCharacter, 0, len(rows))
	for _, row := range rows {
		result = append(result, playerCharacterFromEnt(row))
	}
	return result, nil
}

// GetActive 查询账号跨设备共享的持久活动角色绑定。
func (s *repository) GetActive(ctx context.Context, accountID snowflake.ID) (playercharacter.ActiveBinding, error) {
	row, err := s.pool.Client(ctx).ActivePlayerCharacter.Query().Where(activeplayercharacter.IDEQ(accountID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return playercharacter.ActiveBinding{}, playercharacter.ErrPlayerCharacterNotFound
	}
	if err != nil {
		return playercharacter.ActiveBinding{}, fmt.Errorf("查询活动 PlayerCharacter: %w", err)
	}
	return playercharacter.ActiveBinding{AccountID: snowflake.ID(row.ID), PlayerCharacterID: snowflake.ID(row.PlayerCharacterID), Version: row.Version, UpdatedAt: row.UpdatedAt.UTC()}, nil
}

// FindActiveByDisplayNameKey 只返回仍是其账号活动绑定的未归档角色。
func (s *repository) FindActiveByDisplayNameKey(ctx context.Context, displayNameKey string) (playercharacter.PlayerCharacter, error) {
	// 先读取所有持久活动绑定，再按展示名称查找角色，确保未绑定或已归档角色不会被误返回。
	bindings, err := s.pool.Client(ctx).ActivePlayerCharacter.Query().All(ctx)
	if err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("查询活动 PlayerCharacter 绑定: %w", err)
	}
	ids := make([]snowflake.ID, 0, len(bindings))
	for _, binding := range bindings {
		ids = append(ids, binding.PlayerCharacterID)
	}
	if len(ids) == 0 {
		return playercharacter.PlayerCharacter{}, playercharacter.ErrPlayerCharacterNotFound
	}
	row, err := s.pool.Client(ctx).PlayerCharacter.Query().Where(entpc.DisplayNameKeyEQ(displayNameKey), entpc.ArchivedAtIsNil(), entpc.IDIn(ids...)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return playercharacter.PlayerCharacter{}, playercharacter.ErrPlayerCharacterNotFound
	}
	if err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("按展示名称查询活动 PlayerCharacter: %w", err)
	}
	return playerCharacterFromEnt(row), nil
}

// SwitchActive 在账号锁内校验角色所有权并以乐观版本更新唯一活动绑定。
func (s *repository) SwitchActive(ctx context.Context, record playercharacter.SwitchActiveRecord) (playercharacter.SwitchActiveResult, error) {
	var result playercharacter.SwitchActiveResult
	err := s.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := s.pool.Client(transactionCtx)
		executor := database.Executor(transactionCtx, s.pool)
		if err := lockAccount(ctx, client, record.AccountID); err != nil {
			if avalonent.IsNotFound(err) {
				return playercharacter.ErrInvalidCommand
			}
			return fmt.Errorf("锁定活动 PlayerCharacter 账号: %w", err)
		}
		digest, err := idempotency.Digest(struct {
			PlayerCharacterID snowflake.ID
			ExpectedVersion   int64
		}{record.PlayerCharacterID, record.ExpectedVersion})
		if err != nil {
			return fmt.Errorf("计算活动 PlayerCharacter 幂等摘要: %w", err)
		}
		request := idempotency.Request{
			ActorAccountID: record.AccountID, OperationID: switchActiveOperationID,
			Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt,
		}
		replay, err := claimResponse(ctx, idempotency.NewEntRecords(client, s.newID), request, &result)
		if err != nil {
			return err
		}
		if replay {
			result.Replayed = true
			return nil
		}
		current, err := client.ActivePlayerCharacter.Query().Where(activeplayercharacter.IDEQ(record.AccountID)).Only(ctx)
		if err == nil {
			result.PreviousPlayerCharacterID = snowflake.ID(current.PlayerCharacterID)
		} else if !avalonent.IsNotFound(err) {
			return fmt.Errorf("查询切换前活动 PlayerCharacter: %w", err)
		}
		target, err := client.PlayerCharacter.Query().Where(entpc.IDEQ(record.PlayerCharacterID), entpc.AccountIDEQ(record.AccountID), entpc.ArchivedAtIsNil()).Only(ctx)
		if avalonent.IsNotFound(err) {
			return playercharacter.ErrActiveBindingConflict
		}
		if err != nil {
			return fmt.Errorf("查询待激活 PlayerCharacter: %w", err)
		}
		var binding *avalonent.ActivePlayerCharacter
		if record.ExpectedVersion == 0 {
			if current != nil {
				return playercharacter.ErrActiveBindingConflict
			}
			binding, err = client.ActivePlayerCharacter.Create().SetID(record.AccountID).SetPlayerCharacterID(target.ID).SetVersion(1).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
		} else {
			if current == nil || current.Version != record.ExpectedVersion {
				return playercharacter.ErrActiveBindingConflict
			}
			binding, err = client.ActivePlayerCharacter.UpdateOne(current).Where(activeplayercharacter.VersionEQ(record.ExpectedVersion)).SetPlayerCharacterID(target.ID).SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
		}
		if avalonent.IsNotFound(err) {
			return playercharacter.ErrActiveBindingConflict
		}
		if err != nil {
			return fmt.Errorf("切换活动 PlayerCharacter: %w", err)
		}
		result.Binding = playercharacter.ActiveBinding{AccountID: snowflake.ID(binding.ID), PlayerCharacterID: snowflake.ID(binding.PlayerCharacterID), Version: binding.Version, UpdatedAt: binding.UpdatedAt.UTC()}
		if err := s.recordActiveAudit(ctx, executor, record, result); err != nil {
			return err
		}
		if err := completeResponse(ctx, idempotency.NewEntRecords(client, s.newID), request, result); err != nil {
			return fmt.Errorf("保存活动 PlayerCharacter 幂等结果: %w", err)
		}
		return nil
	})
	return result, err
}

type transactionRepository struct {
	parent   *repository
	client   *avalonent.Client
	records  idempotency.RecordStore
	executor database.Transaction
}

func (w *transactionRepository) countUnarchived(ctx context.Context, accountID snowflake.ID) (int64, error) {
	count, err := w.client.PlayerCharacter.Query().Where(entpc.AccountIDEQ(accountID), entpc.ArchivedAtIsNil()).Count(ctx)
	if err != nil {
		return 0, fmt.Errorf("统计未归档 PlayerCharacter: %w", err)
	}
	return int64(count), nil
}

func (w *transactionRepository) sensitiveNameBlocked(ctx context.Context, moderationKey string) (bool, error) {
	rules, err := w.client.PlayerCharacterSensitiveNameRule.Query().Where(playercharactersensitivenamerule.EnabledEQ(true)).All(ctx)
	if err != nil {
		return false, fmt.Errorf("检查 PlayerCharacter 敏感名称: %w", err)
	}
	for _, rule := range rules {
		if rule.MatchType == "exact" && moderationKey == rule.NormalizedTerm ||
			rule.MatchType == "contains" && strings.Contains(moderationKey, rule.NormalizedTerm) {
			return true, nil
		}
	}
	return false, nil
}

func (w *transactionRepository) Create(
	ctx context.Context,
	record playercharacter.CreateRecord,
) (playercharacter.PlayerCharacter, error) {
	digest, err := idempotency.Digest(struct {
		DisplayNameKey string
	}{record.PlayerCharacter.DisplayNameKey})
	if err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("计算 PlayerCharacter 创建幂等摘要: %w", err)
	}
	request := idempotency.Request{
		ActorAccountID: record.PlayerCharacter.AccountID, OperationID: createOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.PlayerCharacter.CreatedAt,
	}
	created := record.PlayerCharacter
	replay, err := claimResponse(ctx, w.records, request, &created)
	if err != nil || replay {
		return created, err
	}
	count, err := w.countUnarchived(ctx, created.AccountID)
	if err != nil {
		return playercharacter.PlayerCharacter{}, err
	}
	if count >= playercharacter.MaximumUnarchivedPerAccount {
		return playercharacter.PlayerCharacter{}, playercharacter.ErrActiveLimitExceeded
	}
	blocked, err := w.sensitiveNameBlocked(ctx, record.ModerationKey)
	if err != nil {
		return playercharacter.PlayerCharacter{}, err
	}
	if blocked {
		return playercharacter.PlayerCharacter{}, playercharacter.ErrSensitiveDisplayName
	}
	row, err := w.client.PlayerCharacter.Create().SetID(created.ID).
		SetAccountID(created.AccountID).SetDisplayName(created.DisplayName).
		SetDisplayNameKey(created.DisplayNameKey).SetVersion(created.Version).
		SetCreatedAt(created.CreatedAt.UTC()).SetUpdatedAt(created.UpdatedAt.UTC()).Save(ctx)
	if err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("创建 PlayerCharacter: %w", err)
	}
	created = playerCharacterFromEnt(row)
	if err := w.initializeRPGWorld(ctx, created); err != nil {
		return playercharacter.PlayerCharacter{}, err
	}
	if err := w.claimDisplayName(ctx, created.ID, created.DisplayName, created.DisplayNameKey, created.CreatedAt); err != nil {
		return playercharacter.PlayerCharacter{}, err
	}
	if err := w.recordAudit(ctx, created.AccountID, "player-character.created", created, record.RequestID, created.CreatedAt); err != nil {
		return playercharacter.PlayerCharacter{}, err
	}
	if err := completeResponse(ctx, w.records, request, created); err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("保存 PlayerCharacter 创建幂等结果: %w", err)
	}
	return created, nil
}

// initializeRPGWorld 在角色创建事务中写入出生位置、首次发现和空 Party 根。
func (w *transactionRepository) initializeRPGWorld(ctx context.Context, created playercharacter.PlayerCharacter) error {
	spawn, err := w.client.RpgLocation.Query().Where(rpglocation.DefaultSpawnEQ(true), rpglocation.EnabledEQ(true)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return errors.New("不存在启用的 RPG 默认出生地点")
	}
	if err != nil {
		return fmt.Errorf("查询 RPG 默认出生地点: %w", err)
	}
	positionID, err := w.parent.newID.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成 PlayerCharacter 初始位置标识: %w", err)
	}
	if _, err := w.client.PlayerCharacterPosition.Create().SetID(positionID).SetPlayerCharacterID(created.ID).SetLocationID(spawn.ID).SetMoveSequence(0).SetVersion(1).SetUpdatedAt(created.CreatedAt.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("创建 PlayerCharacter 初始位置: %w", err)
	}
	discoveredLocationID, err := w.parent.newID.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成 PlayerCharacter 地点发现标识: %w", err)
	}
	if _, err := w.client.PlayerCharacterDiscoveredLocation.Create().SetID(discoveredLocationID).SetPlayerCharacterID(created.ID).SetLocationID(spawn.ID).SetSource("admin").SetDiscoveredAt(created.CreatedAt.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("创建 PlayerCharacter 首次地点发现: %w", err)
	}
	outgoing, err := w.client.RpgLocationExit.Query().Where(rpglocationexit.SourceLocationIDEQ(spawn.ID), rpglocationexit.EnabledEQ(true)).All(ctx)
	if err != nil {
		return fmt.Errorf("查询出生地点出口: %w", err)
	}
	for _, exit := range outgoing {
		discoveredExitID, idErr := w.parent.newID.Next(ctx)
		if idErr != nil {
			return fmt.Errorf("生成 PlayerCharacter 出口发现标识: %w", idErr)
		}
		if _, err := w.client.PlayerCharacterDiscoveredExit.Create().SetID(discoveredExitID).SetPlayerCharacterID(created.ID).SetLocationExitID(exit.ID).SetDiscoveredAt(created.CreatedAt.UTC()).Save(ctx); err != nil {
			return fmt.Errorf("创建出生地点出口发现: %w", err)
		}
	}
	partyID, err := w.parent.newID.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成 PlayerCharacter Party 标识: %w", err)
	}
	if _, err := w.client.PlayerCharacterParty.Create().SetID(partyID).SetPlayerCharacterID(created.ID).SetName("探索 Party").SetVersion(1).SetCreatedAt(created.CreatedAt.UTC()).SetUpdatedAt(created.CreatedAt.UTC()).Save(ctx); err != nil {
		return fmt.Errorf("创建 PlayerCharacter RPG Party: %w", err)
	}
	return nil
}

func (w *transactionRepository) Rename(
	ctx context.Context,
	record playercharacter.RenameRecord,
) (playercharacter.PlayerCharacter, error) {
	digest, err := idempotency.Digest(struct {
		PlayerCharacterID snowflake.ID
		ExpectedVersion   int64
		DisplayNameKey    string
	}{record.PlayerCharacterID, record.ExpectedVersion, record.DisplayNameKey})
	if err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("计算 PlayerCharacter 改名幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: record.AccountID, OperationID: renameOperationID,
		Key: record.IdempotencyKey, RequestDigest: digest, CreatedAt: record.UpdatedAt}
	var renamed playercharacter.PlayerCharacter
	replay, err := claimResponse(ctx, w.records, request, &renamed)
	if err != nil || replay {
		return renamed, err
	}
	blocked, err := w.sensitiveNameBlocked(ctx, record.ModerationKey)
	if err != nil {
		return playercharacter.PlayerCharacter{}, err
	}
	if blocked {
		return playercharacter.PlayerCharacter{}, playercharacter.ErrSensitiveDisplayName
	}
	retired, err := w.client.PlayerCharacterDisplayNameHistory.Update().Where(
		playercharacterdisplaynamehistory.PlayerCharacterIDEQ(record.PlayerCharacterID),
		playercharacterdisplaynamehistory.RetiredAtIsNil(),
	).SetRetiredAt(record.UpdatedAt.UTC()).Save(ctx)
	if err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("退役 PlayerCharacter 当前名称: %w", err)
	}
	if retired != 1 {
		return playercharacter.PlayerCharacter{}, playercharacter.ErrVersionConflict
	}
	if err := w.claimDisplayName(
		ctx, record.PlayerCharacterID, record.DisplayName, record.DisplayNameKey, record.UpdatedAt,
	); err != nil {
		return playercharacter.PlayerCharacter{}, err
	}
	current, err := w.client.PlayerCharacter.Query().Where(entpc.IDEQ(record.PlayerCharacterID), entpc.AccountIDEQ(record.AccountID), entpc.ArchivedAtIsNil()).Only(ctx)
	if avalonent.IsNotFound(err) {
		return playercharacter.PlayerCharacter{}, playercharacter.ErrVersionConflict
	}
	if err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("查询待改名 PlayerCharacter: %w", err)
	}
	row, err := w.client.PlayerCharacter.UpdateOne(current).Where(entpc.VersionEQ(record.ExpectedVersion)).
		SetDisplayName(record.DisplayName).SetDisplayNameKey(record.DisplayNameKey).
		SetVersion(record.ExpectedVersion + 1).SetUpdatedAt(record.UpdatedAt.UTC()).Save(ctx)
	if avalonent.IsNotFound(err) {
		return playercharacter.PlayerCharacter{}, playercharacter.ErrVersionConflict
	}
	if err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("改名 PlayerCharacter: %w", err)
	}
	renamed = playerCharacterFromEnt(row)
	if err := w.recordAudit(ctx, record.AccountID, "player-character.renamed", renamed, record.RequestID, record.UpdatedAt); err != nil {
		return playercharacter.PlayerCharacter{}, err
	}
	if err := completeResponse(ctx, w.records, request, renamed); err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("保存 PlayerCharacter 改名幂等结果: %w", err)
	}
	return renamed, nil
}

func (w *transactionRepository) Archive(
	ctx context.Context,
	record playercharacter.ArchiveRecord,
) (playercharacter.PlayerCharacter, error) {
	return w.changeArchiveState(ctx, archiveOperationID, record.AccountID, record.PlayerCharacterID,
		record.ExpectedVersion, record.IdempotencyKey, record.RequestID, record.ArchivedAt, true)
}

func (w *transactionRepository) Restore(
	ctx context.Context,
	record playercharacter.RestoreRecord,
) (playercharacter.PlayerCharacter, error) {
	return w.changeArchiveState(ctx, restoreOperationID, record.AccountID, record.PlayerCharacterID,
		record.ExpectedVersion, record.IdempotencyKey, record.RequestID, record.RestoredAt, false)
}

func (w *transactionRepository) changeArchiveState(
	ctx context.Context,
	operationID string,
	accountID snowflake.ID,
	playerCharacterID snowflake.ID,
	expectedVersion int64,
	idempotencyKey string,
	requestID string,
	changedAt time.Time,
	archive bool,
) (playercharacter.PlayerCharacter, error) {
	digest, err := idempotency.Digest(struct {
		PlayerCharacterID snowflake.ID
		ExpectedVersion   int64
	}{playerCharacterID, expectedVersion})
	if err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("计算 PlayerCharacter 生命周期幂等摘要: %w", err)
	}
	request := idempotency.Request{ActorAccountID: accountID, OperationID: operationID,
		Key: idempotencyKey, RequestDigest: digest, CreatedAt: changedAt}
	var changed playercharacter.PlayerCharacter
	replay, err := claimResponse(ctx, w.records, request, &changed)
	if err != nil || replay {
		return changed, err
	}
	if !archive {
		count, countErr := w.countUnarchived(ctx, accountID)
		if countErr != nil {
			return playercharacter.PlayerCharacter{}, countErr
		}
		if count >= playercharacter.MaximumUnarchivedPerAccount {
			return playercharacter.PlayerCharacter{}, playercharacter.ErrActiveLimitExceeded
		}
	}
	if archive {
		current, queryErr := w.client.PlayerCharacter.Query().Where(entpc.IDEQ(playerCharacterID), entpc.AccountIDEQ(accountID), entpc.ArchivedAtIsNil()).Only(ctx)
		if avalonent.IsNotFound(queryErr) {
			return playercharacter.PlayerCharacter{}, playercharacter.ErrVersionConflict
		}
		if queryErr != nil {
			return playercharacter.PlayerCharacter{}, fmt.Errorf("查询待归档 PlayerCharacter: %w", queryErr)
		}
		row, updateErr := w.client.PlayerCharacter.UpdateOne(current).Where(entpc.VersionEQ(expectedVersion)).SetArchivedAt(changedAt.UTC()).SetVersion(expectedVersion + 1).SetUpdatedAt(changedAt.UTC()).Save(ctx)
		if avalonent.IsNotFound(updateErr) {
			return playercharacter.PlayerCharacter{}, playercharacter.ErrVersionConflict
		}
		if updateErr == nil {
			changed = playerCharacterFromEnt(row)
			_, updateErr = w.client.ActivePlayerCharacter.Delete().Where(activeplayercharacter.IDEQ(accountID), activeplayercharacter.PlayerCharacterIDEQ(playerCharacterID)).Exec(ctx)
		}
		err = updateErr
	} else {
		current, queryErr := w.client.PlayerCharacter.Query().Where(entpc.IDEQ(playerCharacterID), entpc.AccountIDEQ(accountID)).Only(ctx)
		if avalonent.IsNotFound(queryErr) {
			return playercharacter.PlayerCharacter{}, playercharacter.ErrVersionConflict
		}
		if queryErr != nil {
			return playercharacter.PlayerCharacter{}, fmt.Errorf("查询待恢复 PlayerCharacter: %w", queryErr)
		}
		if current.ArchivedAt == nil {
			return playercharacter.PlayerCharacter{}, playercharacter.ErrVersionConflict
		}
		row, updateErr := w.client.PlayerCharacter.UpdateOne(current).Where(entpc.VersionEQ(expectedVersion)).ClearArchivedAt().SetVersion(expectedVersion + 1).SetUpdatedAt(changedAt.UTC()).Save(ctx)
		if avalonent.IsNotFound(updateErr) {
			return playercharacter.PlayerCharacter{}, playercharacter.ErrVersionConflict
		}
		if updateErr == nil {
			changed = playerCharacterFromEnt(row)
		}
		err = updateErr
	}
	if errors.Is(err, pgx.ErrNoRows) {
		return playercharacter.PlayerCharacter{}, playercharacter.ErrVersionConflict
	}
	if err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("更新 PlayerCharacter 生命周期: %w", err)
	}
	action := "player-character.restored"
	if archive {
		action = "player-character.archived"
	}
	if err := w.recordAudit(ctx, accountID, action, changed, requestID, changedAt); err != nil {
		return playercharacter.PlayerCharacter{}, err
	}
	if err := completeResponse(ctx, w.records, request, changed); err != nil {
		return playercharacter.PlayerCharacter{}, fmt.Errorf("保存 PlayerCharacter 生命周期幂等结果: %w", err)
	}
	return changed, nil
}

func (w *transactionRepository) claimDisplayName(
	ctx context.Context,
	playerCharacterID snowflake.ID,
	displayName string,
	displayNameKey string,
	claimedAt time.Time,
) error {
	value, err := w.client.PlayerCharacterDisplayNameHistory.Query().Where(playercharacterdisplaynamehistory.DisplayNameKeyEQ(displayNameKey)).Only(ctx)
	if avalonent.IsNotFound(err) {
		historyID, idErr := w.parent.newID.Next(ctx)
		if idErr != nil {
			return fmt.Errorf("生成 PlayerCharacter 展示名称历史标识: %w", idErr)
		}
		_, err = w.client.PlayerCharacterDisplayNameHistory.Create().SetID(historyID).
			SetDisplayNameKey(displayNameKey).SetPlayerCharacterID(playerCharacterID).SetDisplayName(displayName).
			SetClaimedAt(claimedAt.UTC()).Save(ctx)
		if isUniqueViolation(err) {
			return playercharacter.ErrDisplayNameUnavailable
		}
		if err != nil {
			return fmt.Errorf("占用 PlayerCharacter 展示名称: %w", err)
		}
		return nil
	}
	if err != nil {
		return fmt.Errorf("查询 PlayerCharacter 展示名称: %w", err)
	}
	if value.PlayerCharacterID != playerCharacterID {
		return playercharacter.ErrDisplayNameUnavailable
	}
	_, err = w.client.PlayerCharacterDisplayNameHistory.UpdateOne(value).SetDisplayName(displayName).SetClaimedAt(claimedAt.UTC()).ClearRetiredAt().Save(ctx)
	if err != nil {
		return fmt.Errorf("更新 PlayerCharacter 展示名称: %w", err)
	}
	return nil
}

func (w *transactionRepository) recordAudit(
	ctx context.Context,
	accountID snowflake.ID,
	action string,
	value playercharacter.PlayerCharacter,
	requestID string,
	createdAt time.Time,
) error {
	auditID, err := w.parent.newID.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成 PlayerCharacter 审计标识: %w", err)
	}
	changes, err := json.Marshal(struct {
		After playercharacter.PlayerCharacter `json:"after"`
	}{value})
	if err != nil {
		return fmt.Errorf("编码 PlayerCharacter 审计变化: %w", err)
	}
	if err := platformaudit.Append(ctx, w.executor, platformaudit.AdministrationLedger, platformaudit.Entry{
		ID: auditID, ActorAccountID: &accountID, ActorKind: "account",
		ActionCode: action, ObjectType: "player_character", ObjectID: stringPointer(value.ID.String()),
		RequestID: requestID, Changes: changes, CreatedAt: createdAt.UTC(),
	}); err != nil {
		return fmt.Errorf("记录 PlayerCharacter 审计: %w", err)
	}
	return nil
}

func (s *repository) recordActiveAudit(
	ctx context.Context,
	executor database.Transaction,
	record playercharacter.SwitchActiveRecord,
	result playercharacter.SwitchActiveResult,
) error {
	auditID, err := s.newID.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成活动 PlayerCharacter 审计标识: %w", err)
	}
	var before *snowflake.ID
	if result.PreviousPlayerCharacterID != snowflake.ID(0) {
		value := result.PreviousPlayerCharacterID
		before = &value
	}
	changes, err := json.Marshal(struct {
		Before *snowflake.ID                 `json:"before,omitempty"`
		After  playercharacter.ActiveBinding `json:"after"`
	}{before, result.Binding})
	if err != nil {
		return fmt.Errorf("编码活动 PlayerCharacter 审计变化: %w", err)
	}
	if err := platformaudit.Append(ctx, executor, platformaudit.AdministrationLedger, platformaudit.Entry{
		ID: auditID, ActorAccountID: &record.AccountID, ActorKind: "account",
		ActionCode: "player-character.active-changed", ObjectType: "active_player_character",
		ObjectID: stringPointer(result.Binding.PlayerCharacterID.String()), RequestID: record.RequestID,
		Changes: changes, CreatedAt: record.UpdatedAt.UTC(),
	}); err != nil {
		return fmt.Errorf("记录活动 PlayerCharacter 审计: %w", err)
	}
	return nil
}

// stringPointer 返回审计条目使用的稳定可选字符串。
func stringPointer(value string) *string { return &value }

// lockAccount 在当前事务中锁定账号行，串行化角色上限、改名和活动绑定变更。
func lockAccount(ctx context.Context, client *avalonent.Client, accountID snowflake.ID) error {
	// 对安全版本执行加零更新会获得 PostgreSQL 行锁，但不会改变任何业务值，
	// 从而在纯 Ent 写入路径中保留账号级串行化语义。
	_, err := client.Account.UpdateOneID(accountID).
		Where(account.IDEQ(accountID)).AddSecurityVersion(0).Save(ctx)
	if avalonent.IsNotFound(err) {
		return err
	}
	return err
}

func databaseTime(value time.Time) pgtype.Timestamptz {
	return pgtype.Timestamptz{Time: value.UTC(), Valid: true}
}

// playerCharacterFromEnt 将 Ent 生成实体转换为领域 PlayerCharacter，集中处理 Identifier 与时间边界。
func playerCharacterFromEnt(row *avalonent.PlayerCharacter) playercharacter.PlayerCharacter {
	value := playercharacter.PlayerCharacter{ID: snowflake.ID(row.ID), AccountID: snowflake.ID(row.AccountID), DisplayName: row.DisplayName, DisplayNameKey: row.DisplayNameKey, Version: row.Version, CreatedAt: row.CreatedAt.UTC(), UpdatedAt: row.UpdatedAt.UTC()}
	if row.ArchivedAt != nil {
		archived := row.ArchivedAt.UTC()
		value.ArchivedAt = &archived
	}
	return value
}

func isUniqueViolation(err error) bool {
	var databaseError *pgconn.PgError
	return errors.As(err, &databaseError) && databaseError.Code == "23505"
}
