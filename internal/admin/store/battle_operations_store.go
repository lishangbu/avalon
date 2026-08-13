package store

import (
	"context"
	"fmt"

	entsql "entgo.io/ent/dialect/sql"
	"github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/battle"
	"github.com/lishangbu/avalon/ent/battleoutbox"
	"github.com/lishangbu/avalon/ent/battleparticipant"
	"github.com/lishangbu/avalon/ent/battlerecoveryattempt"
	"github.com/lishangbu/avalon/ent/battleruntimelease"
	"github.com/lishangbu/avalon/internal/admin"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// BattleOperationsStore 使用 Ent 提供管理端 Battle 运维只读投影。
type BattleOperationsStore struct {
	pool *database.Pool
}

// NewBattleOperationsStore 创建 Battle 运维只读存储。
func NewBattleOperationsStore(pool *database.Pool) *BattleOperationsStore {
	return &BattleOperationsStore{pool: pool}
}

// ListBattles 按筛选和稳定倒序分页返回 Battle 摘要。
func (store *BattleOperationsStore) ListBattles(ctx context.Context, query admin.BattleOperationsQuery) (admin.BattleOperationsPage, error) {
	if store == nil || store.pool == nil || query.Page < 1 || query.PageSize < 1 || query.PageSize > 100 {
		return admin.BattleOperationsPage{}, admin.ErrInvalidBattleOperationsQuery
	}
	q := store.pool.Client(ctx).Battle.Query()
	if query.Mode != "" {
		q = q.Where(battle.ModeEQ(query.Mode))
	}
	if query.SourceType != "" {
		q = q.Where(battle.SourceTypeEQ(query.SourceType))
	}
	if query.Status != "" {
		q = q.Where(battle.StatusEQ(query.Status))
	}
	total, err := q.Count(ctx)
	if err != nil {
		return admin.BattleOperationsPage{}, fmt.Errorf("统计 Battle: %w", err)
	}
	rows, err := q.Order(battle.ByCreatedAt(entsql.OrderDesc()), battle.ByID(entsql.OrderDesc())).Offset((query.Page - 1) * query.PageSize).Limit(query.PageSize).All(ctx)
	if err != nil {
		return admin.BattleOperationsPage{}, fmt.Errorf("查询 Battle: %w", err)
	}
	items := make([]admin.BattleOperationsItem, 0, len(rows))
	for _, row := range rows {
		items = append(items, battleOperationsItem(row))
	}
	return admin.BattleOperationsPage{Items: items, Total: int64(total)}, nil
}

// GetBattleOperationsDetail 返回单场 Battle 的受控运维详情。
func (store *BattleOperationsStore) GetBattleOperationsDetail(ctx context.Context, battleID snowflake.ID) (admin.BattleOperationsDetail, error) {
	if store == nil || store.pool == nil || battleID == 0 {
		return admin.BattleOperationsDetail{}, admin.ErrBattleOperationsNotFound
	}
	client := store.pool.Client(ctx)
	row, err := client.Battle.Query().Where(battle.IDEQ(battleID)).Only(ctx)
	if ent.IsNotFound(err) {
		return admin.BattleOperationsDetail{}, admin.ErrBattleOperationsNotFound
	}
	if err != nil {
		return admin.BattleOperationsDetail{}, fmt.Errorf("查询 Battle: %w", err)
	}
	participantRows, err := client.BattleParticipant.Query().Where(battleparticipant.BattleIDEQ(battleID)).Order(battleparticipant.BySide()).All(ctx)
	if err != nil {
		return admin.BattleOperationsDetail{}, fmt.Errorf("查询 Battle Participant: %w", err)
	}
	participants := make([]admin.BattleOperationsParticipant, 0, len(participantRows))
	for _, participant := range participantRows {
		participants = append(participants, admin.BattleOperationsParticipant{
			Side: participant.Side, ParticipantType: participant.ParticipantType, InputType: participant.InputType,
			DisplayName: participant.DisplayName, PlayerCharacterID: participant.PlayerCharacterID,
			BotCode: stringValue(participant.BotCode),
		})
	}
	leaseRow, err := client.BattleRuntimeLease.Query().Where(battleruntimelease.IDEQ(battleID)).Only(ctx)
	var lease *admin.BattleRuntimeLeaseView
	if err == nil {
		lease = &admin.BattleRuntimeLeaseView{HolderID: leaseRow.HolderID, FencingToken: leaseRow.FencingToken, ExpiresAt: leaseRow.LeaseExpiresAt, RenewedAt: leaseRow.RenewedAt}
	} else if !ent.IsNotFound(err) {
		return admin.BattleOperationsDetail{}, fmt.Errorf("查询 Battle Runtime Lease: %w", err)
	}
	recoveryRows, err := client.BattleRecoveryAttempt.Query().Where(battlerecoveryattempt.BattleIDEQ(battleID)).Order(battlerecoveryattempt.ByAttemptNumber(entsql.OrderDesc())).All(ctx)
	if err != nil {
		return admin.BattleOperationsDetail{}, fmt.Errorf("查询 Battle Recovery Attempt: %w", err)
	}
	recoveryAttempts := make([]admin.BattleRecoveryAttemptView, 0, len(recoveryRows))
	for _, attempt := range recoveryRows {
		recoveryAttempts = append(recoveryAttempts, admin.BattleRecoveryAttemptView{
			ID: attempt.ID, AttemptNumber: attempt.AttemptNumber, State: attempt.State, AvailableAt: attempt.AvailableAt,
			ClaimedBy: stringValue(attempt.ClaimedBy), FailureReason: stringValue(attempt.FailureReason),
		})
	}
	pendingOutboxCount, err := client.BattleOutbox.Query().Where(battleoutbox.BattleIDEQ(battleID), battleoutbox.PublishedAtIsNil()).Count(ctx)
	if err != nil {
		return admin.BattleOperationsDetail{}, fmt.Errorf("统计 Battle Outbox: %w", err)
	}
	return admin.BattleOperationsDetail{
		Battle: battleOperationsItem(row), Participants: participants, RuntimeLease: lease,
		RecoveryAttempts: recoveryAttempts, PendingOutboxCount: pendingOutboxCount,
	}, nil
}

func battleOperationsItem(row *ent.Battle) admin.BattleOperationsItem {
	return admin.BattleOperationsItem{
		ID: row.ID, Mode: row.Mode, SourceType: row.SourceType, Status: row.Status, StateVersion: row.StateVersion,
		TerminalReason: stringValue(row.TerminalReason), CreatedAt: row.CreatedAt, UpdatedAt: row.UpdatedAt,
		StartedAt: row.StartedAt, CompletedAt: row.CompletedAt,
	}
}

func stringValue(value *string) string {
	if value == nil {
		return ""
	}
	return *value
}
