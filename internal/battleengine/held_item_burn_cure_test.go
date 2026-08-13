package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemCuresBurn 验证灼伤净化道具只会在持有者成功获得灼伤后立即消耗。
// 测试固定主要异常写入和道具净化的相邻事件顺序，并拒绝把该道具错误扩大为其它主要异常的通用治疗。
func TestHeldItemCuresBurn(t *testing.T) {
	t.Parallel()
	t.Run("成功获得灼伤时立即净化并消耗", func(t *testing.T) {
		actor := newMember(1, "burn-cure-actor", 500, 500)
		actor.Stats.Speed = 200
		actor.Skills[0] = burnApplicationSkill()
		target := newMember(1, "burn-cure-target", 500, 500)
		target.ItemID = testID("burn-cure-item")
		target.HeldItemCuresBurn = true
		target.Skills[0] = fieldSpeedOrderSkill(1, "灼伤净化后行动", 0)

		result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, target), volatileTurn(1, 1, 1), mustRandom(t, 548))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
		if !found || member.MajorStatus != "" || member.ItemID != 0 || member.HeldItemCuresBurn {
			t.Fatalf("灼伤净化后的成员快照 = %+v", member)
		}
		assertBurnCureEventOrder(t, result.Events, testID("burn-cure-item"))
	})

	t.Run("其它主要异常不消耗道具", func(t *testing.T) {
		actor := newMember(1, "burn-cure-boundary-actor", 500, 500)
		actor.Stats.Speed = 200
		actor.Skills[0] = burnApplicationSkill()
		actor.Skills[0].StatusApplications[0].Status = battleengine.MajorStatusPoison
		target := newMember(1, "burn-cure-boundary-target", 500, 500)
		target.ItemID = testID("burn-cure-item")
		target.HeldItemCuresBurn = true
		target.Skills[0] = fieldSpeedOrderSkill(1, "边界后行动", 0)

		result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, target), volatileTurn(1, 1, 1), mustRandom(t, 549))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
		if !found || member.MajorStatus != battleengine.MajorStatusPoison || member.ItemID != testID("burn-cure-item") || !member.HeldItemCuresBurn {
			t.Fatalf("非灼伤不应消耗净化道具 = %+v", member)
		}
		for _, event := range result.Events {
			if _, consumed := event.(battleengine.HeldItemBurnCuredEvent); consumed {
				t.Fatalf("非灼伤不应产生道具净化事件 = %+v", event)
			}
		}
	})
}

// burnApplicationSkill 构造必定令单名对手灼伤的变化技能，用于验证一次性灼伤净化道具的触发窗口。
func burnApplicationSkill() battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("burn-application-skill"), Name: "灼伤施加", ElementID: testID("burn-element"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10,
		StatusApplications: []battleengine.MajorStatusApplication{{
			Status: battleengine.MajorStatusBurn, Target: battleengine.EffectTargetSelected, ChancePercent: 100,
		}},
	}
}

// assertBurnCureEventOrder 校验灼伤实际写入后才由道具解除，保证回放可观察到完整生命周期。
func assertBurnCureEventOrder(t *testing.T, events []battleengine.Event, itemID Identifier) {
	t.Helper()
	applicationIndex, cureIndex := -1, -1
	for index, event := range events {
		switch value := event.(type) {
		case battleengine.MajorStatusAppliedEvent:
			if value.Status == battleengine.MajorStatusBurn {
				applicationIndex = index
			}
		case battleengine.HeldItemBurnCuredEvent:
			if value.ItemID == itemID && value.Status == battleengine.MajorStatusBurn {
				cureIndex = index
			}
		}
	}
	if applicationIndex < 0 || cureIndex != applicationIndex+1 {
		t.Fatalf("灼伤净化事件顺序 = %+v", events)
	}
}
