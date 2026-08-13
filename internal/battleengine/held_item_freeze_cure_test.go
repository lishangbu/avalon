package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemCuresFreeze 验证冰冻净化道具只会在持有者成功获得冰冻后立即消耗。
// 测试覆盖状态写入与道具消耗的事件顺序，以及其它主要异常不能错误消耗该道具的边界。
func TestHeldItemCuresFreeze(t *testing.T) {
	t.Parallel()
	t.Run("成功获得冰冻时立即净化并消耗", func(t *testing.T) {
		actor := newMember(1, "freeze-cure-actor", 500, 500)
		actor.Stats.Speed = 200
		actor.Skills[0] = freezeApplicationSkill()
		target := newMember(1, "freeze-cure-target", 500, 500)
		target.ItemID = testID("freeze-cure-item")
		target.HeldItemCuresFreeze = true
		target.Skills[0] = fieldSpeedOrderSkill(1, "冰冻净化后行动", 0)

		result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, target), volatileTurn(1, 1, 1), mustRandom(t, 550))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
		if !found || member.MajorStatus != "" || member.ItemID != 0 || member.HeldItemCuresFreeze {
			t.Fatalf("冰冻净化后的成员快照 = %+v", member)
		}
		assertFreezeCureEventOrder(t, result.Events, testID("freeze-cure-item"))
	})

	t.Run("其它主要异常不消耗道具", func(t *testing.T) {
		actor := newMember(1, "freeze-cure-boundary-actor", 500, 500)
		actor.Stats.Speed = 200
		actor.Skills[0] = freezeApplicationSkill()
		actor.Skills[0].StatusApplications[0].Status = battleengine.MajorStatusBurn
		target := newMember(1, "freeze-cure-boundary-target", 500, 500)
		target.ItemID = testID("freeze-cure-item")
		target.HeldItemCuresFreeze = true
		target.Skills[0] = fieldSpeedOrderSkill(1, "边界后行动", 0)

		result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, target), volatileTurn(1, 1, 1), mustRandom(t, 551))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
		if !found || member.MajorStatus != battleengine.MajorStatusBurn || member.ItemID != testID("freeze-cure-item") || !member.HeldItemCuresFreeze {
			t.Fatalf("非冰冻不应消耗净化道具 = %+v", member)
		}
		for _, event := range result.Events {
			if _, consumed := event.(battleengine.HeldItemFreezeCuredEvent); consumed {
				t.Fatalf("非冰冻不应产生道具净化事件 = %+v", event)
			}
		}
	})
}

// freezeApplicationSkill 构造必定令单名对手冰冻的变化技能，用于验证一次性冰冻净化道具的触发窗口。
func freezeApplicationSkill() battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("freeze-application-skill"), Name: "冰冻施加", ElementID: testID("freeze-element"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10,
		StatusApplications: []battleengine.MajorStatusApplication{{
			Status: battleengine.MajorStatusFreeze, Target: battleengine.EffectTargetSelected, ChancePercent: 100,
		}},
	}
}

// assertFreezeCureEventOrder 校验冰冻实际写入后才由道具解除，保证回放可观察到完整生命周期。
func assertFreezeCureEventOrder(t *testing.T, events []battleengine.Event, itemID Identifier) {
	t.Helper()
	applicationIndex, cureIndex := -1, -1
	for index, event := range events {
		switch value := event.(type) {
		case battleengine.MajorStatusAppliedEvent:
			if value.Status == battleengine.MajorStatusFreeze {
				applicationIndex = index
			}
		case battleengine.HeldItemFreezeCuredEvent:
			if value.ItemID == itemID && value.Status == battleengine.MajorStatusFreeze {
				cureIndex = index
			}
		}
	}
	if applicationIndex < 0 || cureIndex != applicationIndex+1 {
		t.Fatalf("冰冻净化事件顺序 = %+v", events)
	}
}
