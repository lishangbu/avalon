package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnWeatherElementOverridesUseEffectiveElement 验证匹配天气下的替换属性同时驱动属性相性、同属性
// 加成、天气和场地伤害修正；这些机制必须读取同一个有效属性，而不是在不同公式分支回退技能基础属性。
func TestResolveTurnWeatherElementOverridesUseEffectiveElement(t *testing.T) {
	t.Parallel()

	rain := &battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 3}
	baseRainDamage := resolveWeatherElementDamage(t, battleengine.EnvironmentSnapshot{Weather: rain}, nil, "water", "fire", nil)
	waterOverrideDamage := resolveWeatherElementDamage(t, battleengine.EnvironmentSnapshot{Weather: rain}, []battleengine.WeatherElementOverride{{
		Weather: battleengine.WeatherKindRain, ElementID: testID("water"),
	}}, "water", "fire", []battleengine.ElementEffectiveness{{
		AttackElementID: testID("water"), DefenseElementID: testID("fire"), Numerator: 2, Denominator: 1,
	}})
	if waterOverrideDamage <= baseRainDamage {
		t.Fatalf("降雨水属性覆盖伤害 = %d，基础火属性伤害 = %d；期望相性、同属性加成和降雨修正均使用覆盖属性", waterOverrideDamage, baseRainDamage)
	}

	electricTerrain := &battleengine.TerrainEffect{Kind: battleengine.TerrainKindElectric, TurnsRemaining: 3}
	baseTerrainDamage := resolveWeatherElementDamage(t, battleengine.EnvironmentSnapshot{Weather: rain, Terrain: electricTerrain}, nil, "electric", "normal", nil)
	electricOverrideDamage := resolveWeatherElementDamage(t, battleengine.EnvironmentSnapshot{Weather: rain, Terrain: electricTerrain}, []battleengine.WeatherElementOverride{{
		Weather: battleengine.WeatherKindRain, ElementID: testID("electric"),
	}}, "electric", "normal", nil)
	if electricOverrideDamage <= baseTerrainDamage {
		t.Fatalf("电气场地下的电属性覆盖伤害 = %d，基础火属性伤害 = %d；期望场地修正使用覆盖属性", electricOverrideDamage, baseTerrainDamage)
	}
}

// TestResolveTurnWeatherElementOverridesFallBackAndAffectOneHitKnockOut 验证未匹配天气严格回退基础属性，并且
// 一击必杀同属性目标阻止规则读取天气后的有效属性，避免该专用分支与普通伤害公式出现属性判断分叉。
func TestResolveTurnWeatherElementOverridesFallBackAndAffectOneHitKnockOut(t *testing.T) {
	t.Parallel()

	sun := &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSun, TurnsRemaining: 3}
	baseSunDamage := resolveWeatherElementDamage(t, battleengine.EnvironmentSnapshot{Weather: sun}, nil, "water", "normal", nil)
	unmatchedOverrideDamage := resolveWeatherElementDamage(t, battleengine.EnvironmentSnapshot{Weather: sun}, []battleengine.WeatherElementOverride{{
		Weather: battleengine.WeatherKindRain, ElementID: testID("water"),
	}}, "water", "normal", nil)
	if unmatchedOverrideDamage != baseSunDamage {
		t.Fatalf("未匹配天气的属性覆盖伤害 = %d，基础属性伤害 = %d；期望严格回退基础属性", unmatchedOverrideDamage, baseSunDamage)
	}

	attacker := newMember(1, "weather-element-ohko-attacker", 100, 100)
	attacker.Stats.Speed = 200
	attacker.ElementIDs = testIDs("water")
	attacker.Skills[0] = battleengine.SkillSnapshot{MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("weather-element-ohko"), Name: "天气一击必杀", ElementID: testID("fire"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 5, MaxPP: 5,
		DamageMode:                 battleengine.SkillDamageModeOneHitKnockOut,
		OneHitKnockOutBaseAccuracy: 100, OneHitKnockOutBlocksSameElementTarget: true,
		WeatherElementOverrides: []battleengine.WeatherElementOverride{{Weather: battleengine.WeatherKindRain, ElementID: testID("water")}},
	}
	defender := newMember(1, "weather-element-ohko-defender", 100, 100)
	defender.ElementIDs = testIDs("water")
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 3}},
		battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"fire": testID("fire"), "water": testID("water")}}, attacker, defender)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
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
		if blocked, ok := event.(battleengine.SkillBlockedEvent); ok && blocked.Actor.Side == battleengine.SideOne &&
			blocked.Reason == battleengine.SkillBlockReasonOneHitKnockOutSameElementTarget {
			return
		}
	}
	t.Fatalf("天气属性覆盖后的一击必杀未被同属性目标阻止: %+v", result.Events)
}

// resolveWeatherElementDamage 返回第一方测试技能造成的伤害，并把环境、属性覆盖和相性表显式作为输入，确保
// 每个断言只依赖冻结资料而不从技能名称或测试夹具隐式推断战斗属性。
func resolveWeatherElementDamage(
	t *testing.T,
	environment battleengine.EnvironmentSnapshot,
	overrides []battleengine.WeatherElementOverride,
	attackerElementID, defenderElementID string,
	effectiveness []battleengine.ElementEffectiveness,
) uint32 {
	t.Helper()
	attacker := newMember(1, "weather-element-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 200
	attacker.ElementIDs = testIDs(attackerElementID)
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("weather-element-damage"), Name: "天气属性伤害", ElementID: testID("fire"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5, WeatherElementOverrides: overrides,
	}
	defender := newMember(1, "weather-element-defender", 1_000, 1_000)
	defender.ElementIDs = testIDs(defenderElementID)
	defender.Skills[0].Power = 1
	state := newWeatherState(t, environment, battleengine.RuleSnapshot{
		SchemaVersion: 1,
		ElementIDs: map[string]Identifier{
			"fire": testID("fire"), "water": testID("water"), "electric": testID("electric"), "normal": testID("normal"), "flying": testID("flying"),
		},
		ElementEffectiveness: effectiveness,
	}, attacker, defender)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 3)
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
		if damage, ok := event.(battleengine.DamageAppliedEvent); ok && damage.Actor.Side == battleengine.SideOne && damage.SkillID == testID("weather-element-damage") {
			return damage.Amount
		}
	}
	t.Fatalf("天气属性覆盖测试技能没有产生伤害事件: %+v", result.Events)
	return 0
}
