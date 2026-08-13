package battle_test

import (
	"context"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/itemrules"
)

// TestGameDataFactsReaderFreezesElementDamageReduction 验证抗性属性及克制条件在 Battle 启动时被冻结。
func TestGameDataFactsReaderFreezesElementDamageReduction(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	itemID := snowflake.NewTestID()
	fixture.session.Participants[0].Team.Members[0].ItemID = &itemID
	fixture.itemRules = itemrules.Projection{Details: []itemrules.Detail{{
		ID: snowflake.NewTestID(), ItemID: itemID, ElementDamageReductionElementID: &fixture.elementID,
		ElementDamageReductionRequiresSuperEffective: true,
	}}}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if member.HeldItemElementDamageReductionElementID != fixture.elementID || !member.HeldItemElementDamageReductionRequiresSuperEffective {
		t.Fatalf("冻结的抗性规则 = %+v", member)
	}
	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	result := initial.Sides[0].Members[0]
	if result.HeldItemElementDamageReductionElementID != fixture.elementID || !result.HeldItemElementDamageReductionRequiresSuperEffective {
		t.Fatalf("引擎成员中的抗性规则 = %+v", result)
	}
}
