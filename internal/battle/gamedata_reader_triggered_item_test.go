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

// TestGameDataFactsReaderFreezesTriggeredItemRules 验证 221–229 的资料事实完整冻结到 Battle Engine 成员快照。
func TestGameDataFactsReaderFreezesTriggeredItemRules(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID, waterID, electricID, iceID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	fixture.session.Participants[0].Team.Members[0].ItemID = &itemID
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, AccuracyMissStatStageBoostStat: battleengine.StatSpeed, AccuracyMissStatStageBoostDelta: 2, WeaknessPolicy: true,
		WaterDamageSpecialAttackBoostElementID: &waterID, ElectricDamageAttackBoostElementID: &electricID,
		WaterDamageSpecialDefenseBoostElementID: &waterID, IceDamageAttackBoostElementID: &iceID,
		AdditionalFlinchChancePercent: 10, RandomActionOrderBoostChancePercent: 20, ForcedLastActionOrder: true,
		LowHPActionOrderBoost: true, FieldSpeedOrderSpeedStageDrop: true, ConsecutiveSkillDamageBoost: true,
	}}}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error=%v", err)
	}
	member := facts.Sides[0].Members[0]
	if member.HeldItemAccuracyMissStatStageBoostStat != battleengine.StatSpeed || member.HeldItemAccuracyMissStatStageBoostDelta != 2 || !member.HeldItemWeaknessPolicy || member.HeldItemWaterDamageSpecialAttackBoostElementID != waterID ||
		member.HeldItemElectricDamageAttackBoostElementID != electricID || member.HeldItemAdditionalFlinchChancePercent != 10 ||
		member.HeldItemWaterDamageSpecialDefenseBoostElementID != waterID || member.HeldItemIceDamageAttackBoostElementID != iceID ||
		member.HeldItemRandomActionOrderBoostChancePercent != 20 || !member.HeldItemForcedLastActionOrder || !member.HeldItemLowHPActionOrderBoost ||
		!member.HeldItemFieldSpeedOrderSpeedStageDrop || !member.HeldItemConsecutiveSkillDamageBoost {
		t.Fatalf("Battle 触发型道具事实 = %+v", member)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error=%v", err)
	}
	engineMember := initial.Sides[0].Members[0]
	if engineMember.HeldItemAccuracyMissStatStageBoostStat != battleengine.StatSpeed || engineMember.HeldItemAccuracyMissStatStageBoostDelta != 2 || !engineMember.HeldItemWeaknessPolicy || engineMember.HeldItemWaterDamageSpecialDefenseBoostElementID != waterID || engineMember.HeldItemIceDamageAttackBoostElementID != iceID || !engineMember.HeldItemLowHPActionOrderBoost || !engineMember.HeldItemConsecutiveSkillDamageBoost {
		t.Fatalf("Battle Engine 触发型道具快照 = %+v", engineMember)
	}
}
