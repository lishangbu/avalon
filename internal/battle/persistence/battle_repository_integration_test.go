//go:build integration

package persistence_test

import (
	"context"
	"encoding/json"
	"errors"
	"path/filepath"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
	battlepersistence "github.com/lishangbu/avalon/internal/battle/persistence"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/administration"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/platform/persistence"
	"github.com/lishangbu/avalon/internal/team"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

const battlePostgresImage = "postgres:18.4@sha256:311136771dca6826c3b6e691ebf8cb6e896e165074bc57a728f9619f25f0c4c7"

// TestRuntimeFencingRejectsStaleTurnCommit 验证租约被另一 Server 接管后，旧 token 无法提交状态或 Turn Record。
func TestRuntimeFencingRejectsStaleTurnCommit(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startBattleDatabase(t, ctx)
	repository := battlepersistence.NewAdapters(pool, snowflake.NewTestID, nil)
	fixture := loadBattleGoldenReplay(t)
	seedBattleDependencies(t, ctx, pool)

	createdAt := time.Date(2026, time.August, 12, 14, 0, 0, 0, time.UTC)
	value := persistedPreviewSession(createdAt)
	if err := repository.Create(ctx, value); err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	if _, err := pool.Exec(ctx, `UPDATE battle SET status = 'running' WHERE id = $1`, value.ID); err != nil {
		t.Fatalf("设置待承载 Battle: %v", err)
	}
	leaseA, err := repository.AcquireRuntimeLease(ctx, value.ID, "server-a")
	if err != nil {
		t.Fatalf("AcquireRuntimeLease(server-a) error = %v", err)
	}
	randomA, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	randomSnapshot, err := randomA.Snapshot()
	if err != nil {
		t.Fatalf("Snapshot() error = %v", err)
	}
	started, err := repository.Start(ctx, leaseA, fixture.InitialState, randomSnapshot, createdAt.Add(time.Minute))
	if err != nil {
		t.Fatalf("Start() error = %v", err)
	}
	if _, err := pool.Exec(ctx, `
		UPDATE battle_runtime_lease
		SET acquired_at = CURRENT_TIMESTAMP - INTERVAL '32 seconds',
		    renewed_at = CURRENT_TIMESTAMP - INTERVAL '31 seconds',
		    lease_expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second'
		WHERE battle_id = $1
	`, value.ID); err != nil {
		t.Fatalf("使 Server A Lease 过期: %v", err)
	}
	leaseB, err := repository.AcquireRuntimeLease(ctx, value.ID, "server-b")
	if err != nil || leaseB.FencingToken <= leaseA.FencingToken {
		t.Fatalf("AcquireRuntimeLease(server-b) = %+v, error = %v", leaseB, err)
	}

	stateA, err := battleengine.NewState(fixture.InitialState)
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	runtimeA, err := battle.NewRuntime(started, stateA, randomA, repository.TurnCommitter(leaseA), nil, func() time.Time { return createdAt.Add(2 * time.Minute) }, nil)
	if err != nil {
		t.Fatalf("NewRuntime(server-a) error = %v", err)
	}
	if _, err := runtimeA.Submit(ctx, battle.TurnSubmission{PlayerCharacterID: started.Participants[0].PlayerCharacterID, ExpectedStateVersion: 0, IdempotencyKey: "stale-a-left", Actions: fixture.Turns[0].Command.Actions[:1]}); err != nil {
		t.Fatalf("Submit(server-a left) error = %v", err)
	}
	_, err = runtimeA.Submit(ctx, battle.TurnSubmission{PlayerCharacterID: started.Participants[1].PlayerCharacterID, ExpectedStateVersion: 0, IdempotencyKey: "stale-a-right", Actions: fixture.Turns[0].Command.Actions[1:]})
	if !errors.Is(err, battlepersistence.ErrRuntimeLeaseLost) {
		t.Fatalf("Submit(server-a right) error = %v, want ErrRuntimeLeaseLost", err)
	}
	var stateVersion, turnRecords int
	if err := pool.QueryRow(ctx, `SELECT state_version FROM battle WHERE id = $1`, value.ID).Scan(&stateVersion); err != nil {
		t.Fatalf("读取 Battle state_version: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM battle_turn_record WHERE battle_id = $1`, value.ID).Scan(&turnRecords); err != nil {
		t.Fatalf("统计 Turn Record: %v", err)
	}
	if stateVersion != 0 || turnRecords != 0 {
		t.Fatalf("旧 token 写入结果 = state_version %d, turns %d", stateVersion, turnRecords)
	}

	stateB, _ := battleengine.NewState(fixture.InitialState)
	randomB, _ := battleengine.RestoreRandomSource(randomSnapshot)
	runtimeB, err := battle.NewRuntime(started, stateB, randomB, repository.TurnCommitter(leaseB), nil, func() time.Time { return createdAt.Add(2 * time.Minute) }, nil)
	if err != nil {
		t.Fatalf("NewRuntime(server-b) error = %v", err)
	}
	_, _ = runtimeB.Submit(ctx, battle.TurnSubmission{PlayerCharacterID: started.Participants[0].PlayerCharacterID, ExpectedStateVersion: 0, IdempotencyKey: "fresh-b-left", Actions: fixture.Turns[0].Command.Actions[:1]})
	if _, err := runtimeB.Submit(ctx, battle.TurnSubmission{PlayerCharacterID: started.Participants[1].PlayerCharacterID, ExpectedStateVersion: 0, IdempotencyKey: "fresh-b-right", Actions: fixture.Turns[0].Command.Actions[1:]}); err != nil {
		t.Fatalf("Submit(server-b) error = %v", err)
	}
}

// TestRecoveryAttemptCanBeReclaimedAfterClaimTimeout 验证领取 Server 崩溃后，claimed 尝试不会永久阻塞恢复。
func TestRecoveryAttemptCanBeReclaimedAfterClaimTimeout(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startBattleDatabase(t, ctx)
	repository := battlepersistence.NewAdapters(pool, snowflake.NewTestID, nil)
	seedBattleDependencies(t, ctx, pool)
	observedAt := time.Date(2026, time.August, 12, 15, 0, 0, 0, time.UTC)
	value := persistedPreviewSession(observedAt)
	if err := repository.Create(ctx, value); err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	attemptID := snowflake.NewTestID()
	if _, err := pool.Exec(ctx, `
		INSERT INTO battle_recovery_attempt (id, battle_id, attempt_number, state, available_at, claimed_by, claimed_at, created_at)
		VALUES ($1, $2, 1, 'claimed', $3, 'dead-server', $4, $3)
	`, attemptID, value.ID, observedAt.Add(-2*time.Minute), observedAt.Add(-battlepersistence.RecoveryClaimTimeout-time.Second)); err != nil {
		t.Fatalf("创建超时 claimed Recovery Attempt: %v", err)
	}
	due, err := repository.ListDueRecoveryAttempts(ctx, observedAt, 10)
	if err != nil || !containsIdentifier(due, attemptID) {
		t.Fatalf("ListDueRecoveryAttempts() = %v, error = %v", due, err)
	}
	claimed, err := repository.ClaimRecoveryAttempt(ctx, attemptID, "replacement-server", observedAt)
	if err != nil || claimed.ID != attemptID || claimed.BattleID != value.ID {
		t.Fatalf("ClaimRecoveryAttempt() = %+v, error = %v", claimed, err)
	}
	var claimedBy string
	if err := pool.QueryRow(ctx, `SELECT claimed_by FROM battle_recovery_attempt WHERE id = $1`, attemptID).Scan(&claimedBy); err != nil || claimedBy != "replacement-server" {
		t.Fatalf("claimed_by = %q, error = %v", claimedBy, err)
	}
	if err := repository.CompleteRecoveryAttempt(ctx, attemptID, "dead-server", true, "", observedAt.Add(time.Second)); !errors.Is(err, battlepersistence.ErrRuntimeLeaseLost) {
		t.Fatalf("CompleteRecoveryAttempt(dead-server) error = %v, want ErrRuntimeLeaseLost", err)
	}
	if err := repository.CompleteRecoveryAttempt(ctx, attemptID, "replacement-server", true, "", observedAt.Add(time.Second)); err != nil {
		t.Fatalf("CompleteRecoveryAttempt(replacement-server) error = %v", err)
	}
}

// TestBotStrategyAdministrationUsesAdminSecurityDomain 验证 Bot 策略管理只依赖管理员账号，
// 并把幂等记录与审计事实写入管理员安全域。
func TestBotStrategyAdministrationUsesAdminSecurityDomain(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startBattleDatabase(t, ctx)
	repository := battlepersistence.NewAdapters(pool, snowflake.NewTestID, nil)
	actorID := snowflake.NewTestID()
	createdAt := time.Date(2026, time.August, 12, 12, 0, 0, 0, time.UTC)
	if _, err := pool.Exec(ctx, `
		INSERT INTO admin_account (
			id, username, username_key, display_name, password_hash, password_algorithm,
			password_parameters, status, created_at, updated_at
		) VALUES ($1, 'bot-admin', 'bot-admin', 'Bot 策略管理员', 'unused', 'argon2id', '{}', 'active', $2, $2)
	`, actorID, createdAt); err != nil {
		t.Fatalf("创建 Bot 管理员夹具: %v", err)
	}
	if _, err := pool.Exec(ctx, `INSERT INTO audit_hash_chain_state (id, ledger, latest_hash, updated_at) VALUES ($1, 'admin_audit_log', ''::bytea, $2) ON CONFLICT (ledger) DO NOTHING`, snowflake.NewTestID(), createdAt); err != nil {
		t.Fatalf("初始化管理员审计哈希链: %v", err)
	}
	definition := json.RawMessage(`{"schemaVersion":1,"displayName":"训练机器人","planner":{"kind":"first_available","fallbackKind":"first_available"},"generator":{"kind":"mirror"},"budget":{"maxMembers":6,"maxSkillsPerMember":4,"maxDecisionMillis":50}}`)
	command := battle.CreateBotStrategyCommand{
		GameDataWriteContext: administration.NewGameDataWriteContext(actorID, "bot-create-admin-domain", "bot-create-admin-domain-request"),
		Code:                 "admin-domain-bot",
		Definition:           definition,
	}
	created, err := repository.CreateBotStrategy(ctx, command, definition, createdAt)
	if err != nil {
		t.Fatalf("CreateBotStrategy() error = %v", err)
	}
	replayed, err := repository.CreateBotStrategy(ctx, command, definition, createdAt)
	if err != nil || replayed.Code != created.Code || replayed.Version != created.Version {
		t.Fatalf("replayed CreateBotStrategy() = %+v, error = %v", replayed, err)
	}

	var strategies, adminRecords, playerRecords, adminAudits, playerAudits int
	queries := []struct {
		name   string
		query  string
		args   []any
		target *int
	}{
		{"Bot 策略", `SELECT count(*) FROM battle_bot_strategy WHERE code = 'admin-domain-bot'`, nil, &strategies},
		{"管理员幂等记录", `SELECT count(*) FROM admin_idempotency_record WHERE actor_account_id = $1 AND operation_id = 'battle.bot-strategy.create'`, []any{actorID}, &adminRecords},
		{"玩家幂等记录", `SELECT count(*) FROM administration_idempotency_record WHERE actor_account_id = $1 AND operation_id = 'battle.bot-strategy.create'`, []any{actorID}, &playerRecords},
		{"管理员审计", `SELECT count(*) FROM admin_audit_log WHERE actor_account_id = $1 AND action_code = 'battle.bot-strategy.created'`, []any{actorID}, &adminAudits},
		{"玩家审计", `SELECT count(*) FROM administration_audit_log WHERE actor_account_id = $1 AND action_code = 'battle.bot-strategy.created'`, []any{actorID}, &playerAudits},
	}
	for _, query := range queries {
		if err := pool.QueryRow(ctx, query.query, query.args...).Scan(query.target); err != nil {
			t.Fatalf("查询%s: %v", query.name, err)
		}
	}
	if strategies != 1 || adminRecords != 1 || playerRecords != 0 || adminAudits != 1 || playerAudits != 0 {
		t.Fatalf("Bot 管理事实 = strategies %d, admin idempotency %d, player idempotency %d, admin audit %d, player audit %d", strategies, adminRecords, playerRecords, adminAudits, playerAudits)
	}
}

// TestRepositoryPersistsActiveBattleTurnAndTerminalHistory 验证 Preview 到 Active、Actor 回合事务、
// 终局摘要、Outbox 与账号占用释放在 PostgreSQL 中形成完整权威链路。
func TestRepositoryPersistsActiveBattleTurnAndTerminalHistory(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startBattleDatabase(t, ctx)
	repository := battlepersistence.NewAdapters(pool, snowflake.NewTestID, nil)
	fixture := loadBattleGoldenReplay(t)
	// 把首个黄金回合设为赛制最后一回合，验证 Runtime 提交终局 Turn Record 时会在同一事务自动完成
	// Battle，而不是依赖 RPC 或另一个后台调用再补写终局状态。
	fixture.InitialState.Format.MaxTurns = 1
	// 启动阶段的公开事件必须进入独立账本，不能被伪造成 Turn 0 记录或仅留在启动进程内存。
	fixture.InitialState.Sides[0].Members[0].SwitchInRevealOpponentHeldItems = true
	initialHeldItemID := snowflake.NewTestID()
	fixture.InitialState.Sides[1].Members[0].ItemID = initialHeldItemID
	seedBattleDependencies(t, ctx, pool)

	createdAt := time.Date(2026, time.July, 30, 12, 0, 0, 0, time.UTC)
	session := persistedPreviewSession(createdAt)
	if err := repository.Create(ctx, session); err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	stored, err := repository.Get(ctx, session.ID)
	if err != nil || stored.ID != session.ID || len(stored.Participants) != 2 {
		t.Fatalf("Get() = %+v, error = %v", stored, err)
	}
	if _, err := pool.Exec(ctx, `UPDATE battle SET status = 'running' WHERE id = $1`, session.ID); err != nil {
		t.Fatalf("设置待承载 Battle: %v", err)
	}
	lease, err := repository.AcquireRuntimeLease(ctx, session.ID, "store-integration-server")
	if err != nil {
		t.Fatalf("AcquireRuntimeLease() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	randomSnapshot, err := random.Snapshot()
	if err != nil {
		t.Fatalf("Snapshot() error = %v", err)
	}
	started, err := repository.Start(ctx, lease, fixture.InitialState, randomSnapshot, createdAt.Add(time.Minute))
	if err != nil || started.Status != battle.StatusRunning || started.StartedAt.IsZero() {
		t.Fatalf("Start() = %+v, error = %v", started, err)
	}
	initialDisclosure, err := repository.GetParticipantDisclosure(ctx, started.ID, started.Participants[0].PlayerCharacterID)
	if err != nil || len(initialDisclosure.InitialEvents) != 1 {
		t.Fatalf("GetParticipantDisclosure() 初始事件 = %+v, error = %v", initialDisclosure, err)
	}
	var initialReveal battleengine.OpponentHeldItemRevealedEvent
	if err := json.Unmarshal(initialDisclosure.InitialEvents[0], &initialReveal); err != nil ||
		initialReveal.Type != battleengine.EventKindOpponentHeldItemRevealed || initialReveal.ItemID != initialHeldItemID {
		t.Fatalf("初始披露事件 = %+v, error = %v", initialReveal, err)
	}
	var initialEventsPayload []byte
	if err := pool.QueryRow(ctx, `SELECT initial_events FROM battle WHERE id = $1`, started.ID).Scan(&initialEventsPayload); err != nil {
		t.Fatalf("读取 Battle 初始事件账本: %v", err)
	}
	var persistedInitialEvents []json.RawMessage
	if err := json.Unmarshal(initialEventsPayload, &persistedInitialEvents); err != nil || len(persistedInitialEvents) != 1 {
		t.Fatalf("持久化初始事件 = %s, error = %v", initialEventsPayload, err)
	}
	engineState, err := battleengine.NewState(fixture.InitialState)
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	runtime, err := battle.NewRuntime(started, engineState, random, repository.TurnCommitter(lease), nil, func() time.Time {
		return createdAt.Add(time.Minute + time.Second)
	}, nil)
	if err != nil {
		t.Fatalf("NewRuntime() error = %v", err)
	}
	if _, err := runtime.Submit(ctx, battle.TurnSubmission{
		PlayerCharacterID: started.Participants[0].PlayerCharacterID, ExpectedStateVersion: 0,
		IdempotencyKey: "battle-store-side-one", Actions: fixture.Turns[0].Command.Actions[:1],
	}); err != nil {
		t.Fatalf("Submit(side one) error = %v", err)
	}
	resolved, err := runtime.Submit(ctx, battle.TurnSubmission{
		PlayerCharacterID: started.Participants[1].PlayerCharacterID, ExpectedStateVersion: 0,
		IdempotencyKey: "battle-store-side-two", Actions: fixture.Turns[0].Command.Actions[1:],
	})
	if err != nil || !resolved.Resolved || resolved.StateVersion != 1 {
		t.Fatalf("Submit(side two) = %+v, error = %v", resolved, err)
	}
	disclosureAfterTurn, err := repository.GetParticipantDisclosure(ctx, started.ID, started.Participants[0].PlayerCharacterID)
	if err != nil || len(disclosureAfterTurn.InitialEvents) != 1 {
		t.Fatalf("回合提交后的初始事件 = %+v, error = %v", disclosureAfterTurn, err)
	}
	completed, err := repository.Get(ctx, started.ID)
	if err != nil || completed.Status != battle.StatusCompleted || completed.StateVersion != 1 {
		t.Fatalf("automatic terminal completion = %+v, error = %v", completed, err)
	}
	if completed.TerminalReason != string(battle.TerminalReasonDraw) {
		t.Fatalf("automatic terminal reason = %q, want %q", completed.TerminalReason, battle.TerminalReasonDraw)
	}
	history, err := repository.ListHistory(ctx, started.Participants[0].PlayerCharacterID, 1, 20)
	if err != nil || history.Total != 1 || len(history.Items) != 1 ||
		history.Items[0].BattleID != started.ID || history.Items[0].TurnCount != 1 {
		t.Fatalf("ListHistory() = %+v, error = %v", history, err)
	}
	var turnRecords, reservations, outboxCount int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM battle_turn_record WHERE battle_id = $1`, started.ID).Scan(&turnRecords); err != nil {
		t.Fatalf("统计 Turn Record: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM battle_participant_reservation WHERE battle_id = $1`, started.ID).Scan(&reservations); err != nil {
		t.Fatalf("统计账号占用: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM battle_outbox WHERE battle_id = $1`, started.ID).Scan(&outboxCount); err != nil {
		t.Fatalf("统计 Battle Outbox: %v", err)
	}
	if turnRecords != 1 || reservations != 0 || outboxCount != 1 {
		t.Fatalf("terminal persistence counts: turns=%d reservations=%d outbox=%d", turnRecords, reservations, outboxCount)
	}
	analytics, err := repository.DrainTerminalOutbox(ctx, createdAt.Add(2*time.Minute), 10)
	if err != nil || analytics.Published != 1 || analytics.Projection.CompletedBattles != 1 ||
		analytics.Projection.ChallengeBattles != 1 || analytics.Projection.TrainingBattles != 0 {
		t.Fatalf("DrainTerminalOutbox() = %+v, error = %v", analytics, err)
	}
	var publishedOutbox int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM battle_outbox WHERE battle_id = $1 AND published_at IS NOT NULL`, started.ID).Scan(&publishedOutbox); err != nil {
		t.Fatalf("统计已发布 Battle Outbox: %v", err)
	}
	if publishedOutbox != 1 {
		t.Fatalf("已发布 Battle Outbox 数量 = %d，期望 1", publishedOutbox)
	}
	archive, err := repository.LoadReplayArchive(ctx, started.ID)
	if err != nil {
		t.Fatalf("LoadReplayArchive() error = %v", err)
	}
	replayed, err := battleengine.ReplayGolden(archive)
	if err != nil || replayed.ReplayedTurns != 1 || replayed.FinalState.TurnNumber != 1 {
		t.Fatalf("ReplayGolden(LoadReplayArchive()) = %+v, error = %v", replayed, err)
	}
	rebuilt, err := repository.RebuildAnalyticsProjection(ctx, createdAt.Add(3*time.Minute))
	if err != nil || rebuilt != analytics.Projection {
		t.Fatalf("RebuildAnalyticsProjection() = %+v, error = %v，增量投影 = %+v", rebuilt, err, analytics.Projection)
	}
}

// 正确终局，并且整场超时基于持久化初始状态在没有任何回合记录时得到确定性的 No Contest。
func TestRepositoryExpiresDueLifecycleRecordsAndPersistsBattleTimeout(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startBattleDatabase(t, ctx)
	repository := battlepersistence.NewAdapters(pool, snowflake.NewTestID, nil)
	fixture := loadBattleGoldenReplay(t)
	seedBattleDependencies(t, ctx, pool)

	createdAt := time.Date(2026, time.July, 31, 12, 0, 0, 0, time.UTC)
	challengeSession := persistedPreviewSession(createdAt)
	challengeID := snowflake.NewTestID()
	challenge, err := battle.NewChallenge(context.Background(), battle.CreateChallengeCommand{
		ChallengerAccountID:         challengeSession.Participants[0].AccountID,
		ChallengerPlayerCharacterID: challengeSession.Participants[0].PlayerCharacterID,
		ChallengerDisplayName:       challengeSession.Participants[0].DisplayName,
		ChallengerTeam: team.Team{
			ID: challengeSession.Participants[0].Team.SourceTeamID, PlayerCharacterID: challengeSession.Participants[0].PlayerCharacterID,
			Version: 1, Members: []team.Member{battleTeamMember()},
		},
		TargetAccountID: challengeSession.Participants[1].AccountID, TargetPlayerCharacterID: challengeSession.Participants[1].PlayerCharacterID,
		TargetDisplayName: challengeSession.Participants[1].DisplayName, BattleFormatID: challengeSession.BattleFormatID,
		BattleFormatSnapshot: challengeSession.BattleFormatSnapshot,
	}, snowflake.TestSource(func() snowflake.ID { return challengeID }), func() time.Time { return createdAt.Add(-6 * time.Minute) })
	if err != nil {
		t.Fatalf("NewChallenge() error = %v", err)
	}
	if err := repository.CreateChallenge(ctx, challenge); err != nil {
		t.Fatalf("CreateChallenge() error = %v", err)
	}

	previewSession := persistedPreviewSession(createdAt)
	if err := repository.Create(ctx, previewSession); err != nil {
		t.Fatalf("Create(preview) error = %v", err)
	}
	previewObservedAt := previewSession.PreviewDeadlineAt.Add(time.Second)
	challengeIDs, err := repository.ListExpiredChallengeIDs(ctx, previewObservedAt)
	if err != nil || !containsIdentifier(challengeIDs, challenge.ID) {
		t.Fatalf("ListExpiredChallengeIDs() = %+v, error = %v", challengeIDs, err)
	}
	if _, err := repository.ExpireChallenge(ctx, challenge.ID, previewObservedAt); err != nil {
		t.Fatalf("ExpireChallenge() error = %v", err)
	}

	previewIDs, err := repository.ListExpiredPreviewBattleIDs(ctx, previewObservedAt)
	if err != nil || !containsIdentifier(previewIDs, previewSession.ID) {
		t.Fatalf("ListExpiredPreviewBattleIDs() = %+v, error = %v", previewIDs, err)
	}
	if autoCompleted, err := repository.CompleteExpiredPreview(ctx, previewSession.ID, previewObservedAt); err != nil ||
		autoCompleted.Status != battle.StatusRunning || len(autoCompleted.PreviewSubmissions) != len(autoCompleted.Participants) {
		t.Fatalf("CompleteExpiredPreview() = %+v, error = %v", autoCompleted, err)
	}
	if startingIDs, err := repository.ListPendingRuntimeBattleIDs(ctx); err != nil || !containsIdentifier(startingIDs, previewSession.ID) {
		t.Fatalf("ListPendingRuntimeBattleIDs() = %+v, error = %v", startingIDs, err)
	}
	persistedPreview, err := repository.Get(ctx, previewSession.ID)
	if err != nil || len(persistedPreview.PreviewSubmissions) != 2 ||
		len(persistedPreview.PreviewSubmissions[0].RandomTrace) == 0 || len(persistedPreview.PreviewSubmissions[1].RandomTrace) == 0 {
		t.Fatalf("Get(auto completed preview) = %+v, error = %v", persistedPreview, err)
	}
	// 该用例还需要使用相同账号构造整场超时，因此由参与者在 Runtime 启动前明确取消 Battle。
	canceled, err := repository.Cancel(ctx, previewSession.ID, previewObservedAt.Add(time.Second))
	if err != nil || canceled.Status != battle.StatusCanceled || canceled.TerminalReason != string(battle.TerminalReasonCanceled) {
		t.Fatalf("Cancel(pending runtime) = %+v, error = %v", canceled, err)
	}
	var canceledReservations, canceledSummaries, canceledOutbox int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM battle_participant_reservation WHERE battle_id = $1`, previewSession.ID).Scan(&canceledReservations); err != nil {
		t.Fatalf("统计已取消 Battle 占用: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM battle_authoritative_summary WHERE battle_id = $1`, previewSession.ID).Scan(&canceledSummaries); err != nil {
		t.Fatalf("统计已取消 Battle 摘要: %v", err)
	}
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM battle_outbox WHERE battle_id = $1`, previewSession.ID).Scan(&canceledOutbox); err != nil {
		t.Fatalf("统计已取消 Battle Outbox: %v", err)
	}
	if canceledReservations != 0 || canceledSummaries != 1 || canceledOutbox != 1 {
		t.Fatalf("已取消 Battle 事实 = reservations %d, summaries %d, outbox %d", canceledReservations, canceledSummaries, canceledOutbox)
	}
	canceledAnalytics, err := repository.DrainTerminalOutbox(ctx, previewObservedAt.Add(2*time.Second), 10)
	if err != nil || canceledAnalytics.Published != 1 || canceledAnalytics.Projection.CompletedBattles != 1 || canceledAnalytics.Projection.CanceledBattles != 1 {
		t.Fatalf("已取消 Battle 分析投影 = %+v, error = %v", canceledAnalytics, err)
	}

	activeSession := persistedTrainingBattle(t, createdAt.Add(2*time.Hour))
	if err := repository.Create(ctx, activeSession); err != nil {
		t.Fatalf("Create(active) error = %v", err)
	}
	readyAt := activeSession.PreviewDeadlineAt.Add(time.Second)
	if _, err := repository.CompleteExpiredPreview(ctx, activeSession.ID, readyAt); err != nil {
		t.Fatalf("CompleteExpiredPreview(active) error = %v", err)
	}
	lease, err := repository.AcquireRuntimeLease(ctx, activeSession.ID, "timeout-integration-server")
	if err != nil {
		t.Fatalf("AcquireRuntimeLease() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	randomSnapshot, err := random.Snapshot()
	if err != nil {
		t.Fatalf("Snapshot() error = %v", err)
	}
	started, err := repository.Start(ctx, lease, fixture.InitialState, randomSnapshot, readyAt.Add(time.Second))
	if err != nil {
		t.Fatalf("Start() error = %v", err)
	}
	activeObservedAt := activeSession.BattleDeadlineAt.Add(time.Second)

	activeIDs, err := repository.ListExpiredRunningBattleIDs(ctx, activeObservedAt)
	if err != nil || !containsIdentifier(activeIDs, started.ID) {
		t.Fatalf("ListExpiredRunningBattleIDs() = %+v, error = %v", activeIDs, err)
	}
	if completed, err := repository.CompleteBattleTimeout(ctx, started.ID, activeObservedAt); err != nil ||
		completed.Status != battle.StatusCompleted || completed.TerminalReason != string(battle.TerminalReasonNoContest) {
		t.Fatalf("CompleteBattleTimeout() = %+v, error = %v", completed, err)
	}
}

// TestRepositoryCreatesTrainingWithPersistedBotPreview 验证 Training Battle 会持久化自动 Bot Preview。
//
// 若漏写该记录，重载 Session 后真人 Preview 永远只有一方，自动开局链路会永久停在 preview。
func TestRepositoryCreatesTrainingWithPersistedBotPreview(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startBattleDatabase(t, ctx)
	repository := battlepersistence.NewAdapters(pool, snowflake.NewTestID, nil)
	seedBattleDependencies(t, ctx, pool)

	session := persistedTrainingBattle(t, time.Date(2026, time.July, 30, 13, 30, 0, 0, time.UTC))
	if err := repository.Create(ctx, session); err != nil {
		t.Fatalf("Create() error = %v", err)
	}
	stored, err := repository.Get(ctx, session.ID)
	if err != nil || len(stored.PreviewSubmissions) != 1 || stored.PreviewSubmissions[0].Side != battle.ParticipantSideTwo {
		t.Fatalf("Get() = %+v, error = %v", stored, err)
	}
	if len(stored.PreviewSubmissions[0].MemberPositions) != 1 || stored.PreviewSubmissions[0].MemberPositions[0] != 1 {
		t.Fatalf("持久化 Bot Preview = %+v", stored.PreviewSubmissions[0])
	}
}

// 并且双方账号占用、其他关联待处理邀请的作废都和新 Preview Battle 同事务提交。
func TestRepositoryAcceptsChallengeAndCreatesReservedPreview(t *testing.T) {
	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Minute)
	defer cancel()
	pool := startBattleDatabase(t, ctx)
	repository := battlepersistence.NewAdapters(pool, snowflake.NewTestID, nil)
	seedBattleDependencies(t, ctx, pool)

	createdAt := time.Date(2026, time.July, 30, 13, 0, 0, 0, time.UTC)
	fixture := persistedPreviewSession(createdAt)
	challengeID := snowflake.MustParse("1048576040")
	challenge, err := battle.NewChallenge(context.Background(), battle.CreateChallengeCommand{
		ChallengerAccountID:         fixture.Participants[0].AccountID,
		ChallengerPlayerCharacterID: fixture.Participants[0].PlayerCharacterID,
		ChallengerDisplayName:       fixture.Participants[0].DisplayName,
		ChallengerTeam:              team.Team{ID: fixture.Participants[0].Team.SourceTeamID, Version: 1, Members: []team.Member{battleTeamMember()}},
		TargetAccountID:             fixture.Participants[1].AccountID,
		TargetPlayerCharacterID:     fixture.Participants[1].PlayerCharacterID,
		TargetDisplayName:           fixture.Participants[1].DisplayName,
		BattleFormatID:              fixture.BattleFormatID, BattleFormatSnapshot: fixture.BattleFormatSnapshot,
	}, snowflake.TestSource(func() snowflake.ID { return challengeID }), func() time.Time { return createdAt })
	if err != nil {
		t.Fatalf("NewChallenge() error = %v", err)
	}
	if err := repository.CreateChallenge(ctx, challenge); err != nil {
		t.Fatalf("CreateChallenge() error = %v", err)
	}
	thirdAccountID := snowflake.MustParse("1048576042")
	thirdCharacterID := snowflake.MustParse("1048576043")
	thirdTeamID := snowflake.MustParse("1048576044")
	seedAdditionalBattlePlayer(t, ctx, pool, thirdAccountID, thirdCharacterID, thirdTeamID, "丙")
	otherChallengeID := snowflake.MustParse("1048576041")
	otherChallenge, err := battle.NewChallenge(context.Background(), battle.CreateChallengeCommand{
		ChallengerAccountID:         fixture.Participants[0].AccountID,
		ChallengerPlayerCharacterID: fixture.Participants[0].PlayerCharacterID,
		ChallengerDisplayName:       fixture.Participants[0].DisplayName,
		ChallengerTeam:              team.Team{ID: fixture.Participants[0].Team.SourceTeamID, Version: 1, Members: []team.Member{battleTeamMember()}},
		TargetAccountID:             thirdAccountID, TargetPlayerCharacterID: thirdCharacterID, TargetDisplayName: "丙", BattleFormatID: fixture.BattleFormatID,
		BattleFormatSnapshot: fixture.BattleFormatSnapshot,
	}, snowflake.TestSource(func() snowflake.ID { return otherChallengeID }), func() time.Time { return createdAt })
	if err != nil {
		t.Fatalf("NewChallenge(other) error = %v", err)
	}
	if err := repository.CreateChallenge(ctx, otherChallenge); err != nil {
		t.Fatalf("CreateChallenge(other) error = %v", err)
	}

	accepted, err := repository.AcceptChallenge(
		ctx, challenge.ID, fixture.Participants[1].PlayerCharacterID,
		team.Team{ID: fixture.Participants[1].Team.SourceTeamID, PlayerCharacterID: fixture.Participants[1].PlayerCharacterID, Version: 1, Members: []team.Member{battleTeamMember()}},
		fixture.Format, createdAt.Add(time.Minute),
	)
	if err != nil || accepted.Status != battle.StatusPreview || accepted.ChallengeID != challenge.ID {
		t.Fatalf("AcceptChallenge() = %+v, error = %v", accepted, err)
	}
	storedChallenge, err := repository.GetChallenge(ctx, challenge.ID)
	if err != nil || storedChallenge.Status != battle.ChallengeAccepted || storedChallenge.ResolvedAt.IsZero() {
		t.Fatalf("GetChallenge(accepted) = %+v, error = %v", storedChallenge, err)
	}
	storedOther, err := repository.GetChallenge(ctx, otherChallenge.ID)
	if err != nil || storedOther.Status != battle.ChallengeSuperseded {
		t.Fatalf("GetChallenge(superseded) = %+v, error = %v", storedOther, err)
	}
	var reservations int
	if err := pool.QueryRow(ctx, `SELECT count(*) FROM battle_participant_reservation WHERE battle_id = $1`, accepted.ID).Scan(&reservations); err != nil {
		t.Fatalf("统计接受邀请后的账号占用: %v", err)
	}
	if reservations != 2 {
		t.Fatalf("账号占用数量 = %d，期望 2", reservations)
	}
}

func startBattleDatabase(t *testing.T, ctx context.Context) *database.Pool {
	t.Helper()
	container, err := postgres.Run(
		ctx,
		battlePostgresImage,
		postgres.WithDatabase("avalon_battle_test"),
		postgres.WithUsername("avalon"),
		postgres.WithPassword("avalon"),
		postgres.BasicWaitStrategies(),
	)
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
	resetBattleGameDataFixture(t, ctx, pool)
	return pool
}

// resetBattleGameDataFixture 移除与 Battle 存储夹具同 Stable Code 的部署 seed。
// 部署资料本身由 migration 集成测试验证；本包只验证 Battle 事务，不应依赖或改写 seed 的身份。
func resetBattleGameDataFixture(t *testing.T, ctx context.Context, pool *database.Pool) {
	t.Helper()
	if _, err := pool.Exec(ctx, `TRUNCATE TABLE game_battle_format CASCADE`); err != nil {
		t.Fatalf("清理 Battle 资料夹具: %v", err)
	}
}

func loadBattleGoldenReplay(t *testing.T) battleengine.GoldenReplay {
	t.Helper()
	fixture, err := battleengine.LoadGoldenReplay(filepath.Join("..", "..", "battleengine", "testdata", "golden", "major-status-turn.v1.json"))
	if err != nil {
		t.Fatalf("LoadGoldenReplay() error = %v", err)
	}
	return fixture
}

func persistedPreviewSession(createdAt time.Time) battle.Battle {
	return battle.Battle{
		ID: snowflake.MustParse("1048576033"), Mode: battle.BattleModePvP, SourceType: battle.BattleSourceChallenge,
		ChallengeID: snowflake.MustParse("1048576034"), Status: battle.StatusPreview,
		BattleFormatID:       snowflake.MustParse("1048576035"),
		BattleFormatSnapshot: []byte(`{"schemaVersion":1}`),
		Format: battle.Format{
			RosterCount: 1, SelectCount: 1, ActiveParticipantsPerSide: 1,
			PreviewDuration: time.Minute, BattleDuration: 30 * time.Minute,
		},
		PreviewDeadlineAt: createdAt.Add(time.Minute), BattleDeadlineAt: createdAt.Add(30 * time.Minute),
		Version: 1, CreatedAt: createdAt, UpdatedAt: createdAt,
		Participants: []battle.Participant{
			{
				Side: battle.ParticipantSideOne, AccountID: snowflake.MustParse("1048576036"),
				PlayerCharacterID: snowflake.MustParse("1048576038"), DisplayName: "甲",
				Team: teamSnapshot("1048576040"),
			},
			{
				Side: battle.ParticipantSideTwo, AccountID: snowflake.MustParse("1048576037"),
				PlayerCharacterID: snowflake.MustParse("1048576039"), DisplayName: "乙",
				Team: teamSnapshot("1048576041"),
			},
		},
	}
}

func persistedTrainingBattle(t *testing.T, createdAt time.Time) battle.Battle {
	t.Helper()
	challenge := persistedPreviewSession(createdAt)
	player := challenge.Participants[0]
	session, err := battle.NewTrainingBattle(context.Background(), battle.NewTrainingBattleCommand{
		AccountID: player.AccountID, PlayerCharacterID: player.PlayerCharacterID, DisplayName: player.DisplayName,
		Team: team.Team{
			ID: player.Team.SourceTeamID, PlayerCharacterID: player.PlayerCharacterID, Version: player.Team.SourceTeamVersion,
			Members: []team.Member{battleTeamMember()},
		},
		BattleFormatID: challenge.BattleFormatID, BattleFormatSnapshot: challenge.BattleFormatSnapshot,
		Format: challenge.Format,
		Bot: battle.BotProfile{
			Code: "training-mirror", StrategyVersion: 1, DisplayName: "训练机器人", Definition: json.RawMessage(`{"schemaVersion":1,"displayName":"训练机器人","planner":{"kind":"first_available","fallbackKind":"first_available"},"generator":{"kind":"mirror"},"budget":{"maxMembers":6,"maxSkillsPerMember":4,"maxDecisionMillis":50}}`),
			Team: battle.TeamSnapshot{SourceTeamID: snowflake.NewTestID(), SourceTeamVersion: 1, Members: []team.Member{battleTeamMember()}},
		},
	}, snowflake.NewTestID, func() time.Time { return createdAt })
	if err != nil {
		t.Fatalf("NewTrainingBattle() error = %v", err)
	}
	return session
}

func teamSnapshot(rawID string) battle.TeamSnapshot {
	return battle.TeamSnapshot{
		SourceTeamID: snowflake.MustParse(rawID), SourceTeamVersion: 1,
		Members: []team.Member{battleTeamMember()},
	}
}

// battleTeamMember 构造能够通过 Snowflake JSON 契约编码的最小 Team 成员快照。
func battleTeamMember() team.Member {
	return team.Member{
		Position:      1,
		CreatureID:    snowflake.MustParse("1048576101"),
		AbilityID:     snowflake.MustParse("1048576102"),
		TeraElementID: snowflake.MustParse("1048576103"),
		NatureID:      snowflake.MustParse("1048576104"),
		Level:         50,
	}
}

func containsIdentifier(values []snowflake.ID, expected snowflake.ID) bool {
	for _, value := range values {
		if value == expected {
			return true
		}
	}
	return false
}

func seedBattleDependencies(t *testing.T, ctx context.Context, pool *database.Pool) {
	t.Helper()
	session := persistedPreviewSession(time.Now().UTC())
	for index, participant := range session.Participants {
		if _, err := pool.Exec(ctx, `
            INSERT INTO account (
                id, username, username_key, password_hash, password_algorithm, password_parameters,
                status, security_version, created_at, updated_at, display_name
            ) VALUES ($1, $2, $2, 'test', 'argon2id', '{}'::jsonb, 'active', 1, now(), now(), $3)
		`, participant.AccountID, "battle-account-"+string(rune('1'+index)), participant.DisplayName); err != nil {
			t.Fatalf("创建 Battle 测试账号: %v", err)
		}
		if _, err := pool.Exec(ctx, `
            INSERT INTO player_character (id, account_id, display_name, display_name_key, version, created_at, updated_at)
            VALUES ($1, $2, $3, $3, 1, now(), now())
        `, participant.PlayerCharacterID, participant.AccountID, participant.DisplayName); err != nil {
			t.Fatalf("创建 Battle 测试角色: %v", err)
		}
		if _, err := pool.Exec(ctx, `
            INSERT INTO player_character_team (id, player_character_id, name, name_key, version, created_at, updated_at)
            VALUES ($1, $2, $3, $3, 1, now(), now())
        `, participant.Team.SourceTeamID, participant.PlayerCharacterID, "测试队伍"+string(rune('甲'+index))); err != nil {
			t.Fatalf("创建 Battle 测试 Team: %v", err)
		}
	}
	if _, err := pool.Exec(ctx, `
        INSERT INTO game_battle_format (
            id, code, name, description, mode, roster_count, select_count, active_participants_per_side,
            level_rule, normalized_level, preview_seconds, turn_seconds, battle_seconds,
            challenge_available, training_available, encounter_available, admin_preview_available,
            clause_ids, restriction_ids, mechanic_ids, is_default, enabled, version, created_at, updated_at
        ) VALUES (
            $1, 'standard-single', '标准单打', '', 'single', 1, 1, 1,
            'normalize', 50, 60, 60, 1800, true, true, true, false,
            '{}'::bigint[], '{}'::bigint[], '{}'::bigint[], true, true, 1, now(), now()
        )
    `, session.BattleFormatID); err != nil {
		t.Fatalf("创建 Battle 测试赛制: %v", err)
	}
	if _, err := pool.Exec(ctx, `
        INSERT INTO battle_challenge (
            id, challenger_account_id, challenger_player_character_id, challenger_display_name,
            challenger_team_id, challenger_team_version, challenger_team_snapshot,
            target_account_id, target_player_character_id, target_display_name,
            battle_format_id, battle_format_snapshot, status, terminal_reason,
            expires_at, resolved_at, version, created_at, updated_at
        ) VALUES (
            $1, $2, $3, '甲', $4, 1, '{}'::jsonb,
            $5, $6, '乙', $7, '{}'::jsonb, 'accepted', 'accepted',
            now() + interval '5 minutes', now(), 2, now(), now()
        )
    `, session.ChallengeID, session.Participants[0].AccountID, session.Participants[0].PlayerCharacterID,
		session.Participants[0].Team.SourceTeamID, session.Participants[1].AccountID,
		session.Participants[1].PlayerCharacterID, session.BattleFormatID); err != nil {
		t.Fatalf("创建 Battle 测试 Challenge: %v", err)
	}
}

func seedAdditionalBattlePlayer(
	t *testing.T,
	ctx context.Context,
	pool *database.Pool,
	accountID snowflake.ID,
	characterID snowflake.ID,
	teamID snowflake.ID,
	displayName string,
) {
	t.Helper()
	if _, err := pool.Exec(ctx, `
        INSERT INTO account (
            id, username, username_key, password_hash, password_algorithm, password_parameters,
            status, security_version, created_at, updated_at, display_name
        ) VALUES ($1, $2, $2, 'test', 'argon2id', '{}'::jsonb, 'active', 1, now(), now(), $3)
	`, accountID, "battle-account-"+displayName, displayName); err != nil {
		t.Fatalf("创建额外 Battle 测试账号: %v", err)
	}
	if _, err := pool.Exec(ctx, `
        INSERT INTO player_character (id, account_id, display_name, display_name_key, version, created_at, updated_at)
        VALUES ($1, $2, $3, $3, 1, now(), now())
    `, characterID, accountID, displayName); err != nil {
		t.Fatalf("创建额外 Battle 测试角色: %v", err)
	}
	if _, err := pool.Exec(ctx, `
        INSERT INTO player_character_team (id, player_character_id, name, name_key, version, created_at, updated_at)
        VALUES ($1, $2, $3, $3, 1, now(), now())
    `, teamID, characterID, "测试队伍"+displayName); err != nil {
		t.Fatalf("创建额外 Battle 测试 Team: %v", err)
	}
}
