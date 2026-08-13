package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestInitialStateAppliesSwitchInTransformIntoOpponent 验证初始入场会复制对手画像并保存原始画像。
func TestInitialStateAppliesSwitchInTransformIntoOpponent(t *testing.T) {
	t.Parallel()
	receiver := newMember(1, "transform-receiver", 1_000, 1_000)
	receiver.SwitchInTransformIntoOpponent = true
	receiver.Stats.Attack = 101
	receiver.CriticalHitImmunity = true
	receiver.SkillRecoilDamageImmunity = true
	receiver.IndirectDamageImmunity = true
	receiver.ContactDamageToAttackerDenominator = 8
	receiver.IgnoreOpponentDamageStatStages = true
	receiver.IgnoreTargetAbilityEffects = true
	receiver.SurviveFatalDamageAtFullHP = true
	receiver.OpponentStatusSkillImmunity = true
	receiver.NonSuperEffectiveDamageImmunity = true
	receiver.ContactSkillProtectionBypass = true
	receiver.ContactSuppression = true
	receiver.ReceivedContactDamageHalved = true
	receiver.ReceivedFireDamageDoubled = true
	receiver.HeldItemContactDamageToAttackerDenominator = 6
	receiver.HeldItemEndTurnHealDenominator = 16
	receiver.HeldItemEndTurnHealForElementID = testID("receiver-element")
	receiver.HeldItemEndTurnHealForElementDenominator = 8
	receiver.HeldItemEndTurnDamageDenominator = 8
	receiver.HeldItemEndTurnDamageWithoutElementID = testID("receiver-damage-element")
	receiver.HeldItemEndTurnDamageWithoutElementDenominator = 16
	receiver.ContactTransferToAttacker = true
	receiver.ChargeSkipOnce = true
	receiver.HeldItemSurviveFatalDamageAtFullHP = true
	receiver.HeldItemReflectTurnsRemaining = 8
	receiver.HeldItemLightScreenTurnsRemaining = 8
	receiver.HeldItemAuroraVeilTurnsRemaining = 8
	receiver.HeldItemRainTurnsRemaining = 8
	receiver.HeldItemSandstormTurnsRemaining = 8
	receiver.HeldItemSnowTurnsRemaining = 8
	receiver.HeldItemSunTurnsRemaining = 8
	receiver.HeldItemTerrainTurnsRemaining = 8
	receiver.HeldItemSandstormDamageImmunity = true
	receiver.HeldItemWeightHalf = true
	receiver.HeldItemCuresParalysis = true
	receiver.HeldItemCuresSleep = true
	receiver.HeldItemCuresPoison = true
	receiver.HeldItemCuresBurn = true
	receiver.HeldItemCuresFreeze = true
	receiver.HeldItemCuresAllMajorStatuses = true
	receiver.HeldItemCuresConfusion = true
	receiver.HeldItemPunchBasedSkillPowerBoost = true
	receiver.HeldItemPhysicalDamagePowerBoost = true
	receiver.HeldItemSpecialDamagePowerBoost = true
	receiver.HeldItemElementDamageReductionElementID = testID("receiver-resistance-element")
	receiver.HeldItemElementDamageReductionRequiresSuperEffective = true
	receiver.HeldItemSuperEffectiveDamageBoost = true
	receiver.HeldItemDamageBoostWithRecoil = true
	receiver.HeldItemPunchBasedContactSuppression = true
	receiver.HeldItemMultiHitCountMinimum = 4
	receiver.HeldItemMultiHitCountMaximum = 5
	receiver.HeldItemMultiHitRequiredMinimum = 2
	receiver.HeldItemMultiHitRequiredMaximum = 5
	receiver.Skills[0].SkillID = testID("00000000-0000-0000-0000-000000000101")
	receiver.OpponentSwitchRestriction = &battleengine.OpponentSwitchRestriction{RequiredTargetElementID: testID("receiver-element")}
	target := newMember(1, "transform-target", 1_000, 1_000)
	target.Stats.Attack = 303
	target.CriticalHitImmunity = false
	target.SkillRecoilDamageImmunity = false
	target.IgnoreOpponentDamageStatStages = false
	target.IgnoreTargetAbilityEffects = false
	target.SurviveFatalDamageAtFullHP = false
	target.OpponentStatusSkillImmunity = false
	target.NonSuperEffectiveDamageImmunity = false
	target.ContactSkillProtectionBypass = false
	target.Skills[0].SkillID = testID("00000000-0000-0000-0000-000000000202")
	target.OpponentSwitchRestriction = &battleengine.OpponentSwitchRestriction{RequiredTargetElementID: testID("target-element"), RequiresGroundedTarget: true}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-transform-initial", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{receiver}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	member, ok := state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !ok || member.CreatureID != target.CreatureID || member.Stats.Attack != target.Stats.Attack || member.Skills[0].SkillID != target.Skills[0].SkillID {
		t.Fatalf("变身后画像 = %+v", member)
	}
	if member.TransformSnapshot == nil || member.TransformSnapshot.CreatureID != receiver.CreatureID || member.TransformSnapshot.Stats.Attack != 101 ||
		!member.TransformSnapshot.CriticalHitImmunity || !member.TransformSnapshot.SkillRecoilDamageImmunity || !member.TransformSnapshot.IndirectDamageImmunity || member.TransformSnapshot.ContactDamageToAttackerDenominator != 8 || !member.TransformSnapshot.IgnoreOpponentDamageStatStages ||
		!member.TransformSnapshot.IgnoreTargetAbilityEffects || !member.TransformSnapshot.SurviveFatalDamageAtFullHP || !member.TransformSnapshot.OpponentStatusSkillImmunity || !member.TransformSnapshot.NonSuperEffectiveDamageImmunity || !member.TransformSnapshot.ContactSkillProtectionBypass || !member.TransformSnapshot.ContactSuppression || !member.TransformSnapshot.ReceivedContactDamageHalved || !member.TransformSnapshot.ReceivedFireDamageDoubled || member.TransformSnapshot.HeldItemContactDamageToAttackerDenominator != 6 || member.TransformSnapshot.HeldItemEndTurnHealDenominator != 16 || member.TransformSnapshot.HeldItemEndTurnDamageDenominator != 8 || !member.TransformSnapshot.ContactTransferToAttacker || !member.TransformSnapshot.ChargeSkipOnce || !member.TransformSnapshot.HeldItemSurviveFatalDamageAtFullHP || member.TransformSnapshot.HeldItemReflectTurnsRemaining != 8 || member.TransformSnapshot.HeldItemLightScreenTurnsRemaining != 8 || member.TransformSnapshot.HeldItemAuroraVeilTurnsRemaining != 8 || member.TransformSnapshot.HeldItemRainTurnsRemaining != 8 || member.TransformSnapshot.HeldItemSandstormTurnsRemaining != 8 || member.TransformSnapshot.HeldItemSnowTurnsRemaining != 8 || member.TransformSnapshot.HeldItemSunTurnsRemaining != 8 || member.TransformSnapshot.HeldItemTerrainTurnsRemaining != 8 || member.CriticalHitImmunity || member.SkillRecoilDamageImmunity || member.IndirectDamageImmunity || member.IgnoreOpponentDamageStatStages || member.IgnoreTargetAbilityEffects || member.SurviveFatalDamageAtFullHP || member.OpponentStatusSkillImmunity || member.NonSuperEffectiveDamageImmunity || member.ContactSkillProtectionBypass || member.ContactSuppression || member.ReceivedContactDamageHalved || member.ReceivedFireDamageDoubled || member.HeldItemContactDamageToAttackerDenominator != 6 || member.HeldItemEndTurnHealDenominator != 16 || member.HeldItemEndTurnDamageDenominator != 8 || !member.ContactTransferToAttacker || !member.ChargeSkipOnce || !member.HeldItemSurviveFatalDamageAtFullHP || member.HeldItemReflectTurnsRemaining != 8 || member.HeldItemLightScreenTurnsRemaining != 8 || member.HeldItemAuroraVeilTurnsRemaining != 8 || member.HeldItemRainTurnsRemaining != 8 || member.HeldItemSandstormTurnsRemaining != 8 || member.HeldItemSnowTurnsRemaining != 8 || member.HeldItemSunTurnsRemaining != 8 || member.HeldItemTerrainTurnsRemaining != 8 {
		t.Fatalf("变身前画像未保存 = %+v", member.TransformSnapshot)
	}
	if !member.TransformSnapshot.HeldItemSandstormDamageImmunity || !member.HeldItemSandstormDamageImmunity || !member.TransformSnapshot.HeldItemWeightHalf || !member.HeldItemWeightHalf || !member.TransformSnapshot.HeldItemCuresParalysis || !member.HeldItemCuresParalysis || !member.TransformSnapshot.HeldItemCuresSleep || !member.HeldItemCuresSleep || !member.TransformSnapshot.HeldItemCuresPoison || !member.HeldItemCuresPoison || !member.TransformSnapshot.HeldItemCuresBurn || !member.HeldItemCuresBurn || !member.TransformSnapshot.HeldItemCuresFreeze || !member.HeldItemCuresFreeze || !member.TransformSnapshot.HeldItemCuresAllMajorStatuses || !member.HeldItemCuresAllMajorStatuses || !member.TransformSnapshot.HeldItemCuresConfusion || !member.HeldItemCuresConfusion || !member.TransformSnapshot.HeldItemPunchBasedSkillPowerBoost || !member.HeldItemPunchBasedSkillPowerBoost || !member.TransformSnapshot.HeldItemPunchBasedContactSuppression || !member.HeldItemPunchBasedContactSuppression {
		t.Fatalf("变身前道具投影未保存 = snapshot:%+v member:%+v", member.TransformSnapshot, member)
	}
	if !member.TransformSnapshot.HeldItemPhysicalDamagePowerBoost || !member.HeldItemPhysicalDamagePowerBoost ||
		!member.TransformSnapshot.HeldItemSpecialDamagePowerBoost || !member.HeldItemSpecialDamagePowerBoost {
		t.Fatalf("变身前伤害分类强化投影未保存 = snapshot:%+v member:%+v", member.TransformSnapshot, member)
	}
	if member.TransformSnapshot.HeldItemElementDamageReductionElementID != testID("receiver-resistance-element") ||
		member.HeldItemElementDamageReductionElementID != testID("receiver-resistance-element") ||
		!member.TransformSnapshot.HeldItemElementDamageReductionRequiresSuperEffective ||
		!member.HeldItemElementDamageReductionRequiresSuperEffective {
		t.Fatalf("变身前抗性道具投影未保存 = snapshot:%+v member:%+v", member.TransformSnapshot, member)
	}
	if !member.TransformSnapshot.HeldItemSuperEffectiveDamageBoost || !member.HeldItemSuperEffectiveDamageBoost ||
		!member.TransformSnapshot.HeldItemDamageBoostWithRecoil || !member.HeldItemDamageBoostWithRecoil {
		t.Fatalf("变身前条件伤害强化投影未保存 = snapshot:%+v member:%+v", member.TransformSnapshot, member)
	}
	if member.TransformSnapshot.HeldItemEndTurnHealForElementID != testID("receiver-element") || member.TransformSnapshot.HeldItemEndTurnHealForElementDenominator != 8 ||
		member.HeldItemEndTurnHealForElementID != testID("receiver-element") || member.HeldItemEndTurnHealForElementDenominator != 8 {
		t.Fatalf("变身前属性条件回复道具投影未保存 = snapshot:%+v member:%+v", member.TransformSnapshot, member)
	}
	if member.TransformSnapshot.HeldItemEndTurnDamageWithoutElementID != testID("receiver-damage-element") || member.TransformSnapshot.HeldItemEndTurnDamageWithoutElementDenominator != 16 ||
		member.HeldItemEndTurnDamageWithoutElementID != testID("receiver-damage-element") || member.HeldItemEndTurnDamageWithoutElementDenominator != 16 {
		t.Fatalf("变身前属性条件自伤道具投影未保存 = snapshot:%+v member:%+v", member.TransformSnapshot, member)
	}
	if member.TransformSnapshot.HeldItemMultiHitCountMinimum != 4 || member.TransformSnapshot.HeldItemMultiHitCountMaximum != 5 ||
		member.TransformSnapshot.HeldItemMultiHitRequiredMinimum != 2 || member.TransformSnapshot.HeldItemMultiHitRequiredMaximum != 5 ||
		member.HeldItemMultiHitCountMinimum != 4 || member.HeldItemMultiHitCountMaximum != 5 ||
		member.HeldItemMultiHitRequiredMinimum != 2 || member.HeldItemMultiHitRequiredMaximum != 5 {
		t.Fatalf("变身前连续命中道具投影未保存 = snapshot:%+v member:%+v", member.TransformSnapshot, member)
	}
	if member.OpponentSwitchRestriction == nil || member.OpponentSwitchRestriction.RequiredTargetElementID != testID("target-element") ||
		member.TransformSnapshot.OpponentSwitchRestriction == nil || member.TransformSnapshot.OpponentSwitchRestriction.RequiredTargetElementID != testID("receiver-element") {
		t.Fatalf("变身前后主动换人限制未完整保存: member=%+v, snapshot=%+v", member.OpponentSwitchRestriction, member.TransformSnapshot.OpponentSwitchRestriction)
	}
	target.OpponentSwitchRestriction.RequiredTargetElementID = testID("changed-after-state-creation")
	member, ok = state.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !ok || member.OpponentSwitchRestriction == nil || member.OpponentSwitchRestriction.RequiredTargetElementID != testID("target-element") {
		t.Fatalf("变身画像错误共享主动换人限制规则: %+v", member.OpponentSwitchRestriction)
	}
	events := state.InitialEvents()
	if len(events) != 1 {
		t.Fatalf("初始变身事件数量 = %d, events=%+v", len(events), events)
	}
	transformed, ok := events[0].(battleengine.ParticipantTransformedEvent)
	if !ok || transformed.Target != (battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) || transformed.CopiedCreatureID != target.CreatureID {
		t.Fatalf("初始变身事件 = %+v", events[0])
	}
}

// TestSwitchInTransformIntoOpponentRestoresOriginalImageOnLeave 验证实际换入变身后离场会还原原始画像。
func TestSwitchInTransformIntoOpponentRestoresOriginalImageOnLeave(t *testing.T) {
	t.Parallel()
	first := newMember(1, "transform-first", 1_000, 1_000)
	incoming := newMember(2, "transform-incoming", 1_000, 1_000)
	incoming.SwitchInTransformIntoOpponent = true
	incoming.CriticalHitImmunity = true
	incoming.SkillRecoilDamageImmunity = true
	incoming.IndirectDamageImmunity = true
	incoming.ContactDamageToAttackerDenominator = 8
	incoming.IgnoreOpponentDamageStatStages = true
	incoming.IgnoreTargetAbilityEffects = true
	incoming.SurviveFatalDamageAtFullHP = true
	incoming.OpponentStatusSkillImmunity = true
	incoming.NonSuperEffectiveDamageImmunity = true
	incoming.ContactSkillProtectionBypass = true
	incoming.ContactSuppression = true
	incoming.ReceivedContactDamageHalved = true
	incoming.ReceivedFireDamageDoubled = true
	incoming.HeldItemContactDamageToAttackerDenominator = 6
	incoming.HeldItemEndTurnHealDenominator = 16
	incoming.HeldItemEndTurnHealForElementID = testID("incoming-element")
	incoming.HeldItemEndTurnHealForElementDenominator = 8
	incoming.HeldItemEndTurnDamageDenominator = 8
	incoming.HeldItemEndTurnDamageWithoutElementID = testID("incoming-damage-element")
	incoming.HeldItemEndTurnDamageWithoutElementDenominator = 16
	incoming.ContactTransferToAttacker = true
	incoming.ChargeSkipOnce = true
	incoming.HeldItemSurviveFatalDamageAtFullHP = true
	incoming.HeldItemReflectTurnsRemaining = 8
	incoming.HeldItemLightScreenTurnsRemaining = 8
	incoming.HeldItemAuroraVeilTurnsRemaining = 8
	incoming.HeldItemRainTurnsRemaining = 8
	incoming.HeldItemSandstormTurnsRemaining = 8
	incoming.HeldItemSnowTurnsRemaining = 8
	incoming.HeldItemSunTurnsRemaining = 8
	incoming.HeldItemTerrainTurnsRemaining = 8
	incoming.HeldItemSandstormDamageImmunity = true
	incoming.HeldItemWeightHalf = true
	incoming.HeldItemCuresParalysis = true
	incoming.HeldItemCuresSleep = true
	incoming.HeldItemCuresPoison = true
	incoming.HeldItemCuresBurn = true
	incoming.HeldItemCuresFreeze = true
	incoming.HeldItemCuresAllMajorStatuses = true
	incoming.HeldItemCuresConfusion = true
	incoming.HeldItemPunchBasedSkillPowerBoost = true
	incoming.HeldItemPunchBasedContactSuppression = true
	incoming.HeldItemMultiHitCountMinimum = 4
	incoming.HeldItemMultiHitCountMaximum = 5
	incoming.HeldItemMultiHitRequiredMinimum = 2
	incoming.HeldItemMultiHitRequiredMaximum = 5
	reserve := newMember(3, "transform-reserve", 1_000, 1_000)
	target := newMember(1, "transform-target-switch", 1_000, 1_000)
	target.Stats.Defense = 404
	target.CriticalHitImmunity = false
	target.SkillRecoilDamageImmunity = false
	target.IgnoreOpponentDamageStatStages = false
	target.IgnoreTargetAbilityEffects = false
	target.SurviveFatalDamageAtFullHP = false
	target.OpponentStatusSkillImmunity = false
	target.NonSuperEffectiveDamageImmunity = false
	target.ContactSkillProtectionBypass = false
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "switch-in-transform-switch", ActiveSlotsPerSide: 1, TeamSize: 3},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{first, incoming, reserve}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}, mustRandom(t, 284))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	transformedMember, ok := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !ok || transformedMember.Stats.Defense != target.Stats.Defense || transformedMember.TransformSnapshot == nil || transformedMember.CriticalHitImmunity || transformedMember.SkillRecoilDamageImmunity || transformedMember.IndirectDamageImmunity || transformedMember.ContactDamageToAttackerDenominator != 0 || transformedMember.HeldItemContactDamageToAttackerDenominator != 6 || transformedMember.HeldItemEndTurnHealDenominator != 16 || transformedMember.HeldItemEndTurnDamageDenominator != 8 || !transformedMember.ContactTransferToAttacker || !transformedMember.ChargeSkipOnce || !transformedMember.HeldItemSurviveFatalDamageAtFullHP || transformedMember.HeldItemReflectTurnsRemaining != 8 || transformedMember.HeldItemLightScreenTurnsRemaining != 8 || transformedMember.HeldItemAuroraVeilTurnsRemaining != 8 || transformedMember.HeldItemRainTurnsRemaining != 8 || transformedMember.HeldItemSandstormTurnsRemaining != 8 || transformedMember.HeldItemSnowTurnsRemaining != 8 || transformedMember.HeldItemSunTurnsRemaining != 8 || transformedMember.HeldItemTerrainTurnsRemaining != 8 || transformedMember.IgnoreOpponentDamageStatStages || transformedMember.IgnoreTargetAbilityEffects || transformedMember.SurviveFatalDamageAtFullHP || transformedMember.OpponentStatusSkillImmunity || transformedMember.NonSuperEffectiveDamageImmunity || transformedMember.ContactSkillProtectionBypass || transformedMember.ContactSuppression || transformedMember.ReceivedContactDamageHalved || transformedMember.ReceivedFireDamageDoubled {
		t.Fatalf("换入变身后的成员 = %+v", transformedMember)
	}
	if !transformedMember.HeldItemSandstormDamageImmunity || !transformedMember.TransformSnapshot.HeldItemSandstormDamageImmunity || !transformedMember.HeldItemWeightHalf || !transformedMember.TransformSnapshot.HeldItemWeightHalf || !transformedMember.HeldItemCuresParalysis || !transformedMember.TransformSnapshot.HeldItemCuresParalysis || !transformedMember.HeldItemCuresSleep || !transformedMember.TransformSnapshot.HeldItemCuresSleep || !transformedMember.HeldItemCuresPoison || !transformedMember.TransformSnapshot.HeldItemCuresPoison || !transformedMember.HeldItemCuresBurn || !transformedMember.TransformSnapshot.HeldItemCuresBurn || !transformedMember.HeldItemCuresFreeze || !transformedMember.TransformSnapshot.HeldItemCuresFreeze || !transformedMember.HeldItemCuresAllMajorStatuses || !transformedMember.TransformSnapshot.HeldItemCuresAllMajorStatuses || !transformedMember.HeldItemCuresConfusion || !transformedMember.TransformSnapshot.HeldItemCuresConfusion || !transformedMember.HeldItemPunchBasedSkillPowerBoost || !transformedMember.TransformSnapshot.HeldItemPunchBasedSkillPowerBoost || !transformedMember.HeldItemPunchBasedContactSuppression || !transformedMember.TransformSnapshot.HeldItemPunchBasedContactSuppression {
		t.Fatalf("换入变身未保留道具投影 = %+v", transformedMember)
	}
	if transformedMember.HeldItemEndTurnHealForElementID != testID("incoming-element") || transformedMember.HeldItemEndTurnHealForElementDenominator != 8 ||
		transformedMember.TransformSnapshot.HeldItemEndTurnHealForElementID != testID("incoming-element") || transformedMember.TransformSnapshot.HeldItemEndTurnHealForElementDenominator != 8 {
		t.Fatalf("换入变身未保留属性条件回复道具投影 = %+v", transformedMember)
	}
	if transformedMember.HeldItemEndTurnDamageWithoutElementID != testID("incoming-damage-element") || transformedMember.HeldItemEndTurnDamageWithoutElementDenominator != 16 ||
		transformedMember.TransformSnapshot.HeldItemEndTurnDamageWithoutElementID != testID("incoming-damage-element") || transformedMember.TransformSnapshot.HeldItemEndTurnDamageWithoutElementDenominator != 16 {
		t.Fatalf("换入变身未保留属性条件自伤道具投影 = %+v", transformedMember)
	}
	if transformedMember.HeldItemMultiHitCountMinimum != 4 || transformedMember.HeldItemMultiHitCountMaximum != 5 ||
		transformedMember.HeldItemMultiHitRequiredMinimum != 2 || transformedMember.HeldItemMultiHitRequiredMaximum != 5 ||
		transformedMember.TransformSnapshot.HeldItemMultiHitCountMinimum != 4 || transformedMember.TransformSnapshot.HeldItemMultiHitCountMaximum != 5 ||
		transformedMember.TransformSnapshot.HeldItemMultiHitRequiredMinimum != 2 || transformedMember.TransformSnapshot.HeldItemMultiHitRequiredMaximum != 5 {
		t.Fatalf("换入变身未保留连续命中道具投影 = %+v", transformedMember)
	}
	var transformedEvents []battleengine.ParticipantTransformedEvent
	for _, event := range result.Events {
		if value, ok := event.(battleengine.ParticipantTransformedEvent); ok {
			transformedEvents = append(transformedEvents, value)
		}
	}
	if len(transformedEvents) != 1 {
		t.Fatalf("换入变身事件 = %+v", result.Events)
	}

	result, err = battleengine.ResolveTurn(result.State, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    2,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 3}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}, mustRandom(t, 284))
	if err != nil {
		t.Fatalf("ResolveTurn() restore error = %v", err)
	}
	var restored battleengine.MemberSnapshot
	for _, member := range result.State.Snapshot().Sides[0].Members {
		if member.Position == incoming.Position {
			restored = member
			break
		}
	}
	if restored.CreatureID != incoming.CreatureID || restored.Stats.Defense != incoming.Stats.Defense || restored.TransformSnapshot != nil || !restored.CriticalHitImmunity || !restored.SkillRecoilDamageImmunity || !restored.IndirectDamageImmunity || restored.ContactDamageToAttackerDenominator != 8 || restored.HeldItemContactDamageToAttackerDenominator != 6 || restored.HeldItemEndTurnHealDenominator != 16 || restored.HeldItemEndTurnDamageDenominator != 8 || !restored.ContactTransferToAttacker || !restored.ChargeSkipOnce || !restored.HeldItemSurviveFatalDamageAtFullHP || restored.HeldItemReflectTurnsRemaining != 8 || restored.HeldItemLightScreenTurnsRemaining != 8 || restored.HeldItemAuroraVeilTurnsRemaining != 8 || restored.HeldItemRainTurnsRemaining != 8 || restored.HeldItemSandstormTurnsRemaining != 8 || restored.HeldItemSnowTurnsRemaining != 8 || restored.HeldItemSunTurnsRemaining != 8 || restored.HeldItemTerrainTurnsRemaining != 8 || !restored.IgnoreOpponentDamageStatStages || !restored.IgnoreTargetAbilityEffects || !restored.SurviveFatalDamageAtFullHP || !restored.OpponentStatusSkillImmunity || !restored.NonSuperEffectiveDamageImmunity || !restored.ContactSkillProtectionBypass || !restored.ContactSuppression || !restored.ReceivedContactDamageHalved || !restored.ReceivedFireDamageDoubled {
		t.Fatalf("离场后原始画像 = %+v", restored)
	}
	if !restored.HeldItemSandstormDamageImmunity || !restored.HeldItemWeightHalf || !restored.HeldItemCuresParalysis || !restored.HeldItemCuresSleep || !restored.HeldItemCuresPoison || !restored.HeldItemCuresBurn || !restored.HeldItemCuresFreeze || !restored.HeldItemCuresAllMajorStatuses || !restored.HeldItemCuresConfusion || !restored.HeldItemPunchBasedSkillPowerBoost || !restored.HeldItemPunchBasedContactSuppression {
		t.Fatalf("离场还原未恢复道具投影 = %+v", restored)
	}
	if restored.HeldItemEndTurnHealForElementID != testID("incoming-element") || restored.HeldItemEndTurnHealForElementDenominator != 8 {
		t.Fatalf("离场还原未恢复属性条件回复道具投影 = %+v", restored)
	}
	if restored.HeldItemEndTurnDamageWithoutElementID != testID("incoming-damage-element") || restored.HeldItemEndTurnDamageWithoutElementDenominator != 16 {
		t.Fatalf("离场还原未恢复属性条件自伤道具投影 = %+v", restored)
	}
	if restored.HeldItemMultiHitCountMinimum != 4 || restored.HeldItemMultiHitCountMaximum != 5 ||
		restored.HeldItemMultiHitRequiredMinimum != 2 || restored.HeldItemMultiHitRequiredMaximum != 5 {
		t.Fatalf("离场后的连续命中道具投影未还原 = %+v", restored)
	}
}
