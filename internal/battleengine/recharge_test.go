package battleengine_test

import (
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnRechargeStartsAfterBodyDamageAndConsumesNextAction 验证休整技能只有在真正扣除目标本体
// 生命后才会写入一次后续行动阻止，并且下一次行动不消费已提交技能的 PP。
func TestResolveTurnRechargeStartsAfterBodyDamageAndConsumesNextAction(t *testing.T) {
	t.Parallel()
	left := rechargeAttacker(1, "recharge-user")
	left.Stats.Speed = 200
	right := rechargePassiveMember(1, "recharge-target")
	state := volatileState(t, left, right)

	first, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 61))
	if err != nil {
		t.Fatalf("首次休整技能 ResolveTurn() error = %v", err)
	}
	user, found := first.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || user.RechargeTurnsRemaining != 1 || !rechargeStarted(first.Events, battleengine.SideOne) {
		t.Fatalf("首次命中后未正确进入休整: user=%+v events=%+v", user, first.Events)
	}
	remainingPP := user.Skills[0].RemainingPP

	second, err := battleengine.ResolveTurn(first.State, volatileTurn(2, 1, 1), mustRandom(t, 62))
	if err != nil {
		t.Fatalf("休整行动 ResolveTurn() error = %v", err)
	}
	user, found = second.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || user.RechargeTurnsRemaining != 0 || user.Skills[0].RemainingPP != remainingPP {
		t.Fatalf("休整行动后的成员状态 = %+v，期望消耗状态但不消耗 PP", user)
	}
	if !volatilePreventionExists(second.Events, battleengine.SideOne, battleengine.SkillPreventionReasonRecharge) {
		t.Fatalf("事件未记录休整阻止: %+v", second.Events)
	}
}

// TestResolveTurnForcedSwitchClearsRecharge 验证换出是休整状态的明确生命周期终点。
//
// 被对手技能强制换人时，当前回合仍先记录休整阻止，但原成员进入后备后不能把旧休整状态带回下一次换入。
func TestResolveTurnForcedSwitchClearsRecharge(t *testing.T) {
	t.Parallel()

	user := rechargeAttacker(1, "recharge-forced-switch-user")
	user.Stats.Speed = 200
	reserve := rechargePassiveMember(2, "recharge-forced-switch-reserve")
	opponent := rechargePassiveMember(1, "recharge-forced-switch-opponent")
	opponent.Skills = append(opponent.Skills, battleengine.SkillSnapshot{TargetScope: battleengine.SkillTargetScopeSelectedTarget, DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 2, SkillID: testID("recharge-forced-switch"), Name: "强制换人", ElementID: opponent.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, MaxPP: 20, RemainingPP: 20, ForceTargetSwitch: true,
	})
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "recharge-forced-switch", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{user, reserve}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{opponent}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	first, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 661))
	if err != nil {
		t.Fatalf("建立休整状态 ResolveTurn() error = %v", err)
	}
	if active, found := first.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}); !found || active.RechargeTurnsRemaining != 1 {
		t.Fatalf("强制换人前的休整状态 = %+v，found=%t", active, found)
	}
	second, err := battleengine.ResolveTurn(first.State, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    2,
		Actions: []battleengine.Action{
			volatileSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
			volatileSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 1),
		},
	}, mustRandom(t, 662))
	if err != nil {
		t.Fatalf("强制换人 ResolveTurn() error = %v", err)
	}
	if !volatilePreventionExists(second.Events, battleengine.SideOne, battleengine.SkillPreventionReasonRecharge) ||
		!containsForcedSwitch(second.Events, battleengine.SideOne, 2) {
		t.Fatalf("休整阻止或强制换人事件缺失: %+v", second.Events)
	}
	for _, member := range second.State.Snapshot().Sides[0].Members {
		if member.Position == 1 && member.RechargeTurnsRemaining != 0 {
			t.Fatalf("离场成员保留了休整状态: %+v", member)
		}
	}
}

// TestResolveTurnRechargePreventsVoluntarySwitch 验证成员不能通过主动换人跳过下一次休整。
//
// 倒下补位和技能强制换人不属于主动换人，本用例只覆盖命令边界的用户主动请求。
func TestResolveTurnRechargePreventsVoluntarySwitch(t *testing.T) {
	t.Parallel()
	left := rechargeAttacker(1, "recharge-switch-user")
	left.Stats.Speed = 200
	reserve := rechargePassiveMember(2, "recharge-switch-reserve")
	right := rechargePassiveMember(1, "recharge-switch-target")
	state := volatileStateWithReserve(t, left, right, reserve)

	first, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 63))
	if err != nil {
		t.Fatalf("建立休整状态 ResolveTurn() error = %v", err)
	}
	_, err = battleengine.ResolveTurn(first.State, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    2,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
			volatileSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		},
	}, mustRandom(t, 64))
	var commandError *battleengine.TurnCommandError
	if !errors.As(err, &commandError) || commandError.Code != battleengine.TurnCommandErrorSwitchPrevented {
		t.Fatalf("休整主动换人错误 = %v，期望 switchPrevented", err)
	}
}

// TestResolveTurnRechargeDoesNotStartAfterSubstituteDamage 验证替身承伤不是目标本体生命损失，不能触发休整。
func TestResolveTurnRechargeDoesNotStartAfterSubstituteDamage(t *testing.T) {
	t.Parallel()
	left := rechargeAttacker(1, "recharge-substitute-user")
	left.Stats.Speed = 100
	right := rechargePassiveMember(1, "recharge-substitute-target")
	right.Skills[0].Priority = 1
	right.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
		ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
	}}

	result, err := battleengine.ResolveTurn(volatileState(t, left, right), volatileTurn(1, 1, 1), mustRandom(t, 65))
	if err != nil {
		t.Fatalf("替身承伤 ResolveTurn() error = %v", err)
	}
	user, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found || user.RechargeTurnsRemaining != 0 || rechargeStarted(result.Events, battleengine.SideOne) {
		t.Fatalf("替身承伤错误触发休整: user=%+v events=%+v", user, result.Events)
	}
}

// rechargeAttacker 构造一项不依赖伤害随机数、成功命中后需要休整的伤害技能使用者。
func rechargeAttacker(position battleengine.MemberPosition, id string) battleengine.MemberSnapshot {
	member := newMember(position, id, 500, 500)
	member.Skills[0].SkillID = testID(id + "-skill")
	member.Skills[0].DamageMode = battleengine.SkillDamageModeFixedAmount
	member.Skills[0].DamageAmount = 100
	member.Skills[0].RechargesAfterUse = true
	return member
}

// rechargePassiveMember 构造不改变当前战斗断言的合法变化技能成员，避免无关伤害影响休整边界。
func rechargePassiveMember(position battleengine.MemberPosition, id string) battleengine.MemberSnapshot {
	member := newMember(position, id, 500, 500)
	member.Skills[0].DamageClass = battleengine.DamageClassStatus
	member.Skills[0].Power = 0
	member.Skills[0].HealingPercent = 1
	return member
}

// rechargeStarted 报告事件流是否已为指定阵营成员记录休整开始事实。
func rechargeStarted(events []battleengine.Event, side battleengine.Side) bool {
	for _, event := range events {
		value, ok := event.(battleengine.RechargeStartedEvent)
		if ok && value.Actor.Side == side {
			return true
		}
	}
	return false
}
