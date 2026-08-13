package battleengine_test

import (
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnTauntPreventsStatusSkill 验证挑衅在更快成员命中后会阻止目标同回合使用变化技能，
// 且被阻止的技能不会消费 PP。
func TestResolveTurnTauntPreventsStatusSkill(t *testing.T) {
	t.Parallel()
	left := newMember(1, "taunt-user", 400, 400)
	left.Stats.Speed = 200
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusTaunt, Target: battleengine.EffectTargetSelected,
		ChancePercent: 100, MinTurns: 2, MaxTurns: 2,
	}}
	right := newMember(1, "taunt-target", 400, 400)
	right.Stats.Speed = 10
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetUser, StageDelta: 1, ChancePercent: 100,
	}}

	result, err := battleengine.ResolveTurn(volatileState(t, left, right), volatileTurn(1, 1, 1), mustRandom(t, 11))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if target.TauntTurnsRemaining != 1 || target.Skills[0].RemainingPP != target.Skills[0].MaxPP {
		t.Fatalf("挑衅后的目标状态 = %+v", target)
	}
	if !volatilePreventionExists(result.Events, battleengine.SideTwo, battleengine.SkillPreventionReasonTaunt) {
		t.Fatalf("事件未记录挑衅阻止: %+v", result.Events)
	}
}

// TestResolveTurnBindingBlocksSwitchAndDamagesAtEnd 验证束缚会在回合末扣血、递减时长，且下回合的
// 主动换人会在命令边界被明确拒绝。
func TestResolveTurnBindingBlocksSwitchAndDamagesAtEnd(t *testing.T) {
	t.Parallel()
	left := newMember(1, "binding-user", 500, 500)
	left.Stats.Speed = 200
	left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusBinding, Target: battleengine.EffectTargetSelected,
		ChancePercent: 100, MinTurns: 2, MaxTurns: 2,
	}}
	right := newMember(1, "binding-target", 500, 500)
	right.Stats.Speed = 10
	reserve := newMember(2, "binding-reserve", 500, 500)
	reserve.Stats.Speed = 5
	state := volatileStateWithReserve(t, left, right, reserve)

	result, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 12))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if target.BindingTurnsRemaining != 1 || target.CurrentHP >= target.MaxHP {
		t.Fatalf("束缚后的目标状态 = %+v", target)
	}
	_, err = battleengine.ResolveTurn(result.State, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 2,
		Actions: []battleengine.Action{
			volatileSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
		},
	}, mustRandom(t, 13))
	var commandError *battleengine.TurnCommandError
	if !errors.As(err, &commandError) || commandError.Code != battleengine.TurnCommandErrorSwitchPrevented {
		t.Fatalf("绑定换人错误 = %v，期望 switchPrevented", err)
	}
}

// TestResolveTurnHeldItemBindingDurationOverride 验证紧缠钩爪类道具把新建立的束缚固定为七次回合末
// 结算；本回合结算一次后剩余六次，道具保持持有且随机轨迹不再为持续时间额外掷骰。
func TestResolveTurnHeldItemBindingDurationOverride(t *testing.T) {
	t.Parallel()
	left := newMember(1, "binding-duration-user", 600, 600)
	left.Stats.Speed = 200
	left.ItemID = testID("grip-claw")
	left.HeldItemBindingTurns = 7
	left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusBinding, Target: battleengine.EffectTargetSelected,
		ChancePercent: 100, MinTurns: 2, MaxTurns: 5,
	}}
	right := newMember(1, "binding-duration-target", 600, 600)
	right.Stats.Speed = 10
	result, err := battleengine.ResolveTurn(volatileState(t, left, right), volatileTurn(1, 1, 1), mustRandom(t, 120))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	actor, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if target.BindingTurnsRemaining != 6 || actor.ItemID != testID("grip-claw") || actor.HeldItemBindingTurns != 7 {
		t.Fatalf("束缚持续时间或道具快照错误: target=%+v actor=%+v", target, actor)
	}
}

// TestResolveTurnHeldItemBindingDamageOverride 验证紧绑束带类道具把本次新束缚的回合末伤害冻结为
// 最大生命六分之一；规则写入目标状态后不依赖后续实时道具读取。
func TestResolveTurnHeldItemBindingDamageOverride(t *testing.T) {
	t.Parallel()
	left := newMember(1, "binding-damage-user", 600, 600)
	left.Stats.Speed = 200
	left.ItemID = testID("binding-band")
	left.HeldItemBindingDamageDenominator = 6
	left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusBinding, Target: battleengine.EffectTargetSelected,
		ChancePercent: 100, MinTurns: 2, MaxTurns: 2,
	}}
	right := newMember(1, "binding-damage-target", 600, 600)
	right.Stats.Speed = 10
	result, err := battleengine.ResolveTurn(volatileState(t, left, right), volatileTurn(1, 1, 1), mustRandom(t, 121))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	var damage uint32
	for _, event := range result.Events {
		if applied, ok := event.(battleengine.VolatileStatusDamageAppliedEvent); ok && applied.Status == battleengine.VolatileStatusBinding {
			damage = applied.Amount
		}
	}
	target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if damage != 100 || target.BindingDamageDenominator != 6 || target.BindingTurnsRemaining != 1 {
		t.Fatalf("束缚伤害冻结结果: damage=%d target=%+v", damage, target)
	}
}

// TestResolveTurnSwitchRestrictionImmunityBypassesBinding 验证专用道具豁免同时绕过敌方特性与仍有效的束缚限制。
//
// 它不能绕过蓄力或锁招，因为这两类限制要求成员继续完成自身已承诺的技能；将三种来源写成同一条件会改变
// 强制行动的回合顺序和随机轨迹。
func TestResolveTurnSwitchRestrictionImmunityBypassesBinding(t *testing.T) {
	t.Parallel()
	left := newMember(1, "binding-immunity-opponent", 500, 500)
	left.Stats.Speed = 200
	left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusBinding, Target: battleengine.EffectTargetSelected,
		ChancePercent: 100, MinTurns: 2, MaxTurns: 2,
	}}
	right := newMember(1, "binding-immunity-target", 500, 500)
	right.Stats.Speed = 10
	right.SwitchRestrictionImmunity = true
	reserve := newMember(2, "binding-immunity-reserve", 500, 500)

	result, err := battleengine.ResolveTurn(volatileStateWithReserve(t, left, right, reserve), volatileTurn(1, 1, 1), mustRandom(t, 15))
	if err != nil {
		t.Fatalf("束缚回合 ResolveTurn() error = %v", err)
	}
	bound, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || bound.BindingTurnsRemaining == 0 {
		t.Fatalf("未建立用于豁免校验的束缚状态: %+v", bound)
	}
	result, err = battleengine.ResolveTurn(result.State, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 2,
		Actions: []battleengine.Action{
			volatileSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
		},
	}, mustRandom(t, 16))
	if err != nil {
		t.Fatalf("豁免换人 ResolveTurn() error = %v", err)
	}
	if active, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}); !found || active.Position != 2 {
		t.Fatalf("束缚豁免后的上场成员 = %+v, found=%t", active, found)
	}
}

// TestResolveTurnProtectionBlocksDamageAndExpiresAtEnd 验证保护技能先写入使用者状态，随后阻止对方
// 技能的伤害和附加效果；它不伪装为命中失败，也不会额外消耗命中随机数，并在当前回合结束时清除。
func TestResolveTurnProtectionBlocksDamageAndExpiresAtEnd(t *testing.T) {
	t.Parallel()

	left := newMember(1, "protection-attacker", 500, 500)
	left.Stats.Speed = 200
	left.Skills[0].Accuracy = 50
	right := newMember(1, "protection-user", 500, 500)
	right.Stats.Speed = 10
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].Priority = 1
	right.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusProtection, Target: battleengine.EffectTargetUser,
		ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
	}}

	result, err := battleengine.ResolveTurn(volatileState(t, left, right), volatileTurn(1, 1, 1), mustRandom(t, 12))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	protected, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	attacker, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if protected.CurrentHP != protected.MaxHP || protected.ProtectionTurnsRemaining != 0 || protected.ProtectionChain != 1 ||
		attacker.Skills[0].RemainingPP != attacker.Skills[0].MaxPP-1 {
		t.Fatalf("保护回合后的成员状态 = attacker:%+v protected:%+v", attacker, protected)
	}
	if len(result.RandomTrace) != 0 {
		t.Fatalf("保护不应消耗命中随机数，轨迹 = %+v", result.RandomTrace)
	}
	if !volatileSkillBlockExists(result.Events, battleengine.SkillBlockReasonProtection) {
		t.Fatalf("事件未记录保护阻止: %+v", result.Events)
	}
	if !volatileProtectionChainInSummary(result.State.Summary(), battleengine.SideTwo, 1) {
		t.Fatalf("状态摘要未记录保护连续次数: %+v", result.State.Summary())
	}

	trace, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 3, Reason: "protection chance for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 1},
		{Sequence: 2, Bound: 100, Reason: "accuracy for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 1},
		{Sequence: 3, Bound: 24, Reason: "critical hit for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 1},
		{Sequence: 4, Bound: 16, Reason: "damage random for " + testID("00000000-0000-0000-0000-000000000020").String(), Value: 1},
	})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	failed, err := battleengine.ResolveTurn(result.State, volatileTurn(2, 1, 1), trace)
	if err != nil {
		t.Fatalf("连续保护失败 ResolveTurn() error = %v", err)
	}
	protected, _ = failed.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if protected.CurrentHP >= protected.MaxHP || protected.ProtectionTurnsRemaining != 0 || protected.ProtectionChain != 0 {
		t.Fatalf("连续保护失败后的成员状态 = %+v", protected)
	}
	if !volatileSkillFailureExists(failed.Events, battleengine.SkillFailureReasonProtectionFailed) {
		t.Fatalf("事件未记录连续保护失败: %+v", failed.Events)
	}
}

// TestResolveTurnChargeConsumesPPOnlyOnPreparation 验证蓄力技能先进入强制准备状态，完成段必须重复同一
// 技能，并且整次两段行动只消费一次 PP。
func TestResolveTurnChargeConsumesPPOnlyOnPreparation(t *testing.T) {
	t.Parallel()
	left := newMember(1, "charge-user", 500, 500)
	left.Stats.Speed = 200
	left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusCharging, Target: battleengine.EffectTargetUser,
		ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
	}}
	right := newMember(1, "charge-target", 500, 500)
	right.Stats.Speed = 10
	state := volatileState(t, left, right)

	prepared, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 14))
	if err != nil {
		t.Fatalf("准备回合 ResolveTurn() error = %v", err)
	}
	preparedUser, _ := prepared.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	preparedTarget, _ := prepared.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if preparedUser.ChargingSkillPosition != 1 || preparedUser.Skills[0].RemainingPP != preparedUser.Skills[0].MaxPP-1 || preparedTarget.CurrentHP != preparedTarget.MaxHP {
		t.Fatalf("蓄力准备状态 = user:%+v target:%+v", preparedUser, preparedTarget)
	}
	completed, err := battleengine.ResolveTurn(prepared.State, volatileTurn(2, 1, 1), mustRandom(t, 15))
	if err != nil {
		t.Fatalf("完成回合 ResolveTurn() error = %v", err)
	}
	completedUser, _ := completed.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	completedTarget, _ := completed.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if completedUser.ChargingSkillPosition != 0 || completedUser.Skills[0].RemainingPP != completedUser.Skills[0].MaxPP-1 || completedTarget.CurrentHP >= completedTarget.MaxHP {
		t.Fatalf("蓄力完成状态 = user:%+v target:%+v", completedUser, completedTarget)
	}
}

// TestResolveTurnWeatherSkipsCharge 验证技能只在资料明确列出的普通天气下跳过首次蓄力，并在同一回合正常造成伤害。
// 未匹配天气继续使用原有两段流程，避免把环境特例错误扩展为所有蓄力技能的全局捷径。
func TestResolveTurnWeatherSkipsCharge(t *testing.T) {
	t.Parallel()
	left := newMember(1, "weather-charge-user", 500, 500)
	left.Stats.Speed = 200
	left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusCharging, Target: battleengine.EffectTargetUser,
		ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
	}}
	left.Skills[0].ChargeSkippedWeathers = []battleengine.WeatherKind{battleengine.WeatherKindSun}
	right := newMember(1, "weather-charge-target", 500, 500)
	right.Stats.Speed = 10
	state := newWeatherState(t, battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{
		Kind: battleengine.WeatherKindSun, TurnsRemaining: 3,
	}}, battleengine.RuleSnapshot{SchemaVersion: 1}, left, right)

	resolved, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 118))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	user, _ := resolved.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	target, _ := resolved.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if user.ChargingSkillPosition != 0 || user.ChargingTurnsRemaining != 0 || user.Skills[0].RemainingPP != user.Skills[0].MaxPP-1 || target.CurrentHP >= target.MaxHP {
		t.Fatalf("天气跳过蓄力后状态 = user:%+v target:%+v", user, target)
	}
}

// TestResolveTurnChargeSkipOnceItem 验证一次性蓄力跳过道具在首次蓄力行动消费 PP 后立即生效。
// 它不能伪造蓄力易变状态、重复扣除 PP，亦不能被天气跳过蓄力或普通技能错误消耗。
func TestResolveTurnChargeSkipOnceItem(t *testing.T) {
	t.Parallel()
	t.Run("消耗道具并直接结算", func(t *testing.T) {
		left := newMember(1, "charge-skip-item-user", 500, 500)
		left.Stats.Speed = 200
		left.ItemID = testID("charge-skip-item")
		left.ChargeSkipOnce = true
		left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
			Status: battleengine.VolatileStatusCharging, Target: battleengine.EffectTargetUser,
			ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
		}}
		right := newMember(1, "charge-skip-item-target", 500, 500)
		right.Stats.Speed = 10

		result, err := battleengine.ResolveTurn(volatileState(t, left, right), volatileTurn(1, 1, 1), mustRandom(t, 119))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		user, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
		target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
		if user.ItemID != 0 || user.ChargeSkipOnce || user.ChargingSkillPosition != 0 || user.ChargingTurnsRemaining != 0 ||
			user.Skills[0].RemainingPP != user.Skills[0].MaxPP-1 || target.CurrentHP >= target.MaxHP {
			t.Fatalf("一次性蓄力跳过后的成员状态 = user:%+v target:%+v", user, target)
		}
		usedIndex, skippedIndex := -1, -1
		for index, event := range result.Events {
			if used, ok := event.(battleengine.SkillUsedEvent); ok && used.Actor == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}) && used.SkillID == left.Skills[0].SkillID {
				usedIndex = index
			}
			if skipped, ok := event.(battleengine.SkillChargeSkippedByItemEvent); ok {
				if skipped.Actor != (battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}) || skipped.SkillID != left.Skills[0].SkillID || skipped.ItemID != testID("charge-skip-item") {
					t.Fatalf("蓄力跳过事件 = %+v", skipped)
				}
				skippedIndex = index
			}
		}
		if usedIndex < 0 || skippedIndex < 0 || usedIndex >= skippedIndex {
			t.Fatalf("技能使用与蓄力跳过事件顺序错误 = %+v", result.Events)
		}
	})

	t.Run("普通技能和天气跳过都不消费道具", func(t *testing.T) {
		left := newMember(1, "charge-skip-item-guard", 500, 500)
		left.Stats.Speed = 200
		left.ItemID = testID("charge-skip-item")
		left.ChargeSkipOnce = true
		right := newMember(1, "charge-skip-item-guard-target", 500, 500)
		right.Stats.Speed = 10

		normal, err := battleengine.ResolveTurn(volatileState(t, left, right), volatileTurn(1, 1, 1), mustRandom(t, 120))
		if err != nil {
			t.Fatalf("普通技能 ResolveTurn() error = %v", err)
		}
		user, _ := normal.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
		if user.ItemID != testID("charge-skip-item") || !user.ChargeSkipOnce || chargeSkippedByItemExists(normal.Events) {
			t.Fatalf("普通技能错误消费蓄力跳过道具 = user:%+v events:%+v", user, normal.Events)
		}

		left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
			Status: battleengine.VolatileStatusCharging, Target: battleengine.EffectTargetUser,
			ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
		}}
		left.Skills[0].ChargeSkippedWeathers = []battleengine.WeatherKind{battleengine.WeatherKindSun}
		weatherState := newWeatherState(t, battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{
			Kind: battleengine.WeatherKindSun, TurnsRemaining: 3,
		}}, battleengine.RuleSnapshot{SchemaVersion: 1}, left, right)
		weather, err := battleengine.ResolveTurn(weatherState, volatileTurn(1, 1, 1), mustRandom(t, 121))
		if err != nil {
			t.Fatalf("天气跳过蓄力 ResolveTurn() error = %v", err)
		}
		user, _ = weather.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
		if user.ItemID != testID("charge-skip-item") || !user.ChargeSkipOnce || chargeSkippedByItemExists(weather.Events) {
			t.Fatalf("天气跳过蓄力错误消费道具 = user:%+v events:%+v", user, weather.Events)
		}
	})
}

// TestResolveTurnLockForcesTheDeclaredSkill 验证锁招会把首段成功技能的剩余重复次数写入状态，并在下回合
// 拒绝换技能；最后一次强制使用后状态自动清理。
func TestResolveTurnLockForcesTheDeclaredSkill(t *testing.T) {
	t.Parallel()
	left := newMember(1, "lock-user", 500, 500)
	left.Stats.Speed = 200
	second := left.Skills[0]
	second.Position = 2
	second.SkillID = testID("lock-alternative")
	left.Skills = append(left.Skills, second)
	left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusLockedMove, Target: battleengine.EffectTargetUser,
		ChancePercent: 100, MinTurns: 2, MaxTurns: 2,
	}}
	right := newMember(1, "lock-target", 500, 500)
	right.Stats.Speed = 10
	first, err := battleengine.ResolveTurn(volatileState(t, left, right), volatileTurn(1, 1, 1), mustRandom(t, 16))
	if err != nil {
		t.Fatalf("首段 ResolveTurn() error = %v", err)
	}
	user, _ := first.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if user.LockedSkillPosition != 1 || user.LockedTurnsRemaining != 1 {
		t.Fatalf("锁招首段状态 = %+v", user)
	}
	_, err = battleengine.ResolveTurn(first.State, volatileTurn(2, 2, 1), mustRandom(t, 17))
	var commandError *battleengine.TurnCommandError
	if !errors.As(err, &commandError) || commandError.Code != battleengine.TurnCommandErrorForcedSkill {
		t.Fatalf("锁招换技能错误 = %v，期望 forcedSkill", err)
	}
	completed, err := battleengine.ResolveTurn(first.State, volatileTurn(2, 1, 1), mustRandom(t, 18))
	if err != nil {
		t.Fatalf("锁招完成 ResolveTurn() error = %v", err)
	}
	user, _ = completed.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if user.LockedSkillPosition != 0 || user.LockedTurnsRemaining != 0 {
		t.Fatalf("锁招完成状态 = %+v", user)
	}
}

// TestResolveTurnDisableUsesTargetLastDeclaredSkill 验证定身只锁定目标实际宣告过的稳定技能槽，不依赖
// 名称匹配；下一回合选择该技能会在 PP 消耗前得到明确阻止事件。
func TestResolveTurnDisableUsesTargetLastDeclaredSkill(t *testing.T) {
	t.Parallel()
	left := newMember(1, "disable-target", 500, 500)
	left.Stats.Speed = 200
	right := newMember(1, "disable-user", 500, 500)
	right.Stats.Speed = 10
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusDisable, Target: battleengine.EffectTargetSelected,
		ChancePercent: 100, MinTurns: 2, MaxTurns: 2,
	}}
	first, err := battleengine.ResolveTurn(volatileState(t, left, right), volatileTurn(1, 1, 1), mustRandom(t, 19))
	if err != nil {
		t.Fatalf("定身首回合 ResolveTurn() error = %v", err)
	}
	target, _ := first.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if target.DisabledSkillPosition != 1 || target.DisabledTurnsRemaining != 2 {
		t.Fatalf("定身首回合状态 = %+v", target)
	}
	second, err := battleengine.ResolveTurn(first.State, volatileTurn(2, 1, 1), mustRandom(t, 20))
	if err != nil {
		t.Fatalf("定身第二回合 ResolveTurn() error = %v", err)
	}
	target, _ = second.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if target.Skills[0].RemainingPP != target.Skills[0].MaxPP-1 || !volatilePreventionExists(second.Events, battleengine.SideOne, battleengine.SkillPreventionReasonDisable) {
		t.Fatalf("定身第二回合状态或事件 = target:%+v events:%+v", target, second.Events)
	}
}

// TestResolveTurnConfusionCanDamageSelf 验证混乱在目标获得行动机会时使用独立随机接点、阻止技能且
// 造成最大生命八分之一的直接自身伤害。
func TestResolveTurnConfusionCanDamageSelf(t *testing.T) {
	t.Parallel()
	left := newMember(1, "confusion-user", 400, 400)
	left.Stats.Speed = 200
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusConfusion, Target: battleengine.EffectTargetSelected,
		ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
	}}
	right := newMember(1, "confusion-target", 400, 400)
	right.Stats.Speed = 10
	random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{{
		Sequence: 1, Bound: 3, Reason: "confusion chance for side 2 member 1", Value: 0,
	}})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(volatileState(t, left, right), volatileTurn(1, 1, 1), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	target, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if target.CurrentHP != 350 || target.ConfusionTurnsRemaining != 0 || target.Skills[0].RemainingPP != target.Skills[0].MaxPP {
		t.Fatalf("混乱目标状态 = %+v", target)
	}
	if !volatilePreventionExists(result.Events, battleengine.SideTwo, battleengine.SkillPreventionReasonConfusion) {
		t.Fatalf("事件未记录混乱阻止: %+v", result.Events)
	}
}

// TestResolveTurnSubstituteAbsorbsDamageAndBlocksTargetEffects 验证替身使用独立生命承受对方伤害，
// 破裂后仍会阻止本次命中附带的异常、能力阶级、易变状态和畏缩效果。
func TestResolveTurnSubstituteAbsorbsDamageAndBlocksTargetEffects(t *testing.T) {
	t.Parallel()
	left := newMember(1, "substitute-user", 400, 400)
	left.Stats.Speed = 200
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
		ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
	}}
	right := newMember(1, "substitute-attacker", 400, 200)
	right.Stats.Speed = 10
	right.Skills[0].DamageMode = battleengine.SkillDamageModeFixedAmount
	right.Skills[0].DamageAmount = 150
	right.Skills[0].FlinchChancePercent = 100
	right.Skills[0].DrainPercent = 50
	right.Skills[0].StatusApplications = []battleengine.MajorStatusApplication{{
		Status: battleengine.MajorStatusSleep, Target: battleengine.EffectTargetSelected, ChancePercent: 100,
	}}
	right.Skills[0].StatStageEffects = []battleengine.StatStageEffect{{
		Stat: battleengine.StatAttack, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 100,
	}}
	right.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusTaunt, Target: battleengine.EffectTargetSelected,
		ChancePercent: 100, MinTurns: 2, MaxTurns: 2,
	}}

	result, err := battleengine.ResolveTurn(volatileState(t, left, right), volatileTurn(1, 1, 1), mustRandom(t, 21))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	user, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	attacker, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if user.CurrentHP != 300 || user.SubstituteHP != 0 || user.MajorStatus != "" || user.FlinchedTurn != 0 ||
		user.StatStages[battleengine.StatAttack] != 0 || user.TauntTurnsRemaining != 0 {
		t.Fatalf("替身破裂后的目标状态 = %+v", user)
	}
	if attacker.CurrentHP != 250 {
		t.Fatalf("吸取应以替身实际承伤 100 为基数，攻击者状态 = %+v", attacker)
	}
	if !volatileSubstituteEventExists(result.Events, battleengine.EventKindSubstituteStarted) ||
		!volatileSubstituteEventExists(result.Events, battleengine.EventKindSubstituteDamageApplied) ||
		!volatileSubstituteEventExists(result.Events, battleengine.EventKindSubstituteBroken) {
		t.Fatalf("事件未完整记录替身建立、承伤和破裂: %+v", result.Events)
	}
	for _, event := range result.Events {
		if damage, ok := event.(battleengine.DamageAppliedEvent); ok && damage.Target.Side == battleengine.SideOne {
			t.Fatalf("替身承伤不应产生目标本体伤害事件: %+v", result.Events)
		}
	}
}

// TestResolveTurnSubstituteRejectsRepeatedUseAndInsufficientHP 验证重复建立和生命不足都会在 PP 已消费后
// 产生明确失败原因，而不会修改既有替身或本体生命。
func TestResolveTurnSubstituteRejectsRepeatedUseAndInsufficientHP(t *testing.T) {
	t.Parallel()
	left := newMember(1, "substitute-repeat-user", 400, 400)
	left.Stats.Speed = 200
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
		ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
	}}
	right := newMember(1, "substitute-repeat-target", 400, 400)
	right.Stats.Speed = 10
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].HealingPercent = 1

	first, err := battleengine.ResolveTurn(volatileState(t, left, right), volatileTurn(1, 1, 1), mustRandom(t, 22))
	if err != nil {
		t.Fatalf("首次建立替身 ResolveTurn() error = %v", err)
	}
	second, err := battleengine.ResolveTurn(first.State, volatileTurn(2, 1, 1), mustRandom(t, 23))
	if err != nil {
		t.Fatalf("重复建立替身 ResolveTurn() error = %v", err)
	}
	user, _ := second.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if user.CurrentHP != 300 || user.SubstituteHP != 100 || !volatileSkillFailureExists(second.Events, battleengine.SkillFailureReasonSubstituteAlreadyActive) {
		t.Fatalf("重复建立替身后的状态或事件 = user:%+v events:%+v", user, second.Events)
	}

	lowHP := newMember(1, "substitute-low-hp-user", 400, 100)
	lowHP.Stats.Speed = 200
	lowHP.Skills[0].DamageClass = battleengine.DamageClassStatus
	lowHP.Skills[0].Power = 0
	lowHP.Skills[0].VolatileStatusApplications = left.Skills[0].VolatileStatusApplications
	failed, err := battleengine.ResolveTurn(volatileState(t, lowHP, right), volatileTurn(1, 1, 1), mustRandom(t, 24))
	if err != nil {
		t.Fatalf("生命不足建立替身 ResolveTurn() error = %v", err)
	}
	user, _ = failed.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if user.CurrentHP != 100 || user.SubstituteHP != 0 || !volatileSkillFailureExists(failed.Events, battleengine.SkillFailureReasonInsufficientHPForSubstitute) {
		t.Fatalf("生命不足建立替身后的状态或事件 = user:%+v events:%+v", user, failed.Events)
	}
}

// TestResolveTurnSwitchClearsSubstitute 验证替身只属于成员当前连续在场周期，主动换人后不会随成员
// 保留到后备或下一次上场。
func TestResolveTurnSwitchClearsSubstitute(t *testing.T) {
	t.Parallel()
	left := newMember(1, "substitute-switch-user", 400, 400)
	left.Stats.Speed = 200
	left.Skills[0].DamageClass = battleengine.DamageClassStatus
	left.Skills[0].Power = 0
	left.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
		ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
	}}
	reserve := newMember(2, "substitute-switch-reserve", 400, 400)
	right := newMember(1, "substitute-switch-target", 400, 400)
	right.Stats.Speed = 10
	right.Skills[0].DamageClass = battleengine.DamageClassStatus
	right.Skills[0].Power = 0
	right.Skills[0].HealingPercent = 1
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "substitute-switch", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left, reserve}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	created, err := battleengine.ResolveTurn(state, volatileTurn(1, 1, 1), mustRandom(t, 25))
	if err != nil {
		t.Fatalf("建立替身 ResolveTurn() error = %v", err)
	}
	switched, err := battleengine.ResolveTurn(created.State, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 2,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
			volatileSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		},
	}, mustRandom(t, 26))
	if err != nil {
		t.Fatalf("换人 ResolveTurn() error = %v", err)
	}
	for _, side := range switched.State.Snapshot().Sides {
		if side.Side != battleengine.SideOne {
			continue
		}
		for _, member := range side.Members {
			if member.Position == 1 && (member.CurrentHP != 300 || member.SubstituteHP != 0) {
				t.Fatalf("换出成员的替身状态 = %+v", member)
			}
		}
	}
}

// volatileState 创建两个单打成员的最小有效状态。
func volatileState(t *testing.T, left, right battleengine.MemberSnapshot) battleengine.State {
	t.Helper()
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "volatile-status", ActiveSlotsPerSide: 1, TeamSize: 1},
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

// volatileStateWithReserve 为束缚换人校验准备一名同侧后备成员。
func volatileStateWithReserve(t *testing.T, left, right, reserve battleengine.MemberSnapshot) battleengine.State {
	t.Helper()
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "volatile-status", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{left}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{right, reserve}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	return state
}

// volatileTurn 返回双方使用指定技能槽的完整单打回合。
func volatileTurn(turn uint32, leftSkill, rightSkill battleengine.SkillPosition) battleengine.TurnCommand {
	return battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: turn, Actions: []battleengine.Action{
		volatileSkillAction(battleengine.SideOne, leftSkill, battleengine.SideTwo, 1),
		volatileSkillAction(battleengine.SideTwo, rightSkill, battleengine.SideOne, 1),
	}}
}

// volatileSkillAction 构造一份单打技能行动。
func volatileSkillAction(side battleengine.Side, skill battleengine.SkillPosition, targetSide battleengine.Side, targetSlot battleengine.SlotPosition) battleengine.Action {
	return battleengine.Action{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: side, Position: 1}, UseSkill: &battleengine.UseSkillAction{
		SkillPosition: skill, Target: battleengine.SlotRef{Side: targetSide, Position: targetSlot},
	}}
}

// mustRandom 创建可复现随机源，失败时立即标记测试失败。
func mustRandom(t *testing.T, seed uint64) battleengine.RandomSource {
	t.Helper()
	random, err := battleengine.NewRandomSource(battleengine.RandomAlgorithmSplitMix64V1, seed)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	return random
}

// chargeSkippedByItemExists 报告事件流是否记录了一次性道具跳过蓄力的事实。
func chargeSkippedByItemExists(events []battleengine.Event) bool {
	for _, event := range events {
		if _, ok := event.(battleengine.SkillChargeSkippedByItemEvent); ok {
			return true
		}
	}
	return false
}

// volatilePreventionExists 报告事件流是否包含指定成员和原因的行动阻止事实。
func volatilePreventionExists(events []battleengine.Event, side battleengine.Side, reason battleengine.SkillPreventionReason) bool {
	for _, event := range events {
		value, ok := event.(battleengine.SkillPreventedEvent)
		if ok && value.Actor.Side == side && value.Reason == reason {
			return true
		}
	}
	return false
}

// volatileSkillBlockExists 报告事件流是否包含指定的确定性技能阻止事实。
func volatileSkillBlockExists(events []battleengine.Event, reason battleengine.SkillBlockReason) bool {
	for _, event := range events {
		value, ok := event.(battleengine.SkillBlockedEvent)
		if ok && value.Reason == reason {
			return true
		}
	}
	return false
}

// volatileSkillFailureExists 报告事件流是否包含指定的、已宣告技能自身失败事实。
func volatileSkillFailureExists(events []battleengine.Event, reason battleengine.SkillFailureReason) bool {
	for _, event := range events {
		value, ok := event.(battleengine.SkillFailedEvent)
		if ok && value.Reason == reason {
			return true
		}
	}
	return false
}

// volatileSubstituteEventExists 报告事件流是否包含指定的替身生命周期事件种类。
func volatileSubstituteEventExists(events []battleengine.Event, kind battleengine.EventKind) bool {
	for _, event := range events {
		if event.Kind() == kind {
			return true
		}
	}
	return false
}

// volatileProtectionChainInSummary 报告回放状态摘要是否保留了指定成员的保护连续成功次数。
func volatileProtectionChainInSummary(summary battleengine.StateSummary, side battleengine.Side, want uint8) bool {
	for _, member := range summary.Members {
		if member.Side == side && member.MemberPosition == 1 {
			return member.ProtectionChain == want
		}
	}
	return false
}
