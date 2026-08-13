package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnDamageCrossedHalfHPForceSelfSwitch 验证成员本体生命首次跨过半血阈值后，会通过完整换入生命周期
// 强制自身换下。只有一个健康后备时不需要选择随机数。
func TestResolveTurnDamageCrossedHalfHPForceSelfSwitch(t *testing.T) {
	t.Parallel()

	attacker := fixedDamageUser(1, "threshold-switch-attacker")
	attacker.Stats.Speed = 200
	attacker.Skills[0].DamageAmount = 200
	target := passiveMember(1, "threshold-switch-target", 1_000, 600)
	target.Stats.Speed = 50
	target.AbilityID = testID("threshold-switch-ability")
	target.DamageCrossedHalfHPForceSelfSwitch = true
	reserve := passiveMember(2, "threshold-switch-reserve", 1_000, 1_000)

	result := resolveForceTargetSwitchTurn(t, forceTargetSwitchState(
		t, attacker, []battleengine.MemberSnapshot{target, reserve}, battleengine.SideConditionSnapshot{},
	), 71)
	selection, found := abilityForcedSwitchSelection(result.Events)
	if !found || selection.Source != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) ||
		selection.SelectedMember != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 2}) {
		t.Fatalf("半血强制自换选择事件 = %#v, found=%t", selection, found)
	}
	if containsRandomReason(result.RandomTrace, "ability forced switch selection for "+testID("threshold-switch-ability").String()) ||
		!containsForcedSwitch(result.Events, battleengine.SideTwo, 2) {
		t.Fatalf("半血强制自换的随机或换人事件错误: trace=%+v events=%v", result.RandomTrace, eventKinds(result.Events))
	}
}

// TestResolveTurnDamageCrossedHalfHPForceSelfSwitchOnlyTriggersOnCrossing 验证已低于半血、替身承伤和没有健康后备
// 都不会触发选择或消费随机数；这些路径只能保留已经发生的技能伤害或替身伤害。
func TestResolveTurnDamageCrossedHalfHPForceSelfSwitchOnlyTriggersOnCrossing(t *testing.T) {
	t.Parallel()

	for _, test := range []struct {
		name     string
		target   battleengine.MemberSnapshot
		reserves []battleengine.MemberSnapshot
	}{
		{
			name:   "已经低于半血",
			target: thresholdSwitchTarget("threshold-already-below", 400),
			reserves: []battleengine.MemberSnapshot{
				passiveMember(2, "threshold-already-below-reserve", 1_000, 1_000),
			},
		},
		{
			name:   "没有健康后备",
			target: thresholdSwitchTarget("threshold-no-reserve", 600),
			reserves: []battleengine.MemberSnapshot{
				passiveMember(2, "threshold-fainted-reserve", 1_000, 0),
			},
		},
	} {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			attacker := fixedDamageUser(1, "threshold-blocked-attacker-"+test.name)
			attacker.Stats.Speed = 200
			attacker.Skills[0].DamageAmount = 200
			members := append([]battleengine.MemberSnapshot{test.target}, test.reserves...)
			result := resolveForceTargetSwitchTurn(t, forceTargetSwitchState(
				t, attacker, members, battleengine.SideConditionSnapshot{},
			), 72)
			if _, found := abilityForcedSwitchSelection(result.Events); found ||
				containsRandomReason(result.RandomTrace, "ability forced switch selection for "+test.target.AbilityID.String()) {
				t.Fatalf("不应触发半血强制自换: trace=%+v events=%v", result.RandomTrace, eventKinds(result.Events))
			}
			active, exists := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
			if !exists || active.Position != 1 {
				t.Fatalf("不触发时应保持原成员上场: member=%+v found=%t", active, exists)
			}
		})
	}
}

// TestResolveTurnDamageCrossedHalfHPForceSelfSwitchIgnoresSubstituteDamage 验证成员在本回合先建立替身后，即使
// 后手技能对替身造成足以跨过本体半血的伤害，也不会触发本体的半血强制自换特性。
func TestResolveTurnDamageCrossedHalfHPForceSelfSwitchIgnoresSubstituteDamage(t *testing.T) {
	t.Parallel()

	attacker := fixedDamageUser(1, "threshold-substitute-attacker")
	attacker.Stats.Speed = 50
	attacker.Skills[0].DamageAmount = 200
	target := thresholdSwitchTarget("threshold-substitute", 800)
	target.Stats.Speed = 200
	target.Skills[0].DamageClass = battleengine.DamageClassStatus
	target.Skills[0].Power = 0
	target.Skills[0].TargetScope = battleengine.SkillTargetScopeSelf
	target.Skills[0].HealingPercent = 0
	target.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
		ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
	}}
	reserve := passiveMember(2, "threshold-substitute-reserve", 1_000, 1_000)
	result := resolveForceTargetSwitchTurn(t, forceTargetSwitchState(
		t, attacker, []battleengine.MemberSnapshot{target, reserve}, battleengine.SideConditionSnapshot{},
	), 74)
	if _, found := abilityForcedSwitchSelection(result.Events); found {
		t.Fatalf("替身承伤不应触发半血强制自换: %v", eventKinds(result.Events))
	}
	active, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || active.Position != 1 || active.CurrentHP != 550 || active.SubstituteHP != 50 {
		t.Fatalf("替身承伤后的成员状态 = %+v, found=%t", active, found)
	}
}

// TestResolveTurnDamageCrossedHalfHPForceSelfSwitchUsesOneRandomChoice 验证多个健康后备按稳定成员位置排序，并且只
// 消耗一次专用随机数；结构化选择事件和实际强制换人必须使用同一个成员。
func TestResolveTurnDamageCrossedHalfHPForceSelfSwitchUsesOneRandomChoice(t *testing.T) {
	t.Parallel()

	attacker := fixedDamageUser(1, "threshold-random-attacker")
	attacker.Stats.Speed = 200
	attacker.Skills[0].DamageAmount = 200
	target := thresholdSwitchTarget("threshold-random", 600)
	reserveThree := passiveMember(3, "threshold-random-reserve-three", 1_000, 1_000)
	reserveTwo := passiveMember(2, "threshold-random-reserve-two", 1_000, 1_000)
	result := resolveForceTargetSwitchTurn(t, forceTargetSwitchState(
		t, attacker, []battleengine.MemberSnapshot{target, reserveThree, reserveTwo}, battleengine.SideConditionSnapshot{},
	), 73)

	selection, found := abilityForcedSwitchSelection(result.Events)
	trace, traced := randomTraceByReason(result.RandomTrace, "ability forced switch selection for "+testID("threshold-random-ability").String())
	if !found || !traced || trace.Bound != 2 || len(selection.Candidates) != 2 ||
		selection.Candidates[0].Position != 2 || selection.Candidates[1].Position != 3 ||
		selection.SelectedMember != selection.Candidates[trace.Value] ||
		!containsForcedSwitch(result.Events, battleengine.SideTwo, selection.SelectedMember.Position) {
		t.Fatalf("多后备半血强制自换结果错误: selection=%#v trace=%+v traces=%+v found=%t traced=%t", selection, trace, result.RandomTrace, found, traced)
	}
}

// thresholdSwitchTarget 创建具有半血跨越强制自换特性的目标成员。
func thresholdSwitchTarget(creatureID string, currentHP uint32) battleengine.MemberSnapshot {
	member := passiveMember(1, creatureID, 1_000, currentHP)
	member.Stats.Speed = 50
	member.AbilityID = testID(creatureID + "-ability")
	member.DamageCrossedHalfHPForceSelfSwitch = true
	return member
}

// abilityForcedSwitchSelection 从事件流中读取半血跨越特性已经确定的强制换人选择。
func abilityForcedSwitchSelection(events []battleengine.Event) (battleengine.AbilityForcedSwitchSelectedEvent, bool) {
	for _, event := range events {
		if selection, ok := event.(battleengine.AbilityForcedSwitchSelectedEvent); ok {
			return selection, true
		}
	}
	return battleengine.AbilityForcedSwitchSelectedEvent{}, false
}
