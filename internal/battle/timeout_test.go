package battle_test

import (
	"errors"
	"testing"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestBattleTimeoutResultUsesSurvivingMembersThenExactRemainingHPRatio 验证整场超时先按存活成员数、
// 再按精确总剩余生命比例裁定，完全相同则为 No Contest。
func TestBattleTimeoutResultUsesSurvivingMembersThenExactRemainingHPRatio(t *testing.T) {
	t.Parallel()

	initial, err := battle.CompileInitialState(startingSession(), initialFacts())
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	state, err := battleengine.NewState(initial)
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}

	t.Run("存活成员更多的一方获胜", func(t *testing.T) {
		summary := state.Summary()
		setCurrentHP(t, &summary, battleengine.SideTwo, 1, 0)
		setCurrentHP(t, &summary, battleengine.SideTwo, 3, 99)

		result, resultErr := battle.BattleTimeoutResult(initial, summary)
		if resultErr != nil {
			t.Fatalf("BattleTimeoutResult() error = %v", resultErr)
		}
		if result != (battle.Result{WinnerSide: battle.ParticipantSideOne, Reason: battle.TerminalReasonBattleTimeout}) {
			t.Fatalf("BattleTimeoutResult() = %+v，期望第一方按存活人数获胜", result)
		}
	})

	t.Run("存活人数相同时按剩余生命比例裁定", func(t *testing.T) {
		summary := state.Summary()
		setCurrentHP(t, &summary, battleengine.SideOne, 1, 25)
		setCurrentHP(t, &summary, battleengine.SideOne, 3, 24)
		setCurrentHP(t, &summary, battleengine.SideTwo, 1, 51)
		setCurrentHP(t, &summary, battleengine.SideTwo, 3, 49)

		result, resultErr := battle.BattleTimeoutResult(initial, summary)
		if resultErr != nil {
			t.Fatalf("BattleTimeoutResult() error = %v", resultErr)
		}
		if result != (battle.Result{WinnerSide: battle.ParticipantSideTwo, Reason: battle.TerminalReasonBattleTimeout}) {
			t.Fatalf("BattleTimeoutResult() = %+v，期望第二方按剩余生命比例获胜", result)
		}
	})

	t.Run("完全相同的剩余生命比例产生 No Contest", func(t *testing.T) {
		summary := state.Summary()
		setCurrentHP(t, &summary, battleengine.SideOne, 1, 50)
		setCurrentHP(t, &summary, battleengine.SideOne, 3, 50)
		setCurrentHP(t, &summary, battleengine.SideTwo, 1, 50)
		setCurrentHP(t, &summary, battleengine.SideTwo, 3, 50)

		result, resultErr := battle.BattleTimeoutResult(initial, summary)
		if resultErr != nil {
			t.Fatalf("BattleTimeoutResult() error = %v", resultErr)
		}
		if result != (battle.Result{Reason: battle.TerminalReasonNoContest}) {
			t.Fatalf("BattleTimeoutResult() = %+v，期望 No Contest", result)
		}
	})
}

// TestBattleTimeoutResultRejectsSummaryMissingFrozenMember 验证超时裁定不会把不完整的状态摘要静默当作零血量。
func TestBattleTimeoutResultRejectsSummaryMissingFrozenMember(t *testing.T) {
	t.Parallel()

	initial, err := battle.CompileInitialState(startingSession(), initialFacts())
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	state, err := battleengine.NewState(initial)
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	summary := state.Summary()
	summary.Members = summary.Members[1:]

	_, err = battle.BattleTimeoutResult(initial, summary)
	if !errors.Is(err, battle.ErrInvalidBattleResult) {
		t.Fatalf("BattleTimeoutResult() error = %v，期望 ErrInvalidBattleResult", err)
	}
}

func setCurrentHP(
	t *testing.T,
	summary *battleengine.StateSummary,
	side battleengine.Side,
	position battleengine.MemberPosition,
	currentHP uint32,
) {
	t.Helper()
	for index := range summary.Members {
		member := &summary.Members[index]
		if member.Side == side && member.MemberPosition == position {
			member.CurrentHP = currentHP
			return
		}
	}
	t.Fatalf("状态摘要中不存在阵营 %d 的成员 %d", side, position)
}
