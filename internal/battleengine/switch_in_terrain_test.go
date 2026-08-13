package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInTerrain 验证初始上场成员的普通场地特性会写入权威环境快照。
//
// 初始建场不写入第 0 回合事件流，避免客户端把尚未开始的回合误认为一次实时换人；场地持续回合来自特性
// 自身冻结规则，不能借用技能场地参数。
func TestInitialStateAppliesSwitchInTerrain(t *testing.T) {
	t.Parallel()
	setter := newMember(1, "switch-in-electric-terrain", 1_000, 1_000)
	setter.SwitchInTerrain = &battleengine.SwitchInTerrain{Effect: battleengine.TerrainEffect{
		Kind: battleengine.TerrainKindElectric, TurnsRemaining: 5,
	}}
	observer := newMember(1, "switch-in-terrain-observer", 1_000, 1_000)
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, setter, observer)
	terrain := state.Snapshot().Environment.Terrain
	if terrain == nil || *terrain != (battleengine.TerrainEffect{Kind: battleengine.TerrainKindElectric, TurnsRemaining: 5}) {
		t.Fatalf("初始入场场地 = %+v", terrain)
	}
}

// TestResolveTurnSwitchInTerrainOverridesTerrain 验证后备成员实际换入后覆盖既有场地并发布独立事件。
//
// 回合末仍会推进刚建立的场地一次，说明入场场地复用的是环境生命周期，而不是另建一份特性专属计时器。
func TestResolveTurnSwitchInTerrainOverridesTerrain(t *testing.T) {
	t.Parallel()
	first := newMember(1, "switch-in-electric-terrain-source", 1_000, 1_000)
	first.SwitchInTerrain = &battleengine.SwitchInTerrain{Effect: battleengine.TerrainEffect{
		Kind: battleengine.TerrainKindElectric, TurnsRemaining: 5,
	}}
	incoming := newMember(2, "switch-in-grassy-terrain-source", 1_000, 1_000)
	incoming.SwitchInTerrain = &battleengine.SwitchInTerrain{Effect: battleengine.TerrainEffect{
		Kind: battleengine.TerrainKindGrassy, TurnsRemaining: 5,
	}}
	opponent := newMember(1, "switch-in-terrain-opponent", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-terrain", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first, incoming}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
			fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
		},
	}, mustRandom(t, 239))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	found := false
	for _, event := range result.Events {
		started, ok := event.(battleengine.AbilityTerrainStartedEvent)
		if ok && started.Source == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) {
			found = started.Terrain == battleengine.TerrainKindGrassy && started.TurnsRemaining == 5
		}
	}
	if !found {
		t.Fatalf("换入青草场地缺少正确入场场地事件: %+v", result.Events)
	}
	terrain := result.State.Snapshot().Environment.Terrain
	if terrain == nil || *terrain != (battleengine.TerrainEffect{Kind: battleengine.TerrainKindGrassy, TurnsRemaining: 4}) {
		t.Fatalf("换入青草场地后的环境 = %+v", terrain)
	}
}
