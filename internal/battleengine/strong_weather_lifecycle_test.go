package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStrongWeatherUsesEffectiveSpeedOrder 验证初始上场特性按有效速度从快到慢触发，
// 因此后触发的较慢成员会成为最终强天气来源；阵营数组顺序不能覆盖这一公开规则。
func TestInitialStrongWeatherUsesEffectiveSpeedOrder(t *testing.T) {
	t.Parallel()
	slow := strongWeatherMember(1, "slow-heavy-rain", 80, battleengine.StrongWeatherKindHeavyRain)
	fast := strongWeatherMember(1, "fast-harsh-sunlight", 120, battleengine.StrongWeatherKindHarshSunlight)
	state := newStrongWeatherSinglesState(t, battleengine.EnvironmentSnapshot{}, slow, fast)
	assertStrongWeather(t, state.Snapshot().Environment, battleengine.StrongWeatherKindHeavyRain, battleengine.MemberRef{
		Side: battleengine.SideOne, Position: 1,
	})
}

// TestInitialStrongWeatherUsesReversedSpeedOrder 验证戏法空间反转初始入场特性的速度比较方向，
// 较快成员在反转顺序中最后触发并保留其强天气。
func TestInitialStrongWeatherUsesReversedSpeedOrder(t *testing.T) {
	t.Parallel()
	slow := strongWeatherMember(1, "slow-heavy-rain", 80, battleengine.StrongWeatherKindHeavyRain)
	fast := strongWeatherMember(1, "fast-harsh-sunlight", 120, battleengine.StrongWeatherKindHarshSunlight)
	state := newStrongWeatherSinglesState(t, battleengine.EnvironmentSnapshot{FieldSpeedOrder: &battleengine.FieldSpeedOrderEffect{
		Kind: battleengine.FieldSpeedOrderKindTrickRoom, TurnsRemaining: 3,
	}}, slow, fast)
	assertStrongWeather(t, state.Snapshot().Environment, battleengine.StrongWeatherKindHarshSunlight, battleengine.MemberRef{
		Side: battleengine.SideTwo, Position: 1,
	})
}

// TestInitialStrongWeatherKeepsStableTieOrder 验证初始有效速度相同时保持冻结阵营与席位顺序，
// 后触发的第二方成员稳定覆盖强天气，不能引入随机消费。
func TestInitialStrongWeatherKeepsStableTieOrder(t *testing.T) {
	t.Parallel()
	first := strongWeatherMember(1, "tied-heavy-rain", 100, battleengine.StrongWeatherKindHeavyRain)
	second := strongWeatherMember(1, "tied-strong-winds", 100, battleengine.StrongWeatherKindStrongWinds)
	state := newStrongWeatherSinglesState(t, battleengine.EnvironmentSnapshot{}, first, second)
	assertStrongWeather(t, state.Snapshot().Environment, battleengine.StrongWeatherKindStrongWinds, battleengine.MemberRef{
		Side: battleengine.SideTwo, Position: 1,
	})
}

// TestResolveTurnSwitchInStrongWeatherOverridesCurrentSource 验证实际换入的强天气特性覆盖现有强天气、
// 更新来源并发布结构化开始事件；覆盖过程不借用普通天气持续回合。
func TestResolveTurnSwitchInStrongWeatherOverridesCurrentSource(t *testing.T) {
	t.Parallel()
	current := strongWeatherMember(1, "current-harsh-sunlight", 100, battleengine.StrongWeatherKindHarshSunlight)
	incoming := strongWeatherMember(2, "incoming-heavy-rain", 100, battleengine.StrongWeatherKindHeavyRain)
	opponent := sleepingStrongWeatherSource("strong-weather-switch-observer", "")
	state := newStrongWeatherSwitchState(t, current, incoming, opponent)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			switchStrongWeatherAction(battleengine.SideOne, 1, 2),
			strongWeatherUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		},
	}, mustRandom(t, 271))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	assertStrongWeather(t, result.State.Snapshot().Environment, battleengine.StrongWeatherKindHeavyRain, battleengine.MemberRef{
		Side: battleengine.SideOne, Position: 2,
	})
	if !containsStrongWeatherStarted(result.Events, battleengine.StrongWeatherKindHeavyRain, battleengine.MemberRef{Side: battleengine.SideOne, Position: 2}) {
		t.Fatalf("换入强降雨缺少正确开始事件: %+v", result.Events)
	}
}

// TestResolveTurnStrongWeatherEndsWhenSourceSwitchesOut 验证最后来源成功换出后立即结束强天气，
// 结束事件位于该换人行动内，最终环境不保留失效来源。
func TestResolveTurnStrongWeatherEndsWhenSourceSwitchesOut(t *testing.T) {
	t.Parallel()
	source := strongWeatherMember(1, "switching-heavy-rain", 100, battleengine.StrongWeatherKindHeavyRain)
	reserve := strongWeatherMember(2, "plain-reserve", 100, "")
	opponent := sleepingStrongWeatherSource("switch-out-observer", "")
	state := newStrongWeatherSwitchState(t, source, reserve, opponent)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			switchStrongWeatherAction(battleengine.SideOne, 1, 2),
			strongWeatherUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		},
	}, mustRandom(t, 277))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if result.State.Snapshot().Environment.StrongWeather != nil || !containsEvent(result.Events, battleengine.EventKindStrongWeatherEnded) {
		t.Fatalf("最后来源换出后的强天气或事件 = environment:%+v events:%+v", result.State.Snapshot().Environment, result.Events)
	}
}

// TestResolveTurnStrongWeatherEndsWhenSourceFaints 验证最后来源被技能伤害击倒后在下一行动前结束强天气，
// 不能让已倒下来源继续影响随后同回合结算。
func TestResolveTurnStrongWeatherEndsWhenSourceFaints(t *testing.T) {
	t.Parallel()
	attacker := strongWeatherAttacker(1, "strong-weather-finisher", 200)
	source := strongWeatherMember(1, "fainted-harsh-sunlight", 100, battleengine.StrongWeatherKindHarshSunlight)
	source.CurrentHP = 1
	reserve := strongWeatherMember(2, "fainted-source-reserve", 90, "")
	state := newStrongWeatherFaintState(t, []battleengine.MemberSnapshot{attacker}, []battleengine.MemberSnapshot{source, reserve})
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		strongWeatherUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		strongWeatherUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
	), mustRandom(t, 281))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if result.State.Snapshot().Environment.StrongWeather != nil || !containsEvent(result.Events, battleengine.EventKindStrongWeatherEnded) {
		t.Fatalf("最后来源倒下后的强天气或事件 = environment:%+v events:%+v", result.State.Snapshot().Environment, result.Events)
	}
}

// TestResolveTurnStrongWeatherTransfersToSameKindHolder 验证当前来源倒下后，同类强天气的其它存活上场
// 持有者接管来源而不让环境出现空窗。
func TestResolveTurnStrongWeatherTransfersToSameKindHolder(t *testing.T) {
	t.Parallel()
	result := resolveStrongWeatherHolderTakeover(t, battleengine.StrongWeatherKindHeavyRain, battleengine.StrongWeatherKindHeavyRain)
	assertStrongWeather(t, result.State.Snapshot().Environment, battleengine.StrongWeatherKindHeavyRain, battleengine.MemberRef{
		Side: battleengine.SideTwo, Position: 2,
	})
}

// TestResolveTurnStrongWeatherTransfersToDifferentKindHolder 验证当前来源倒下后，仍在场的另一种强天气
// 持有者按稳定顺序建立自己的天气并成为新来源。
func TestResolveTurnStrongWeatherTransfersToDifferentKindHolder(t *testing.T) {
	t.Parallel()
	result := resolveStrongWeatherHolderTakeover(t, battleengine.StrongWeatherKindHeavyRain, battleengine.StrongWeatherKindStrongWinds)
	assertStrongWeather(t, result.State.Snapshot().Environment, battleengine.StrongWeatherKindStrongWinds, battleengine.MemberRef{
		Side: battleengine.SideTwo, Position: 2,
	})
}

// TestResolveTurnStrongWeatherDoesNotExpireAtEndOfTurn 验证强天气没有普通天气持续回合，
// 回合末环境推进不会递减或自然结束仍有存活来源维持的强天气。
func TestResolveTurnStrongWeatherDoesNotExpireAtEndOfTurn(t *testing.T) {
	t.Parallel()
	first := sleepingStrongWeatherSource("persistent-heavy-rain", battleengine.StrongWeatherKindHeavyRain)
	second := sleepingStrongWeatherSource("persistent-observer", "")
	state := newStrongWeatherSinglesState(t, battleengine.EnvironmentSnapshot{}, first, second)
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		strongWeatherUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		strongWeatherUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
	), mustRandom(t, 283))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	assertStrongWeather(t, result.State.Snapshot().Environment, battleengine.StrongWeatherKindHeavyRain, battleengine.MemberRef{
		Side: battleengine.SideOne, Position: 1,
	})
	if containsEvent(result.Events, battleengine.EventKindStrongWeatherEnded) {
		t.Fatalf("仍有来源时强天气不应自然结束: %+v", result.Events)
	}
}

// TestResolveTurnWeatherSuppressionPausesStrongWeatherWithoutDeletingIt 验证天气封锁只暂停强天气的可执行
// 阻止规则：强日照下水属性伤害仍能结算，但权威强天气种类与来源保持不变。
func TestResolveTurnWeatherSuppressionPausesStrongWeatherWithoutDeletingIt(t *testing.T) {
	t.Parallel()
	suppressor := strongWeatherAttacker(1, "strong-weather-suppressor", 200)
	suppressor.WeatherEffectsSuppressed = true
	suppressor.Skills[0].ElementID = testID("water")
	source := sleepingStrongWeatherSource("suppressed-harsh-sunlight", battleengine.StrongWeatherKindHarshSunlight)
	state := newStrongWeatherSinglesState(t, battleengine.EnvironmentSnapshot{}, suppressor, source)
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		strongWeatherUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		strongWeatherUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
	), mustRandom(t, 293))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if containsStrongWeatherSkillFailure(result.Events, battleengine.SkillFailureReasonStrongWeatherNegatesDamagingSkill) {
		t.Fatalf("天气封锁存在时强日照仍阻止了水属性技能: %+v", result.Events)
	}
	target, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || target.CurrentHP >= target.MaxHP {
		t.Fatalf("天气封锁下水属性技能未造成伤害: %+v", target)
	}
	assertStrongWeather(t, result.State.Snapshot().Environment, battleengine.StrongWeatherKindHarshSunlight, battleengine.MemberRef{
		Side: battleengine.SideTwo, Position: 1,
	})
}

// strongWeatherMember 构造带指定入场强天气和有效速度的最小成员快照。
func strongWeatherMember(position battleengine.MemberPosition, creatureID string, speed uint32, weather battleengine.StrongWeatherKind) battleengine.MemberSnapshot {
	member := newMember(position, creatureID, 1_000, 1_000)
	member.Stats.Speed = speed
	member.SwitchInStrongWeather = weather
	return member
}

// strongWeatherAttacker 构造能够稳定击倒低生命目标的普通伤害使用者。
func strongWeatherAttacker(position battleengine.MemberPosition, creatureID string, speed uint32) battleengine.MemberSnapshot {
	member := strongWeatherMember(position, creatureID, speed, "")
	member.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID(creatureID + "-skill"), Name: "强天气生命周期攻击", ElementID: testID("neutral"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 250, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
	}
	return member
}

// newStrongWeatherSinglesState 创建只包含强天气生命周期所需属性代码的单打状态。
func newStrongWeatherSinglesState(
	t *testing.T,
	environment battleengine.EnvironmentSnapshot,
	first battleengine.MemberSnapshot,
	second battleengine.MemberSnapshot,
) battleengine.State {
	t.Helper()
	state, err := battleengine.NewState(battleengine.InitialState{
		Format:      battleengine.FormatSnapshot{Code: "strong-weather-lifecycle", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:       battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"water": testID("water"), "fire": testID("fire"), "flying": testID("flying")}},
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

// newStrongWeatherSwitchState 创建单方拥有一名后备成员的强天气换人状态。
func newStrongWeatherSwitchState(
	t *testing.T,
	current battleengine.MemberSnapshot,
	reserve battleengine.MemberSnapshot,
	opponent battleengine.MemberSnapshot,
) battleengine.State {
	t.Helper()
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "strong-weather-switch-lifecycle", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"water": testID("water"), "fire": testID("fire"), "flying": testID("flying")}},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{current, reserve}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// newStrongWeatherFaintState 创建允许一方保留后备或双打伙伴的强天气倒下状态。
func newStrongWeatherFaintState(
	t *testing.T,
	first []battleengine.MemberSnapshot,
	second []battleengine.MemberSnapshot,
) battleengine.State {
	t.Helper()
	firstActive := make([]battleengine.MemberPosition, min(2, len(first)))
	for index := range firstActive {
		firstActive[index] = battleengine.MemberPosition(index + 1)
	}
	secondActive := make([]battleengine.MemberPosition, min(2, len(second)))
	for index := range secondActive {
		secondActive[index] = battleengine.MemberPosition(index + 1)
	}
	activeSlots := min(len(firstActive), len(secondActive))
	firstActive = firstActive[:activeSlots]
	secondActive = secondActive[:activeSlots]
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "strong-weather-faint-lifecycle", ActiveSlotsPerSide: battleengine.SlotPosition(activeSlots), TeamSize: uint8(max(len(first), len(second)))},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"water": testID("water"), "fire": testID("fire"), "flying": testID("flying")}},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: firstActive, Members: first},
			{Side: battleengine.SideTwo, ActiveMembers: secondActive, Members: second},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// resolveStrongWeatherHolderTakeover 使第二方第一槽来源倒下，并返回另一持有者接管后的回合结果。
func resolveStrongWeatherHolderTakeover(
	t *testing.T,
	sourceWeather battleengine.StrongWeatherKind,
	holderWeather battleengine.StrongWeatherKind,
) battleengine.TurnResult {
	t.Helper()
	attacker := strongWeatherAttacker(1, "holder-takeover-attacker", 220)
	attackerPartner := sleepingStrongWeatherSource("holder-takeover-attacker-partner", "")
	attackerPartner.Position = 2
	source := strongWeatherMember(1, "holder-takeover-source", 70, sourceWeather)
	source.CurrentHP = 1
	holder := sleepingStrongWeatherSource("holder-takeover-holder", holderWeather)
	holder.Position = 2
	holder.Stats.Speed = 140
	state := newStrongWeatherFaintState(t,
		[]battleengine.MemberSnapshot{attacker, attackerPartner},
		[]battleengine.MemberSnapshot{source, holder},
	)
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			strongWeatherUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
			strongWeatherUseSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 2),
			strongWeatherUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
			strongWeatherUseSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 2),
		},
	}, mustRandom(t, 307))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	return result
}

// switchStrongWeatherAction 构造单个场上槽位换入指定后备成员的命令。
func switchStrongWeatherAction(side battleengine.Side, slot battleengine.SlotPosition, member battleengine.MemberPosition) battleengine.Action {
	return battleengine.Action{
		Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: side, Position: slot},
		Switch: &battleengine.SwitchAction{MemberPosition: member},
	}
}

// strongWeatherUseSkillAction 构造强天气生命周期测试使用的显式槽位技能行动。
func strongWeatherUseSkillAction(
	actorSide battleengine.Side,
	actorSlot battleengine.SlotPosition,
	targetSide battleengine.Side,
	targetSlot battleengine.SlotPosition,
) battleengine.Action {
	return battleengine.Action{
		Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: actorSide, Position: actorSlot},
		UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: targetSide, Position: targetSlot}},
	}
}

// assertStrongWeather 断言环境中的强天气种类和稳定成员来源完全匹配。
func assertStrongWeather(
	t *testing.T,
	environment battleengine.EnvironmentSnapshot,
	wantKind battleengine.StrongWeatherKind,
	wantSource battleengine.MemberRef,
) {
	t.Helper()
	if environment.StrongWeather == nil || environment.StrongWeather.Kind != wantKind || environment.StrongWeather.Source != wantSource {
		t.Fatalf("强天气 = %+v，期望 kind=%q source=%+v", environment.StrongWeather, wantKind, wantSource)
	}
}

// containsStrongWeatherStarted 报告事件流是否包含指定种类与来源的强天气开始事件。
func containsStrongWeatherStarted(events []battleengine.Event, kind battleengine.StrongWeatherKind, source battleengine.MemberRef) bool {
	for _, event := range events {
		started, ok := event.(battleengine.StrongWeatherStartedEvent)
		if ok && started.StrongWeather == kind && started.Source == source {
			return true
		}
	}
	return false
}
