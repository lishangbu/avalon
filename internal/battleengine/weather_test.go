package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnStartsAndExpiresWeather 验证天气建立后在回合末递减，并在最后一个持续回合自然结束。
func TestResolveTurnStartsAndExpiresWeather(t *testing.T) {
	t.Parallel()

	setter := newMember(1, "weather-setter", 500, 500)
	setter.Stats.Speed = 200
	setter.Skills[0] = weatherSkill(1, battleengine.WeatherKindRain, 2)
	other := newMember(1, "weather-observer", 500, 500)
	other.Skills[0].Power = 1
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, setter, other)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 1)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	started, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideOne),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if effect := started.State.Snapshot().Environment.Weather; effect == nil || effect.Kind != battleengine.WeatherKindRain || effect.TurnsRemaining != 1 {
		t.Fatalf("weather after start = %+v", effect)
	}
	if !containsWeatherEvent(started.Events, battleengine.EventKindWeatherStarted) {
		t.Fatalf("weather start event missing: %v", started.Events)
	}

	expired, err := battleengine.ResolveTurn(started.State, fieldSpeedOrderTurn(2,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideOne),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), started.RandomSource)
	if err != nil {
		t.Fatalf("ResolveTurn() expiry error = %v", err)
	}
	if effect := expired.State.Snapshot().Environment.Weather; effect != nil {
		t.Fatalf("weather after expiry = %+v", effect)
	}
	if !containsWeatherEvent(expired.Events, battleengine.EventKindWeatherEnded) {
		t.Fatalf("weather end event missing: %v", expired.Events)
	}
}

// TestResolveTurnSandstormDamagesOnlyNonImmuneMembers 验证沙暴只伤害不具岩石、地面或钢属性的场上成员。
func TestResolveTurnSandstormDamagesOnlyNonImmuneMembers(t *testing.T) {
	t.Parallel()

	immune := newMember(1, "sandstorm-immune", 160, 160)
	normal := newMember(1, "sandstorm-normal", 160, 160)
	normal.Skills[0].Power = 1
	immune.ElementIDs = testIDs("rock")
	state := newWeatherState(t,
		battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 2}},
		battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"rock": testID("rock")}},
		immune,
		normal,
	)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 2)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	weatherDamageTargets := make([]battleengine.MemberRef, 0, 1)
	for _, event := range result.Events {
		if damage, ok := event.(battleengine.WeatherDamageAppliedEvent); ok {
			weatherDamageTargets = append(weatherDamageTargets, damage.Target)
		}
	}
	if len(weatherDamageTargets) != 1 || weatherDamageTargets[0] != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) {
		t.Fatalf("沙暴伤害目标 = %+v，期望仅伤害第二方非免疫成员", weatherDamageTargets)
	}
}

// TestResolveTurnSandstormRespectsAbilityWeatherDamageImmunity 验证特性冻结的天气伤害免疫是独立于岩石、地面和
// 钢属性天然免疫的规则；成员即使是普通属性，也能仅对声明的沙暴回合末伤害保持免疫。
func TestResolveTurnSandstormRespectsAbilityWeatherDamageImmunity(t *testing.T) {
	t.Parallel()
	immune := newMember(1, "ability-sandstorm-immune", 160, 160)
	immune.WeatherDamageImmunities = []battleengine.WeatherKind{battleengine.WeatherKindSandstorm}
	normal := newMember(1, "ability-sandstorm-normal", 160, 160)
	normal.Skills[0].Power = 1
	state := newWeatherState(t,
		battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 2}},
		battleengine.RuleSnapshot{SchemaVersion: 1}, immune, normal,
	)
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), mustRandom(t, 101))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for _, event := range result.Events {
		if damage, ok := event.(battleengine.WeatherDamageAppliedEvent); ok && damage.Target.Side == battleengine.SideOne {
			t.Fatalf("特性天气免疫成员仍受到沙暴伤害: %+v", damage)
		}
	}
}

// TestResolveTurnAbilityWeatherSuppressionSkipsEffectsButKeepsWeatherLifecycle 验证特性天气封锁只暂停普通天气的可执行
// 效果：沙暴伤害不应发生，但原始天气仍需在回合末递减，不能被错误清除或停止到期。
func TestResolveTurnAbilityWeatherSuppressionSkipsEffectsButKeepsWeatherLifecycle(t *testing.T) {
	t.Parallel()
	suppressor := newMember(1, "weather-suppressor", 160, 160)
	suppressor.WeatherEffectsSuppressed = true
	normal := newMember(1, "weather-suppressed-normal", 160, 160)
	normal.Skills[0].Power = 1
	state := newWeatherState(t,
		battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 2}},
		battleengine.RuleSnapshot{SchemaVersion: 1}, suppressor, normal,
	)
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), mustRandom(t, 103))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for _, event := range result.Events {
		if damage, ok := event.(battleengine.WeatherDamageAppliedEvent); ok {
			t.Fatalf("天气被封锁时仍产生沙暴伤害: %+v", damage)
		}
	}
	weather := result.State.Snapshot().Environment.Weather
	if weather == nil || weather.Kind != battleengine.WeatherKindSandstorm || weather.TurnsRemaining != 1 {
		t.Fatalf("天气封锁后天气生命周期 = %+v，期望保留并递减", weather)
	}
}

// TestResolveTurnAbilityWeatherEndTurnHealing 验证特性冻结的天气回合末回复只在匹配普通天气生效，并以最大生命的
// 固定整数比例结算。天气封锁存在时，回复必须同其它普通天气效果一起暂停，但原始天气仍照常递减。
func TestResolveTurnAbilityWeatherEndTurnHealing(t *testing.T) {
	t.Parallel()
	for _, test := range []struct {
		name       string
		suppressed bool
		wantEvents int
	}{
		{name: "匹配天气回复", wantEvents: 1},
		{name: "天气封锁暂停回复", suppressed: true, wantEvents: 0},
	} {
		t.Run(test.name, func(t *testing.T) {
			healer := newMember(1, "weather-healer", 160, 100)
			healer.Skills[0].Power = 1
			healer.WeatherEndTurnHealing = &battleengine.WeatherEndTurnHealing{
				Weathers: []battleengine.WeatherKind{battleengine.WeatherKindRain}, HealDenominator: 16,
			}
			suppressor := newMember(1, "weather-observer", 160, 160)
			suppressor.Skills[0].Power = 1
			suppressor.WeatherEffectsSuppressed = test.suppressed
			state := newWeatherState(t,
				battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 2}},
				battleengine.RuleSnapshot{SchemaVersion: 1}, healer, suppressor,
			)

			result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
				fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
				fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
			), mustRandom(t, 107))
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
			if !found || member.CurrentHP == 0 {
				t.Fatalf("天气回复后的成员 = %+v，期望成员仍可战斗", member)
			}
			events := 0
			for _, event := range result.Events {
				if healing, ok := event.(battleengine.WeatherHealingAppliedEvent); ok {
					events++
					if healing.Target != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}) || healing.Weather != battleengine.WeatherKindRain || healing.Amount != 10 || healing.CurrentHP != member.CurrentHP {
						t.Fatalf("天气回复事件 = %+v", healing)
					}
				}
			}
			if events != test.wantEvents {
				t.Fatalf("天气回复事件数量 = %d，期望 %d", events, test.wantEvents)
			}
			weather := result.State.Snapshot().Environment.Weather
			if weather == nil || weather.TurnsRemaining != 1 {
				t.Fatalf("天气回合末生命周期 = %+v，期望保留并递减", weather)
			}
		})
	}
}

// TestResolveTurnWeatherAdjustsFireAndWaterDamage 验证日照和降雨以当前环境的强类型天气状态影响火、水属性伤害，
// 而不是通过技能名称、资料显示文本或测试常量推断。
func TestResolveTurnWeatherAdjustsFireAndWaterDamage(t *testing.T) {
	t.Parallel()

	noWeatherFire := resolveWeatherDamage(t, nil, "fire")
	noWeatherWater := resolveWeatherDamage(t, nil, "water")
	sun := &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSun, TurnsRemaining: 2}
	rain := &battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 2}
	if actual := resolveWeatherDamage(t, sun, "fire"); actual <= noWeatherFire {
		t.Fatalf("日照火属性伤害 = %d，常规伤害 = %d；期望日照强化火属性", actual, noWeatherFire)
	}
	if actual := resolveWeatherDamage(t, sun, "water"); actual >= noWeatherWater {
		t.Fatalf("日照水属性伤害 = %d，常规伤害 = %d；期望日照削弱水属性", actual, noWeatherWater)
	}
	if actual := resolveWeatherDamage(t, rain, "water"); actual <= noWeatherWater {
		t.Fatalf("降雨水属性伤害 = %d，常规伤害 = %d；期望降雨强化水属性", actual, noWeatherWater)
	}
	if actual := resolveWeatherDamage(t, rain, "fire"); actual >= noWeatherFire {
		t.Fatalf("降雨火属性伤害 = %d，常规伤害 = %d；期望降雨削弱火属性", actual, noWeatherFire)
	}
}

// TestResolveTurnWeatherAdjustsDefenderStats 验证降雪的冰属性物防与沙暴的岩石属性特防在能力阶级之后进入伤害
// 公式；两项加成只能影响各自的伤害类别，不能被误用成无条件的全属性减伤。
func TestResolveTurnWeatherAdjustsDefenderStats(t *testing.T) {
	t.Parallel()

	noWeatherPhysical := resolveWeatherDamageAgainst(t, nil, "normal", battleengine.DamageClassPhysical, "ice")
	snowPhysical := resolveWeatherDamageAgainst(t, &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSnow, TurnsRemaining: 2}, "normal", battleengine.DamageClassPhysical, "ice")
	if snowPhysical >= noWeatherPhysical {
		t.Fatalf("降雪冰属性物防伤害 = %d，常规伤害 = %d；期望降雪降低物理伤害", snowPhysical, noWeatherPhysical)
	}
	noWeatherSpecial := resolveWeatherDamageAgainst(t, nil, "normal", battleengine.DamageClassSpecial, "rock")
	sandstormSpecial := resolveWeatherDamageAgainst(t, &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 2}, "normal", battleengine.DamageClassSpecial, "rock")
	if sandstormSpecial >= noWeatherSpecial {
		t.Fatalf("沙暴岩石属性特防伤害 = %d，常规伤害 = %d；期望沙暴降低特殊伤害", sandstormSpecial, noWeatherSpecial)
	}
}

func weatherSkill(position battleengine.SkillPosition, kind battleengine.WeatherKind, turns uint8) battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: position, SkillID: testID("weather-skill"), Name: "天气", ElementID: testID("weather-element"), DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf, Accuracy: 100, RemainingPP: 5, MaxPP: 5, WeatherApplication: &battleengine.WeatherApplication{Effect: battleengine.WeatherEffect{Kind: kind, TurnsRemaining: turns}, ChancePercent: 100}}
}

// newWeatherState 使用包含天气依赖属性代码的独立规则快照创建单打状态，避免借用其它效果测试的空规则快照。
func newWeatherState(
	t *testing.T,
	environment battleengine.EnvironmentSnapshot,
	rules battleengine.RuleSnapshot,
	first battleengine.MemberSnapshot,
	second battleengine.MemberSnapshot,
) battleengine.State {
	t.Helper()
	state, err := battleengine.NewState(battleengine.InitialState{
		Format:      battleengine.FormatSnapshot{Code: "weather", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:       rules,
		Environment: environment,
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{second}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// resolveWeatherDamage 返回第一方天气测试技能写入的直接伤害，用于比较同一随机序列下不同天气的倍率。
func resolveWeatherDamage(t *testing.T, weather *battleengine.WeatherEffect, elementID string) uint32 {
	return resolveWeatherDamageAgainst(t, weather, elementID, battleengine.DamageClassPhysical, "normal")
}

// resolveWeatherDamageAgainst 返回第一方天气测试技能造成的直接伤害，并允许精确指定伤害类别和防守方属性。
func resolveWeatherDamageAgainst(
	t *testing.T,
	weather *battleengine.WeatherEffect,
	elementID string,
	damageClass battleengine.DamageClass,
	defenderElementID string,
) uint32 {
	return resolveEnvironmentDamage(t, battleengine.EnvironmentSnapshot{Weather: weather}, testID(elementID), damageClass, testID(defenderElementID))
}

// resolveEnvironmentDamage 返回第一方测试技能在指定全场环境下造成的直接伤害，并允许精确指定伤害类别和防守方属性。
func resolveEnvironmentDamage(
	t *testing.T,
	environment battleengine.EnvironmentSnapshot,
	elementID Identifier,
	damageClass battleengine.DamageClass,
	defenderElementID Identifier,
) uint32 {
	return resolveEnvironmentDamageWithElements(t, environment, elementID, damageClass, defenderElementID, []Identifier{elementID})
}

// resolveEnvironmentDamageWithElements 返回测试技能在指定环境和使用者属性下造成的直接伤害，用于验证接地等成员条件。
func resolveEnvironmentDamageWithElements(
	t *testing.T,
	environment battleengine.EnvironmentSnapshot,
	elementID Identifier,
	damageClass battleengine.DamageClass,
	defenderElementID Identifier,
	attackerElementIDs []Identifier,
) uint32 {
	return resolveEnvironmentDamageWithSkillProperties(
		t, environment, elementID, damageClass, attackerElementIDs, []Identifier{defenderElementID}, false,
	)
}

// resolveEnvironmentDamageWithSkillProperties 返回带有显式技能属性与双方属性的测试技能在指定环境下造成的伤害。
//
// weakenedByGrassyTerrain 与技能属性刻意分开传入，以验证资料层的专用震动标签不会退化为根据地面属性或名称猜测的
// 规则；defenderElementIDs 则用于精确覆盖接地与非接地目标。
func resolveEnvironmentDamageWithSkillProperties(
	t *testing.T,
	environment battleengine.EnvironmentSnapshot,
	elementID Identifier,
	damageClass battleengine.DamageClass,
	attackerElementIDs, defenderElementIDs []Identifier,
	weakenedByGrassyTerrain bool,
) uint32 {
	t.Helper()
	attacker := newMember(1, "weather-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 200
	attacker.ElementIDs = append([]Identifier(nil), attackerElementIDs...)
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("weather-damage-skill"), Name: "天气伤害", ElementID: elementID,
		DamageClass: damageClass, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5, WeakenedByGrassyTerrain: weakenedByGrassyTerrain,
	}
	defender := newMember(1, "weather-defender", 1_000, 1_000)
	defender.ElementIDs = append([]Identifier(nil), defenderElementIDs...)
	defender.Skills[0].Power = 1
	state := newWeatherState(t, environment, battleengine.RuleSnapshot{
		SchemaVersion: 1, ElementIDs: map[string]Identifier{"fire": testID("fire"), "water": testID("water"), "ice": testID("ice"), "rock": testID("rock"), "electric": testID("electric"), "grass": testID("grass"), "psychic": testID("psychic"), "dragon": testID("dragon"), "flying": testID("flying")},
	}, attacker, defender)
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 3)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for _, event := range result.Events {
		if damage, ok := event.(battleengine.DamageAppliedEvent); ok && damage.Actor.Side == battleengine.SideOne && damage.SkillID == testID("weather-damage-skill") {
			return damage.Amount
		}
	}
	t.Fatalf("天气测试技能没有产生伤害事件: %+v", result.Events)
	return 0
}

func containsWeatherEvent(events []battleengine.Event, kind battleengine.EventKind) bool {
	for _, event := range events {
		if event.Kind() == kind {
			return true
		}
	}
	return false
}
