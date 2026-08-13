package battle

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battleengine"
)

var (
	// ErrInvalidRuntime 表示 Runtime 缺少 running Battle、双方 Participant、引擎状态或提交器等必需事实。
	ErrInvalidRuntime = errors.New("对战 Runtime 无效")
	// ErrRuntimeStateVersionConflict 表示客户端基于已经过期的权威战斗状态提交命令。
	ErrRuntimeStateVersionConflict = errors.New("对战状态版本冲突")
	// ErrRuntimeParticipantMismatch 表示调用者不是本场 Battle 的真人 Participant，或试图提交另一方槽位。
	ErrRuntimeParticipantMismatch = errors.New("对战参赛方不匹配")
	// ErrRuntimeIdempotencyConflict 表示同一幂等键被用于内容不同的回合提交。
	ErrRuntimeIdempotencyConflict = errors.New("对战回合幂等键冲突")
	// ErrRuntimeClockInvalid 表示 Runtime 提供的逻辑时钟早于战斗开始时间，无法写入可重放的 TimeInput。
	ErrRuntimeClockInvalid = errors.New("对战 Runtime 时钟无效")
	// ErrBotStrategyUnavailable 表示 Training Battle 冻结的 Bot 代码和版本在当前进程没有对应策略实现。
	ErrBotStrategyUnavailable = errors.New("对战机器人策略不可用")
)

// TurnSubmission 是一名真人 Participant 对己方全部当前场上槽位的秘密回合选择。
//
// 双打时 Actions 必须覆盖该 Participant 所有需要人工决策的槽位；Runtime 在双方都锁定后才把
// 两份提交组合为纯 Battle Engine 的完整 TurnCommand，因此提交结果不会在此之前泄露对方选择。
type TurnSubmission struct {
	// PlayerCharacterID 是当前提交者的活动 PlayerCharacter 稳定 Identifier。
	PlayerCharacterID snowflake.ID `json:"playerCharacterId"`
	// ExpectedStateVersion 是客户端视图基于的已提交 Turn Record 状态版本。
	ExpectedStateVersion int64 `json:"expectedStateVersion"`
	// IdempotencyKey 是本次提交在本场 Battle 内唯一、可重试的客户端幂等键。
	IdempotencyKey string `json:"idempotencyKey"`
	// Actions 是仅属于提交者固定 Side 的全部行动，Runtime 会在组合前验证 Side 归属。
	Actions []battleengine.Action `json:"actions"`
}

// TurnRecord 是在更新 Runtime 内存状态前必须成功提交的语言无关回合权威事实。
//
// 它保存完整命令、实际事件、随机轨迹和状态摘要，供参与者历史、离线重放、审计和异步分析使用；
// 它不作为服务重启后的 Runtime 恢复机制，重启中的活跃 Battle 由生命周期规则进入 interrupted。
type TurnRecord struct {
	// SchemaVersion 是 Turn Record JSON 结构版本，首版固定为 1。
	SchemaVersion uint32 `json:"schemaVersion"`
	// BattleID 是本条记录所属权威 Battle 的稳定 Identifier。
	BattleID snowflake.ID `json:"battleId"`
	// StateVersion 是提交本记录后产生的连续权威状态版本，从 1 开始递增。
	StateVersion int64 `json:"stateVersion"`
	// TurnNumber 是纯引擎完成结算后的连续回合号。
	TurnNumber uint32 `json:"turnNumber"`
	// Command 是双方选择组合后的完整、可重放纯引擎命令。
	Command battleengine.TurnCommand `json:"command"`
	// Events 保留按真实发生顺序编码的版本化事件 JSON。
	Events []json.RawMessage `json:"events"`
	// RandomTrace 是本回合按序实际消耗的确定性随机轨迹。
	RandomTrace []battleengine.RandomTraceEntry `json:"randomTrace"`
	// NextRandomSource 是本回合提交后下一回合必须继续使用的确定性随机游标。
	NextRandomSource battleengine.RandomSourceSnapshot `json:"nextRandomSource"`
	// State 是本回合提交后产生的可审计状态摘要。
	State battleengine.StateSummary `json:"state"`
	// Submissions 保存两名真人 Participant 的幂等键与请求摘要，并关联本条权威 Turn Record。
	Submissions []TurnSubmissionRecord `json:"submissions"`
	// CreatedAt 是候选结果被持久化协调器接受提交的 UTC 时间。
	CreatedAt time.Time `json:"createdAt"`
}

// TurnSubmissionRecord 是一名真人 Participant 锁定选择与最终 Turn Record 的持久化幂等关联。
type TurnSubmissionRecord struct {
	// Side 是提交选择的固定 Battle 参赛方位置。
	Side ParticipantSide `json:"side"`
	// PlayerCharacterID 是真人提交者的稳定 PlayerCharacter Identifier；Bot 提交时为零 Identifier。
	PlayerCharacterID snowflake.ID `json:"playerCharacterId,omitempty"`
	// IsBot 标识本条提交是否由服务端冻结 Bot 策略生成。
	IsBot bool `json:"isBot"`
	// BotCode 是 Bot 策略稳定代码；真人提交时为空。
	BotCode string `json:"botCode,omitempty"`
	// BotStrategyVersion 是 Bot 策略冻结版本；真人提交时为 0。
	BotStrategyVersion uint32 `json:"botStrategyVersion,omitempty"`
	// IdempotencyKey 是本场 Battle 内由客户端提供的可重试幂等键。
	IdempotencyKey string `json:"idempotencyKey"`
	// RequestDigest 是提交身份、预期状态版本和行动 JSON 的 SHA-256 十六进制摘要。
	RequestDigest string `json:"requestDigest"`
}

// TurnCommitter 定义 Runtime 与 PostgreSQL 事务之间的最小写入边界。
//
// 实现必须在一个事务内写入命令幂等结果、Turn Record、状态摘要、Disclosure Ledger 增量和
// battle.state_version 比较更新；返回 nil 才表示 Runtime 可以发布候选 State。
type TurnCommitter interface {
	CommitTurn(ctx context.Context, record TurnRecord) error
}

// TurnTimeoutCompleter 持久化一场 running Battle 的回合超时终局。
//
// 实现必须和普通 Battle 完成路径一样，在同一事务内写入终局、释放真人账号占用、递减活动对局计数、
// 写入历史摘要和 Outbox。Runtime 只确定超时胜负，不直接操作数据库或全局维护状态。
type TurnTimeoutCompleter interface {
	// Complete 将仍活跃的 Battle 以稳定回合超时结果推进为 completed。
	Complete(context.Context, snowflake.ID, Result, time.Time) (Battle, error)
}

// BotStrategy 为已冻结代码与版本的 Bot 提供纯、可测试的当回合选择能力。
//
// 策略不得访问数据库、网络、系统时钟或未披露的对方意图；它只能根据当前权威纯引擎状态为己方
// Side 生成完整行动。策略升级必须使用新 Version，已创建 Training Battle 永远调用原版本。
type BotStrategy interface {
	// Code 返回策略稳定代码。
	Code() string
	// Version 返回策略不可变版本。
	Version() uint32
	// Plan 为当前权威状态中指定 Bot Side 生成完整行动。
	Plan(state battleengine.State, side battleengine.Side) ([]battleengine.Action, error)
}

// TurnSubmissionResult 是单次秘密提交的最小可公开结果。
type TurnSubmissionResult struct {
	// Locked 表示本方选择已锁定，但尚未收到对方完整选择，因此 Events 与 State 均为空。
	Locked bool `json:"locked"`
	// Resolved 表示双方选择已齐全、候选结果已成功持久化并推进了权威状态。
	Resolved bool `json:"resolved"`
	// StateVersion 是响应对应的已提交权威战斗状态版本。
	StateVersion int64 `json:"stateVersion"`
	// Events 只在 Resolved 时包含本回合版本化事件；Disclosure Ledger 接入后由视图层按接收者过滤。
	Events []json.RawMessage `json:"events,omitempty"`
	// State 只在 Resolved 时包含本回合结算后的状态摘要。
	State battleengine.StateSummary `json:"state"`
}

// Runtime 为一场 running Battle 串行处理秘密回合选择与纯 Battle Engine 结算。
//
// Runtime 不启动 goroutine；Registry 与 RPC 适配器通过同步 Submit 串行化同一 Battle。
// 每一次可能影响游戏结果的写入都遵循“先事务提交、后替换内存状态”，从而在失败时保留可重试候选。
type Runtime struct {
	// mutex 保护 Runtime 内的所有可变状态；不同 Battle 使用不同 Runtime，因此不会全局串行化。
	mutex sync.Mutex
	// session 保存参与者身份、开始时间和 Battle 标识等不可变会话快照。
	session Battle
	// state 是最近一次成功提交的纯引擎权威状态。
	state battleengine.State
	// random 是下一回合开始时的确定性随机源。
	random battleengine.RandomSource
	// stateVersion 与数据库 battle.state_version 一一对应。
	stateVersion int64
	// turnDeadlineAt 是当前回合必须锁定双方选择的权威 UTC 截止时间；零值表示该离线 Runtime 未启用回合超时。
	turnDeadlineAt time.Time
	// pending 保存当前回合双方尚未组合或尚未提交成功的秘密选择。
	pending map[ParticipantSide]pendingTurnSubmission
	// botStrategies 保存按固定 Side 解析后的冻结 Bot 策略，不按运行时最新配置查找。
	botStrategies map[ParticipantSide]BotStrategy
	// completed 保存已提交结果的幂等键与请求摘要，以安全重放响应并拒绝复用键篡改。
	completed map[string]completedTurnSubmission
	// committer 负责把候选结果作为单个数据库事务提交。
	committer TurnCommitter
	// timeoutCompleter 把 Runtime 判定的回合超时以完整 Battle 终局事务持久化；nil 仅用于离线回放工具。
	timeoutCompleter TurnTimeoutCompleter
	// now 提供唯一的 Runtime 逻辑时钟来源，纯引擎永远只接收已编码到命令中的时间。
	now func() time.Time
}

type pendingTurnSubmission struct {
	playerCharacterID  snowflake.ID
	isBot              bool
	botCode            string
	botStrategyVersion uint32
	idempotencyKey     string
	digest             [sha256.Size]byte
	actions            []battleengine.Action
}

type completedTurnSubmission struct {
	digest [sha256.Size]byte
	result TurnSubmissionResult
}

// NewRuntime 以完整的持久化、超时和 Bot 依赖创建串行 Battle Runtime。
// timeoutCompleter 和 strategies 仅可在离线回放或不存在相应能力时传入 nil。
func NewRuntime(
	session Battle,
	state battleengine.State,
	random battleengine.RandomSource,
	committer TurnCommitter,
	timeoutCompleter TurnTimeoutCompleter,
	now func() time.Time,
	strategies []BotStrategy,
) (*Runtime, error) {
	return newRuntime(session, state, random, committer, timeoutCompleter, now, strategies)
}

func newRuntime(
	session Battle,
	state battleengine.State,
	random battleengine.RandomSource,
	committer TurnCommitter,
	timeoutCompleter TurnTimeoutCompleter,
	now func() time.Time,
	strategies []BotStrategy,
) (*Runtime, error) {
	if session.ID == snowflake.ID(0) || session.Status != StatusRunning || session.StartedAt.IsZero() || session.StateVersion < 0 ||
		session.BattleDeadlineAt.IsZero() || committer == nil || now == nil ||
		!validRuntimeParticipants(session.Participants) {
		return nil, ErrInvalidRuntime
	}
	botStrategies, err := resolveBotStrategies(session.Participants, strategies)
	if err != nil {
		return nil, err
	}
	actor := &Runtime{
		session:          cloneBattle(session),
		state:            state,
		random:           random,
		stateVersion:     session.StateVersion,
		pending:          make(map[ParticipantSide]pendingTurnSubmission, 2),
		botStrategies:    botStrategies,
		completed:        make(map[string]completedTurnSubmission),
		committer:        committer,
		timeoutCompleter: timeoutCompleter,
		now:              now,
	}
	if session.Format.TurnDuration > 0 {
		actor.turnDeadlineAt = session.StartedAt.Add(session.Format.TurnDuration).UTC()
	}
	return actor, nil
}

// Battle 返回 Runtime 持有的 Battle 不可变快照副本。
func (actor *Runtime) Battle() Battle {
	actor.mutex.Lock()
	defer actor.mutex.Unlock()
	return cloneBattle(actor.session)
}

// StateVersion 返回最近一次成功提交 Turn Record 后的连续权威状态版本。
func (actor *Runtime) StateVersion() int64 {
	actor.mutex.Lock()
	defer actor.mutex.Unlock()
	return actor.stateVersion
}

// Summary 返回最近一次成功提交状态的只读可审计摘要。
func (actor *Runtime) Summary() battleengine.StateSummary {
	actor.mutex.Lock()
	defer actor.mutex.Unlock()
	return cloneStateSummary(actor.state.Summary())
}

// Submit 锁定本方完整回合选择；第二方到达后结算、提交候选结果并返回公开回合结果。
func (actor *Runtime) Submit(ctx context.Context, submission TurnSubmission) (TurnSubmissionResult, error) {
	actor.mutex.Lock()
	defer actor.mutex.Unlock()
	if !actor.turnDeadlineAt.IsZero() && !actor.now().UTC().Before(actor.turnDeadlineAt) {
		return TurnSubmissionResult{}, ErrBattleDeadlineExpired
	}

	if strings.TrimSpace(submission.IdempotencyKey) == "" {
		return TurnSubmissionResult{}, ErrInvalidRuntime
	}
	digest, err := turnSubmissionDigest(submission)
	if err != nil {
		return TurnSubmissionResult{}, err
	}
	if completed, found := actor.completed[submission.IdempotencyKey]; found {
		if completed.digest != digest {
			return TurnSubmissionResult{}, ErrRuntimeIdempotencyConflict
		}
		return cloneTurnSubmissionResult(completed.result), nil
	}
	side, err := actor.validateSubmission(submission)
	if err != nil {
		return TurnSubmissionResult{}, err
	}
	if pending, found := actor.pending[side]; found {
		if pending.idempotencyKey != submission.IdempotencyKey || pending.digest != digest {
			return TurnSubmissionResult{}, ErrRuntimeIdempotencyConflict
		}
		if len(actor.pending) < 2 {
			return TurnSubmissionResult{Locked: true, StateVersion: actor.stateVersion}, nil
		}
		return actor.resolvePending(ctx)
	}
	actor.pending[side] = pendingTurnSubmission{
		playerCharacterID: submission.PlayerCharacterID,
		idempotencyKey:    submission.IdempotencyKey,
		digest:            digest,
		actions:           cloneActions(submission.Actions),
	}
	if err := actor.lockBotSubmissions(); err != nil {
		delete(actor.pending, side)
		return TurnSubmissionResult{}, err
	}
	if len(actor.pending) < 2 {
		return TurnSubmissionResult{Locked: true, StateVersion: actor.stateVersion}, nil
	}
	return actor.resolvePending(ctx)
}

func (actor *Runtime) validateSubmission(submission TurnSubmission) (ParticipantSide, error) {
	if submission.PlayerCharacterID == snowflake.ID(0) || strings.TrimSpace(submission.IdempotencyKey) == "" ||
		submission.ExpectedStateVersion < 0 || len(submission.Actions) == 0 {
		return 0, ErrInvalidRuntime
	}
	side, found := actor.participantSide(submission.PlayerCharacterID)
	if !found {
		return 0, ErrRuntimeParticipantMismatch
	}
	if submission.ExpectedStateVersion != actor.stateVersion {
		return 0, ErrRuntimeStateVersionConflict
	}
	for _, action := range submission.Actions {
		if actorSide := action.Actor.Side; actorSide != battleSide(side) {
			return 0, ErrRuntimeParticipantMismatch
		}
	}
	return side, nil
}

func (actor *Runtime) resolvePending(ctx context.Context) (TurnSubmissionResult, error) {
	command, err := actor.completeTurnCommand()
	if err != nil {
		return TurnSubmissionResult{}, err
	}
	result, err := battleengine.ResolveTurn(actor.state, command, actor.random)
	if err != nil {
		// 尚未产生可提交事实时不得永久锁死任一玩家；双方可以更正后重新提交完整选择。
		actor.pending = make(map[ParticipantSide]pendingTurnSubmission, 2)
		return TurnSubmissionResult{}, err
	}
	record, err := actor.newTurnRecord(command, result)
	if err != nil {
		return TurnSubmissionResult{}, err
	}
	if err := actor.committer.CommitTurn(ctx, record); err != nil {
		// 保留完整 pending 提交和旧内存 State，使同键重试能重新计算完全相同的候选结果。
		return TurnSubmissionResult{}, fmt.Errorf("提交 Battle 回合记录: %w", err)
	}

	actor.state = result.State
	actor.random = result.RandomSource
	actor.stateVersion++
	if actor.session.Format.TurnDuration > 0 {
		actor.turnDeadlineAt = record.CreatedAt.Add(actor.session.Format.TurnDuration).UTC()
	}
	resolved := TurnSubmissionResult{
		Resolved:     true,
		StateVersion: actor.stateVersion,
		Events:       cloneJSONMessages(record.Events),
		State:        cloneStateSummary(record.State),
	}
	for _, pending := range actor.pending {
		actor.completed[pending.idempotencyKey] = completedTurnSubmission{
			digest: pending.digest,
			result: cloneTurnSubmissionResult(resolved),
		}
	}
	actor.pending = make(map[ParticipantSide]pendingTurnSubmission, 2)
	return resolved, nil
}

// ExpireTurn 在当前回合截止时按已经锁定的秘密选择裁定胜负，并持久化完整 Battle 终局。
//
// 仅锁定一方时该方获胜；双方都未锁定时为 No Contest。双方已锁定时会由正常 Submit 路径原子解析，
// 因而本方法不与已完成的回合竞争。整场截止时间优先交给 Battle 生命周期 Worker 的生命值裁定。
func (actor *Runtime) ExpireTurn(ctx context.Context, observedAt time.Time) (bool, error) {
	if actor == nil || actor.timeoutCompleter == nil {
		return false, ErrInvalidRuntime
	}
	actor.mutex.Lock()
	defer actor.mutex.Unlock()
	observedAt = observedAt.UTC()
	if actor.turnDeadlineAt.IsZero() || observedAt.Before(actor.turnDeadlineAt) || !observedAt.Before(actor.session.BattleDeadlineAt) {
		return false, nil
	}
	if len(actor.pending) >= 2 {
		return false, nil
	}
	result := Result{Reason: TerminalReasonNoContest}
	for side := range actor.pending {
		result = Result{WinnerSide: side, Reason: TerminalReasonTurnTimeout}
	}
	if _, err := actor.timeoutCompleter.Complete(ctx, actor.session.ID, result, observedAt); err != nil {
		return false, fmt.Errorf("持久化 Battle 回合超时: %w", err)
	}
	return true, nil
}

func (actor *Runtime) completeTurnCommand() (battleengine.TurnCommand, error) {
	now := actor.now().UTC()
	if now.Before(actor.session.StartedAt) {
		return battleengine.TurnCommand{}, ErrRuntimeClockInvalid
	}
	elapsedMilliseconds := now.Sub(actor.session.StartedAt).Milliseconds()
	if elapsedMilliseconds > int64(^uint32(0)>>1) {
		return battleengine.TurnCommand{}, ErrRuntimeClockInvalid
	}
	actions := make([]battleengine.Action, 0)
	for _, side := range []ParticipantSide{ParticipantSideOne, ParticipantSideTwo} {
		pending, found := actor.pending[side]
		if !found {
			return battleengine.TurnCommand{}, ErrInvalidRuntime
		}
		actions = append(actions, cloneActions(pending.actions)...)
	}
	sort.SliceStable(actions, func(left, right int) bool {
		if actions[left].Actor.Side != actions[right].Actor.Side {
			return actions[left].Actor.Side < actions[right].Actor.Side
		}
		return actions[left].Actor.Position < actions[right].Actor.Position
	})
	return battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    actor.state.TurnNumber() + 1,
		Time:          battleengine.TimeInput{ElapsedMilliseconds: int32(elapsedMilliseconds)},
		Actions:       actions,
	}, nil
}

func (actor *Runtime) newTurnRecord(command battleengine.TurnCommand, result battleengine.TurnResult) (TurnRecord, error) {
	nextRandomSource, err := result.RandomSource.Snapshot()
	if err != nil {
		return TurnRecord{}, fmt.Errorf("快照 Battle 随机源: %w", err)
	}
	events := make([]json.RawMessage, len(result.Events))
	for index, event := range result.Events {
		encoded, err := json.Marshal(event)
		if err != nil {
			return TurnRecord{}, fmt.Errorf("编码 Battle 回合事件: %w", err)
		}
		events[index] = encoded
	}
	return TurnRecord{
		SchemaVersion:    1,
		BattleID:         actor.session.ID,
		StateVersion:     actor.stateVersion + 1,
		TurnNumber:       result.State.TurnNumber(),
		Command:          command,
		Events:           events,
		RandomTrace:      append([]battleengine.RandomTraceEntry(nil), result.RandomTrace...),
		NextRandomSource: nextRandomSource,
		State:            result.State.Summary(),
		Submissions:      actor.pendingSubmissionRecords(),
		CreatedAt:        actor.now().UTC(),
	}, nil
}

func (actor *Runtime) pendingSubmissionRecords() []TurnSubmissionRecord {
	records := make([]TurnSubmissionRecord, 0, len(actor.pending))
	for _, side := range []ParticipantSide{ParticipantSideOne, ParticipantSideTwo} {
		pending := actor.pending[side]
		records = append(records, TurnSubmissionRecord{
			Side: side, PlayerCharacterID: pending.playerCharacterID, IsBot: pending.isBot,
			BotCode: pending.botCode, BotStrategyVersion: pending.botStrategyVersion,
			IdempotencyKey: pending.idempotencyKey, RequestDigest: hex.EncodeToString(pending.digest[:]),
		})
	}
	return records
}

func (actor *Runtime) participantSide(playerCharacterID snowflake.ID) (ParticipantSide, bool) {
	for _, participant := range actor.session.Participants {
		if !participant.IsBot && participant.PlayerCharacterID == playerCharacterID {
			return participant.Side, true
		}
	}
	return 0, false
}

func validRuntimeParticipants(participants []Participant) bool {
	if len(participants) != 2 {
		return false
	}
	seen := make(map[ParticipantSide]struct{}, 2)
	for _, participant := range participants {
		if participant.Side != ParticipantSideOne && participant.Side != ParticipantSideTwo {
			return false
		}
		if participant.IsBot {
			if strings.TrimSpace(participant.BotCode) == "" || participant.BotStrategyVersion == 0 ||
				participant.PlayerCharacterID != snowflake.ID(0) || participant.AccountID != snowflake.ID(0) {
				return false
			}
		} else if participant.PlayerCharacterID == snowflake.ID(0) {
			return false
		}
		if _, duplicate := seen[participant.Side]; duplicate {
			return false
		}
		seen[participant.Side] = struct{}{}
	}
	return len(seen) == 2
}

func resolveBotStrategies(participants []Participant, strategies []BotStrategy) (map[ParticipantSide]BotStrategy, error) {
	available := make(map[string]BotStrategy, len(strategies))
	for _, strategy := range strategies {
		if strategy == nil || strings.TrimSpace(strategy.Code()) == "" || strategy.Version() == 0 {
			return nil, ErrBotStrategyUnavailable
		}
		available[botStrategyKey(strategy.Code(), strategy.Version())] = strategy
	}
	resolved := make(map[ParticipantSide]BotStrategy)
	for _, participant := range participants {
		if !participant.IsBot {
			continue
		}
		strategy, found := available[botStrategyKey(participant.BotCode, participant.BotStrategyVersion)]
		if !found {
			return nil, ErrBotStrategyUnavailable
		}
		resolved[participant.Side] = strategy
	}
	return resolved, nil
}

func botStrategyKey(code string, version uint32) string {
	return fmt.Sprintf("%s:%d", code, version)
}

func (actor *Runtime) lockBotSubmissions() error {
	for side, strategy := range actor.botStrategies {
		if _, alreadyLocked := actor.pending[side]; alreadyLocked {
			continue
		}
		actions, err := strategy.Plan(actor.state, battleSide(side))
		if err != nil || len(actions) == 0 {
			if err != nil {
				return fmt.Errorf("生成 Battle Bot 选择: %w", err)
			}
			return ErrBotStrategyUnavailable
		}
		for _, action := range actions {
			if action.Actor.Side != battleSide(side) {
				return ErrBotStrategyUnavailable
			}
		}
		participant := actor.participantBySide(side)
		key := fmt.Sprintf("bot:%s:%d:%d", participant.BotCode, participant.BotStrategyVersion, actor.stateVersion+1)
		digest, err := botSubmissionDigest(side, participant.BotCode, participant.BotStrategyVersion, actor.stateVersion, actions)
		if err != nil {
			return err
		}
		actor.pending[side] = pendingTurnSubmission{
			isBot: true, botCode: participant.BotCode, botStrategyVersion: participant.BotStrategyVersion,
			idempotencyKey: key, digest: digest, actions: cloneActions(actions),
		}
	}
	return nil
}

func (actor *Runtime) participantBySide(side ParticipantSide) Participant {
	for _, participant := range actor.session.Participants {
		if participant.Side == side {
			return cloneParticipant(participant)
		}
	}
	return Participant{}
}

func botSubmissionDigest(
	side ParticipantSide,
	code string,
	version uint32,
	stateVersion int64,
	actions []battleengine.Action,
) ([sha256.Size]byte, error) {
	encoded, err := json.Marshal(struct {
		Side         ParticipantSide       `json:"side"`
		Code         string                `json:"code"`
		Version      uint32                `json:"version"`
		StateVersion int64                 `json:"stateVersion"`
		Actions      []battleengine.Action `json:"actions"`
	}{side, code, version, stateVersion, actions})
	if err != nil {
		return [sha256.Size]byte{}, fmt.Errorf("编码 Battle Bot 幂等摘要: %w", err)
	}
	return sha256.Sum256(encoded), nil
}

func battleSide(side ParticipantSide) battleengine.Side {
	if side == ParticipantSideOne {
		return battleengine.SideOne
	}
	return battleengine.SideTwo
}

func turnSubmissionDigest(submission TurnSubmission) ([sha256.Size]byte, error) {
	encoded, err := json.Marshal(struct {
		PlayerCharacterID    snowflake.ID          `json:"playerCharacterId"`
		ExpectedStateVersion int64                 `json:"expectedStateVersion"`
		Actions              []battleengine.Action `json:"actions"`
	}{submission.PlayerCharacterID, submission.ExpectedStateVersion, submission.Actions})
	if err != nil {
		return [sha256.Size]byte{}, fmt.Errorf("编码 Battle 回合幂等摘要: %w", err)
	}
	return sha256.Sum256(encoded), nil
}

func cloneActions(source []battleengine.Action) []battleengine.Action {
	cloned := make([]battleengine.Action, len(source))
	for index, action := range source {
		cloned[index] = action
		if action.UseSkill != nil {
			useSkill := *action.UseSkill
			cloned[index].UseSkill = &useSkill
		}
		if action.Switch != nil {
			switchAction := *action.Switch
			cloned[index].Switch = &switchAction
		}
	}
	return cloned
}

func cloneTurnSubmissionResult(source TurnSubmissionResult) TurnSubmissionResult {
	return TurnSubmissionResult{
		Locked: source.Locked, Resolved: source.Resolved, StateVersion: source.StateVersion,
		Events: cloneJSONMessages(source.Events), State: cloneStateSummary(source.State),
	}
}

func cloneJSONMessages(source []json.RawMessage) []json.RawMessage {
	if source == nil {
		return nil
	}
	cloned := make([]json.RawMessage, len(source))
	for index, item := range source {
		cloned[index] = append(json.RawMessage(nil), item...)
	}
	return cloned
}

func cloneStateSummary(source battleengine.StateSummary) battleengine.StateSummary {
	cloned := source
	if source.Result != nil {
		result := *source.Result
		cloned.Result = &result
	}
	cloned.Members = make([]battleengine.MemberStateSummary, len(source.Members))
	for index, member := range source.Members {
		cloned.Members[index] = member
		cloned.Members[index].ElementIDs = append([]battleengine.Identifier(nil), member.ElementIDs...)
		cloned.Members[index].RemainingPP = append([]uint8(nil), member.RemainingPP...)
		if member.StatStages != nil {
			cloned.Members[index].StatStages = make(map[battleengine.Stat]int8, len(member.StatStages))
			for stat, stage := range member.StatStages {
				cloned.Members[index].StatStages[stat] = stage
			}
		}
	}
	return cloned
}
