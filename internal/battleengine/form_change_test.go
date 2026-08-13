package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInFormChange 验证初始上场阶段会切换形态，并只在明确规则下补齐最大生命差额。
func TestInitialStateAppliesSwitchInFormChange(t *testing.T) {
	t.Parallel()
	base := newMember(1, "terapagos-base", 100, 80)
	base.AbilityID = testID("switch-form-ability")
	base.Stats.Attack = 90
	base.FormProfiles = []battleengine.FormProfile{
		formProfile(base),
		{CreatureID: testID("terapagos-terastal"), MaxHP: 120, Stats: battleengine.StatBlock{Attack: 120, Defense: 100, SpecialAttack: 100, SpecialDefense: 100, Speed: 90}, Weight: 700, ElementIDs: testIDs("normal")},
	}
	base.SwitchInFormChange = &battleengine.SwitchInFormChange{
		BaseCreatureID: base.CreatureID, AlternateCreatureID: testID("terapagos-terastal"), AddsMaximumHPDifference: true,
	}
	opponent := newMember(1, "form-opponent", 100, 100)
	state, err := battleengine.NewState(formState("switch-in-form-initial", base, opponent))
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	changed, found := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || changed.CreatureID != testID("terapagos-terastal") || changed.MaxHP != 120 || changed.CurrentHP != 100 || changed.Stats.Attack != 120 {
		t.Fatalf("初始入场形态 = %+v", changed)
	}
	summary := state.Summary()
	if len(summary.Members) != 2 || summary.Members[0].CreatureID != testID("terapagos-terastal") ||
		summary.Members[0].MaxHP != 120 || summary.Members[0].Stats.Attack != 120 || summary.Members[0].Weight != 700 ||
		len(summary.Members[0].ElementIDs) != 1 || summary.Members[0].ElementIDs[0] != testID("normal") ||
		summary.Members[0].AbilityID != testID("switch-form-ability") {
		t.Fatalf("形态状态摘要 = %+v", summary.Members[0])
	}
	events := state.InitialEvents()
	if len(events) != 1 {
		t.Fatalf("初始形态事件数量 = %d, events=%+v", len(events), events)
	}
	event, ok := events[0].(battleengine.FormChangedEvent)
	if !ok || event.Reason != battleengine.FormChangeReasonSwitchInAbility || event.FromCreatureID != testID("terapagos-base") || event.ToCreatureID != testID("terapagos-terastal") {
		t.Fatalf("初始形态事件 = %+v", events[0])
	}
}

// TestResolveTurnSwitchInFormChangeKeepsCurrentHPWithoutSpecialRule 验证普通入场形态切换保留生命并按新上限夹取。
func TestResolveTurnSwitchInFormChangeKeepsCurrentHPWithoutSpecialRule(t *testing.T) {
	t.Parallel()
	front := newMember(1, "form-front", 100, 100)
	incoming := newMember(2, "form-incoming-base", 100, 90)
	incoming.FormProfiles = []battleengine.FormProfile{
		formProfile(incoming),
		{CreatureID: testID("form-incoming-alternate"), MaxHP: 70, Stats: battleengine.StatBlock{Attack: 130, Defense: 100, SpecialAttack: 100, SpecialDefense: 100, Speed: 80}, Weight: 20, ElementIDs: testIDs("fire")},
	}
	incoming.SwitchInFormChange = &battleengine.SwitchInFormChange{
		BaseCreatureID: incoming.CreatureID, AlternateCreatureID: testID("form-incoming-alternate"),
	}
	opponent := newMember(1, "form-switch-opponent", 100, 100)
	opponent.Skills[0] = fieldSpeedOrderSkill(1, "无伤害行动", 0)
	state, err := battleengine.NewState(formStateWithReserve("switch-in-form-switch", front, incoming, opponent))
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, formSwitchTurn(1, 2), mustRandom(t, 201))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	changed, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || changed.CreatureID != testID("form-incoming-alternate") || changed.MaxHP != 70 || changed.CurrentHP != 70 || changed.Stats.Attack != 130 {
		t.Fatalf("换入形态 = %+v", changed)
	}
	if !hasFormChange(result.Events, battleengine.FormChangeReasonSwitchInAbility) {
		t.Fatalf("换入未记录形态事件: %+v", result.Events)
	}
}

// TestWeatherFormChangeTracksEffectiveWeather 验证天气形态会随天气建立、到期和天气封锁回到默认形态。
func TestWeatherFormChangeTracksEffectiveWeather(t *testing.T) {
	t.Parallel()
	castform := newMember(1, "castform-normal", 100, 100)
	castform.FormProfiles = []battleengine.FormProfile{
		formProfile(castform),
		{CreatureID: testID("castform-sunny"), MaxHP: 100, Stats: castform.Stats, Weight: 1, ElementIDs: testIDs("fire")},
	}
	castform.WeatherFormChange = &battleengine.WeatherFormChange{
		DefaultCreatureID: testID("castform-normal"),
		Targets:           []battleengine.WeatherFormTarget{{Weather: battleengine.WeatherKindSun, CreatureID: testID("castform-sunny")}},
	}
	opponent := newMember(1, "weather-form-opponent", 100, 100)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "weather-form", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Environment: battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{
			Kind: battleengine.WeatherKindSun, TurnsRemaining: 1,
		}},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{castform}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	sunny, _ := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if sunny.CreatureID != testID("castform-sunny") || len(state.InitialEvents()) != 1 {
		t.Fatalf("初始天气形态 = %+v, events=%+v", sunny, state.InitialEvents())
	}
	result, err := battleengine.ResolveTurn(state, formIdleTurn(1), mustRandom(t, 301))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	normal, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if normal.CreatureID != testID("castform-normal") || !hasFormChange(result.Events, battleengine.FormChangeReasonWeatherAbility) {
		t.Fatalf("天气到期后形态 = %+v, events=%+v", normal, result.Events)
	}
}

func formProfile(member battleengine.MemberSnapshot) battleengine.FormProfile {
	weight := member.Weight
	if weight == 0 {
		weight = 1
	}
	return battleengine.FormProfile{CreatureID: member.CreatureID, MaxHP: member.MaxHP, Stats: member.Stats, Weight: weight, ElementIDs: append([]Identifier(nil), member.ElementIDs...)}
}

func hasFormChange(events []battleengine.Event, reason battleengine.FormChangeReason) bool {
	for _, event := range events {
		if changed, ok := event.(battleengine.FormChangedEvent); ok && changed.Reason == reason {
			return true
		}
	}
	return false
}

func formState(code string, first, second battleengine.MemberSnapshot) battleengine.InitialState {
	return battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: code, ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{second}},
		},
	}
}

func formStateWithReserve(code string, first, reserve, second battleengine.MemberSnapshot) battleengine.InitialState {
	state := formState(code, first, second)
	state.Format.TeamSize = 2
	state.Sides[0].Members = []battleengine.MemberSnapshot{first, reserve}
	return state
}

func formSwitchTurn(turnNumber uint32, incoming battleengine.MemberPosition) battleengine.TurnCommand {
	return battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: turnNumber,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: incoming}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}
}

func formIdleTurn(turnNumber uint32) battleengine.TurnCommand {
	return battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: turnNumber,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}
}
