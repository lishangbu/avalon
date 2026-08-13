package battle

import (
	"context"
	"errors"
	"path/filepath"
	"reflect"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestActorLocksBothSidesAndCommitsBeforePublishing 验证 Actor 不会在对方选择尚未锁定时泄露事件，
// 且只有 Turn Record 成功提交后才推进内存中的权威状态版本。
func TestActorLocksBothSidesAndCommitsBeforePublishing(t *testing.T) {
	t.Parallel()

	actor, replay, committer := newGoldenActor(t)
	first, err := actor.Submit(context.Background(), TurnSubmission{
		PlayerCharacterID:    actor.Battle().Participants[0].PlayerCharacterID,
		ExpectedStateVersion: 0,
		IdempotencyKey:       "side-one-turn-one",
		Actions:              replay.Turns[0].Command.Actions[:1],
	})
	if err != nil || !first.Locked || first.Resolved || len(first.Events) != 0 || first.StateVersion != 0 {
		t.Fatalf("first Submit() = %+v, error = %v", first, err)
	}
	replayedLock, err := actor.Submit(context.Background(), TurnSubmission{
		PlayerCharacterID:    actor.Battle().Participants[0].PlayerCharacterID,
		ExpectedStateVersion: 0,
		IdempotencyKey:       "side-one-turn-one",
		Actions:              replay.Turns[0].Command.Actions[:1],
	})
	if err != nil || !reflect.DeepEqual(replayedLock, first) {
		t.Fatalf("replayed locked Submit() = %+v, error = %v", replayedLock, err)
	}

	resolved, err := actor.Submit(context.Background(), TurnSubmission{
		PlayerCharacterID:    actor.Battle().Participants[1].PlayerCharacterID,
		ExpectedStateVersion: 0,
		IdempotencyKey:       "side-two-turn-one",
		Actions:              replay.Turns[0].Command.Actions[1:],
	})
	if err != nil || !resolved.Resolved || resolved.Locked || resolved.StateVersion != 1 ||
		resolved.State.TurnNumber != 1 || len(resolved.Events) == 0 || len(committer.records) != 1 {
		t.Fatalf("second Submit() = %+v, records=%+v, error = %v", resolved, committer.records, err)
	}
	if committer.records[0].StateVersion != 1 || committer.records[0].TurnNumber != 1 ||
		len(committer.records[0].RandomTrace) != 0 {
		t.Fatalf("stored TurnRecord = %+v", committer.records[0])
	}
	replayedResolution, err := actor.Submit(context.Background(), TurnSubmission{
		PlayerCharacterID:    actor.Battle().Participants[1].PlayerCharacterID,
		ExpectedStateVersion: 0,
		IdempotencyKey:       "side-two-turn-one",
		Actions:              replay.Turns[0].Command.Actions[1:],
	})
	if err != nil || !reflect.DeepEqual(replayedResolution, resolved) || len(committer.records) != 1 {
		t.Fatalf("replayed resolved Submit() = %+v, records=%d, error = %v", replayedResolution, len(committer.records), err)
	}
}

// TestActorLeavesCandidateUnpublishedWhenCommitFails 验证数据库事务失败后不会修改内存 State，
// 随后使用相同锁定选择重新提交时仍可得到同一候选结果。
func TestActorLeavesCandidateUnpublishedWhenCommitFails(t *testing.T) {
	t.Parallel()

	actor, replay, committer := newGoldenActor(t)
	committer.err = errors.New("模拟事务失败")
	for side := range 2 {
		_, err := actor.Submit(context.Background(), TurnSubmission{
			PlayerCharacterID:    actor.Battle().Participants[side].PlayerCharacterID,
			ExpectedStateVersion: 0,
			IdempotencyKey:       []string{"side-one", "side-two"}[side],
			Actions:              replay.Turns[0].Command.Actions[side : side+1],
		})
		if side == 1 && err == nil {
			t.Fatal("second Submit() error = nil, want persistence failure")
		}
	}
	if actor.StateVersion() != 0 || actor.Summary().TurnNumber != 0 || len(committer.records) != 0 {
		t.Fatalf("failed commit published state: version=%d summary=%+v records=%+v", actor.StateVersion(), actor.Summary(), committer.records)
	}
	committer.err = nil
	resolved, err := actor.Submit(context.Background(), TurnSubmission{
		PlayerCharacterID:    actor.Battle().Participants[1].PlayerCharacterID,
		ExpectedStateVersion: 0,
		IdempotencyKey:       "side-two",
		Actions:              replay.Turns[0].Command.Actions[1:],
	})
	if err != nil || !resolved.Resolved || actor.StateVersion() != 1 || len(committer.records) != 1 {
		t.Fatalf("retry Submit() = %+v, version=%d records=%+v, error = %v", resolved, actor.StateVersion(), committer.records, err)
	}
}

// TestRuntimeUsesFrozenBotStrategy 验证 Training Runtime 只根据冻结代码和版本选择 Bot，
// 并把服务端自动选择作为带版本摘要的 Turn Record 提交，而非伪造真人角色身份。
func TestRuntimeUsesFrozenBotStrategy(t *testing.T) {
	t.Parallel()

	actor, replay, committer := newGoldenActor(t)
	bot, err := NewFirstAvailableBot("first-available", 1)
	if err != nil {
		t.Fatalf("NewFirstAvailableBot() error = %v", err)
	}
	actor.session.Mode = BattleModePvE
	actor.session.SourceType = BattleSourceTraining
	actor.session.Participants[1] = Participant{
		Side: ParticipantSideTwo, DisplayName: "练习 Bot", IsBot: true,
		BotCode: "first-available", BotStrategyVersion: 1,
	}
	actor, err = NewRuntime(actor.session, actor.state, actor.random, committer, nil, actor.now, []BotStrategy{bot})
	if err != nil {
		t.Fatalf("NewRuntime() error = %v", err)
	}
	resolved, err := actor.Submit(context.Background(), TurnSubmission{
		PlayerCharacterID: actor.Battle().Participants[0].PlayerCharacterID, ExpectedStateVersion: 0,
		IdempotencyKey: "practice-player-turn-one", Actions: replay.Turns[0].Command.Actions[:1],
	})
	if err != nil || !resolved.Resolved || len(committer.records) != 1 || len(committer.records[0].Submissions) != 2 {
		t.Fatalf("Training Submit() = %+v, records=%+v, error = %v", resolved, committer.records, err)
	}
	botSubmission := committer.records[0].Submissions[1]
	if !botSubmission.IsBot || botSubmission.PlayerCharacterID != snowflake.ID(0) || botSubmission.BotCode != "first-available" ||
		botSubmission.BotStrategyVersion != 1 {
		t.Fatalf("Bot TurnSubmissionRecord = %+v", botSubmission)
	}
}

type recordingTurnCommitter struct {
	records       []TurnRecord
	err           error
	panicOnCommit bool
}

func (committer *recordingTurnCommitter) CommitTurn(_ context.Context, record TurnRecord) error {
	if committer.panicOnCommit {
		panic("模拟 Runtime 提交 panic")
	}
	if committer.err != nil {
		return committer.err
	}
	committer.records = append(committer.records, record)
	return nil
}

func newGoldenActor(t *testing.T) (*Runtime, battleengine.GoldenReplay, *recordingTurnCommitter) {
	t.Helper()
	replay, err := battleengine.LoadGoldenReplay(filepath.Join("..", "battleengine", "testdata", "golden", "major-status-turn.v1.json"))
	if err != nil {
		t.Fatalf("LoadGoldenReplay() error = %v", err)
	}
	state, err := battleengine.NewState(replay.InitialState)
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	startedAt := time.Date(2026, time.July, 30, 12, 0, 0, 0, time.UTC)
	session := Battle{
		ID: snowflake.MustParse("1048576005"), Status: StatusRunning,
		StartedAt: startedAt, BattleDeadlineAt: startedAt.Add(time.Hour), Version: 4,
		Participants: []Participant{
			{Side: ParticipantSideOne, PlayerCharacterID: snowflake.MustParse("1048576006"), DisplayName: "甲"},
			{Side: ParticipantSideTwo, PlayerCharacterID: snowflake.MustParse("1048576007"), DisplayName: "乙"},
		},
	}
	committer := &recordingTurnCommitter{}
	actor, err := NewRuntime(session, state, random, committer, nil, func() time.Time { return startedAt.Add(time.Second) }, nil)
	if err != nil {
		t.Fatalf("NewRuntime() error = %v", err)
	}
	return actor, replay, committer
}
