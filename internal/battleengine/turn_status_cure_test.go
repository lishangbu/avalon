package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnCuresOnlyActiveMembersOnUsersSide 验证同侧上场净化会清除使用者与同伴，
// 但绝不会把同一队伍的后备成员错误纳入范围。
func TestResolveTurnCuresOnlyActiveMembersOnUsersSide(t *testing.T) {
	t.Parallel()
	actor, ally, opponentOne, opponentTwo := targetScopeMembers()
	actor.Stats.Speed = 200
	actor.MajorStatus = battleengine.MajorStatusPoison
	actor.Skills[0].SkillID = testID("active-side-status-cure-skill")
	actor.Skills[0].DamageClass = battleengine.DamageClassStatus
	actor.Skills[0].Power = 0
	actor.Skills[0].TargetScope = battleengine.SkillTargetScopeSelf
	actor.Skills[0].CuresUserSideActiveMajorStatuses = true
	ally.MajorStatus = battleengine.MajorStatusBurn
	bench := newMember(3, "status-cure-bench", 500, 500)
	bench.MajorStatus = battleengine.MajorStatusSleep
	bench.SleepTurnsRemaining = 2
	opponentBench := newMember(3, "status-cure-opponent-bench", 500, 500)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "status-cure-double", ActiveSlotsPerSide: 2, TeamSize: 3},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{actor, ally, bench}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{opponentOne, opponentTwo, opponentBench}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 19)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, targetScopeTurn(
		targetScopeSkillAction(battleengine.SideOne, 1, 0, 0),
		targetScopeSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		targetScopeSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 1),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for _, slot := range []battleengine.SlotRef{
		{Side: battleengine.SideOne, Position: 1},
		{Side: battleengine.SideOne, Position: 2},
	} {
		member, exists := result.State.ActiveMember(slot)
		if !exists || member.MajorStatus != "" {
			t.Fatalf("场上成员 %+v 的主要异常 = %q，期望已清除", slot, member.MajorStatus)
		}
	}
	finalSnapshot := result.State.Snapshot()
	benchAfter := finalSnapshot.Sides[0].Members[2]
	if benchAfter.MajorStatus != battleengine.MajorStatusSleep || benchAfter.SleepTurnsRemaining != 2 {
		t.Fatalf("后备成员状态 = %+v，期望保留睡眠", benchAfter)
	}
}

// TestResolveTurnCuresEntireUsersSide 验证整队净化会包含后备成员，并为每个实际清除的主要异常保留事件。
func TestResolveTurnCuresEntireUsersSide(t *testing.T) {
	t.Parallel()
	actor, ally, opponentOne, opponentTwo := targetScopeMembers()
	actor.Stats.Speed = 200
	actor.MajorStatus = battleengine.MajorStatusPoison
	actor.Skills[0].SkillID = testID("whole-side-status-cure-skill")
	actor.Skills[0].DamageClass = battleengine.DamageClassStatus
	actor.Skills[0].Power = 0
	actor.Skills[0].TargetScope = battleengine.SkillTargetScopeSelf
	actor.Skills[0].CuresUserSideMajorStatuses = true
	ally.MajorStatus = battleengine.MajorStatusBurn
	bench := newMember(3, "whole-side-status-cure-bench", 500, 500)
	bench.MajorStatus = battleengine.MajorStatusBadPoison
	bench.BadPoisonCounter = 3
	opponentBench := newMember(3, "whole-side-status-cure-opponent-bench", 500, 500)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "whole-side-status-cure-double", ActiveSlotsPerSide: 2, TeamSize: 3},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{actor, ally, bench}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{opponentOne, opponentTwo, opponentBench}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 29)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, targetScopeTurn(
		targetScopeSkillAction(battleengine.SideOne, 1, 0, 0),
		targetScopeSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		targetScopeSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 1),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	finalSnapshot := result.State.Snapshot()
	for _, member := range finalSnapshot.Sides[0].Members {
		if member.MajorStatus != "" || member.BadPoisonCounter != 0 || member.SleepTurnsRemaining != 0 {
			t.Fatalf("同侧成员 %d 的主要异常快照 = %+v，期望全部清除", member.Position, member)
		}
	}
	clearedCount := 0
	for _, event := range result.Events {
		cleared, isCleared := event.(battleengine.MajorStatusClearedEvent)
		if isCleared && cleared.Target.Side == battleengine.SideOne {
			clearedCount++
		}
	}
	if clearedCount != 3 {
		t.Fatalf("同侧主要异常清除事件数量 = %d，期望 3", clearedCount)
	}
}
