package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnHeldItemExtendsRainDuration 验证降雨延长道具仅在持有者成功建立普通降雨时参与持续回合计算。
// 技能和入场特性是两个独立天气来源；不匹配天气、已失去道具和更长的原始持续回合都不能被错误改写。
func TestResolveTurnHeldItemExtendsRainDuration(t *testing.T) {
	t.Parallel()
	t.Run("技能建立降雨时延长", func(t *testing.T) {
		caster := newMember(1, "rain-duration-skill-user", 500, 500)
		caster.Stats.Speed = 200
		caster.ItemID = testID("rain-duration-item")
		caster.HeldItemRainTurnsRemaining = 8
		caster.Skills[0] = weatherSkill(1, battleengine.WeatherKindRain, 5)
		target := newMember(1, "rain-duration-skill-target", 500, 500)
		target.Stats.Speed = 10

		result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 520))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		weather := result.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 7}) {
			t.Fatalf("回合末降雨 = %+v", weather)
		}
		if !weatherStartedWithDuration(result.Events, battleengine.WeatherKindRain, 8) {
			t.Fatalf("降雨开始事件未记录道具延长回合 = %+v", result.Events)
		}
	})
	t.Run("不匹配天气和更长原始持续回合不被改写", func(t *testing.T) {
		caster := newMember(1, "rain-duration-boundary-user", 500, 500)
		caster.Stats.Speed = 200
		caster.ItemID = testID("rain-duration-item")
		caster.HeldItemRainTurnsRemaining = 8
		caster.Skills[0] = weatherSkill(1, battleengine.WeatherKindSun, 5)
		target := newMember(1, "rain-duration-boundary-target", 500, 500)
		target.Stats.Speed = 10

		unmatched, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 521))
		if err != nil {
			t.Fatalf("不匹配天气 ResolveTurn() error = %v", err)
		}
		weather := unmatched.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSun, TurnsRemaining: 4}) || !weatherStartedWithDuration(unmatched.Events, battleengine.WeatherKindSun, 5) {
			t.Fatalf("不匹配天气结果 = weather:%+v events:%+v", weather, unmatched.Events)
		}

		caster.Skills[0] = weatherSkill(1, battleengine.WeatherKindRain, 9)
		longer, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 522))
		if err != nil {
			t.Fatalf("更长降雨 ResolveTurn() error = %v", err)
		}
		weather = longer.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 8}) || !weatherStartedWithDuration(longer.Events, battleengine.WeatherKindRain, 9) {
			t.Fatalf("更长降雨结果 = weather:%+v events:%+v", weather, longer.Events)
		}
	})
	t.Run("入场特性建立降雨时延长", func(t *testing.T) {
		setter := newMember(1, "rain-duration-switch-in-user", 500, 500)
		setter.ItemID = testID("rain-duration-item")
		setter.HeldItemRainTurnsRemaining = 8
		setter.SwitchInWeather = &battleengine.SwitchInWeather{Effect: battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 5}}
		observer := newMember(1, "rain-duration-switch-in-observer", 500, 500)

		state := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, setter, observer)
		weather := state.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 8}) {
			t.Fatalf("初始入场降雨 = %+v", weather)
		}
	})
	t.Run("实际换入记录延长后的回合且不重复刷新", func(t *testing.T) {
		first := newMember(1, "rain-duration-first", 500, 500)
		incoming := newMember(2, "rain-duration-incoming", 500, 500)
		incoming.ItemID = testID("rain-duration-item")
		incoming.HeldItemRainTurnsRemaining = 8
		incoming.SwitchInWeather = &battleengine.SwitchInWeather{Effect: battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 5}}
		opponent := newMember(1, "rain-duration-switch-opponent", 500, 500)

		state, err := battleengine.NewState(battleengine.InitialState{
			Format: battleengine.FormatSnapshot{Code: "rain-duration-switch", ActiveSlotsPerSide: 1, TeamSize: 2},
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
		}, mustRandom(t, 523))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		if !weatherStartedWithDuration(result.Events, battleengine.WeatherKindRain, 8) {
			t.Fatalf("实际换入降雨事件未记录延长回合 = %+v", result.Events)
		}
		weather := result.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 7}) {
			t.Fatalf("实际换入降雨回合末快照 = %+v", weather)
		}

		second := newMember(3, "rain-duration-same-weather", 500, 500)
		second.ItemID = testID("rain-duration-item")
		second.HeldItemRainTurnsRemaining = 8
		second.SwitchInWeather = &battleengine.SwitchInWeather{Effect: battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 5}}
		secondState, err := battleengine.NewState(battleengine.InitialState{
			Format:      battleengine.FormatSnapshot{Code: "rain-duration-same-weather", ActiveSlotsPerSide: 1, TeamSize: 3},
			Rules:       battleengine.RuleSnapshot{SchemaVersion: 1},
			Environment: battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 8}},
			Sides: []battleengine.SideSnapshot{
				{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first, second}},
				{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
			},
		})
		if err != nil {
			t.Fatalf("相同天气 NewState() error = %v", err)
		}
		repeated, err := battleengine.ResolveTurn(secondState, battleengine.TurnCommand{
			SchemaVersion: 1, TurnNumber: 1,
			Actions: []battleengine.Action{
				{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 3}},
				fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
			},
		}, mustRandom(t, 524))
		if err != nil {
			t.Fatalf("相同天气 ResolveTurn() error = %v", err)
		}
		if weatherStartedWithDuration(repeated.Events, battleengine.WeatherKindRain, 8) {
			t.Fatalf("相同延长降雨不应重复建立 = %+v", repeated.Events)
		}
	})
}

// weatherStartedWithDuration 报告事件流是否包含指定普通天气及其建立时完整持续回合。
func weatherStartedWithDuration(events []battleengine.Event, weather battleengine.WeatherKind, turns uint8) bool {
	for _, event := range events {
		switch value := event.(type) {
		case battleengine.WeatherStartedEvent:
			if value.Weather == weather && value.TurnsRemaining == turns {
				return true
			}
		case battleengine.AbilityWeatherStartedEvent:
			if value.Weather == weather && value.TurnsRemaining == turns {
				return true
			}
		}
	}
	return false
}
