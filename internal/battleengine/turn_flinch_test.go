package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnAppliesFlinchBeforeLaterAction 验证较快成员的命中技能会把畏缩写入目标的当前回合，
// 使目标在宣告技能和消费 PP 前被阻止行动；畏缩不占用主要异常状态。
func TestResolveTurnAppliesFlinchBeforeLaterAction(t *testing.T) {
	t.Parallel()
	state := flinchState(t, 100)
	random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("flinch-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("flinch-skill").String(), Value: 15},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, flinchTurn(), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	flinchIndex, preventedIndex := -1, -1
	for index, event := range result.Events {
		switch value := event.(type) {
		case battleengine.FlinchAppliedEvent:
			if value.SkillID == testID("flinch-skill") && value.ChancePercent == 100 {
				flinchIndex = index
			}
		case battleengine.SkillPreventedEvent:
			if value.Actor.Side == battleengine.SideTwo && value.Reason == battleengine.SkillPreventionReasonFlinch {
				preventedIndex = index
			}
		}
	}
	if flinchIndex < 0 || preventedIndex <= flinchIndex {
		t.Fatalf("畏缩事件顺序 = %+v", result.Events)
	}
	target, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !exists || target.MajorStatus != "" || target.FlinchedTurn != 1 {
		t.Fatalf("畏缩目标状态 = %+v，期望仅写入第 1 回合畏缩", target)
	}
	if len(result.RandomTrace) != 2 {
		t.Fatalf("100%% 畏缩不应额外消耗随机数: %+v", result.RandomTrace)
	}
}

// TestResolveTurnRecordsFailedFlinchChance 验证非必定畏缩使用独立概率随机接点；失败后目标仍会照常
// 宣告自身技能，且不会残留当前回合的畏缩状态。
func TestResolveTurnRecordsFailedFlinchChance(t *testing.T) {
	t.Parallel()
	state := flinchState(t, 30)
	random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("flinch-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("flinch-skill").String(), Value: 15},
		{Sequence: 3, Bound: 100, Reason: "flinch chance for " + testID("flinch-skill").String(), Value: 30},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, flinchTurn(), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if flinchEventExists(result.Events) {
		t.Fatalf("失败的畏缩判定不应写入畏缩事件: %+v", result.Events)
	}
	if !flinchSkillUsedBy(result.Events, battleengine.SideTwo) {
		t.Fatalf("畏缩失败后目标应正常行动: %+v", result.Events)
	}
	target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if target.FlinchedTurn != 0 {
		t.Fatalf("畏缩失败后状态 = %d，期望 0", target.FlinchedTurn)
	}
	if len(result.RandomTrace) != 3 || result.RandomTrace[2].Bound != 100 {
		t.Fatalf("畏缩概率随机轨迹 = %+v", result.RandomTrace)
	}
}

// flinchState 创建速度领先的畏缩技能使用者和无随机状态技能目标。
func flinchState(t *testing.T, flinchChance uint8) battleengine.State {
	t.Helper()
	left := newMember(1, "flinch-user", 200, 200)
	left.Stats.Speed = 200
	left.Skills[0].SkillID = testID("flinch-skill")
	left.Skills[0].FlinchChancePercent = flinchChance
	right := newMember(1, "flinch-target", 200, 200)
	right.Stats.Speed = 10
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 0,
	}}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "flinch", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// flinchTurn 返回两名上场成员的第一回合技能行动。
func flinchTurn() battleengine.TurnCommand {
	return battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			{
				Kind:  battleengine.ActionKindUseSkill,
				Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{
					SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				},
			},
			{
				Kind:  battleengine.ActionKindUseSkill,
				Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				UseSkill: &battleengine.UseSkillAction{
					SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				},
			},
		},
	}
}

// flinchEventExists 报告事件流是否已经记录目标畏缩。
func flinchEventExists(events []battleengine.Event) bool {
	for _, event := range events {
		if _, ok := event.(battleengine.FlinchAppliedEvent); ok {
			return true
		}
	}
	return false
}

// flinchSkillUsedBy 报告指定阵营是否实际宣告了技能，用于区分畏缩阻止与其他失败原因。
func flinchSkillUsedBy(events []battleengine.Event, side battleengine.Side) bool {
	for _, event := range events {
		used, ok := event.(battleengine.SkillUsedEvent)
		if ok && used.Actor.Side == side {
			return true
		}
	}
	return false
}
