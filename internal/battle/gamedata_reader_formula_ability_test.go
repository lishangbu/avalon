package battle_test

import (
	"context"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/gamedata/abilitydetail"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
)

// TestGameDataFactsReaderFreezesFormulaAbilityRules112Through121 验证性别关系、技能分类、追加效果抑制
// 与三类承伤倍率在 Battle 启动时形成独立深副本。读取完成后修改 Current Game Data 的规则指针，不能
// 改变已经冻结的 Battle 事实。
func TestGameDataFactsReaderFreezesFormulaAbilityRules112Through121(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.abilityDetail = &abilitydetail.RuleSet{
		ID: snowflake.NewTestID(), AbilityID: fixture.member.AbilityID,
		OptionalValues: abilitydetail.OptionalValues{
			TargetGenderDamageMultiplier:          &abilitydetail.TargetGenderDamageMultiplier{SameGenderNumerator: 5, SameGenderDenominator: 4, OppositeGenderNumerator: 3, OppositeGenderDenominator: 4},
			PunchBasedSkillDamageBoost:            &abilitydetail.PunchBasedSkillDamageBoost{Numerator: 6, Denominator: 5},
			SlicingBasedSkillDamageBoost:          &abilitydetail.SlicingBasedSkillDamageBoost{Numerator: 3, Denominator: 2},
			SoundBasedSkillDamageBoost:            &abilitydetail.SoundBasedSkillDamageBoost{Numerator: 13, Denominator: 10},
			PulseBasedSkillDamageBoost:            &abilitydetail.PulseBasedSkillDamageBoost{Numerator: 3, Denominator: 2},
			BiteBasedSkillDamageBoost:             &abilitydetail.BiteBasedSkillDamageBoost{Numerator: 3, Denominator: 2},
			SecondaryEffectsSuppressedDamageBoost: &abilitydetail.SecondaryEffectsSuppressedDamageBoost{Numerator: 13, Denominator: 10},
			SoundBasedSkillDamageReduction:        &abilitydetail.SoundBasedSkillDamageReduction{Numerator: 1, Denominator: 2},
			SuperEffectiveDamageReduction:         &abilitydetail.SuperEffectiveDamageReduction{Numerator: 3, Denominator: 4},
			FullHPDamageReduction:                 &abilitydetail.FullHPDamageReduction{Numerator: 1, Denominator: 2},
		},
		Version: 1,
	}

	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	member := facts.Sides[0].Members[0]
	fixture.abilityDetail.TargetGenderDamageMultiplier.SameGenderNumerator = 1
	fixture.abilityDetail.PunchBasedSkillDamageBoost.Numerator = 1
	fixture.abilityDetail.SlicingBasedSkillDamageBoost.Numerator = 1
	fixture.abilityDetail.SoundBasedSkillDamageBoost.Numerator = 1
	fixture.abilityDetail.PulseBasedSkillDamageBoost.Numerator = 1
	fixture.abilityDetail.BiteBasedSkillDamageBoost.Numerator = 1
	fixture.abilityDetail.SecondaryEffectsSuppressedDamageBoost.Numerator = 1
	fixture.abilityDetail.SoundBasedSkillDamageReduction.Numerator = 2
	fixture.abilityDetail.SuperEffectiveDamageReduction.Numerator = 1
	fixture.abilityDetail.FullHPDamageReduction.Numerator = 2

	if member.TargetGenderDamageMultiplier == nil || member.TargetGenderDamageMultiplier.SameGenderNumerator != 5 ||
		member.PunchBasedSkillDamageBoost == nil || member.PunchBasedSkillDamageBoost.Numerator != 6 ||
		member.SlicingBasedSkillDamageBoost == nil || member.SlicingBasedSkillDamageBoost.Numerator != 3 ||
		member.SoundBasedSkillDamageBoost == nil || member.SoundBasedSkillDamageBoost.Numerator != 13 ||
		member.PulseBasedSkillDamageBoost == nil || member.PulseBasedSkillDamageBoost.Numerator != 3 ||
		member.BiteBasedSkillDamageBoost == nil || member.BiteBasedSkillDamageBoost.Numerator != 3 ||
		member.SecondaryEffectsSuppressedDamageBoost == nil || member.SecondaryEffectsSuppressedDamageBoost.Numerator != 13 ||
		member.SoundBasedSkillDamageReduction == nil || member.SoundBasedSkillDamageReduction.Numerator != 1 ||
		member.SuperEffectiveDamageReduction == nil || member.SuperEffectiveDamageReduction.Numerator != 3 ||
		member.FullHPDamageReduction == nil || member.FullHPDamageReduction.Numerator != 1 {
		t.Fatalf("规则 112—121 的 Battle 冻结事实被实时资料污染: %+v", member)
	}
	if member.SoundBasedSkillDamageReduction.Denominator != 2 || member.FullHPDamageReduction.Denominator != 2 ||
		member.TargetGenderDamageMultiplier.OppositeGenderDenominator != 4 {
		t.Fatalf("规则 112—121 的冻结分数不完整: %+v", member)
	}
}
