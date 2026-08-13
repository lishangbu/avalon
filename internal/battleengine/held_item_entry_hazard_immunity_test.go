package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemEntryHazardImmunity 验证入场危害免疫道具只跳过持有者换入时的危害伤害、异常和能力阶级变化。
// 道具不会移除侧状态，也不会保护未持有道具的同侧其它成员；失去道具后必须恢复通常结算。
func TestHeldItemEntryHazardImmunity(t *testing.T) {
	t.Parallel()
	active := newMember(1, "hazard-item-active", 160, 160)
	incoming := newMember(2, "hazard-item-immune", 160, 160)
	incoming.ItemID = testID("entry-hazard-immunity-item")
	incoming.HeldItemEntryHazardImmunity = true
	opponent := newMember(1, "hazard-item-opponent", 160, 160)
	// 使用无伤害的自我范围变化技能，隔离换入危害结算，避免对手普通攻击污染生命值断言。
	opponent.Skills[0] = fieldSpeedOrderSkill(1, "危害测试等待", 0)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "entry-hazard-immunity", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"rock": testID("rock-element")}},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{active, incoming}, Conditions: battleengine.SideConditionSnapshot{StealthRock: true, SpikesLayers: 3, ToxicSpikesLayers: 2, StickyWeb: true}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{
		{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	}}, mustRandom(t, 541))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	member, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if member.CurrentHP != 160 || member.MajorStatus != "" || member.StatStages[battleengine.StatSpeed] != 0 {
		t.Fatalf("免疫道具换入结果 = %+v", member)
	}
	switchEventIndex := -1
	for index, event := range result.Events {
		switch value := event.(type) {
		case battleengine.ParticipantSwitchedEvent:
			if value.NextMember == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) {
				switchEventIndex = index
			}
		case battleengine.SpikesDamageAppliedEvent, battleengine.StealthRockDamageAppliedEvent,
			battleengine.ToxicSpikesAbsorbedEvent, battleengine.ToxicSpikesStatusAppliedEvent,
			battleengine.StickyWebSpeedLoweredEvent:
			t.Fatalf("免疫道具换入后不应产生危害事件 = %+v", event)
		}
	}
	if switchEventIndex < 0 {
		t.Fatalf("换入事件未写入事件流 = %+v", result.Events)
	}
	if !result.State.Snapshot().Sides[0].Conditions.StealthRock || result.State.Snapshot().Sides[0].Conditions.SpikesLayers != 3 || result.State.Snapshot().Sides[0].Conditions.ToxicSpikesLayers != 2 || !result.State.Snapshot().Sides[0].Conditions.StickyWeb {
		t.Fatalf("危害不应被免疫道具移除 = %+v", result.State.Snapshot().Sides[0].Conditions)
	}
}
