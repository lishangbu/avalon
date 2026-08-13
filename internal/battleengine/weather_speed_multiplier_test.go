package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnAbilityWeatherSpeedMultiplier 验证特性的天气速度整数分数倍率改变同优先度行动顺序；任一场上
// 天气封锁特性存在时，倍率与其它普通天气效果一样暂停，而不是删除天气环境。
func TestResolveTurnAbilityWeatherSpeedMultiplier(t *testing.T) {
	t.Parallel()
	for _, test := range []struct {
		name           string
		suppressed     bool
		wantFirstActor battleengine.Side
	}{
		{name: "降雨速度倍率", wantFirstActor: battleengine.SideOne},
		{name: "天气封锁暂停速度倍率", suppressed: true, wantFirstActor: battleengine.SideTwo},
	} {
		t.Run(test.name, func(t *testing.T) {
			boosted := newMember(1, "weather-speed-boosted", 500, 500)
			boosted.Stats.Speed = 100
			boosted.WeatherSpeedMultipliers = []battleengine.WeatherSpeedMultiplier{{
				Weather: battleengine.WeatherKindRain, Numerator: 2, Denominator: 1,
			}}
			observer := newMember(1, "weather-speed-observer", 500, 500)
			observer.Stats.Speed = 150
			observer.WeatherEffectsSuppressed = test.suppressed
			state := newWeatherState(t,
				battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 2}},
				battleengine.RuleSnapshot{SchemaVersion: 1}, boosted, observer,
			)
			result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
				fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
				fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
			), mustRandom(t, 109))
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			first, found := findFirstDamage(result.Events)
			if !found || first.Actor.Side != test.wantFirstActor {
				t.Fatalf("首个伤害事件 = %+v，期望第一行动方 %d", first, test.wantFirstActor)
			}
		})
	}
}
