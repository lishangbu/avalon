package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemCuresParalysis 验证麻痹净化道具只会在持有者成功获得麻痹后立即消耗。
// 测试同时固定异常写入、道具净化和后续行动的事件顺序，避免将道具错误用于其它主要异常或无异常的成员。
func TestHeldItemCuresParalysis(t *testing.T) {
	t.Parallel()
	t.Run("成功获得麻痹时立即净化并消耗", func(t *testing.T) {
		actor := newMember(1, "paralysis-cure-actor", 500, 500)
		actor.Stats.Speed = 200
		actor.Skills[0] = paralysisApplicationSkill()
		target := newMember(1, "paralysis-cure-target", 500, 500)
		target.ItemID = testID("paralysis-cure-item")
		target.HeldItemCuresParalysis = true
		target.Skills[0] = fieldSpeedOrderSkill(1, "净化后行动", 0)

		result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, target), volatileTurn(1, 1, 1), mustRandom(t, 544))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
		if !found || member.MajorStatus != "" || member.ItemID != 0 || member.HeldItemCuresParalysis {
			t.Fatalf("麻痹净化后的成员快照 = %+v", member)
		}
		assertParalysisCureEventOrder(t, result.Events, testID("paralysis-cure-item"))
	})

	t.Run("其它主要异常不消耗道具", func(t *testing.T) {
		actor := newMember(1, "paralysis-cure-boundary-actor", 500, 500)
		actor.Stats.Speed = 200
		actor.Skills[0] = paralysisApplicationSkill()
		actor.Skills[0].StatusApplications[0].Status = battleengine.MajorStatusBurn
		target := newMember(1, "paralysis-cure-boundary-target", 500, 500)
		target.ItemID = testID("paralysis-cure-item")
		target.HeldItemCuresParalysis = true
		target.Skills[0] = fieldSpeedOrderSkill(1, "边界后行动", 0)

		result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, target), volatileTurn(1, 1, 1), mustRandom(t, 545))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
		if !found || member.MajorStatus != battleengine.MajorStatusBurn || member.ItemID != testID("paralysis-cure-item") || !member.HeldItemCuresParalysis {
			t.Fatalf("非麻痹不应消耗净化道具 = %+v", member)
		}
		for _, event := range result.Events {
			if _, consumed := event.(battleengine.HeldItemParalysisCuredEvent); consumed {
				t.Fatalf("非麻痹不应产生道具净化事件 = %+v", event)
			}
		}
	})
}

// paralysisApplicationSkill 构造必定对单名对手施加麻痹的变化技能，用于验证持有道具的即时触发窗口。
func paralysisApplicationSkill() battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("paralysis-application-skill"), Name: "麻痹施加", ElementID: testID("paralysis-element"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10,
		StatusApplications: []battleengine.MajorStatusApplication{{
			Status: battleengine.MajorStatusParalysis, Target: battleengine.EffectTargetSelected, ChancePercent: 100,
		}},
	}
}

// assertParalysisCureEventOrder 校验异常实际写入后才由道具解除，保证重放可观察到完整生命周期。
func assertParalysisCureEventOrder(t *testing.T, events []battleengine.Event, itemID Identifier) {
	t.Helper()
	applicationIndex, cureIndex := -1, -1
	for index, event := range events {
		switch value := event.(type) {
		case battleengine.MajorStatusAppliedEvent:
			if value.Status == battleengine.MajorStatusParalysis {
				applicationIndex = index
			}
		case battleengine.HeldItemParalysisCuredEvent:
			if value.ItemID == itemID && value.Status == battleengine.MajorStatusParalysis {
				cureIndex = index
			}
		}
	}
	if applicationIndex < 0 || cureIndex != applicationIndex+1 {
		t.Fatalf("麻痹净化事件顺序 = %+v", events)
	}
}
