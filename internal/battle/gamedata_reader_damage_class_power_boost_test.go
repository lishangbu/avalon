package battle_test

import (
	"context"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/itemrules"
)

// TestGameDataFactsReaderFreezesDamageClassPowerBoosts 验证两类稳定威力强化在 Battle 启动时进入纯引擎快照。
//
// 对局开始后即使 Item Metadata 被维护者修改，运行时也只能读取这里冻结的两个开关；不得按道具名称或效果文本
// 回查实时资料并重新解释规则。
func TestGameDataFactsReaderFreezesDamageClassPowerBoosts(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	fixture.session.Participants[0].Team.Members[0].ItemID = &itemID
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, PhysicalDamagePowerBoost: true, SpecialDamagePowerBoost: true,
		SuperEffectiveDamageBoost: true, DamageBoostWithRecoil: true,
		DamageDealtHeal: true, DrainHealingBoost: true, AccuracyBoost: true, OpponentAccuracyReduction: true,
		CriticalHitStageBoost: true, AirborneUntilDamaged: true, ForceGrounded: true, SpeedHalf: true,
		SpecialDefenseBoost: true, StatusSkillRestriction: true,
		PhysicalDamagePowerBoost50: true, SpecialDamagePowerBoost50: true, ChoiceSkillLock: true, SpeedBoost50: true,
		AccuracyAfterTargetActedBoost: true, TypeImmunitySuppression: true, OpponentStatStageReductionImmunity: true,
		NegativeStatStageReset: true, AbilityStatReductionSpeedBoost: true, OpponentPositiveStatStageCopy: true,
	}}}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if !member.HeldItemPhysicalDamagePowerBoost || !member.HeldItemSpecialDamagePowerBoost ||
		!member.HeldItemSuperEffectiveDamageBoost || !member.HeldItemDamageBoostWithRecoil {
		t.Fatalf("冻结的伤害分类威力强化规则 = %+v", member)
	}
	if !member.HeldItemDamageDealtHeal || !member.HeldItemDrainHealingBoost || !member.HeldItemAccuracyBoost ||
		!member.HeldItemOpponentAccuracyReduction || !member.HeldItemCriticalHitStageBoost || !member.HeldItemAirborneUntilDamaged ||
		!member.HeldItemForceGrounded || !member.HeldItemSpeedHalf || !member.HeldItemSpecialDefenseBoost || !member.HeldItemStatusSkillRestriction {
		t.Fatalf("冻结的通用道具规则 = %+v", member)
	}
	if !battleMemberAdvancedUtilityFacts(member) {
		t.Fatalf("冻结的高级道具规则 = %+v", member)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	finalMember := initial.Sides[0].Members[0]
	if !finalMember.HeldItemPhysicalDamagePowerBoost || !finalMember.HeldItemSpecialDamagePowerBoost ||
		!finalMember.HeldItemSuperEffectiveDamageBoost || !finalMember.HeldItemDamageBoostWithRecoil {
		t.Fatalf("引擎成员中的伤害分类威力强化规则 = %+v", finalMember)
	}
	if !finalMember.HeldItemDamageDealtHeal || !finalMember.HeldItemDrainHealingBoost || !finalMember.HeldItemAccuracyBoost ||
		!finalMember.HeldItemOpponentAccuracyReduction || !finalMember.HeldItemCriticalHitStageBoost || !finalMember.HeldItemAirborneUntilDamaged ||
		!finalMember.HeldItemForceGrounded || !finalMember.HeldItemSpeedHalf || !finalMember.HeldItemSpecialDefenseBoost || !finalMember.HeldItemStatusSkillRestriction {
		t.Fatalf("引擎成员中的通用道具规则 = %+v", finalMember)
	}
	if !engineMemberAdvancedUtilityFacts(finalMember) {
		t.Fatalf("引擎成员中的高级道具规则 = %+v", finalMember)
	}
}

// battleMemberAdvancedUtilityFacts 判断 Battle 事实中的 201–210 十项规则是否全部存在。
func battleMemberAdvancedUtilityFacts(member battle.BattleMemberFacts) bool {
	return member.HeldItemPhysicalDamagePowerBoost50 && member.HeldItemSpecialDamagePowerBoost50 && member.HeldItemChoiceSkillLock && member.HeldItemSpeedBoost50 &&
		member.HeldItemAccuracyAfterTargetActedBoost && member.HeldItemTypeImmunitySuppression && member.HeldItemOpponentStatStageReductionImmunity &&
		member.HeldItemNegativeStatStageReset && member.HeldItemAbilityStatReductionSpeedBoost && member.HeldItemOpponentPositiveStatStageCopy
}

// engineMemberAdvancedUtilityFacts 判断纯引擎快照中的 201–210 十项规则是否全部存在。
func engineMemberAdvancedUtilityFacts(member battleengine.MemberSnapshot) bool {
	return member.HeldItemPhysicalDamagePowerBoost50 && member.HeldItemSpecialDamagePowerBoost50 && member.HeldItemChoiceSkillLock && member.HeldItemSpeedBoost50 &&
		member.HeldItemAccuracyAfterTargetActedBoost && member.HeldItemTypeImmunitySuppression && member.HeldItemOpponentStatStageReductionImmunity &&
		member.HeldItemNegativeStatStageReset && member.HeldItemAbilityStatReductionSpeedBoost && member.HeldItemOpponentPositiveStatStageCopy
}
