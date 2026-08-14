//go:build integration

package rpg_test

import (
	"context"
	"encoding/json"
	"errors"
	"testing"
	"time"

	adminstore "github.com/lishangbu/avalon/internal/admin/store"
	"github.com/lishangbu/avalon/internal/battle"
	battlepersistence "github.com/lishangbu/avalon/internal/battle/persistence"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/lishangbu/avalon/internal/platform/snowflake"
	"github.com/lishangbu/avalon/internal/rpg"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

const encounterPostgresImage = "postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7"

// TestEncounterBattleRunsToCheckpointRecovery 验证 Encounter 接受、Runtime 启动、终局生命写回与
// Checkpoint 恢复共享同一套 PostgreSQL 权威事实，并且客户端重试不会重复创建或结算 Battle。
func TestEncounterBattleRunsToCheckpointRecovery(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	fixture := seedEncounterBattleFixture(t, ctx, pool)
	world := rpg.NewEntWorldStore(pool, snowflake.NewTestID)
	repository := battlepersistence.NewAdapters(pool, snowflake.NewTestID, world)

	command := rpg.ResolveEncounterCommand{
		AccountID: fixture.accountID, PendingEncounterID: fixture.pendingEncounterID,
		Resolution: rpg.EncounterResolutionAccept, IdempotencyKey: "accept-encounter-integration", Now: fixture.createdAt,
	}
	accepted, err := world.ResolvePendingEncounter(ctx, command)
	if err != nil {
		t.Fatalf("ResolvePendingEncounter() error = %v", err)
	}
	if accepted.State != "accepted" || accepted.BattleID == 0 {
		t.Fatalf("ResolvePendingEncounter() = %+v", accepted)
	}
	replayed, err := world.ResolvePendingEncounter(ctx, command)
	if err != nil || replayed.BattleID != accepted.BattleID || replayed.State != accepted.State {
		t.Fatalf("ResolvePendingEncounter(replay) = %+v, error = %v", replayed, err)
	}
	var battleCount int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM battle WHERE pending_encounter_id = $1`, fixture.pendingEncounterID).Scan(&battleCount); err != nil || battleCount != 1 {
		t.Fatalf("Encounter Battle 数量 = %d, error = %v", battleCount, err)
	}

	session, err := repository.Get(ctx, accepted.BattleID)
	if err != nil {
		t.Fatalf("Get() error = %v", err)
	}
	if session.Status != battle.StatusRunning || !session.StartedAt.IsZero() || len(session.Participants) != 2 || len(session.PreviewSubmissions) != 2 {
		t.Fatalf("接受后的 Battle = %+v", session)
	}
	var frozenFormat battleformat.Format
	if err := json.Unmarshal(session.BattleFormatSnapshot, &frozenFormat); err != nil ||
		len(frozenFormat.ClauseIDs) != 1 || frozenFormat.ClauseIDs[0] != snowflake.MustParse("1048576003") ||
		len(frozenFormat.RestrictionIDs) != 1 || frozenFormat.RestrictionIDs[0] != snowflake.MustParse("1048576004") ||
		len(frozenFormat.MechanicIDs) != 1 || frozenFormat.MechanicIDs[0] != snowflake.MustParse("1048576005") {
		t.Fatalf("Encounter BattleFormat 快照 = %+v, error = %v", frozenFormat, err)
	}
	player := session.Participants[0]
	if player.Party == nil || len(player.Party.Team.Members) != 1 || len(player.Party.Members) != 1 ||
		player.Party.Members[0].PlayerCharacterCreatureID != fixture.ownedCreatureID || player.Party.Members[0].CurrentHP != 60 || player.Party.Members[0].MaximumHP != 110 {
		t.Fatalf("Encounter Party 快照 = %+v", player.Party)
	}
	if player.Party.Loot == nil || player.Party.Loot.LootTableID != fixture.lootTableID || player.Party.Loot.LootEntryID != fixture.lootEntryID || player.Party.Loot.ItemID != fixture.lootItemID || player.Party.Loot.Quantity < 1 || player.Party.Loot.Quantity > 3 {
		t.Fatalf("Encounter Loot 快照 = %+v", player.Party.Loot)
	}
	pendingIDs, err := repository.ListPendingRuntimeBattleIDs(ctx)
	if err != nil || !containsID(pendingIDs, accepted.BattleID) {
		t.Fatalf("ListPendingRuntimeBattleIDs() = %v, error = %v", pendingIDs, err)
	}

	lease, err := repository.AcquireRuntimeLease(ctx, accepted.BattleID, "encounter-integration-server")
	if err != nil {
		t.Fatalf("AcquireRuntimeLease() error = %v", err)
	}
	initial := encounterInitialState()
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 7)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	randomSnapshot, err := random.Snapshot()
	if err != nil {
		t.Fatalf("Snapshot() error = %v", err)
	}
	if _, err := repository.Start(ctx, lease, initial, randomSnapshot, fixture.createdAt.Add(time.Second)); err != nil {
		t.Fatalf("Start() error = %v", err)
	}
	completedAt := fixture.createdAt.Add(61 * time.Second)
	completed, err := repository.CompleteBattleTimeout(ctx, accepted.BattleID, completedAt)
	if err != nil {
		t.Fatalf("CompleteBattleTimeout() error = %v", err)
	}
	if completed.Status != battle.StatusCompleted || completed.TerminalReason != string(battle.TerminalReasonBattleTimeout) {
		t.Fatalf("CompleteBattleTimeout() = %+v", completed)
	}

	var currentHP int32
	var creatureVersion int64
	if err := pool.QueryRow(ctx, `SELECT current_hp, version FROM player_character_creature WHERE id = $1`, fixture.ownedCreatureID).Scan(&currentHP, &creatureVersion); err != nil {
		t.Fatalf("读取终局 Owned Creature: %v", err)
	}
	var locationID snowflake.ID
	var moveSequence, positionVersion int64
	if err := pool.QueryRow(ctx, `SELECT location_id, move_sequence, version FROM player_character_position WHERE player_character_id = $1`, fixture.playerCharacterID).Scan(&locationID, &moveSequence, &positionVersion); err != nil {
		t.Fatalf("读取终局 Player Position: %v", err)
	}
	if currentHP != 110 || creatureVersion != 2 || locationID != fixture.checkpointLocationID || moveSequence != 2 || positionVersion != 2 {
		t.Fatalf("Checkpoint 恢复结果 = hp %d, creature version %d, location %s, move %d, position version %d", currentHP, creatureVersion, locationID, moveSequence, positionVersion)
	}
	// 直接检查权威摘要，确保终局事务结果先作为不可变历史持久化，而不是仅由管理端从当前 RPG 状态推断。
	var authoritativeSummary []byte
	if err := pool.QueryRow(ctx, `SELECT summary FROM battle_authoritative_summary WHERE battle_id = $1`, accepted.BattleID).Scan(&authoritativeSummary); err != nil {
		t.Fatalf("读取 Encounter Battle 权威摘要: %v", err)
	}
	var terminalPayload struct {
		EncounterTerminal *battle.EncounterTerminalResult `json:"encounterTerminal"`
	}
	if err := json.Unmarshal(authoritativeSummary, &terminalPayload); err != nil || terminalPayload.EncounterTerminal == nil ||
		!terminalPayload.EncounterTerminal.Defeated || !terminalPayload.EncounterTerminal.CheckpointRecovered ||
		terminalPayload.EncounterTerminal.RecoveryLocationID != fixture.checkpointLocationID ||
		len(terminalPayload.EncounterTerminal.Members) != 1 || terminalPayload.EncounterTerminal.Members[0].CurrentHP != 110 {
		t.Fatalf("Encounter 终局权威摘要 = %+v, error = %v", terminalPayload, err)
	}
	if _, err := repository.CompleteBattleTimeout(ctx, accepted.BattleID, completedAt.Add(time.Second)); !errors.Is(err, battle.ErrBattleNotRunning) {
		t.Fatalf("CompleteBattleTimeout(replay) error = %v, want ErrBattleNotRunning", err)
	}
	var replayHP int32
	var replayCreatureVersion int64
	if err := pool.QueryRow(ctx, `SELECT current_hp, version FROM player_character_creature WHERE id = $1`, fixture.ownedCreatureID).Scan(&replayHP, &replayCreatureVersion); err != nil {
		t.Fatalf("读取重复终局后的 Owned Creature: %v", err)
	}
	if replayHP != currentHP || replayCreatureVersion != creatureVersion {
		t.Fatalf("重复终局改写 Owned Creature = hp %d, version %d", replayHP, replayCreatureVersion)
	}
	var defeatedSettlements int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM player_character_loot_settlement WHERE source_type = 'battle' AND source_reference_id = $1`, accepted.BattleID).Scan(&defeatedSettlements); err != nil || defeatedSettlements != 0 {
		t.Fatalf("落败 Encounter Settlement 数量 = %d, error = %v", defeatedSettlements, err)
	}
	operations, err := adminstore.NewBattleOperationsStore(pool).GetBattleOperationsDetail(ctx, accepted.BattleID)
	if err != nil {
		t.Fatalf("GetBattleOperationsDetail() error = %v", err)
	}
	if operations.Encounter == nil || !operations.Encounter.PlayerDefeated || !operations.Encounter.CheckpointRecovered ||
		operations.Encounter.CheckpointID == 0 || operations.Encounter.RecoveryLocationID != fixture.checkpointLocationID ||
		len(operations.Encounter.RecoveredMembers) != 1 || operations.Encounter.RecoveredMembers[0].CurrentHP != 110 ||
		len(operations.Participants) != 2 || len(operations.Participants[0].FrozenMembers) != 1 ||
		operations.Participants[0].FrozenMembers[0].PlayerCharacterCreatureID != fixture.ownedCreatureID ||
		operations.Participants[1].FrozenMembers[0].CreatureID == 0 {
		t.Fatalf("Encounter Battle 运维详情 = %+v", operations)
	}
}

// TestEncounterVictoryCreatesClaimableDeterministicLoot 验证胜利终局按冻结快照建立唯一可领取结算并交付道具。
func TestEncounterVictoryCreatesClaimableDeterministicLoot(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	fixture := seedEncounterBattleFixture(t, ctx, pool)
	world := rpg.NewEntWorldStore(pool, snowflake.NewTestID)
	battleID := snowflake.NewTestID()
	loot := &battle.EncounterLootSnapshot{LootTableID: fixture.lootTableID, LootEntryID: fixture.lootEntryID, ItemID: fixture.lootItemID, Quantity: 2, RandomAlgorithm: "hmac-sha256-v1", EntryDrawNumber: 3, QuantityDrawNumber: 4}
	terminal, err := world.HandleEncounterTerminal(ctx, battle.EncounterTerminalCommand{BattleID: battleID, PlayerCharacterID: fixture.playerCharacterID, Members: []battle.EncounterTerminalMember{{PlayerCharacterCreatureID: fixture.ownedCreatureID, CurrentHP: 40, MaximumHP: 110}}, CompletedAt: fixture.createdAt.Add(time.Minute), Loot: loot})
	if err != nil || !terminal.LootSettlementID.IsValid() {
		t.Fatalf("HandleEncounterTerminal(victory) = %+v, error = %v", terminal, err)
	}
	claimed, err := world.ClaimLootSettlement(ctx, rpg.ClaimLootSettlementCommand{AccountID: fixture.accountID, LootSettlementID: terminal.LootSettlementID, IdempotencyKey: "claim-encounter-loot", Now: fixture.createdAt.Add(2 * time.Minute)})
	if err != nil || len(claimed.InventoryStacks) != 1 || claimed.InventoryStacks[0].ItemID != fixture.lootItemID || claimed.InventoryStacks[0].QuantityDelta != 2 {
		t.Fatalf("ClaimLootSettlement() = %+v, error = %v", claimed, err)
	}
	var settlements, entries int
	if err = pool.QueryRow(ctx, `SELECT count(*) FROM player_character_loot_settlement WHERE source_type = 'battle' AND source_reference_id = $1`, battleID).Scan(&settlements); err != nil {
		t.Fatal(err)
	}
	if err = pool.QueryRow(ctx, `SELECT count(*) FROM player_character_loot_settlement_entry WHERE loot_settlement_id = $1`, terminal.LootSettlementID).Scan(&entries); err != nil {
		t.Fatal(err)
	}
	if settlements != 1 || entries != 1 {
		t.Fatalf("Loot Settlement facts = settlements %d entries %d", settlements, entries)
	}
}

// TestConcurrentEncounterAcceptanceCreatesOneBattle 验证两个不同幂等键并发接受同一 Pending Encounter
// 时由行锁串行化状态转换，双方都观察到同一终态且数据库只存在一场 Battle。
func TestConcurrentEncounterAcceptanceCreatesOneBattle(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	fixture := seedEncounterBattleFixture(t, ctx, pool)
	world := rpg.NewEntWorldStore(pool, snowflake.NewTestID)
	type result struct {
		value rpg.PendingEncounter
		err   error
	}
	results := make(chan result, 2)
	for _, key := range []string{"concurrent-accept-one", "concurrent-accept-two"} {
		key := key
		go func() {
			value, err := world.ResolvePendingEncounter(ctx, rpg.ResolveEncounterCommand{AccountID: fixture.accountID, PendingEncounterID: fixture.pendingEncounterID, Resolution: rpg.EncounterResolutionAccept, IdempotencyKey: key, Now: fixture.createdAt})
			results <- result{value: value, err: err}
		}()
	}
	first, second := <-results, <-results
	if first.err != nil || second.err != nil || first.value.BattleID == 0 || first.value.BattleID != second.value.BattleID {
		t.Fatalf("并发接受结果 = first %+v/%v, second %+v/%v", first.value, first.err, second.value, second.err)
	}
	var battleCount int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM battle WHERE pending_encounter_id = $1`, fixture.pendingEncounterID).Scan(&battleCount); err != nil || battleCount != 1 {
		t.Fatalf("并发接受后的 Battle 数量 = %d, error = %v", battleCount, err)
	}
}

// TestEncounterAcceptanceRollsBackInvalidInputs 验证资料不完整、Party 全员倒下或角色已被其它 Battle
// 占用时，接受事务会完整回滚，不留下半成品 Battle、Participant、Preview 或 Reservation。
func TestEncounterAcceptanceRollsBackInvalidInputs(t *testing.T) {
	tests := []struct {
		name   string
		mutate func(context.Context, *testing.T, *database.Pool, encounterFixture)
	}{
		{
			name: "野生技能资料缺失",
			mutate: func(ctx context.Context, t *testing.T, pool *database.Pool, _ encounterFixture) {
				t.Helper()
				if _, err := pool.Exec(ctx, `UPDATE game_creature_skill_learn SET enabled = false`); err != nil {
					t.Fatalf("禁用野生技能资料: %v", err)
				}
			},
		},
		{
			name: "Party 全员倒下",
			mutate: func(ctx context.Context, t *testing.T, pool *database.Pool, fixture encounterFixture) {
				t.Helper()
				if _, err := pool.Exec(ctx, `UPDATE player_character_creature SET current_hp = 0 WHERE id = $1`, fixture.ownedCreatureID); err != nil {
					t.Fatalf("设置 Party 全员倒下: %v", err)
				}
			},
		},
		{
			name: "角色已有 Battle 占用",
			mutate: func(ctx context.Context, t *testing.T, pool *database.Pool, fixture encounterFixture) {
				t.Helper()
				seedConflictingBattleReservation(t, ctx, pool, fixture)
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
			defer cancel()
			pool := startEncounterDatabase(t, ctx)
			fixture := seedEncounterBattleFixture(t, ctx, pool)
			test.mutate(ctx, t, pool, fixture)
			world := rpg.NewEntWorldStore(pool, snowflake.NewTestID)
			_, err := world.ResolvePendingEncounter(ctx, rpg.ResolveEncounterCommand{AccountID: fixture.accountID, PendingEncounterID: fixture.pendingEncounterID, Resolution: rpg.EncounterResolutionAccept, IdempotencyKey: "invalid-accept", Now: fixture.createdAt})
			if err == nil {
				t.Fatal("ResolvePendingEncounter() error = nil")
			}
			var state string
			var battleID *snowflake.ID
			if err := pool.QueryRow(ctx, `SELECT state, battle_id FROM player_character_pending_encounter WHERE id = $1`, fixture.pendingEncounterID).Scan(&state, &battleID); err != nil {
				t.Fatalf("读取回滚后的 Pending Encounter: %v", err)
			}
			var battleCount int
			if err := pool.QueryRow(ctx, `SELECT count(*) FROM battle WHERE pending_encounter_id = $1`, fixture.pendingEncounterID).Scan(&battleCount); err != nil {
				t.Fatalf("统计回滚后的 Encounter Battle: %v", err)
			}
			if state != "pending" || battleID != nil || battleCount != 0 {
				t.Fatalf("接受失败后的事实 = state %q, battle %v, count %d", state, battleID, battleCount)
			}
		})
	}
}

// TestEncounterDefeatWithoutMatchingCheckpointWritesHPOnly 验证恢复条件不满足时仍写回终局生命，
// 但不会恢复到 Checkpoint、不会满血，也不会推进位置版本。
func TestEncounterDefeatWithoutMatchingCheckpointWritesHPOnly(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startEncounterDatabase(t, ctx)
	fixture := seedEncounterBattleFixture(t, ctx, pool)
	if _, err := pool.Exec(ctx, `UPDATE rpg_checkpoint SET recovery_condition = '{"op":"level_gte","value":100}'::jsonb`); err != nil {
		t.Fatalf("设置不满足的 Checkpoint 恢复条件: %v", err)
	}
	world := rpg.NewEntWorldStore(pool, snowflake.NewTestID)
	repository := battlepersistence.NewAdapters(pool, snowflake.NewTestID, world)
	accepted, err := world.ResolvePendingEncounter(ctx, rpg.ResolveEncounterCommand{AccountID: fixture.accountID, PendingEncounterID: fixture.pendingEncounterID, Resolution: rpg.EncounterResolutionAccept, IdempotencyKey: "accept-no-recovery", Now: fixture.createdAt})
	if err != nil {
		t.Fatalf("ResolvePendingEncounter() error = %v", err)
	}
	lease, err := repository.AcquireRuntimeLease(ctx, accepted.BattleID, "encounter-no-recovery-server")
	if err != nil {
		t.Fatalf("AcquireRuntimeLease() error = %v", err)
	}
	random, _ := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 9)
	randomSnapshot, _ := random.Snapshot()
	if _, err := repository.Start(ctx, lease, encounterInitialState(), randomSnapshot, fixture.createdAt.Add(time.Second)); err != nil {
		t.Fatalf("Start() error = %v", err)
	}
	if _, err := repository.CompleteBattleTimeout(ctx, accepted.BattleID, fixture.createdAt.Add(61*time.Second)); err != nil {
		t.Fatalf("CompleteBattleTimeout() error = %v", err)
	}
	var currentHP int32
	var locationID snowflake.ID
	var moveSequence, positionVersion int64
	if err := pool.QueryRow(ctx, `SELECT current_hp FROM player_character_creature WHERE id = $1`, fixture.ownedCreatureID).Scan(&currentHP); err != nil {
		t.Fatalf("读取终局生命: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT location_id, move_sequence, version FROM player_character_position WHERE player_character_id = $1`, fixture.playerCharacterID).Scan(&locationID, &moveSequence, &positionVersion); err != nil {
		t.Fatalf("读取终局位置: %v", err)
	}
	if currentHP != 66 || locationID == fixture.checkpointLocationID || moveSequence != 1 || positionVersion != 1 {
		t.Fatalf("未满足恢复条件的终局 = hp %d, location %s, move %d, version %d", currentHP, locationID, moveSequence, positionVersion)
	}
}

type encounterFixture struct {
	accountID, playerCharacterID, ownedCreatureID snowflake.ID
	pendingEncounterID, checkpointLocationID      snowflake.ID
	lootTableID, lootEntryID, lootItemID          snowflake.ID
	createdAt                                     time.Time
}

func startEncounterDatabase(t *testing.T, ctx context.Context) *database.Pool {
	t.Helper()
	container, err := postgres.Run(ctx, encounterPostgresImage, postgres.WithDatabase("avalon_encounter_test"), postgres.WithUsername("avalon"), postgres.WithPassword("avalon"), postgres.BasicWaitStrategies())
	if err != nil {
		t.Fatalf("启动 PostgreSQL: %v", err)
	}
	t.Cleanup(func() {
		if err := container.Terminate(context.Background()); err != nil {
			t.Errorf("停止 PostgreSQL: %v", err)
		}
	})
	url, err := container.ConnectionString(ctx, "sslmode=disable")
	if err != nil {
		t.Fatalf("读取 PostgreSQL 地址: %v", err)
	}
	pool, err := database.Open(database.Config{URL: url, MaxOpenConnections: 20, MaxIdleConnections: 10})
	if err != nil {
		t.Fatalf("打开数据库: %v", err)
	}
	t.Cleanup(pool.Close)
	if err := pool.Persistence().ApplySchema(ctx, persistence.SchemaModeCreate); err != nil {
		t.Fatalf("创建 Ent Schema: %v", err)
	}
	return pool
}

func seedEncounterBattleFixture(t *testing.T, ctx context.Context, pool *database.Pool) encounterFixture {
	t.Helper()
	ids := make([]snowflake.ID, 25)
	for index := range ids {
		ids[index] = snowflake.NewTestID()
	}
	createdAt := time.Date(2026, time.August, 13, 12, 0, 0, 0, time.UTC)
	fixture := encounterFixture{accountID: ids[0], playerCharacterID: ids[1], ownedCreatureID: ids[17], pendingEncounterID: ids[24], checkpointLocationID: ids[14], lootTableID: snowflake.NewTestID(), lootEntryID: snowflake.NewTestID(), lootItemID: snowflake.NewTestID(), createdAt: createdAt}
	statements := []struct {
		query string
		args  []any
	}{
		{`INSERT INTO account (id, username, username_key, display_name, password_hash, password_algorithm, password_parameters, status, security_version, created_at, updated_at) VALUES ($1, 'encounter-owner', 'encounter-owner', '遭遇玩家', 'test', 'argon2id', '{}', 'active', 1, $2, $2)`, []any{ids[0], createdAt}},
		{`INSERT INTO player_character (id, account_id, display_name, display_name_key, version, created_at, updated_at) VALUES ($1, $2, '遭遇角色', '遭遇角色', 1, $3, $3)`, []any{ids[1], ids[0], createdAt}},
		{`INSERT INTO active_player_character (account_id, player_character_id, version, updated_at) VALUES ($1, $2, 1, $3)`, []any{ids[0], ids[1], createdAt}},
		{`INSERT INTO game_element (id, code, name, sort_order, enabled, version, created_at, updated_at) VALUES ($1, 'encounter-normal', '一般', 1, true, 1, $2, $2)`, []any{ids[2], createdAt}},
		{`INSERT INTO game_species (id, national_dex_number, code, name, gender_differences, forms_switchable, enabled, version, created_at, updated_at) VALUES ($1, 9999, 'encounter-species', '遭遇物种', false, false, true, 1, $2, $2)`, []any{ids[3], createdAt}},
		{`INSERT INTO game_creature (id, code, name, species_id, male_eighths, female_eighths, default_form, enabled, version, created_at, updated_at) VALUES ($1, 'encounter-creature', '遭遇生物', $2, 0, 0, true, true, 1, $3, $3)`, []any{ids[4], ids[3], createdAt}},
		{`INSERT INTO game_creature_form (id, code, name, creature_id, battle_only, default_form, enhanced_form, enabled, version, created_at, updated_at) VALUES ($1, 'encounter-form', '默认形态', $2, false, true, false, true, 1, $3, $3)`, []any{ids[5], ids[4], createdAt}},
		{`INSERT INTO game_creature_form_element (id, form_id, element_id) VALUES ($1, $2, $3)`, []any{ids[6], ids[5], ids[2]}},
		{`INSERT INTO game_ability (id, code, name, main_series, rules, enabled, version, created_at, updated_at) VALUES ($1, 'encounter-ability', '遭遇特性', true, '{}', true, 1, $2, $2)`, []any{ids[7], createdAt}},
		{`INSERT INTO game_creature_ability (id, creature_id, ability_id, hidden, slot, enabled, version, created_at, updated_at) VALUES ($1, $2, $3, false, 1, true, 1, $4, $4)`, []any{ids[8], ids[4], ids[7], createdAt}},
		{`INSERT INTO game_nature (id, code, name, enabled, version, created_at, updated_at) VALUES ($1, 'encounter-neutral', '勤奋', true, 1, $2, $2)`, []any{ids[9], createdAt}},
		{`INSERT INTO game_skill (id, code, name, element_id, priority, rules, enabled, version, created_at, updated_at) VALUES ($1, 'encounter-skill', '撞击', $2, 0, '{}', true, 1, $3, $3)`, []any{ids[10], ids[2], createdAt}},
		{`INSERT INTO game_skill_learn_method (id, code, name, enabled, version, created_at, updated_at) VALUES ($1, 'encounter-level-up', '升级', true, 1, $2, $2)`, []any{ids[11], createdAt}},
		{`INSERT INTO game_creature_skill_learn (id, creature_id, skill_id, learn_method_id, level_learned_at, enabled, version, created_at, updated_at) VALUES ($1, $2, $3, $4, 1, true, 1, $5, $5)`, []any{ids[12], ids[4], ids[10], ids[11], createdAt}},
		{`INSERT INTO game_stat (id, code, name, sort_order, battle_only, enabled, version, created_at, updated_at) VALUES ($1, 'hp', '生命', 1, false, true, 1, $2, $2)`, []any{ids[13], createdAt}},
		{`INSERT INTO game_creature_stat (id, creature_id, stat_id, base_value, effort, enabled, version, created_at, updated_at) VALUES ($1, $2, $3, 50, 0, true, 1, $4, $4)`, []any{snowflake.NewTestID(), ids[4], ids[13], createdAt}},
		{`INSERT INTO rpg_region (id, code, name, enabled, version, created_at, updated_at) VALUES ($1, 'encounter-region', '遭遇区域', true, 1, $2, $2)`, []any{ids[15], createdAt}},
		{`INSERT INTO rpg_location (id, region_id, code, name, location_type, default_spawn, enabled, version, created_at, updated_at) VALUES ($1, $2, 'encounter-checkpoint', '恢复地点', 'settlement', true, true, 1, $3, $3), ($4, $2, 'encounter-wild', '野外地点', 'wild', false, true, 1, $3, $3)`, []any{ids[14], ids[15], createdAt, ids[16]}},
		{`INSERT INTO player_character_creature (id, player_character_id, creature_id, form_id, ability_id, nature_id, origin_type, level, experience, current_hp, friendship, is_egg, hatch_progress, version, acquired_at, updated_at) VALUES ($1, $2, $3, $4, $5, $6, 'capture', 50, 0, 60, 0, false, 0, 1, $7, $7)`, []any{ids[17], ids[1], ids[4], ids[5], ids[7], ids[9], createdAt}},
		{`INSERT INTO player_character_creature_skill (id, player_character_creature_id, position, skill_id, current_pp) VALUES ($1, $2, 1, $3, 35)`, []any{ids[18], ids[17], ids[10]}},
		{`INSERT INTO player_character_creature_stat (id, player_character_creature_id, stat_id, individual_value, effort_value) VALUES ($1, $2, $3, 0, 0)`, []any{snowflake.NewTestID(), ids[17], ids[13]}},
		{`INSERT INTO player_character_party (id, player_character_id, name, version, created_at, updated_at) VALUES ($1, $2, '探索队伍', 1, $3, $3)`, []any{ids[19], ids[1], createdAt}},
		{`INSERT INTO player_character_party_member (id, party_id, position, player_character_creature_id) VALUES ($1, $2, 1, $3)`, []any{ids[20], ids[19], ids[17]}},
		{`INSERT INTO player_character_position (id, player_character_id, location_id, move_sequence, version, updated_at) VALUES ($1, $2, $3, 1, 1, $4)`, []any{snowflake.NewTestID(), ids[1], ids[16], createdAt}},
		{`INSERT INTO rpg_checkpoint (id, location_id, code, name, enabled, version, created_at, updated_at) VALUES ($1, $2, 'encounter-home', '恢复点', true, 1, $3, $3)`, []any{ids[21], ids[14], createdAt}},
		{`INSERT INTO player_character_checkpoint (id, player_character_id, checkpoint_id, version, updated_at) VALUES ($1, $2, $3, 1, $4)`, []any{snowflake.NewTestID(), ids[1], ids[21], createdAt}},
		{`INSERT INTO rpg_location_exit (id, source_location_id, target_location_id, code, name, sort_order, condition, enabled, version, created_at, updated_at) VALUES ($1, $2, $3, 'encounter-exit', '前往野外', 1, '{}', true, 1, $4, $4)`, []any{ids[22], ids[14], ids[16], createdAt}},
		{`INSERT INTO player_character_traversal (id, player_character_id, location_exit_id, source_location_id, target_location_id, position_version_before, position_version_after, idempotency_key, request_digest, response, created_at) VALUES ($1, $2, $3, $4, $5, 1, 2, 'encounter-traversal', decode(repeat('00', 32), 'hex'), '{}', $6)`, []any{ids[23], ids[1], ids[22], ids[14], ids[16], createdAt}},
		{`INSERT INTO rpg_encounter_table (id, location_id, code, name, encounter_method, trigger_probability_bps, cooldown_moves, enabled, version, created_at, updated_at) VALUES ($1, $2, 'encounter-table', '遭遇表', 'walk', 10000, 0, true, 1, $3, $3)`, []any{snowflake.NewTestID(), ids[16], createdAt}},
		{`INSERT INTO game_item (id, code, name, usage_type, cost, enabled, version, created_at, updated_at) VALUES ($1, 'encounter-loot-item', '遭遇掉落', 'material', 0, true, 1, $2, $2)`, []any{fixture.lootItemID, createdAt}},
		{`INSERT INTO rpg_loot_table (id, code, name, enabled, version, created_at, updated_at) VALUES ($1, 'encounter-loot', '遭遇掉落表', true, 1, $2, $2)`, []any{fixture.lootTableID, createdAt}},
		{`INSERT INTO rpg_loot_entry (id, loot_table_id, item_id, minimum_quantity, maximum_quantity, weight) VALUES ($1, $2, $3, 1, 3, 1)`, []any{fixture.lootEntryID, fixture.lootTableID, fixture.lootItemID}},
	}
	for _, statement := range statements {
		if _, err := pool.Exec(ctx, statement.query, statement.args...); err != nil {
			t.Fatalf("创建 Encounter 集成夹具: %v\nSQL: %s", err, statement.query)
		}
	}
	var encounterTableID snowflake.ID
	if err := pool.QueryRow(ctx, `SELECT id FROM rpg_encounter_table WHERE code = 'encounter-table'`).Scan(&encounterTableID); err != nil {
		t.Fatalf("读取 Encounter Table: %v", err)
	}
	entryID := snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `INSERT INTO rpg_encounter_entry (id, encounter_table_id, creature_id, form_id, loot_table_id, minimum_level, maximum_level, weight, enabled) VALUES ($1, $2, $3, $4, $5, 50, 50, 1, true)`, entryID, encounterTableID, ids[4], ids[5], fixture.lootTableID); err != nil {
		t.Fatalf("创建 Encounter Entry: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO player_character_pending_encounter (id, player_character_id, traversal_id, encounter_table_id, encounter_entry_id, encounter_table_version, encounter_level, random_algorithm, random_seed, random_draw_number, random_result, state, expires_at, created_at) VALUES ($1, $2, $3, $4, $5, 1, 50, 'hmac-sha256-v1', decode(repeat('01', 32), 'hex'), 2, '{}', 'pending', $6, $7)`, ids[24], ids[1], ids[23], encounterTableID, entryID, createdAt.Add(10*time.Minute), createdAt); err != nil {
		t.Fatalf("创建 Pending Encounter: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO game_battle_format (id, code, name, description, mode, roster_count, select_count, active_participants_per_side, level_rule, normalized_level, preview_seconds, turn_seconds, battle_seconds, challenge_available, training_available, encounter_available, admin_preview_available, clause_ids, restriction_ids, mechanic_ids, is_default, enabled, version, created_at, updated_at) VALUES ($1, 'standard-single', '标准单打', '', 'single', 1, 1, 1, 'preserve', NULL, 10, 10, 60, false, false, true, false, ARRAY[1048576003]::bigint[], ARRAY[1048576004]::bigint[], ARRAY[1048576005]::bigint[], true, true, 1, $2, $2)`, snowflake.NewTestID(), createdAt); err != nil {
		t.Fatalf("创建 Encounter BattleFormat: %v", err)
	}
	return fixture
}

func encounterInitialState() battleengine.InitialState {
	elementID := snowflake.MustParse("1048576001")
	skillID := snowflake.MustParse("1048576002")
	member := func(position battleengine.MemberPosition, creatureID snowflake.ID, currentHP uint32) battleengine.MemberSnapshot {
		return battleengine.MemberSnapshot{Position: position, CreatureID: creatureID, Level: 50, MaxHP: 100, CurrentHP: currentHP, StatStages: map[battleengine.Stat]int8{}, Stats: battleengine.StatBlock{Attack: 50, Defense: 50, SpecialAttack: 50, SpecialDefense: 50, Speed: 50}, ElementIDs: []battleengine.Identifier{elementID}, Skills: []battleengine.SkillSnapshot{{Position: 1, SkillID: skillID, Name: "等待", ElementID: elementID, DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf, DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, RemainingPP: 10, MaxPP: 10}}}
	}
	return battleengine.InitialState{Format: battleengine.FormatSnapshot{Code: "encounter-integration", ActiveSlotsPerSide: 1, TeamSize: 1}, Rules: battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]battleengine.Identifier{}}, Sides: []battleengine.SideSnapshot{{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{member(1, snowflake.MustParse("1048576101"), 60)}}, {Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{member(1, snowflake.MustParse("1048576102"), 100)}}}}
}

func seedConflictingBattleReservation(t *testing.T, ctx context.Context, pool *database.Pool, fixture encounterFixture) {
	t.Helper()
	formatID := snowflake.NewTestID()
	battleID := snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `INSERT INTO game_battle_format (id, code, name, description, mode, roster_count, select_count, active_participants_per_side, level_rule, normalized_level, preview_seconds, turn_seconds, battle_seconds, challenge_available, training_available, encounter_available, admin_preview_available, clause_ids, restriction_ids, mechanic_ids, is_default, enabled, version, created_at, updated_at) VALUES ($1, 'occupied-single', '占用赛制', '', 'single', 1, 1, 1, 'preserve', NULL, 10, 10, 60, false, true, false, false, '{}'::bigint[], '{}'::bigint[], '{}'::bigint[], false, true, 1, $2, $2)`, formatID, fixture.createdAt); err != nil {
		t.Fatalf("创建占用 BattleFormat: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO battle (id, mode, source_type, status, battle_format_id, battle_format_snapshot, format, preview_deadline_at, battle_deadline_at, state_version, version, created_at, updated_at) VALUES ($1, 'pve', 'training', 'running', $2, '{}', '{}', $3, $4, 0, 1, $5, $5)`, battleID, formatID, fixture.createdAt.Add(10*time.Second), fixture.createdAt.Add(60*time.Second), fixture.createdAt); err != nil {
		t.Fatalf("创建占用 Battle: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO battle_participant_reservation (player_character_id, battle_id, created_at) VALUES ($1, $2, $3)`, fixture.playerCharacterID, battleID, fixture.createdAt); err != nil {
		t.Fatalf("创建角色 Battle 占用: %v", err)
	}
}

func containsID(values []snowflake.ID, expected snowflake.ID) bool {
	for _, value := range values {
		if value == expected {
			return true
		}
	}
	return false
}
