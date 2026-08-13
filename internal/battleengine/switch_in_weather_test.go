package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInWeather 验证初始上场成员的普通天气特性会写入权威环境快照，并使用自身的
// 强类型持续回合，而不是借用初始环境参数或技能天气规则。
func TestInitialStateAppliesSwitchInWeather(t *testing.T) {
	t.Parallel()
	setter := newMember(1, "switch-in-rain", 1_000, 1_000)
	setter.SwitchInWeather = &battleengine.SwitchInWeather{Effect: battleengine.WeatherEffect{
		Kind: battleengine.WeatherKindRain, TurnsRemaining: 5,
	}}
	observer := newMember(1, "switch-in-rain-observer", 1_000, 1_000)
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, setter, observer)
	weather := state.Snapshot().Environment.Weather
	if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 5}) {
		t.Fatalf("初始入场天气 = %+v", weather)
	}
}

// TestResolveTurnSwitchInWeatherOverridesOrdinaryWeather 验证后备成员实际换入后会覆盖既有普通天气并发出独立
// 入场天气事件；建立当回合的回合末仍会推进一次持续时间。
func TestResolveTurnSwitchInWeatherOverridesOrdinaryWeather(t *testing.T) {
	t.Parallel()
	first := newMember(1, "switch-in-rain-source", 1_000, 1_000)
	first.SwitchInWeather = &battleengine.SwitchInWeather{Effect: battleengine.WeatherEffect{
		Kind: battleengine.WeatherKindRain, TurnsRemaining: 5,
	}}
	incoming := newMember(2, "switch-in-sun-source", 1_000, 1_000)
	incoming.SwitchInWeather = &battleengine.SwitchInWeather{Effect: battleengine.WeatherEffect{
		Kind: battleengine.WeatherKindSun, TurnsRemaining: 5,
	}}
	opponent := newMember(1, "switch-in-weather-opponent", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-weather", ActiveSlotsPerSide: 1, TeamSize: 2},
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
	}, mustRandom(t, 233))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	found := false
	for _, event := range result.Events {
		started, ok := event.(battleengine.AbilityWeatherStartedEvent)
		if ok && started.Source == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) {
			found = started.Weather == battleengine.WeatherKindSun && started.TurnsRemaining == 5
		}
	}
	if !found {
		t.Fatalf("换入日照缺少正确入场天气事件: %+v", result.Events)
	}
	weather := result.State.Snapshot().Environment.Weather
	if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSun, TurnsRemaining: 4}) {
		t.Fatalf("换入日照后的环境 = %+v", weather)
	}
}

// TestInitialStrongWeatherSuppressesSwitchInWeather 验证普通入场天气与强天气分开建模：同一初始站位中
// 强天气在建立后清除普通天气，普通特性不能把它覆盖为有限持续时间天气。
func TestInitialStrongWeatherSuppressesSwitchInWeather(t *testing.T) {
	t.Parallel()
	normalWeather := newMember(1, "switch-in-normal-weather", 1_000, 1_000)
	normalWeather.SwitchInWeather = &battleengine.SwitchInWeather{Effect: battleengine.WeatherEffect{
		Kind: battleengine.WeatherKindRain, TurnsRemaining: 5,
	}}
	strongWeather := newMember(1, "switch-in-strong-weather", 1_000, 1_000)
	strongWeather.SwitchInStrongWeather = battleengine.StrongWeatherKindHarshSunlight
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, normalWeather, strongWeather)
	environment := state.Snapshot().Environment
	if environment.Weather != nil || environment.StrongWeather == nil || environment.StrongWeather.Kind != battleengine.StrongWeatherKindHarshSunlight {
		t.Fatalf("强天气建立后的初始环境 = %+v", environment)
	}
}
