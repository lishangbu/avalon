package battleengine_test

import (
	"strings"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnWeatherAccuracyOverrides 验证天气命中覆盖只在匹配天气下替换基础命中率，并保留 0 为必中。
//
// 三个子场景分别覆盖：必中不消费命中随机数、低命中覆盖在事件中记录覆盖后命中率，以及不匹配天气仍使用
// 技能原始命中率。这样资料中的 0 不会被 Go 零值误当成未配置，且规则不会越过天气边界泄漏到其它对局。
func TestResolveTurnWeatherAccuracyOverrides(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name             string
		weather          battleengine.WeatherKind
		baseAccuracy     uint8
		overrideWeather  battleengine.WeatherKind
		overrideAccuracy uint8
		wantMissAccuracy uint8
		wantDamage       bool
		wantAccuracyRoll bool
	}{
		{
			name: "降雨必中覆盖不消费命中随机数", weather: battleengine.WeatherKindRain,
			baseAccuracy: 50, overrideWeather: battleengine.WeatherKindRain, overrideAccuracy: 0,
			wantDamage: true,
		},
		{
			name: "低命中覆盖记录覆盖后的未命中率", weather: battleengine.WeatherKindRain,
			baseAccuracy: 100, overrideWeather: battleengine.WeatherKindRain, overrideAccuracy: 25,
			wantMissAccuracy: 25, wantAccuracyRoll: true,
		},
		{
			name: "不匹配天气继续使用基础命中率", weather: battleengine.WeatherKindSun,
			baseAccuracy: 50, overrideWeather: battleengine.WeatherKindRain, overrideAccuracy: 25,
			wantMissAccuracy: 50, wantAccuracyRoll: true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			attacker := newMember(1, "weather-accuracy-attacker", 100, 100)
			attacker.Stats.Speed = 200
			attacker.Skills[0].Accuracy = test.baseAccuracy
			attacker.Skills[0].WeatherAccuracyOverrides = []battleengine.WeatherAccuracyOverride{{
				Weather: test.overrideWeather, AccuracyPercent: test.overrideAccuracy,
			}}
			defender := newMember(1, "weather-accuracy-defender", 100, 100)
			defender.Stats.Speed = 10
			state := newWeatherState(t, battleengine.EnvironmentSnapshot{
				Weather: &battleengine.WeatherEffect{Kind: test.weather, TurnsRemaining: 3},
			}, battleengine.RuleSnapshot{SchemaVersion: 1}, attacker, defender)
			random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
			if err != nil {
				t.Fatalf("NewRandomSource() error = %v", err)
			}
			result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
				SchemaVersion: 1, TurnNumber: 1,
				Actions: []battleengine.Action{
					useSkillCommand(battleengine.SideOne, battleengine.SideTwo),
					useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
				},
			}, random)
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			missedAccuracy, missed := weatherAccuracyMiss(result.Events, battleengine.SideOne)
			if test.wantMissAccuracy == 0 && missed {
				t.Fatalf("攻击者不应未命中，事件 = %+v", result.Events)
			}
			if test.wantMissAccuracy != 0 && (!missed || missedAccuracy != test.wantMissAccuracy) {
				t.Fatalf("攻击者未命中率 = %d, missed=%t，期望 %d，事件 = %+v", missedAccuracy, missed, test.wantMissAccuracy, result.Events)
			}
			if test.wantDamage && !weatherAccuracyDamageExists(result.Events, battleengine.SideOne) {
				t.Fatalf("必中覆盖未产生攻击者伤害，事件 = %+v", result.Events)
			}
			hasAccuracyRoll := false
			for _, entry := range result.RandomTrace {
				if strings.HasPrefix(entry.Reason, "accuracy for "+attacker.Skills[0].SkillID.String()) {
					hasAccuracyRoll = true
				}
			}
			if hasAccuracyRoll != test.wantAccuracyRoll {
				t.Fatalf("攻击者命中随机数存在=%t，期望=%t，轨迹=%+v", hasAccuracyRoll, test.wantAccuracyRoll, result.RandomTrace)
			}
		})
	}
}

// weatherAccuracyMiss 从事件流中读取指定行动方的技能未命中事件及其有效命中率。
func weatherAccuracyMiss(events []battleengine.Event, side battleengine.Side) (uint8, bool) {
	for _, event := range events {
		missed, ok := event.(battleengine.SkillMissedEvent)
		if ok && missed.Actor.Side == side {
			return missed.Accuracy, true
		}
	}
	return 0, false
}

// weatherAccuracyDamageExists 报告指定行动方是否成功产生普通伤害事件。
func weatherAccuracyDamageExists(events []battleengine.Event, side battleengine.Side) bool {
	for _, event := range events {
		damage, ok := event.(battleengine.DamageAppliedEvent)
		if ok && damage.Actor.Side == side {
			return true
		}
	}
	return false
}
