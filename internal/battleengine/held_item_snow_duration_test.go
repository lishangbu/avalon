package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnHeldItemExtendsSnowDuration 验证降雪延长道具只影响持有者新建的普通降雪。
// 测试锁定技能、入场特性、事件完整初始回合和回合末状态，避免把独立的降雪资料错误复用到其它天气。
func TestResolveTurnHeldItemExtendsSnowDuration(t *testing.T) {
	t.Parallel()
	t.Run("技能建立降雪时延长", func(t *testing.T) {
		caster := newMember(1, "snow-duration-skill-user", 500, 500)
		caster.Stats.Speed = 200
		caster.ItemID = testID("snow-duration-item")
		caster.HeldItemSnowTurnsRemaining = 8
		caster.Skills[0] = weatherSkill(1, battleengine.WeatherKindSnow, 5)
		target := newMember(1, "snow-duration-skill-target", 500, 500)
		target.Stats.Speed = 10

		result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 529))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		weather := result.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSnow, TurnsRemaining: 7}) {
			t.Fatalf("回合末降雪 = %+v", weather)
		}
		if !weatherStartedWithDuration(result.Events, battleengine.WeatherKindSnow, 8) {
			t.Fatalf("降雪开始事件未记录道具延长回合 = %+v", result.Events)
		}
	})
	t.Run("不匹配天气与更长来源持续回合保持原值", func(t *testing.T) {
		caster := newMember(1, "snow-duration-boundary-user", 500, 500)
		caster.Stats.Speed = 200
		caster.ItemID = testID("snow-duration-item")
		caster.HeldItemSnowTurnsRemaining = 8
		caster.Skills[0] = weatherSkill(1, battleengine.WeatherKindSun, 5)
		target := newMember(1, "snow-duration-boundary-target", 500, 500)
		target.Stats.Speed = 10

		unmatched, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 530))
		if err != nil {
			t.Fatalf("不匹配天气 ResolveTurn() error = %v", err)
		}
		weather := unmatched.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSun, TurnsRemaining: 4}) || !weatherStartedWithDuration(unmatched.Events, battleengine.WeatherKindSun, 5) {
			t.Fatalf("不匹配天气结果 = weather:%+v events:%+v", weather, unmatched.Events)
		}

		caster.Skills[0] = weatherSkill(1, battleengine.WeatherKindSnow, 9)
		longer, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 531))
		if err != nil {
			t.Fatalf("更长降雪 ResolveTurn() error = %v", err)
		}
		weather = longer.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSnow, TurnsRemaining: 8}) || !weatherStartedWithDuration(longer.Events, battleengine.WeatherKindSnow, 9) {
			t.Fatalf("更长降雪结果 = weather:%+v events:%+v", weather, longer.Events)
		}
	})
	t.Run("实际换入建立降雪时记录延长回合", func(t *testing.T) {
		first := newMember(1, "snow-duration-first", 500, 500)
		incoming := newMember(2, "snow-duration-incoming", 500, 500)
		incoming.ItemID = testID("snow-duration-item")
		incoming.HeldItemSnowTurnsRemaining = 8
		incoming.SwitchInWeather = &battleengine.SwitchInWeather{Effect: battleengine.WeatherEffect{Kind: battleengine.WeatherKindSnow, TurnsRemaining: 5}}
		opponent := newMember(1, "snow-duration-switch-opponent", 500, 500)

		state, err := battleengine.NewState(battleengine.InitialState{
			Format: battleengine.FormatSnapshot{Code: "snow-duration-switch", ActiveSlotsPerSide: 1, TeamSize: 2},
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
		}, mustRandom(t, 532))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		if !weatherStartedWithDuration(result.Events, battleengine.WeatherKindSnow, 8) {
			t.Fatalf("实际换入降雪事件未记录延长回合 = %+v", result.Events)
		}
		weather := result.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSnow, TurnsRemaining: 7}) {
			t.Fatalf("实际换入降雪回合末快照 = %+v", weather)
		}
	})
}
