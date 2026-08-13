package battle_test

import (
	"context"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/itemrules"
)

// TestGameDataFactsReaderFreezesConsumableElementDamageBoost 验证一次性属性威力强化在 Battle 启动时被冻结。
//
// 对局中不得回查可变资料：属性、分子和分母都必须进入引擎初始状态，以保证资料后续变更不会改变已开始对局的
// 道具消费边界和伤害倍率。
func TestGameDataFactsReaderFreezesConsumableElementDamageBoost(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	fixture.session.Participants[0].Team.Members[0].ItemID = &itemID
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, ConsumableElementDamageBoostElementID: &fixture.elementID,
		ConsumableElementDamageBoostNumerator: 6, ConsumableElementDamageBoostDenominator: 5,
	}}}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if member.HeldItemConsumableElementDamageBoostElementID != fixture.elementID ||
		member.HeldItemConsumableElementDamageBoostNumerator != 6 || member.HeldItemConsumableElementDamageBoostDenominator != 5 {
		t.Fatalf("冻结的一次性属性威力强化规则 = %+v", member)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	finalMember := initial.Sides[0].Members[0]
	if finalMember.HeldItemConsumableElementDamageBoostElementID != fixture.elementID ||
		finalMember.HeldItemConsumableElementDamageBoostNumerator != 6 || finalMember.HeldItemConsumableElementDamageBoostDenominator != 5 {
		t.Fatalf("引擎成员中的一次性属性威力强化规则 = %+v", finalMember)
	}
}
