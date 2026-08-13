package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnHeldItemExtendsSandstormDuration 验证沙暴延长道具的匹配范围、事件顺序和环境快照。
// 沙暴与降雨使用彼此独立的资料字段；测试同时防止道具被误用于其它天气或缩短更长的来源持续回合。
func TestResolveTurnHeldItemExtendsSandstormDuration(t *testing.T) {
	t.Parallel()
	t.Run("技能建立沙暴时延长", func(t *testing.T) {
		caster := newMember(1, "sandstorm-duration-skill-user", 500, 500)
		caster.Stats.Speed = 200
		caster.ItemID = testID("sandstorm-duration-item")
		caster.HeldItemSandstormTurnsRemaining = 8
		caster.Skills[0] = weatherSkill(1, battleengine.WeatherKindSandstorm, 5)
		target := newMember(1, "sandstorm-duration-skill-target", 500, 500)
		target.Stats.Speed = 10

		result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 525))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		weather := result.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 7}) {
			t.Fatalf("回合末沙暴 = %+v", weather)
		}
		if !weatherStartedWithDuration(result.Events, battleengine.WeatherKindSandstorm, 8) {
			t.Fatalf("沙暴开始事件未记录道具延长回合 = %+v", result.Events)
		}
	})
	t.Run("不匹配天气与更长来源持续回合保持原值", func(t *testing.T) {
		caster := newMember(1, "sandstorm-duration-boundary-user", 500, 500)
		caster.Stats.Speed = 200
		caster.ItemID = testID("sandstorm-duration-item")
		caster.HeldItemSandstormTurnsRemaining = 8
		caster.Skills[0] = weatherSkill(1, battleengine.WeatherKindSnow, 5)
		target := newMember(1, "sandstorm-duration-boundary-target", 500, 500)
		target.Stats.Speed = 10

		unmatched, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 526))
		if err != nil {
			t.Fatalf("不匹配天气 ResolveTurn() error = %v", err)
		}
		weather := unmatched.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSnow, TurnsRemaining: 4}) || !weatherStartedWithDuration(unmatched.Events, battleengine.WeatherKindSnow, 5) {
			t.Fatalf("不匹配天气结果 = weather:%+v events:%+v", weather, unmatched.Events)
		}

		caster.Skills[0] = weatherSkill(1, battleengine.WeatherKindSandstorm, 9)
		longer, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 527))
		if err != nil {
			t.Fatalf("更长沙暴 ResolveTurn() error = %v", err)
		}
		weather = longer.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 8}) || !weatherStartedWithDuration(longer.Events, battleengine.WeatherKindSandstorm, 9) {
			t.Fatalf("更长沙暴结果 = weather:%+v events:%+v", weather, longer.Events)
		}
	})
	t.Run("实际换入建立沙暴时记录延长回合", func(t *testing.T) {
		first := newMember(1, "sandstorm-duration-first", 500, 500)
		incoming := newMember(2, "sandstorm-duration-incoming", 500, 500)
		incoming.ItemID = testID("sandstorm-duration-item")
		incoming.HeldItemSandstormTurnsRemaining = 8
		incoming.SwitchInWeather = &battleengine.SwitchInWeather{Effect: battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 5}}
		opponent := newMember(1, "sandstorm-duration-switch-opponent", 500, 500)

		state, err := battleengine.NewState(battleengine.InitialState{
			Format: battleengine.FormatSnapshot{Code: "sandstorm-duration-switch", ActiveSlotsPerSide: 1, TeamSize: 2},
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
		}, mustRandom(t, 528))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		if !weatherStartedWithDuration(result.Events, battleengine.WeatherKindSandstorm, 8) {
			t.Fatalf("实际换入沙暴事件未记录延长回合 = %+v", result.Events)
		}
		weather := result.State.Snapshot().Environment.Weather
		if weather == nil || *weather != (battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 7}) {
			t.Fatalf("实际换入沙暴回合末快照 = %+v", weather)
		}
	})
}
