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

// TestGameDataFactsReaderFreezesSkillUseAbilityRules 验证三类技能使用规则与受伤反制规则
// 能从 Current Game Data 穿过 Battle facts，并以完全隔离的快照进入 Battle Engine。
func TestGameDataFactsReaderFreezesSkillUseAbilityRules(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	normalID, dragonID := snowflake.NewTestID(), snowflake.NewTestID()
	detail := &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.ability.ID, Version: 1,
		OptionalValues: abilitydetail.OptionalValues{
			ContactSkillProtectionBypass:                 true,
			ContactSkillProtectionBypassDamageMultiplier: &battleengine.DamageFraction{Numerator: 1, Denominator: 4},
			SkillWeatherOverride:                         battleengine.WeatherKindSun,
			SkillElementConversion: &battleengine.SkillElementConversion{
				SourceElementID: normalID, TargetElementID: dragonID, DamageNumerator: 6, DamageDenominator: 5,
			},
			ReactiveAbilityRules: &battleengine.ReactiveAbilityRules{
				ReceivedDamageAttackerMajorStatus: battleengine.MajorStatusBurn,
			},
		},
	}
	fixture.abilityDetail = detail

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	frozenFacts := facts.Sides[0].Members[0]
	detail.ContactSkillProtectionBypassDamageMultiplier.Denominator = 2
	detail.SkillElementConversion.TargetElementID = normalID
	detail.ReactiveAbilityRules.ReceivedDamageAttackerMajorStatus = battleengine.MajorStatusPoison
	if frozenFacts.ContactSkillProtectionBypassDamageMultiplier == nil || frozenFacts.ContactSkillProtectionBypassDamageMultiplier.Denominator != 4 ||
		frozenFacts.SkillElementConversion == nil || frozenFacts.SkillElementConversion.TargetElementID != dragonID ||
		frozenFacts.ReactiveAbilityRules == nil || frozenFacts.ReactiveAbilityRules.ReceivedDamageAttackerMajorStatus != battleengine.MajorStatusBurn {
		t.Fatalf("Battle facts 未隔离技能使用型特性规则: %+v", frozenFacts)
	}

	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	frozenFacts.ContactSkillProtectionBypassDamageMultiplier.Numerator = 3
	frozenFacts.SkillElementConversion.DamageNumerator = 9
	member := initial.Sides[0].Members[0]
	if !member.ContactSkillProtectionBypass || member.ContactSkillProtectionBypassDamageMultiplier == nil ||
		member.ContactSkillProtectionBypassDamageMultiplier.Numerator != 1 || member.SkillWeatherOverride != battleengine.WeatherKindSun ||
		member.SkillElementConversion == nil || member.SkillElementConversion.DamageNumerator != 6 ||
		member.ReactiveAbilityRules == nil || member.ReactiveAbilityRules.ReceivedDamageAttackerMajorStatus != battleengine.MajorStatusBurn {
		t.Fatalf("Battle Engine 初始快照未完整冻结技能使用型特性规则: %+v", member)
	}
}
