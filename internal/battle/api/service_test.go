package api

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battlev1 "github.com/lishangbu/avalon/api/gen/go/avalon/battle/v1"
	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/playercharacter"
	"github.com/lishangbu/avalon/internal/security/authentication"
)

// TestGetBattleDoesNotExposeOpponentPreview 验证秘密 Preview 阶段只返回当前账户自己的锁定选择。
func TestGetBattleDoesNotExposeOpponentPreview(t *testing.T) {
	t.Parallel()
	leftAccount := snowflake.MustParse("1048576159")
	rightAccount := snowflake.MustParse("1048576160")
	value := battle.Battle{
		ID: snowflake.MustParse("1048576161"), Mode: battle.BattleModePvP, SourceType: battle.BattleSourceChallenge,
		Status: battle.StatusPreview, PreviewDeadlineAt: time.Date(2026, 7, 30, 12, 0, 0, 0, time.UTC),
		BattleDeadlineAt: time.Date(2026, 7, 30, 12, 30, 0, 0, time.UTC),
		Participants: []battle.Participant{
			{Side: battle.ParticipantSideOne, AccountID: leftAccount, PlayerCharacterID: snowflake.MustParse("1048576162"), DisplayName: "甲"},
			{Side: battle.ParticipantSideTwo, AccountID: rightAccount, PlayerCharacterID: snowflake.MustParse("1048576163"), DisplayName: "乙"},
		},
		PreviewSubmissions: []battle.PreviewSubmission{
			{Side: battle.ParticipantSideOne, MemberPositions: []int32{1, 2}, ActivePositions: []int32{1}},
			{Side: battle.ParticipantSideTwo, MemberPositions: []int32{3, 4}, ActivePositions: []int32{3}},
		},
	}
	service := NewKratosService(stubBattleRepository{session: value}, nil, nil, nil, nil, nil, nil, nil, nil, time.Now, nil)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: leftAccount})

	response, err := service.GetBattle(ctx, &battlev1.GetBattleRequest{BattleId: value.ID.String()})
	if err != nil {
		t.Fatalf("GetBattle() error = %v", err)
	}
	if len(response.GetBody().GetPreviews()) != 1 {
		t.Fatalf("预览数量 = %d，期望只暴露当前参与者的一条", len(response.GetBody().GetPreviews()))
	}
	preview := response.GetBody().GetPreviews()[0]
	if preview.GetSide() != "one" || len(preview.GetMemberPositions()) != 2 || preview.GetMemberPositions()[0] != 1 {
		t.Fatalf("当前参与者预览 = %+v，期望仅包含甲方锁定选择", preview)
	}
}

// TestTurnActionsPreservesTerastallizeRequest 验证 RPC/Proto 边界只传递太晶化请求，不在传输层解释赛制或次数规则。
func TestTurnActionsPreservesTerastallizeRequest(t *testing.T) {
	t.Parallel()
	actions, err := turnActions([]*battlev1.BattleTurnAction{{
		Kind: "useSkill", ActorSlotPosition: 1,
		UseSkill: &battlev1.BattleUseSkillAction{
			SkillPosition: 1, TargetSide: "two", TargetSlotPosition: 1, Terastallize: true,
		},
	}})
	if err != nil {
		t.Fatalf("turnActions() error = %v", err)
	}
	if len(actions) != 1 || actions[0].UseSkill == nil || !actions[0].UseSkill.Terastallize {
		t.Fatalf("turnActions() = %+v，期望保留太晶化请求", actions)
	}
}

// TestListBattleHistoryChecksPlayerCharacterOwnership 验证历史查询不能通过猜测角色 Identifier 越权读取。
func TestListBattleHistoryChecksPlayerCharacterOwnership(t *testing.T) {
	t.Parallel()
	accountID := snowflake.MustParse("1048576164")
	characterID := snowflake.MustParse("1048576165")
	service := NewKratosService(
		nil, stubBattleRepository{}, nil, nil,
		stubPlayerCharacterReader{err: playercharacter.ErrPlayerCharacterNotFound}, nil, nil, nil, nil, time.Now, nil,
	)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	_, err := service.ListBattleHistory(ctx, &battlev1.ListBattleHistoryRequest{
		PlayerCharacterId: characterID.String(), Page: 1, PageSize: 20,
	})
	if err == nil {
		t.Fatal("ListBattleHistory() error = nil，期望拒绝非本人角色")
	}
}

// TestSubmitBattleTurnDerivesPlayerCharacterFromParticipant 验证回合身份只来自已认证账号在 Battle 中冻结的 Participant。
func TestSubmitBattleTurnDerivesPlayerCharacterFromParticipant(t *testing.T) {
	t.Parallel()
	accountID := snowflake.MustParse("1048576166")
	ownedCharacterID := snowflake.MustParse("1048576167")
	value := battle.Battle{
		ID: snowflake.MustParse("1048576169"), Status: battle.StatusRunning, StartedAt: time.Now().UTC(),
		Participants: []battle.Participant{
			{Side: battle.ParticipantSideOne, AccountID: accountID, PlayerCharacterID: ownedCharacterID},
			{Side: battle.ParticipantSideTwo, AccountID: snowflake.MustParse("1048576170"), PlayerCharacterID: snowflake.MustParse("1048576171")},
		},
	}
	turns := &stubTurnSubmitter{}
	service := NewKratosService(stubBattleRepository{session: value}, nil, nil, turns, nil, nil, nil, nil, nil, time.Now, nil)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	_, err := service.SubmitBattleTurn(ctx, &battlev1.SubmitBattleTurnRequest{
		BattleId: value.ID.String(), HeaderIdempotencyKey: "test-battle-turn",
		Body: &battlev1.BattleTurnSubmission{ExpectedStateVersion: "0", Actions: []*battlev1.BattleTurnAction{{
			Kind: "switch", ActorSlotPosition: 1, Switch: &battlev1.BattleSwitchAction{MemberPosition: 2},
		}}},
	})
	if err != nil {
		t.Fatalf("SubmitBattleTurn() error = %v", err)
	}
	if !turns.called || turns.submission.PlayerCharacterID != ownedCharacterID {
		t.Fatalf("SubmitBattleTurn() submission = %+v", turns.submission)
	}
}

// TestCancelBattleAllowsParticipantAndReturnsCanceledView 验证取消身份来自 Bearer Principal，且成功返回终局视图。
func TestCancelBattleAllowsParticipantAndReturnsCanceledView(t *testing.T) {
	t.Parallel()
	accountID := snowflake.MustParse("1048576201")
	value := battle.Battle{
		ID: snowflake.MustParse("1048576202"), Status: battle.StatusPreview,
		Participants: []battle.Participant{{Side: battle.ParticipantSideOne, AccountID: accountID, PlayerCharacterID: snowflake.MustParse("1048576203")}},
	}
	canceled := value
	canceled.Status = battle.StatusCanceled
	canceled.TerminalReason = string(battle.TerminalReasonCanceled)
	service := NewKratosService(
		stubBattleRepository{session: canceled}, nil, stubBattleRepository{session: canceled},
		nil, nil, nil, nil, nil, nil, time.Now, nil,
	)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.CancelBattle(ctx, &battlev1.CancelBattleRequest{BattleId: value.ID.String()})
	if err != nil {
		t.Fatalf("CancelBattle() error = %v", err)
	}
	if response.GetBody().GetStatus() != string(battle.StatusCanceled) || response.GetBody().GetTerminalReason() != string(battle.TerminalReasonCanceled) {
		t.Fatalf("CancelBattle() response = %+v", response.GetBody())
	}
}

// TestSubmitBattlePreviewStartsBattleWhenSecondPreviewCompletes 验证第二份 Preview 提交后 RPC 层只返回启动成功的 running Battle。
func TestSubmitBattlePreviewStartsBattleWhenSecondPreviewCompletes(t *testing.T) {
	t.Parallel()
	accountID := snowflake.MustParse("1048576172")
	characterID := snowflake.MustParse("1048576173")
	session := battle.Battle{
		ID: snowflake.MustParse("1048576174"), Status: battle.StatusRunning,
		Participants: []battle.Participant{
			{Side: battle.ParticipantSideOne, AccountID: accountID, PlayerCharacterID: characterID},
			{Side: battle.ParticipantSideTwo, AccountID: snowflake.NewTestID(), PlayerCharacterID: snowflake.NewTestID()},
		},
	}
	started := session
	started.Status = battle.StatusRunning
	started.StartedAt = time.Now().UTC()
	starter := &stubBattleStarter{session: started}
	service := NewKratosService(
		stubBattleRepository{session: session}, nil, stubBattleRepository{session: session},
		nil, nil, nil, nil, nil, starter, time.Now, nil,
	)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})

	response, err := service.SubmitBattlePreview(ctx, &battlev1.SubmitBattlePreviewRequest{
		BattleId: session.ID.String(), HeaderIdempotencyKey: "preview-start-001",
		Body: &battlev1.BattlePreviewSubmission{MemberPositions: []int32{1}, ActivePositions: []int32{1}},
	})
	if err != nil {
		t.Fatalf("SubmitBattlePreview() error = %v", err)
	}
	if !starter.called || response.GetBody().GetStatus() != string(battle.StatusRunning) {
		t.Fatalf("自动启动结果 = %+v，响应 = %+v", starter, response.GetBody())
	}
}

// TestGetBattleHistoryDetailUsesParticipantDisclosureLedger 验证已终局历史详情只返回所属角色最后获准知道
// 的安全账本，不会改读包含双方秘密命令的 Turn Record。
func TestGetBattleHistoryDetailUsesParticipantDisclosureLedger(t *testing.T) {
	t.Parallel()
	accountID := snowflake.MustParse("1048576196")
	characterID := snowflake.MustParse("1048576197")
	battleID := snowflake.MustParse("1048576198")
	session := battle.Battle{
		ID: battleID, Status: battle.StatusCompleted,
		Participants: []battle.Participant{{Side: battle.ParticipantSideOne, AccountID: accountID, PlayerCharacterID: characterID}},
	}
	service := NewKratosService(
		stubBattleRepository{session: session, disclosure: battle.DisclosureView{
			SchemaVersion: 1, StateVersion: 7, TurnNumber: 4,
			Members: []battle.DisclosureMember{{Side: battle.ParticipantSideOne, MemberPosition: 1, CurrentHP: 21, RemainingPP: []uint8{3}}},
		}},
		stubBattleRepository{session: session, disclosure: battle.DisclosureView{
			SchemaVersion: 1, StateVersion: 7, TurnNumber: 4,
			Members: []battle.DisclosureMember{{Side: battle.ParticipantSideOne, MemberPosition: 1, CurrentHP: 21, RemainingPP: []uint8{3}}},
		}},
		nil,
		nil,
		stubPlayerCharacterReader{character: playercharacter.PlayerCharacter{ID: characterID, AccountID: accountID}},
		nil,
		nil,
		nil,
		nil,
		time.Now,
		nil,
	)
	ctx := authentication.WithPrincipal(context.Background(), authentication.Principal{AccountID: accountID})
	response, err := service.GetBattleHistoryDetail(ctx, &battlev1.GetBattleHistoryDetailRequest{
		PlayerCharacterId: characterID.String(), BattleId: battleID.String(),
	})
	if err != nil {
		t.Fatalf("GetBattleHistoryDetail() error = %v", err)
	}
	view := response.GetBody().GetDisclosure()
	if view.GetStateVersion() != "7" || view.GetTurnNumber() != 4 || len(view.GetMembers()) != 1 ||
		view.GetMembers()[0].GetCurrentHp() != 21 || len(view.GetMembers()[0].GetRemainingPp()) != 1 {
		t.Fatalf("历史安全视图 = %+v", view)
	}
}

type stubBattleRepository struct {
	session battle.Battle
	err     error
	page    battle.HistoryPage
	// disclosure 是历史详情查询应返回的已隔离安全视图。
	disclosure battle.DisclosureView
}

func (repository stubBattleRepository) Get(context.Context, snowflake.ID) (battle.Battle, error) {
	return repository.session, repository.err
}

func (repository stubBattleRepository) SubmitPreview(
	context.Context,
	snowflake.ID,
	battle.PreviewSubmissionCommand,
	time.Time,
) (battle.Battle, error) {
	return repository.session, repository.err
}

func (repository stubBattleRepository) Cancel(context.Context, snowflake.ID, time.Time) (battle.Battle, error) {
	return repository.session, repository.err
}

func (repository stubBattleRepository) ListHistory(context.Context, snowflake.ID, int32, int32) (battle.HistoryPage, error) {
	return repository.page, repository.err
}

// GetParticipantDisclosure 返回预设安全账本视图，不提供完整 Turn Record。
func (repository stubBattleRepository) GetParticipantDisclosure(
	context.Context,
	snowflake.ID,
	snowflake.ID,
) (battle.DisclosureView, error) {
	return repository.disclosure, repository.err
}

type stubTurnSubmitter struct {
	called     bool
	submission battle.TurnSubmission
	err        error
}

// stubBattleStarter 用于验证 RPC 适配器在 pending 到 running 边界调用启动应用服务。
type stubBattleStarter struct {
	called  bool
	session battle.Battle
	err     error
}

func (starter *stubBattleStarter) Start(context.Context, battle.Battle) (battle.Battle, error) {
	starter.called = true
	return starter.session, starter.err
}

func (submitter *stubTurnSubmitter) Submit(
	_ context.Context,
	_ snowflake.ID,
	submission battle.TurnSubmission,
) (battle.TurnSubmissionResult, error) {
	submitter.called = true
	submitter.submission = submission
	return battle.TurnSubmissionResult{}, submitter.err
}

type stubPlayerCharacterReader struct {
	// character 是测试中允许当前账号读取的角色。
	character playercharacter.PlayerCharacter
	// err 模拟角色归属查询失败或越权。
	err error
}

func (query stubPlayerCharacterReader) GetOwned(
	context.Context,
	snowflake.ID,
	snowflake.ID,
) (playercharacter.PlayerCharacter, error) {
	if query.err != nil {
		return playercharacter.PlayerCharacter{}, query.err
	}
	if query.character.ID != snowflake.ID(0) {
		return query.character, nil
	}
	return playercharacter.PlayerCharacter{}, errors.New("测试查询未配置角色")
}
