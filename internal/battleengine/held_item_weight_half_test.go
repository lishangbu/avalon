package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemWeightHalf 验证减半体重道具只改变持有者参与动态威力判断时的有效体重。
// 它不会改写权威基础体重，也不会在道具移除后继续影响重量阈值和双方体重比例。
func TestHeldItemWeightHalf(t *testing.T) {
	t.Parallel()
	actor := newMember(1, "weight-half-actor", 500, 500)
	actor.Stats.Speed = 200
	actor.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("weight-half-skill"), Name: "重量技", ElementID: actor.ElementIDs[0], DamageClass: battleengine.DamageClassPhysical,
		Power: 120, Accuracy: 100, RemainingPP: 10, MaxPP: 10, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		DynamicPower: battleengine.DynamicPowerRule{Kind: battleengine.DynamicPowerKindTargetWeightThresholds, FallbackPower: 120, WeightThresholds: []battleengine.WeightPowerThreshold{{MaximumWeightInclusive: 250, Power: 20}}},
	}
	target := newMember(1, "weight-half-target", 500, 500)
	target.Stats.Speed = 10
	target.Weight = 400
	target.ItemID = testID("weight-half-item")
	target.HeldItemWeightHalf = true

	result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, target), volatileTurn(1, 1, 1), mustRandom(t, 542))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	if member, _ := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}); member.Weight != 400 {
		t.Fatalf("道具不应改写权威基础体重 = %d", member.Weight)
	}
	withoutItem := target
	withoutItem.ItemID = 0
	noItem, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, withoutItem), volatileTurn(1, 1, 1), mustRandom(t, 543))
	if err != nil {
		t.Fatalf("失去道具 ResolveTurn() error = %v", err)
	}
	withDamage := targetDamageForSkill(result.Events, testID("weight-half-skill"))
	withoutDamage := targetDamageForSkill(noItem.Events, testID("weight-half-skill"))
	if withDamage == 0 || withoutDamage <= withDamage {
		t.Fatalf("减半体重动态威力伤害不正确 = with:%d without:%d", withDamage, withoutDamage)
	}
}

// targetDamageForSkill 返回事件流中指定技能对目标造成的实际本体伤害。
func targetDamageForSkill(events []battleengine.Event, skillID Identifier) uint32 {
	for _, event := range events {
		if value, ok := event.(battleengine.DamageAppliedEvent); ok && value.SkillID == skillID {
			return value.Amount
		}
	}
	return 0
}
