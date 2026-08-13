package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemEndTurnHealingForElement 验证仅当前有效属性匹配的持有道具会在回合末回复生命。
//
// 属性判断直接读取成员当前 ElementIDs，因此太晶化、形态变化和道具属性身份等已经投影到该字段的运行态变化
// 都会自然影响触发条件。规则既不消费道具也不读取随机数；满生命、倒下、失去道具或属性不匹配时均不产生零
// 回复事件，保证重放能够从结构化事件准确识别实际生效的道具来源。
func TestHeldItemEndTurnHealingForElement(t *testing.T) {
	t.Parallel()
	for _, test := range []struct {
		// name 是当前属性与道具条件分支的稳定中文描述。
		name string
		// elementIDs 是持有者在回合结算时的当前有效属性集合。
		elementIDs []Identifier
		// holdsItem 表示成员在回合末是否仍持有该条件回复道具。
		holdsItem bool
		// wantHP 是完整回合结束后的期望生命值。
		wantHP uint32
		// wantEvents 是期望写入的条件道具回复事件数量。
		wantEvents int
	}{
		{name: "当前属性匹配时按最大生命十六分之一回复", elementIDs: testIDs("grass-element"), holdsItem: true, wantHP: 90, wantEvents: 1},
		{name: "双属性中任一匹配即可回复", elementIDs: testIDs("water-element", "grass-element"), holdsItem: true, wantHP: 90, wantEvents: 1},
		{name: "当前属性不匹配时不回复", elementIDs: testIDs("water-element"), holdsItem: true, wantHP: 80, wantEvents: 0},
		{name: "失去道具后即使属性匹配也不回复", elementIDs: testIDs("grass-element"), holdsItem: false, wantHP: 80, wantEvents: 0},
	} {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			result := resolveHeldItemEndTurnHealingForElementTurn(t, test.elementIDs, test.holdsItem)
			if result.holder.CurrentHP != test.wantHP || len(result.events) != test.wantEvents || len(result.trace) != 0 {
				t.Fatalf("条件道具回合末回复结果 = holder:%+v events:%+v trace:%+v，期望 hp:%d events:%d", result.holder, result.events, result.trace, test.wantHP, test.wantEvents)
			}
			if test.wantEvents == 1 && (result.events[0].Amount != 10 || result.events[0].CurrentHP != test.wantHP || result.events[0].Denominator != 16) {
				t.Fatalf("条件道具回合末回复事件 = %+v", result.events[0])
			}
			if test.wantEvents == 1 && !eventOccursBefore(result.allEvents, battleengine.EventKindHeldItemHealingApplied, battleengine.EventKindTurnEnded) {
				t.Fatalf("条件道具回合末回复事件顺序 = %+v，期望 heldItemHealingApplied 位于 turnEnded 之前", result.allEvents)
			}
		})
	}
}

// TestHeldItemEndTurnHealingForElementReadsTerastallizedElement 验证条件回复读取太晶化后的当前有效属性。
//
// 资料规则只冻结所需属性 Identifier，不能把初始自然属性作为回合末条件。太晶化在技能结算前改写 ElementIDs 后，
// 同一回合的道具回复必须据此触发，并保持道具规则本身不增加随机轨迹。
func TestHeldItemEndTurnHealingForElementReadsTerastallizedElement(t *testing.T) {
	t.Parallel()
	holder := newMember(1, "held-item-element-healing-tera-holder", 160, 80)
	holder.Stats.Speed = 200
	holder.ElementIDs = testIDs("normal-element")
	holder.NaturalElementIDs = testIDs("normal-element")
	holder.TeraElementID = testID("grass-element")
	holder.ItemID = testID("held-item-element-healing-item")
	holder.HeldItemEndTurnHealForElementID = testID("grass-element")
	holder.HeldItemEndTurnHealForElementDenominator = 16
	holder.Skills[0] = heldItemEndTurnHealingForElementWaitSkill(holder.ElementIDs[0], "held-item-element-healing-tera-wait")
	opponent := newMember(1, "held-item-element-healing-tera-opponent", 160, 160)
	opponent.Skills[0] = heldItemEndTurnHealingForElementWaitSkill(opponent.ElementIDs[0], "held-item-element-healing-tera-opponent-wait")
	state := newTerastallizationState(t, holder, opponent)
	teraAction := battleengine.Action{
		Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
		UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Terastallize: true},
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			teraAction,
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
		},
	}, mustRandom(t, 287))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	member, ok := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !ok || !member.Terastallized || len(member.ElementIDs) != 1 || member.ElementIDs[0] != testID("grass-element") || member.CurrentHP != 90 {
		t.Fatalf("太晶化后的属性条件回复状态 = %+v", member)
	}
	if len(result.RandomTrace) != 0 || !eventOccursBefore(result.Events, battleengine.EventKindParticipantTerastallized, battleengine.EventKindHeldItemHealingApplied) || !eventOccursBefore(result.Events, battleengine.EventKindHeldItemHealingApplied, battleengine.EventKindTurnEnded) {
		t.Fatalf("太晶化后的属性条件回复事件或随机轨迹 = events:%+v trace:%+v", result.Events, result.RandomTrace)
	}
}

// heldItemEndTurnHealingForElementResult 汇总条件回复规则断言所需的权威状态、事件与随机轨迹。
type heldItemEndTurnHealingForElementResult struct {
	// holder 是完整回合结束后道具持有者的冻结成员快照。
	holder battleengine.MemberSnapshot
	// events 只包含本规则产生的回合末道具回复事件。
	events []battleengine.HeldItemHealingAppliedEvent
	// trace 是完整回合的随机轨迹；本规则不应消费任何随机数。
	trace []battleengine.RandomTraceEntry
	// allEvents 是完整事件流，用于断言回合末阶段的稳定顺序。
	allEvents []battleengine.Event
}

// resolveHeldItemEndTurnHealingForElementTurn 构造双方无随机等待动作的单打回合，并提取条件道具回复结果。
func resolveHeldItemEndTurnHealingForElementTurn(
	t *testing.T,
	elementIDs []Identifier,
	holdsItem bool,
) heldItemEndTurnHealingForElementResult {
	t.Helper()
	holder := newMember(1, "held-item-element-healing-holder", 160, 80)
	holder.Stats.Speed = 200
	holder.ElementIDs = append([]Identifier(nil), elementIDs...)
	holder.NaturalElementIDs = append([]Identifier(nil), elementIDs...)
	if holdsItem {
		holder.ItemID = testID("held-item-element-healing-item")
		holder.HeldItemEndTurnHealForElementID = testID("grass-element")
		holder.HeldItemEndTurnHealForElementDenominator = 16
	}
	holder.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("held-item-element-healing-wait"), Name: "等待", ElementID: holder.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, CuresUserMajorStatus: true,
	}
	opponent := newMember(1, "held-item-element-healing-opponent", 160, 160)
	opponent.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("held-item-element-healing-opponent-wait"), Name: "等待", ElementID: opponent.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, CuresUserMajorStatus: true,
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "held-item-end-turn-healing-for-element", ActiveSlotsPerSide: 1, TeamSize: 1},
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
	}, mustRandom(t, 286))
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
	return heldItemEndTurnHealingForElementResult{holder: finalHolder, events: events, trace: result.RandomTrace, allEvents: result.Events}
}

// heldItemEndTurnHealingForElementWaitSkill 创建不会消耗随机数的自我等待技能，隔离回合末道具规则。
func heldItemEndTurnHealingForElementWaitSkill(elementID Identifier, skillID string) battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID(skillID), Name: "等待", ElementID: elementID,
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, CuresUserMajorStatus: true,
	}
}
