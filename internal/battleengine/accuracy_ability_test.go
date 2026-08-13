package battleengine

import "testing"

// TestSkillAccuracyAppliesAbilityMultipliers 验证特性命中倍率在能力阶级之后统一相乘并只在最后取整。
func TestSkillAccuracyAppliesAbilityMultipliers(t *testing.T) {
	t.Parallel()
	actor := MemberSnapshot{
		AccuracyMultiplier:              &AccuracyMultiplier{Numerator: 13, Denominator: 10},
		PhysicalSkillAccuracyMultiplier: &AccuracyMultiplier{Numerator: 4, Denominator: 5},
	}
	target := MemberSnapshot{}
	if got := skillAccuracy(nil, actor, target, SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Accuracy: 70, DamageClass: DamageClassPhysical}); got != 72 {
		t.Fatalf("物理技能特性命中率 = %d，期望 72", got)
	}
	if got := skillAccuracy(nil, actor, target, SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Accuracy: 70, DamageClass: DamageClassSpecial}); got != 91 {
		t.Fatalf("特殊技能不应使用物理专用倍率，命中率 = %d，期望 91", got)
	}
}

// TestSkillAccuracyAppliesOpponentConditionalMultipliers 验证目标侧的沙暴、降雪和混乱规则各自独立生效。
func TestSkillAccuracyAppliesOpponentConditionalMultipliers(t *testing.T) {
	t.Parallel()
	actor := MemberSnapshot{}
	skill := SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Accuracy: 100, DamageClass: DamageClassPhysical}
	tests := []struct {
		name         string
		weather      *WeatherEffect
		target       MemberSnapshot
		wantAccuracy uint8
	}{
		{
			name:         "沙暴",
			weather:      &WeatherEffect{Kind: WeatherKindSandstorm},
			target:       MemberSnapshot{OpponentAccuracySandstormMultiplier: &AccuracyMultiplier{Numerator: 4, Denominator: 5}},
			wantAccuracy: 80,
		},
		{
			name:         "降雪",
			weather:      &WeatherEffect{Kind: WeatherKindSnow},
			target:       MemberSnapshot{OpponentAccuracySnowMultiplier: &AccuracyMultiplier{Numerator: 4, Denominator: 5}},
			wantAccuracy: 80,
		},
		{
			name: "混乱",
			target: MemberSnapshot{
				ConfusionTurnsRemaining:             2,
				OpponentAccuracyConfusionMultiplier: &AccuracyMultiplier{Numerator: 1, Denominator: 2},
			},
			wantAccuracy: 50,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			if got := skillAccuracy(test.weather, actor, test.target, skill); got != test.wantAccuracy {
				t.Fatalf("命中率 = %d，期望 %d", got, test.wantAccuracy)
			}
		})
	}
}

// TestSkillAccuracyAppliesAlwaysHitCapAndStageIgnore 验证必中、变化技能命中上限及双方命中阶级忽略规则的边界。
func TestSkillAccuracyAppliesAlwaysHitCapAndStageIgnore(t *testing.T) {
	t.Parallel()
	t.Run("普通必中不影响一击必杀", func(t *testing.T) {
		t.Parallel()
		actor := MemberSnapshot{Level: 50, AccuracyAlwaysHits: true}
		target := MemberSnapshot{Level: 50}
		if got := skillAccuracy(nil, actor, target, SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Accuracy: 25, DamageClass: DamageClassPhysical}); got != 0 {
			t.Fatalf("普通必中技能命中率 = %d，期望 0", got)
		}
		if got := skillAccuracy(nil, actor, target, SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, MinHits: 1, MaxHits: 1, DamageClass: DamageClassPhysical, DamageMode: SkillDamageModeOneHitKnockOut, OneHitKnockOutBaseAccuracy: 30}); got != 30 {
			t.Fatalf("一击必杀命中率 = %d，期望 30", got)
		}
	})
	t.Run("变化技能上限只作用于目标", func(t *testing.T) {
		t.Parallel()
		target := MemberSnapshot{StatusSkillAccuracyCap: 50}
		if got := skillAccuracy(nil, MemberSnapshot{}, target, SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Accuracy: 100, DamageClass: DamageClassStatus}); got != 50 {
			t.Fatalf("变化技能命中上限 = %d，期望 50", got)
		}
		if got := skillAccuracy(nil, MemberSnapshot{}, target, SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Accuracy: 100, DamageClass: DamageClassPhysical}); got != 100 {
			t.Fatalf("物理技能不应受变化技能上限影响，命中率 = %d，期望 100", got)
		}
	})
	t.Run("双方按角色忽略对手阶级", func(t *testing.T) {
		t.Parallel()
		if got := skillAccuracy(nil, MemberSnapshot{StatStages: map[Stat]int8{StatAccuracy: -1}}, MemberSnapshot{
			IgnoreOpponentAccuracyStatStages: true,
		}, SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Accuracy: 75, DamageClass: DamageClassPhysical}); got != 75 {
			t.Fatalf("目标无视使用者命中阶级后的命中率 = %d，期望 75", got)
		}
		if got := skillAccuracy(nil, MemberSnapshot{IgnoreOpponentAccuracyStatStages: true}, MemberSnapshot{
			StatStages: map[Stat]int8{StatEvasion: 1},
		}, SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Accuracy: 100, DamageClass: DamageClassPhysical}); got != 100 {
			t.Fatalf("使用者无视目标闪避阶级后的命中率 = %d，期望 100", got)
		}
	})
}

// TestSkillAccuracyIgnoreTargetAbilityEffects 验证使用者无视目标特性时，只跳过目标侧的必中、阶级、倍率和
// 变化技能命中上限；使用者自身的命中规则仍按原逻辑计算。
func TestSkillAccuracyIgnoreTargetAbilityEffects(t *testing.T) {
	t.Parallel()
	actor := MemberSnapshot{
		IgnoreTargetAbilityEffects: true,
		StatStages:                 map[Stat]int8{StatAccuracy: -1},
	}
	target := MemberSnapshot{
		AccuracyAlwaysHits:                  true,
		IgnoreOpponentAccuracyStatStages:    true,
		OpponentAccuracyConfusionMultiplier: &AccuracyMultiplier{Numerator: 1, Denominator: 2},
		ConfusionTurnsRemaining:             2,
		StatusSkillAccuracyCap:              40,
	}
	if got := skillAccuracy(nil, actor, target, SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Accuracy: 75, DamageClass: DamageClassStatus}); got != 56 {
		t.Fatalf("无视目标命中防守特性后的命中率 = %d，期望 56", got)
	}
}
