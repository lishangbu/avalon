package battleengine

import "testing"

// TestDynamicPowerCalculatesEverySupportedRule 验证每种动态基础威力规则都读取当前战斗快照，而不是静态技能名称或
// 初始资料值。数值断言覆盖排序方向、交叉相乘边界、整数向下取整和兜底档位。
func TestDynamicPowerCalculatesEverySupportedRule(t *testing.T) {
	t.Parallel()
	user := MemberSnapshot{
		MaxHP: 100, CurrentHP: 100, Weight: 500,
		Stats: StatBlock{Speed: 100}, StatStages: map[Stat]int8{StatAttack: 2, StatSpecialAttack: 3},
	}
	target := MemberSnapshot{
		MaxHP: 100, CurrentHP: 100, Weight: 100,
		Stats: StatBlock{Speed: 300}, StatStages: map[Stat]int8{StatDefense: 1},
	}
	tests := []struct {
		// name 是当前规则类型的稳定测试名称。
		name string
		// userMutate 仅修改本场使用者运行态，以验证规则读取的是行动时的最新数据。
		userMutate func(*MemberSnapshot)
		// targetMutate 仅修改本场目标运行态，以验证逐目标范围会分别计算威力。
		targetMutate func(*MemberSnapshot)
		// rule 是需要执行的已冻结动态威力规则。
		rule DynamicPowerRule
		// want 是本次进入伤害公式的确切基础威力。
		want uint16
	}{
		{
			name: "正向能力阶级总和", rule: DynamicPowerRule{
				Kind: DynamicPowerKindPositiveStatStageSum, Source: EffectTargetUser, BasePower: 20, PowerPerPositiveStage: 20,
			}, want: 120,
		},
		{
			name: "使用者速度比例阈值", rule: DynamicPowerRule{
				Kind: DynamicPowerKindUserSpeedRatioThresholds, FallbackPower: 40,
				SpeedThresholds: []SpeedPowerThreshold{{MinimumRatio: 4, Power: 150}, {MinimumRatio: 3, Power: 120}, {MinimumRatio: 2, Power: 80}, {MinimumRatio: 1, Power: 60}},
			}, want: 40,
		},
		{
			name: "目标相对使用者速度比例", rule: DynamicPowerRule{
				Kind: DynamicPowerKindTargetToUserSpeedRatio, SpeedRatioMultiplier: 25, SpeedRatioAdditivePower: 1, MaximumPower: 150,
			}, want: 76,
		},
		{
			name: "目标体重阈值", rule: DynamicPowerRule{
				Kind: DynamicPowerKindTargetWeightThresholds, FallbackPower: 120,
				WeightThresholds: []WeightPowerThreshold{{MaximumWeightInclusive: 100, Power: 20}, {MaximumWeightInclusive: 250, Power: 40}},
			}, want: 20,
		},
		{
			name: "使用者与目标体重比例", rule: DynamicPowerRule{
				Kind: DynamicPowerKindUserTargetWeightRatioThresholds, FallbackPower: 40,
				WeightRatioThresholds: []WeightRatioPowerThreshold{{MinimumUserToTargetRatio: 5, Power: 120}, {MinimumUserToTargetRatio: 3, Power: 80}, {MinimumUserToTargetRatio: 2, Power: 60}},
			}, want: 120,
		},
		{
			name: "使用者生命比例", userMutate: func(member *MemberSnapshot) { member.CurrentHP = 1 }, rule: DynamicPowerRule{
				Kind: DynamicPowerKindUserHPFractionThresholds, HPFractionScale: 48, FallbackPower: 20,
				HPFractionThresholds: []HPFractionPowerThreshold{{MaximumScaledHPInclusive: 2, Power: 200}, {MaximumScaledHPInclusive: 10, Power: 150}},
			}, want: 200,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			currentUser, currentTarget := user, target
			if test.userMutate != nil {
				test.userMutate(&currentUser)
			}
			if test.targetMutate != nil {
				test.targetMutate(&currentTarget)
			}
			if err := validateDynamicPowerRule(test.rule); err != nil {
				t.Fatalf("validateDynamicPowerRule() error = %v", err)
			}
			if got := dynamicPower(SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Power: 10, DynamicPower: test.rule}, currentUser, currentTarget); got != test.want {
				t.Fatalf("dynamicPower() = %d，期望 %d", got, test.want)
			}
		})
	}
}

// TestDynamicPowerUsesEffectiveSpeed 验证速度类动态威力与行动排序共享同一有效速度口径，包括速度能力阶级和
// 麻痹造成的减速；否则战斗的行动顺序与伤害公式会对同一状态得出互相矛盾的结论。
func TestDynamicPowerUsesEffectiveSpeed(t *testing.T) {
	t.Parallel()
	rule := DynamicPowerRule{
		Kind: DynamicPowerKindUserSpeedRatioThresholds, FallbackPower: 40,
		SpeedThresholds: []SpeedPowerThreshold{{MinimumRatio: 2, Power: 80}, {MinimumRatio: 1, Power: 60}},
	}
	user := MemberSnapshot{Stats: StatBlock{Speed: 100}, StatStages: map[Stat]int8{StatSpeed: 6}}
	target := MemberSnapshot{Stats: StatBlock{Speed: 200}, StatStages: map[Stat]int8{}}
	if got := dynamicPower(SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, DynamicPower: rule}, user, target); got != 80 {
		t.Fatalf("速度 +6 时动态威力 = %d，期望 80", got)
	}
	user.MajorStatus = MajorStatusParalysis
	if got := dynamicPower(SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, DynamicPower: rule}, user, target); got != 60 {
		t.Fatalf("麻痹后动态威力 = %d，期望 60", got)
	}
}

// TestValidateDynamicPowerRuleRejectsUnsafeOrUnorderedDefinitions 验证资料编译器不会把可能溢出或排序含糊的
// 动态威力规则交给战斗引擎，从而保持离线重放的基础威力计算可复现。
func TestValidateDynamicPowerRuleRejectsUnsafeOrUnorderedDefinitions(t *testing.T) {
	t.Parallel()
	tests := []DynamicPowerRule{
		{
			Kind: DynamicPowerKindPositiveStatStageSum, Source: EffectTargetUser, BasePower: 1, PowerPerPositiveStage: ^uint16(0),
		},
		{
			Kind: DynamicPowerKindUserSpeedRatioThresholds, FallbackPower: 20,
			SpeedThresholds: []SpeedPowerThreshold{{MinimumRatio: 2, Power: 40}, {MinimumRatio: 2, Power: 60}},
		},
		{
			Kind: DynamicPowerKindTargetWeightThresholds, FallbackPower: 20,
			WeightThresholds: []WeightPowerThreshold{{MaximumWeightInclusive: 250, Power: 40}, {MaximumWeightInclusive: 100, Power: 20}},
		},
	}
	for index, rule := range tests {
		if err := validateDynamicPowerRule(rule); err == nil {
			t.Fatalf("规则 %d 未拒绝无效动态威力定义：%+v", index, rule)
		}
	}
}
