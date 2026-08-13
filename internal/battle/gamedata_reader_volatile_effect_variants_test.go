package battle_test

import (
	"context"
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	"github.com/lishangbu/avalon/internal/battleengine"
	"github.com/lishangbu/avalon/internal/gamedata/battleformat"
	"github.com/lishangbu/avalon/internal/gamedata/skilldetail"
)

// TestGameDataFactsReaderFreezesEveryVolatileEffectVariant 验证全部易变状态资料在 Battle 创建时转换为
// Battle Engine 封闭枚举与精确参数，避免运行期依赖管理模型、展示名称或自由文本。
func TestGameDataFactsReaderFreezesEveryVolatileEffectVariant(t *testing.T) {
	t.Parallel()
	fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
	fixture.detail = &skilldetail.RuleSet{
		ID: snowflake.NewTestID(), SkillID: fixture.skillID,
		OptionalValues: skilldetail.OptionalValues{VolatileEffects: []skilldetail.VolatileEffect{
			{Status: skilldetail.VolatileStatusConfusion, Target: skilldetail.VolatileEffectTargetSelectedTarget, ChancePercent: 100, MinTurns: 2, MaxTurns: 5},
			{Status: skilldetail.VolatileStatusBinding, Target: skilldetail.VolatileEffectTargetSelectedTarget, ChancePercent: 100, MinTurns: 4, MaxTurns: 5},
			{Status: skilldetail.VolatileStatusTaunt, Target: skilldetail.VolatileEffectTargetSelectedTarget, ChancePercent: 100, MinTurns: 3, MaxTurns: 3},
			{Status: skilldetail.VolatileStatusCharging, Target: skilldetail.VolatileEffectTargetUser, ChancePercent: 100, MinTurns: 1, MaxTurns: 1},
			{Status: skilldetail.VolatileStatusLockedMove, Target: skilldetail.VolatileEffectTargetUser, ChancePercent: 100, MinTurns: 2, MaxTurns: 3},
			{Status: skilldetail.VolatileStatusDisable, Target: skilldetail.VolatileEffectTargetSelectedTarget, ChancePercent: 100, MinTurns: 4, MaxTurns: 4},
			{Status: skilldetail.VolatileStatusProtection, Target: skilldetail.VolatileEffectTargetUser, ChancePercent: 100, MinTurns: 1, MaxTurns: 1},
			{Status: skilldetail.VolatileStatusSubstitute, Target: skilldetail.VolatileEffectTargetUser, ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4},
		}}, Version: 1,
	}
	facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
	if err != nil {
		t.Fatalf("ReadInitialStateFacts() error = %v", err)
	}
	want := []battleengine.VolatileStatusApplication{
		{Status: battleengine.VolatileStatusConfusion, Target: battleengine.EffectTargetSelected, ChancePercent: 100, MinTurns: 2, MaxTurns: 5},
		{Status: battleengine.VolatileStatusBinding, Target: battleengine.EffectTargetSelected, ChancePercent: 100, MinTurns: 4, MaxTurns: 5},
		{Status: battleengine.VolatileStatusTaunt, Target: battleengine.EffectTargetSelected, ChancePercent: 100, MinTurns: 3, MaxTurns: 3},
		{Status: battleengine.VolatileStatusCharging, Target: battleengine.EffectTargetUser, ChancePercent: 100, MinTurns: 1, MaxTurns: 1},
		{Status: battleengine.VolatileStatusLockedMove, Target: battleengine.EffectTargetUser, ChancePercent: 100, MinTurns: 2, MaxTurns: 3},
		{Status: battleengine.VolatileStatusDisable, Target: battleengine.EffectTargetSelected, ChancePercent: 100, MinTurns: 4, MaxTurns: 4},
		{Status: battleengine.VolatileStatusProtection, Target: battleengine.EffectTargetUser, ChancePercent: 100, MinTurns: 1, MaxTurns: 1},
		{Status: battleengine.VolatileStatusSubstitute, Target: battleengine.EffectTargetUser, ChancePercent: 100, MinTurns: 1, MaxTurns: 1, SubstituteCostNumerator: 1, SubstituteCostDenominator: 4},
	}
	got := facts.Sides[0].Members[0].Skills[0].VolatileStatusApplications
	if !reflect.DeepEqual(got, want) {
		t.Fatalf("冻结易变状态 = %+v，期望 %+v", got, want)
	}
}
