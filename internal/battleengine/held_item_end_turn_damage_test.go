package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemEndTurnDamage 验证持有道具在回合末按最大生命固定比例对当前持有者造成间接伤害。
//
// 该规则不要求成员在本回合造成伤害或发生接触，不消费道具也不读取随机数；生命损失受当前生命封顶，并由
// IndirectDamageImmunity 阻止。结构化事件必须保存实际伤害、伤害后的生命和分母，供回放区分道具自伤来源。
func TestHeldItemEndTurnDamage(t *testing.T) {
	t.Parallel()
	for _, test := range []struct {
		// name 是当前规则分支的稳定描述。
		name string
		// currentHP 是持有者回合开始时的当前生命。
		currentHP uint32
		// holdsItem 表示成员在回合末是否仍持有自伤道具。
		holdsItem bool
		// indirectImmune 表示成员是否具备间接伤害免疫。
		indirectImmune bool
		// wantHP 是完整回合后的期望当前生命。
		wantHP uint32
		// wantEvents 是期望的道具自伤事件数。
		wantEvents int
	}{
		{name: "按最大生命八分之一自伤", currentHP: 160, holdsItem: true, wantHP: 140, wantEvents: 1},
		{name: "当前生命不足伤害量时倒下", currentHP: 10, holdsItem: true, wantHP: 0, wantEvents: 1},
		{name: "失去道具后不再自伤", currentHP: 160, holdsItem: false, wantHP: 160, wantEvents: 0},
		{name: "间接伤害免疫阻止道具自伤", currentHP: 160, holdsItem: true, indirectImmune: true, wantHP: 160, wantEvents: 0},
	} {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			result := resolveHeldItemEndTurnDamageTurn(t, test.currentHP, test.holdsItem, test.indirectImmune)
			if result.holder.CurrentHP != test.wantHP || len(result.events) != test.wantEvents || len(result.trace) != 0 {
				t.Fatalf("回合末自伤结果 = holder:%+v events:%+v trace:%+v，期望 hp:%d events:%d", result.holder, result.events, result.trace, test.wantHP, test.wantEvents)
			}
			if test.wantEvents == 1 && (result.events[0].Amount == 0 || result.events[0].CurrentHP != test.wantHP || result.events[0].Denominator != 8) {
				t.Fatalf("回合末自伤事件 = %+v", result.events[0])
			}
			if test.wantEvents == 1 && !eventOccursBefore(result.allEvents, battleengine.EventKindHeldItemDamageApplied, battleengine.EventKindTurnEnded) {
				t.Fatalf("回合末自伤事件顺序 = %+v，期望 heldItemDamageApplied 位于 turnEnded 之前", result.allEvents)
			}
			if test.wantHP == 0 && !eventOccursBefore(result.allEvents, battleengine.EventKindHeldItemDamageApplied, battleengine.EventKindParticipantFainted) {
				t.Fatalf("回合末自伤倒下顺序 = %+v，期望 heldItemDamageApplied 位于 participantFainted 之前", result.allEvents)
			}
		})
	}
}

// heldItemEndTurnDamageResult 汇集回合末道具自伤断言所需的权威状态、事件与随机轨迹。
type heldItemEndTurnDamageResult struct {
	// holder 是完整回合结束后道具持有者的冻结成员快照。
	holder battleengine.MemberSnapshot
	// events 只包含本规则产生的回合末道具自伤事件。
	events []battleengine.HeldItemDamageAppliedEvent
	// trace 是本回合的全部随机轨迹；本规则不应消费任何随机值。
	trace []battleengine.RandomTraceEntry
	// allEvents 是完整事件流，用于断言道具自伤、倒下和回合结束的稳定顺序。
	allEvents []battleengine.Event
}

// resolveHeldItemEndTurnDamageTurn 构造双方均提交无伤害净化技能的单打回合，并提取道具自伤结果。
func resolveHeldItemEndTurnDamageTurn(
	t *testing.T,
	currentHP uint32,
	holdsItem bool,
	indirectImmune bool,
) heldItemEndTurnDamageResult {
	t.Helper()
	holder := newMember(1, "held-item-damage-holder", 160, currentHP)
	holder.Stats.Speed = 200
	holder.IndirectDamageImmunity = indirectImmune
	if holdsItem {
		holder.ItemID = testID("held-item-damage-item")
		holder.HeldItemEndTurnDamageDenominator = 8
	}
	holder.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("held-item-damage-wait"), Name: "等待", ElementID: holder.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, CuresUserMajorStatus: true,
	}
	opponent := newMember(1, "held-item-damage-opponent", 160, 160)
	opponent.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("held-item-damage-opponent-wait"), Name: "等待", ElementID: opponent.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, CuresUserMajorStatus: true,
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "held-item-end-turn-damage", ActiveSlotsPerSide: 1, TeamSize: 1},
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
	}, mustRandom(t, 285))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	finalHolder, ok := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !ok {
		t.Fatal("道具持有者在回合结束后不在场")
	}
	var events []battleengine.HeldItemDamageAppliedEvent
	for _, event := range result.Events {
		if damage, ok := event.(battleengine.HeldItemDamageAppliedEvent); ok {
			events = append(events, damage)
		}
	}
	return heldItemEndTurnDamageResult{holder: finalHolder, events: events, trace: result.RandomTrace, allEvents: result.Events}
}
