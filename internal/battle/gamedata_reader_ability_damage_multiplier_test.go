package battle_test

import (
	"context"
	"errors"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battle"
	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
)

// TestGameDataFactsReaderRejectsDamagedAbilityDamageMultipliers 验证绕过管理服务写入的越界分数、重复属性和未知天气
// 会在创建 Battle 的资料编译边界被拒绝，不能被截断、去重或静默解释为没有规则。
func TestGameDataFactsReaderRejectsDamagedAbilityDamageMultipliers(t *testing.T) {
	t.Parallel()
	elementID := snowflake.NewTestID()
	tests := []struct {
		name   string
		values abilitydetail.OptionalValues
	}{
		{
			name: "基础威力上限无法无损冻结",
			values: abilitydetail.OptionalValues{
				BasePowerAtMostDamageBoost: &abilitydetail.BasePowerAtMostDamageBoost{MaximumPower: 65_536, Numerator: 3, Denominator: 2},
			},
		},
		{
			name: "属性集合包含重复身份",
			values: abilitydetail.OptionalValues{
				ElementSkillDamageBoost: &abilitydetail.ElementSkillDamageBoost{ElementIDs: []snowflake.ID{elementID, elementID}, Numerator: 3, Denominator: 2},
			},
		},
		{
			name: "天气不属于封闭集合",
			values: abilitydetail.OptionalValues{
				WeatherElementDamageBoost: &abilitydetail.WeatherElementDamageBoost{Weather: abilitydetail.WeatherKind("unknown"), ElementIDs: []snowflake.ID{elementID}, Numerator: 13, Denominator: 10},
			},
		},
	}
	for _, test := range tests {
		test := test
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
			fixture.abilityDetail = &abilitydetail.RuleSet{
				ID: snowflake.NewTestID(), AbilityID: fixture.ability.ID, OptionalValues: test.values, Version: 1,
			}
			_, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
			if !errors.Is(err, battle.ErrInitialStateCompilation) {
				t.Fatalf("ReadInitialStateFacts() error = %v，期望 ErrInitialStateCompilation", err)
			}
		})
	}
}

// TestGameDataFactsReaderFreezesAbilityDamageMultipliers 验证十类攻击方伤害倍率都由 Current Game Data
// 无损编译为 Battle 独占事实，并在生成 Battle Engine 初始状态时再次深复制可变属性集合。
func TestGameDataFactsReaderFreezesAbilityDamageMultipliers(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	lowHPElementID, weatherElementID, skillElementID := snowflake.NewTestID(), snowflake.NewTestID(), snowflake.NewTestID()
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.ability.ID,
		OptionalValues: abilitydetail.OptionalValues{
			BasePowerAtMostDamageBoost:   &abilitydetail.BasePowerAtMostDamageBoost{MaximumPower: 60, Numerator: 3, Denominator: 2},
			RecoilSkillDamageBoost:       &abilitydetail.RecoilSkillDamageBoost{Numerator: 6, Denominator: 5},
			LowHPElementDamageBoost:      &abilitydetail.LowHPElementDamageBoost{ElementID: lowHPElementID, HPThresholdNumerator: 1, HPThresholdDenominator: 3, DamageNumerator: 3, DamageDenominator: 2},
			WeatherElementDamageBoost:    &abilitydetail.WeatherElementDamageBoost{Weather: abilitydetail.WeatherKindSandstorm, ElementIDs: []snowflake.ID{weatherElementID}, Numerator: 13, Denominator: 10},
			ElementSkillDamageBoost:      &abilitydetail.ElementSkillDamageBoost{ElementIDs: []snowflake.ID{skillElementID}, Numerator: 3, Denominator: 2},
			SameElementBonusOverride:     &abilitydetail.SameElementBonusOverride{Numerator: 2, Denominator: 1},
			ContactBasedSkillDamageBoost: &abilitydetail.ContactBasedSkillDamageBoost{Numerator: 13, Denominator: 10},
			CriticalHitDamageBoost:       &abilitydetail.CriticalHitDamageBoost{Numerator: 3, Denominator: 2},
			SuperEffectiveDamageBoost:    &abilitydetail.SuperEffectiveDamageBoost{Numerator: 5, Denominator: 4},
			NotVeryEffectiveDamageBoost: &abilitydetail.NotVeryEffectiveDamageBoost{
				Numerator: 2, Denominator: 1,
			},
		}, Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	if member.BasePowerAtMostDamageBoost == nil || member.BasePowerAtMostDamageBoost.MaximumPower != 60 ||
		member.RecoilSkillDamageBoost == nil || member.LowHPElementDamageBoost == nil ||
		member.LowHPElementDamageBoost.ElementID != lowHPElementID || member.WeatherElementDamageBoost == nil ||
		len(member.WeatherElementDamageBoost.ElementIDs) != 1 || member.WeatherElementDamageBoost.ElementIDs[0] != weatherElementID ||
		member.ElementSkillDamageBoost == nil || member.SameElementBonusOverride == nil ||
		member.ContactBasedSkillDamageBoost == nil || member.CriticalHitDamageBoost == nil ||
		member.SuperEffectiveDamageBoost == nil || member.NotVeryEffectiveDamageBoost == nil {
		t.Fatalf("攻击型特性成员事实 = %+v", member)
	}

	initial, err := battle.CompileInitialState(fixture.session, facts)
	if err != nil {
		t.Fatalf("CompileInitialState() error = %v", err)
	}
	frozen := initial.Sides[0].Members[0]
	member.WeatherElementDamageBoost.ElementIDs[0] = snowflake.NewTestID()
	member.ElementSkillDamageBoost.ElementIDs[0] = snowflake.NewTestID()
	if frozen.BasePowerAtMostDamageBoost == nil || frozen.RecoilSkillDamageBoost == nil || frozen.LowHPElementDamageBoost == nil ||
		frozen.WeatherElementDamageBoost == nil || frozen.WeatherElementDamageBoost.ElementIDs[0] != weatherElementID ||
		frozen.ElementSkillDamageBoost == nil || frozen.ElementSkillDamageBoost.ElementIDs[0] != skillElementID ||
		frozen.SameElementBonusOverride == nil || frozen.ContactBasedSkillDamageBoost == nil ||
		frozen.CriticalHitDamageBoost == nil || frozen.SuperEffectiveDamageBoost == nil || frozen.NotVeryEffectiveDamageBoost == nil {
		t.Fatalf("Battle 未独立冻结攻击型特性: %+v", frozen)
	}
}
