package battle_test

import (
	"bytes"
	"context"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/team"
)

// TestChallengeBattleFreezesParticipantsAndTransitionsAfterBothPreviewSubmissions
// 验证接受邀请会创建双方已冻结的 Preview Battle，且只有双方都提交合法选择后才进入 starting。
func TestChallengeBattleFreezesParticipantsAndTransitionsAfterBothPreviewSubmissions(t *testing.T) {
	t.Parallel()

	createdAt := time.Date(2026, time.July, 30, 12, 0, 0, 0, time.UTC)
	challenge, err := battle.NewChallenge(context.Background(), validChallengeCommand(), snowflake.NewTestID, func() time.Time { return createdAt })
	if err != nil {
		t.Fatalf("NewChallenge() error = %v", err)
	}
	challenge, err = challenge.Accept(challenge.TargetPlayerCharacterID, createdAt.Add(time.Minute))
	if err != nil {
		t.Fatalf("Accept() error = %v", err)
	}
	targetTeam := team.Team{
		ID:                snowflake.MustParse("1048576004"),
		PlayerCharacterID: challenge.TargetPlayerCharacterID,
		Version:           4,
		Members:           challenge.ChallengerTeam.Members,
	}

	session, err := battle.NewChallengeBattle(context.Background(), battle.NewChallengeBattleCommand{
		Challenge:  challenge,
		TargetTeam: targetTeam,
		Format: battle.Format{
			RosterCount:               1,
			SelectCount:               1,
			ActiveParticipantsPerSide: 1,
			PreviewDuration:           time.Minute,
			BattleDuration:            30 * time.Minute,
		},
	}, snowflake.NewTestID, func() time.Time { return createdAt.Add(time.Minute) })
	if err != nil {
		t.Fatalf("NewChallengeBattle() error = %v", err)
	}
	if session.Mode != battle.BattleModePvP || session.SourceType != battle.BattleSourceChallenge || session.Status != battle.StatusPreview ||
		session.Version != 1 || len(session.Participants) != 2 || len(session.PreviewSubmissions) != 0 {
		t.Fatalf("NewChallengeBattle() = %+v", session)
	}
	if !session.PreviewDeadlineAt.Equal(createdAt.Add(2*time.Minute)) ||
		!session.BattleDeadlineAt.Equal(createdAt.Add(31*time.Minute)) {
		t.Fatalf("session deadlines = %s, %s", session.PreviewDeadlineAt, session.BattleDeadlineAt)
	}

	session, err = session.SubmitPreview(battle.PreviewSubmissionCommand{
		PlayerCharacterID: challenge.ChallengerPlayerCharacterID,
		MemberPositions:   []int32{1},
		ActivePositions:   []int32{1},
	}, createdAt.Add(time.Minute+10*time.Second))
	if err != nil {
		t.Fatalf("SubmitPreview() challenger error = %v", err)
	}
	if session.Status != battle.StatusPreview || session.Version != 2 || len(session.PreviewSubmissions) != 1 {
		t.Fatalf("first SubmitPreview() = %+v", session)
	}

	session, err = session.SubmitPreview(battle.PreviewSubmissionCommand{
		PlayerCharacterID: challenge.TargetPlayerCharacterID,
		MemberPositions:   []int32{1},
		ActivePositions:   []int32{1},
	}, createdAt.Add(time.Minute+20*time.Second))
	if err != nil {
		t.Fatalf("SubmitPreview() target error = %v", err)
	}
	if session.Status != battle.StatusRunning || session.Version != 3 || len(session.PreviewSubmissions) != 2 {
		t.Fatalf("second SubmitPreview() = %+v", session)
	}
}

// TestSessionRejectsInvalidPreviewAndPreservesPriorSnapshot 验证秘密 Preview 不能被越权、重复或
// 非法成员选择覆盖，且值对象转换不会反向修改已经保存的历史快照。
func TestSessionRejectsInvalidPreviewAndPreservesPriorSnapshot(t *testing.T) {
	t.Parallel()

	createdAt := time.Date(2026, time.July, 30, 12, 0, 0, 0, time.UTC)
	session, challenge := acceptedChallengeBattle(t, createdAt)

	_, err := session.SubmitPreview(battle.PreviewSubmissionCommand{
		PlayerCharacterID: snowflake.NewTestID(), MemberPositions: []int32{1}, ActivePositions: []int32{1},
	}, createdAt.Add(10*time.Second))
	if !errors.Is(err, battle.ErrInvalidBattle) {
		t.Fatalf("foreign SubmitPreview() error = %v, want ErrInvalidBattle", err)
	}
	_, err = session.SubmitPreview(battle.PreviewSubmissionCommand{
		PlayerCharacterID: challenge.ChallengerPlayerCharacterID, MemberPositions: []int32{2}, ActivePositions: []int32{2},
	}, createdAt.Add(10*time.Second))
	if !errors.Is(err, battle.ErrInvalidBattle) {
		t.Fatalf("unknown member SubmitPreview() error = %v, want ErrInvalidBattle", err)
	}

	first, err := session.SubmitPreview(battle.PreviewSubmissionCommand{
		PlayerCharacterID: challenge.ChallengerPlayerCharacterID, MemberPositions: []int32{1}, ActivePositions: []int32{1},
	}, createdAt.Add(10*time.Second))
	if err != nil {
		t.Fatalf("first SubmitPreview() error = %v", err)
	}
	_, err = first.SubmitPreview(battle.PreviewSubmissionCommand{
		PlayerCharacterID: challenge.ChallengerPlayerCharacterID, MemberPositions: []int32{1}, ActivePositions: []int32{1},
	}, createdAt.Add(20*time.Second))
	if !errors.Is(err, battle.ErrPreviewAlreadySubmitted) {
		t.Fatalf("duplicate SubmitPreview() error = %v, want ErrPreviewAlreadySubmitted", err)
	}
	first.PreviewSubmissions[0].MemberPositions[0] = 99
	if session.PreviewSubmissions != nil {
		t.Fatalf("source Session preview submissions were mutated: %+v", session.PreviewSubmissions)
	}

	_, err = first.SubmitPreview(battle.PreviewSubmissionCommand{
		PlayerCharacterID: challenge.TargetPlayerCharacterID, MemberPositions: []int32{1}, ActivePositions: []int32{1},
	}, createdAt.Add(2*time.Minute))
	if !errors.Is(err, battle.ErrPreviewExpired) {
		t.Fatalf("expired SubmitPreview() error = %v, want ErrPreviewExpired", err)
	}
}

// TestSessionAutoCompletesExpiredPreviewWithReplayableDeterministicTrace 验证到期补选不会依赖进程时钟或
// 未持久化随机源，并会记录足以审计相同结果的随机轨迹。
func TestSessionAutoCompletesExpiredPreviewWithReplayableDeterministicTrace(t *testing.T) {
	t.Parallel()

	createdAt := time.Date(2026, time.July, 30, 12, 0, 0, 0, time.UTC)
	session := acceptedSession(t, createdAt)
	session.Format = battle.Format{
		RosterCount: 3, SelectCount: 2, ActiveParticipantsPerSide: 1,
		PreviewDuration: time.Minute, BattleDuration: 30 * time.Minute,
	}
	for index := range session.Participants {
		session.Participants[index].Team.Members = []team.Member{{Position: 1}, {Position: 2}, {Position: 3}}
	}
	first, err := session.CompleteExpiredPreview(session.PreviewDeadlineAt)
	if err != nil {
		t.Fatalf("first CompleteExpiredPreview() error = %v", err)
	}
	second, err := session.CompleteExpiredPreview(session.PreviewDeadlineAt)
	if err != nil {
		t.Fatalf("second CompleteExpiredPreview() error = %v", err)
	}
	if first.Status != battle.StatusRunning || len(first.PreviewSubmissions) != 2 {
		t.Fatalf("first CompleteExpiredPreview() = %+v", first)
	}
	for index := range first.PreviewSubmissions {
		left := first.PreviewSubmissions[index]
		right := second.PreviewSubmissions[index]
		if len(left.MemberPositions) != 2 || len(left.ActivePositions) != 1 || len(left.RandomTrace) == 0 {
			t.Fatalf("自动 Preview 选择无效: %+v", left)
		}
		if !bytes.Equal(left.RandomTrace, right.RandomTrace) ||
			!samePositions(left.MemberPositions, right.MemberPositions) || !samePositions(left.ActivePositions, right.ActivePositions) {
			t.Fatalf("相同冻结输入产生了不同自动 Preview: first=%+v second=%+v", left, right)
		}
	}
}

// TestSessionLifecycleSeparatesCompletionAndInterruption 验证 Active 之外不能写入正常结果，
// 而超时与运行时故障始终以 interrupted 保存，避免污染正式胜负历史。
func TestSessionLifecycleSeparatesCompletionAndInterruption(t *testing.T) {
	t.Parallel()

	createdAt := time.Date(2026, time.July, 30, 12, 0, 0, 0, time.UTC)
	session, challenge := acceptedChallengeBattle(t, createdAt)
	_, err := session.Complete(battle.Result{
		WinnerSide: battle.ParticipantSideOne, Reason: battle.TerminalReasonBattleEnded,
	}, createdAt.Add(time.Second))
	if !errors.Is(err, battle.ErrBattleNotRunning) {
		t.Fatalf("Complete(preview) error = %v, want ErrBattleNotRunning", err)
	}

	session = submitBothPreviews(t, session, challenge, createdAt)
	active, err := session.Start(createdAt.Add(30 * time.Second))
	if err != nil || active.Status != battle.StatusRunning || active.StartedAt.IsZero() || active.Version != session.Version+1 {
		t.Fatalf("Start() = %+v, error = %v", active, err)
	}
	completed, err := active.Complete(battle.Result{
		WinnerSide: battle.ParticipantSideOne, Reason: battle.TerminalReasonBattleEnded,
	}, createdAt.Add(time.Minute))
	if err != nil || completed.Status != battle.StatusCompleted || completed.CompletedAt.IsZero() ||
		completed.TerminalReason != string(battle.TerminalReasonBattleEnded) {
		t.Fatalf("Complete() = %+v, error = %v", completed, err)
	}
	_, err = completed.Interrupt(battle.TerminalReasonRuntimeFailed, createdAt.Add(2*time.Minute))
	if !errors.Is(err, battle.ErrBattleTerminal) {
		t.Fatalf("Interrupt(completed) error = %v, want ErrBattleTerminal", err)
	}

	autoCompleted, err := acceptedSession(t, createdAt).CompleteExpiredPreview(createdAt.Add(2 * time.Minute))
	if err != nil || autoCompleted.Status != battle.StatusRunning || autoCompleted.TerminalReason != "" ||
		len(autoCompleted.PreviewSubmissions) != len(autoCompleted.Participants) {
		t.Fatalf("CompleteExpiredPreview() = %+v, error = %v", autoCompleted, err)
	}
	for _, preview := range autoCompleted.PreviewSubmissions {
		if len(preview.RandomTrace) == 0 {
			t.Fatalf("自动补选缺少随机轨迹: %+v", preview)
		}
	}
}

// TestBattleCancelOnlyBeforeRuntimeStart 验证取消只适用于尚未启动 Runtime 的 Battle，且不会写入胜负结果。
func TestBattleCancelOnlyBeforeRuntimeStart(t *testing.T) {
	t.Parallel()
	createdAt := time.Date(2026, 8, 12, 10, 0, 0, 0, time.UTC)
	pending := acceptedSession(t, createdAt)
	canceledAt := createdAt.Add(time.Minute)
	canceled, err := pending.Cancel(canceledAt)
	if err != nil {
		t.Fatalf("Cancel() error = %v", err)
	}
	if canceled.Status != battle.StatusCanceled || canceled.TerminalReason != string(battle.TerminalReasonCanceled) ||
		!canceled.CompletedAt.Equal(canceledAt) || len(canceled.Result) != 0 {
		t.Fatalf("Cancel() = %+v", canceled)
	}
	running := pending
	running.Status = battle.StatusRunning
	running.StartedAt = createdAt.Add(time.Second)
	if _, err := running.Cancel(canceledAt); !errors.Is(err, battle.ErrBattleNotPendingRuntime) {
		t.Fatalf("running Cancel() error = %v, want ErrBattleNotPendingRuntime", err)
	}
}

// TestTrainingBattleFreezesBotVersionAndAutomaticallyLocksBotPreview 验证练习赛只占用真人账号，
// 同时冻结 Bot 策略版本，并使用稳定成员位置生成不可变的 Bot Preview。
func TestTrainingBattleFreezesBotVersionAndAutomaticallyLocksBotPreview(t *testing.T) {
	t.Parallel()

	createdAt := time.Date(2026, time.July, 30, 12, 0, 0, 0, time.UTC)
	playerID := snowflake.MustParse("1048576008")
	playerTeam := team.Team{
		ID: snowflake.MustParse("1048576009"), PlayerCharacterID: playerID, Version: 1,
		Members: []team.Member{{Position: 2}, {Position: 1}},
	}
	botTeam := battle.TeamSnapshot{
		SourceTeamID: snowflake.MustParse("1048576010"), SourceTeamVersion: 1,
		Members: []team.Member{{Position: 2}, {Position: 1}},
	}
	session, err := battle.NewTrainingBattle(context.Background(), battle.NewTrainingBattleCommand{
		AccountID: snowflake.MustParse("1048576011"), PlayerCharacterID: playerID,
		DisplayName: "练习玩家", Team: playerTeam,
		BattleFormatID: snowflake.MustParse("1048576012"), BattleFormatSnapshot: []byte(`{"schemaVersion":1}`),
		Format: battle.Format{
			RosterCount: 2, SelectCount: 2, ActiveParticipantsPerSide: 1,
			PreviewDuration: time.Minute, BattleDuration: 30 * time.Minute,
		},
		Bot: battle.BotProfile{Code: "first-legal", StrategyVersion: 3, DisplayName: "练习 Bot", Team: botTeam, Definition: testMirrorBotDefinition("练习 Bot")},
	}, snowflake.NewTestID, func() time.Time { return createdAt })
	if err != nil || session.Mode != battle.BattleModePvE || session.SourceType != battle.BattleSourceTraining || len(session.PreviewSubmissions) != 1 ||
		session.PreviewSubmissions[0].Side != battle.ParticipantSideTwo ||
		session.PreviewSubmissions[0].MemberPositions[0] != 1 || session.Participants[1].BotStrategyVersion != 3 {
		t.Fatalf("NewTrainingBattle() = %+v, error = %v", session, err)
	}
	next, err := session.SubmitPreview(battle.PreviewSubmissionCommand{
		PlayerCharacterID: playerID, MemberPositions: []int32{1, 2}, ActivePositions: []int32{1},
	}, createdAt.Add(time.Second))
	if err != nil || next.Status != battle.StatusRunning || len(next.PreviewSubmissions) != 2 {
		t.Fatalf("SubmitPreview(player) = %+v, error = %v", next, err)
	}
}

func acceptedChallengeBattle(t *testing.T, createdAt time.Time) (battle.Battle, battle.Challenge) {
	t.Helper()
	challenge, err := battle.NewChallenge(context.Background(), validChallengeCommand(), snowflake.NewTestID, func() time.Time { return createdAt })
	if err != nil {
		t.Fatalf("NewChallenge() error = %v", err)
	}
	challenge, err = challenge.Accept(challenge.TargetPlayerCharacterID, createdAt.Add(time.Second))
	if err != nil {
		t.Fatalf("Accept() error = %v", err)
	}
	targetTeam := team.Team{
		ID: snowflake.MustParse("1048576004"), PlayerCharacterID: challenge.TargetPlayerCharacterID,
		Version: 4, Members: challenge.ChallengerTeam.Members,
	}
	session, err := battle.NewChallengeBattle(context.Background(), battle.NewChallengeBattleCommand{
		Challenge: challenge, TargetTeam: targetTeam,
		Format: battle.Format{
			RosterCount: 1, SelectCount: 1, ActiveParticipantsPerSide: 1,
			PreviewDuration: time.Minute, BattleDuration: 30 * time.Minute,
		},
	}, snowflake.NewTestID, func() time.Time { return createdAt.Add(time.Second) })
	if err != nil {
		t.Fatalf("NewChallengeBattle() error = %v", err)
	}
	return session, challenge
}

func acceptedSession(t *testing.T, createdAt time.Time) battle.Battle {
	t.Helper()
	session, _ := acceptedChallengeBattle(t, createdAt)
	return session
}

// samePositions 比较两个位置切片的长度与顺序，避免测试因切片底层数组不同产生误判。
func samePositions(left, right []int32) bool {
	if len(left) != len(right) {
		return false
	}
	for index := range left {
		if left[index] != right[index] {
			return false
		}
	}
	return true
}

func submitBothPreviews(t *testing.T, session battle.Battle, challenge battle.Challenge, createdAt time.Time) battle.Battle {
	t.Helper()
	var err error
	session, err = session.SubmitPreview(battle.PreviewSubmissionCommand{
		PlayerCharacterID: challenge.ChallengerPlayerCharacterID, MemberPositions: []int32{1}, ActivePositions: []int32{1},
	}, createdAt.Add(10*time.Second))
	if err != nil {
		t.Fatalf("SubmitPreview(challenger) error = %v", err)
	}
	session, err = session.SubmitPreview(battle.PreviewSubmissionCommand{
		PlayerCharacterID: challenge.TargetPlayerCharacterID, MemberPositions: []int32{1}, ActivePositions: []int32{1},
	}, createdAt.Add(20*time.Second))
	if err != nil {
		t.Fatalf("SubmitPreview(target) error = %v", err)
	}
	return session
}
