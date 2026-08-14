// Package persistence 提供 Battle 的 PostgreSQL 持久化适配器。
package persistence

import (
	"context"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"math"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"entgo.io/ent/dialect/sql"
	"github.com/jackc/pgx/v5/pgconn"
	avalonent "github.com/lishangbu/avalon/ent"
	entbattle "github.com/lishangbu/avalon/ent/battle"
	"github.com/lishangbu/avalon/ent/battleauthoritativesummary"
	"github.com/lishangbu/avalon/ent/battlebotstrategy"
	"github.com/lishangbu/avalon/ent/battlechallenge"
	"github.com/lishangbu/avalon/ent/battledisclosureledger"
	"github.com/lishangbu/avalon/ent/battleparticipant"
	"github.com/lishangbu/avalon/ent/battleparticipantreservation"
	battlepreviewsubmission "github.com/lishangbu/avalon/ent/battlepreviewsubmission"
	"github.com/lishangbu/avalon/ent/battleturnrecord"
	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/platform/database"
	"github.com/lishangbu/avalon/internal/rpg"
	"github.com/lishangbu/avalon/internal/team"
)

var (
	// ErrBattleNotFound 表示指定 Battle 不存在，或当前调用者不可读取其参与者视角。
	ErrBattleNotFound = errors.New("对战不存在")
	// ErrBattleConflict 表示乐观版本、唯一账号占用或同一回合记录已经被并发请求改变。
	ErrBattleConflict = errors.New("对战并发冲突")
	// ErrChallengeNotFound 表示指定 Challenge 不存在，或它已不属于可处理的待处理邀请。
	ErrChallengeNotFound = errors.New("挑战不存在")
)

// Adapters 汇集 Battle 表、回合记录、历史摘要和账号占用的 PostgreSQL 适配器。
type Adapters struct {
	// pool 提供普通 Preview、读取和回合提交使用的连接池与事务执行器。
	pool *database.Pool
	// newID 为 Outbox 等持久化事实生成稳定 Identifier，便于测试注入。
	newID snowflake.Source
	// encounterTerminalHandler 在同一事务内处理 Encounter 生命写回与 Checkpoint 恢复。
	encounterTerminalHandler battle.EncounterTerminalHandler
}

// NewAdapters 创建 Battle PostgreSQL 持久化适配器。
func NewAdapters(pool *database.Pool, newID snowflake.Source, encounterTerminalHandler battle.EncounterTerminalHandler) *Adapters {
	return &Adapters{pool: pool, newID: newID, encounterTerminalHandler: encounterTerminalHandler}
}

// GetEnabledBotStrategy 返回指定 Bot Code 当前唯一启用的版本化资料定义。
//
// 创建 Training Battle 的调用方会立刻严格校验并把 Definition 冻结到 battle_participant；因此此读取只
// 决定新 Battle 使用哪个版本，绝不会影响已经创建的对局。
func (store *Adapters) GetEnabledBotStrategy(ctx context.Context, code string) (battle.BotStrategyRecord, error) {
	if store == nil || store.pool == nil || strings.TrimSpace(code) == "" {
		return battle.BotStrategyRecord{}, battle.ErrBotStrategyUnavailable
	}
	rows, err := store.pool.Client(ctx).BattleBotStrategy.Query().Where(battlebotstrategy.CodeEQ(strings.TrimSpace(code)), battlebotstrategy.EnabledEQ(true)).Order(battlebotstrategy.ByVersion()).All(ctx)
	var row *avalonent.BattleBotStrategy
	if len(rows) > 0 {
		row = rows[len(rows)-1]
	} else {
		return battle.BotStrategyRecord{}, battle.ErrBotStrategyUnavailable
	}
	if avalonent.IsNotFound(err) {
		return battle.BotStrategyRecord{}, battle.ErrBotStrategyUnavailable
	}
	if err != nil {
		return battle.BotStrategyRecord{}, fmt.Errorf("读取已启用 Bot 策略: %w", err)
	}
	if row.Version < 1 || len(row.Definition) == 0 {
		return battle.BotStrategyRecord{}, battle.ErrBotStrategyUnavailable
	}
	return battle.BotStrategyRecord{
		Code: row.Code, Version: uint32(row.Version), Definition: append(json.RawMessage(nil), row.Definition...),
	}, nil
}

// Create 保存刚由已接受 Challenge 或 Training Battle 入口冻结出的 Preview Battle、Participant 与账号占用。
//
// 调用方必须已完成实时资料和 Team 校验；本方法不访问可变资料表，避免在写入期间绕开维护窗口策略。
func (store *Adapters) Create(ctx context.Context, session battle.Battle) error {
	if store == nil || store.pool == nil || !validNewBattle(session) {
		return battle.ErrInvalidBattle
	}
	return store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		return store.createBattleWithEnt(transactionCtx, store.pool.Client(transactionCtx), session)
	})
}

// CreateChallenge 保存一个尚未接受、已冻结发起方 Team 和实时资料修订的短期 Challenge。
func (store *Adapters) CreateChallenge(ctx context.Context, challenge battle.Challenge) error {
	if store == nil || store.pool == nil || !validNewChallenge(challenge) {
		return battle.ErrInvalidChallenge
	}
	return store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		if err := insertChallengeEnt(transactionCtx, store.pool.Client(transactionCtx), challenge); err != nil {
			if isUniqueViolation(err) {
				return ErrBattleConflict
			}
			return err
		}
		return nil
	})
}

// GetChallenge 返回完整 Challenge 冻结事实，供接收、拒绝、过期和运维任务使用。
func (store *Adapters) GetChallenge(ctx context.Context, challengeID snowflake.ID) (battle.Challenge, error) {
	if store == nil || store.pool == nil || challengeID == snowflake.ID(0) {
		return battle.Challenge{}, ErrChallengeNotFound
	}
	row, err := store.pool.Client(ctx).BattleChallenge.Query().Where(battlechallenge.IDEQ(challengeID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battle.Challenge{}, ErrChallengeNotFound
	}
	if err != nil {
		return battle.Challenge{}, fmt.Errorf("读取 Challenge: %w", err)
	}
	return challengeFromEnt(row)
}

// AcceptChallenge 在同一事务内接受 Challenge、创建 Preview Battle、占用双方账号并作废双方其他待处理邀请。
//
// targetTeam 必须已由应用层按接收方账号和实时资料重新校验；本方法只保存已经冻结的领域事实，
// 不访问可变 Team 或游戏资料表，避免持有 Challenge 行锁时穿透领域所有权。
func (store *Adapters) AcceptChallenge(
	ctx context.Context,
	challengeID snowflake.ID,
	targetPlayerCharacterID snowflake.ID,
	targetTeam team.Team,
	format battle.Format,
	acceptedAt time.Time,
) (battle.Battle, error) {
	if store == nil || store.pool == nil || store.newID == nil || challengeID == snowflake.ID(0) || targetPlayerCharacterID == snowflake.ID(0) {
		return battle.Battle{}, battle.ErrInvalidChallenge
	}
	var session battle.Battle
	err := store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		var acceptErr error
		session, acceptErr = store.acceptChallengeWithEnt(transactionCtx, store.pool.Client(transactionCtx), challengeID, targetPlayerCharacterID, targetTeam, format, acceptedAt)
		return acceptErr
	})
	return session, err
}

// acceptChallengeWithEnt 在事务中接受 Challenge、创建 Preview Battle 并作废双方其他待处理邀请。
func (store *Adapters) acceptChallengeWithEnt(ctx context.Context, client *avalonent.Client, challengeID snowflake.ID, targetPlayerCharacterID snowflake.ID, targetTeam team.Team, format battle.Format, acceptedAt time.Time) (battle.Battle, error) {
	row, err := client.BattleChallenge.Query().Where(battlechallenge.IDEQ(challengeID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battle.Battle{}, ErrChallengeNotFound
	}
	if err != nil {
		return battle.Battle{}, fmt.Errorf("读取 Challenge: %w", err)
	}
	challenge, err := challengeFromEnt(row)
	if err != nil {
		return battle.Battle{}, err
	}
	accepted, err := challenge.Accept(targetPlayerCharacterID, acceptedAt)
	if err != nil {
		return battle.Battle{}, err
	}
	session, err := battle.NewChallengeBattle(ctx, battle.NewChallengeBattleCommand{Challenge: accepted, TargetTeam: targetTeam, Format: format}, store.newID, func() time.Time { return acceptedAt })
	if err != nil {
		return battle.Battle{}, err
	}
	if err := store.createBattleWithEnt(ctx, client, session); err != nil {
		return battle.Battle{}, err
	}
	update := client.BattleChallenge.UpdateOne(row).Where(battlechallenge.VersionEQ(challenge.Version)).SetStatus(string(accepted.Status)).SetVersion(accepted.Version).SetUpdatedAt(accepted.UpdatedAt).SetResolvedAt(accepted.ResolvedAt)
	if accepted.TerminalReason != "" {
		update.SetTerminalReason(accepted.TerminalReason)
	} else {
		update.ClearTerminalReason()
	}
	if _, err := update.Save(ctx); avalonent.IsNotFound(err) {
		return battle.Battle{}, ErrBattleConflict
	} else if err != nil {
		return battle.Battle{}, fmt.Errorf("接受 Challenge: %w", err)
	}
	players := []snowflake.ID{accepted.ChallengerPlayerCharacterID, accepted.TargetPlayerCharacterID}
	_, err = client.BattleChallenge.Update().Where(battlechallenge.IDNEQ(accepted.ID), battlechallenge.StatusEQ(string(battle.ChallengePending)), battlechallenge.Or(battlechallenge.ChallengerPlayerCharacterIDIn(players...), battlechallenge.TargetPlayerCharacterIDIn(players...))).SetStatus(string(battle.ChallengeSuperseded)).SetTerminalReason("superseded").SetResolvedAt(accepted.ResolvedAt).SetUpdatedAt(accepted.ResolvedAt).AddVersion(1).Save(ctx)
	if err != nil {
		return battle.Battle{}, fmt.Errorf("作废关联 Challenge: %w", err)
	}
	return session, nil
}

// RejectChallenge 由目标 PlayerCharacter 在同一行锁事务内拒绝一个仍处于 pending 的 Challenge。
func (store *Adapters) RejectChallenge(
	ctx context.Context,
	challengeID snowflake.ID,
	targetPlayerCharacterID snowflake.ID,
	rejectedAt time.Time,
) (battle.Challenge, error) {
	return store.resolveChallenge(ctx, challengeID, func(value battle.Challenge) (battle.Challenge, error) {
		return value.Reject(targetPlayerCharacterID, rejectedAt)
	})
}

// WithdrawChallenge 由发起方 PlayerCharacter 在同一行锁事务内撤回一个仍处于 pending 的 Challenge。
func (store *Adapters) WithdrawChallenge(
	ctx context.Context,
	challengeID snowflake.ID,
	challengerPlayerCharacterID snowflake.ID,
	withdrawnAt time.Time,
) (battle.Challenge, error) {
	return store.resolveChallenge(ctx, challengeID, func(value battle.Challenge) (battle.Challenge, error) {
		return value.Withdraw(challengerPlayerCharacterID, withdrawnAt)
	})
}

// ExpireChallenge 将已超过固定有效期且尚未处理的 Challenge 标记为 expired。
//
// 该方法供定时任务与读取时懒惰过期共用；未到期、已终态或并发接受的邀请都不会被错误覆盖。
func (store *Adapters) ExpireChallenge(
	ctx context.Context,
	challengeID snowflake.ID,
	observedAt time.Time,
) (battle.Challenge, error) {
	return store.resolveChallenge(ctx, challengeID, func(value battle.Challenge) (battle.Challenge, error) {
		return value.Expire(observedAt)
	})
}

// ListExpiredChallengeIDs 返回尚未结算且已经到期的 Challenge 稳定 Identifier。
//
// 该方法只提供扫描候选项，不改变状态；Worker 必须继续调用 ExpireChallenge，以行锁二次确认候选项仍然
// 处于 pending，避免与接受、拒绝或撤回入口并发时产生重复终态。
func (store *Adapters) ListExpiredChallengeIDs(ctx context.Context, observedAt time.Time) ([]snowflake.ID, error) {
	if store == nil || store.pool == nil || observedAt.IsZero() {
		return nil, battle.ErrInvalidChallenge
	}
	rows, err := store.pool.Client(ctx).BattleChallenge.Query().Where(battlechallenge.StatusEQ(string(battle.ChallengePending)), battlechallenge.ExpiresAtLTE(observedAt.UTC())).Order(battlechallenge.ByID(sql.OrderAsc())).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询到期 Challenge: %w", err)
	}
	result := make([]snowflake.ID, len(rows))
	for index, row := range rows {
		result[index] = snowflake.ID(row.ID)
	}
	return result, nil
}

func (store *Adapters) resolveChallenge(
	ctx context.Context,
	challengeID snowflake.ID,
	transition func(battle.Challenge) (battle.Challenge, error),
) (battle.Challenge, error) {
	if store == nil || store.pool == nil || challengeID == snowflake.ID(0) || transition == nil {
		return battle.Challenge{}, ErrChallengeNotFound
	}
	var resolved battle.Challenge
	err := store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := store.pool.Client(transactionCtx)
		row, err := client.BattleChallenge.Query().Where(battlechallenge.IDEQ(challengeID)).Only(transactionCtx)
		if avalonent.IsNotFound(err) {
			return ErrChallengeNotFound
		}
		if err != nil {
			return fmt.Errorf("锁定 Challenge: %w", err)
		}
		current, err := challengeFromEnt(row)
		if err != nil {
			return err
		}
		resolved, err = transition(current)
		if err != nil {
			return err
		}
		update := client.BattleChallenge.UpdateOne(row).Where(battlechallenge.VersionEQ(current.Version)).SetStatus(string(resolved.Status)).SetVersion(resolved.Version).SetUpdatedAt(resolved.UpdatedAt)
		if resolved.TerminalReason != "" {
			update.SetTerminalReason(resolved.TerminalReason)
		} else {
			update.ClearTerminalReason()
		}
		if !resolved.ResolvedAt.IsZero() {
			update.SetResolvedAt(resolved.ResolvedAt)
		} else {
			update.ClearResolvedAt()
		}
		if _, err := update.Save(transactionCtx); avalonent.IsNotFound(err) {
			return ErrBattleConflict
		} else if err != nil {
			return fmt.Errorf("更新 Challenge 状态: %w", err)
		}
		return nil
	})
	return resolved, err
}

// Get 返回一个可安全交给领域服务和 Runtime 的完整 Battle 快照。
func (store *Adapters) Get(ctx context.Context, battleID snowflake.ID) (battle.Battle, error) {
	if store == nil || store.pool == nil || battleID == snowflake.ID(0) {
		return battle.Battle{}, ErrBattleNotFound
	}
	return loadBattleEnt(ctx, store.pool.Client(ctx), battleID)
}

// loadBattleEnt 读取 Battle 及其 Participant、Preview 子记录并组装领域快照。
func loadBattleEnt(ctx context.Context, client *avalonent.Client, battleID snowflake.ID) (battle.Battle, error) {
	row, err := client.Battle.Query().Where(entbattle.IDEQ(battleID)).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battle.Battle{}, ErrBattleNotFound
	}
	if err != nil {
		return battle.Battle{}, fmt.Errorf("读取 Battle: %w", err)
	}
	participants, err := client.BattleParticipant.Query().Where(battleparticipant.BattleIDEQ(battleID)).Order(battleparticipant.BySide()).All(ctx)
	if err != nil {
		return battle.Battle{}, fmt.Errorf("读取 Battle Participant: %w", err)
	}
	previews, err := client.BattlePreviewSubmission.Query().Where(battlepreviewsubmission.BattleIDEQ(battleID)).Order(battlepreviewsubmission.BySide()).All(ctx)
	if err != nil {
		return battle.Battle{}, fmt.Errorf("读取 Battle Preview: %w", err)
	}
	return battleFromEnt(row, participants, previews)
}

// ListExpiredPreviewBattleIDs 返回仍处于 preview 且 Preview 截止时间已经到达的 Battle Identifier。
//
// 结果仅是可重试的候选集合；CompleteExpiredPreview 会在同一 Battle 行锁内重新检查状态与截止时间。
func (store *Adapters) ListExpiredPreviewBattleIDs(ctx context.Context, observedAt time.Time) ([]snowflake.ID, error) {
	if store == nil || store.pool == nil || observedAt.IsZero() {
		return nil, battle.ErrInvalidBattle
	}
	rows, err := store.pool.Client(ctx).Battle.Query().Where(entbattle.StatusEQ(string(battle.StatusPreview)), entbattle.PreviewDeadlineAtLTE(observedAt.UTC())).Order(entbattle.ByID(sql.OrderAsc())).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询到期 Preview Battle: %w", err)
	}
	result := make([]snowflake.ID, len(rows))
	for index, row := range rows {
		result[index] = snowflake.ID(row.ID)
	}
	return result, nil
}

// ListExpiredRunningBattleIDs 返回仍处于 active 且整场截止时间已经到达的 Battle Identifier。
//
// 调用方必须通过 CompleteBattleTimeout 在锁定行内读取最后一次权威状态再裁定，不能把本查询时的
// 活跃状态直接当作超时依据。
func (store *Adapters) ListExpiredRunningBattleIDs(ctx context.Context, observedAt time.Time) ([]snowflake.ID, error) {
	if store == nil || store.pool == nil || observedAt.IsZero() {
		return nil, battle.ErrInvalidBattle
	}
	rows, err := store.pool.Client(ctx).Battle.Query().Where(entbattle.StatusEQ(string(battle.StatusRunning)), entbattle.StartedAtNotNil(), entbattle.BattleDeadlineAtLTE(observedAt.UTC())).Order(entbattle.ByID(sql.OrderAsc())).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询到期 Active Battle: %w", err)
	}
	result := make([]snowflake.ID, len(rows))
	for index, row := range rows {
		result[index] = snowflake.ID(row.ID)
	}
	return result, nil
}

// ListPendingRuntimeBattleIDs 返回已经完成 Preview、等待单实例 Server 创建本机 Runtime 的 Battle Identifier。
//
// 该查询只返回候选项；StartService 必须在随后行锁事务内重新确认 Starting 状态，避免与玩家提交
// Preview 后的同步启动路径发生竞态。
func (store *Adapters) ListPendingRuntimeBattleIDs(ctx context.Context) ([]snowflake.ID, error) {
	if store == nil || store.pool == nil {
		return nil, battle.ErrInvalidBattle
	}
	rows, err := store.pool.Client(ctx).Battle.Query().Where(entbattle.StatusEQ(string(battle.StatusRunning)), entbattle.StartedAtIsNil()).Order(entbattle.ByID(sql.OrderAsc())).All(ctx)
	if err != nil {
		return nil, fmt.Errorf("查询待启动 Battle: %w", err)
	}
	result := make([]snowflake.ID, len(rows))
	for index, row := range rows {
		result[index] = snowflake.ID(row.ID)
	}
	return result, nil
}

// SubmitPreview 以 Battle 行锁串行化秘密 Team Preview，拒绝覆盖已有选择并同步推进生命周期版本。
func (store *Adapters) SubmitPreview(
	ctx context.Context,
	battleID snowflake.ID,
	command battle.PreviewSubmissionCommand,
	submittedAt time.Time,
) (battle.Battle, error) {
	var updated battle.Battle
	err := store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := store.pool.Client(transactionCtx)
		current, err := loadBattleEnt(transactionCtx, client, battleID)
		if err != nil {
			return err
		}
		updated, err = current.SubmitPreview(command, submittedAt)
		if err != nil {
			return err
		}
		participant, found := participantByPlayerCharacter(updated.Participants, command.PlayerCharacterID)
		if !found {
			return battle.ErrInvalidBattle
		}
		preview, found := previewBySide(updated.PreviewSubmissions, participant.Side)
		if !found {
			return battle.ErrInvalidBattle
		}
		if err := insertPreviewSubmissionEnt(transactionCtx, client, store.newID, battleID, preview); err != nil {
			if isUniqueViolation(err) {
				return battle.ErrPreviewAlreadySubmitted
			}
			return err
		}
		rows, err := client.Battle.Update().Where(entbattle.IDEQ(battleID), entbattle.VersionEQ(current.Version)).SetStatus(string(updated.Status)).SetUpdatedAt(updated.UpdatedAt).AddVersion(1).Save(transactionCtx)
		if err != nil {
			return fmt.Errorf("推进 Battle Preview 状态: %w", err)
		}
		if rows != 1 {
			return ErrBattleConflict
		}
		return nil
	})
	return updated, err
}

// Start 将等待 Runtime 承载的 Battle 和完整初始 Battle Engine State 原子写入 running 状态，并登记活跃对局计数。
func (store *Adapters) Start(
	ctx context.Context,
	lease battle.RuntimeLease,
	initial battleengine.InitialState,
	randomSource battleengine.RandomSourceSnapshot,
	startedAt time.Time,
) (battle.Battle, error) {
	battleID := lease.BattleID
	if store == nil || store.pool == nil {
		return battle.Battle{}, battle.ErrInvalidBattle
	}
	initialEngineState, err := battleengine.NewState(initial)
	if err != nil {
		return battle.Battle{}, fmt.Errorf("对战初始战斗状态无效: %w", err)
	}
	initialState, err := json.Marshal(initial)
	if err != nil {
		return battle.Battle{}, fmt.Errorf("编码 Battle 初始战斗状态: %w", err)
	}
	if _, err := battleengine.RestoreRandomSource(randomSource); err != nil {
		return battle.Battle{}, fmt.Errorf("校验 Battle 随机源: %w", err)
	}
	randomSourcePayload, err := json.Marshal(randomSource)
	if err != nil {
		return battle.Battle{}, fmt.Errorf("编码 Battle 随机源: %w", err)
	}
	initialEventsPayload, initialEvents, err := encodeInitialEvents(initialEngineState.InitialEvents())
	if err != nil {
		return battle.Battle{}, err
	}
	var started battle.Battle
	err = store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		if err := store.validateRuntimeLease(transactionCtx, lease); err != nil {
			return err
		}
		client := store.pool.Client(transactionCtx)
		current, err := loadBattleEnt(transactionCtx, client, battleID)
		if err != nil {
			return err
		}
		started, err = current.Start(startedAt)
		if err != nil {
			return err
		}
		rows, err := client.Battle.Update().Where(entbattle.IDEQ(battleID), entbattle.VersionEQ(current.Version), entbattle.StatusEQ(string(battle.StatusRunning)), entbattle.StartedAtIsNil()).SetStatus(string(started.Status)).SetInitialState(initialState).SetRandomSource(randomSourcePayload).SetInitialEvents(initialEventsPayload).SetStartedAt(started.StartedAt).SetUpdatedAt(started.UpdatedAt).AddVersion(1).Save(transactionCtx)
		if err != nil {
			return fmt.Errorf("启动 Battle: %w", err)
		}
		if rows != 1 {
			return ErrBattleConflict
		}
		if err := writeDisclosureLedgerEnt(
			transactionCtx, client, store.newID, battleID, initialEngineState.Summary(), initialEvents, started.StateVersion, startedAt,
		); err != nil {
			return err
		}
		return nil
	})
	return started, err
}

// TurnCommitter 为单个 Battle 创建供内存 Runtime 使用的事务提交器。
func (store *Adapters) TurnCommitter(lease battle.RuntimeLease) battle.TurnCommitter {
	return turnCommitter{store: store, lease: lease}
}

type turnCommitter struct {
	store *Adapters
	lease battle.RuntimeLease
}

func (committer turnCommitter) CommitTurn(ctx context.Context, record battle.TurnRecord) error {
	if committer.store == nil || committer.store.pool == nil || record.BattleID != committer.lease.BattleID ||
		record.BattleID == snowflake.ID(0) || record.StateVersion < 1 || record.TurnNumber < 1 || len(record.Submissions) != 2 {
		return battle.ErrInvalidRuntime
	}
	if record.State.Result != nil {
		result, err := battleResultFromEngine(record.State.Result)
		if err != nil {
			return err
		}
		return committer.store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
			if err := committer.store.validateRuntimeLease(transactionCtx, committer.lease); err != nil {
				return err
			}
			client := committer.store.pool.Client(transactionCtx)
			if err := committer.commitTurnWithinEntTransaction(transactionCtx, client, record); err != nil {
				return err
			}
			_, err := committer.store.finishWithinEntTransaction(transactionCtx, client, record.BattleID, record.CreatedAt, &record.State, func(current battle.Battle) (battle.Battle, error) {
				return current.Complete(result, record.CreatedAt)
			})
			return err
		})
	}
	return committer.store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		if err := committer.store.validateRuntimeLease(transactionCtx, committer.lease); err != nil {
			return err
		}
		return committer.commitTurnWithinEntTransaction(transactionCtx, committer.store.pool.Client(transactionCtx), record)
	})
}

// commitTurnWithinTransaction 写入一次已经由 Runtime 完成的回合解析，但不自行决定事务的外层生命周期。
//
// 普通回合使用连接池事务直接调用它；引擎终局回合则由 WithinFinishedBattle 包裹，再在同一事务内释放
// 账号占用、递减活跃计数、写入历史与 Outbox，避免客户端看到已结束的状态而系统仍占用 Battle 名额。
func (committer turnCommitter) commitTurnWithinEntTransaction(
	ctx context.Context,
	client *avalonent.Client,
	record battle.TurnRecord,
) error {
	command, events, randomTrace, summary, err := encodeTurnRecord(record)
	if err != nil {
		return err
	}
	if _, err := battleengine.RestoreRandomSource(record.NextRandomSource); err != nil {
		return battle.ErrInvalidRuntime
	}
	randomSource, err := json.Marshal(record.NextRandomSource)
	if err != nil {
		return fmt.Errorf("编码 Battle 下一随机源: %w", err)
	}
	sessionEntity, err := client.Battle.Query().Where(entbattle.IDEQ(record.BattleID)).Only(ctx)
	if err != nil {
		return fmt.Errorf("读取 Battle 初始事件: %w", err)
	}
	initialEvents, err := decodeInitialEvents(sessionEntity.InitialEvents)
	if err != nil {
		return err
	}
	rows, err := client.Battle.Update().Where(entbattle.IDEQ(record.BattleID), entbattle.StatusEQ(string(battle.StatusRunning)), entbattle.StartedAtNotNil(), entbattle.StateVersionEQ(record.StateVersion-1)).SetStateVersion(record.StateVersion).SetRandomSource(randomSource).SetUpdatedAt(record.CreatedAt).Save(ctx)
	if err != nil {
		return fmt.Errorf("推进 Battle 权威状态版本: %w", err)
	}
	if rows != 1 {
		return ErrBattleConflict
	}
	turnRecordID, err := committer.store.newID.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成 Battle Turn Record Identifier: %w", err)
	}
	if err := client.BattleTurnRecord.Create().SetID(turnRecordID).SetBattleID(record.BattleID).SetStateVersion(record.StateVersion).SetTurnNumber(int32(record.TurnNumber)).SetSchemaVersion(int32(record.SchemaVersion)).SetCommand(command).SetEvents(events).SetRandomTrace(randomTrace).SetStateSummary(summary).SetCreatedAt(record.CreatedAt).Exec(ctx); err != nil {
		return fmt.Errorf("写入 Battle Turn Record: %w", err)
	}
	for _, submission := range record.Submissions {
		digest, err := hex.DecodeString(submission.RequestDigest)
		if err != nil || len(digest) != 32 || submission.IdempotencyKey == "" ||
			(submission.IsBot && (submission.PlayerCharacterID != snowflake.ID(0) || submission.BotCode == "" || submission.BotStrategyVersion == 0)) ||
			(!submission.IsBot && submission.PlayerCharacterID == snowflake.ID(0)) {
			return battle.ErrInvalidRuntime
		}
		submissionID, err := committer.store.newID.Next(ctx)
		if err != nil {
			return fmt.Errorf("生成 Battle Turn Submission Identifier: %w", err)
		}
		builder := client.BattleTurnSubmission.Create().SetID(submissionID).SetBattleID(record.BattleID).SetIdempotencyKey(submission.IdempotencyKey).SetSide(int16(submission.Side)).SetStateVersion(record.StateVersion).SetRequestDigest(digest).SetCreatedAt(record.CreatedAt)
		if submission.IsBot {
			builder.SetBotCode(submission.BotCode).SetBotStrategyVersion(int32(submission.BotStrategyVersion))
		} else {
			builder.SetPlayerCharacterID(submission.PlayerCharacterID)
		}
		if err := builder.Exec(ctx); err != nil {
			return fmt.Errorf("写入 Battle 回合幂等键: %w", err)
		}
	}
	if err := writeDisclosureLedgerEnt(ctx, client, committer.store.newID, record.BattleID, record.State, initialEvents, record.StateVersion, record.CreatedAt); err != nil {
		return err
	}
	return nil
}

// battleResultFromEngine 将纯 Battle Engine 的终局事实映射为 Battle 历史使用的稳定完成结果。
//
// 引擎只描述战斗规则内的结果；Battle 负责把它转换为数据库、历史与 Outbox 共用的终局原因。双方
// 同时失去全部成员和赛制最大回合数都不产生胜方，因此明确写入 draw 而非猜测性指定一方。
func battleResultFromEngine(result *battleengine.BattleResult) (battle.Result, error) {
	if result == nil {
		return battle.Result{}, battle.ErrInvalidBattleResult
	}
	switch result.Reason {
	case battleengine.BattleResultReasonAllMembersFainted:
		if result.WinningSide == battleengine.SideOne {
			return battle.Result{WinnerSide: battle.ParticipantSideOne, Reason: battle.TerminalReasonBattleEnded}, nil
		}
		if result.WinningSide == battleengine.SideTwo {
			return battle.Result{WinnerSide: battle.ParticipantSideTwo, Reason: battle.TerminalReasonBattleEnded}, nil
		}
		return battle.Result{Reason: battle.TerminalReasonDraw}, nil
	case battleengine.BattleResultReasonMaxTurnsReached:
		if result.WinningSide != 0 {
			return battle.Result{}, battle.ErrInvalidBattleResult
		}
		return battle.Result{Reason: battle.TerminalReasonDraw}, nil
	default:
		return battle.Result{}, battle.ErrInvalidBattleResult
	}
}

// writeDisclosureLedger 将同一权威状态和启动阶段公开事件投影为两个互相隔离的 Participant 视图，并在当前事务内写入。
//
// Turn Record 仍保存完整 StateSummary，供重放、审计和异步分析使用；绝不能将它直接复制到
// battle_disclosure_ledger。初始事件不伪装为第 0 回合，而是由每一份后续视图持续携带，保证重连时
// 不会遗失已公开的入场事实。该辅助函数集中保证所有写入路径都使用最小披露投影。
func writeDisclosureLedgerEnt(
	ctx context.Context,
	client *avalonent.Client,
	identifiers snowflake.Source,
	battleID snowflake.ID,
	state battleengine.StateSummary,
	initialEvents []json.RawMessage,
	stateVersion int64,
	updatedAt time.Time,
) error {
	if client == nil || battleID == snowflake.ID(0) || stateVersion < 0 || updatedAt.IsZero() {
		return battle.ErrInvalidBattle
	}
	for _, side := range []battle.ParticipantSide{battle.ParticipantSideOne, battle.ParticipantSideTwo} {
		view, err := battle.ParticipantViewFor(state, side, stateVersion)
		if err != nil {
			return err
		}
		view.InitialEvents = cloneJSONRawMessages(initialEvents)
		encoded, err := json.Marshal(view)
		if err != nil {
			return fmt.Errorf("编码 Battle 披露账本: %w", err)
		}
		row, queryErr := client.BattleDisclosureLedger.Query().Where(battledisclosureledger.BattleIDEQ(battleID), battledisclosureledger.SideEQ(int16(side))).Only(ctx)
		if queryErr == nil {
			_, err = client.BattleDisclosureLedger.UpdateOne(row).SetStateVersion(stateVersion).SetView(encoded).SetUpdatedAt(updatedAt).Save(ctx)
		} else if avalonent.IsNotFound(queryErr) {
			ledgerID, idErr := identifiers.Next(ctx)
			if idErr != nil {
				return fmt.Errorf("生成 Battle Disclosure Ledger Identifier: %w", idErr)
			}
			_, err = client.BattleDisclosureLedger.Create().SetID(ledgerID).SetBattleID(battleID).SetSide(int16(side)).SetStateVersion(stateVersion).SetView(encoded).SetUpdatedAt(updatedAt).Save(ctx)
		} else {
			err = queryErr
		}
		if err != nil {
			return fmt.Errorf("写入 Battle 披露账本: %w", err)
		}
	}
	return nil
}

// encodeInitialEvents 将启动阶段产生的接口事件编码为稳定 JSON 数组和可写入披露视图的独立原始消息。
//
// 初始事件不具备 Turn Record 的命令、随机轨迹或回合编号约束，因此必须在启动事务中单独编码；即使没有
// 事件也固定持久化空数组，避免 null 与“尚未启动”混淆。
func encodeInitialEvents(events []battleengine.Event) ([]byte, []json.RawMessage, error) {
	payload := make([]json.RawMessage, 0, len(events))
	for _, event := range events {
		encoded, err := json.Marshal(event)
		if err != nil {
			return nil, nil, fmt.Errorf("编码 Battle 初始事件: %w", err)
		}
		payload = append(payload, encoded)
	}
	encoded, err := json.Marshal(payload)
	if err != nil {
		return nil, nil, fmt.Errorf("编码 Battle 初始事件账本: %w", err)
	}
	return encoded, cloneJSONRawMessages(payload), nil
}

// decodeInitialEvents 严格解码已持久化的启动事件账本。
//
// 数据库中的值必须始终是 JSON 数组；将历史损坏数据显式拒绝，避免用空事件覆盖已公开的对局信息。
func decodeInitialEvents(payload []byte) ([]json.RawMessage, error) {
	var events []json.RawMessage
	if err := json.Unmarshal(payload, &events); err != nil {
		return nil, fmt.Errorf("解码 Battle 初始事件: %w", err)
	}
	if events == nil {
		return nil, fmt.Errorf("解码 Battle 初始事件: 必须是 JSON 数组")
	}
	for _, event := range events {
		if !json.Valid(event) {
			return nil, fmt.Errorf("解码 Battle 初始事件: 包含非法 JSON")
		}
	}
	return cloneJSONRawMessages(events), nil
}

// cloneJSONRawMessages 复制事件 JSON，避免事务编码或实时读取共享底层字节数组。
func cloneJSONRawMessages(source []json.RawMessage) []json.RawMessage {
	if source == nil {
		return nil
	}
	cloned := make([]json.RawMessage, len(source))
	for index, item := range source {
		cloned[index] = append(json.RawMessage(nil), item...)
	}
	return cloned
}

// GetParticipantDisclosure 返回指定真人 Participant 当前可读取的最后一份安全披露视图。
//
// 该方法先通过 Battle Participant 关系校验角色归属，再按固定 Side 读取独立账本；因此调用者
// 无法把另一个 PlayerCharacter ID 代入同一 Battle 来读取对手的秘密视图。对局尚未启动、尚未写入
// 初始视图或调用者不是参与者时，统一返回 ErrBattleNotFound，避免泄露 Battle 生命周期细节。
func (store *Adapters) GetParticipantDisclosure(
	ctx context.Context,
	battleID snowflake.ID,
	playerCharacterID snowflake.ID,
) (battle.DisclosureView, error) {
	if store == nil || store.pool == nil || battleID == snowflake.ID(0) || playerCharacterID == snowflake.ID(0) {
		return battle.DisclosureView{}, ErrBattleNotFound
	}
	session, err := store.Get(ctx, battleID)
	if err != nil {
		return battle.DisclosureView{}, err
	}
	participant, found := participantByPlayerCharacter(session.Participants, playerCharacterID)
	if !found {
		return battle.DisclosureView{}, ErrBattleNotFound
	}
	row, err := store.pool.Client(ctx).BattleDisclosureLedger.Query().Where(battledisclosureledger.BattleIDEQ(battleID), battledisclosureledger.SideEQ(int16(participant.Side))).Only(ctx)
	if avalonent.IsNotFound(err) {
		return battle.DisclosureView{}, ErrBattleNotFound
	}
	if err != nil {
		return battle.DisclosureView{}, fmt.Errorf("读取 Battle 披露账本: %w", err)
	}
	var view battle.DisclosureView
	if err := json.Unmarshal(row.View, &view); err != nil {
		return battle.DisclosureView{}, fmt.Errorf("解码 Battle 披露账本: %w", err)
	}
	if view.SchemaVersion != 1 || view.StateVersion != row.StateVersion {
		return battle.DisclosureView{}, fmt.Errorf("读取 Battle 披露账本: 数据不一致")
	}
	return view, nil
}

// TurnTimeoutCompleter 返回绑定当前 Runtime Lease 的回合超时终局写入器。
func (store *Adapters) TurnTimeoutCompleter(lease battle.RuntimeLease) battle.TurnTimeoutCompleter {
	return runtimeTurnTimeoutCompleter{store: store, lease: lease}
}

type runtimeTurnTimeoutCompleter struct {
	store *Adapters
	lease battle.RuntimeLease
}

func (completer runtimeTurnTimeoutCompleter) Complete(ctx context.Context, battleID snowflake.ID, result battle.Result, completedAt time.Time) (battle.Battle, error) {
	if completer.store == nil || battleID != completer.lease.BattleID {
		return battle.Battle{}, ErrRuntimeLeaseLost
	}
	var completed battle.Battle
	err := completer.store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		if err := completer.store.validateRuntimeLease(transactionCtx, completer.lease); err != nil {
			return err
		}
		var err error
		completed, err = completer.store.finishWithinEntTransaction(transactionCtx, completer.store.pool.Client(transactionCtx), battleID, completedAt, nil, func(current battle.Battle) (battle.Battle, error) {
			return current.Complete(result, completedAt)
		})
		return err
	})
	return completed, err
}

// Cancel 将尚未启动 Runtime 的 Battle 取消，并在同一事务释放占用、写入摘要与 Outbox。
func (store *Adapters) Cancel(ctx context.Context, battleID snowflake.ID, canceledAt time.Time) (battle.Battle, error) {
	if store == nil || store.pool == nil || battleID == snowflake.ID(0) || canceledAt.IsZero() {
		return battle.Battle{}, battle.ErrInvalidBattle
	}
	var canceled battle.Battle
	err := store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		var err error
		canceled, err = store.finishWithinEntTransaction(transactionCtx, store.pool.Client(transactionCtx), battleID, canceledAt.UTC(), nil, func(current battle.Battle) (battle.Battle, error) {
			return current.Cancel(canceledAt)
		})
		return err
	})
	return canceled, err
}

// InterruptRuntime 使用当前 holder 与 fencing token 中断 Runtime 承载的 Battle。
func (store *Adapters) InterruptRuntime(
	ctx context.Context,
	lease battle.RuntimeLease,
	reason battle.TerminalReason,
	interruptedAt time.Time,
) (battle.Battle, error) {
	var interrupted battle.Battle
	err := store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		if err := store.validateRuntimeLease(transactionCtx, lease); err != nil {
			return err
		}
		var err error
		interrupted, err = store.finishWithinEntTransaction(transactionCtx, store.pool.Client(transactionCtx), lease.BattleID, interruptedAt, nil, func(locked battle.Battle) (battle.Battle, error) {
			return locked.Interrupt(reason, interruptedAt)
		})
		return err
	})
	return interrupted, err
}

// CompleteExpiredPreview 为仍处于 preview 且截止时间已经到达的 Battle 原子补齐自动随机选择。
//
// 与通用 Interrupt 不同，该方法在锁定行中调用 Battle.CompleteExpiredPreview，因此周期扫描结果过期后不会
// 改写已经进入 starting 或 active 的 Battle。新选择和状态转换在同一事务提交，供 Server 进程随后的受控
// 启动循环读取。
func (store *Adapters) CompleteExpiredPreview(
	ctx context.Context,
	battleID snowflake.ID,
	observedAt time.Time,
) (battle.Battle, error) {
	if store == nil || store.pool == nil || battleID == snowflake.ID(0) || observedAt.IsZero() {
		return battle.Battle{}, battle.ErrInvalidBattle
	}
	var completed battle.Battle
	err := store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := store.pool.Client(transactionCtx)
		current, err := loadBattleEnt(transactionCtx, client, battleID)
		if err != nil {
			return err
		}
		completed, err = current.CompleteExpiredPreview(observedAt)
		if err != nil {
			return err
		}
		for _, preview := range completed.PreviewSubmissions {
			if _, exists := previewBySide(current.PreviewSubmissions, preview.Side); exists {
				continue
			}
			if err := insertPreviewSubmissionEnt(transactionCtx, client, store.newID, battleID, preview); err != nil {
				return err
			}
		}
		rows, err := client.Battle.Update().Where(entbattle.IDEQ(battleID), entbattle.VersionEQ(current.Version)).SetStatus(string(completed.Status)).SetUpdatedAt(completed.UpdatedAt).AddVersion(1).Save(transactionCtx)
		if err != nil {
			return fmt.Errorf("推进到期 Battle Preview 状态: %w", err)
		}
		if rows != 1 {
			return ErrBattleConflict
		}
		return nil
	})
	return completed, err
}

// CompleteBattleTimeout 以最后一次已提交 Turn Record 的状态摘要裁定一个到期 running Battle。
//
// 事务先锁定 Battle 行，随后读取初始状态和最新摘要，保证没有回合提交能夹在裁定读写之间。若尚未
// 提交任何回合，初始状态就是权威的零回合状态；裁定和释放活跃计数、账号占用、历史摘要、Outbox
// 保持同一事务提交。
func (store *Adapters) CompleteBattleTimeout(
	ctx context.Context,
	battleID snowflake.ID,
	observedAt time.Time,
) (battle.Battle, error) {
	if store == nil || store.pool == nil || battleID == snowflake.ID(0) || observedAt.IsZero() {
		return battle.Battle{}, battle.ErrInvalidBattle
	}
	var completed battle.Battle
	err := store.pool.WithinTransaction(ctx, func(transactionCtx context.Context) error {
		client := store.pool.Client(transactionCtx)
		current, err := loadBattleEnt(transactionCtx, client, battleID)
		if err != nil {
			return err
		}
		if current.Status != battle.StatusRunning || current.StartedAt.IsZero() || observedAt.Before(current.BattleDeadlineAt) {
			return battle.ErrBattleNotRunning
		}
		rowEntity, err := client.Battle.Query().Where(entbattle.IDEQ(battleID)).Only(transactionCtx)
		if err != nil {
			return fmt.Errorf("读取整场超时 Battle 初始状态: %w", err)
		}
		var initial battleengine.InitialState
		if len(rowEntity.InitialState) == 0 || json.Unmarshal(rowEntity.InitialState, &initial) != nil {
			return fmt.Errorf("%w: 解析活跃 Battle 初始状态", battle.ErrInvalidBattleResult)
		}
		state, err := battleengine.NewState(initial)
		if err != nil {
			return fmt.Errorf("%w: 校验活跃 Battle 初始状态", battle.ErrInvalidBattleResult)
		}
		summary := state.Summary()
		turn, summaryErr := client.BattleTurnRecord.Query().Where(battleturnrecord.BattleIDEQ(battleID)).Order(battleturnrecord.ByStateVersion(sql.OrderDesc())).First(transactionCtx)
		if summaryErr == nil {
			encodedSummary := turn.StateSummary
			if err := json.Unmarshal(encodedSummary, &summary); err != nil {
				return fmt.Errorf("%w: 解析最后 Battle 状态摘要", battle.ErrInvalidBattleResult)
			}
		} else if !avalonent.IsNotFound(summaryErr) {
			return fmt.Errorf("读取最后 Battle 状态摘要: %w", summaryErr)
		}
		result, err := battle.BattleTimeoutResult(initial, summary)
		if err != nil {
			return err
		}
		completed, err = current.Complete(result, observedAt.UTC())
		if err != nil {
			return err
		}
		update := client.Battle.Update().Where(entbattle.IDEQ(battleID), entbattle.StatusEQ(string(current.Status)), entbattle.VersionEQ(current.Version)).SetStatus(string(completed.Status)).SetResult(completed.Result).SetCompletedAt(completed.CompletedAt).SetUpdatedAt(completed.UpdatedAt).AddVersion(1)
		if completed.TerminalReason != "" {
			update.SetTerminalReason(completed.TerminalReason)
		} else {
			update.ClearTerminalReason()
		}
		rows, err := update.Save(transactionCtx)
		if err != nil {
			return fmt.Errorf("完成整场超时 Battle: %w", err)
		}
		if rows != 1 {
			return ErrBattleConflict
		}
		if _, err := client.BattleParticipantReservation.Delete().Where(battleparticipantreservation.BattleIDEQ(battleID)).Exec(transactionCtx); err != nil {
			return fmt.Errorf("释放整场超时 Battle 账号占用: %w", err)
		}
		return store.writeTerminalHistoryEnt(transactionCtx, client, completed, &summary)
	})
	return completed, err
}

// finishWithinEntTransaction 在 Ent 事务 Client 中完成 Battle 终局转换并释放账号占用。
func (store *Adapters) finishWithinEntTransaction(ctx context.Context, client *avalonent.Client, battleID snowflake.ID, completedAt time.Time, summary *battleengine.StateSummary, transition func(battle.Battle) (battle.Battle, error)) (battle.Battle, error) {
	current, err := loadBattleEnt(ctx, client, battleID)
	if err != nil {
		return battle.Battle{}, err
	}
	completed, err := transition(current)
	if err != nil {
		return battle.Battle{}, err
	}
	update := client.Battle.Update().Where(entbattle.IDEQ(battleID), entbattle.StatusEQ(string(current.Status)), entbattle.VersionEQ(current.Version)).SetStatus(string(completed.Status)).SetResult(completed.Result).SetCompletedAt(completedAt).SetUpdatedAt(completedAt).AddVersion(1)
	if completed.TerminalReason != "" {
		update.SetTerminalReason(completed.TerminalReason)
	} else {
		update.ClearTerminalReason()
	}
	if _, err := update.Save(ctx); avalonent.IsNotFound(err) {
		return battle.Battle{}, ErrBattleConflict
	} else if err != nil {
		return battle.Battle{}, fmt.Errorf("完成 Battle: %w", err)
	}
	if _, err := client.BattleParticipantReservation.Delete().Where(battleparticipantreservation.BattleIDEQ(battleID)).Exec(ctx); err != nil {
		return battle.Battle{}, fmt.Errorf("释放 Battle 账号占用: %w", err)
	}
	if err := store.writeTerminalHistoryEnt(ctx, client, completed, summary); err != nil {
		return battle.Battle{}, err
	}
	return completed, nil
}

// writeTerminalHistoryEnt 写入终局权威摘要与 Outbox 事实。
func (store *Adapters) writeTerminalHistoryEnt(ctx context.Context, client *avalonent.Client, session battle.Battle, summary *battleengine.StateSummary) error {
	if store.newID == nil {
		return battle.ErrInvalidBattle
	}
	if err := store.applyHeldItemConsumptionsEnt(ctx, client, session); err != nil {
		return err
	}
	var encounterTerminal *battle.EncounterTerminalResult
	if command, ok, err := store.encounterTerminalCommandEnt(ctx, client, session, summary); err != nil {
		return err
	} else if ok && store.encounterTerminalHandler != nil {
		result, handleErr := store.encounterTerminalHandler.HandleEncounterTerminal(ctx, command)
		if handleErr != nil {
			return fmt.Errorf("执行 PvE Checkpoint 恢复: %w", handleErr)
		}
		encounterTerminal = &result
	}
	payload, err := json.Marshal(struct {
		BattleID       snowflake.ID            `json:"battleId"`
		Mode           battle.BattleMode       `json:"mode"`
		SourceType     battle.BattleSourceType `json:"sourceType"`
		StateVersion   int64                   `json:"stateVersion"`
		TerminalReason string                  `json:"terminalReason"`
		Result         json.RawMessage         `json:"result,omitempty"`
		// EncounterTerminal 是 Encounter PvE 在同一事务中实际提交的生命与 Checkpoint 结果。
		EncounterTerminal *battle.EncounterTerminalResult `json:"encounterTerminal,omitempty"`
		CompletedAt       time.Time                       `json:"completedAt"`
	}{session.ID, session.Mode, session.SourceType, session.StateVersion, session.TerminalReason, session.Result, encounterTerminal, session.CompletedAt})
	if err != nil {
		return fmt.Errorf("编码 Battle 权威摘要: %w", err)
	}
	var winner *int16
	if session.Status == battle.StatusCompleted {
		var result battle.Result
		if err := json.Unmarshal(session.Result, &result); err != nil {
			return fmt.Errorf("解析已完成 Battle 结果: %w", err)
		}
		if result.WinnerSide != 0 {
			value := int16(result.WinnerSide)
			winner = &value
		}
	}
	if session.StateVersion > math.MaxInt32 {
		return battle.ErrInvalidBattle
	}
	if err := client.BattleAuthoritativeSummary.Create().SetID(session.ID).SetMode(string(session.Mode)).SetSourceType(string(session.SourceType)).SetNillableWinnerSide(winner).SetTerminalReason(session.TerminalReason).SetTurnCount(int32(session.StateVersion)).SetSummary(payload).SetCompletedAt(session.CompletedAt).Exec(ctx); err != nil {
		return fmt.Errorf("写入 Battle 权威摘要: %w", err)
	}
	outboxID, err := store.newID.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成 Battle Outbox 标识: %w", err)
	}
	if err := client.BattleOutbox.Create().SetID(outboxID).SetBattleID(session.ID).SetTopic("battle.terminal.v1").SetPayload(payload).SetCreatedAt(session.CompletedAt).Exec(ctx); err != nil {
		return fmt.Errorf("写入 Battle Outbox: %w", err)
	}
	return nil
}

// encounterTerminalCommandEnt 为 Encounter 正常终局选择调用方提供的引擎摘要，或从持久回合事实重建摘要。
// 非 PvE Encounter、非 completed 状态和缺少规范终局结果的 Battle 明确返回 ok=false，不触发 RPG 写回。
func (store *Adapters) encounterTerminalCommandEnt(ctx context.Context, client *avalonent.Client, session battle.Battle, provided *battleengine.StateSummary) (battle.EncounterTerminalCommand, bool, error) {
	if session.Status != battle.StatusCompleted || session.Mode != battle.BattleModePvE || session.SourceType != battle.BattleSourceEncounter || session.CompletedAt.IsZero() {
		return battle.EncounterTerminalCommand{}, false, nil
	}
	var result battle.Result
	if json.Unmarshal(session.Result, &result) != nil || !result.Reason.Valid() {
		return battle.EncounterTerminalCommand{}, false, nil
	}
	var summary battleengine.StateSummary
	if provided != nil {
		summary = *provided
	} else {
		var err error
		summary, err = terminalStateSummaryEnt(ctx, client, session.ID)
		if err != nil {
			return battle.EncounterTerminalCommand{}, false, err
		}
	}
	return encounterTerminalCommand(session, summary)
}

// encounterTerminalCommand 将真人 Party 快照与引擎终局摘要组合成一次可原子提交的 RPG 写回命令。
// 引擎生命按创建时冻结的持久生命上限等比例换算，避免规范化赛制等级覆盖 Owned Creature 的真实上限。
func encounterTerminalCommand(session battle.Battle, summary battleengine.StateSummary) (battle.EncounterTerminalCommand, bool, error) {
	if session.Status != battle.StatusCompleted || session.Mode != battle.BattleModePvE || session.SourceType != battle.BattleSourceEncounter || session.CompletedAt.IsZero() {
		return battle.EncounterTerminalCommand{}, false, nil
	}
	var result battle.Result
	if json.Unmarshal(session.Result, &result) != nil || !pveEncounterCompletedReason(result.Reason) {
		return battle.EncounterTerminalCommand{}, false, nil
	}
	for _, participant := range session.Participants {
		if participant.IsBot || participant.PlayerCharacterID == 0 || participant.Party == nil {
			continue
		}
		defeated := encounterDefeatReason(result.Reason) && result.WinnerSide != 0 && participant.Side != result.WinnerSide
		command := battle.EncounterTerminalCommand{BattleID: session.ID, PlayerCharacterID: participant.PlayerCharacterID, Defeated: defeated, CompletedAt: session.CompletedAt, Members: make([]battle.EncounterTerminalMember, 0, len(participant.Party.Members))}
		if !defeated && result.WinnerSide == participant.Side {
			command.Loot = participant.Party.Loot
		}
		for _, member := range participant.Party.Members {
			if member.PlayerCharacterCreatureID == 0 || member.MaximumHP <= 0 {
				return battle.EncounterTerminalCommand{}, false, nil
			}
			currentHP, engineMaximumHP, found := terminalMemberHP(summary, participant.Side, member.Position)
			if !found {
				currentHP = member.CurrentHP
			} else if currentHP > 0 {
				currentHP = int32(min((uint64(currentHP)*uint64(member.MaximumHP))/uint64(engineMaximumHP), uint64(member.MaximumHP)))
			}
			if currentHP < 0 || currentHP > member.MaximumHP {
				return battle.EncounterTerminalCommand{}, false, fmt.Errorf("读取 Encounter 终局生命: Party 成员位置 %d 数值无效", member.Position)
			}
			command.Members = append(command.Members, battle.EncounterTerminalMember{PlayerCharacterCreatureID: member.PlayerCharacterCreatureID, CurrentHP: currentHP, MaximumHP: member.MaximumHP})
		}
		return command, len(command.Members) > 0, nil
	}
	return battle.EncounterTerminalCommand{}, false, nil
}

// pveEncounterCompletedReason 限定允许写回 RPG 生命的正常 Encounter 终局原因。
func pveEncounterCompletedReason(reason battle.TerminalReason) bool {
	return encounterDefeatReason(reason) || reason == battle.TerminalReasonDraw || reason == battle.TerminalReasonNoContest
}

// terminalStateSummaryEnt 优先读取最后一条回合摘要；无回合记录时从 Battle 初始状态生成等价摘要。
// 后一种路径覆盖整场超时等 Runtime 尚未提交回合便结束的合法终局。
func terminalStateSummaryEnt(ctx context.Context, client *avalonent.Client, battleID snowflake.ID) (battleengine.StateSummary, error) {
	turn, err := client.BattleTurnRecord.Query().Where(battleturnrecord.BattleIDEQ(battleID)).Order(battleturnrecord.ByStateVersion(sql.OrderDesc())).First(ctx)
	if err == nil {
		var summary battleengine.StateSummary
		if json.Unmarshal(turn.StateSummary, &summary) != nil {
			return battleengine.StateSummary{}, fmt.Errorf("解析 Encounter 最后状态摘要失败")
		}
		return summary, nil
	}
	if !avalonent.IsNotFound(err) {
		return battleengine.StateSummary{}, fmt.Errorf("读取 Encounter 最后状态摘要: %w", err)
	}
	row, err := client.Battle.Query().Where(entbattle.IDEQ(battleID)).Only(ctx)
	if err != nil {
		return battleengine.StateSummary{}, fmt.Errorf("读取 Encounter 初始状态: %w", err)
	}
	var initial battleengine.InitialState
	if json.Unmarshal(row.InitialState, &initial) != nil {
		return battleengine.StateSummary{}, fmt.Errorf("解析 Encounter 初始状态失败")
	}
	state, err := battleengine.NewState(initial)
	if err != nil {
		return battleengine.StateSummary{}, fmt.Errorf("校验 Encounter 初始状态: %w", err)
	}
	return state.Summary(), nil
}

// terminalMemberHP 按 Battle 阵营和冻结成员位置查找引擎终局生命，并拒绝超出持久 int32 边界的值。
func terminalMemberHP(summary battleengine.StateSummary, side battle.ParticipantSide, position int16) (int32, int32, bool) {
	engineSide := battleengine.SideTwo
	if side == battle.ParticipantSideOne {
		engineSide = battleengine.SideOne
	} else if side != battle.ParticipantSideTwo {
		return 0, 0, false
	}
	for _, member := range summary.Members {
		if member.Side == engineSide && member.MemberPosition == battleengine.MemberPosition(position) && member.CurrentHP <= math.MaxInt32 && member.MaxHP > 0 && member.MaxHP <= math.MaxInt32 {
			return int32(member.CurrentHP), int32(member.MaxHP), true
		}
	}
	return 0, 0, false
}

// encounterDefeatReason 识别能够证明一方明确战败的终局原因；平局和无结果不能触发 Checkpoint 恢复。
func encounterDefeatReason(reason battle.TerminalReason) bool {
	return reason == battle.TerminalReasonBattleEnded || reason == battle.TerminalReasonSurrender ||
		reason == battle.TerminalReasonTurnTimeout || reason == battle.TerminalReasonBattleTimeout
}

// ListHistory 返回某一 PlayerCharacter 可见的历史和精确总数。
//
// 统一页码只适用于受限历史页；调用方不得把页码当作可长期保存的游标，因为新的终局 Battle 会改变后续页的位置。
func (store *Adapters) ListHistory(
	ctx context.Context,
	playerCharacterID snowflake.ID,
	page int32,
	pageSize int32,
) (battle.HistoryPage, error) {
	if page < 1 || pageSize < 1 || pageSize > 100 || playerCharacterID == snowflake.ID(0) {
		return battle.HistoryPage{}, battle.ErrInvalidBattle
	}
	offset := int64(page-1) * int64(pageSize)
	if offset > math.MaxInt32 {
		return battle.HistoryPage{}, battle.ErrInvalidBattle
	}
	client := store.pool.Client(ctx)
	participants, err := client.BattleParticipant.Query().Where(battleparticipant.PlayerCharacterIDEQ(playerCharacterID)).All(ctx)
	if err != nil {
		return battle.HistoryPage{}, fmt.Errorf("查询 Battle 历史参赛记录: %w", err)
	}
	battleIDs := make([]snowflake.ID, 0, len(participants))
	participantByBattle := make(map[snowflake.ID]*avalonent.BattleParticipant, len(participants))
	for _, participant := range participants {
		battleIDs = append(battleIDs, participant.BattleID)
		participantByBattle[participant.BattleID] = participant
	}
	if len(battleIDs) == 0 {
		return battle.HistoryPage{Items: []battle.HistoryEntry{}, Page: page, PageSize: pageSize, Total: 0}, nil
	}
	summaryQuery := client.BattleAuthoritativeSummary.Query().Where(battleauthoritativesummary.IDIn(battleIDs...))
	total, err := summaryQuery.Count(ctx)
	if err != nil {
		return battle.HistoryPage{}, fmt.Errorf("统计 Battle 历史: %w", err)
	}
	rows, err := summaryQuery.Order(battleauthoritativesummary.ByCompletedAt(sql.OrderDesc()), battleauthoritativesummary.ByID(sql.OrderDesc())).Offset(int(offset)).Limit(int(pageSize)).All(ctx)
	if err != nil {
		return battle.HistoryPage{}, fmt.Errorf("查询 Battle 历史: %w", err)
	}
	entries := make([]battle.HistoryEntry, 0, len(rows))
	for _, row := range rows {
		participant := participantByBattle[row.ID]
		if participant == nil {
			continue
		}
		entry := battle.HistoryEntry{
			BattleID: snowflake.ID(row.ID), Mode: battle.BattleMode(row.Mode), SourceType: battle.BattleSourceType(row.SourceType), Side: battle.ParticipantSide(participant.Side),
			DisplayName: participant.DisplayName, TerminalReason: row.TerminalReason,
			TurnCount: row.TurnCount, Summary: append(json.RawMessage(nil), row.Summary...), CompletedAt: row.CompletedAt.UTC(),
		}
		if row.WinnerSide != nil {
			entry.WinnerSide = battle.ParticipantSide(*row.WinnerSide)
		}
		entries = append(entries, entry)
	}
	return battle.HistoryPage{Items: entries, Page: page, PageSize: pageSize, Total: int64(total)}, nil
}

func validNewBattle(session battle.Battle) bool {
	return session.ID != snowflake.ID(0) && session.Status == battle.StatusPreview && session.StateVersion == 0 &&
		session.Version >= 1 && len(session.Participants) == 2 && json.Valid(session.BattleFormatSnapshot)
}

func validNewChallenge(challenge battle.Challenge) bool {
	return challenge.ID != snowflake.ID(0) && challenge.Status == battle.ChallengePending &&
		challenge.ChallengerAccountID != snowflake.ID(0) && challenge.ChallengerPlayerCharacterID != snowflake.ID(0) &&
		challenge.TargetAccountID != snowflake.ID(0) && challenge.TargetPlayerCharacterID != snowflake.ID(0) &&
		challenge.ChallengerAccountID != challenge.TargetAccountID &&
		challenge.ChallengerPlayerCharacterID != challenge.TargetPlayerCharacterID &&
		challenge.ChallengerTeam.SourceTeamID != snowflake.ID(0) && challenge.ChallengerTeam.SourceTeamVersion >= 1 &&
		len(challenge.ChallengerTeam.Members) > 0 && challenge.BattleFormatID != snowflake.ID(0) &&
		json.Valid(challenge.BattleFormatSnapshot) && challenge.Version >= 1 &&
		!challenge.CreatedAt.IsZero() && !challenge.ExpiresAt.IsZero() && challenge.ExpiresAt.After(challenge.CreatedAt)
}

// createBattleWithEnt 使用 Ent 在单个事务中保存 Battle、Participant、账号占用和 Preview 快照。
// 所有写入都通过同一个事务 Client，确保对局创建不会留下半条冻结事实。
func (store *Adapters) createBattleWithEnt(ctx context.Context, client *avalonent.Client, session battle.Battle) error {
	if err := insertBattleEnt(ctx, client, session); err != nil {
		return err
	}
	for _, participant := range session.Participants {
		if err := insertParticipantEnt(ctx, client, store.newID, session.ID, participant); err != nil {
			return err
		}
		if !participant.IsBot {
			if _, err := client.BattleParticipantReservation.Create().SetID(participant.PlayerCharacterID).SetBattleID(session.ID).SetCreatedAt(session.CreatedAt).Save(ctx); err != nil {
				if isUniqueViolation(err) {
					return ErrBattleConflict
				}
				return fmt.Errorf("创建 Battle 账号占用: %w", err)
			}
		}
	}
	for _, preview := range session.PreviewSubmissions {
		if err := insertPreviewSubmissionEnt(ctx, client, store.newID, session.ID, preview); err != nil {
			return err
		}
	}
	return nil
}

// insertBattleEnt 写入 Battle 的冻结赛制和生命周期初始状态。
func insertBattleEnt(ctx context.Context, client *avalonent.Client, session battle.Battle) error {
	format, err := json.Marshal(session.Format)
	if err != nil {
		return fmt.Errorf("编码 Battle Format: %w", err)
	}
	b := client.Battle.Create().SetID(session.ID).SetMode(string(session.Mode)).SetSourceType(string(session.SourceType)).SetStatus(string(session.Status)).SetBattleFormatID(session.BattleFormatID).SetBattleFormatSnapshot(session.BattleFormatSnapshot).SetFormat(format).SetPreviewDeadlineAt(session.PreviewDeadlineAt).SetBattleDeadlineAt(session.BattleDeadlineAt).SetStateVersion(session.StateVersion).SetVersion(session.Version).SetCreatedAt(session.CreatedAt).SetUpdatedAt(session.UpdatedAt)
	if session.ChallengeID != snowflake.ID(0) {
		b.SetChallengeID(session.ChallengeID)
	}
	if _, err := b.Save(ctx); err != nil {
		return fmt.Errorf("创建 Battle: %w", err)
	}
	return nil
}

// insertParticipantEnt 写入参赛方及其完整 Team/Bot 冻结快照。
func insertParticipantEnt(ctx context.Context, client *avalonent.Client, identifiers snowflake.Source, battleID snowflake.ID, participant battle.Participant) error {
	// Team 输入与人物 Equipment 使用同一个 Participant 快照边界；装备读取使用当前 Battle 创建事务，
	// 因而 Loadout 与 Participant 要么一起提交，要么一起回滚。
	if !participant.IsBot {
		var err error
		participant.Equipment, err = rpg.FreezePlayerCharacterEquipmentWithEnt(ctx, client, participant.PlayerCharacterID)
		if err != nil {
			return fmt.Errorf("冻结 Battle Equipment Snapshot: %w", err)
		}
	}
	type teamInputSnapshot struct {
		Team      battle.TeamSnapshot `json:"team"`
		Equipment json.RawMessage     `json:"equipment"`
	}
	snapshot, err := json.Marshal(teamInputSnapshot{Team: participant.Team, Equipment: participant.Equipment})
	if err != nil {
		return fmt.Errorf("编码 Battle Participant Team 快照: %w", err)
	}
	participantID, err := identifiers.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成 Battle Participant Identifier: %w", err)
	}
	b := client.BattleParticipant.Create().SetID(participantID).SetBattleID(battleID).SetSide(int16(participant.Side)).SetDisplayName(participant.DisplayName).SetInputSnapshot(snapshot)
	if participant.IsBot {
		b.SetParticipantType("bot").SetInputType("generated").SetBotCode(participant.BotCode).SetBotStrategyVersion(int32(participant.BotStrategyVersion)).SetBotDefinition(participant.BotDefinition)
	} else {
		b.SetParticipantType("player_character").SetInputType("team").SetAccountID(participant.AccountID).SetPlayerCharacterID(participant.PlayerCharacterID).SetSourceTeamID(participant.Team.SourceTeamID).SetSourceTeamVersion(participant.Team.SourceTeamVersion)
	}
	if _, err := b.Save(ctx); err != nil {
		return fmt.Errorf("创建 Battle Participant: %w", err)
	}
	return nil
}

// insertPreviewSubmissionEnt 保存 Preview 的成员、上场位置和随机轨迹。
func insertPreviewSubmissionEnt(ctx context.Context, client *avalonent.Client, identifiers snowflake.Source, battleID snowflake.ID, preview battle.PreviewSubmission) error {
	members, err := json.Marshal(preview.MemberPositions)
	if err != nil {
		return fmt.Errorf("编码 Battle Preview 成员位置: %w", err)
	}
	active, err := json.Marshal(preview.ActivePositions)
	if err != nil {
		return fmt.Errorf("编码 Battle Preview 上场位置: %w", err)
	}
	trace := preview.RandomTrace
	if len(trace) == 0 {
		trace = json.RawMessage("[]")
	}
	if !json.Valid(trace) || trace[0] != '[' {
		return battle.ErrInvalidBattle
	}
	previewID, err := identifiers.Next(ctx)
	if err != nil {
		return fmt.Errorf("生成 Battle Preview Submission Identifier: %w", err)
	}
	if _, err := client.BattlePreviewSubmission.Create().SetID(previewID).SetBattleID(battleID).SetSide(int16(preview.Side)).SetMemberPositions(members).SetActivePositions(active).SetSubmittedAt(preview.SubmittedAt).SetRandomTrace(trace).Save(ctx); err != nil {
		return fmt.Errorf("保存 Battle Preview: %w", err)
	}
	return nil
}

// insertChallengeEnt 写入待处理 Challenge 及发起方 Team 冻结快照。
func insertChallengeEnt(ctx context.Context, client *avalonent.Client, challenge battle.Challenge) error {
	snapshot, err := json.Marshal(challenge.ChallengerTeam)
	if err != nil {
		return fmt.Errorf("编码 Challenge 发起方 Team 快照: %w", err)
	}
	b := client.BattleChallenge.Create().SetID(challenge.ID).SetChallengerAccountID(challenge.ChallengerAccountID).SetChallengerPlayerCharacterID(challenge.ChallengerPlayerCharacterID).SetChallengerDisplayName(challenge.ChallengerDisplayName).SetChallengerTeamID(challenge.ChallengerTeam.SourceTeamID).SetChallengerTeamVersion(challenge.ChallengerTeam.SourceTeamVersion).SetChallengerTeamSnapshot(snapshot).SetTargetAccountID(challenge.TargetAccountID).SetTargetPlayerCharacterID(challenge.TargetPlayerCharacterID).SetTargetDisplayName(challenge.TargetDisplayName).SetBattleFormatID(challenge.BattleFormatID).SetBattleFormatSnapshot(challenge.BattleFormatSnapshot).SetStatus(string(challenge.Status)).SetExpiresAt(challenge.ExpiresAt).SetVersion(challenge.Version).SetCreatedAt(challenge.CreatedAt).SetUpdatedAt(challenge.UpdatedAt)
	if challenge.TerminalReason != "" {
		b.SetTerminalReason(challenge.TerminalReason)
	}
	if !challenge.ResolvedAt.IsZero() {
		b.SetResolvedAt(challenge.ResolvedAt)
	}
	if _, err := b.Save(ctx); err != nil {
		return fmt.Errorf("创建 Challenge: %w", err)
	}
	return nil
}

// challengeFromEnt 将 Ent Challenge 实体转换为包含冻结 Team 快照的领域对象。
func challengeFromEnt(row *avalonent.BattleChallenge) (battle.Challenge, error) {
	challenge := battle.Challenge{ID: snowflake.ID(row.ID), ChallengerAccountID: snowflake.ID(row.ChallengerAccountID), ChallengerPlayerCharacterID: snowflake.ID(row.ChallengerPlayerCharacterID), ChallengerDisplayName: row.ChallengerDisplayName, TargetAccountID: snowflake.ID(row.TargetAccountID), TargetPlayerCharacterID: snowflake.ID(row.TargetPlayerCharacterID), TargetDisplayName: row.TargetDisplayName, BattleFormatID: snowflake.ID(row.BattleFormatID), BattleFormatSnapshot: append(json.RawMessage(nil), row.BattleFormatSnapshot...), Status: battle.ChallengeStatus(row.Status), Version: row.Version, ExpiresAt: row.ExpiresAt.UTC(), CreatedAt: row.CreatedAt.UTC(), UpdatedAt: row.UpdatedAt.UTC()}
	if err := json.Unmarshal(row.ChallengerTeamSnapshot, &challenge.ChallengerTeam); err != nil {
		return battle.Challenge{}, fmt.Errorf("解析 Challenge 发起方 Team 快照: %w", err)
	}
	if challenge.ChallengerTeam.SourceTeamID != snowflake.ID(row.ChallengerTeamID) || challenge.ChallengerTeam.SourceTeamVersion != row.ChallengerTeamVersion {
		return battle.Challenge{}, fmt.Errorf("挑战发起方队伍快照与来源不一致: %w", battle.ErrInvalidChallenge)
	}
	if row.TerminalReason != nil {
		challenge.TerminalReason = *row.TerminalReason
	}
	if row.ResolvedAt != nil {
		challenge.ResolvedAt = row.ResolvedAt.UTC()
	}
	return challenge, nil
}

// battleFromEnt 将 Ent 查询结果组装为完整 Battle 快照，并校验冻结 JSON 的基本结构。
func battleFromEnt(row *avalonent.Battle, participantRows []*avalonent.BattleParticipant, previewRows []*avalonent.BattlePreviewSubmission) (battle.Battle, error) {
	format := battle.Format{}
	if err := json.Unmarshal(row.Format, &format); err != nil {
		return battle.Battle{}, fmt.Errorf("解析 Battle Format: %w", err)
	}
	session := battle.Battle{ID: snowflake.ID(row.ID), Mode: battle.BattleMode(row.Mode), SourceType: battle.BattleSourceType(row.SourceType), Status: battle.Status(row.Status), BattleFormatID: snowflake.ID(row.BattleFormatID), BattleFormatSnapshot: append(json.RawMessage(nil), row.BattleFormatSnapshot...), Format: format, PreviewDeadlineAt: row.PreviewDeadlineAt.UTC(), BattleDeadlineAt: row.BattleDeadlineAt.UTC(), Result: append(json.RawMessage(nil), row.Result...), StateVersion: row.StateVersion, Version: row.Version, CreatedAt: row.CreatedAt.UTC(), UpdatedAt: row.UpdatedAt.UTC()}
	if row.ChallengeID != nil {
		session.ChallengeID = snowflake.ID(*row.ChallengeID)
	}
	if row.TerminalReason != nil {
		session.TerminalReason = *row.TerminalReason
	}
	if row.StartedAt != nil {
		session.StartedAt = row.StartedAt.UTC()
	}
	if row.CompletedAt != nil {
		session.CompletedAt = row.CompletedAt.UTC()
	}
	session.Participants = make([]battle.Participant, 0, len(participantRows))
	for _, value := range participantRows {
		participant := battle.Participant{Side: battle.ParticipantSide(value.Side), DisplayName: value.DisplayName, IsBot: value.ParticipantType == "bot"}
		switch value.InputType {
		case "party":
			participant.Party = &battle.PartyBattleSnapshot{}
			if err := json.Unmarshal(value.InputSnapshot, participant.Party); err != nil {
				return battle.Battle{}, fmt.Errorf("解析 Battle Participant Party 快照: %w", err)
			}
			participant.Team = participant.Party.Team
		case "generated":
			if err := json.Unmarshal(value.InputSnapshot, &participant.Team); err != nil {
				return battle.Battle{}, fmt.Errorf("解析 Battle Participant Generated Team 快照: %w", err)
			}
		default:
			var snapshot struct {
				Team      battle.TeamSnapshot `json:"team"`
				Equipment json.RawMessage     `json:"equipment"`
			}
			if err := json.Unmarshal(value.InputSnapshot, &snapshot); err != nil {
				return battle.Battle{}, fmt.Errorf("解析 Battle Participant Team 与 Equipment 快照: %w", err)
			}
			if len(snapshot.Equipment) == 0 || !json.Valid(snapshot.Equipment) {
				return battle.Battle{}, fmt.Errorf("解析 Battle Participant Team 与 Equipment 快照: %w", battle.ErrInvalidBattle)
			}
			participant.Team = snapshot.Team
			participant.Equipment = append(json.RawMessage(nil), snapshot.Equipment...)
		}
		if value.ParticipantType == "bot" {
			if value.BotCode != nil {
				participant.BotCode = *value.BotCode
			}
			if value.BotStrategyVersion != nil {
				participant.BotStrategyVersion = uint32(*value.BotStrategyVersion)
			}
			participant.BotDefinition = append(json.RawMessage(nil), value.BotDefinition...)
			if _, _, err := battle.DecodeBotStrategyDefinition(participant.BotDefinition); err != nil {
				return battle.Battle{}, fmt.Errorf("解析冻结 Battle Bot 定义: %w", err)
			}
		} else {
			if value.AccountID != nil {
				participant.AccountID = snowflake.ID(*value.AccountID)
			}
			if value.PlayerCharacterID != nil {
				participant.PlayerCharacterID = snowflake.ID(*value.PlayerCharacterID)
			}
		}
		session.Participants = append(session.Participants, participant)
	}
	session.PreviewSubmissions = make([]battle.PreviewSubmission, 0, len(previewRows))
	for _, value := range previewRows {
		preview := battle.PreviewSubmission{Side: battle.ParticipantSide(value.Side), SubmittedAt: value.SubmittedAt.UTC(), RandomTrace: append(json.RawMessage(nil), value.RandomTrace...)}
		if err := json.Unmarshal(value.MemberPositions, &preview.MemberPositions); err != nil {
			return battle.Battle{}, fmt.Errorf("解析 Battle Preview 成员位置: %w", err)
		}
		if err := json.Unmarshal(value.ActivePositions, &preview.ActivePositions); err != nil {
			return battle.Battle{}, fmt.Errorf("解析 Battle Preview 上场位置: %w", err)
		}
		if !json.Valid(preview.RandomTrace) || len(preview.RandomTrace) == 0 || preview.RandomTrace[0] != '[' {
			return battle.Battle{}, fmt.Errorf("解析 Battle Preview 随机轨迹: 非法 JSON 数组")
		}
		session.PreviewSubmissions = append(session.PreviewSubmissions, preview)
	}
	return session, nil
}

func encodeTurnRecord(record battle.TurnRecord) ([]byte, []byte, []byte, []byte, error) {
	command, err := json.Marshal(record.Command)
	if err != nil {
		return nil, nil, nil, nil, fmt.Errorf("编码 Battle 回合命令: %w", err)
	}
	eventsPayload := record.Events
	if eventsPayload == nil {
		eventsPayload = []json.RawMessage{}
	}
	events, err := json.Marshal(eventsPayload)
	if err != nil {
		return nil, nil, nil, nil, fmt.Errorf("编码 Battle 回合事件: %w", err)
	}
	randomTracePayload := record.RandomTrace
	if randomTracePayload == nil {
		randomTracePayload = []battleengine.RandomTraceEntry{}
	}
	randomTrace, err := json.Marshal(randomTracePayload)
	if err != nil {
		return nil, nil, nil, nil, fmt.Errorf("编码 Battle 随机轨迹: %w", err)
	}
	summary, err := json.Marshal(record.State)
	if err != nil {
		return nil, nil, nil, nil, fmt.Errorf("编码 Battle 状态摘要: %w", err)
	}
	return command, events, randomTrace, summary, nil
}

func participantByPlayerCharacter(participants []battle.Participant, playerCharacterID snowflake.ID) (battle.Participant, bool) {
	for _, participant := range participants {
		if !participant.IsBot && participant.PlayerCharacterID == playerCharacterID {
			return participant, true
		}
	}
	return battle.Participant{}, false
}

func previewBySide(previews []battle.PreviewSubmission, side battle.ParticipantSide) (battle.PreviewSubmission, bool) {
	for _, preview := range previews {
		if preview.Side == side {
			return preview, true
		}
	}
	return battle.PreviewSubmission{}, false
}

func isUniqueViolation(err error) bool {
	var databaseError *pgconn.PgError
	return errors.As(err, &databaseError) && databaseError.Code == "23505"
}
