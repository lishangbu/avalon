package battle_test

import (
	"context"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
)

// TestGameDataFactsReaderFreezesReactiveAbilityRules 验证 Current Game Data 到 Battle facts 再到 Battle Engine
// 初始快照的两次深复制；资料对象和 facts 在冻结后被修改都不能污染已开始对局。
func TestGameDataFactsReaderFreezesReactiveAbilityRules(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	elementID := snowflake.NewTestID()
	source := &battleengine.ReactiveAbilityRules{
		EndTurnStatStageChanges: []battleengine.StatStageDelta{{Stat: battleengine.StatSpeed, Delta: 1}},
		FaintStatStageBoosts:    []battleengine.FaintStatStageBoost{{Stat: battleengine.StatAttack, Delta: 1, RequiresCausedFaint: true}},
		ReceivedDamageCharge:    &battleengine.ReceivedDamageCharge{ElementID: elementID, Numerator: 2, Denominator: 1},
	}
	fixture.abilityDetail = &abilitydetail.RuleSet{ID: snowflake.NewTestID(), AbilityID: fixture.ability.ID, OptionalValues: abilitydetail.OptionalValues{ReactiveAbilityRules: source}, Version: 1}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	frozenFacts := facts.Sides[0].Members[0].ReactiveAbilityRules
	source.EndTurnStatStageChanges[0].Delta = 6
	if frozenFacts == nil || frozenFacts.EndTurnStatStageChanges[0].Delta != 1 || frozenFacts.ReceivedDamageCharge.ElementID != elementID {
		t.Fatalf("facts = %+v", frozenFacts)
	}

	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	frozenFacts.FaintStatStageBoosts[0].Delta = 6
	frozen := initial.Sides[0].Members[0].ReactiveAbilityRules
	if frozen == nil || frozen.FaintStatStageBoosts[0].Delta != 1 || frozen.ReceivedDamageCharge.Numerator != 2 {
		t.Fatalf("initial reactive rules = %+v", frozen)
	}
}
