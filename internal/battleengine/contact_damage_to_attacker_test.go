package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnContactDamageToAttacker 验证接触反制只由目标本体实际受到的有效接触伤害触发。
//
// 这些分支将接触标签、替身、攻击方免疫、特性穿透、接触抑制、多段和倒下分别固定，确保道具免疫不会被
// 错误实现为取消接触事实，也确保“攻击者最大生命 / 分母”的反制基数不会退化为本次命中伤害。
func TestResolveTurnContactDamageToAttacker(t *testing.T) {
	t.Parallel()
	tests := []contactDamageToAttackerTurnCase{
		{name: "有效接触按攻击者最大生命反制", makesContact: true, targetHP: 500, hits: 1, attackerHP: 160, wantContactEvents: 1, wantAttackerHP: 140},
		{name: "持有道具按独立分母反制", makesContact: true, heldItemDenominator: 6, targetHP: 500, hits: 1, attackerHP: 160, wantContactEvents: 1, wantAttackerHP: 134},
		{name: "无视目标特性不跳过目标道具", makesContact: true, attackerIgnoresTarget: true, heldItemDenominator: 6, targetHP: 500, hits: 1, attackerHP: 160, wantContactEvents: 1, wantAttackerHP: 134},
		{name: "非接触不触发", targetHP: 500, hits: 1, attackerHP: 160, wantContactEvents: 0, wantAttackerHP: 160},
		{name: "替身承伤不触发", makesContact: true, targetSubstitute: 500, targetHP: 500, hits: 1, attackerHP: 160, wantContactEvents: 0, wantAttackerHP: 160},
		{name: "接触副作用免疫不触发", makesContact: true, attackerItemImmune: true, targetHP: 500, hits: 1, attackerHP: 160, wantContactEvents: 0, wantAttackerHP: 160},
		{name: "拳击道具仅抑制拳击技能的有效接触", makesContact: true, attackerPunchBased: true, attackerPunchBasedContactSuppression: true, targetHP: 500, hits: 1, attackerHP: 160, wantContactEvents: 0, wantAttackerHP: 160},
		{name: "拳击道具不抑制非拳击接触技能", makesContact: true, attackerPunchBasedContactSuppression: true, targetHP: 500, hits: 1, attackerHP: 160, wantContactEvents: 1, wantAttackerHP: 140},
		{name: "间接伤害免疫不触发", makesContact: true, attackerIndirectImmune: true, targetHP: 500, hits: 1, attackerHP: 160, wantContactEvents: 0, wantAttackerHP: 160},
		{name: "无视目标特性不触发", makesContact: true, attackerIgnoresTarget: true, targetHP: 500, hits: 1, attackerHP: 160, wantContactEvents: 0, wantAttackerHP: 160},
		{name: "接触抑制不触发", makesContact: true, attackerSuppresses: true, targetHP: 500, hits: 1, attackerHP: 160, wantContactEvents: 0, wantAttackerHP: 160},
		{name: "多段逐段反制", makesContact: true, targetHP: 500, hits: 2, attackerHP: 160, wantContactEvents: 2, wantAttackerHP: 120},
		{name: "反制可以击倒攻击者", makesContact: true, targetHP: 500, hits: 1, attackerHP: 20, wantContactEvents: 1, wantAttackerHP: 0, wantFainted: true},
		{name: "受击方倒下仍完成当段反制", makesContact: true, targetHP: 1, hits: 1, attackerHP: 160, wantContactEvents: 1, wantAttackerHP: 140},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			result := resolveContactDamageToAttackerTurn(t, test)
			if len(result.contactEvents) != test.wantContactEvents {
				t.Fatalf("接触反制事件数 = %d，期望 %d，事件 = %+v", len(result.contactEvents), test.wantContactEvents, result.events)
			}
			if result.attacker.CurrentHP != test.wantAttackerHP {
				t.Fatalf("攻击者当前生命 = %d，期望 %d", result.attacker.CurrentHP, test.wantAttackerHP)
			}
			if test.wantContactEvents > 0 {
				for _, event := range result.contactEvents {
					expectedDenominator := uint16(8)
					expectedSourceAbilityID, expectedSourceItemID := testID("contact-retaliation-ability"), battleengine.Identifier(0)
					if test.heldItemDenominator != 0 {
						expectedDenominator = test.heldItemDenominator
						expectedSourceAbilityID, expectedSourceItemID = 0, testID("contact-retaliation-item")
					}
					expectedAmount := uint32(160) / uint32(expectedDenominator)
					if expectedAmount == 0 {
						expectedAmount = 1
					}
					if expectedAmount > test.attackerHP {
						expectedAmount = test.attackerHP
					}
					if event.Denominator != expectedDenominator || event.Amount != expectedAmount || event.SourceAbilityID != expectedSourceAbilityID || event.SourceItemID != expectedSourceItemID {
						t.Fatalf("接触反制事件 = %+v，期望分母=%d，特性来源=%q，道具来源=%q，伤害=%d", event, expectedDenominator, expectedSourceAbilityID, expectedSourceItemID, expectedAmount)
					}
				}
			}
			if contactDamageFaintExists(result.events) != test.wantFainted {
				t.Fatalf("接触反制倒下事件存在性 = %t，期望 %t，事件 = %+v", contactDamageFaintExists(result.events), test.wantFainted, result.events)
			}
		})
	}
}

// TestResolveTurnContactItemTransferToAttacker 验证接触转移只移动目标当前仍持有的道具运行时投影。
//
// 转移必须发生在目标道具反伤读取之前：目标交出道具后不应继续以已移走道具反伤攻击者；目标独立特性反伤仍然
// 可以结算。攻击方已经有道具、没有有效接触或具备接触副作用免疫时都不能发生所有权变化。
func TestResolveTurnContactItemTransferToAttacker(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name               string
		makesContact       bool
		attackerHasItem    bool
		attackerItemImmune bool
		wantTransfer       bool
	}{
		{name: "有效接触转移", makesContact: true, wantTransfer: true},
		{name: "非接触不转移", wantTransfer: false},
		{name: "攻击者已有道具不转移", makesContact: true, attackerHasItem: true, wantTransfer: false},
		{name: "接触副作用免疫不转移", makesContact: true, attackerItemImmune: true, wantTransfer: false},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			result := resolveContactDamageToAttackerTurn(t, contactDamageToAttackerTurnCase{
				name: test.name, makesContact: test.makesContact, attackerHasItem: test.attackerHasItem,
				attackerItemImmune: test.attackerItemImmune, contactTransferToAttacker: true,
				targetHP: 500, hits: 1, attackerHP: 160,
			})
			if (len(result.transferEvents) == 1) != test.wantTransfer {
				t.Fatalf("道具转移事件 = %+v，期望发生=%t，全部事件=%+v", result.transferEvents, test.wantTransfer, result.events)
			}
			if !test.wantTransfer {
				return
			}
			transfer := result.transferEvents[0]
			if transfer.ItemID != testID("contact-transfer-item") || transfer.From.Side != battleengine.SideTwo || transfer.To.Side != battleengine.SideOne ||
				result.attacker.ItemID != testID("contact-transfer-item") || !result.attacker.ChargeSkipOnce || !result.attacker.HeldItemSurviveFatalDamageAtFullHP || result.attacker.HeldItemEndTurnHealDenominator != 16 || result.attacker.HeldItemEndTurnHealForElementID != testID("defender-element") || result.attacker.HeldItemEndTurnHealForElementDenominator != 8 || result.attacker.HeldItemEndTurnDamageDenominator != 8 || result.attacker.HeldItemEndTurnDamageWithoutElementID != testID("grass-element") || result.attacker.HeldItemEndTurnDamageWithoutElementDenominator != 16 || result.attacker.HeldItemReflectTurnsRemaining != 8 || result.attacker.HeldItemLightScreenTurnsRemaining != 8 || result.attacker.HeldItemAuroraVeilTurnsRemaining != 8 || result.attacker.HeldItemRainTurnsRemaining != 8 || result.attacker.HeldItemSandstormTurnsRemaining != 8 || result.attacker.HeldItemSnowTurnsRemaining != 8 || result.attacker.HeldItemSunTurnsRemaining != 8 || result.attacker.HeldItemTerrainTurnsRemaining != 8 || !result.attacker.HeldItemSandstormDamageImmunity || !result.attacker.HeldItemWeightHalf || !result.attacker.HeldItemCuresParalysis || !result.attacker.HeldItemCuresSleep || !result.attacker.HeldItemCuresPoison || !result.attacker.HeldItemCuresBurn || !result.attacker.HeldItemCuresFreeze || !result.attacker.HeldItemCuresAllMajorStatuses || !result.attacker.HeldItemCuresConfusion || !result.attacker.HeldItemPunchBasedSkillPowerBoost || !result.attacker.HeldItemPunchBasedContactSuppression || result.target.ItemID != 0 || result.target.ContactTransferToAttacker || result.target.ChargeSkipOnce || result.target.HeldItemSurviveFatalDamageAtFullHP || result.target.HeldItemEndTurnHealDenominator != 0 || result.target.HeldItemEndTurnHealForElementID != 0 || result.target.HeldItemEndTurnHealForElementDenominator != 0 || result.target.HeldItemEndTurnDamageDenominator != 0 || result.target.HeldItemEndTurnDamageWithoutElementID != 0 || result.target.HeldItemEndTurnDamageWithoutElementDenominator != 0 || result.target.HeldItemReflectTurnsRemaining != 0 || result.target.HeldItemLightScreenTurnsRemaining != 0 || result.target.HeldItemAuroraVeilTurnsRemaining != 0 || result.target.HeldItemRainTurnsRemaining != 0 || result.target.HeldItemSandstormTurnsRemaining != 0 || result.target.HeldItemSnowTurnsRemaining != 0 || result.target.HeldItemSunTurnsRemaining != 0 || result.target.HeldItemTerrainTurnsRemaining != 0 || result.target.HeldItemSandstormDamageImmunity || result.target.HeldItemWeightHalf || result.target.HeldItemCuresParalysis || result.target.HeldItemCuresSleep || result.target.HeldItemCuresPoison || result.target.HeldItemCuresBurn || result.target.HeldItemCuresFreeze || result.target.HeldItemCuresAllMajorStatuses || result.target.HeldItemCuresConfusion || result.target.HeldItemPunchBasedSkillPowerBoost || result.target.HeldItemPunchBasedContactSuppression {
				t.Fatalf("接触道具转移结果不一致: event=%+v attacker=%+v target=%+v", transfer, result.attacker, result.target)
			}
			if len(result.contactEvents) != 1 || result.contactEvents[0].SourceAbilityID != testID("contact-retaliation-ability") || result.contactEvents[0].SourceItemID != 0 {
				t.Fatalf("道具转移后仍错误读取目标已移走道具的反伤: contacts=%+v", result.contactEvents)
			}
			if result.attacker.HeldItemMultiHitCountMinimum != 4 || result.attacker.HeldItemMultiHitCountMaximum != 5 ||
				result.attacker.HeldItemMultiHitRequiredMinimum != 2 || result.attacker.HeldItemMultiHitRequiredMaximum != 5 ||
				result.target.HeldItemMultiHitCountMinimum != 0 || result.target.HeldItemMultiHitCountMaximum != 0 ||
				result.target.HeldItemMultiHitRequiredMinimum != 0 || result.target.HeldItemMultiHitRequiredMaximum != 0 {
				t.Fatalf("连续命中道具投影未随转移原子迁移: attacker=%+v target=%+v", result.attacker, result.target)
			}
			if !result.attacker.HeldItemPhysicalDamagePowerBoost || !result.attacker.HeldItemSpecialDamagePowerBoost ||
				result.target.HeldItemPhysicalDamagePowerBoost || result.target.HeldItemSpecialDamagePowerBoost {
				t.Fatalf("伤害分类强化投影未随道具原子迁移: attacker=%+v target=%+v", result.attacker, result.target)
			}
			if result.attacker.HeldItemElementDamageReductionElementID != testID("resistance-element") ||
				!result.attacker.HeldItemElementDamageReductionRequiresSuperEffective ||
				result.target.HeldItemElementDamageReductionElementID != 0 || result.target.HeldItemElementDamageReductionRequiresSuperEffective {
				t.Fatalf("抗性道具投影未随所有权原子迁移: attacker=%+v target=%+v", result.attacker, result.target)
			}
			if !result.attacker.HeldItemSuperEffectiveDamageBoost || !result.attacker.HeldItemDamageBoostWithRecoil ||
				result.target.HeldItemSuperEffectiveDamageBoost || result.target.HeldItemDamageBoostWithRecoil {
				t.Fatalf("条件伤害强化投影未随所有权原子迁移: attacker=%+v target=%+v", result.attacker, result.target)
			}
		})
	}
}

// contactDamageToAttackerTurnCase 保存接触反制测试各分支的最小资料化输入。
type contactDamageToAttackerTurnCase struct {
	name                                 string
	makesContact                         bool
	attackerItemImmune                   bool
	attackerHasItem                      bool
	attackerIndirectImmune               bool
	attackerIgnoresTarget                bool
	attackerSuppresses                   bool
	attackerPunchBased                   bool
	attackerPunchBasedContactSuppression bool
	heldItemDenominator                  uint16
	contactTransferToAttacker            bool
	targetSubstitute                     uint32
	targetHP                             uint32
	hits                                 uint8
	attackerHP                           uint32
	wantContactEvents                    int
	wantAttackerHP                       uint32
	wantFainted                          bool
}

// contactDamageToAttackerTurnResult 汇集接触反制断言所需的权威快照和结构化事件。
type contactDamageToAttackerTurnResult struct {
	// attacker 是完整回合结算后的攻击方成员快照。
	attacker battleengine.MemberSnapshot
	// target 是完整回合结算后的受击方成员快照，用于断言道具所有权变化。
	target battleengine.MemberSnapshot
	// contactEvents 只包含目标特性造成的接触反制事件。
	contactEvents []battleengine.ContactDamageAppliedEvent
	// transferEvents 只包含本次有效接触造成的道具转移事件。
	transferEvents []battleengine.HeldItemTransferredEvent
	// events 是用于断言倒下原因和事件顺序的完整回合事件流。
	events []battleengine.Event
}

// resolveContactDamageToAttackerTurn 建立固定单打场景，并以可复现随机源结算攻击方的一次接触技能。
func resolveContactDamageToAttackerTurn(
	t *testing.T,
	test contactDamageToAttackerTurnCase,
) contactDamageToAttackerTurnResult {
	t.Helper()
	attacker := newMember(1, "contact-retaliation-attacker", 160, test.attackerHP)
	attacker.Stats.Speed = 200
	attacker.ContactSideEffectImmunity = test.attackerItemImmune
	attacker.IndirectDamageImmunity = test.attackerIndirectImmune
	attacker.IgnoreTargetAbilityEffects = test.attackerIgnoresTarget
	attacker.ContactSuppression = test.attackerSuppresses
	attacker.HeldItemPunchBasedContactSuppression = test.attackerPunchBasedContactSuppression
	if test.attackerHasItem {
		attacker.ItemID = testID("contact-retaliation-attacker-item")
	}
	if test.attackerPunchBasedContactSuppression {
		attacker.ItemID = testID("punch-contact-suppression-item")
	}
	attacker.Skills[0].SkillID = testID("contact-retaliation-strike")
	attacker.Skills[0].MakesContact = test.makesContact
	attacker.Skills[0].PunchBased = test.attackerPunchBased
	attacker.Skills[0].MinHits = test.hits
	attacker.Skills[0].MaxHits = test.hits

	target := newMember(1, "contact-retaliation-target", 500, test.targetHP)
	target.Stats.Speed = 10
	target.AbilityID = testID("contact-retaliation-ability")
	if test.heldItemDenominator == 0 {
		target.ContactDamageToAttackerDenominator = 8
	} else {
		target.ItemID = testID("contact-retaliation-item")
		target.HeldItemContactDamageToAttackerDenominator = test.heldItemDenominator
	}
	if test.contactTransferToAttacker {
		target.ItemID = testID("contact-transfer-item")
		target.ContactTransferToAttacker = true
		target.ChargeSkipOnce = true
		target.HeldItemSurviveFatalDamageAtFullHP = true
		target.HeldItemEndTurnHealDenominator = 16
		target.HeldItemEndTurnHealForElementID = testID("defender-element")
		target.HeldItemEndTurnHealForElementDenominator = 8
		target.HeldItemEndTurnDamageDenominator = 8
		target.HeldItemEndTurnDamageWithoutElementID = testID("grass-element")
		target.HeldItemEndTurnDamageWithoutElementDenominator = 16
		target.HeldItemReflectTurnsRemaining = 8
		target.HeldItemLightScreenTurnsRemaining = 8
		target.HeldItemAuroraVeilTurnsRemaining = 8
		target.HeldItemRainTurnsRemaining = 8
		target.HeldItemSandstormTurnsRemaining = 8
		target.HeldItemSnowTurnsRemaining = 8
		target.HeldItemSunTurnsRemaining = 8
		target.HeldItemTerrainTurnsRemaining = 8
		target.HeldItemSandstormDamageImmunity = true
		target.HeldItemWeightHalf = true
		target.HeldItemCuresParalysis = true
		target.HeldItemCuresSleep = true
		target.HeldItemCuresPoison = true
		target.HeldItemCuresBurn = true
		target.HeldItemCuresFreeze = true
		target.HeldItemCuresAllMajorStatuses = true
		target.HeldItemCuresConfusion = true
		target.HeldItemPunchBasedSkillPowerBoost = true
		target.HeldItemPhysicalDamagePowerBoost = true
		target.HeldItemSpecialDamagePowerBoost = true
		target.HeldItemElementDamageReductionElementID = testID("resistance-element")
		target.HeldItemElementDamageReductionRequiresSuperEffective = true
		target.HeldItemSuperEffectiveDamageBoost = true
		target.HeldItemDamageBoostWithRecoil = true
		target.HeldItemPunchBasedContactSuppression = true
		target.HeldItemMultiHitCountMinimum = 4
		target.HeldItemMultiHitCountMaximum = 5
		target.HeldItemMultiHitRequiredMinimum = 2
		target.HeldItemMultiHitRequiredMaximum = 5
	}
	target.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("contact-retaliation-wait"), Name: "等待", ElementID: target.ElementIDs[0],
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, HealingPercent: 1,
	}
	if test.targetSubstitute != 0 {
		// 初始状态禁止直接携带替身，因此让更快的目标在同一回合先合法建立替身，再承受攻击方的技能。
		target.Stats.Speed = 300
		target.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("contact-retaliation-substitute"), Name: "替身", ElementID: target.ElementIDs[0],
			DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
			Accuracy: 100, RemainingPP: 10, MaxPP: 10,
			VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
				Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser,
				ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4,
			}},
		}
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "contact-damage-to-attacker", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{attacker}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	resolved, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1, TurnNumber: 1,
		Actions: []battleengine.Action{
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}}},
			{Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
				UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}}},
		},
	}, mustRandom(t, 2_185))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	attacker, found := resolved.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if !found {
		t.Fatal("结算后未找到攻击者")
	}
	target, found = resolved.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found {
		t.Fatal("结算后未找到受击方")
	}
	result := contactDamageToAttackerTurnResult{attacker: attacker, target: target, events: resolved.Events}
	for _, event := range resolved.Events {
		if contactDamage, ok := event.(battleengine.ContactDamageAppliedEvent); ok {
			result.contactEvents = append(result.contactEvents, contactDamage)
		}
		if transfer, ok := event.(battleengine.HeldItemTransferredEvent); ok {
			result.transferEvents = append(result.transferEvents, transfer)
		}
	}
	return result
}

// contactDamageFaintExists 报告攻击者是否因接触反制伤害而倒下。
func contactDamageFaintExists(events []battleengine.Event) bool {
	for _, event := range events {
		if fainted, ok := event.(battleengine.ParticipantFaintedEvent); ok && fainted.Cause == battleengine.FaintCauseContactDamage {
			return true
		}
	}
	return false
}
