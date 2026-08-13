package battleengine_test

import (
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnAbilityAccuracyMultiplier 验证任意技能命中倍率通过公开回合结算边界改变命中结果，且只消费
// 一次命中随机数；未命中不会继续消费要害或伤害浮动，也不会改写目标生命快照。
func TestResolveTurnAbilityAccuracyMultiplier(t *testing.T) {
	t.Parallel()
	resolveAbilityAccuracyMiss(t, func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.EnvironmentSnapshot) {
		actor.AccuracyMultiplier = &battleengine.AccuracyMultiplier{Numerator: 1, Denominator: 2}
	}, battleengine.DamageClassPhysical, 100, 51)
}

// TestResolveTurnPhysicalSkillAbilityAccuracyMultiplier 验证物理技能专用倍率只影响物理技能，特殊技能仍按原始
// 命中率执行；两条路径都通过结构化事件、随机轨迹和最终生命快照观察。
func TestResolveTurnPhysicalSkillAbilityAccuracyMultiplier(t *testing.T) {
	t.Parallel()
	t.Run("物理技能应用倍率", func(t *testing.T) {
		t.Parallel()
		resolveAbilityAccuracyMiss(t, func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.EnvironmentSnapshot) {
			actor.PhysicalSkillAccuracyMultiplier = &battleengine.AccuracyMultiplier{Numerator: 1, Denominator: 2}
		}, battleengine.DamageClassPhysical, 100, 51)
	})
	t.Run("特殊技能不应用倍率", func(t *testing.T) {
		t.Parallel()
		resolveAbilityAccuracyHitWithoutAccuracyRoll(t, func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.EnvironmentSnapshot) {
			actor.PhysicalSkillAccuracyMultiplier = &battleengine.AccuracyMultiplier{Numerator: 1, Denominator: 2}
		}, battleengine.DamageClassSpecial, 100)
	})
}

// TestResolveTurnSandstormOpponentAccuracyMultiplier 验证目标特性只在普通沙暴有效读取时降低对手命中率；
// 结算失败停在命中阶段，不产生伤害事件或额外随机轨迹。
func TestResolveTurnSandstormOpponentAccuracyMultiplier(t *testing.T) {
	t.Parallel()
	resolveAbilityAccuracyMiss(t, func(_, target *battleengine.MemberSnapshot, environment *battleengine.EnvironmentSnapshot) {
		target.OpponentAccuracySandstormMultiplier = &battleengine.AccuracyMultiplier{Numerator: 1, Denominator: 2}
		// 隔离回合末天气伤害，确保最终生命快照只反映被测技能是否命中。
		target.WeatherDamageImmunities = []battleengine.WeatherKind{battleengine.WeatherKindSandstorm}
		environment.Weather = &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 5}
	}, battleengine.DamageClassPhysical, 100, 51)
}

// TestResolveTurnSnowOpponentAccuracyMultiplier 验证降雪命中倍率与沙暴字段相互独立，并按冻结环境在命中阶段
// 产生可重放的失败轨迹。
func TestResolveTurnSnowOpponentAccuracyMultiplier(t *testing.T) {
	t.Parallel()
	resolveAbilityAccuracyMiss(t, func(_, target *battleengine.MemberSnapshot, environment *battleengine.EnvironmentSnapshot) {
		target.OpponentAccuracySnowMultiplier = &battleengine.AccuracyMultiplier{Numerator: 1, Denominator: 2}
		environment.Weather = &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSnow, TurnsRemaining: 5}
	}, battleengine.DamageClassPhysical, 100, 51)
}

// TestResolveTurnConfusedOpponentAccuracyMultiplier 验证目标仍处于混乱时才应用面向对手的命中倍率；目标本回合
// 因休整而不行动，确保随机轨迹只记录被测技能的命中事实。
func TestResolveTurnConfusedOpponentAccuracyMultiplier(t *testing.T) {
	t.Parallel()
	resolveAbilityAccuracyMiss(t, func(_, target *battleengine.MemberSnapshot, _ *battleengine.EnvironmentSnapshot) {
		target.ConfusionTurnsRemaining = 2
		target.OpponentAccuracyConfusionMultiplier = &battleengine.AccuracyMultiplier{Numerator: 1, Denominator: 2}
	}, battleengine.DamageClassPhysical, 100, 51)
}

// TestResolveTurnAbilityAlwaysHitSkipsAccuracyTrace 验证普通技能必中特性完全跳过命中掷骰，但仍按原顺序消费要害
// 与伤害浮动；最终状态必须真实扣血，而不是只省略未命中事件。
func TestResolveTurnAbilityAlwaysHitSkipsAccuracyTrace(t *testing.T) {
	t.Parallel()
	resolveAbilityAccuracyHitWithoutAccuracyRoll(t, func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.EnvironmentSnapshot) {
		actor.AccuracyAlwaysHits = true
	}, battleengine.DamageClassPhysical, 1)
}

// TestResolveTurnStatusSkillAccuracyCap 验证目标特性在命中公式最后限制对手变化技能的命中上限；未命中时不会
// 写入技能声明的能力阶级变化，权威状态快照保持原值。
func TestResolveTurnStatusSkillAccuracyCap(t *testing.T) {
	t.Parallel()
	result := resolveAbilityAccuracyTurn(
		t,
		func(_, target *battleengine.MemberSnapshot, _ *battleengine.EnvironmentSnapshot) {
			target.StatusSkillAccuracyCap = 50
		},
		battleengine.DamageClassStatus,
		100,
		[]battleengine.RandomTraceEntry{{Sequence: 1, Bound: 100, Reason: "accuracy for " + testID("ability-accuracy-skill").String(), Value: 50}},
	)
	missed, found := abilityAccuracyMissedEvent(result.Events)
	if !found || missed.Accuracy != 50 || missed.Roll != 51 {
		t.Fatalf("变化技能命中上限事件 = %+v, events=%+v", missed, result.Events)
	}
	target, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || target.StatStages[battleengine.StatAttack] != 0 || target.CurrentHP != 1_000 {
		t.Fatalf("未命中后的目标快照 = %+v", target)
	}
}

// TestResolveTurnIgnoreOpponentAccuracyStatStages 验证特性按持有者在本次命中判定中的角色忽略对手阶级，且不
// 清除双方权威能力阶级。必中结果跳过命中骰，随机轨迹从要害判定开始。
func TestResolveTurnIgnoreOpponentAccuracyStatStages(t *testing.T) {
	t.Parallel()
	t.Run("目标忽略使用者命中阶级", func(t *testing.T) {
		t.Parallel()
		result := resolveAbilityAccuracyHitWithoutAccuracyRoll(t, func(actor, target *battleengine.MemberSnapshot, _ *battleengine.EnvironmentSnapshot) {
			actor.StatStages = map[battleengine.Stat]int8{battleengine.StatAccuracy: -6}
			target.IgnoreOpponentAccuracyStatStages = true
		}, battleengine.DamageClassPhysical, 100)
		actor, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
		if actor.StatStages[battleengine.StatAccuracy] != -6 {
			t.Fatalf("使用者命中阶级被特性改写: %+v", actor.StatStages)
		}
	})
	t.Run("使用者忽略目标闪避阶级", func(t *testing.T) {
		t.Parallel()
		result := resolveAbilityAccuracyHitWithoutAccuracyRoll(t, func(actor, target *battleengine.MemberSnapshot, _ *battleengine.EnvironmentSnapshot) {
			actor.IgnoreOpponentAccuracyStatStages = true
			target.StatStages = map[battleengine.Stat]int8{battleengine.StatEvasion: 6}
		}, battleengine.DamageClassPhysical, 100)
		target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
		if target.StatStages[battleengine.StatEvasion] != 6 {
			t.Fatalf("目标闪避阶级被特性改写: %+v", target.StatStages)
		}
	})
}

// abilityAccuracyMutation 为命中类公开结算夹具注入单条被测特性及其必要环境。
type abilityAccuracyMutation func(*battleengine.MemberSnapshot, *battleengine.MemberSnapshot, *battleengine.EnvironmentSnapshot)

// resolveAbilityAccuracyMiss 执行一次应在命中阶段失败的技能，并统一验证事件、随机轨迹及生命快照。
func resolveAbilityAccuracyMiss(
	t *testing.T,
	mutate abilityAccuracyMutation,
	damageClass battleengine.DamageClass,
	accuracy uint8,
	wantRoll uint8,
) {
	t.Helper()
	result := resolveAbilityAccuracyTurn(t, mutate, damageClass, accuracy, []battleengine.RandomTraceEntry{{
		Sequence: 1, Bound: 100, Reason: "accuracy for " + testID("ability-accuracy-skill").String(), Value: int32(wantRoll - 1),
	}})
	missed, found := abilityAccuracyMissedEvent(result.Events)
	if !found || missed.Roll != wantRoll || missed.Accuracy >= wantRoll {
		t.Fatalf("命中特性未命中事件 = %+v, events=%+v", missed, result.Events)
	}
	target, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || target.CurrentHP != 1_000 {
		t.Fatalf("未命中后的目标生命快照 = %+v", target)
	}
	for _, event := range result.Events {
		if _, ok := event.(battleengine.DamageAppliedEvent); ok {
			t.Fatalf("未命中后不应产生伤害事件: %+v", result.Events)
		}
	}
}

// resolveAbilityAccuracyHitWithoutAccuracyRoll 执行一次应跳过命中骰的普通伤害，并验证随机消费从要害与伤害
// 浮动开始、目标生命真实下降。
func resolveAbilityAccuracyHitWithoutAccuracyRoll(
	t *testing.T,
	mutate abilityAccuracyMutation,
	damageClass battleengine.DamageClass,
	accuracy uint8,
) battleengine.TurnResult {
	t.Helper()
	result := resolveAbilityAccuracyTurn(t, mutate, damageClass, accuracy, []battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("ability-accuracy-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("ability-accuracy-skill").String(), Value: 15},
	})
	target, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || target.CurrentHP >= 1_000 {
		t.Fatalf("命中后的目标生命快照 = %+v", target)
	}
	if _, found := abilityAccuracyMissedEvent(result.Events); found {
		t.Fatalf("跳过命中骰后出现未命中事件: %+v", result.Events)
	}
	return result
}

// resolveAbilityAccuracyTurn 从完整公开状态、命令和显式重放随机源执行一回合，避免直接测试包内命中公式。
func resolveAbilityAccuracyTurn(
	t *testing.T,
	mutate abilityAccuracyMutation,
	damageClass battleengine.DamageClass,
	accuracy uint8,
	trace []battleengine.RandomTraceEntry,
) battleengine.TurnResult {
	t.Helper()
	actor := newMember(1, "ability-accuracy-actor", 1_000, 1_000)
	actor.Stats.Speed = 200
	actor.Skills[0].SkillID = testID("ability-accuracy-skill")
	actor.Skills[0].DamageClass = damageClass
	actor.Skills[0].Accuracy = accuracy
	if damageClass == battleengine.DamageClassStatus {
		actor.Skills[0].Power = 0
		actor.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
			Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 100,
		}}
	}
	target := newMember(1, "ability-accuracy-target", 1_000, 1_000)
	target.Stats.Speed = 100
	// 睡眠属于允许冻结进初始事实的主要异常，可稳定阻止目标行动而不消费随机数。
	target.MajorStatus = battleengine.MajorStatusSleep
	target.SleepTurnsRemaining = 3
	target.Skills[0].DamageClass = battleengine.DamageClassStatus
	target.Skills[0].Power = 0
	target.Skills[0].Accuracy = 0
	target.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatDefense, Target: battleengine.EffectTargetUser, StageDelta: 1, ChancePercent: 100,
	}}
	environment := battleengine.EnvironmentSnapshot{}
	mutate(&actor, &target, &environment)
	confusionTurns := target.ConfusionTurnsRemaining
	target.ConfusionTurnsRemaining = 0
	if confusionTurns != 0 {
		actor.Skills = append(actor.Skills, battleengine.SkillSnapshot{TargetScope: battleengine.SkillTargetScopeSelectedTarget, DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 2, SkillID: testID("ability-accuracy-confusion-setup"), Name: "混乱准备",
			ElementID: actor.Skills[0].ElementID, DamageClass: battleengine.DamageClassStatus,
			RemainingPP: 1, MaxPP: 1,
			VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
				Status: battleengine.VolatileStatusConfusion, Target: battleengine.EffectTargetSelected,
				ChancePercent: 100, MinTurns: confusionTurns, MaxTurns: confusionTurns,
			}},
		})
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format:      battleengine.FormatSnapshot{Code: "ability-accuracy", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:       battleengine.RuleSnapshot{SchemaVersion: 1},
		Environment: environment,
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{actor}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	turnNumber := uint32(1)
	if confusionTurns != 0 {
		setupRandom, randomErr := battleengine.NewTracedRandom(nil)
		if randomErr != nil {
			t.Fatalf("NewTracedRandom(setup) error = %v", randomErr)
		}
		setup, setupErr := battleengine.ResolveTurn(state, battleengine.TurnCommand{
			SchemaVersion: 1,
			TurnNumber:    1,
			Actions: []battleengine.Action{
				volatileSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 1),
				volatileSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
			},
		}, setupRandom)
		if setupErr != nil {
			t.Fatalf("ResolveTurn(setup) error = %v", setupErr)
		}
		state = setup.State
		turnNumber = 2
	}
	random, err := battleengine.NewTracedRandom(trace)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    turnNumber,
		Actions: []battleengine.Action{
			volatileSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
			volatileSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		},
	}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if !reflect.DeepEqual(result.RandomTrace, trace) {
		t.Fatalf("随机轨迹 = %+v，期望 %+v", result.RandomTrace, trace)
	}
	return result
}

// abilityAccuracyMissedEvent 返回事件流中命中类夹具技能的结构化未命中事实。
func abilityAccuracyMissedEvent(events []battleengine.Event) (battleengine.SkillMissedEvent, bool) {
	for _, event := range events {
		missed, ok := event.(battleengine.SkillMissedEvent)
		if ok && missed.SkillID == testID("ability-accuracy-skill") {
			return missed, true
		}
	}
	return battleengine.SkillMissedEvent{}, false
}
