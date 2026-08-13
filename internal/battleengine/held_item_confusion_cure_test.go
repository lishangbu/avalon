package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemCuresConfusion 验证混乱净化道具仅在成员成功获得混乱后立即消耗并清空持续回合。
// 它还覆盖其它易变状态边界，确保道具不会被解释为对束缚、挑衅或其它状态的通用净化器。
func TestHeldItemCuresConfusion(t *testing.T) {
	t.Parallel()
	t.Run("成功获得混乱时立即净化并消耗", func(t *testing.T) {
		actor := newMember(1, "confusion-cure-actor", 500, 500)
		actor.Stats.Speed = 200
		actor.Skills[0] = confusionApplicationSkill(battleengine.VolatileStatusConfusion)
		target := newMember(1, "confusion-cure-target", 500, 500)
		target.ItemID = testID("confusion-cure-item")
		target.HeldItemCuresConfusion = true
		target.Skills[0] = fieldSpeedOrderSkill(1, "混乱净化后行动", 0)

		result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, target), volatileTurn(1, 1, 1), mustRandom(t, 558))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
		if !found || member.ConfusionTurnsRemaining != 0 || member.ItemID != 0 || member.HeldItemCuresConfusion {
			t.Fatalf("混乱净化后的成员快照 = %+v", member)
		}
		assertConfusionCureEventOrder(t, result.Events, testID("confusion-cure-item"))
	})

	t.Run("其它易变状态不消耗道具", func(t *testing.T) {
		actor := newMember(1, "confusion-cure-boundary-actor", 500, 500)
		actor.Stats.Speed = 200
		actor.Skills[0] = confusionApplicationSkill(battleengine.VolatileStatusTaunt)
		target := newMember(1, "confusion-cure-boundary-target", 500, 500)
		target.ItemID = testID("confusion-cure-item")
		target.HeldItemCuresConfusion = true
		target.Skills[0] = fieldSpeedOrderSkill(1, "边界后行动", 0)

		result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, target), volatileTurn(1, 1, 1), mustRandom(t, 559))
		if err != nil {
			t.Fatalf("ResolveTurn() error = %v", err)
		}
		member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
		if !found || member.TauntTurnsRemaining == 0 || member.ItemID != testID("confusion-cure-item") || !member.HeldItemCuresConfusion {
			t.Fatalf("非混乱不应消耗净化道具 = %+v", member)
		}
		for _, event := range result.Events {
			if _, consumed := event.(battleengine.HeldItemConfusionCuredEvent); consumed {
				t.Fatalf("非混乱不应产生道具净化事件 = %+v", event)
			}
		}
	})
}

// confusionApplicationSkill 构造必定令单名对手获得指定易变状态的变化技能，用于测试道具的混乱专属语义。
func confusionApplicationSkill(status battleengine.VolatileStatus) battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("confusion-application-" + string(status)), Name: "易变状态施加", ElementID: testID("confusion-element"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10,
		VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
			Status: status, Target: battleengine.EffectTargetSelected, ChancePercent: 100, MinTurns: 2, MaxTurns: 2,
		}},
	}
}

// assertConfusionCureEventOrder 校验混乱写入后紧接着发生道具消耗，保证回放可观察完整生命周期。
func assertConfusionCureEventOrder(t *testing.T, events []battleengine.Event, itemID Identifier) {
	t.Helper()
	applicationIndex, cureIndex := -1, -1
	for index, event := range events {
		switch value := event.(type) {
		case battleengine.VolatileStatusAppliedEvent:
			if value.Status == battleengine.VolatileStatusConfusion {
				applicationIndex = index
			}
		case battleengine.HeldItemConfusionCuredEvent:
			if value.ItemID == itemID && value.Status == battleengine.VolatileStatusConfusion {
				cureIndex = index
			}
		}
	}
	if applicationIndex < 0 || cureIndex != applicationIndex+1 {
		t.Fatalf("混乱净化事件顺序 = %+v", events)
	}
}
