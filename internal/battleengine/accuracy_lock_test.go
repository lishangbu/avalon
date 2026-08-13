package battleengine_test

import (
	"strings"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnAccuracyLockMakesNextTurnLowAccuracySkillHit 验证命中锁定只跳过对同一具体目标的常规命中骰。
//
// 技能建立后的当前回合末会将两阶段寿命推进为一；下一回合的低命中技能仍会正常结算要害与伤害随机数，
// 但绝不能再消费 accuracy 随机项。
func TestResolveTurnAccuracyLockMakesNextTurnLowAccuracySkillHit(t *testing.T) {
	t.Parallel()
	attacker := accuracyLockUser(1, "accuracy-lock-user")
	attacker.Stats.Speed = 200
	target := newMember(1, "accuracy-lock-target", 500, 500)
	target.Stats.Speed = 50
	state := volatileState(t, attacker, target)

	started, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 101))
	if err != nil {
		t.Fatalf("建立命中锁定 ResolveTurn() error = %v", err)
	}
	locked, found := started.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	wantTarget := battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}
	if !found || locked.AccuracyLockTarget == nil || *locked.AccuracyLockTarget != wantTarget || locked.AccuracyLockTurnsRemaining != 1 {
		t.Fatalf("建立回合结束后的命中锁定 = %+v，期望目标=%+v、剩余阶段=1", locked, wantTarget)
	}
	if !accuracyLockStarted(started.Events, battleengine.SideOne, wantTarget) {
		t.Fatalf("建立回合未记录命中锁定事件: %+v", started.Events)
	}

	result, err := battleengine.ResolveTurn(started.State, volatileTurn(2, 2, 1), started.RandomSource)
	if err != nil {
		t.Fatalf("命中锁定后的低命中技能 ResolveTurn() error = %v", err)
	}
	remainingTarget, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || remainingTarget.CurrentHP >= target.CurrentHP {
		t.Fatalf("低命中技能未命中锁定目标: target=%+v", remainingTarget)
	}
	if randomTraceContains(result.RandomTrace, "accuracy for "+testID("accuracy-lock-low-accuracy").String()) {
		t.Fatalf("命中锁定不应消费常规命中随机项: %+v", result.RandomTrace)
	}
	locked, found = result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || locked.AccuracyLockTarget != nil || locked.AccuracyLockTurnsRemaining != 0 {
		t.Fatalf("第二个回合末应清除命中锁定: %+v", locked)
	}
}

// TestResolveTurnAccuracyLockClearsWhenTargetSwitches 验证锁定绑定到具体成员而非场上槽位。
func TestResolveTurnAccuracyLockClearsWhenTargetSwitches(t *testing.T) {
	t.Parallel()
	attacker := accuracyLockUser(1, "accuracy-lock-switch-user")
	attacker.Stats.Speed = 200
	target := newMember(1, "accuracy-lock-switch-target", 500, 500)
	reserve := newMember(2, "accuracy-lock-switch-reserve", 500, 500)
	state := volatileStateWithReserve(t, attacker, target, reserve)

	started, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 102))
	if err != nil {
		t.Fatalf("建立命中锁定 ResolveTurn() error = %v", err)
	}
	command := battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 2, Actions: []battleengine.Action{
		volatileSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 1),
		{
			Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
			Switch: &battleengine.SwitchAction{MemberPosition: 2},
		},
	}}
	result, err := battleengine.ResolveTurn(started.State, command, started.RandomSource)
	if err != nil {
		t.Fatalf("目标换出后的技能 ResolveTurn() error = %v", err)
	}
	if !randomTraceContains(result.RandomTrace, "accuracy for "+testID("accuracy-lock-low-accuracy").String()) {
		t.Fatalf("目标换出后必须恢复常规命中判定: %+v", result.RandomTrace)
	}
	user, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || user.AccuracyLockTarget != nil || user.AccuracyLockTurnsRemaining != 0 {
		t.Fatalf("目标换出后使用者仍保留命中锁定: %+v", user)
	}
}

// TestResolveTurnAccuracyLockDoesNotBypassProtectionOrDuplicateRules 验证锁定仍服从保护，且不能刷新同一目标。
func TestResolveTurnAccuracyLockDoesNotBypassProtectionOrDuplicateRules(t *testing.T) {
	t.Parallel()
	t.Run("重复锁定失败", func(t *testing.T) {
		t.Parallel()
		attacker := accuracyLockUser(1, "accuracy-lock-repeat-user")
		attacker.Stats.Speed = 200
		target := newMember(1, "accuracy-lock-repeat-target", 500, 500)
		state := volatileState(t, attacker, target)
		started, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 103))
		if err != nil {
			t.Fatalf("建立命中锁定 ResolveTurn() error = %v", err)
		}
		result, err := battleengine.ResolveTurn(started.State, volatileTurn(2, 1, 1), started.RandomSource)
		if err != nil {
			t.Fatalf("重复命中锁定 ResolveTurn() error = %v", err)
		}
		if !volatileSkillFailureExists(result.Events, battleengine.SkillFailureReasonAccuracyLockAlreadyActive) {
			t.Fatalf("重复锁定未产生明确失败事件: %+v", result.Events)
		}
	})
	t.Run("保护优先于锁定", func(t *testing.T) {
		t.Parallel()
		attacker := accuracyLockUser(1, "accuracy-lock-protection-user")
		attacker.Stats.Speed = 100
		target := accuracyLockProtectedTarget(1, "accuracy-lock-protection-target")
		target.Stats.Speed = 200
		state := volatileState(t, attacker, target)
		started, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 104))
		if err != nil {
			t.Fatalf("建立命中锁定 ResolveTurn() error = %v", err)
		}
		result, err := battleengine.ResolveTurn(started.State, volatileTurn(2, 2, 2), started.RandomSource)
		if err != nil {
			t.Fatalf("保护目标后的低命中技能 ResolveTurn() error = %v", err)
		}
		if !volatileSkillBlockExists(result.Events, battleengine.SkillBlockReasonProtection) {
			t.Fatalf("命中锁定错误绕过保护: %+v", result.Events)
		}
	})
}

// TestResolveTurnAccuracyLockRejectsTargetBehindSubstitute 验证替身阻止把命中锁定写入目标本体。
func TestResolveTurnAccuracyLockRejectsTargetBehindSubstitute(t *testing.T) {
	t.Parallel()
	attacker := accuracyLockUser(1, "accuracy-lock-substitute-user")
	attacker.Stats.Speed = 100
	target := accuracyLockProtectedTarget(1, "accuracy-lock-substitute-target")
	target.Stats.Speed = 200
	target.Skills[1] = battleengine.SkillSnapshot{TargetScope: battleengine.SkillTargetScopeSelectedTarget, DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 2, SkillID: testID("accuracy-lock-substitute"), Name: "替身", ElementID: target.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, MaxPP: 10, RemainingPP: 10,
		VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
			Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser, ChancePercent: 100,
			MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
		}},
	}
	state := volatileState(t, attacker, target)
	result, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 2), mustRandom(t, 105))
	if err != nil {
		t.Fatalf("替身阻止命中锁定 ResolveTurn() error = %v", err)
	}
	if !volatileSkillFailureExists(result.Events, battleengine.SkillFailureReasonAccuracyLockTargetBehindSubstitute) {
		t.Fatalf("替身未阻止命中锁定: %+v", result.Events)
	}
}

// TestResolveTurnAccuracyLockDoesNotBypassOneHitKnockOutLevelGate 验证锁定只能跳过常规命中骰。
//
// 一击必杀的目标等级限制属于命中随机数之前的规则门槛；因此即使锁定仍有效，也必须明确失败而不能造成伤害。
func TestResolveTurnAccuracyLockDoesNotBypassOneHitKnockOutLevelGate(t *testing.T) {
	t.Parallel()
	attacker := accuracyLockUser(1, "accuracy-lock-ohko-user")
	attacker.Stats.Speed = 200
	attacker.Skills = append(attacker.Skills, battleengine.SkillSnapshot{MinHits: 1, MaxHits: 1, Position: 3, SkillID: testID("accuracy-lock-one-hit-knock-out"), Name: "一击必杀", ElementID: attacker.ElementIDs[0],
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		DamageMode: battleengine.SkillDamageModeOneHitKnockOut, OneHitKnockOutBaseAccuracy: 30, MaxPP: 5, RemainingPP: 5,
	})
	target := newMember(1, "accuracy-lock-ohko-target", 500, 500)
	target.Level = 51
	target.Stats.Speed = 50
	state := volatileState(t, attacker, target)

	started, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 106))
	if err != nil {
		t.Fatalf("建立命中锁定 ResolveTurn() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(started.State, volatileTurn(2, 3, 1), started.RandomSource)
	if err != nil {
		t.Fatalf("锁定后一击必杀 ResolveTurn() error = %v", err)
	}
	if !volatileSkillFailureExists(result.Events, battleengine.SkillFailureReasonOneHitKnockOutTargetLevelHigher) {
		t.Fatalf("命中锁定错误绕过一击必杀等级限制: %+v", result.Events)
	}
	if randomTraceContains(result.RandomTrace, "accuracy for "+testID("accuracy-lock-one-hit-knock-out").String()) {
		t.Fatalf("一击必杀等级失败不应产生命中随机项: %+v", result.RandomTrace)
	}
}

// accuracyLockUser 创建拥有命中锁定与低命中伤害技能的测试成员。
func accuracyLockUser(position battleengine.MemberPosition, creatureID string) battleengine.MemberSnapshot {
	member := newMember(position, creatureID, 500, 500)
	member.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("accuracy-lock"), Name: "命中锁定", ElementID: member.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		MaxPP: 10, RemainingPP: 10, LocksAccuracyOnTarget: true,
	}
	member.Skills = append(member.Skills, battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 2, SkillID: testID("accuracy-lock-low-accuracy"), Name: "低命中技能", ElementID: member.ElementIDs[0],
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 40, Accuracy: 1, MaxPP: 10, RemainingPP: 10,
	})
	return member
}

// accuracyLockProtectedTarget 创建包含保护技能的被锁定目标。
func accuracyLockProtectedTarget(position battleengine.MemberPosition, creatureID string) battleengine.MemberSnapshot {
	member := newMember(position, creatureID, 500, 500)
	member.Skills = append(member.Skills, battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 2, SkillID: testID("accuracy-lock-protection"), Name: "保护", ElementID: member.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		MaxPP: 10, RemainingPP: 10, VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
			Status: battleengine.VolatileStatusProtection, Target: battleengine.EffectTargetUser, ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
		}},
	})
	return member
}

// accuracyLockStarted 报告事件流是否记录了指定成员对指定目标建立命中锁定。
func accuracyLockStarted(events []battleengine.Event, side battleengine.Side, target battleengine.MemberRef) bool {
	for _, event := range events {
		value, ok := event.(battleengine.AccuracyLockStartedEvent)
		if ok && value.Actor.Side == side && value.Target == target {
			return true
		}
	}
	return false
}

// randomTraceContains 报告本回合随机轨迹是否包含指定用途文本。
func randomTraceContains(trace []battleengine.RandomTraceEntry, want string) bool {
	for _, entry := range trace {
		if strings.Contains(entry.Reason, want) {
			return true
		}
	}
	return false
}
