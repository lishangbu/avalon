package battle_test

import (
	"context"
	"encoding/json"
	"errors"
	"testing"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/team"
)

// TestNewChallengeFreezesTeamAndLifetime 验证邀请保存独立 Team 快照和固定有效期。
func TestNewChallengeFreezesTeamAndLifetime(t *testing.T) {
	t.Parallel()
	createdAt := time.Date(2026, time.July, 30, 10, 0, 0, 0, time.UTC)
	command := validChallengeCommand()
	challenge, err := battle.NewChallenge(context.Background(), command, snowflake.NewTestID, func() time.Time { return createdAt })
	if err != nil {
		t.Fatalf("NewChallenge() error = %v", err)
	}
	if challenge.Status != battle.ChallengePending || challenge.Version != 1 ||
		!challenge.ExpiresAt.Equal(createdAt.Add(5*time.Minute)) {
		t.Fatalf("NewChallenge() = %+v", challenge)
	}
	command.ChallengerTeam.Members[0].Skills[0].SkillID = snowflake.NewTestID()
	if challenge.ChallengerTeam.Members[0].Skills[0].SkillID == command.ChallengerTeam.Members[0].Skills[0].SkillID {
		t.Fatal("Challenge Team 快照被后续可变 Team 修改")
	}
}

// TestChallengeAcceptRequiresRecipientAndUnexpired 验证只有目标角色可在有效期内接受邀请。
func TestChallengeAcceptRequiresRecipientAndUnexpired(t *testing.T) {
	t.Parallel()
	createdAt := time.Date(2026, time.July, 30, 10, 0, 0, 0, time.UTC)
	challenge, err := battle.NewChallenge(context.Background(), validChallengeCommand(), snowflake.NewTestID, func() time.Time { return createdAt })
	if err != nil {
		t.Fatalf("NewChallenge() error = %v", err)
	}
	if _, acceptErr := challenge.Accept(snowflake.NewTestID(), createdAt.Add(time.Minute)); !errors.Is(acceptErr, battle.ErrChallengeRecipientMismatch) {
		t.Fatalf("Accept() error = %v, want ErrChallengeRecipientMismatch", acceptErr)
	}
	accepted, err := challenge.Accept(challenge.TargetPlayerCharacterID, createdAt.Add(time.Minute))
	if err != nil || accepted.Status != battle.ChallengeAccepted || accepted.TerminalReason != "accepted" || accepted.Version != 2 {
		t.Fatalf("Accept() = %+v, error = %v", accepted, err)
	}
	if _, lateErr := challenge.Accept(challenge.TargetPlayerCharacterID, challenge.ExpiresAt); !errors.Is(lateErr, battle.ErrChallengeExpired) {
		t.Fatalf("late Accept() error = %v, want ErrChallengeExpired", lateErr)
	}
}

// TestChallengeWithdrawRequiresChallenger 验证只有发起方可以在到期前撤回待处理邀请。
func TestChallengeWithdrawRequiresChallenger(t *testing.T) {
	t.Parallel()
	createdAt := time.Date(2026, time.July, 30, 0, 0, 0, 0, time.UTC)
	challenge, err := battle.NewChallenge(context.Background(), validChallengeCommand(), snowflake.NewTestID, func() time.Time { return createdAt })
	if err != nil {
		t.Fatalf("NewChallenge() error = %v", err)
	}
	if _, withdrawErr := challenge.Withdraw(snowflake.NewTestID(), createdAt.Add(time.Minute)); !errors.Is(withdrawErr, battle.ErrChallengeRecipientMismatch) {
		t.Fatalf("Withdraw() error = %v, want ErrChallengeRecipientMismatch", withdrawErr)
	}
	withdrawn, err := challenge.Withdraw(challenge.ChallengerPlayerCharacterID, createdAt.Add(time.Minute))
	if err != nil || withdrawn.Status != battle.ChallengeWithdrawn || withdrawn.TerminalReason != "withdrawn" || withdrawn.Version != 2 {
		t.Fatalf("Withdraw() = %+v, error = %v", withdrawn, err)
	}
}

// TestChallengeExpireOnlyAfterDeadline 验证过期转换不会提前结束仍可接受的邀请。
func TestChallengeExpireOnlyAfterDeadline(t *testing.T) {
	t.Parallel()
	createdAt := time.Date(2026, time.July, 30, 10, 0, 0, 0, time.UTC)
	challenge, err := battle.NewChallenge(context.Background(), validChallengeCommand(), snowflake.NewTestID, func() time.Time { return createdAt })
	if err != nil {
		t.Fatalf("NewChallenge() error = %v", err)
	}
	if _, expireErr := challenge.Expire(challenge.ExpiresAt.Add(-time.Nanosecond)); !errors.Is(expireErr, battle.ErrChallengeExpired) {
		t.Fatalf("Expire() error = %v, want ErrChallengeExpired", expireErr)
	}
	expired, err := challenge.Expire(challenge.ExpiresAt)
	if err != nil || expired.Status != battle.ChallengeExpired || expired.TerminalReason != "expired" {
		t.Fatalf("Expire() = %+v, error = %v", expired, err)
	}
}

// validChallengeCommand 返回可以进入领域生命周期的最小冻结邀请事实。
func validChallengeCommand() battle.CreateChallengeCommand {
	challengerAccountID, targetAccountID := snowflake.NewTestID(), snowflake.NewTestID()
	challengerCharacterID, targetCharacterID := snowflake.NewTestID(), snowflake.NewTestID()
	teamID, creatureID, abilityID, elementID, skillID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	return battle.CreateChallengeCommand{
		ChallengerAccountID:         challengerAccountID,
		ChallengerPlayerCharacterID: challengerCharacterID,
		ChallengerDisplayName:       "挑战者",
		ChallengerTeam: team.Team{ID: teamID, Version: 3, Members: []team.Member{{
			Position: 1, CreatureID: creatureID, AbilityID: abilityID, TeraElementID: elementID,
			Skills: []team.MemberSkill{{Position: 1, SkillID: skillID}},
		}}},
		TargetAccountID:         targetAccountID,
		TargetPlayerCharacterID: targetCharacterID,
		TargetDisplayName:       "接收者",
		BattleFormatID:          snowflake.NewTestID(),
		BattleFormatSnapshot:    json.RawMessage(`{"id":"standard-single","rosterCount":6}`),
	}
}
