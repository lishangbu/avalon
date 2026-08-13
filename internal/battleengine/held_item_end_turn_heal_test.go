package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemEndTurnHealing 验证持有道具在环境回复阶段之后，按持有者最大生命的固定比例执行回合末回复。
//
// 该道具不消费、不读取随机数，也不回复已满生命、倒下或已失去道具的成员；结构化事件和最终状态快照必须给出
// 同一个实际回复量，避免回放只能从生命差猜测道具效果。
func TestHeldItemEndTurnHealing(t *testing.T) {
	t.Parallel()
	for _, test := range []struct {
		// name 是当前边界的稳定描述。
		name string
		// currentHP 是持有者回合开始时的生命值。
		currentHP uint32
		// holdsItem 表示持有者是否仍保有回复道具。
		holdsItem bool
		// wantHP 是完整回合后的期望生命值。
		wantHP uint32
		// wantEvents 是期望的道具回复事件数。
		wantEvents int
	}{
		{name: "按最大生命十六分之一回复", currentHP: 80, holdsItem: true, wantHP: 90, wantEvents: 1},
		{name: "接近满生命时按缺失生命封顶", currentHP: 155, holdsItem: true, wantHP: 160, wantEvents: 1},
		{name: "满生命不产生零回复事件", currentHP: 160, holdsItem: true, wantHP: 160, wantEvents: 0},
		{name: "失去道具后不再回复", currentHP: 80, holdsItem: false, wantHP: 80, wantEvents: 0},
	} {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			result := resolveHeldItemEndTurnHealingTurn(t, test.currentHP, test.holdsItem)
			if result.holder.CurrentHP != test.wantHP || len(result.events) != test.wantEvents || len(result.trace) != 0 {
				t.Fatalf("回合末回复结果 = holder:%+v events:%+v trace:%+v，期望 hp:%d events:%d", result.holder, result.events, result.trace, test.wantHP, test.wantEvents)
			}
			if test.wantEvents == 1 && (result.events[0].Amount == 0 || result.events[0].CurrentHP != test.wantHP || result.events[0].Denominator != 16) {
				t.Fatalf("回合末回复事件 = %+v", result.events[0])
			}
			if test.wantEvents == 1 && !eventOccursBefore(result.allEvents, battleengine.EventKindHeldItemHealingApplied, battleengine.EventKindTurnEnded) {
				t.Fatalf("回合末回复事件顺序 = %+v，期望 heldItemHealingApplied 位于 turnEnded 之前", result.allEvents)
			}
		})
	}
}

// heldItemEndTurnHealingResult 汇集回合末道具回复断言所需的权威状态、事件与随机轨迹。
type heldItemEndTurnHealingResult struct {
	// holder 是完整回合结束后道具持有者的冻结成员快照。
	holder battleengine.MemberSnapshot
	// events 只包含本规则产生的回合末道具回复事件。
	events []battleengine.HeldItemHealingAppliedEvent
	// trace 是本回合的全部随机轨迹；本规则不应消费任何随机值。
	trace []battleengine.RandomTraceEntry
	// allEvents 是完整事件流，用于断言回合末阶段的稳定事件顺序。
	allEvents []battleengine.Event
}

// resolveHeldItemEndTurnHealingTurn 构造双方均提交无随机等待动作的单打回合，并提取道具回复结果。
func resolveHeldItemEndTurnHealingTurn(t *testing.T, currentHP uint32, holdsItem bool) heldItemEndTurnHealingResult {
	t.Helper()
	holder := newMember(1, "held-item-healing-holder", 160, currentHP)
	holder.Stats.Speed = 200
	if holdsItem {
		holder.ItemID = testID("held-item-healing-item")
		holder.HeldItemEndTurnHealDenominator = 16
	}
	holder.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("held-item-healing-wait"), Name: "等待", ElementID: holder.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, CuresUserMajorStatus: true,
	}
	opponent := newMember(1, "held-item-healing-opponent", 160, 160)
	opponent.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("held-item-healing-opponent-wait"), Name: "等待", ElementID: opponent.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, CuresUserMajorStatus: true,
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "held-item-end-turn-healing", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{holder}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
		},
	}, mustRandom(t, 284))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	finalHolder, ok := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !ok {
		t.Fatal("道具持有者在回合结束后不在场")
	}
	var events []battleengine.HeldItemHealingAppliedEvent
	for _, event := range result.Events {
		if healing, ok := event.(battleengine.HeldItemHealingAppliedEvent); ok {
			events = append(events, healing)
		}
	}
	return heldItemEndTurnHealingResult{holder: finalHolder, events: events, trace: result.RandomTrace, allEvents: result.Events}
}

// eventOccursBefore 报告完整事件流中前一种稳定事件是否位于后一种稳定事件之前。
func eventOccursBefore(events []battleengine.Event, first battleengine.EventKind, second battleengine.EventKind) bool {
	firstIndex, secondIndex := -1, -1
	for index, event := range events {
		if event.Kind() == first && firstIndex < 0 {
			firstIndex = index
		}
		if event.Kind() == second && secondIndex < 0 {
			secondIndex = index
		}
	}
	return firstIndex >= 0 && secondIndex >= 0 && firstIndex < secondIndex
}
