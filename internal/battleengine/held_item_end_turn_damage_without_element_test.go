package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemEndTurnDamageWithoutElement 验证成员当前不具备指定属性时，持有道具会在回合末造成固定比例间接伤害。
//
// 条件只读取当前 ElementIDs：双属性中只要包含指定属性便不触发，太晶化、形态变化和属性身份改变则会即时改变
// 结果。道具失去与间接伤害免疫都会阻止结算；规则不读取随机数，并以结构化事件记录实际扣除量和伤害后生命。
func TestHeldItemEndTurnDamageWithoutElement(t *testing.T) {
	t.Parallel()
	for _, test := range []struct {
		// name 是当前属性与道具条件分支的稳定中文描述。
		name string
		// elementIDs 是持有者在回合结算时的当前有效属性集合。
		elementIDs []Identifier
		// holdsItem 表示成员在回合末是否仍持有条件自伤道具。
		holdsItem bool
		// indirectImmune 表示成员是否具备间接伤害免疫。
		indirectImmune bool
		// wantHP 是完整回合结束后的期望生命值。
		wantHP uint32
		// wantEvents 是期望写入的条件道具自伤事件数量。
		wantEvents int
	}{
		{name: "不具备指定属性时按最大生命八分之一自伤", elementIDs: testIDs("water-element"), holdsItem: true, wantHP: 140, wantEvents: 1},
		{name: "双属性包含指定属性时不自伤", elementIDs: testIDs("water-element", "grass-element"), holdsItem: true, wantHP: 160, wantEvents: 0},
		{name: "单属性匹配时不自伤", elementIDs: testIDs("grass-element"), holdsItem: true, wantHP: 160, wantEvents: 0},
		{name: "失去道具后不自伤", elementIDs: testIDs("water-element"), holdsItem: false, wantHP: 160, wantEvents: 0},
		{name: "间接伤害免疫阻止条件自伤", elementIDs: testIDs("water-element"), holdsItem: true, indirectImmune: true, wantHP: 160, wantEvents: 0},
	} {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			result := resolveHeldItemEndTurnDamageWithoutElementTurn(t, test.elementIDs, test.holdsItem, test.indirectImmune)
			if result.holder.CurrentHP != test.wantHP || len(result.events) != test.wantEvents || len(result.trace) != 0 {
				t.Fatalf("条件道具回合末自伤结果 = holder:%+v events:%+v trace:%+v，期望 hp:%d events:%d", result.holder, result.events, result.trace, test.wantHP, test.wantEvents)
			}
			if test.wantEvents == 1 && (result.events[0].Amount != 20 || result.events[0].CurrentHP != test.wantHP || result.events[0].Denominator != 8) {
				t.Fatalf("条件道具回合末自伤事件 = %+v", result.events[0])
			}
			if test.wantEvents == 1 && !eventOccursBefore(result.allEvents, battleengine.EventKindHeldItemDamageApplied, battleengine.EventKindTurnEnded) {
				t.Fatalf("条件道具回合末自伤事件顺序 = %+v，期望 heldItemDamageApplied 位于 turnEnded 之前", result.allEvents)
			}
		})
	}
}

// TestHeldItemEndTurnDamageWithoutElementReadsTerastallizedElement 验证条件自伤读取太晶化后的当前有效属性。
//
// 成员初始不具备指定属性，本应受到道具自伤；太晶化在技能结算前把当前属性改写为指定属性后，同一回合末必须
// 停止该间接伤害。该规则不增加随机轨迹，且不会写入伪造的零伤害事件。
func TestHeldItemEndTurnDamageWithoutElementReadsTerastallizedElement(t *testing.T) {
	t.Parallel()
	holder := newMember(1, "held-item-element-damage-tera-holder", 160, 160)
	holder.Stats.Speed = 200
	holder.ElementIDs = testIDs("normal-element")
	holder.NaturalElementIDs = testIDs("normal-element")
	holder.TeraElementID = testID("grass-element")
	holder.ItemID = testID("held-item-element-damage-item")
	holder.HeldItemEndTurnDamageWithoutElementID = testID("grass-element")
	holder.HeldItemEndTurnDamageWithoutElementDenominator = 8
	holder.Skills[0] = heldItemEndTurnHealingForElementWaitSkill(holder.ElementIDs[0], "held-item-element-damage-tera-wait")
	opponent := newMember(1, "held-item-element-damage-tera-opponent", 160, 160)
	opponent.Skills[0] = heldItemEndTurnHealingForElementWaitSkill(opponent.ElementIDs[0], "held-item-element-damage-tera-opponent-wait")
	state := newTerastallizationState(t, holder, opponent)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Terastallize: true}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
		},
	}, mustRandom(t, 289))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	member, ok := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !ok || !member.Terastallized || len(member.ElementIDs) != 1 || member.ElementIDs[0] != testID("grass-element") || member.CurrentHP != 160 {
		t.Fatalf("太晶化后的属性条件自伤状态 = %+v", member)
	}
	for _, event := range result.Events {
		if _, isDamage := event.(battleengine.HeldItemDamageAppliedEvent); isDamage {
			t.Fatalf("太晶化匹配排除属性后不应产生条件道具自伤事件: %+v", result.Events)
		}
	}
	if len(result.RandomTrace) != 0 || !eventOccursBefore(result.Events, battleengine.EventKindParticipantTerastallized, battleengine.EventKindTurnEnded) {
		t.Fatalf("太晶化后的属性条件自伤事件或随机轨迹 = events:%+v trace:%+v", result.Events, result.RandomTrace)
	}
}

// heldItemEndTurnDamageWithoutElementResult 汇总条件自伤规则断言所需的权威状态、事件与随机轨迹。
type heldItemEndTurnDamageWithoutElementResult struct {
	// holder 是完整回合结束后道具持有者的冻结成员快照。
	holder battleengine.MemberSnapshot
	// events 只包含本规则产生的回合末道具自伤事件。
	events []battleengine.HeldItemDamageAppliedEvent
	// trace 是完整回合的随机轨迹；本规则不应消费任何随机数。
	trace []battleengine.RandomTraceEntry
	// allEvents 是完整事件流，用于断言回合末阶段的稳定顺序。
	allEvents []battleengine.Event
}

// resolveHeldItemEndTurnDamageWithoutElementTurn 构造双方无随机等待动作的单打回合，并提取条件道具自伤结果。
func resolveHeldItemEndTurnDamageWithoutElementTurn(
	t *testing.T,
	elementIDs []Identifier,
	holdsItem bool,
	indirectImmune bool,
) heldItemEndTurnDamageWithoutElementResult {
	t.Helper()
	holder := newMember(1, "held-item-element-damage-holder", 160, 160)
	holder.Stats.Speed = 200
	holder.ElementIDs = append([]Identifier(nil), elementIDs...)
	holder.NaturalElementIDs = append([]Identifier(nil), elementIDs...)
	holder.IndirectDamageImmunity = indirectImmune
	if holdsItem {
		holder.ItemID = testID("held-item-element-damage-item")
		holder.HeldItemEndTurnDamageWithoutElementID = testID("grass-element")
		holder.HeldItemEndTurnDamageWithoutElementDenominator = 8
	}
	holder.Skills[0] = heldItemEndTurnHealingForElementWaitSkill(holder.ElementIDs[0], "held-item-element-damage-wait")
	opponent := newMember(1, "held-item-element-damage-opponent", 160, 160)
	opponent.Skills[0] = heldItemEndTurnHealingForElementWaitSkill(opponent.ElementIDs[0], "held-item-element-damage-opponent-wait")
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "held-item-end-turn-damage-without-element", ActiveSlotsPerSide: 1, TeamSize: 1},
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
	}, mustRandom(t, 288))
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
	return heldItemEndTurnDamageWithoutElementResult{holder: finalHolder, events: events, trace: result.RandomTrace, allEvents: result.Events}
}
