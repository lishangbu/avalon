package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnAppliesDirectDamageModes 验证固定伤害、使用者等级伤害、目标当前生命比例伤害和双方当前
// 生命差值伤害均跳过普通公式及其要害、伤害浮动随机数，并写入统一的结构化伤害事件。
func TestResolveTurnAppliesDirectDamageModes(t *testing.T) {
	t.Parallel()
	tests := []struct {
		// name 是当前直接伤害模式的稳定子测试名称。
		name string
		// mode 是交给技能快照的冻结伤害模式。
		mode battleengine.SkillDamageMode
		// actorHP 是使用者行动开始时的当前生命。
		actorHP uint32
		// targetHP 是目标行动开始时的当前生命。
		targetHP uint32
		// configure 仅配置当前模式所需的强类型参数。
		configure func(*battleengine.SkillSnapshot)
		// expectedDamage 是本次应写入目标的实际伤害。
		expectedDamage uint32
	}{
		{
			name: "固定数值", mode: battleengine.SkillDamageModeFixedAmount, actorHP: 100, targetHP: 100,
			configure: func(skill *battleengine.SkillSnapshot) { skill.DamageAmount = 40 }, expectedDamage: 40,
		},
		{
			name: "使用者等级", mode: battleengine.SkillDamageModeUserLevel, actorHP: 100, targetHP: 100,
			configure: func(*battleengine.SkillSnapshot) {}, expectedDamage: 50,
		},
		{
			name: "目标当前生命比例", mode: battleengine.SkillDamageModeTargetCurrentHPFraction, actorHP: 100, targetHP: 101,
			configure: func(skill *battleengine.SkillSnapshot) {
				skill.DamageNumerator, skill.DamageDenominator, skill.MinimumDamage = 1, 2, 1
			}, expectedDamage: 50,
		},
		{
			name: "当前生命差值", mode: battleengine.SkillDamageModeTargetCurrentHPMinusUserCurrentHP, actorHP: 40, targetHP: 100,
			configure: func(*battleengine.SkillSnapshot) {}, expectedDamage: 60,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			state := directDamageState(t, test.mode, test.actorHP, test.targetHP, test.configure)
			random, err := battleengine.NewTracedRandom(nil)
			if err != nil {
				t.Fatalf("NewTracedRandom() error = %v", err)
			}
			result, err := battleengine.ResolveTurn(state, directDamageTurn(), random)
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			damages := directDamageEvents(result.Events)
			if len(damages) != 1 || damages[0].Amount != test.expectedDamage || damages[0].CriticalHit || damages[0].RandomPercent != 0 {
				t.Fatalf("直接伤害事件 = %+v，期望伤害 %d 且无要害/浮动随机数", damages, test.expectedDamage)
			}
			if len(result.RandomTrace) != 0 {
				t.Fatalf("直接伤害不应消耗随机轨迹，实际 = %+v", result.RandomTrace)
			}
		})
	}
}

// TestResolveTurnAppliesOneHitKnockOutDamage 验证一击必杀使用专用命中率命中后会造成目标完整当前生命的
// 直接伤害，不进入普通伤害公式、要害或伤害浮动随机数路径。
func TestResolveTurnAppliesOneHitKnockOutDamage(t *testing.T) {
	t.Parallel()
	state := directDamageState(t, battleengine.SkillDamageModeOneHitKnockOut, 100, 83, func(skill *battleengine.SkillSnapshot) {
		skill.OneHitKnockOutBaseAccuracy = 30
	})
	random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{{
		Sequence: 1, Bound: 100, Reason: "accuracy for " + testID("direct-damage-skill").String(), Value: 29,
	}})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, directDamageTurn(), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	damages := directDamageEvents(result.Events)
	if len(damages) != 1 || damages[0].Amount != 83 || damages[0].CriticalHit || damages[0].RandomPercent != 0 {
		t.Fatalf("一击必杀伤害事件 = %+v，期望直接造成目标当前生命 83", damages)
	}
	if len(result.RandomTrace) != 1 || result.RandomTrace[0].Reason != "accuracy for "+testID("direct-damage-skill").String() {
		t.Fatalf("一击必杀随机轨迹 = %+v，期望仅一次专用命中判定", result.RandomTrace)
	}
}

// TestResolveTurnOneHitKnockOutRejectsHigherLevelTarget 验证目标等级较高会在命中掷骰前使一击必杀失败，
// 因此不能错误消费随机数或回退到普通伤害公式。
func TestResolveTurnOneHitKnockOutRejectsHigherLevelTarget(t *testing.T) {
	t.Parallel()
	state := directDamageState(t, battleengine.SkillDamageModeOneHitKnockOut, 100, 100, func(skill *battleengine.SkillSnapshot) {
		skill.OneHitKnockOutBaseAccuracy = 30
	})
	snapshot := state.Snapshot()
	snapshot.Sides[1].Members[0].Level = 51
	state, err := battleengine.NewState(battleengine.InitialState{Format: snapshot.Format, Rules: snapshot.Rules, Sides: snapshot.Sides})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	random, err := battleengine.NewTracedRandom(nil)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, directDamageTurn(), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if len(result.RandomTrace) != 0 || len(directDamageEvents(result.Events)) != 0 {
		t.Fatalf("高等级目标的一击必杀不应消耗随机数或造成伤害：trace=%+v events=%+v", result.RandomTrace, result.Events)
	}
	for _, event := range result.Events {
		failed, isFailed := event.(battleengine.SkillFailedEvent)
		if isFailed && failed.Reason == battleengine.SkillFailureReasonOneHitKnockOutTargetLevelHigher {
			return
		}
	}
	t.Fatalf("事件流缺少一击必杀等级失败事件：%+v", result.Events)
}

// TestResolveTurnOneHitKnockOutUsesSpecialAccuracyExceptions 验证一击必杀忽略命中与闪避能力阶级，
// 并且会使用同属性使用者专属基础命中率；同属性目标阻止则早于命中随机数。
func TestResolveTurnOneHitKnockOutUsesSpecialAccuracyExceptions(t *testing.T) {
	t.Parallel()
	t.Run("忽略命中与闪避阶段并采用同属性使用者基础命中", func(t *testing.T) {
		state := directDamageState(t, battleengine.SkillDamageModeOneHitKnockOut, 100, 100, func(skill *battleengine.SkillSnapshot) {
			skill.OneHitKnockOutBaseAccuracy = 20
			skill.OneHitKnockOutSameElementUserBaseAccuracy = 30
		})
		snapshot := state.Snapshot()
		snapshot.Sides[0].Members[0].Level = 60
		snapshot.Sides[0].Members[0].StatStages = map[battleengine.Stat]int8{battleengine.StatAccuracy: -6}
		snapshot.Sides[1].Members[0].Level = 50
		snapshot.Sides[1].Members[0].StatStages = map[battleengine.Stat]int8{battleengine.StatEvasion: 6}
		state, err := battleengine.NewState(battleengine.InitialState{Format: snapshot.Format, Rules: snapshot.Rules, Sides: snapshot.Sides})
		if err != nil {
			t.Fatalf("NewState() error = %v", err)
		}
		random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{{
			Sequence: 1, Bound: 100, Reason: "accuracy for " + testID("direct-damage-skill").String(), Value: 39,
		}})
		if err != nil {
			t.Fatalf("NewTracedRandom() error = %v", err)
		}
		result, err := battleengine.ResolveTurn(state, directDamageTurn(), random)
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		if damages := directDamageEvents(result.Events); len(damages) != 1 || damages[0].Amount != 100 {
			t.Fatalf("一击必杀专用命中规则后的伤害 = %+v，期望命中并造成 100", damages)
		}
	})
	t.Run("同属性目标在命中前阻止", func(t *testing.T) {
		state := directDamageState(t, battleengine.SkillDamageModeOneHitKnockOut, 100, 100, func(skill *battleengine.SkillSnapshot) {
			skill.OneHitKnockOutBaseAccuracy = 30
			skill.OneHitKnockOutBlocksSameElementTarget = true
		})
		random, err := battleengine.NewTracedRandom(nil)
		if err != nil {
			t.Fatalf("NewTracedRandom() error = %v", err)
		}
		result, err := battleengine.ResolveTurn(state, directDamageTurn(), random)
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		if len(result.RandomTrace) != 0 || len(directDamageEvents(result.Events)) != 0 {
			t.Fatalf("同属性阻止不应消耗随机数或造成伤害：trace=%+v events=%+v", result.RandomTrace, result.Events)
		}
		for _, event := range result.Events {
			blocked, isBlocked := event.(battleengine.SkillBlockedEvent)
			if isBlocked && blocked.Reason == battleengine.SkillBlockReasonOneHitKnockOutSameElementTarget {
				return
			}
		}
		t.Fatalf("事件流缺少同属性一击必杀阻止事件：%+v", result.Events)
	})
}

// TestResolveTurnFailsHPDifferenceDamageWhenTargetIsNotHealthier 验证生命差值不为正时不会错误回退到
// 普通伤害公式，而是留下能被重放和统计识别的技能失败事件。
func TestResolveTurnFailsHPDifferenceDamageWhenTargetIsNotHealthier(t *testing.T) {
	t.Parallel()
	state := directDamageState(t, battleengine.SkillDamageModeTargetCurrentHPMinusUserCurrentHP, 100, 80, func(*battleengine.SkillSnapshot) {})
	random, err := battleengine.NewTracedRandom(nil)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, directDamageTurn(), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if len(directDamageEvents(result.Events)) != 0 {
		t.Fatalf("生命差值失败不应产生伤害事件: %+v", result.Events)
	}
	for _, event := range result.Events {
		failed, ok := event.(battleengine.SkillFailedEvent)
		if ok && failed.SkillID == testID("direct-damage-skill") &&
			failed.Reason == battleengine.SkillFailureReasonTargetHPNotGreaterThanUserHP {
			return
		}
	}
	t.Fatalf("事件流缺少生命差值失败事件: %+v", result.Events)
}

// TestResolveTurnAppliesSelfSacrificeAfterDirectDamage 验证以使用者当前生命作为伤害的直接技能先写入
// 目标伤害，再以独立事件记录使用者自我牺牲和倒下。
func TestResolveTurnAppliesSelfSacrificeAfterDirectDamage(t *testing.T) {
	t.Parallel()
	state := directDamageState(t, battleengine.SkillDamageModeUserCurrentHPAndUserFaints, 60, 100, func(*battleengine.SkillSnapshot) {})
	random, err := battleengine.NewTracedRandom(nil)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, directDamageTurn(), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	damages := directDamageEvents(result.Events)
	if len(damages) != 1 || damages[0].Amount != 60 {
		t.Fatalf("自我牺牲技能伤害 = %+v，期望 60", damages)
	}
	actor, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !exists || actor.CurrentHP != 0 {
		t.Fatalf("自我牺牲后使用者生命 = %d，期望 0", actor.CurrentHP)
	}
	for index, event := range result.Events {
		sacrifice, ok := event.(battleengine.SkillSelfSacrificeDamageAppliedEvent)
		if !ok || sacrifice.SkillID != testID("direct-damage-skill") {
			continue
		}
		if sacrifice.Amount != 60 || index == 0 || result.Events[index-1].Kind() != battleengine.EventKindDamageApplied {
			t.Fatalf("自我牺牲事件顺序或数值无效: %+v", result.Events)
		}
		return
	}
	t.Fatalf("事件流缺少自我牺牲事件: %+v", result.Events)
}

// TestResolveTurnAveragesCurrentHPWithoutTreatingItAsDamage 验证当前生命平均规则不会产生普通伤害或
// 吸取/反作用后效，而是以专用事件写入双方各自夹取后的当前生命。
func TestResolveTurnAveragesCurrentHPWithoutTreatingItAsDamage(t *testing.T) {
	t.Parallel()
	state := directDamageState(t, battleengine.SkillDamageModeAverageUserAndTargetCurrentHP, 50, 151, func(skill *battleengine.SkillSnapshot) {
		skill.DamageClass = battleengine.DamageClassStatus
	})
	random, err := battleengine.NewTracedRandom(nil)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, directDamageTurn(), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	actor, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if actor.CurrentHP != 100 || target.CurrentHP != 100 || len(directDamageEvents(result.Events)) != 0 {
		t.Fatalf("当前生命平均后的状态或伤害事件 = actor:%+v target:%+v events:%+v", actor, target, result.Events)
	}
	for _, event := range result.Events {
		averaged, ok := event.(battleengine.HPAveragedBySkillEvent)
		if ok && averaged.ActorCurrentHP == 100 && averaged.TargetCurrentHP == 100 {
			return
		}
	}
	t.Fatalf("事件流缺少当前生命平均事件: %+v", result.Events)
}

// TestResolveTurnCurrentHPAverageFailsBehindSubstitute 验证直接改写目标本体生命的平均规则不会穿透
// 对方替身，即使替身在同一回合较早建立也会阻止后续技能。
func TestResolveTurnCurrentHPAverageFailsBehindSubstitute(t *testing.T) {
	t.Parallel()
	left := newMember(1, "average-user", 200, 50)
	left.Stats.Speed = 200
	left.Skills[0].SkillID = testID("average-current-hp")
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].DamageMode = battleengine.SkillDamageModeAverageUserAndTargetCurrentHP
	right := newMember(1, "average-substitute-target", 200, 200)
	right.Stats.Speed = 10
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].Priority = 1
	right.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
		ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
	}}
	state := volatileState(t, left, right)
	result, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 27))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if target.CurrentHP != 150 || target.SubstituteHP != 50 || len(directDamageEvents(result.Events)) != 0 {
		t.Fatalf("替身阻止平均生命规则后的目标状态 = %+v, events = %+v", target, result.Events)
	}
	for _, event := range result.Events {
		failed, ok := event.(battleengine.SkillFailedEvent)
		if ok && failed.SkillID == testID("average-current-hp") && failed.Reason == battleengine.SkillFailureReasonTargetBehindSubstitute {
			return
		}
	}
	t.Fatalf("事件流缺少替身阻止当前生命平均的失败事件: %+v", result.Events)
}

// TestResolveTurnReturnsLatestReceivedDamageToItsActualSource 验证伤害记忆只读取本回合最后一段合格 HP 伤害，
// 并在双打中无视客户端提交的普通目标，改为命中实际伤害来源。测试让两个对手先后攻击使用者，以确保倒序事件
// 检索、倍率取整和目标重定向共同生效，而不是偶然命中玩家选择的第一个对手槽位。
func TestResolveTurnReturnsLatestReceivedDamageToItsActualSource(t *testing.T) {
	t.Parallel()
	reflector := newMember(1, "received-damage-reflector", 200, 200)
	reflector.Stats.Speed = 100
	reflector.Skills[0].SkillID = testID("received-damage-skill")
	reflector.Skills[0].Power = 0
	reflector.Skills[0].DamageMode = battleengine.SkillDamageModeReceivedDamage
	reflector.Skills[0].ReceivedDamageNumerator = 2
	reflector.Skills[0].ReceivedDamageDenominator = 1
	reflector.Skills[0].ReceivedDamageAcceptsPhysical = true
	reflector.Skills[0].ReceivedDamageIgnoreNonImmuneElementEffectiveness = true

	leftPartner := newMember(2, "received-damage-left-partner", 200, 200)
	leftPartner.Stats.Speed = 10
	leftPartner.Skills[0].DamageClass = battleengine.DamageClassStatus
	leftPartner.Skills[0].Power = 0
	// 每名上场成员都必须提交一个可执行行动；使用仅作用于自身的能力阶级变化避免干扰伤害记忆断言。
	leftPartner.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetUser, StageDelta: 1, ChancePercent: 100,
	}}

	firstSource := newMember(1, "received-damage-first-source", 200, 200)
	firstSource.Stats.Speed = 200
	firstSource.Skills[0].SkillID = testID("received-damage-first-source-skill")
	firstSource.Skills[0].Power = 0
	firstSource.Skills[0].DamageMode = battleengine.SkillDamageModeFixedAmount
	firstSource.Skills[0].DamageAmount = 30

	latestSource := newMember(2, "received-damage-latest-source", 200, 200)
	latestSource.Stats.Speed = 180
	latestSource.Skills[0].SkillID = testID("received-damage-latest-source-skill")
	latestSource.Skills[0].Power = 0
	latestSource.Skills[0].DamageMode = battleengine.SkillDamageModeFixedAmount
	latestSource.Skills[0].DamageAmount = 40

	state := targetScopeDoubleState(t, reflector, leftPartner, firstSource, latestSource)
	random, err := battleengine.NewTracedRandom(nil)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, targetScopeTurn(
		// 反打者故意选择右侧第一个槽位；最后一段合格伤害来自第二个槽位，结算必须重定向。
		targetScopeSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 1),
		targetScopeSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		targetScopeSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 1),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	damages := receivedDamageEvents(result.Events)
	if len(damages) != 1 || damages[0].Amount != 80 || damages[0].Target.Side != battleengine.SideTwo ||
		damages[0].Target.Position != 2 {
		t.Fatalf("伤害记忆反打事件 = %+v，期望将最后 40 点物理伤害的两倍返还给右侧第二槽", damages)
	}
	if len(result.RandomTrace) != 0 {
		t.Fatalf("固定伤害和伤害记忆不应消费随机数，实际轨迹 = %+v", result.RandomTrace)
	}
}

// TestResolveTurnAppliesReceivedDamageElementEffectiveness 验证伤害记忆把“完全免疫”与“非零倍率”严格区分。
//
// 开启忽略开关后，反打仍必须尊重目标的 0 倍属性免疫，但不能再因克制或抵抗改变记忆伤害；关闭开关则使用
// 冻结资料中完整的相性分数。两条路径均不进入要害或伤害随机数结算。
func TestResolveTurnAppliesReceivedDamageElementEffectiveness(t *testing.T) {
	t.Parallel()
	t.Run("保留或忽略非免疫倍率", func(t *testing.T) {
		tests := []struct {
			// name 是本次资料开关的稳定子测试名称。
			name string
			// ignoreNonImmuneEffectiveness 控制反打是否跳过非零属性倍率。
			ignoreNonImmuneEffectiveness bool
			// expectedDamage 是源成员先造成 20 点伤害后，反打应实际扣除的生命值。
			expectedDamage uint32
		}{
			{name: "保留克制倍率", ignoreNonImmuneEffectiveness: false, expectedDamage: 40},
			{name: "忽略克制倍率", ignoreNonImmuneEffectiveness: true, expectedDamage: 20},
		}
		for _, test := range tests {
			t.Run(test.name, func(t *testing.T) {
				state := receivedDamageElementState(t, test.ignoreNonImmuneEffectiveness, 2)
				result, err := battleengine.ResolveTurn(state, directDamageTurn(), mustRandom(t, 31))
				if err != nil {
					t.Fatalf("ResolveTurn() error = %v", err)
				}
				damages := receivedDamageEvents(result.Events)
				if len(damages) != 1 || damages[0].Amount != test.expectedDamage {
					t.Fatalf("伤害记忆属性倍率后的伤害 = %+v，期望 %d", damages, test.expectedDamage)
				}
			})
		}
	})
	t.Run("完全免疫始终阻止", func(t *testing.T) {
		state := receivedDamageElementState(t, true, 0)
		result, err := battleengine.ResolveTurn(state, directDamageTurn(), mustRandom(t, 32))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		if damages := receivedDamageEvents(result.Events); len(damages) != 0 {
			t.Fatalf("完全免疫不应产生伤害记忆扣血事件：%+v", damages)
		}
		for _, event := range result.Events {
			blocked, isBlocked := event.(battleengine.SkillBlockedEvent)
			if isBlocked && blocked.SkillID == testID("received-damage-skill") && blocked.Reason == battleengine.SkillBlockReasonElementImmunity {
				return
			}
		}
		t.Fatalf("事件流缺少伤害记忆的属性免疫阻止事件：%+v", result.Events)
	})
}

// TestResolveTurnRejectsUnavailableOrIneligibleReceivedDamage 验证伤害记忆不会借用本回合外、错误伤害类别或并未
// 真实扣除本体生命的事件。失败时技能仍已经宣告并消耗 PP，但不能继续命中或进入直接伤害路径。
func TestResolveTurnRejectsUnavailableOrIneligibleReceivedDamage(t *testing.T) {
	t.Parallel()
	t.Run("本回合没有合格受伤", func(t *testing.T) {
		reflector := receivedDamageReflector(false, true)
		reflector.Stats.Speed = 200
		target := newMember(1, "received-damage-idle-target", 200, 200)
		target.Stats.Speed = 10
		target.Skills[0].DamageClass = battleengine.DamageClassStatus
		target.Skills[0].Power = 0
		target.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
			Stat: battleengine.StatAttack, Target: battleengine.EffectTargetUser, StageDelta: 1, ChancePercent: 100,
		}}
		state := volatileState(t, reflector, target)
		result, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 7))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		assertReceivedDamageFailure(t, result.Events)
		if len(receivedDamageEvents(result.Events)) != 0 {
			t.Fatalf("没有受伤记录时不应产生反打伤害事件: %+v", result.Events)
		}
	})
	t.Run("不接受特殊伤害来源", func(t *testing.T) {
		reflector := receivedDamageReflector(true, false)
		reflector.Stats.Speed = 100
		source := newMember(1, "received-damage-special-source", 200, 200)
		source.Stats.Speed = 200
		source.Skills[0].SkillID = testID("received-damage-special-source-skill")
		source.Skills[0].DamageClass = battleengine.DamageClassSpecial
		source.Skills[0].Power = 0
		source.Skills[0].DamageMode = battleengine.SkillDamageModeFixedAmount
		source.Skills[0].DamageAmount = 30
		state := volatileState(t, reflector, source)
		result, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 8))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		assertReceivedDamageFailure(t, result.Events)
		if len(receivedDamageEvents(result.Events)) != 0 {
			t.Fatalf("不接受的伤害类别不应产生反打伤害事件: %+v", result.Events)
		}
	})
}

// directDamageState 创建一方带有直接伤害技能、另一方只有无随机状态技能的最小有效单打状态。
func directDamageState(
	t *testing.T,
	mode battleengine.SkillDamageMode,
	actorHP uint32,
	targetHP uint32,
	configure func(*battleengine.SkillSnapshot),
) battleengine.State {
	t.Helper()
	left := newMember(1, "direct-damage-user", 100, actorHP)
	left.Stats.Speed = 200
	left.Skills[0].SkillID = testID("direct-damage-skill")
	left.Skills[0].DamageMode = mode
	// 直接伤害不依赖普通威力；显式置零可防止测试错误地通过旧公式伤害可用性分支。
	left.Skills[0].Power = 0
	configure(&left.Skills[0])
	right := newMember(1, "direct-damage-target", 200, targetHP)
	right.Stats.Speed = 10
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 0,
	}}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "direct-damage", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// receivedDamageElementState 创建伤害记忆与其物理伤害来源的最小单打状态。
//
// effectivenessNumerator 用于固定“反打属性 / 来源当前属性”的相性分子；分母始终为 1，便于让测试只关注
// 忽略非免疫倍率和完全免疫之间的行为差异。
func receivedDamageElementState(
	t *testing.T,
	ignoreNonImmuneEffectiveness bool,
	effectivenessNumerator uint16,
) battleengine.State {
	t.Helper()
	reflector := receivedDamageReflector(true, false)
	reflector.Stats.Speed = 100
	reflector.Skills[0].ReceivedDamageNumerator = 1
	reflector.Skills[0].ReceivedDamageDenominator = 1
	reflector.Skills[0].ReceivedDamageIgnoreNonImmuneElementEffectiveness = ignoreNonImmuneEffectiveness
	reflector.Skills[0].ElementID = testID("counter-element")
	source := newMember(1, "received-damage-element-source", 200, 200)
	source.Stats.Speed = 200
	source.ElementIDs = testIDs("counter-target-element")
	source.Skills[0].SkillID = testID("received-damage-element-source-skill")
	source.Skills[0].DamageClass = battleengine.DamageClassPhysical
	source.Skills[0].Power = 0
	source.Skills[0].DamageMode = battleengine.SkillDamageModeFixedAmount
	source.Skills[0].DamageAmount = 20
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "received-damage-element", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules: battleengine.RuleSnapshot{
			SchemaVersion: 1,
			ElementEffectiveness: []battleengine.ElementEffectiveness{{
				AttackElementID: testID("counter-element"), DefenseElementID: testID("counter-target-element"),
				Numerator: effectivenessNumerator, Denominator: 1,
			}},
		},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{reflector}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{source}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// directDamageTurn 返回直接伤害测试所需的完整第一回合命令集合。
func directDamageTurn() battleengine.TurnCommand {
	return battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			{
				Kind:  battleengine.ActionKindUseSkill,
				Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{
					SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				},
			},
			{
				Kind:  battleengine.ActionKindUseSkill,
				Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				UseSkill: &battleengine.UseSkillAction{
					SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				},
			},
		},
	}
}

// directDamageEvents 从完整事件流提取本测试技能的伤害事件。
func directDamageEvents(events []battleengine.Event) []battleengine.DamageAppliedEvent {
	result := make([]battleengine.DamageAppliedEvent, 0, 1)
	for _, event := range events {
		damage, ok := event.(battleengine.DamageAppliedEvent)
		if ok && damage.SkillID == testID("direct-damage-skill") {
			result = append(result, damage)
		}
	}
	return result
}

// receivedDamageReflector 构造具有合法伤害记忆参数、但尚未决定可接受伤害类别的最小攻击成员。
func receivedDamageReflector(acceptsPhysical, acceptsSpecial bool) battleengine.MemberSnapshot {
	member := newMember(1, "received-damage-reflector", 200, 200)
	member.Skills[0].SkillID = testID("received-damage-skill")
	member.Skills[0].Power = 0
	member.Skills[0].DamageMode = battleengine.SkillDamageModeReceivedDamage
	member.Skills[0].ReceivedDamageNumerator = 2
	member.Skills[0].ReceivedDamageDenominator = 1
	member.Skills[0].ReceivedDamageAcceptsPhysical = acceptsPhysical
	member.Skills[0].ReceivedDamageAcceptsSpecial = acceptsSpecial
	member.Skills[0].ReceivedDamageIgnoreNonImmuneElementEffectiveness = true
	return member
}

// receivedDamageEvents 提取由伤害记忆技能产生的 HP 伤害事件，忽略被记忆的原始伤害和其他回合末事件。
func receivedDamageEvents(events []battleengine.Event) []battleengine.DamageAppliedEvent {
	result := make([]battleengine.DamageAppliedEvent, 0, 1)
	for _, event := range events {
		damage, ok := event.(battleengine.DamageAppliedEvent)
		if ok && damage.SkillID == testID("received-damage-skill") {
			result = append(result, damage)
		}
	}
	return result
}

// assertReceivedDamageFailure 断言事件流用稳定失败原因表达“没有可返还伤害”，而没有回退成普通未命中。
func assertReceivedDamageFailure(t *testing.T, events []battleengine.Event) {
	t.Helper()
	for _, event := range events {
		failed, ok := event.(battleengine.SkillFailedEvent)
		if ok && failed.SkillID == testID("received-damage-skill") &&
			failed.Reason == battleengine.SkillFailureReasonReceivedDamageMemoryUnavailable {
			return
		}
	}
	t.Fatalf("事件流缺少伤害记忆不可用失败事件: %+v", events)
}
