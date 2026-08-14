package api

import (
	"context"
	"errors"
	"log/slog"
	"strconv"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	kratoserrors "github.com/go-kratos/kratos/v3/errors"
	battlev1 "github.com/lishangbu/avalon/api/gen/go/avalon/battle/v1"
	battle "github.com/lishangbu/avalon/internal/battle"
	battlepersistence "github.com/lishangbu/avalon/internal/battle/persistence"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/playercharacter"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// KratosService 直接实现生成的 BattleService RPC 契约。
//
// 服务只负责认证、资源所有权、请求字段和领域错误的边界映射。回合结算仍由受限的 Runtime Registry
// 串行执行，传输层不会读取或组装任何对手秘密选择。
type KratosService struct {
	// reader 读取权威 Battle 领域对象。
	reader BattleReader
	// query 读取 Battle 历史与参与者披露投影。
	query BattleQuery
	// repository 保存 Battle Preview 与取消命令。
	repository BattleRepository
	// turns 将真人命令提交到当前进程中唯一的 Battle Runtime。
	turns TurnSubmitter
	// characters 按账户范围验证查询历史的 PlayerCharacter 所有权。
	characters PlayerCharacterQuery
	// now 提供可注入的服务端权威时间，Preview 截止时间不得使用客户端时间。
	now func() time.Time
	// logger 记录不能安全映射为公开错误的基础设施故障。
	logger *slog.Logger
	// realtime 在 Battle 状态成功持久化后向已连接 Participant 推送其各自的安全账本视图；nil 表示
	// 当前进程未启用流式传输时，权威命令仍可独立工作。
	realtime *battle.RealtimeHub
	// challenges 编排 Challenge 生命周期、Team 入场和实时资料校验；nil 表示当前进程不注册邀请能力。
	challenges *battle.ChallengeApplicationService
	// training 编排真人与版本固定 Bot 的训练战创建；nil 表示当前进程不注册训练战能力。
	training *battle.TrainingApplicationService
	// starter 在第二名 Participant 提交 Preview 后自动建立权威战斗状态和唯一 Runtime。
	starter BattleStarter
}

// NewKratosService 创建具备 Challenge、Training Battle、流式视图与自动启动能力的完整 Battle RPC 服务。
func NewKratosService(
	reader BattleReader,
	query BattleQuery,
	repository BattleRepository,
	turns TurnSubmitter,
	characters PlayerCharacterQuery,
	realtime *battle.RealtimeHub,
	challenges *battle.ChallengeApplicationService,
	training *battle.TrainingApplicationService,
	starter BattleStarter,
	now func() time.Time,
	logger *slog.Logger,
) *KratosService {
	if now == nil {
		now = time.Now
	}
	if logger == nil {
		logger = slog.Default()
	}
	return &KratosService{
		reader: reader, query: query, repository: repository, turns: turns, characters: characters, realtime: realtime, challenges: challenges, training: training, starter: starter,
		now: now, logger: logger,
	}
}

// GetBattle 查询当前认证账户参与的 Battle 参与者视图。
func (service *KratosService) GetBattle(
	ctx context.Context,
	request *battlev1.GetBattleRequest,
) (*battlev1.GetBattleResponse, error) {
	principal, err := battlePrincipal(ctx)
	if err != nil {
		return nil, err
	}
	battleID, err := parseBattleIdentifier(request.GetBattleId(), "INVALID_BATTLE_ID")
	if err != nil {
		return nil, err
	}
	session, err := service.getOwnedBattle(ctx, principal.AccountID, battleID)
	if err != nil {
		return nil, err
	}
	return &battlev1.GetBattleResponse{HttpStatusCode: 200, Body: battleView(session, principal.AccountID)}, nil
}

// SubmitBattlePreview 锁定当前真人 Participant 的 Team Preview。
func (service *KratosService) SubmitBattlePreview(
	ctx context.Context,
	request *battlev1.SubmitBattlePreviewRequest,
) (*battlev1.SubmitBattlePreviewResponse, error) {
	principal, err := battlePrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_BATTLE_PREVIEW", "对战预览请求无效")
	}
	battleID, err := parseBattleIdentifier(request.GetBattleId(), "INVALID_BATTLE_ID")
	if err != nil {
		return nil, err
	}
	if strings.TrimSpace(request.GetHeaderIdempotencyKey()) == "" {
		return nil, kratoserrors.BadRequest("IDEMPOTENCY_KEY_REQUIRED", "必须提供幂等键")
	}
	current, err := service.getOwnedBattle(ctx, principal.AccountID, battleID)
	if err != nil {
		return nil, err
	}
	participant, found := participantForAccount(current, principal.AccountID)
	if !found {
		return nil, kratoserrors.NotFound("BATTLE_NOT_FOUND", "对战不存在")
	}
	if current.Status == battle.StatusPreview && len(current.PreviewSubmissions)+1 == len(current.Participants) && service.starter == nil {
		return nil, kratoserrors.ServiceUnavailable("BATTLE_START_UNAVAILABLE", "对战启动服务当前不可用")
	}
	updated, submitErr := service.repository.SubmitPreview(ctx, battleID, battle.PreviewSubmissionCommand{
		PlayerCharacterID: participant.PlayerCharacterID,
		MemberPositions:   append([]int32(nil), request.GetBody().GetMemberPositions()...),
		ActivePositions:   append([]int32(nil), request.GetBody().GetActivePositions()...),
	}, service.now().UTC())
	if submitErr != nil {
		return nil, service.battleError(ctx, "BATTLE_PREVIEW_SUBMIT_FAILED", submitErr)
	}
	if updated.Status == battle.StatusRunning && updated.StartedAt.IsZero() {
		if service.starter == nil {
			return nil, kratoserrors.ServiceUnavailable("BATTLE_START_UNAVAILABLE", "对战启动服务当前不可用")
		}
		updated, submitErr = service.starter.Start(ctx, updated)
		if submitErr != nil {
			return nil, service.battleError(ctx, "BATTLE_START_FAILED", submitErr)
		}
	} else {
		service.publishDisclosure(ctx, battleID)
	}
	return &battlev1.SubmitBattlePreviewResponse{HttpStatusCode: 200, Body: battleView(updated, principal.AccountID)}, nil
}

// SubmitBattleTurn 将当前真人 Participant 的完整秘密回合选择交给内存 Runtime。
func (service *KratosService) SubmitBattleTurn(
	ctx context.Context,
	request *battlev1.SubmitBattleTurnRequest,
) (*battlev1.SubmitBattleTurnResponse, error) {
	principal, err := battlePrincipal(ctx)
	if err != nil {
		return nil, err
	}
	if request.GetBody() == nil {
		return nil, kratoserrors.BadRequest("INVALID_BATTLE_TURN", "对战回合请求无效")
	}
	battleID, err := parseBattleIdentifier(request.GetBattleId(), "INVALID_BATTLE_ID")
	if err != nil {
		return nil, err
	}
	expectedStateVersion, err := parseStateVersion(request.GetBody().GetExpectedStateVersion())
	if err != nil {
		return nil, err
	}
	if strings.TrimSpace(request.GetHeaderIdempotencyKey()) == "" {
		return nil, kratoserrors.BadRequest("IDEMPOTENCY_KEY_REQUIRED", "必须提供幂等键")
	}
	current, err := service.getOwnedBattle(ctx, principal.AccountID, battleID)
	if err != nil {
		return nil, err
	}
	participant, found := participantForAccount(current, principal.AccountID)
	if !found {
		return nil, kratoserrors.NotFound("BATTLE_NOT_FOUND", "对战不存在")
	}
	actions, err := turnActions(request.GetBody().GetActions())
	if err != nil {
		return nil, err
	}
	result, submitErr := service.turns.Submit(ctx, battleID, battle.TurnSubmission{
		PlayerCharacterID:    participant.PlayerCharacterID,
		ExpectedStateVersion: expectedStateVersion,
		IdempotencyKey:       request.GetHeaderIdempotencyKey(),
		Actions:              actions,
	})
	if submitErr != nil {
		return nil, service.battleError(ctx, "BATTLE_TURN_SUBMIT_FAILED", submitErr)
	}
	if result.Resolved {
		service.publishDisclosure(ctx, battleID)
	}
	return &battlev1.SubmitBattleTurnResponse{HttpStatusCode: 200, Body: &battlev1.BattleTurnResult{
		Locked: result.Locked, Resolved: result.Resolved, StateVersion: strconv.FormatInt(result.StateVersion, 10),
	}}, nil
}

// CancelBattle 取消当前认证账户参与且尚未启动 Runtime 的 Battle。
func (service *KratosService) CancelBattle(ctx context.Context, request *battlev1.CancelBattleRequest) (*battlev1.CancelBattleResponse, error) {
	principal, err := battlePrincipal(ctx)
	if err != nil {
		return nil, err
	}
	battleID, err := parseBattleIdentifier(request.GetBattleId(), "INVALID_BATTLE_ID")
	if err != nil {
		return nil, err
	}
	if _, err := service.getOwnedBattle(ctx, principal.AccountID, battleID); err != nil {
		return nil, err
	}
	canceled, err := service.repository.Cancel(ctx, battleID, service.now().UTC())
	if err != nil {
		return nil, service.battleError(ctx, "BATTLE_CANCEL_FAILED", err)
	}
	service.publishDisclosure(ctx, battleID)
	return &battlev1.CancelBattleResponse{HttpStatusCode: 200, Body: battleView(canceled, principal.AccountID)}, nil
}

// publishDisclosure 在权威写入成功后异步语义地通知本进程连接重新读取已提交账本。
//
// Hub 不参与事务也不影响 RPC 命令成功；若连接或广播暂时不可用，客户端可通过重连取得账本的
// 完整最新状态。日志只保留低基数 Battle ID，避免把战斗秘密写入服务端日志。
func (service *KratosService) publishDisclosure(ctx context.Context, battleID snowflake.ID) {
	if service.realtime == nil {
		return
	}
	service.realtime.Publish(ctx, battleID)
}

// ListBattleHistory 查询当前认证账户拥有角色的已终局 Battle 统一页码历史。
func (service *KratosService) ListBattleHistory(
	ctx context.Context,
	request *battlev1.ListBattleHistoryRequest,
) (*battlev1.ListBattleHistoryResponse, error) {
	principal, err := battlePrincipal(ctx)
	if err != nil {
		return nil, err
	}
	characterID, err := parseBattleIdentifier(request.GetPlayerCharacterId(), "INVALID_PLAYER_CHARACTER_ID")
	if err != nil {
		return nil, err
	}
	if request.GetPage() < 1 || request.GetPageSize() < 1 || request.GetPageSize() > 100 {
		return nil, kratoserrors.BadRequest("INVALID_PAGE", "页码或每页数量无效")
	}
	if service.characters == nil {
		return nil, service.battleError(ctx, "BATTLE_HISTORY_QUERY_FAILED", errors.New("玩家角色查询不可用"))
	}
	if service.query == nil {
		return nil, service.battleError(ctx, "BATTLE_HISTORY_QUERY_FAILED", errors.New("对战历史查询端口不可用"))
	}
	if _, err := service.characters.GetOwned(ctx, principal.AccountID, characterID); err != nil {
		return nil, service.battleError(ctx, "BATTLE_HISTORY_QUERY_FAILED", err)
	}
	page, listErr := service.query.ListHistory(ctx, characterID, request.GetPage(), request.GetPageSize())
	if listErr != nil {
		return nil, service.battleError(ctx, "BATTLE_HISTORY_QUERY_FAILED", listErr)
	}
	items := make([]*battlev1.BattleHistoryItem, len(page.Items))
	for index := range page.Items {
		items[index] = historyItem(page.Items[index])
	}
	return &battlev1.ListBattleHistoryResponse{HttpStatusCode: 200, Body: &battlev1.BattleHistoryPage{
		Items: items, Page: page.Page, PageSize: page.PageSize, Total: page.Total,
	}}, nil
}

// GetBattleHistoryDetail 返回当前账户指定角色在一场已终局 Battle 中最后获准知道的安全视图。
//
// 该方法绝不读取或转换完整 Turn Record。所有可见战斗状态只来自按 Participant 分开的 Disclosure Ledger，
// 所以即使对手存在未披露的后备成员、技能或 PP，也不会因为历史详情而泄露。
func (service *KratosService) GetBattleHistoryDetail(
	ctx context.Context,
	request *battlev1.GetBattleHistoryDetailRequest,
) (*battlev1.GetBattleHistoryDetailResponse, error) {
	principal, err := battlePrincipal(ctx)
	if err != nil {
		return nil, err
	}
	playerCharacterID, err := parseBattleIdentifier(request.GetPlayerCharacterId(), "INVALID_PLAYER_CHARACTER_ID")
	if err != nil {
		return nil, err
	}
	battleID, err := parseBattleIdentifier(request.GetBattleId(), "INVALID_BATTLE_ID")
	if err != nil {
		return nil, err
	}
	if service.characters == nil {
		return nil, service.battleError(ctx, "BATTLE_HISTORY_DETAIL_FAILED", errors.New("玩家角色查询不可用"))
	}
	if service.query == nil {
		return nil, service.battleError(ctx, "BATTLE_HISTORY_DETAIL_FAILED", errors.New("对战历史查询端口不可用"))
	}
	if _, err := service.characters.GetOwned(ctx, principal.AccountID, playerCharacterID); err != nil {
		return nil, service.battleError(ctx, "BATTLE_HISTORY_DETAIL_FAILED", err)
	}
	session, err := service.getOwnedBattle(ctx, principal.AccountID, battleID)
	if err != nil {
		return nil, err
	}
	participant, found := participantForAccount(session, principal.AccountID)
	if !found || participant.PlayerCharacterID != playerCharacterID ||
		(session.Status != battle.StatusCompleted && session.Status != battle.StatusInterrupted) {
		return nil, kratoserrors.NotFound("BATTLE_HISTORY_NOT_FOUND", "对战历史不存在")
	}
	disclosure, err := service.query.GetParticipantDisclosure(ctx, battleID, playerCharacterID)
	if err != nil {
		return nil, service.battleError(ctx, "BATTLE_HISTORY_DETAIL_FAILED", err)
	}
	return &battlev1.GetBattleHistoryDetailResponse{HttpStatusCode: 200, Body: &battlev1.BattleHistoryDetail{
		Battle: battleView(session, principal.AccountID), Disclosure: disclosureView(disclosure),
	}}, nil
}

// WatchBattleDisclosure 通过 Server Streaming 推送当前 Participant 的安全披露账本视图。
// 每个订阅先发送完整快照，随后只发送已持久化的新状态版本；连接取消时释放 Hub 订阅。
func (service *KratosService) WatchBattleDisclosure(
	request *battlev1.WatchBattleDisclosureRequest,
	stream battlev1.BattleService_WatchBattleDisclosureServer,
) error {
	ctx := stream.Context()
	if service.realtime == nil {
		return kratoserrors.ServiceUnavailable("REALTIME_UNAVAILABLE", "对战实时视图不可用")
	}
	if request == nil {
		return kratoserrors.BadRequest("INVALID_REQUEST", "请求格式无效")
	}
	if _, err := battlePrincipal(ctx); err != nil {
		return err
	}
	battleID, err := snowflake.Parse(request.GetBattleId())
	if err != nil {
		return kratoserrors.BadRequest("INVALID_BATTLE_ID", "对战标识无效")
	}
	playerCharacterID, err := snowflake.Parse(request.GetPlayerCharacterId())
	if err != nil {
		return kratoserrors.BadRequest("INVALID_PLAYER_CHARACTER_ID", "角色标识无效")
	}
	subscription, err := service.realtime.Subscribe(ctx, battleID, playerCharacterID)
	if err != nil {
		return service.battleError(ctx, "REALTIME_SUBSCRIBE_FAILED", err)
	}
	defer subscription.Close()
	for {
		select {
		case <-ctx.Done():
			return ctx.Err()
		case view, ok := <-subscription.Views:
			if !ok {
				return nil
			}
			if err := stream.Send(&battlev1.WatchBattleDisclosureResponse{Disclosure: disclosureView(view)}); err != nil {
				return err
			}
		}
	}
}

func battlePrincipal(ctx context.Context) (authentication.Principal, error) {
	principal, ok := authentication.PrincipalFromContext(ctx)
	if !ok || principal.AccountID == snowflake.ID(0) {
		return authentication.Principal{}, kratoserrors.Unauthorized("SESSION_INVALID", "登录会话无效")
	}
	return principal, nil
}

func parseBattleIdentifier(raw, reason string) (snowflake.ID, error) {
	value, err := snowflake.Parse(raw)
	if err != nil || value == snowflake.ID(0) {
		return snowflake.ID(0), kratoserrors.BadRequest(reason, "标识格式无效")
	}
	return value, nil
}

func parseStateVersion(raw string) (int64, error) {
	value, err := strconv.ParseInt(raw, 10, 64)
	if err != nil || value < 0 {
		return 0, kratoserrors.BadRequest("INVALID_STATE_VERSION", "状态版本格式无效")
	}
	return value, nil
}

func (service *KratosService) getOwnedBattle(ctx context.Context, accountID, battleID snowflake.ID) (battle.Battle, error) {
	if service.reader == nil {
		return battle.Battle{}, service.battleError(ctx, "BATTLE_QUERY_FAILED", errors.New("对战读取端口不可用"))
	}
	session, err := service.reader.Get(ctx, battleID)
	if err != nil {
		return battle.Battle{}, service.battleError(ctx, "BATTLE_QUERY_FAILED", err)
	}
	if !sessionContainsAccount(session, accountID) {
		return battle.Battle{}, kratoserrors.NotFound("BATTLE_NOT_FOUND", "对战不存在")
	}
	return session, nil
}

func sessionContainsAccount(session battle.Battle, accountID snowflake.ID) bool {
	for _, participant := range session.Participants {
		if !participant.IsBot && participant.AccountID == accountID {
			return true
		}
	}
	return false
}

func participantForAccount(session battle.Battle, accountID snowflake.ID) (battle.Participant, bool) {
	for _, participant := range session.Participants {
		if !participant.IsBot && participant.AccountID == accountID {
			return participant, true
		}
	}
	return battle.Participant{}, false
}

func battleView(session battle.Battle, accountID snowflake.ID) *battlev1.BattleView {
	participants := make([]*battlev1.BattleParticipantView, len(session.Participants))
	for index, participant := range session.Participants {
		participants[index] = &battlev1.BattleParticipantView{
			Side: participantSideString(participant.Side), DisplayName: participant.DisplayName,
			Bot: participant.IsBot, BotCode: participant.BotCode, BotStrategyVersion: int32(participant.BotStrategyVersion),
		}
	}
	previews := make([]*battlev1.BattlePreviewView, 0, 1)
	for _, preview := range session.PreviewSubmissions {
		participant, found := participantBySide(session, preview.Side)
		if !found || participant.IsBot || participant.AccountID != accountID {
			continue
		}
		previews = append(previews, &battlev1.BattlePreviewView{
			Side: participantSideString(preview.Side), Locked: true,
			MemberPositions: append([]int32(nil), preview.MemberPositions...),
			ActivePositions: append([]int32(nil), preview.ActivePositions...),
		})
	}
	return &battlev1.BattleView{
		Id: session.ID.String(), Mode: string(session.Mode), SourceType: string(session.SourceType), Status: string(session.Status),
		StateVersion:      strconv.FormatInt(session.StateVersion, 10),
		PreviewDeadlineAt: timeString(session.PreviewDeadlineAt), BattleDeadlineAt: timeString(session.BattleDeadlineAt),
		StartedAt: timeString(session.StartedAt), CompletedAt: timeString(session.CompletedAt),
		TerminalReason: session.TerminalReason, Participants: participants, Previews: previews,
	}
}

func participantBySide(session battle.Battle, side battle.ParticipantSide) (battle.Participant, bool) {
	for _, participant := range session.Participants {
		if participant.Side == side {
			return participant, true
		}
	}
	return battle.Participant{}, false
}

func participantSideString(side battle.ParticipantSide) string {
	switch side {
	case battle.ParticipantSideOne:
		return "one"
	case battle.ParticipantSideTwo:
		return "two"
	default:
		return ""
	}
}

func historyItem(entry battle.HistoryEntry) *battlev1.BattleHistoryItem {
	return &battlev1.BattleHistoryItem{
		BattleId: entry.BattleID.String(), Mode: string(entry.Mode), SourceType: string(entry.SourceType), Side: participantSideString(entry.Side),
		DisplayName: entry.DisplayName, WinnerSide: participantSideString(entry.WinnerSide),
		TerminalReason: entry.TerminalReason, TurnCount: entry.TurnCount, CompletedAt: timeString(entry.CompletedAt),
	}
}

func disclosureView(view battle.DisclosureView) *battlev1.BattleDisclosureView {
	members := make([]*battlev1.BattleDisclosureMemberView, len(view.Members))
	for index, member := range view.Members {
		stages := make(map[string]int32, len(member.StatStages))
		for stat, stage := range member.StatStages {
			stages[string(stat)] = int32(stage)
		}
		remainingPP := make([]uint32, len(member.RemainingPP))
		for ppIndex, pp := range member.RemainingPP {
			remainingPP[ppIndex] = uint32(pp)
		}
		members[index] = &battlev1.BattleDisclosureMemberView{
			Side: participantSideString(member.Side), MemberPosition: int32(member.MemberPosition),
			SlotPosition: int32(member.SlotPosition), CurrentHp: member.CurrentHP, MajorStatus: string(member.MajorStatus),
			BadPoisonCounter: member.BadPoisonCounter, SleepTurnsRemaining: member.SleepTurnsRemaining,
			StatStages: stages, RemainingPp: remainingPP,
		}
	}
	result := &battlev1.BattleDisclosureView{
		SchemaVersion: int32(view.SchemaVersion), StateVersion: strconv.FormatInt(view.StateVersion, 10),
		TurnNumber: int32(view.TurnNumber), Members: members,
	}
	if view.Result != nil {
		result.WinnerSide = participantSideString(battle.ParticipantSide(view.Result.WinningSide))
		result.TerminalReason = string(view.Result.Reason)
	}
	return result
}

func timeString(value time.Time) string {
	if value.IsZero() {
		return ""
	}
	return value.UTC().Format(time.RFC3339Nano)
}

func turnActions(values []*battlev1.BattleTurnAction) ([]battleengine.Action, error) {
	if len(values) == 0 {
		return nil, kratoserrors.BadRequest("INVALID_BATTLE_ACTIONS", "回合行动不能为空")
	}
	result := make([]battleengine.Action, len(values))
	for index, value := range values {
		if value == nil || value.GetActorSlotPosition() < 1 || value.GetActorSlotPosition() > 2 {
			return nil, kratoserrors.BadRequest("INVALID_BATTLE_ACTION", "行动者槽位无效")
		}
		action := battleengine.Action{Actor: battleengine.SlotRef{Position: battleengine.SlotPosition(value.GetActorSlotPosition())}}
		switch value.GetKind() {
		case string(battleengine.ActionKindUseSkill):
			if value.GetUseSkill() == nil || value.GetSwitch() != nil || value.GetUseSkill().GetSkillPosition() < 1 ||
				value.GetUseSkill().GetSkillPosition() > 4 || value.GetUseSkill().GetTargetSlotPosition() < 1 ||
				value.GetUseSkill().GetTargetSlotPosition() > 2 {
				return nil, kratoserrors.BadRequest("INVALID_BATTLE_ACTION", "技能行动字段无效")
			}
			targetSide, err := parseParticipantSide(value.GetUseSkill().GetTargetSide())
			if err != nil {
				return nil, err
			}
			action.Kind = battleengine.ActionKindUseSkill
			action.UseSkill = &battleengine.UseSkillAction{
				SkillPosition: battleengine.SkillPosition(value.GetUseSkill().GetSkillPosition()),
				Target:        battleengine.SlotRef{Side: battleSide(targetSide), Position: battleengine.SlotPosition(value.GetUseSkill().GetTargetSlotPosition())},
				Terastallize:  value.GetUseSkill().GetTerastallize(),
			}
		case string(battleengine.ActionKindSwitch):
			if value.GetSwitch() == nil || value.GetUseSkill() != nil || value.GetSwitch().GetMemberPosition() < 1 ||
				value.GetSwitch().GetMemberPosition() > 6 {
				return nil, kratoserrors.BadRequest("INVALID_BATTLE_ACTION", "换人行动字段无效")
			}
			action.Kind = battleengine.ActionKindSwitch
			action.Switch = &battleengine.SwitchAction{MemberPosition: battleengine.MemberPosition(value.GetSwitch().GetMemberPosition())}
		default:
			return nil, kratoserrors.BadRequest("INVALID_BATTLE_ACTION", "行动种类无效")
		}
		result[index] = action
	}
	return result, nil
}

func parseParticipantSide(raw string) (battle.ParticipantSide, error) {
	switch raw {
	case "one":
		return battle.ParticipantSideOne, nil
	case "two":
		return battle.ParticipantSideTwo, nil
	default:
		return 0, kratoserrors.BadRequest("INVALID_TARGET_SIDE", "目标阵营无效")
	}
}

func battleSide(side battle.ParticipantSide) battleengine.Side {
	if side == battle.ParticipantSideOne {
		return battleengine.SideOne
	}
	return battleengine.SideTwo
}

func (service *KratosService) battleError(ctx context.Context, reason string, err error) error {
	switch {
	case errors.Is(err, battlepersistence.ErrBattleNotFound), errors.Is(err, playercharacter.ErrPlayerCharacterNotFound):
		return kratoserrors.NotFound("BATTLE_NOT_FOUND", "对战或游戏角色不存在")
	case errors.Is(err, battle.ErrInvalidBattle), errors.Is(err, battle.ErrInvalidRuntime),
		errors.Is(err, battleengine.ErrInvalidTurnCommand):
		return kratoserrors.BadRequest("INVALID_BATTLE_COMMAND", "对战命令无效")
	case errors.Is(err, battle.ErrPreviewExpired):
		return kratoserrors.Conflict("BATTLE_PREVIEW_EXPIRED", "对战预览已超时")
	case errors.Is(err, battle.ErrPreviewAlreadySubmitted), errors.Is(err, battle.ErrBattleNotPreview),
		errors.Is(err, battle.ErrBattleNotRunning), errors.Is(err, battle.ErrBattleTerminal),
		errors.Is(err, battle.ErrBattleNotPendingRuntime),
		errors.Is(err, battle.ErrBattleDeadlineExpired), errors.Is(err, battle.ErrRuntimeNotFound),
		errors.Is(err, battle.ErrRuntimeIdempotencyConflict), errors.Is(err, battlepersistence.ErrBattleConflict):
		return kratoserrors.Conflict("BATTLE_CONFLICT", "对战状态、版本或幂等请求冲突")
	default:
		service.logger.ErrorContext(ctx, "Battle Kratos 服务调用失败", "reason", reason, "error", err)
		return kratoserrors.InternalServer(reason, "服务端无法完成请求")
	}
}
