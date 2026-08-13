package battleengine

import "testing"

// TestHeldItemAccuracyModifiers 验证广角镜与光粉按固定顺序修正普通命中率，必中语义保持为 0。
func TestHeldItemAccuracyModifiers(t *testing.T) {
	t.Parallel()
	actor := MemberSnapshot{ItemID: testID("wide-lens"), HeldItemAccuracyBoost: true}
	target := MemberSnapshot{ItemID: testID("bright-powder"), HeldItemOpponentAccuracyReduction: true}
	skill := SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Accuracy: 80, DamageClass: DamageClassPhysical}
	if got := skillAccuracy(nil, actor, MemberSnapshot{}, skill); got != 88 {
		t.Fatalf("广角镜命中率 = %d，期望 88", got)
	}
	if got := skillAccuracy(nil, MemberSnapshot{}, target, skill); got != 72 {
		t.Fatalf("光粉命中率 = %d，期望 72", got)
	}
	if got := skillAccuracy(nil, actor, target, skill); got != 79 {
		t.Fatalf("两种道具叠加命中率 = %d，期望 79", got)
	}
}

// TestHeldItemCriticalHitStageBoost 验证要害道具把零级概率表从 1/24 提升到一级 1/8。
func TestHeldItemCriticalHitStageBoost(t *testing.T) {
	t.Parallel()
	random, err := NewRandomSource(RandomAlgorithmSplitMix64V1, 1)
	if err != nil {
		t.Fatalf("NewRandomSource() error = %v", err)
	}
	actor := MemberSnapshot{ItemID: testID("scope-lens"), HeldItemCriticalHitStageBoost: true}
	_, _, trace, err := resolveCriticalHit(actor, SkillSnapshot{TargetScope: SkillTargetScopeSelectedTarget, DamageMode: SkillDamageModeFormula, MinHits: 1, MaxHits: 1, SkillID: testID("critical-item")}, random)
	if err != nil || len(trace) != 1 || trace[0].Bound != 8 {
		t.Fatalf("要害道具随机轨迹 = %+v error=%v", trace, err)
	}
}

// TestHeldItemSpeedHalf 验证速度减半道具在麻痹之后继续取整，且最低有效速度为 1。
func TestHeldItemSpeedHalf(t *testing.T) {
	t.Parallel()
	member := MemberSnapshot{ItemID: testID("iron-ball"), HeldItemSpeedHalf: true, Stats: StatBlock{Speed: 101}, MajorStatus: MajorStatusParalysis}
	if got := effectiveSpeed(member); got != 25 {
		t.Fatalf("麻痹并持有铁球的速度 = %d，期望 25", got)
	}
}

// TestHeldItemGroundingOverrides 验证强制接地优先于空中道具，而单独气球会覆盖飞行属性之外的自然接地。
func TestHeldItemGroundingOverrides(t *testing.T) {
	t.Parallel()
	rules := RuleSnapshot{ElementIDs: map[string]Identifier{"flying": testID("flying-element")}}
	if memberGrounded(rules, MemberSnapshot{ItemID: testID("air-balloon"), HeldItemAirborneUntilDamaged: true}) {
		t.Fatal("气球持有者应视为空中")
	}
	if !memberGrounded(rules, MemberSnapshot{ItemID: testID("iron-ball"), HeldItemForceGrounded: true, HeldItemAirborneUntilDamaged: true, ElementIDs: testIDs("flying-element")}) {
		t.Fatal("强制接地必须优先于空中效果和飞行属性")
	}
}
