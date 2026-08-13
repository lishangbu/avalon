package battleengine_test

import (
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnHeavyRainBlocksFireDamage 验证强降雨使火属性伤害技能在命中、要害与伤害浮动之前失败。
// 失败路径必须发布稳定原因、保持目标生命不变并保留空随机轨迹，不能只把最终伤害乘成零。
func TestResolveTurnHeavyRainBlocksFireDamage(t *testing.T) {
	t.Parallel()
	attacker := newMember(1, "heavy-rain-fire-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 200
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("heavy-rain-fire"), Name: "火属性伤害", ElementID: testID("fire"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
	}
	source := sleepingStrongWeatherSource("heavy-rain-source", battleengine.StrongWeatherKindHeavyRain)
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{
		SchemaVersion: 1, ElementIDs: map[string]Identifier{"water": testID("water"), "fire": testID("fire")},
	}, attacker, source)
	random, err := battleengine.NewTracedRandom(nil)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if !containsStrongWeatherSkillFailure(result.Events, battleengine.SkillFailureReasonStrongWeatherNegatesDamagingSkill) {
		t.Fatalf("强降雨未阻止火属性伤害: %+v", result.Events)
	}
	target, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || target.CurrentHP != 1_000 || len(result.RandomTrace) != 0 {
		t.Fatalf("强降雨阻止后的状态或随机轨迹 = target:%+v trace:%+v", target, result.RandomTrace)
	}
}

// TestResolveTurnStrongWeatherAppliesOrdinaryWeatherDamageModifier 验证强日照与强降雨分别提供日照、降雨的普通
// 伤害倍率。固定轨迹下基础伤害为 37，匹配强天气后为 55；这项倍率与“相反属性直接失败”是两条独立规则。
func TestResolveTurnStrongWeatherAppliesOrdinaryWeatherDamageModifier(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name          string
		weather       battleengine.StrongWeatherKind
		elementID     Identifier
		wantClear     uint32
		wantWeathered uint32
	}{
		{name: "强日照强化火属性", weather: battleengine.StrongWeatherKindHarshSunlight, elementID: testID("fire"), wantClear: 37, wantWeathered: 55},
		{name: "强降雨强化水属性", weather: battleengine.StrongWeatherKindHeavyRain, elementID: testID("water"), wantClear: 37, wantWeathered: 55},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			clearDamage := resolveStrongWeatherElementDamage(t, "", test.elementID)
			weatheredDamage := resolveStrongWeatherElementDamage(t, test.weather, test.elementID)
			if clearDamage != test.wantClear || weatheredDamage != test.wantWeathered {
				t.Fatalf("普通天气倍率伤害 = clear:%d weathered:%d，期望 %d/%d", clearDamage, weatheredDamage, test.wantClear, test.wantWeathered)
			}
		})
	}
}

// TestResolveTurnStrongWeatherBlocksOppositeElementAndOrdinaryWeather 验证强日照同时建立等效日照、阻止水属性伤害，
// 并拒绝普通天气技能覆盖；这些规则都不能依赖特性名称或资料显示文本推断。
func TestResolveTurnStrongWeatherBlocksOppositeElementAndOrdinaryWeather(t *testing.T) {
	t.Parallel()
	source := newMember(1, "strong-sun-source", 1_000, 1_000)
	source.SwitchInStrongWeather = battleengine.StrongWeatherKindHarshSunlight
	attacker := newMember(1, "strong-sun-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 200
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("strong-sun-water"), Name: "水属性伤害", ElementID: testID("water"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
	}
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{
		SchemaVersion: 1, ElementIDs: map[string]Identifier{"water": testID("water"), "fire": testID("fire")},
	}, attacker, source)
	if actual := state.Snapshot().Environment.StrongWeather; actual == nil || actual.Kind != battleengine.StrongWeatherKindHarshSunlight {
		t.Fatalf("初始强天气 = %+v，期望强日照", actual)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), mustRandom(t, 211))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if !containsStrongWeatherSkillFailure(result.Events, battleengine.SkillFailureReasonStrongWeatherNegatesDamagingSkill) {
		t.Fatalf("强日照未阻止水属性伤害: %+v", result.Events)
	}
	member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || member.CurrentHP != member.MaxHP {
		t.Fatalf("被强日照阻止后的目标 = %+v，期望未受伤", member)
	}

	weatherSetter := attacker
	weatherSetter.Skills[0] = weatherSkill(1, battleengine.WeatherKindRain, 5)
	weatherState := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{
		SchemaVersion: 1, ElementIDs: map[string]Identifier{"water": testID("water"), "fire": testID("fire")},
	}, weatherSetter, source)
	weatherResult, err := battleengine.ResolveTurn(weatherState, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideOne),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), mustRandom(t, 223))
	if err != nil {
		t.Fatalf("ResolveTurn() weather error = %v", err)
	}
	if !containsStrongWeatherSkillFailure(weatherResult.Events, battleengine.SkillFailureReasonStrongWeatherActive) {
		t.Fatalf("强天气未阻止普通天气建立: %+v", weatherResult.Events)
	}
	environment := weatherResult.State.Snapshot().Environment
	if environment.Weather != nil || environment.StrongWeather == nil || environment.StrongWeather.Kind != battleengine.StrongWeatherKindHarshSunlight {
		t.Fatalf("普通天气覆盖后的环境 = %+v", environment)
	}
}

// TestResolveTurnStrongWeatherEndsWhenLastSourceSwitchesOut 验证来源离场后没有其它持有者时，强天气在同一行动
// 结束前清除并产生结构化结束事件，后续行动不会继续读取已失效环境。
func TestResolveTurnStrongWeatherEndsWhenLastSourceSwitchesOut(t *testing.T) {
	t.Parallel()
	source := newMember(1, "strong-weather-source", 1_000, 1_000)
	source.SwitchInStrongWeather = battleengine.StrongWeatherKindHeavyRain
	reserve := newMember(2, "strong-weather-reserve", 1_000, 1_000)
	opponent := newMember(1, "strong-weather-opponent", 1_000, 1_000)
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "strong-weather-switch", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"water": testID("water"), "fire": testID("fire")}},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source, reserve}},
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
	}, mustRandom(t, 227))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if !containsEvent(result.Events, battleengine.EventKindStrongWeatherEnded) {
		t.Fatalf("最后来源离场后缺少强天气结束事件: %+v", result.Events)
	}
	if actual := result.State.Snapshot().Environment.StrongWeather; actual != nil {
		t.Fatalf("最后来源离场后的强天气 = %+v，期望 nil", actual)
	}
}

// TestResolveTurnStrongWindsOnlyRemovesFlyingWeakness 验证强风仅移除飞行属性单项的弱点倍率，目标其它属性的
// 克制倍率仍然保留，不能把双属性目标整体误判为中性。
func TestResolveTurnStrongWindsOnlyRemovesFlyingWeakness(t *testing.T) {
	t.Parallel()
	clearDamage := resolveStrongWindsDamage(t, nil)
	strongWindsDamage := resolveStrongWindsDamage(t, &battleengine.StrongWeatherState{
		Kind: battleengine.StrongWeatherKindStrongWinds, Source: battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1},
	})
	if strongWindsDamage >= clearDamage || strongWindsDamage == 0 {
		t.Fatalf("强风伤害 = %d，常规双弱点伤害 = %d；期望只移除飞行弱点", strongWindsDamage, clearDamage)
	}
}

// resolveStrongWindsDamage 返回电属性攻击命中飞行/水双属性目标的伤害，用于比较强风前后的单项相性修正。
func resolveStrongWindsDamage(t *testing.T, strongWeather *battleengine.StrongWeatherState) uint32 {
	t.Helper()
	attacker := newMember(1, "strong-winds-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 200
	attacker.ElementIDs = testIDs("electric")
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("strong-winds-electric"), Name: "电属性伤害", ElementID: testID("electric"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
	}
	defender := newMember(1, "strong-winds-defender", 1_000, 1_000)
	defender.ElementIDs = testIDs("flying", "water")
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{StrongWeather: strongWeather}, battleengine.RuleSnapshot{
		SchemaVersion: 1,
		ElementIDs:    map[string]Identifier{"electric": testID("electric"), "flying": testID("flying"), "water": testID("water")},
		ElementEffectiveness: []battleengine.ElementEffectiveness{
			{AttackElementID: testID("electric"), DefenseElementID: testID("flying"), Numerator: 2, Denominator: 1},
			{AttackElementID: testID("electric"), DefenseElementID: testID("water"), Numerator: 2, Denominator: 1},
		},
	}, attacker, defender)
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), mustRandom(t, 229))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	for _, event := range result.Events {
		if damage, ok := event.(battleengine.DamageAppliedEvent); ok && damage.Actor.Side == battleengine.SideOne {
			return damage.Amount
		}
	}
	t.Fatalf("强风测试未产生伤害事件: %+v", result.Events)
	return 0
}

// sleepingStrongWeatherSource 构造一名仍能维持强天气、但在测试回合不会行动或消费随机数的来源成员。
func sleepingStrongWeatherSource(creatureID string, weather battleengine.StrongWeatherKind) battleengine.MemberSnapshot {
	source := newMember(1, creatureID, 1_000, 1_000)
	source.SwitchInStrongWeather = weather
	source.MajorStatus = battleengine.MajorStatusSleep
	source.SleepTurnsRemaining = 2
	return source
}

// resolveStrongWeatherElementDamage 在无本系加成、无属性相性且固定满伤害浮动下返回单段普通伤害。
// 显式重放轨迹同时固定强天气不得增加、删除或重排随机消费。
func resolveStrongWeatherElementDamage(
	t *testing.T,
	weather battleengine.StrongWeatherKind,
	elementID Identifier,
) uint32 {
	t.Helper()
	attacker := newMember(1, "strong-weather-element-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 200
	attacker.ElementIDs = testIDs("neutral")
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("strong-weather-element-skill"), Name: "强天气属性伤害", ElementID: elementID,
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
	}
	source := sleepingStrongWeatherSource("strong-weather-element-source", weather)
	if weather == "" {
		source.SwitchInStrongWeather = ""
	}
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{
		SchemaVersion: 1, ElementIDs: map[string]Identifier{"water": testID("water"), "fire": testID("fire")},
	}, attacker, source)
	trace := []battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("strong-weather-element-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("strong-weather-element-skill").String(), Value: 15},
	}
	random, err := battleengine.NewTracedRandom(trace)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		fieldSpeedOrderAction(battleengine.SideOne, 1, battleengine.SideTwo),
		fieldSpeedOrderAction(battleengine.SideTwo, 1, battleengine.SideOne),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if !reflect.DeepEqual(result.RandomTrace, trace) {
		t.Fatalf("强天气属性伤害随机轨迹 = %+v，期望 %+v", result.RandomTrace, trace)
	}
	for _, event := range result.Events {
		if damage, ok := event.(battleengine.DamageAppliedEvent); ok && damage.Actor.Side == battleengine.SideOne {
			return damage.Amount
		}
	}
	t.Fatalf("强天气属性伤害未产生伤害事件: %+v", result.Events)
	return 0
}

// containsStrongWeatherSkillFailure 报告事件流是否包含指定的强天气相关技能失败原因。
func containsStrongWeatherSkillFailure(events []battleengine.Event, reason battleengine.SkillFailureReason) bool {
	for _, event := range events {
		if failed, ok := event.(battleengine.SkillFailedEvent); ok && failed.Reason == reason {
			return true
		}
	}
	return false
}

// containsEvent 报告事件流是否包含指定的稳定事件种类。
func containsEvent(events []battleengine.Event, kind battleengine.EventKind) bool {
	for _, event := range events {
		if event.Kind() == kind {
			return true
		}
	}
	return false
}
