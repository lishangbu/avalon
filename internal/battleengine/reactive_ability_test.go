package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

var (
	reactiveNormalElement   = testID("00000000-0000-0000-0000-000000000010")
	reactiveElectricElement = testID("00000000-0000-0000-0000-000000000013")
)

// TestReactiveAbilityLedger132To136 覆盖五类回合末反应特性的正常路径、条件边界、事件顺序和状态快照。
func TestReactiveAbilityLedger132To136(t *testing.T) {
	t.Run("132 回合末固定提升速度且在回合结束事件之前公开", func(t *testing.T) {
		holder, opponent := reactiveWaitMember("holder", 100), reactiveWaitMember("opponent", 100)
		holder.Stats.Speed = 200
		opponent.Stats.Speed = 100
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{EndTurnStatStageChanges: []battleengine.StatStageDelta{{Stat: battleengine.StatSpeed, Delta: 1}}}
		result := resolveReactiveSingles(t, holder, opponent, battleengine.EnvironmentSnapshot{}, nil)
		if got := result.State.Snapshot().Sides[0].Members[0].StatStages[battleengine.StatSpeed]; got != 1 {
			t.Fatalf("speed stage = %d, want 1", got)
		}
		assertEventBeforeTurnEnded[battleengine.StatStageChangedEvent](t, result.Events)
	})

	t.Run("133 中毒和剧毒伤害被八分之一回复替换且异常保留", func(t *testing.T) {
		for _, status := range []battleengine.MajorStatus{battleengine.MajorStatusPoison, battleengine.MajorStatusBadPoison} {
			holder, opponent := reactiveWaitMember("holder", 100), reactiveWaitMember("opponent", 100)
			holder.CurrentHP, holder.MajorStatus = 50, status
			if status == battleengine.MajorStatusBadPoison {
				holder.BadPoisonCounter = 3
			}
			holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{MajorStatusEndTurnHealing: &battleengine.MajorStatusEndTurnHealing{Statuses: []battleengine.MajorStatus{battleengine.MajorStatusPoison, battleengine.MajorStatusBadPoison}, Denominator: 8}}
			result := resolveReactiveSingles(t, holder, opponent, battleengine.EnvironmentSnapshot{}, nil)
			got := result.State.Snapshot().Sides[0].Members[0]
			if got.CurrentHP != 62 || got.MajorStatus != status || got.BadPoisonCounter != holder.BadPoisonCounter {
				t.Fatalf("snapshot = %+v", got)
			}
			if countEvents[battleengine.MajorStatusDamageAppliedEvent](result.Events) != 0 || countEvents[battleengine.AbilityHPChangedEvent](result.Events) != 1 {
				t.Fatalf("events = %#v", result.Events)
			}
		}
	})

	t.Run("134 天气回合末伤害只在匹配有效天气触发", func(t *testing.T) {
		for _, test := range []struct {
			weather battleengine.WeatherKind
			want    uint32
		}{{battleengine.WeatherKindSun, 88}, {battleengine.WeatherKindRain, 100}} {
			holder, opponent := reactiveWaitMember("holder", 100), reactiveWaitMember("opponent", 100)
			holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{WeatherEndTurnDamage: &battleengine.WeatherEndTurnDamage{Weathers: []battleengine.WeatherKind{battleengine.WeatherKindSun}, Denominator: 8}}
			result := resolveReactiveSingles(t, holder, opponent, battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{Kind: test.weather, TurnsRemaining: 2}}, nil)
			if got := result.State.Snapshot().Sides[0].Members[0].CurrentHP; got != test.want {
				t.Fatalf("weather %s hp = %d, want %d", test.weather, got, test.want)
			}
		}
	})

	t.Run("135 睡眠对手承受回合末固定伤害而清醒对手不受影响", func(t *testing.T) {
		for _, test := range []struct {
			status battleengine.MajorStatus
			sleep  uint8
			want   uint32
		}{{battleengine.MajorStatusSleep, 2, 88}, {"", 0, 100}} {
			holder, opponent := reactiveWaitMember("holder", 100), reactiveWaitMember("opponent", 100)
			holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{OpponentMajorStatusEndTurnDamage: &battleengine.OpponentMajorStatusEndTurnDamage{Statuses: []battleengine.MajorStatus{battleengine.MajorStatusSleep}, Denominator: 8}}
			opponent.MajorStatus, opponent.SleepTurnsRemaining = test.status, int32(test.sleep)
			result := resolveReactiveSingles(t, holder, opponent, battleengine.EnvironmentSnapshot{}, nil)
			if got := result.State.Snapshot().Sides[1].Members[0].CurrentHP; got != test.want {
				t.Fatalf("status %s hp = %d, want %d", test.status, got, test.want)
			}
		}
	})

	t.Run("136 回合末异常治愈要求匹配有效天气", func(t *testing.T) {
		for _, test := range []struct {
			weather battleengine.WeatherKind
			want    battleengine.MajorStatus
		}{{battleengine.WeatherKindRain, ""}, {battleengine.WeatherKindSun, battleengine.MajorStatusBurn}} {
			holder, opponent := reactiveWaitMember("holder", 100), reactiveWaitMember("opponent", 100)
			holder.MajorStatus = battleengine.MajorStatusBurn
			holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{EndTurnMajorStatusCure: &battleengine.EndTurnMajorStatusCure{ChancePercent: 100, RequiredWeathers: []battleengine.WeatherKind{battleengine.WeatherKindRain}}}
			result := resolveReactiveSingles(t, holder, opponent, battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{Kind: test.weather, TurnsRemaining: 2}}, nil)
			if got := result.State.Snapshot().Sides[0].Members[0].MajorStatus; got != test.want {
				t.Fatalf("weather %s status = %s, want %s", test.weather, got, test.want)
			}
		}
	})
}

// TestReactiveAbilityLedger137To141 覆盖造成倒下、任意倒下、最高能力决胜和阶级上限的完整触发链路。
func TestReactiveAbilityLedger137To141(t *testing.T) {
	t.Run("137 整场一次造成倒下强化只消费一次", func(t *testing.T) {
		holder, target := reactiveFatalAttacker("holder"), reactiveWaitMember("target", 1)
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{OncePerBattleCausedFaintMultiStatBoost: []battleengine.StatStageDelta{{Stat: battleengine.StatAttack, Delta: 1}, {Stat: battleengine.StatSpecialAttack, Delta: 1}, {Stat: battleengine.StatSpeed, Delta: 1}}}
		result := resolveReactiveFatal(t, holder, target)
		got := result.State.Snapshot().Sides[0].Members[0]
		if !got.OncePerBattleFaintBoostActivated || got.StatStages[battleengine.StatAttack] != 1 || got.StatStages[battleengine.StatSpecialAttack] != 1 || got.StatStages[battleengine.StatSpeed] != 1 {
			t.Fatalf("holder = %+v", got)
		}
	})

	t.Run("138 仅持有者造成倒下时提升声明能力", func(t *testing.T) {
		holder, target := reactiveFatalAttacker("holder"), reactiveWaitMember("target", 1)
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{FaintStatStageBoosts: []battleengine.FaintStatStageBoost{{Stat: battleengine.StatAttack, Delta: 1, RequiresCausedFaint: true}}}
		result := resolveReactiveFatal(t, holder, target)
		if got := result.State.Snapshot().Sides[0].Members[0].StatStages[battleengine.StatAttack]; got != 1 {
			t.Fatalf("attack stage = %d", got)
		}
	})

	t.Run("139 任意成员倒下使场上持有者提升能力", func(t *testing.T) {
		holder, target := reactiveFatalAttacker("holder"), reactiveWaitMember("target", 1)
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{FaintStatStageBoosts: []battleengine.FaintStatStageBoost{{Stat: battleengine.StatSpecialAttack, Delta: 1}}}
		result := resolveReactiveFatal(t, holder, target)
		if got := result.State.Snapshot().Sides[0].Members[0].StatStages[battleengine.StatSpecialAttack]; got != 1 {
			t.Fatalf("special attack stage = %d", got)
		}
	})

	t.Run("140 造成倒下后按攻击防御特攻特防速度稳定决胜最高原始能力", func(t *testing.T) {
		holder, target := reactiveFatalAttacker("holder"), reactiveWaitMember("target", 1)
		holder.Stats.Attack, holder.Stats.Defense = 120, 120
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{FaintHighestStatBoost: true}
		result := resolveReactiveFatal(t, holder, target)
		got := result.State.Snapshot().Sides[0].Members[0]
		if got.StatStages[battleengine.StatAttack] != 1 || got.StatStages[battleengine.StatDefense] != 0 {
			t.Fatalf("stages = %+v", got.StatStages)
		}
	})

	t.Run("141 多个倒下触发按每次夹取到能力阶级上限", func(t *testing.T) {
		holder, target := reactiveFatalAttacker("holder"), reactiveWaitMember("target", 1)
		holder.StatStages = map[battleengine.Stat]int8{battleengine.StatSpecialAttack: 5}
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{FaintStatStageBoosts: []battleengine.FaintStatStageBoost{{Stat: battleengine.StatSpecialAttack, Delta: 4}}}
		result := resolveReactiveFatal(t, holder, target)
		if got := result.State.Snapshot().Sides[0].Members[0].StatStages[battleengine.StatSpecialAttack]; got != 6 {
			t.Fatalf("special attack stage = %d, want 6", got)
		}
	})
}

// TestReactiveAbilityLedger142To150 覆盖伙伴治愈、随机能力、倒下反制、伤害阈值和受伤充能九条规则。
func TestReactiveAbilityLedger142To150(t *testing.T) {
	t.Run("142 回合末伙伴治愈严格消费一次概率随机并保留失败状态", func(t *testing.T) {
		for _, test := range []struct {
			value int32
			want  battleengine.MajorStatus
		}{{0, ""}, {99, battleengine.MajorStatusBurn}} {
			replay, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{{Sequence: 1, Bound: 100, Reason: "end turn ally major status cure", Value: test.value}})
			if err != nil {
				t.Fatal(err)
			}
			result := resolveReactiveDoubleEndTurn(t, replay, &battleengine.ReactiveAbilityRules{EndTurnAllyMajorStatusCureChance: 30}, battleengine.MajorStatusBurn)
			if got := result.State.Snapshot().Sides[0].Members[1].MajorStatus; got != test.want {
				t.Fatalf("roll %d status = %s, want %s", test.value, got, test.want)
			}
			if len(result.RandomTrace) != 1 || result.RandomTrace[0].Reason != "end turn ally major status cure" {
				t.Fatalf("trace = %+v", result.RandomTrace)
			}
		}
	})

	t.Run("143 回合末随机能力提升与降低选择不同能力且轨迹固定", func(t *testing.T) {
		holder, opponent := reactiveWaitMember("holder", 100), reactiveWaitMember("opponent", 100)
		holder.Stats.Speed, opponent.Stats.Speed = 200, 100
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{EndTurnRandomStatStageChange: &battleengine.EndTurnRandomStatStageChange{RaiseDelta: 2, LowerDelta: -1}}
		replay, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{{Sequence: 1, Bound: 7, Reason: "end turn random stat raise", Value: 0}, {Sequence: 2, Bound: 6, Reason: "end turn random stat lower", Value: 0}})
		if err != nil {
			t.Fatal(err)
		}
		result := resolveReactiveSingles(t, holder, opponent, battleengine.EnvironmentSnapshot{}, replay)
		got := result.State.Snapshot().Sides[0].Members[0].StatStages
		if got[battleengine.StatAttack] != 2 || got[battleengine.StatDefense] != -1 || len(result.RandomTrace) != 2 {
			t.Fatalf("stages=%+v trace=%+v", got, result.RandomTrace)
		}
	})

	t.Run("144 接触技能使持有者倒下后按攻击者最大生命四分之一反制", func(t *testing.T) {
		attacker, holder := reactiveFatalAttacker("attacker"), reactiveWaitMember("holder", 50)
		attacker.Skills[0].MakesContact = true
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{FaintAttackerDamage: &battleengine.FaintAttackerDamage{RequiresContact: true, AttackerMaxHPDenominator: 4}}
		result := resolveReactiveFatal(t, attacker, holder)
		if got := result.State.Snapshot().Sides[0].Members[0].CurrentHP; got != 75 {
			t.Fatalf("attacker hp = %d, want 75", got)
		}
		assertEventBeforeTurnEnded[battleengine.AbilityHPChangedEvent](t, result.Events)
	})

	t.Run("145 持有者倒下后可按本次实际损失生命反制攻击者", func(t *testing.T) {
		attacker, holder := reactiveFatalAttacker("attacker"), reactiveWaitMember("holder", 50)
		holder.CurrentHP = 40
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{FaintAttackerDamage: &battleengine.FaintAttackerDamage{UsesDamageTaken: true}}
		result := resolveReactiveFatal(t, attacker, holder)
		if got := result.State.Snapshot().Sides[0].Members[0].CurrentHP; got != 60 {
			t.Fatalf("attacker hp = %d, want 60", got)
		}
	})

	t.Run("146 存活场上爆炸效果封锁阻止声明可封锁的倒下反制", func(t *testing.T) {
		attacker, holder := reactiveFatalAttacker("attacker"), reactiveWaitMember("holder", 50)
		attacker.Skills[0].MakesContact = true
		attacker.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{ExplosionEffectSuppression: true}
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{FaintAttackerDamage: &battleengine.FaintAttackerDamage{RequiresContact: true, AttackerMaxHPDenominator: 4, SuppressedByExplosionSuppression: true}}
		result := resolveReactiveFatal(t, attacker, holder)
		if got := result.State.Snapshot().Sides[0].Members[0].CurrentHP; got != 100 || countEvents[battleengine.AbilityHPChangedEvent](result.Events) != 0 {
			t.Fatalf("hp=%d events=%#v", got, result.Events)
		}
	})

	t.Run("147 存活承受要害本体伤害后直接把攻击能力设置到正六", func(t *testing.T) {
		attacker, holder := reactiveFormulaAttacker("attacker"), reactiveWaitMember("holder", 500)
		attacker.Skills[0].CriticalHitStage = 3
		holder.StatStages = map[battleengine.Stat]int8{battleengine.StatAttack: -2}
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{CriticalDamageSetStatStage: &battleengine.StatStageDelta{Stat: battleengine.StatAttack, Delta: 6}}
		result := resolveReactiveAttack(t, attacker, holder)
		if got := result.State.Snapshot().Sides[1].Members[0].StatStages[battleengine.StatAttack]; got != 6 {
			t.Fatalf("attack stage = %d, want 6", got)
		}
	})

	t.Run("148 真实本体伤害首次跨越半血时执行一组能力升降", func(t *testing.T) {
		attacker, holder := reactiveFatalAttacker("attacker"), reactiveWaitMember("holder", 100)
		attacker.Skills[0].DamageAmount = 60
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{DamageCrossedHalfHPStatStageChanges: []battleengine.StatStageDelta{{Stat: battleengine.StatSpecialAttack, Delta: 1}, {Stat: battleengine.StatAttack, Delta: 1}, {Stat: battleengine.StatSpeed, Delta: 1}, {Stat: battleengine.StatDefense, Delta: -1}}}
		result := resolveReactiveAttack(t, attacker, holder)
		got := result.State.Snapshot().Sides[1].Members[0]
		if !got.HalfHPThresholdAbilityActivated || got.StatStages[battleengine.StatSpecialAttack] != 1 || got.StatStages[battleengine.StatDefense] != -1 {
			t.Fatalf("holder = %+v", got)
		}
	})

	t.Run("149 受接触及匹配属性伤害后可分别改变持有者和攻击者能力", func(t *testing.T) {
		attacker, holder := reactiveFatalAttacker("attacker"), reactiveWaitMember("holder", 100)
		attacker.Skills[0].DamageAmount, attacker.Skills[0].MakesContact, attacker.Skills[0].ElementID = 10, true, reactiveElectricElement
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{ReceivedDamageStatStageChanges: []battleengine.ReceivedDamageStatStageChange{{Changes: []battleengine.StatStageDelta{{Stat: battleengine.StatDefense, Delta: 1}}}, {Changes: []battleengine.StatStageDelta{{Stat: battleengine.StatSpeed, Delta: -1}}, RequiresContact: true, ChangesAttacker: true}, {Changes: []battleengine.StatStageDelta{{Stat: battleengine.StatSpecialDefense, Delta: 2}}, ElementIDs: testIDs(reactiveElectricElement)}}}
		result := resolveReactiveAttack(t, attacker, holder)
		first, second := result.State.Snapshot().Sides[0].Members[0], result.State.Snapshot().Sides[1].Members[0]
		if first.StatStages[battleengine.StatSpeed] != -1 || second.StatStages[battleengine.StatDefense] != 1 || second.StatStages[battleengine.StatSpecialDefense] != 2 {
			t.Fatalf("attacker=%+v holder=%+v", first.StatStages, second.StatStages)
		}
	})

	t.Run("150 受伤建立的属性充能强化下一次匹配攻击并在造成本体伤害后消费", func(t *testing.T) {
		attacker, holder := reactiveFatalAttacker("attacker"), reactiveFormulaAttacker("holder")
		attacker.Skills[0].DamageAmount = 1
		holder.Skills[0].ElementID = reactiveElectricElement
		wait := reactiveWaitMember("holder-wait", 100).Skills[0]
		wait.Position, wait.SkillID = 2, testID("00000000-0000-0000-0000-000000000103")
		holder.Skills = append(holder.Skills, wait)
		attackerWait := wait
		attackerWait.SkillID = testID("00000000-0000-0000-0000-000000000104")
		attackerWait.VolatileStatusApplications = nil
		attackerWait.HealingPercent = 1
		attacker.Skills = append(attacker.Skills, attackerWait)
		holder.Stats.Speed = 50
		holder.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{ReceivedDamageCharge: &battleengine.ReceivedDamageCharge{ElementID: reactiveElectricElement, Numerator: 2, Denominator: 1}}
		first := resolveReactiveAttackWithSkills(t, attacker, holder, 1, 2)
		charged := first.State.Snapshot().Sides[1].Members[0]
		if charged.ChargedElementID != reactiveElectricElement || charged.ChargedDamageNumerator != 2 {
			t.Fatalf("charged = %+v", charged)
		}
		second := resolveReactiveNextTurn(t, first, battleengine.SideTwo, battleengine.SideOne)
		consumed := second.State.Snapshot().Sides[1].Members[0]
		if consumed.ChargedElementID != 0 || consumed.ChargedDamageNumerator != 1 || countEvents[battleengine.AbilityChargeChangedEvent](second.Events) != 1 {
			t.Fatalf("consumed=%+v events=%#v", consumed, second.Events)
		}
	})
}

func reactiveWaitMember(id string, hp uint32) battleengine.MemberSnapshot {
	member := newMember(1, id, hp, hp)
	member.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("00000000-0000-0000-0000-000000000101"), Name: "等待", ElementID: reactiveNormalElement, DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf, Accuracy: 100, RemainingPP: 10, MaxPP: 10, VolatileStatusApplications: []battleengine.VolatileStatusApplication{{Status: battleengine.VolatileStatusProtection, Target: battleengine.EffectTargetUser, ChancePercent: 100, MinTurns: 1, MaxTurns: 1}}}
	return member
}

func reactiveFatalAttacker(id string) battleengine.MemberSnapshot {
	member := newMember(1, id, 100, 100)
	member.Skills[0] = battleengine.SkillSnapshot{TargetScope: battleengine.SkillTargetScopeSelectedTarget, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("00000000-0000-0000-0000-000000000102"), Name: "致命攻击", ElementID: reactiveNormalElement, DamageClass: battleengine.DamageClassPhysical, DamageMode: battleengine.SkillDamageModeFixedAmount, DamageAmount: 200, Accuracy: 0, RemainingPP: 10, MaxPP: 10}
	return member
}

func reactiveFormulaAttacker(id string) battleengine.MemberSnapshot {
	member := newMember(1, id, 100, 100)
	member.Skills[0].SkillID = testID("00000000-0000-0000-0000-000000000105")
	member.Skills[0].Accuracy = 100
	return member
}

func resolveReactiveSingles(t *testing.T, first, second battleengine.MemberSnapshot, environment battleengine.EnvironmentSnapshot, random battleengine.RandomInput) battleengine.TurnResult {
	t.Helper()
	state := newWeatherState(t, environment, battleengine.RuleSnapshot{SchemaVersion: 1}, first, second)
	if random == nil {
		source, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 9)
		if err != nil {
			t.Fatal(err)
		}
		random = source
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{reactiveAction(battleengine.SideOne, battleengine.SideOne), reactiveAction(battleengine.SideTwo, battleengine.SideTwo)}}, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	return result
}

func resolveReactiveFatal(t *testing.T, attacker, target battleengine.MemberSnapshot) battleengine.TurnResult {
	return resolveReactiveAttack(t, attacker, target)
}

func resolveReactiveAttack(t *testing.T, attacker, target battleengine.MemberSnapshot) battleengine.TurnResult {
	return resolveReactiveAttackWithSkills(t, attacker, target, 1, 1)
}

func resolveReactiveAttackWithSkills(t *testing.T, attacker, target battleengine.MemberSnapshot, attackerSkill, targetSkill battleengine.SkillPosition) battleengine.TurnResult {
	t.Helper()
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, attacker, target)
	replay, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, 19)
	if err != nil {
		t.Fatal(err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{reactiveActionAt(battleengine.SideOne, battleengine.SideTwo, attackerSkill), reactiveActionAt(battleengine.SideTwo, battleengine.SideTwo, targetSkill)}}, replay)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	return result
}

func resolveReactiveNextTurn(t *testing.T, previous battleengine.TurnResult, actorSide, targetSide battleengine.Side) battleengine.TurnResult {
	t.Helper()
	result, err := battleengine.ResolveTurn(previous.State, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 2, Actions: []battleengine.Action{reactiveActionAt(battleengine.SideOne, battleengine.SideOne, 2), reactiveActionAt(actorSide, targetSide, 1)}}, previous.RandomSource)
	if err != nil {
		t.Fatalf("ResolveTurn(next) error = %v", err)
	}
	return result
}

func resolveReactiveDoubleEndTurn(t *testing.T, random battleengine.RandomInput, rules *battleengine.ReactiveAbilityRules, allyStatus battleengine.MajorStatus) battleengine.TurnResult {
	t.Helper()
	holder, ally := reactiveWaitMember("holder", 100), reactiveWaitMember("ally", 100)
	opponentA, opponentB := reactiveWaitMember("opponent-a", 100), reactiveWaitMember("opponent-b", 100)
	holder.Position, ally.Position, opponentA.Position, opponentB.Position = 1, 2, 1, 2
	holder.Stats.Speed, ally.Stats.Speed, opponentA.Stats.Speed, opponentB.Stats.Speed = 400, 300, 200, 100
	holder.ReactiveAbilityRules, ally.MajorStatus = rules, allyStatus
	state, err := battleengine.NewState(battleengine.InitialState{Format: battleengine.FormatSnapshot{Code: "reactive-double", ActiveSlotsPerSide: 2, TeamSize: 2}, Rules: battleengine.RuleSnapshot{SchemaVersion: 1}, Sides: []battleengine.SideSnapshot{{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{holder, ally}}, {Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{opponentA, opponentB}}}})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	actions := []battleengine.Action{reactiveActionAt(battleengine.SideOne, battleengine.SideOne, 1), {Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}, Kind: battleengine.ActionKindUseSkill, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 2}}}, reactiveActionAt(battleengine.SideTwo, battleengine.SideTwo, 1), {Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 2}, Kind: battleengine.ActionKindUseSkill, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 2}}}}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: actions}, random)
	if err != nil {
		t.Fatalf("ResolveTurn(double) error = %v", err)
	}
	return result
}

func reactiveAction(actorSide, targetSide battleengine.Side) battleengine.Action {
	return reactiveActionAt(actorSide, targetSide, 1)
}

func reactiveActionAt(actorSide, targetSide battleengine.Side, skillPosition battleengine.SkillPosition) battleengine.Action {
	return battleengine.Action{Actor: battleengine.SlotRef{Side: actorSide, Position: 1}, Kind: battleengine.ActionKindUseSkill, UseSkill: &battleengine.UseSkillAction{SkillPosition: skillPosition, Target: battleengine.SlotRef{Side: targetSide, Position: 1}}}
}

func countEvents[T any](events []battleengine.Event) int {
	count := 0
	for _, event := range events {
		if _, ok := event.(T); ok {
			count++
		}
	}
	return count
}
func assertEventBeforeTurnEnded[T any](t *testing.T, events []battleengine.Event) {
	t.Helper()
	found := false
	for _, event := range events {
		if _, ok := event.(T); ok {
			found = true
		}
		if _, ended := event.(battleengine.TurnEndedEvent); ended && !found {
			t.Fatalf("event order = %#v", events)
		}
	}
	if !found {
		t.Fatalf("event missing: %#v", events)
	}
}
