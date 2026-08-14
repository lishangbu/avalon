package persistence

import (
	"errors"
	"testing"

	battle "github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestSessionResultFromEnginePreservesWinnerAndDrawSemantics 验证引擎终局向持久化 Battle 终局转换时，
// 不会把双重击倒或最大回合数错误写成任一方胜利。
func TestSessionResultFromEnginePreservesWinnerAndDrawSemantics(t *testing.T) {
	tests := []struct {
		name   string
		input  *battleengine.BattleResult
		winner battle.ParticipantSide
		reason battle.TerminalReason
	}{
		{
			name: "一方全员失去战斗能力", input: &battleengine.BattleResult{
				WinningSide: battleengine.SideOne, Reason: battleengine.BattleResultReasonAllMembersFainted,
			}, winner: battle.ParticipantSideOne, reason: battle.TerminalReasonBattleEnded,
		},
		{
			name: "双方同时失去战斗能力", input: &battleengine.BattleResult{
				Reason: battleengine.BattleResultReasonAllMembersFainted,
			}, reason: battle.TerminalReasonDraw,
		},
		{
			name: "达到赛制最大回合", input: &battleengine.BattleResult{
				Reason: battleengine.BattleResultReasonMaxTurnsReached,
			}, reason: battle.TerminalReasonDraw,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			result, err := battleResultFromEngine(test.input)
			if err != nil {
				t.Fatalf("battleResultFromEngine() error = %v", err)
			}
			if result.WinnerSide != test.winner || result.Reason != test.reason {
				t.Fatalf("battleResultFromEngine() = %+v, want winner %d reason %q", result, test.winner, test.reason)
			}
		})
	}
	if _, err := battleResultFromEngine(&battleengine.BattleResult{
		WinningSide: battleengine.SideOne, Reason: battleengine.BattleResultReasonMaxTurnsReached,
	}); !errors.Is(err, battle.ErrInvalidBattleResult) {
		t.Fatalf("invalid max turn result error = %v, want ErrInvalidBattleResult", err)
	}
}
