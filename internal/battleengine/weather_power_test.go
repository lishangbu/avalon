package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnWeatherPowerMultipliersChangeBasePower 验证匹配普通天气的独立威力倍率在普通伤害公式前生效，
// 并且未匹配天气严格保留技能基础威力。该断言选择无天气固有属性修正的普通属性，隔离资料倍率本身的结算语义。
func TestResolveTurnWeatherPowerMultipliersChangeBasePower(t *testing.T) {
	t.Parallel()

	rain := &battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 3}
	baseDamage := resolveWeatherPowerDamage(t, battleengine.EnvironmentSnapshot{Weather: rain}, nil)
	doubledDamage := resolveWeatherPowerDamage(t, battleengine.EnvironmentSnapshot{Weather: rain}, []battleengine.WeatherPowerMultiplier{{
		Weather: battleengine.WeatherKindRain, Numerator: 2, Denominator: 1,
	}})
	if doubledDamage <= baseDamage {
		t.Fatalf("降雨匹配的天气威力倍率伤害 = %d，基础伤害 = %d；期望倍率在普通伤害公式前生效", doubledDamage, baseDamage)
	}

	unmatchedDamage := resolveWeatherPowerDamage(t, battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{
		Kind: battleengine.WeatherKindSun, TurnsRemaining: 3,
	}}, []battleengine.WeatherPowerMultiplier{{
		Weather: battleengine.WeatherKindRain, Numerator: 2, Denominator: 1,
	}})
	if unmatchedDamage != baseDamage {
		t.Fatalf("未匹配天气的威力倍率伤害 = %d，基础伤害 = %d；期望回退技能基础威力", unmatchedDamage, baseDamage)
	}

	suppressedDamage := resolveSuppressedWeatherPowerDamage(t, battleengine.EnvironmentSnapshot{Weather: rain}, []battleengine.WeatherPowerMultiplier{{
		Weather: battleengine.WeatherKindRain, Numerator: 2, Denominator: 1,
	}})
	if suppressedDamage != baseDamage {
		t.Fatalf("天气封锁下的降雨威力倍率伤害 = %d，基础伤害 = %d；期望封锁后回退基础威力", suppressedDamage, baseDamage)
	}
}

// resolveSuppressedWeatherPowerDamage 在场上存在天气封锁特性时返回测试技能的实际伤害。
//
// 此辅助仅改变成员冻结的特性规则，技能、随机源和环境均保持与普通倍率测试一致，从公开 ResolveTurn 边界
// 验证封锁会统一阻断普通天气威力读取。
func resolveSuppressedWeatherPowerDamage(
	t *testing.T,
	environment battleengine.EnvironmentSnapshot,
	multipliers []battleengine.WeatherPowerMultiplier,
) uint32 {
	t.Helper()
	attacker := newMember(1, "weather-power-suppressor", 1_000, 1_000)
	attacker.Stats.Speed = 200
	attacker.ElementIDs = testIDs("normal")
	attacker.WeatherEffectsSuppressed = true
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("weather-power-suppressed-damage"), Name: "天气封锁威力", ElementID: testID("normal"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5, WeatherPowerMultipliers: multipliers,
	}
	defender := newMember(1, "weather-power-suppressed-defender", 1_000, 1_000)
	defender.ElementIDs = testIDs("normal")
	defender.Skills[0].Power = 1
	state := newWeatherState(t, environment, battleengine.RuleSnapshot{
		SchemaVersion: 1, ElementIDs: map[string]Identifier{"normal": testID("normal")},
	}, attacker, defender)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{
		useSkillCommand(battleengine.SideOne, battleengine.SideTwo), useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
	}}, mustRandom(t, 5))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for _, event := range result.Events {
		if damage, ok := event.(battleengine.DamageAppliedEvent); ok && damage.Actor.Side == battleengine.SideOne &&
			damage.SkillID == testID("weather-power-suppressed-damage") {
			return damage.Amount
		}
	}
	t.Fatalf("天气封锁威力测试技能没有产生伤害事件: %+v", result.Events)
	return 0
}

// resolveWeatherPowerDamage 返回测试技能造成的实际伤害；测试只通过 Battle Engine 的公开 ResolveTurn 边界观察
// 资料倍率，不依赖伤害公式的内部辅助函数。
func resolveWeatherPowerDamage(
	t *testing.T,
	environment battleengine.EnvironmentSnapshot,
	multipliers []battleengine.WeatherPowerMultiplier,
) uint32 {
	t.Helper()
	attacker := newMember(1, "weather-power-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 200
	attacker.ElementIDs = testIDs("normal")
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("weather-power-damage"), Name: "天气威力", ElementID: testID("normal"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5, WeatherPowerMultipliers: multipliers,
	}
	defender := newMember(1, "weather-power-defender", 1_000, 1_000)
	defender.ElementIDs = testIDs("normal")
	defender.Skills[0].Power = 1
	state := newWeatherState(t, environment, battleengine.RuleSnapshot{
		SchemaVersion: 1, ElementIDs: map[string]Identifier{"normal": testID("normal")},
	}, attacker, defender)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 5)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{
		useSkillCommand(battleengine.SideOne, battleengine.SideTwo), useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
	}}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for _, event := range result.Events {
		if damage, ok := event.(battleengine.DamageAppliedEvent); ok && damage.Actor.Side == battleengine.SideOne &&
			damage.SkillID == testID("weather-power-damage") {
			return damage.Amount
		}
	}
	t.Fatalf("天气威力测试技能没有产生伤害事件: %+v", result.Events)
	return 0
}
