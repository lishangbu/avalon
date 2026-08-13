package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnHeldItemExtendsSunDuration 验证日照延长道具只延长持有者新建的普通日照。
// 日照规则保留独立的资料和运行时字段，测试覆盖技能、边界、入场事件和回合末权威快照。
func TestResolveTurnHeldItemExtendsSunDuration(t *testing.T) {
	t.Parallel()
	t.Run("技能建立日照时延长", func(t *testing.T) {
		caster := newMember(1, "sun-duration-skill-user", 500, 500)
		caster.Stats.Speed = 200
		caster.ItemID = testID("sun-duration-item")
		caster.HeldItemSunTurnsRemaining = 8
		caster.Skills[0] = weatherSkill(1, battleengine.WeatherKindSun, 5)
		target := newMember(1, "sun-duration-skill-target", 500, 500)
		target.Stats.Speed = 10

		result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 533))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		weather := result.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSun, TurnsRemaining: 7}) {
			t.Fatalf("回合末日照 = %+v", weather)
		}
		if !weatherStartedWithDuration(result.Events, battleengine.WeatherKindSun, 8) {
			t.Fatalf("日照开始事件未记录道具延长回合 = %+v", result.Events)
		}
	})
	t.Run("不匹配天气与更长来源持续回合保持原值", func(t *testing.T) {
		caster := newMember(1, "sun-duration-boundary-user", 500, 500)
		caster.Stats.Speed = 200
		caster.ItemID = testID("sun-duration-item")
		caster.HeldItemSunTurnsRemaining = 8
		caster.Skills[0] = weatherSkill(1, battleengine.WeatherKindRain, 5)
		target := newMember(1, "sun-duration-boundary-target", 500, 500)
		target.Stats.Speed = 10

		unmatched, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 534))
		if err != nil {
			t.Fatalf("不匹配天气 ResolveTurn() error = %v", err)
		}
		weather := unmatched.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 4}) || !weatherStartedWithDuration(unmatched.Events, battleengine.WeatherKindRain, 5) {
			t.Fatalf("不匹配天气结果 = weather:%+v events:%+v", weather, unmatched.Events)
		}

		caster.Skills[0] = weatherSkill(1, battleengine.WeatherKindSun, 9)
		longer, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 535))
		if err != nil {
			t.Fatalf("更长日照 ResolveTurn() error = %v", err)
		}
		weather = longer.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSun, TurnsRemaining: 8}) || !weatherStartedWithDuration(longer.Events, battleengine.WeatherKindSun, 9) {
			t.Fatalf("更长日照结果 = weather:%+v events:%+v", weather, longer.Events)
		}
	})
	t.Run("实际换入建立日照时记录延长回合", func(t *testing.T) {
		first := newMember(1, "sun-duration-first", 500, 500)
		incoming := newMember(2, "sun-duration-incoming", 500, 500)
		incoming.ItemID = testID("sun-duration-item")
		incoming.HeldItemSunTurnsRemaining = 8
		incoming.SwitchInWeather = &battleengine.SwitchInWeather{Effect: battleengine.WeatherEffect{Kind: battleengine.WeatherKindSun, TurnsRemaining: 5}}
		opponent := newMember(1, "sun-duration-switch-opponent", 500, 500)

		state, err := battleengine.NewState(battleengine.InitialState{
			Format: battleengine.FormatSnapshot{Code: "sun-duration-switch", ActiveSlotsPerSide: 1, TeamSize: 2},
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
		}, mustRandom(t, 536))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		if !weatherStartedWithDuration(result.Events, battleengine.WeatherKindSun, 8) {
			t.Fatalf("实际换入日照事件未记录延长回合 = %+v", result.Events)
		}
		weather := result.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSun, TurnsRemaining: 7}) {
			t.Fatalf("实际换入日照回合末快照 = %+v", weather)
		}
	})
}
