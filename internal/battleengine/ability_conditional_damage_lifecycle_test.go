package battleengine_test

import (
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnFullHPDamageReductionOnlyAppliesToFirstMultiHitSegment 验证规则 121 在每一段普通伤害
// 计算前重新读取目标当前生命；首段扣血后，后续段不得继续沿用回合开始时的满生命快照。
func TestResolveTurnFullHPDamageReductionOnlyAppliesToFirstMultiHitSegment(t *testing.T) {
	t.Parallel()
	actor := newMember(1, "full-hp-multi-hit-user", 1_000, 1_000)
	actor.Stats.Speed = 200
	actor.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, Position: 1, SkillID: testID("full-hp-multi-hit-skill"), Name: "满生命减伤多段验证", ElementID: testID("neutral"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5, MinHits: 2, MaxHits: 2,
	}
	target := sleepingStrongWeatherSource("full-hp-multi-hit-target", "")
	target.MaxHP, target.CurrentHP = 500, 500
	target.FullHPDamageReduction = &battleengine.FullHPDamageReduction{Numerator: 1, Denominator: 2}
	script := []battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("full-hp-multi-hit-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("full-hp-multi-hit-skill").String(), Value: 15},
		{Sequence: 3, Bound: 24, Reason: "critical hit for " + testID("full-hp-multi-hit-skill").String(), Value: 1},
		{Sequence: 4, Bound: 16, Reason: "damage random for " + testID("full-hp-multi-hit-skill").String(), Value: 15},
	}
	random, err := battleengine.NewTracedRandom(script)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "full-hp-multi-hit", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{actor}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		strongWeatherUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		strongWeatherUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	damages := make([]uint32, 0, 2)
	for _, event := range result.Events {
		if applied, ok := event.(battleengine.DamageAppliedEvent); ok && applied.SkillID == actor.Skills[0].SkillID {
			damages = append(damages, applied.Amount)
		}
	}
	if want := []uint32{18, 37}; !reflect.DeepEqual(damages, want) {
		t.Fatalf("逐段伤害 = %v，期望首段减伤、第二段恢复普通伤害 %v", damages, want)
	}
	updatedTarget, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || updatedTarget.CurrentHP != 445 || !reflect.DeepEqual(result.RandomTrace, script) {
		t.Fatalf("最终目标 = %+v, trace=%+v，期望生命 445 且随机轨迹完全匹配", updatedTarget, result.RandomTrace)
	}
}

// TestResolveTurnPulseBasedAbilityBoostsTargetHealing 验证规则 116 的波动类倍率不仅进入普通伤害公式，
// 还会在目标回复阶段按同一精确分数放大基础回复量，且不会引入任何额外随机消费。
func TestResolveTurnPulseBasedAbilityBoostsTargetHealing(t *testing.T) {
	t.Parallel()
	healer := newMember(1, "pulse-healer", 100, 100)
	healer.Stats.Speed = 200
	healer.PulseBasedSkillDamageBoost = &battleengine.PulseBasedSkillDamageBoost{Numerator: 3, Denominator: 2}
	healer.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("heal-pulse"), Name: "治愈波动", ElementID: testID("psychic"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 5, MaxPP: 5, PulseBased: true,
		TargetHealingNumerator: 1, TargetHealingDenominator: 2,
	}
	target := sleepingStrongWeatherSource("pulse-healing-target", "")
	target.MaxHP, target.CurrentHP = 100, 10
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "pulse-target-healing", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{healer}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		strongWeatherUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		strongWeatherUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
	), battleengine.RandomSource{})
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	updatedTarget, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || updatedTarget.CurrentHP != 85 || len(result.RandomTrace) != 0 {
		t.Fatalf("目标 = %+v, trace=%+v，期望 1/2 回复经 3/2 放大后生命为 85 且无随机轨迹", updatedTarget, result.RandomTrace)
	}
	foundHealingEvent := false
	for _, event := range result.Events {
		healing, ok := event.(battleengine.SkillHealingAppliedEvent)
		if ok && healing.Actor == (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) &&
			healing.SkillID == healer.Skills[0].SkillID && healing.Source == battleengine.SkillHealingSourceTargetMaximumHP &&
			healing.Amount == 75 && healing.CurrentHP == 85 {
			foundHealingEvent = true
		}
	}
	if !foundHealingEvent {
		t.Fatalf("缺少目标回复事件: %v", eventKinds(result.Events))
	}
}

// TestResolveTurnPulseHealingLargeMultiplierClampsWithoutOverflow 验证规则 116 面对 uint32 生命上限和最大
// 16 位倍率时仍先以宽整数计算再按缺失生命夹取，不能在转换回 uint32 时回绕成较小回复量。
func TestResolveTurnPulseHealingLargeMultiplierClampsWithoutOverflow(t *testing.T) {
	t.Parallel()
	healer := newMember(1, "pulse-overflow-healer", 100, 100)
	healer.Stats.Speed = 200
	healer.PulseBasedSkillDamageBoost = &battleengine.PulseBasedSkillDamageBoost{Numerator: ^uint16(0), Denominator: 1}
	healer.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("pulse-overflow-heal"), Name: "波动回复溢出边界", ElementID: testID("psychic"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 5, MaxPP: 5, PulseBased: true,
		TargetHealingNumerator: 1, TargetHealingDenominator: 2,
	}
	maximumHP := ^uint32(0)
	target := sleepingStrongWeatherSource("pulse-overflow-target", "")
	target.MaxHP, target.CurrentHP = maximumHP, 1
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "pulse-overflow", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{healer}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		strongWeatherUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		strongWeatherUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
	), battleengine.RandomSource{})
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	updatedTarget, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || updatedTarget.CurrentHP != maximumHP {
		t.Fatalf("大倍率回复后目标=%+v，期望安全夹取到最大生命 %d", updatedTarget, maximumHP)
	}
	wantAmount := maximumHP - 1
	foundHealing := false
	for _, event := range result.Events {
		if healing, ok := event.(battleengine.SkillHealingAppliedEvent); ok && healing.Amount == wantAmount {
			foundHealing = true
		}
	}
	if !foundHealing {
		t.Fatalf("缺少按缺失生命夹取的回复事件 amount=%d: %v", wantAmount, eventKinds(result.Events))
	}
}

// TestResolveTurnPunchBoostKeepsIndependentFromEffectiveContact 验证规则 113 的拳击标签不会因运行时接触
// 被抑制而消失；拳击增伤与接触反制读取的是两份彼此独立的冻结事实。
func TestResolveTurnPunchBoostKeepsIndependentFromEffectiveContact(t *testing.T) {
	t.Parallel()
	result := resolveAbilityDamageMultiplierTurn(t, func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
		skill.PunchBased = true
		skill.MakesContact = true
		actor.ContactSuppression = true
		actor.PunchBasedSkillDamageBoost = &battleengine.PunchBasedSkillDamageBoost{Numerator: 6, Denominator: 5}
	})
	if result.damage != 44 || !reflect.DeepEqual(result.trace, result.script) {
		t.Fatalf("拳击增伤结果 damage=%d trace=%+v，期望接触被抑制后仍造成 44 点伤害", result.damage, result.trace)
	}
}

// TestResolveTurnSecondaryEffectsSuppressionRemovesEveryStatusStatAndFlinchBranch 验证规则 118 在取得伤害
// 强化后会同时跳过目标主要异常、目标易变状态、使用者与目标能力变化以及道具追加畏缩；被跳过的概率接点
// 不得出现在随机轨迹中，伤害事件之后也不得出现任何被抑制效果事件。
func TestResolveTurnSecondaryEffectsSuppressionRemovesEveryStatusStatAndFlinchBranch(t *testing.T) {
	t.Parallel()
	actor := newMember(1, "secondary-suppression-user", 100, 100)
	actor.Stats.Speed = 200
	actor.ItemID = testID("additional-flinch-item")
	actor.HeldItemAdditionalFlinchChancePercent = 100
	actor.SecondaryEffectsSuppressedDamageBoost = &battleengine.SecondaryEffectsSuppressedDamageBoost{Numerator: 13, Denominator: 10}
	actor.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("secondary-suppression-skill"), Name: "追加效果全抑制验证", ElementID: testID("neutral"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
		StatusApplications: []battleengine.MajorStatusApplication{{
			Status: battleengine.MajorStatusPoison, Target: battleengine.EffectTargetSelected, ChancePercent: 50,
		}},
		StatStageEffects: []battleengine.StatStageEffect{
			{Stat: battleengine.StatAttack, Target: battleengine.EffectTargetUser, StageDelta: 1, ChancePercent: 50},
			{Stat: battleengine.StatDefense, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 50},
		},
		VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
			Status: battleengine.VolatileStatusConfusion, Target: battleengine.EffectTargetSelected,
			ChancePercent: 50, MinTurns: 2, MaxTurns: 3,
		}},
	}
	target := sleepingStrongWeatherSource("secondary-suppression-target", "")
	target.MaxHP, target.CurrentHP = 500, 500
	script := []battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("secondary-suppression-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("secondary-suppression-skill").String(), Value: 15},
	}
	random, err := battleengine.NewTracedRandom(script)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "secondary-suppression", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{actor}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		strongWeatherUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		strongWeatherUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	updatedActor, actorFound := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	updatedTarget, targetFound := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !actorFound || !targetFound || updatedActor.StatStages[battleengine.StatAttack] != 0 ||
		updatedTarget.StatStages[battleengine.StatDefense] != 0 || updatedTarget.ConfusionTurnsRemaining != 0 ||
		!reflect.DeepEqual(result.RandomTrace, script) {
		t.Fatalf("全抑制状态 actor=%+v target=%+v trace=%+v", updatedActor, updatedTarget, result.RandomTrace)
	}
	damageFound := false
	for _, event := range result.Events {
		switch value := event.(type) {
		case battleengine.DamageAppliedEvent:
			if value.Actor == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}) && value.Amount == 48 {
				damageFound = true
			}
		case battleengine.MajorStatusAppliedEvent, battleengine.StatStageChangedEvent,
			battleengine.VolatileStatusAppliedEvent, battleengine.FlinchAppliedEvent:
			t.Fatalf("追加效果抑制后出现了被禁止事件 %T: %+v", event, event)
		}
	}
	if !damageFound {
		t.Fatalf("缺少经 13/10 强化的 48 点伤害事件: %v", eventKinds(result.Events))
	}
}

// TestResolveTurnElementAbilityMultiplierMayIncreaseReceivedDamage 验证规则 123 的字段表达任意正倍率，
// 包含大于一的属性弱点；它不能因名称含“Reduction”而把 2/1 静默夹取为一或拒绝。
func TestResolveTurnElementAbilityMultiplierMayIncreaseReceivedDamage(t *testing.T) {
	t.Parallel()
	result := resolveAbilityDamageMultiplierTurn(t, func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
		skill.ElementID = testID("fire")
		target.ElementSkillDamageReduction = &battleengine.ElementSkillDamageReduction{
			ElementIDs: testIDs("fire"), Numerator: 2, Denominator: 1,
		}
	})
	if result.damage != 74 || !reflect.DeepEqual(result.trace, result.script) {
		t.Fatalf("属性弱点倍率 damage=%d trace=%+v，期望 2/1 倍后为 74", result.damage, result.trace)
	}
}

// TestResolveTurnAttackingStatMultiplierRequiresAllConditionsAndIncludesHalfHPBoundary 验证规则 125 的异常、
// 低生命闭区间、天气与场地条件按合取关系判断；只有整条规则激活时才允许绕过灼伤物攻减半。
func TestResolveTurnAttackingStatMultiplierRequiresAllConditionsAndIncludesHalfHPBoundary(t *testing.T) {
	t.Parallel()
	configure := func(hp uint32, status battleengine.MajorStatus, weather battleengine.WeatherKind, terrain battleengine.TerrainKind) abilityDamageMultiplierResult {
		return resolveAbilityDamageMultiplierTurn(t, func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, environment *battleengine.EnvironmentSnapshot) {
			actor.CurrentHP = hp
			actor.MajorStatus = status
			skill.ElementID = testID("neutral-skill")
			actor.AttackingStatMultiplier = &battleengine.AttackingStatMultiplier{
				Stat: battleengine.StatAttack, Numerator: 3, Denominator: 2,
				RequiredTerrain: terrain, RequiredWeather: weather, RequiresMajorStatus: true,
				RequiredMajorStatuses: []battleengine.MajorStatus{battleengine.MajorStatusBurn},
				MaximumHPNumerator:    1, MaximumHPDenominator: 2, IgnoreBurnAttackReduction: true,
			}
			environment.Weather = &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSun, TurnsRemaining: 3}
			environment.Terrain = &battleengine.TerrainEffect{Kind: battleengine.TerrainKindElectric, TurnsRemaining: 3}
		})
	}
	atBoundary := configure(50, battleengine.MajorStatusBurn, battleengine.WeatherKindSun, battleengine.TerrainKindElectric)
	aboveBoundary := configure(51, battleengine.MajorStatusBurn, battleengine.WeatherKindSun, battleengine.TerrainKindElectric)
	wrongStatus := configure(50, battleengine.MajorStatusPoison, battleengine.WeatherKindSun, battleengine.TerrainKindElectric)
	wrongWeather := configure(50, battleengine.MajorStatusBurn, battleengine.WeatherKindRain, battleengine.TerrainKindElectric)
	wrongTerrain := configure(50, battleengine.MajorStatusBurn, battleengine.WeatherKindSun, battleengine.TerrainKindGrassy)
	if atBoundary.damage != 54 || aboveBoundary.damage != 19 || wrongStatus.damage != 37 || wrongWeather.damage != 19 || wrongTerrain.damage != 19 {
		t.Fatalf("条件攻击倍率 damage boundary=%d above=%d status=%d weather=%d terrain=%d，期望 54/19/37/19/19",
			atBoundary.damage, aboveBoundary.damage, wrongStatus.damage, wrongWeather.damage, wrongTerrain.damage)
	}
	for _, result := range []abilityDamageMultiplierResult{atBoundary, aboveBoundary, wrongStatus, wrongWeather, wrongTerrain} {
		if !reflect.DeepEqual(result.trace, result.script) {
			t.Fatalf("条件攻击倍率改变了随机轨迹: trace=%+v script=%+v", result.trace, result.script)
		}
	}
}

// TestResolveTurnDefendingStatMultiplierRequiresStatusAndTerrain 验证规则 127 只有在目标具备有效主要异常且
// 当前场地匹配时才修正防御能力；任一条件不满足都会保持普通伤害公式。
func TestResolveTurnDefendingStatMultiplierRequiresStatusAndTerrain(t *testing.T) {
	t.Parallel()
	configure := func(status battleengine.MajorStatus, requiredTerrain battleengine.TerrainKind) abilityDamageMultiplierResult {
		return resolveAbilityDamageMultiplierTurn(t, func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, environment *battleengine.EnvironmentSnapshot) {
			target.MajorStatus = status
			if status != battleengine.MajorStatusSleep {
				target.SleepTurnsRemaining = 0
			}
			target.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("conditional-defense-idle"), Name: "条件防守等待", ElementID: testID("neutral"),
				DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
				Accuracy: 100, RemainingPP: 5, MaxPP: 5, HealingPercent: 1,
			}
			skill.ElementID = testID("neutral-skill")
			target.DefendingStatMultiplier = &battleengine.DefendingStatMultiplier{
				Stat: battleengine.StatDefense, Numerator: 2, Denominator: 1,
				RequiredTerrain: requiredTerrain, RequiresMajorStatus: true,
			}
			environment.Terrain = &battleengine.TerrainEffect{Kind: battleengine.TerrainKindGrassy, TurnsRemaining: 3}
		})
	}
	matched := configure(battleengine.MajorStatusPoison, battleengine.TerrainKindGrassy)
	missingStatus := configure("", battleengine.TerrainKindGrassy)
	wrongTerrain := configure(battleengine.MajorStatusPoison, battleengine.TerrainKindElectric)
	if matched.damage != 19 || missingStatus.damage != 37 || wrongTerrain.damage != 37 {
		t.Fatalf("条件防守倍率 damage matched=%d noStatus=%d terrain=%d，期望 19/37/37",
			matched.damage, missingStatus.damage, wrongTerrain.damage)
	}
}

// TestResolveTurnFullHPDamageReductionAppliesToSubstituteDamage 验证规则 121 在普通公式阶段读取目标本体的
// 满生命事实，因此倍率同样进入替身承伤数值，但不能错误扣除目标本体生命。
func TestResolveTurnFullHPDamageReductionAppliesToSubstituteDamage(t *testing.T) {
	t.Parallel()
	actor := newMember(1, "full-hp-substitute-attacker", 500, 500)
	actor.Stats.Speed = 200
	actor.Skills = []battleengine.SkillSnapshot{
		{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("full-hp-substitute-heal"), Name: "替身后回满", ElementID: testID("neutral"),
			DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
			Accuracy: 100, RemainingPP: 5, MaxPP: 5, TargetHealingNumerator: 1, TargetHealingDenominator: 4,
		},
		{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 2, SkillID: testID("full-hp-substitute-attack"), Name: "满生命替身伤害", ElementID: testID("neutral"),
			DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
			Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
		},
	}
	target := newMember(1, "full-hp-substitute-target", 500, 500)
	target.Stats.Speed = 10
	target.FullHPDamageReduction = &battleengine.FullHPDamageReduction{Numerator: 1, Denominator: 2}
	target.Skills = []battleengine.SkillSnapshot{
		{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("full-hp-substitute-start"), Name: "建立替身", ElementID: testID("neutral"),
			DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
			Priority: 1, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
			VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
				Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
				ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
			}},
		},
		{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 2, SkillID: testID("full-hp-substitute-protect"), Name: "后手保护", ElementID: testID("neutral"),
			DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
			Priority: -1, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
			VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
				Status: battleengine.VolatileStatusProtection, Target: battleengine.EffectTargetUser,
				ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
			}},
		},
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "full-hp-substitute", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{actor}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	firstTurn, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
		},
	}, battleengine.RandomSource{})
	if err != nil {
		t.Fatalf("ResolveTurn(turn 1) error = %v", err)
	}
	preparedTarget, found := firstTurn.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || preparedTarget.CurrentHP != preparedTarget.MaxHP || preparedTarget.SubstituteHP != 125 {
		t.Fatalf("替身与回满准备状态 = %+v，期望本体满生命且替身生命 125", preparedTarget)
	}
	script := []battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("full-hp-substitute-attack").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("full-hp-substitute-attack").String(), Value: 15},
	}
	random, err := battleengine.NewTracedRandom(script)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(firstTurn.State, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 2,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 2, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 2, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn(turn 2) error = %v", err)
	}
	updatedTarget, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || updatedTarget.CurrentHP != updatedTarget.MaxHP || updatedTarget.SubstituteHP != 107 ||
		!reflect.DeepEqual(result.RandomTrace, script) {
		t.Fatalf("替身承伤后目标=%+v trace=%+v，期望本体满生命且替身剩余 107", updatedTarget, result.RandomTrace)
	}
	foundSubstituteDamage := false
	for _, event := range result.Events {
		if applied, ok := event.(battleengine.SubstituteDamageAppliedEvent); ok && applied.Amount == 18 {
			foundSubstituteDamage = true
		}
	}
	if !foundSubstituteDamage {
		t.Fatalf("缺少经满生命倍率修正的 18 点替身伤害事件: %v", eventKinds(result.Events))
	}
}
