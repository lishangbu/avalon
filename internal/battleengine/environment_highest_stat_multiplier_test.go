package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnEnvironmentHighestStatMultiplier 验证环境特性只强化五项原始能力中按固定优先级选出的最高项。
//
// 测试通过公开回合边界观察伤害与行动顺序，不依赖引擎内部辅助函数；这样资料冻结后的实际结算顺序、天气封锁
// 和整型截断都能一起受到回归保护。
func TestResolveTurnEnvironmentHighestStatMultiplier(t *testing.T) {
	t.Parallel()

	t.Run("匹配天气强化最高攻击", func(t *testing.T) {
		baseDamage := environmentHighestStatDamage(t, nil, false)
		weather := battleengine.WeatherKindSun
		boostedDamage := environmentHighestStatDamage(t, &battleengine.EnvironmentHighestStatMultiplier{
			RequiredWeather: weather,
		}, false)
		if boostedDamage <= baseDamage {
			t.Fatalf("日照最高攻击强化伤害 = %d，基础伤害 = %d；期望匹配环境提高物理伤害", boostedDamage, baseDamage)
		}
	})

	t.Run("不匹配环境和天气封锁都不强化", func(t *testing.T) {
		sun := battleengine.WeatherKindSun
		rain := battleengine.WeatherKindRain
		baseDamage := environmentHighestStatDamage(t, nil, false)
		unmatchedDamage := environmentHighestStatDamage(t, &battleengine.EnvironmentHighestStatMultiplier{
			RequiredWeather: rain,
		}, false)
		if unmatchedDamage != baseDamage {
			t.Fatalf("不匹配天气伤害 = %d，基础伤害 = %d；期望不产生强化", unmatchedDamage, baseDamage)
		}
		suppressedDamage := environmentHighestStatDamage(t, &battleengine.EnvironmentHighestStatMultiplier{
			RequiredWeather: sun,
		}, true)
		if suppressedDamage != baseDamage {
			t.Fatalf("天气封锁时伤害 = %d，基础伤害 = %d；期望封锁天气触发的最高能力强化", suppressedDamage, baseDamage)
		}
	})

	t.Run("电气场地最高速度改变行动顺序", func(t *testing.T) {
		boosted := newMember(1, "environment-highest-speed", 500, 500)
		boosted.Stats.Attack = 70
		boosted.Stats.Defense = 80
		boosted.Stats.SpecialAttack = 90
		boosted.Stats.SpecialDefense = 95
		boosted.Stats.Speed = 100
		boosted.EnvironmentHighestStatMultiplier = &battleengine.EnvironmentHighestStatMultiplier{
			RequiredTerrain: battleengine.TerrainKindElectric,
		}
		observer := newMember(1, "environment-highest-speed-observer", 500, 500)
		observer.Stats.Speed = 125
		state := newWeatherState(t, battleengine.EnvironmentSnapshot{
			Terrain: &battleengine.TerrainEffect{Kind: battleengine.TerrainKindElectric, TurnsRemaining: 2},
		}, battleengine.RuleSnapshot{SchemaVersion: 1}, boosted, observer)
		result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
			fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
			fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
		), mustRandom(t, 251))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		first, found := findFirstDamage(result.Events)
		if !found || first.Actor.Side != battleengine.SideOne {
			t.Fatalf("首个伤害事件 = %+v，期望场地强化后的左方先行动", first)
		}
	})

	t.Run("并列最高能力按攻击优先级决胜", func(t *testing.T) {
		attacker := newMember(1, "environment-highest-tie-attacker", 500, 500)
		attacker.Stats.Attack = 100
		attacker.Stats.Speed = 100
		attacker.Stats.Defense = 80
		attacker.Stats.SpecialAttack = 80
		attacker.Stats.SpecialDefense = 80
		attacker.EnvironmentHighestStatMultiplier = &battleengine.EnvironmentHighestStatMultiplier{
			RequiredTerrain: battleengine.TerrainKindElectric,
		}
		observer := newMember(1, "environment-highest-tie-observer", 500, 500)
		observer.Stats.Speed = 120
		state := newWeatherState(t, battleengine.EnvironmentSnapshot{
			Terrain: &battleengine.TerrainEffect{Kind: battleengine.TerrainKindElectric, TurnsRemaining: 2},
		}, battleengine.RuleSnapshot{SchemaVersion: 1}, attacker, observer)
		result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
			fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
			fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
		), mustRandom(t, 257))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		first, found := findFirstDamage(result.Events)
		if !found || first.Actor.Side != battleengine.SideTwo {
			t.Fatalf("并列攻击/速度时首个伤害事件 = %+v，期望攻击优先而速度不强化", first)
		}
	})
}

// environmentHighestStatDamage 返回环境最高原始能力规则下左方普通物理技能的实际伤害。
//
// 仅使用普通属性与日照，隔离天气自身的属性伤害修正；最高攻击在规则出现时由 13/10 的固定倍率提高，
// `suppressed` 则让右方成员提供天气封锁，以验证天气触发条件读取的是有效天气而非原始环境字段。
func environmentHighestStatDamage(
	t *testing.T,
	rule *battleengine.EnvironmentHighestStatMultiplier,
	suppressed bool,
) uint32 {
	t.Helper()
	attacker := newMember(1, "environment-highest-attacker", 1_000, 1_000)
	attacker.Stats.Attack = 200
	attacker.Stats.Defense = 100
	attacker.Stats.SpecialAttack = 100
	attacker.Stats.SpecialDefense = 100
	attacker.Stats.Speed = 200
	attacker.ElementIDs = testIDs("normal")
	attacker.EnvironmentHighestStatMultiplier = rule
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("environment-highest-damage"), Name: "环境最高能力伤害",
		ElementID: testID("normal"), DamageClass: battleengine.DamageClassPhysical,
		TargetScope: battleengine.SkillTargetScopeSelectedTarget, Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
	}
	defender := newMember(1, "environment-highest-defender", 1_000, 1_000)
	defender.ElementIDs = testIDs("normal")
	defender.Skills[0].Power = 1
	defender.WeatherEffectsSuppressed = suppressed
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{
		Weather: &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSun, TurnsRemaining: 3},
	}, battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]battleengine.Identifier{"normal": testID("normal")}}, attacker, defender)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{
		useSkillCommand(battleengine.SideOne, battleengine.SideTwo), useSkillCommand(battleengine.SideTwo, battleengine.SideOne),
	}}, mustRandom(t, 241))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for _, event := range result.Events {
		if damage, ok := event.(battleengine.DamageAppliedEvent); ok && damage.Actor.Side == battleengine.SideOne &&
			damage.SkillID == testID("environment-highest-damage") {
			return damage.Amount
		}
	}
	t.Fatalf("环境最高能力测试技能没有产生伤害事件: %+v", result.Events)
	return 0
}
