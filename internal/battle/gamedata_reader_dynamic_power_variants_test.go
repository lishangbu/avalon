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

// TestGameDataFactsReaderFreezesEveryDynamicPowerVariant 验证六类动态基础威力资料都在 Battle 创建时转换为
// Battle Engine 强类型快照；运行期只读取冻结参数和成员当前状态，不再访问或猜测 Current Game Data。
func TestGameDataFactsReaderFreezesEveryDynamicPowerVariant(t *testing.T) {
	t.Parallel()
	tests := []struct {
		// name 是子测试展示的稳定规则名称。
		name string
		// source 是 Current Game Data 中经过管理边界校验的技能详情。
		source skilldetail.DynamicPower
		// want 是 Battle 编译边界必须冻结出的纯引擎规则。
		want battleengine.DynamicPowerRule
	}{
		{name: "正向能力阶级总和", source: skilldetail.DynamicPower{
			Kind: skilldetail.DynamicPowerKindPositiveStatStageSum, Source: skilldetail.DynamicPowerSourceSelectedTarget,
			BasePower: 20, PowerPerPositiveStage: 20, MaximumPower: 200,
		}, want: battleengine.DynamicPowerRule{
			Kind: battleengine.DynamicPowerKindPositiveStatStageSum, Source: battleengine.EffectTargetSelected,
			BasePower: 20, PowerPerPositiveStage: 20, MaximumPower: 200,
		}},
		{name: "使用者速度比例阈值", source: skilldetail.DynamicPower{
			Kind: skilldetail.DynamicPowerKindUserSpeedRatioThresholds, FallbackPower: 40,
			SpeedThresholds: []skilldetail.SpeedPowerThreshold{{MinimumRatio: 2, Power: 80}, {MinimumRatio: 1, Power: 60}},
		}, want: battleengine.DynamicPowerRule{
			Kind: battleengine.DynamicPowerKindUserSpeedRatioThresholds, FallbackPower: 40,
			SpeedThresholds: []battleengine.SpeedPowerThreshold{{MinimumRatio: 2, Power: 80}, {MinimumRatio: 1, Power: 60}},
		}},
		{name: "目标相对使用者速度比例", source: skilldetail.DynamicPower{
			Kind: skilldetail.DynamicPowerKindTargetToUserSpeedRatio, SpeedRatioMultiplier: 25,
			SpeedRatioAdditivePower: 1, MaximumPower: 150,
		}, want: battleengine.DynamicPowerRule{
			Kind: battleengine.DynamicPowerKindTargetToUserSpeedRatio, SpeedRatioMultiplier: 25,
			SpeedRatioAdditivePower: 1, MaximumPower: 150,
		}},
		{name: "目标体重阈值", source: skilldetail.DynamicPower{
			Kind: skilldetail.DynamicPowerKindTargetWeightThresholds, FallbackPower: 120,
			WeightThresholds: []skilldetail.WeightPowerThreshold{{MaximumWeightInclusive: 100, Power: 20}, {MaximumWeightInclusive: 250, Power: 40}},
		}, want: battleengine.DynamicPowerRule{
			Kind: battleengine.DynamicPowerKindTargetWeightThresholds, FallbackPower: 120,
			WeightThresholds: []battleengine.WeightPowerThreshold{{MaximumWeightInclusive: 100, Power: 20}, {MaximumWeightInclusive: 250, Power: 40}},
		}},
		{name: "使用者目标体重比例阈值", source: skilldetail.DynamicPower{
			Kind: skilldetail.DynamicPowerKindUserTargetWeightRatioThresholds, FallbackPower: 40,
			WeightRatioThresholds: []skilldetail.WeightRatioPowerThreshold{{MinimumUserToTargetRatio: 5, Power: 120}, {MinimumUserToTargetRatio: 2, Power: 60}},
		}, want: battleengine.DynamicPowerRule{
			Kind: battleengine.DynamicPowerKindUserTargetWeightRatioThresholds, FallbackPower: 40,
			WeightRatioThresholds: []battleengine.WeightRatioPowerThreshold{{MinimumUserToTargetRatio: 5, Power: 120}, {MinimumUserToTargetRatio: 2, Power: 60}},
		}},
		{name: "使用者生命比例阈值", source: skilldetail.DynamicPower{
			Kind: skilldetail.DynamicPowerKindUserHPFractionThresholds, HPFractionScale: 48, FallbackPower: 20,
			HPFractionThresholds: []skilldetail.HPFractionPowerThreshold{{MaximumScaledHPInclusive: 2, Power: 200}, {MaximumScaledHPInclusive: 10, Power: 150}},
		}, want: battleengine.DynamicPowerRule{
			Kind: battleengine.DynamicPowerKindUserHPFractionThresholds, HPFractionScale: 48, FallbackPower: 20,
			HPFractionThresholds: []battleengine.HPFractionPowerThreshold{{MaximumScaledHPInclusive: 2, Power: 200}, {MaximumScaledHPInclusive: 10, Power: 150}},
		}},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			fixture := newInitialStateDataFixture(t, battleformat.LevelRule{Mode: battleformat.LevelRuleNormalize, Level: int32Pointer(50)})
			fixture.detail = &skilldetail.RuleSet{
				ID: snowflake.NewTestID(), SkillID: fixture.skillID,
				OptionalValues: skilldetail.OptionalValues{DynamicPower: test.source}, Version: 1,
			}
			facts, err := fixture.reader().ReadInitialStateFacts(context.Background(), fixture.session)
			if err != nil {
				t.Fatalf("ReadInitialStateFacts() error = %v", err)
			}
			got := facts.Sides[0].Members[0].Skills[0].DynamicPower
			if !reflect.DeepEqual(got, test.want) {
				t.Fatalf("冻结动态威力 = %+v，期望 %+v", got, test.want)
			}
		})
	}
}
