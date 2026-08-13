package battle

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"sort"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/team"
)

var (
	// ErrInvalidBattle 表示创建 Battle、冻结参赛方或提交 Preview 的基础事实不满足领域不变量。
	ErrInvalidBattle = errors.New("对战无效")
	// ErrBattleNotPreview 表示 Preview 提交只允许发生在仍等待双方选择的 Battle 中。
	ErrBattleNotPreview = errors.New("对战未处于 Preview 阶段")
	// ErrPreviewExpired 表示调用方在冻结的 Preview 截止时间到达后仍尝试提交选择。
	ErrPreviewExpired = errors.New("对战 Preview 已超时")
	// ErrPreviewAlreadySubmitted 表示同一参赛方已经锁定本场 Battle 的 Preview 选择。
	ErrPreviewAlreadySubmitted = errors.New("对战 Preview 已经提交")
	// ErrBattleNotPendingRuntime 表示 Battle 未处于 Preview 已齐备但 Runtime 尚未启动的状态。
	ErrBattleNotPendingRuntime = errors.New("对战未等待 Runtime 启动")
	// ErrBattleNotRunning 表示结算正常结果只允许发生在 Runtime 已经建立的 running Battle 中。
	ErrBattleNotRunning = errors.New("对战未处于 Running 阶段")
	// ErrBattleTerminal 表示调用方试图再次改变已经 completed、canceled 或 interrupted 的 Battle。
	ErrBattleTerminal = errors.New("对战已经结束")
	// ErrBattleDeadlineExpired 表示启动或结算入口发现整场 Battle 的冻结截止时间已经到达。
	ErrBattleDeadlineExpired = errors.New("对战已经超时")
	// ErrInvalidBattleResult 表示终局原因、胜方或完成时间不能构成可持久化的权威结果。
	ErrInvalidBattleResult = errors.New("对战结果无效")
)

// BattleMode 是 Battle 的参与关系类型。
type BattleMode string

const (
	// BattleModePvP 表示两个 PlayerCharacter 之间的 Battle。
	BattleModePvP BattleMode = "pvp"
	// BattleModePvE 表示 PlayerCharacter 与服务端控制对手之间的 Battle。
	BattleModePvE BattleMode = "pve"
)

// BattleSourceType 是创建 Battle 的稳定来源类型。
type BattleSourceType string

const (
	// BattleSourceChallenge 表示接受 Challenge 后创建的 PvP Battle。
	BattleSourceChallenge BattleSourceType = "challenge"
	// BattleSourceTraining 表示玩家主动创建的训练 PvE Battle。
	BattleSourceTraining BattleSourceType = "training"
	// BattleSourceEncounter 表示接受 Pending Encounter 后创建的 PvE Battle。
	BattleSourceEncounter BattleSourceType = "encounter"
)

// Status 是权威 Battle 生命周期的稳定状态。
type Status string

const (
	// StatusCreated 表示 Battle 已持久化但尚未准备好进入 Preview 或 Runtime。
	StatusCreated Status = "created"
	// StatusPreview 表示双方仍在秘密选择参战成员和初始上场顺序。
	StatusPreview Status = "preview"
	// StatusRunning 表示 Preview 已齐备，Battle 正等待 Runtime 承接或已在 Runtime 中执行。
	StatusRunning Status = "running"
	// StatusCompleted 表示 Battle 已按正常规则产生确定性结果。
	StatusCompleted Status = "completed"
	// StatusCanceled 表示尚未启动 Runtime 的 Battle 被明确取消且不产生胜负。
	StatusCanceled Status = "canceled"
	// StatusInterrupted 表示运行时、租约或启动失败导致 Battle 不产生胜负地中断。
	StatusInterrupted Status = "interrupted"
)

// TerminalReason 是终态 Battle 写入历史、分析和客户端的稳定终局原因。
//
// 代码只保存稳定原因，不把底层异常文本、数据库错误或 Runtime 栈暴露给对手和历史查询。
type TerminalReason string

const (
	// TerminalReasonBattleEnded 表示纯 Battle Engine 已按正常战斗规则决出结果。
	TerminalReasonBattleEnded TerminalReason = "battle_ended"
	// TerminalReasonSurrender 表示一方明确认输并由事务先写入了胜者。
	TerminalReasonSurrender TerminalReason = "surrender"
	// TerminalReasonTurnTimeout 表示回合超时裁定已经产生稳定结果。
	TerminalReasonTurnTimeout TerminalReason = "turn_timeout"
	// TerminalReasonBattleTimeout 表示整场冻结截止时间到达后的稳定裁定结果。
	TerminalReasonBattleTimeout TerminalReason = "battle_timeout"
	// TerminalReasonDraw 表示规则或裁定确定本场没有胜方。
	TerminalReasonDraw TerminalReason = "draw"
	// TerminalReasonNoContest 表示对局已开始但按规则不产生胜负的正常终局。
	TerminalReasonNoContest TerminalReason = "no_contest"
	// TerminalReasonCanceled 表示 Battle 在 Runtime 启动前被明确取消。
	TerminalReasonCanceled TerminalReason = "canceled"
	// TerminalReasonStartupFailed 表示等待承载阶段无法建立有效 Battle Engine 或 Runtime。
	TerminalReasonStartupFailed TerminalReason = "startup_failed"
	// TerminalReasonRuntimePanic 表示单个 Runtime 的意外 panic 已被隔离并中断本场 Battle。
	TerminalReasonRuntimePanic TerminalReason = "runtime_panic"
	// TerminalReasonLeaseLost 表示承载本场对局的单实例租约已经丢失。
	TerminalReasonLeaseLost TerminalReason = "lease_lost"
	// TerminalReasonRuntimeFailed 表示不可恢复的运行时或持久化协调失败中断了本场 Battle。
	TerminalReasonRuntimeFailed TerminalReason = "runtime_failed"
	// TerminalReasonRecoveryExhausted 表示五次有界恢复尝试全部失败。
	TerminalReasonRecoveryExhausted TerminalReason = "recovery_exhausted"
)

// Valid 报告终局原因是否是当前契约允许持久化的稳定值。
func (reason TerminalReason) Valid() bool {
	return reason == TerminalReasonBattleEnded || reason == TerminalReasonSurrender ||
		reason == TerminalReasonTurnTimeout || reason == TerminalReasonBattleTimeout ||
		reason == TerminalReasonDraw || reason == TerminalReasonNoContest ||
		reason == TerminalReasonStartupFailed ||
		reason == TerminalReasonRuntimePanic || reason == TerminalReasonLeaseLost ||
		reason == TerminalReasonRuntimeFailed || reason == TerminalReasonRecoveryExhausted
}

// Result 是正常 completed Battle 的最小权威终局事实。
type Result struct {
	// WinnerSide 是获胜参赛方；平局和 No Contest 明确使用 0。
	WinnerSide ParticipantSide `json:"winnerSide,omitempty"`
	// Reason 是产生本结果的稳定终局原因，不允许使用 interrupted 专用原因。
	Reason TerminalReason `json:"reason"`
}

// ParticipantSide 是 Participant 在一场 Battle 中不可变的阵营位置。
type ParticipantSide uint8

const (
	// ParticipantSideOne 是 Battle 创建时固定的第一方。
	ParticipantSideOne ParticipantSide = 1
	// ParticipantSideTwo 是 Battle 创建时固定的第二方。
	ParticipantSideTwo ParticipantSide = 2
)

// Format 是 Battle 创建时从 BattleFormat 冻结的执行边界。
//
// 它只保存 Battle Preview 与期限需要读取的值；完整规则、Clause、Restriction 和
// Mechanic 仍保留在 BattleFormatSnapshot 中并由后续引擎编译边界解释。
type Format struct {
	// RosterCount 是每方允许带入本场 Battle 的最多成员数，取值为 1 至 6。
	RosterCount uint8 `json:"rosterCount"`
	// SelectCount 是 Preview 中必须秘密选择的参战成员数量。
	SelectCount uint8 `json:"selectCount"`
	// ActiveParticipantsPerSide 是双方开始战斗时必须同时上场的成员数量。
	ActiveParticipantsPerSide uint8 `json:"activeParticipantsPerSide"`
	// PreviewDuration 是从 Battle 创建起到双方锁定选择的固定时长。
	PreviewDuration time.Duration `json:"previewDuration"`
	// TurnDuration 是每个完整回合从上一回合结算完成起允许锁定选择的固定时长。
	TurnDuration time.Duration `json:"turnDuration"`
	// BattleDuration 是从 Battle 创建起到整场超时裁决的固定时长。
	BattleDuration time.Duration `json:"battleDuration"`
}

// Participant 是一方已经冻结的 PlayerCharacter 或 Bot 参赛事实。
type Participant struct {
	// Side 是该参赛方在本场 Battle 内永久不变的阵营位置。
	Side ParticipantSide `json:"side"`
	// AccountID 是真人参赛方所属玩家账号；Bot 时为零 Identifier。
	AccountID snowflake.ID `json:"accountId,omitempty"`
	// PlayerCharacterID 是真人参赛角色；Bot 时为零 Identifier。
	PlayerCharacterID snowflake.ID `json:"playerCharacterId,omitempty"`
	// DisplayName 是 Battle 创建时冻结的公开展示名称。
	DisplayName string `json:"displayName"`
	// Team 是独立于可变 Team 记录的完整阵容快照。
	Team TeamSnapshot `json:"team"`
	// IsBot 标识该 Participant 是否由服务端 Bot 策略控制。
	IsBot bool `json:"isBot"`
	// BotCode 是冻结的 Bot 稳定代码；真人 Participant 时为空字符串。
	BotCode string `json:"botCode,omitempty"`
	// BotStrategyVersion 是冻结的 Bot 策略版本；真人 Participant 时为 0。
	BotStrategyVersion uint32 `json:"botStrategyVersion,omitempty"`
	// BotDefinition 是创建 Training Battle 时规范化并冻结的完整 Bot 配置；真人 Participant 时为空。
	//
	// 它不能通过玩家 RPC 视图暴露给对手，但启动、离线重放和受控审计可以据此重建本场策略，而无需
	// 读取后来可能已修改的 battle_bot_strategy 资料。
	BotDefinition json.RawMessage `json:"-"`
}

// PreviewSubmission 是一方已经锁定、不可再修改的 Team Preview 选择。
type PreviewSubmission struct {
	// Side 是提交选择的稳定阵营位置。
	Side ParticipantSide `json:"side"`
	// MemberPositions 是按玩家确认顺序选择的参战成员位置。
	MemberPositions []int32 `json:"memberPositions"`
	// ActivePositions 是按场上槽位顺序选择的初始上场成员位置。
	ActivePositions []int32 `json:"activePositions"`
	// SubmittedAt 是服务器确认并锁定选择的 UTC 时间。
	SubmittedAt time.Time `json:"submittedAt"`
	// RandomTrace 是服务端到期自动补选时使用的可重放随机轨迹；真人和固定 Bot 提交使用空数组。
	RandomTrace json.RawMessage `json:"randomTrace"`
}

// Battle 是一场持久化权威对战的领域生命周期和冻结事实。
//
// Battle 不保存 Runtime、实时连接或数据库连接。应用层需要在同一事务内保存 Battle、
// Participant、PreviewSubmission 与账号占用，并在成功提交后才创建内存 Runtime。
type Battle struct {
	// ID 是 Battle 的 Snowflake Identifier 稳定标识。
	ID snowflake.ID `json:"id"`
	// Mode 是 pvp 或 pve 参与关系类型。
	Mode BattleMode `json:"mode"`
	// SourceType 是 challenge、training 或 encounter 创建来源。
	SourceType BattleSourceType `json:"sourceType"`
	// ChallengeID 是创建本场 Battle 的已接受 Challenge；练习战时为零 Identifier。
	ChallengeID snowflake.ID `json:"challengeId,omitempty"`
	// Status 是当前权威生命周期状态。
	Status Status `json:"status"`
	// BattleFormatID 是创建 Battle 使用的实时 BattleFormat 稳定 Identifier。
	BattleFormatID snowflake.ID `json:"battleFormatId"`
	// BattleFormatSnapshot 是赛制、规则与效果参数的规范 JSON 冻结值。
	BattleFormatSnapshot json.RawMessage `json:"battleFormatSnapshot"`
	// Format 是 Preview 和期限需要使用的最小执行赛制快照。
	Format Format `json:"format"`
	// PreviewDeadlineAt 是双方必须完成 Preview 提交的 UTC 截止时间。
	PreviewDeadlineAt time.Time `json:"previewDeadlineAt"`
	// BattleDeadlineAt 是整场 Battle 的 UTC 超时裁决时间。
	BattleDeadlineAt time.Time `json:"battleDeadlineAt"`
	// Participants 按 SideOne、SideTwo 稳定排序保存两名参赛方。
	Participants []Participant `json:"participants"`
	// PreviewSubmissions 保存已经锁定的秘密选择；双方齐备时状态转为 running 并等待 Runtime。
	PreviewSubmissions []PreviewSubmission `json:"previewSubmissions"`
	// Result 保存 completed Battle 的结构化终局事实；尚未结束时为 nil。
	Result json.RawMessage `json:"result,omitempty"`
	// TerminalReason 是 completed、canceled 或 interrupted 状态的稳定原因代码；未终局时为空。
	TerminalReason string `json:"terminalReason,omitempty"`
	// StateVersion 是已提交 Turn Record 的连续权威状态版本；Runtime 启动前固定为 0。
	// 它独立于 Battle 生命周期的 Version，使回合命令可以只检测战斗状态，而不受展示字段更新影响。
	StateVersion int64 `json:"stateVersion"`
	// Version 是持久化生命周期转换使用的乐观版本，从 1 开始递增。
	Version int64 `json:"version"`
	// CreatedAt 是 Battle 创建时间，统一使用 UTC。
	CreatedAt time.Time `json:"createdAt"`
	// UpdatedAt 是最后一次状态或 Preview 转换时间，统一使用 UTC。
	UpdatedAt time.Time `json:"updatedAt"`
	// StartedAt 是 Battle Engine 与 Runtime 成功建立的 UTC 时间；此前为零值。
	StartedAt time.Time `json:"startedAt,omitempty"`
	// CompletedAt 是 Battle 进入 completed、canceled 或 interrupted 终态的 UTC 时间；未终局时为零值。
	CompletedAt time.Time `json:"completedAt,omitempty"`
}

// NewChallengeBattleCommand 包含已接受 Challenge 进入 Preview Battle 所需的全部冻结输入。
type NewChallengeBattleCommand struct {
	// Challenge 是刚在同一事务中接受、状态为 accepted 的邀请事实。
	Challenge Challenge
	// TargetTeam 是接收方在接受入口重新校验后必须冻结的当前 Team。
	TargetTeam team.Team
	// Format 是从 Challenge 冻结的 BattleFormat 解码出的 Preview 与期限规则。
	Format Format
}

// BotProfile 是创建 Training Battle 时需要冻结到 Participant 的 Bot 身份、策略版本和阵容事实。
type BotProfile struct {
	// Code 是 Bot 策略的稳定代码；已创建 Battle 永远不按同名最新策略重新解释。
	Code string
	// StrategyVersion 是 Code 对应的不可变策略版本，从 1 开始递增。
	StrategyVersion uint32
	// DisplayName 是本场 Battle 参与者视图中冻结的 Bot 展示名称。
	DisplayName string
	// Team 是由 Bot 模板或生成器产生并冻结的完整阵容快照。
	Team TeamSnapshot
	// Definition 是规范化后的完整 Bot 策略定义，会与 Team Snapshot 一起冻结到 Participant。
	Definition json.RawMessage
}

// NewTrainingBattleCommand 包含真人 PlayerCharacter 与固定版本 Bot 创建练习赛所需的全部冻结输入。
type NewTrainingBattleCommand struct {
	// AccountID 是发起练习赛的真人玩家账号。
	AccountID snowflake.ID
	// PlayerCharacterID 是本场练习赛中唯一的真人活动角色。
	PlayerCharacterID snowflake.ID
	// DisplayName 是创建时冻结的真人展示名称。
	DisplayName string
	// Team 是进入练习赛前已按当前实时资料校验的真人 Team。
	Team team.Team
	// BattleFormatID 是启用且允许练习赛使用的 BattleFormat 稳定 Identifier。
	BattleFormatID snowflake.ID
	// BattleFormatSnapshot 是赛制、规则和效果参数的完整冻结 JSON 快照。
	BattleFormatSnapshot json.RawMessage
	// Format 是 Preview 和期限所需的最小执行赛制快照。
	Format Format
	// Bot 是固定版本的 Bot 参与者快照。
	Bot BotProfile
}

// PreviewSubmissionCommand 是真人 Participant 锁定 Team Preview 所需的完整输入。
type PreviewSubmissionCommand struct {
	// PlayerCharacterID 是当前提交选择的活动 PlayerCharacter，必须属于本场真人 Participant。
	PlayerCharacterID snowflake.ID
	// MemberPositions 是按确认顺序选择的全部参战成员位置。
	MemberPositions []int32
	// ActivePositions 是按场上槽位顺序选择的初始上场成员位置。
	ActivePositions []int32
}

// NewChallengeBattle 把已接受 Challenge 和接收方 Team 冻结为新的 Preview Battle。
func NewChallengeBattle(
	ctx context.Context,
	command NewChallengeBattleCommand,
	newID snowflake.Source,
	now func() time.Time,
) (Battle, error) {
	if newID == nil || now == nil || !validChallengeBattle(command) {
		return Battle{}, ErrInvalidBattle
	}
	id, err := newID.Next(ctx)
	if err != nil {
		return Battle{}, err
	}
	createdAt := now().UTC()
	return Battle{
		ID:                   id,
		Mode:                 BattleModePvP,
		SourceType:           BattleSourceChallenge,
		ChallengeID:          command.Challenge.ID,
		Status:               StatusPreview,
		BattleFormatID:       command.Challenge.BattleFormatID,
		BattleFormatSnapshot: append(json.RawMessage(nil), command.Challenge.BattleFormatSnapshot...),
		Format:               command.Format,
		PreviewDeadlineAt:    createdAt.Add(command.Format.PreviewDuration),
		BattleDeadlineAt:     createdAt.Add(command.Format.BattleDuration),
		Participants: []Participant{
			{
				Side:              ParticipantSideOne,
				AccountID:         command.Challenge.ChallengerAccountID,
				PlayerCharacterID: command.Challenge.ChallengerPlayerCharacterID,
				DisplayName:       command.Challenge.ChallengerDisplayName,
				Team:              cloneTeamSnapshot(command.Challenge.ChallengerTeam),
			},
			{
				Side:              ParticipantSideTwo,
				AccountID:         command.Challenge.TargetAccountID,
				PlayerCharacterID: command.Challenge.TargetPlayerCharacterID,
				DisplayName:       command.Challenge.TargetDisplayName,
				Team:              FreezeTeam(command.TargetTeam),
			},
		},
		Version:   1,
		CreatedAt: createdAt,
		UpdatedAt: createdAt,
	}, nil
}

// NewTrainingBattle 创建一场真人对固定版本 Bot 的 Preview Battle。
//
// Bot 的 Team Preview 在创建时按稳定成员位置自动锁定，但不会通过未授权视图提前透露给真人；
// 真人提交合法选择后，双方 Preview 即齐全并进入 starting。
func NewTrainingBattle(
	ctx context.Context,
	command NewTrainingBattleCommand,
	newID snowflake.Source,
	now func() time.Time,
) (Battle, error) {
	if newID == nil || now == nil || !validTrainingBattle(command) {
		return Battle{}, ErrInvalidBattle
	}
	id, err := newID.Next(ctx)
	if err != nil {
		return Battle{}, err
	}
	createdAt := now().UTC()
	botPreview := automaticBotPreview(command.Format, command.Bot.Team, createdAt)
	return Battle{
		ID:                   id,
		Mode:                 BattleModePvE,
		SourceType:           BattleSourceTraining,
		Status:               StatusPreview,
		BattleFormatID:       command.BattleFormatID,
		BattleFormatSnapshot: append(json.RawMessage(nil), command.BattleFormatSnapshot...),
		Format:               command.Format,
		PreviewDeadlineAt:    createdAt.Add(command.Format.PreviewDuration),
		BattleDeadlineAt:     createdAt.Add(command.Format.BattleDuration),
		Participants: []Participant{
			{
				Side:              ParticipantSideOne,
				AccountID:         command.AccountID,
				PlayerCharacterID: command.PlayerCharacterID,
				DisplayName:       command.DisplayName,
				Team:              FreezeTeam(command.Team),
			},
			{
				Side:               ParticipantSideTwo,
				DisplayName:        command.Bot.DisplayName,
				Team:               cloneTeamSnapshot(command.Bot.Team),
				IsBot:              true,
				BotCode:            command.Bot.Code,
				BotStrategyVersion: command.Bot.StrategyVersion,
				BotDefinition:      append(json.RawMessage(nil), command.Bot.Definition...),
			},
		},
		PreviewSubmissions: []PreviewSubmission{botPreview},
		Version:            1,
		CreatedAt:          createdAt,
		UpdatedAt:          createdAt,
	}, nil
}

// SubmitPreview 锁定一方的完整 Team Preview 选择；第二方有效提交后 Battle 转为 starting。
func (value Battle) SubmitPreview(command PreviewSubmissionCommand, submittedAt time.Time) (Battle, error) {
	if value.Status != StatusPreview {
		return Battle{}, ErrBattleNotPreview
	}
	submittedAt = submittedAt.UTC()
	if !submittedAt.Before(value.PreviewDeadlineAt) {
		return Battle{}, ErrPreviewExpired
	}
	participant, found := value.participantByPlayerCharacter(command.PlayerCharacterID)
	if !found {
		return Battle{}, ErrInvalidBattle
	}
	if value.hasPreviewSubmission(participant.Side) {
		return Battle{}, ErrPreviewAlreadySubmitted
	}
	if err := validatePreviewSelection(value.Format, participant.Team, command); err != nil {
		return Battle{}, err
	}

	next := cloneBattle(value)
	next.PreviewSubmissions = append(next.PreviewSubmissions, PreviewSubmission{
		Side:            participant.Side,
		MemberPositions: append([]int32(nil), command.MemberPositions...),
		ActivePositions: append([]int32(nil), command.ActivePositions...),
		SubmittedAt:     submittedAt,
	})
	sort.Slice(next.PreviewSubmissions, func(left, right int) bool {
		return next.PreviewSubmissions[left].Side < next.PreviewSubmissions[right].Side
	})
	if len(next.PreviewSubmissions) == len(next.Participants) {
		next.Status = StatusRunning
	}
	next.Version++
	next.UpdatedAt = submittedAt
	return next, nil
}

// Start 在双方 Preview 已经持久化并成功建立 Battle Engine 初始状态后，把 Battle 转为 active。
//
// 应用层必须在同一个持久化事务内确保 Battle 状态比较更新成功，再把对应 Runtime 放入 Registry；
// 本方法只表达不依赖数据库和 goroutine 的领域状态转换。
func (value Battle) Start(startedAt time.Time) (Battle, error) {
	if value.Status == StatusCompleted || value.Status == StatusCanceled || value.Status == StatusInterrupted {
		return Battle{}, ErrBattleTerminal
	}
	if value.Status != StatusRunning || !value.StartedAt.IsZero() {
		return Battle{}, ErrBattleNotPendingRuntime
	}
	startedAt = startedAt.UTC()
	if !startedAt.Before(value.BattleDeadlineAt) {
		return Battle{}, ErrBattleDeadlineExpired
	}
	next := cloneBattle(value)
	next.Status = StatusRunning
	next.StartedAt = startedAt
	next.Version++
	next.UpdatedAt = startedAt
	return next, nil
}

// Complete 将 running Battle 的结构化正常终局事实固化为 completed。
//
// 认输、引擎正常结束和超时裁定均调用本方法；调用方应在提交该状态前同事务写入 Turn Record、
// Authoritative Summary、Outbox 和账号占用释放，防止历史与在线状态出现半完成结果。
func (value Battle) Complete(result Result, completedAt time.Time) (Battle, error) {
	if value.Status == StatusCompleted || value.Status == StatusCanceled || value.Status == StatusInterrupted {
		return Battle{}, ErrBattleTerminal
	}
	if value.Status != StatusRunning || value.StartedAt.IsZero() {
		return Battle{}, ErrBattleNotRunning
	}
	completedAt = completedAt.UTC()
	// 整场超时可能按存活成员与剩余生命裁定胜方，也可能在完全相同的状态下裁定 No Contest；二者都
	// 必须允许在冻结截止时间到达后完成。其它正常原因在截止后继续写入会掩盖超时，因此统一拒绝。
	if !completedAt.Before(value.BattleDeadlineAt) && result.Reason != TerminalReasonBattleTimeout &&
		result.Reason != TerminalReasonNoContest {
		return Battle{}, ErrBattleDeadlineExpired
	}
	if !validCompletionResult(result) {
		return Battle{}, ErrInvalidBattleResult
	}
	encoded, err := json.Marshal(result)
	if err != nil {
		return Battle{}, fmt.Errorf("编码 Battle 终局结果: %w", err)
	}
	next := cloneBattle(value)
	next.Status = StatusCompleted
	next.Result = encoded
	next.TerminalReason = string(result.Reason)
	next.CompletedAt = completedAt
	next.Version++
	next.UpdatedAt = completedAt
	return next, nil
}

// Cancel 将尚未启动 Runtime 的 Battle 标记为 canceled，且不产生胜负结果。
func (value Battle) Cancel(canceledAt time.Time) (Battle, error) {
	if value.Status == StatusCompleted || value.Status == StatusCanceled || value.Status == StatusInterrupted {
		return Battle{}, ErrBattleTerminal
	}
	if value.Status == StatusRunning && !value.StartedAt.IsZero() {
		return Battle{}, ErrBattleNotPendingRuntime
	}
	canceledAt = canceledAt.UTC()
	next := cloneBattle(value)
	next.Status = StatusCanceled
	next.Result = nil
	next.TerminalReason = string(TerminalReasonCanceled)
	next.CompletedAt = canceledAt
	next.Version++
	next.UpdatedAt = canceledAt
	return next, nil
}

// Interrupt 将尚未正常完成的 Battle 标记为 interrupted，且不写入胜负 Result。
//
// 该转换用于 Preview 超时、Runtime panic、租约丢失和启动失败等不可重试的运行时终止；
// 它不会伪装成普通平局或裁定，以便历史、排名和分析明确排除中断对局。
func (value Battle) Interrupt(reason TerminalReason, interruptedAt time.Time) (Battle, error) {
	if value.Status == StatusCompleted || value.Status == StatusCanceled || value.Status == StatusInterrupted {
		return Battle{}, ErrBattleTerminal
	}
	if !validInterruptReason(reason) {
		return Battle{}, ErrInvalidBattleResult
	}
	interruptedAt = interruptedAt.UTC()
	next := cloneBattle(value)
	next.Status = StatusInterrupted
	next.Result = nil
	next.TerminalReason = string(reason)
	next.CompletedAt = interruptedAt
	next.Version++
	next.UpdatedAt = interruptedAt
	return next, nil
}

// CompleteExpiredPreview 在 Preview 截止时间到达后，为每名尚未提交的 Participant 锁定可重放的自动选择。
//
// 自动选择只补齐缺失的一方，已经持久化的真人或 Bot 选择永远不会改写。双方选择齐备后 Battle
// 进入等待承载状态，后续由拥有单实例 Runtime Registry 的 Server 进程创建内存 Runtime；独立 Worker 不会
// 直接持有或创建 Runtime。
func (value Battle) CompleteExpiredPreview(observedAt time.Time) (Battle, error) {
	if value.Status == StatusCompleted || value.Status == StatusCanceled || value.Status == StatusInterrupted {
		return Battle{}, ErrBattleTerminal
	}
	if value.Status != StatusPreview {
		return Battle{}, ErrBattleNotPreview
	}
	if observedAt.UTC().Before(value.PreviewDeadlineAt) {
		return Battle{}, ErrPreviewExpired
	}
	next := cloneBattle(value)
	for _, participant := range next.Participants {
		if next.hasPreviewSubmission(participant.Side) {
			continue
		}
		selection, err := automaticExpiredPreview(next.ID, participant, next.Format, observedAt.UTC())
		if err != nil {
			return Battle{}, err
		}
		next.PreviewSubmissions = append(next.PreviewSubmissions, selection)
	}
	if len(next.PreviewSubmissions) != len(next.Participants) {
		return Battle{}, ErrInvalidBattle
	}
	sort.Slice(next.PreviewSubmissions, func(left, right int) bool {
		return next.PreviewSubmissions[left].Side < next.PreviewSubmissions[right].Side
	})
	next.Status = StatusRunning
	next.Version++
	next.UpdatedAt = observedAt.UTC()
	return next, nil
}

// validChallengeBattle 校验 Challenge 接受边界、目标 Team 与冻结赛制之间的基础一致性。
func validChallengeBattle(command NewChallengeBattleCommand) bool {
	challenge := command.Challenge
	if challenge.ID == snowflake.ID(0) || challenge.Status != ChallengeAccepted || challenge.ChallengerAccountID == snowflake.ID(0) ||
		challenge.TargetAccountID == snowflake.ID(0) || challenge.ChallengerPlayerCharacterID == snowflake.ID(0) ||
		challenge.TargetPlayerCharacterID == snowflake.ID(0) || challenge.BattleFormatID == snowflake.ID(0) ||
		!json.Valid(challenge.BattleFormatSnapshot) || !validBattleFormat(command.Format) {
		return false
	}
	target := command.TargetTeam
	return target.ID != snowflake.ID(0) && target.PlayerCharacterID == challenge.TargetPlayerCharacterID && target.Version >= 1 &&
		len(challenge.ChallengerTeam.Members) >= int(command.Format.RosterCount) && len(target.Members) >= int(command.Format.RosterCount)
}

// validTrainingBattle 验证真人、冻结赛制和版本化 Bot 共同满足创建练习赛的最小不变量。
func validTrainingBattle(command NewTrainingBattleCommand) bool {
	return command.AccountID != snowflake.ID(0) && command.PlayerCharacterID != snowflake.ID(0) &&
		strings.TrimSpace(command.DisplayName) != "" && command.Team.ID != snowflake.ID(0) &&
		command.Team.PlayerCharacterID == command.PlayerCharacterID && command.Team.Version >= 1 &&
		len(command.Team.Members) >= int(command.Format.RosterCount) && command.BattleFormatID != snowflake.ID(0) &&
		json.Valid(command.BattleFormatSnapshot) && validBattleFormat(command.Format) &&
		strings.TrimSpace(command.Bot.Code) != "" && command.Bot.StrategyVersion >= 1 &&
		strings.TrimSpace(command.Bot.DisplayName) != "" && len(command.Bot.Team.Members) >= int(command.Format.RosterCount) &&
		validFrozenBotDefinition(command.Bot.Definition)
}

// validBattleFormat 校验 Battle Preview 与期限参数的最小执行不变量。
func validBattleFormat(format Format) bool {
	return format.RosterCount >= 1 && format.RosterCount <= 6 && format.SelectCount >= 1 &&
		format.SelectCount <= format.RosterCount && format.ActiveParticipantsPerSide >= 1 &&
		format.ActiveParticipantsPerSide <= format.SelectCount && format.PreviewDuration > 0 &&
		format.BattleDuration > format.PreviewDuration && format.TurnDuration >= 0 &&
		(format.TurnDuration == 0 || format.TurnDuration < format.BattleDuration)
}

// automaticBotPreview 选择 Bot 队伍中稳定成员位置最小的合法成员作为确定性默认 Preview。
func automaticBotPreview(format Format, snapshot TeamSnapshot, submittedAt time.Time) PreviewSubmission {
	positions := make([]int32, 0, len(snapshot.Members))
	for _, member := range snapshot.Members {
		positions = append(positions, member.Position)
	}
	sort.Slice(positions, func(left, right int) bool { return positions[left] < positions[right] })
	selected := append([]int32(nil), positions[:format.SelectCount]...)
	return PreviewSubmission{
		Side: ParticipantSideTwo, MemberPositions: selected,
		ActivePositions: append([]int32(nil), selected[:format.ActiveParticipantsPerSide]...), SubmittedAt: submittedAt,
	}
}

// validCompletionResult 验证 completed 的胜者与原因之间不会产生对历史消费者含义不明的组合。
func validCompletionResult(result Result) bool {
	if !result.Reason.Valid() || validInterruptReason(result.Reason) {
		return false
	}
	if result.Reason == TerminalReasonDraw || result.Reason == TerminalReasonNoContest {
		return result.WinnerSide == 0
	}
	return result.WinnerSide == ParticipantSideOne || result.WinnerSide == ParticipantSideTwo
}

// validInterruptReason 将中断专用原因限制在不会产生胜负或普通分析样本的集合中。
func validInterruptReason(reason TerminalReason) bool {
	return reason == TerminalReasonStartupFailed || reason == TerminalReasonRuntimePanic || reason == TerminalReasonLeaseLost ||
		reason == TerminalReasonRuntimeFailed || reason == TerminalReasonRecoveryExhausted
}

// participantByPlayerCharacter 返回本场指定真人角色的冻结参赛方；Bot 不具有 PlayerCharacter 身份。
func (value Battle) participantByPlayerCharacter(playerCharacterID snowflake.ID) (Participant, bool) {
	if playerCharacterID == snowflake.ID(0) {
		return Participant{}, false
	}
	for _, participant := range value.Participants {
		if !participant.IsBot && participant.PlayerCharacterID == playerCharacterID {
			return cloneParticipant(participant), true
		}
	}
	return Participant{}, false
}

// hasPreviewSubmission 报告指定阵营是否已经锁定 Preview，防止同一方覆盖秘密选择。
func (value Battle) hasPreviewSubmission(side ParticipantSide) bool {
	for _, submission := range value.PreviewSubmissions {
		if submission.Side == side {
			return true
		}
	}
	return false
}

// validatePreviewSelection 验证选择数量、初始上场顺序、重复位置和 Team 快照归属。
func validatePreviewSelection(format Format, snapshot TeamSnapshot, command PreviewSubmissionCommand) error {
	if len(command.MemberPositions) != int(format.SelectCount) || len(command.ActivePositions) != int(format.ActiveParticipantsPerSide) {
		return ErrInvalidBattle
	}
	knownMembers := make(map[int32]struct{}, len(snapshot.Members))
	for _, member := range snapshot.Members {
		knownMembers[member.Position] = struct{}{}
	}
	selected := make(map[int32]struct{}, len(command.MemberPositions))
	for _, position := range command.MemberPositions {
		if _, exists := knownMembers[position]; !exists {
			return fmt.Errorf("%w: 选择成员位置 %d 不属于冻结 Team", ErrInvalidBattle, position)
		}
		if _, duplicate := selected[position]; duplicate {
			return fmt.Errorf("%w: 选择成员位置 %d 重复", ErrInvalidBattle, position)
		}
		selected[position] = struct{}{}
	}
	active := make(map[int32]struct{}, len(command.ActivePositions))
	for _, position := range command.ActivePositions {
		if _, exists := selected[position]; !exists {
			return fmt.Errorf("%w: 初始上场成员位置 %d 未被选择", ErrInvalidBattle, position)
		}
		if _, duplicate := active[position]; duplicate {
			return fmt.Errorf("%w: 初始上场成员位置 %d 重复", ErrInvalidBattle, position)
		}
		active[position] = struct{}{}
	}
	return nil
}

// cloneBattle 复制全部可变引用字段，使 Battle 值转换不会修改先前的历史快照。
func cloneBattle(source Battle) Battle {
	cloned := source
	cloned.BattleFormatSnapshot = append(json.RawMessage(nil), source.BattleFormatSnapshot...)
	cloned.Participants = make([]Participant, len(source.Participants))
	for index, participant := range source.Participants {
		cloned.Participants[index] = cloneParticipant(participant)
	}
	cloned.PreviewSubmissions = make([]PreviewSubmission, len(source.PreviewSubmissions))
	for index, submission := range source.PreviewSubmissions {
		cloned.PreviewSubmissions[index] = PreviewSubmission{
			Side:            submission.Side,
			MemberPositions: append([]int32(nil), submission.MemberPositions...),
			ActivePositions: append([]int32(nil), submission.ActivePositions...),
			SubmittedAt:     submission.SubmittedAt,
			RandomTrace:     append(json.RawMessage(nil), submission.RandomTrace...),
		}
	}
	cloned.Result = append(json.RawMessage(nil), source.Result...)
	return cloned
}

// cloneParticipant 复制 Participant 的 Team 快照，隔离后续 Team 编辑与调用方切片修改。
func cloneParticipant(source Participant) Participant {
	cloned := source
	cloned.Team = cloneTeamSnapshot(source.Team)
	cloned.BotDefinition = append(json.RawMessage(nil), source.BotDefinition...)
	return cloned
}

func validFrozenBotDefinition(definition json.RawMessage) bool {
	_, _, err := DecodeBotStrategyDefinition(definition)
	return err == nil
}

// cloneTeamSnapshot 深复制 Team 成员、技能和培养数据，保持 Battle 事实不受可变 Team 影响。
func cloneTeamSnapshot(source TeamSnapshot) TeamSnapshot {
	cloned := TeamSnapshot{SourceTeamID: source.SourceTeamID, SourceTeamVersion: source.SourceTeamVersion, Members: make([]team.Member, len(source.Members))}
	for index, member := range source.Members {
		cloned.Members[index] = member
		cloned.Members[index].Skills = append([]team.MemberSkill(nil), member.Skills...)
		cloned.Members[index].Stats = append([]team.MemberStat(nil), member.Stats...)
	}
	return cloned
}
