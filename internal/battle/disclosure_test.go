package battle

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestParticipantViewForHidesOpponentReserveAndPP 验证披露账本投影不包含对手后备成员或任何对手 PP。
func TestParticipantViewForHidesOpponentReserveAndPP(t *testing.T) {
	t.Parallel()
	view, err := ParticipantViewFor(battleengine.StateSummary{
		TurnNumber: 4,
		Members: []battleengine.MemberStateSummary{
			{Side: battleengine.SideOne, MemberPosition: 1, SlotPosition: 1, CurrentHP: 50, RemainingPP: []uint8{8, 6}},
			{Side: battleengine.SideOne, MemberPosition: 2, CurrentHP: 100, RemainingPP: []uint8{12}},
			{Side: battleengine.SideTwo, MemberPosition: 1, SlotPosition: 1, CurrentHP: 42, RemainingPP: []uint8{4, 3}},
			{Side: battleengine.SideTwo, MemberPosition: 2, CurrentHP: 100, RemainingPP: []uint8{9}},
		},
	}, ParticipantSideOne, 7)
	if err != nil {
		t.Fatalf("ParticipantViewFor() error = %v", err)
	}
	if len(view.Members) != 3 {
		t.Fatalf("可见成员数量 = %d，期望己方两名和对手当前场上一名", len(view.Members))
	}
	for _, member := range view.Members {
		if member.Side == ParticipantSideTwo && len(member.RemainingPP) != 0 {
			t.Fatalf("对手成员 %+v 不应披露剩余 PP", member)
		}
		if member.Side == ParticipantSideTwo && member.MemberPosition == 2 {
			t.Fatalf("对手后备成员 %+v 不应出现在披露视图", member)
		}
	}
}
