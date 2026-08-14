package persistence

import (
	"context"
	"encoding/json"
	"fmt"

	entsql "entgo.io/ent/dialect/sql"
	"github.com/lishangbu/avalon/ent"
	"github.com/lishangbu/avalon/ent/battle"
	"github.com/lishangbu/avalon/ent/battleauthoritativesummary"
	"github.com/lishangbu/avalon/ent/battleoutbox"
	"github.com/lishangbu/avalon/ent/battleparticipant"
	"github.com/lishangbu/avalon/ent/battlerecoveryattempt"
	"github.com/lishangbu/avalon/ent/battleruntimelease"
	"github.com/lishangbu/avalon/ent/playercharacterpendingencounter"
	"github.com/lishangbu/avalon/internal/admin"
	battledomain "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// battleOperationsQuery 使用 Ent 提供管理端 Battle 运维只读投影。
type battleOperationsQuery struct {
	pool *database.Pool
}

// NewBattleOperationsQuery 创建 Battle 运维只读查询适配器。
func NewBattleOperationsQuery(pool *database.Pool) *battleOperationsQuery {
	return &battleOperationsQuery{pool: pool}
}

// ListBattles 按筛选和稳定倒序分页返回 Battle 摘要。
func (adapter *battleOperationsQuery) ListBattles(ctx context.Context, query admin.BattleOperationsQuery) (admin.BattleOperationsPage, error) {
	if adapter == nil || adapter.pool == nil || query.Page < 1 || query.PageSize < 1 || query.PageSize > 100 {
		return admin.BattleOperationsPage{}, admin.ErrInvalidBattleOperationsQuery
	}
	q := adapter.pool.Client(ctx).Battle.Query()
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
func (adapter *battleOperationsQuery) GetBattleOperationsDetail(ctx context.Context, battleID snowflake.ID) (admin.BattleOperationsDetail, error) {
	if adapter == nil || adapter.pool == nil || battleID == 0 {
		return admin.BattleOperationsDetail{}, admin.ErrBattleOperationsNotFound
	}
	client := adapter.pool.Client(ctx)
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
		value := admin.BattleOperationsParticipant{
			Side: participant.Side, ParticipantType: participant.ParticipantType, InputType: participant.InputType,
			DisplayName: participant.DisplayName, PlayerCharacterID: participant.PlayerCharacterID,
			BotCode: stringValue(participant.BotCode), SourcePartyVersion: int64Value(participant.SourcePartyVersion),
		}
		if participant.SourcePartyID != nil {
			value.SourcePartyID = *participant.SourcePartyID
		}
		value.FrozenMembers, err = frozenParticipantMembers(participant.InputType, participant.InputSnapshot)
		if err != nil {
			return admin.BattleOperationsDetail{}, fmt.Errorf("解析 Battle Participant 冻结输入: %w", err)
		}
		participants = append(participants, value)
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
	encounter, err := adapter.encounterView(ctx, row)
	if err != nil {
		return admin.BattleOperationsDetail{}, err
	}
	return admin.BattleOperationsDetail{
		Battle: battleOperationsItem(row), Participants: participants, RuntimeLease: lease,
		RecoveryAttempts: recoveryAttempts, PendingOutboxCount: pendingOutboxCount, Encounter: encounter,
	}, nil
}

// encounterView 读取 Encounter 固定抽样输入，并在已完成时组合权威摘要中的实际恢复结果。
func (adapter *battleOperationsQuery) encounterView(ctx context.Context, row *ent.Battle) (*admin.BattleOperationsEncounterView, error) {
	if row.SourceType != string(battledomain.BattleSourceEncounter) || row.PendingEncounterID == nil {
		return nil, nil
	}
	client := adapter.pool.Client(ctx)
	pending, err := client.PlayerCharacterPendingEncounter.Query().Where(playercharacterpendingencounter.IDEQ(*row.PendingEncounterID)).Only(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询 Battle Pending Encounter: %w", err)
	}
	result := &admin.BattleOperationsEncounterView{PendingEncounterID: pending.ID, EncounterTableID: pending.EncounterTableID, EncounterEntryID: pending.EncounterEntryID, EncounterLevel: pending.EncounterLevel, State: pending.State, ExpiresAt: pending.ExpiresAt}
	summary, err := client.BattleAuthoritativeSummary.Query().Where(battleauthoritativesummary.IDEQ(row.ID)).Only(ctx)
	if ent.IsNotFound(err) {
		return result, nil
	}
	if err != nil {
		return nil, fmt.Errorf("查询 Encounter Battle 权威摘要: %w", err)
	}
	var payload struct {
		EncounterTerminal *battledomain.EncounterTerminalResult `json:"encounterTerminal"`
	}
	if err := json.Unmarshal(summary.Summary, &payload); err != nil {
		return nil, fmt.Errorf("解析 Encounter Battle 权威摘要: %w", err)
	}
	if payload.EncounterTerminal == nil {
		return result, nil
	}
	terminal := payload.EncounterTerminal
	result.PlayerDefeated = terminal.Defeated
	result.CheckpointRecovered = terminal.CheckpointRecovered
	result.CheckpointID = terminal.CheckpointID
	result.RecoveryLocationID = terminal.RecoveryLocationID
	result.RecoveredMembers = make([]admin.BattleOperationsRecoveredMember, 0, len(terminal.Members))
	for _, member := range terminal.Members {
		result.RecoveredMembers = append(result.RecoveredMembers, admin.BattleOperationsRecoveredMember{PlayerCharacterCreatureID: member.PlayerCharacterCreatureID, CurrentHP: member.CurrentHP, MaximumHP: member.MaximumHP})
	}
	return result, nil
}

// frozenParticipantMembers 只从 Battle 不可变输入快照提取诊断字段。
//
// Party 快照额外关联 Owned Creature 和创建时生命；普通 Team 与生成对手没有这些持久身份，
// 因此保持零值。未知类型或损坏 JSON 会明确失败，避免管理端把不可解析历史误报为空阵容。
func frozenParticipantMembers(inputType string, raw json.RawMessage) ([]admin.BattleOperationsFrozenMember, error) {
	var team battledomain.TeamSnapshot
	var party *battledomain.PartyBattleSnapshot
	switch inputType {
	case "party":
		var value battledomain.PartyBattleSnapshot
		if err := json.Unmarshal(raw, &value); err != nil {
			return nil, err
		}
		team, party = value.Team, &value
	case "team", "generated":
		if err := json.Unmarshal(raw, &team); err != nil {
			return nil, err
		}
	default:
		return nil, fmt.Errorf("不支持的冻结输入类型 %q", inputType)
	}
	result := make([]admin.BattleOperationsFrozenMember, 0, len(team.Members))
	for _, member := range team.Members {
		value := admin.BattleOperationsFrozenMember{Position: member.Position, CreatureID: member.CreatureID, Level: member.Level}
		if party != nil {
			for _, partyMember := range party.Members {
				if int32(partyMember.Position) == member.Position {
					value.PlayerCharacterCreatureID = partyMember.PlayerCharacterCreatureID
					value.CurrentHP, value.MaximumHP = partyMember.CurrentHP, partyMember.MaximumHP
					break
				}
			}
		}
		result = append(result, value)
	}
	return result, nil
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

func int64Value(value *int64) int64 {
	if value == nil {
		return 0
	}
	return *value
}
