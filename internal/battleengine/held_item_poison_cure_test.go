package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemCuresPoison 验证中毒净化道具仅在成员获得普通中毒或剧毒后立即消耗。
// 它分别覆盖两种资料声明的中毒状态，确保剧毒计数不会在状态清除后遗留到后续回合。
func TestHeldItemCuresPoison(t *testing.T) {
	t.Parallel()
	for _, status := range []battleengine.MajorStatus{battleengine.MajorStatusPoison, battleengine.MajorStatusBadPoison} {
		t.Run(string(status), func(t *testing.T) {
			actor := newMember(1, "poison-cure-actor-"+string(status), 500, 500)
			actor.Stats.Speed = 200
			actor.Skills[0] = poisonApplicationSkill(status)
			target := newMember(1, "poison-cure-target-"+string(status), 500, 500)
			target.ItemID = testID("poison-cure-item")
			target.HeldItemCuresPoison = true
			target.Skills[0] = fieldSpeedOrderSkill(1, "中毒净化后行动", 0)

			result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, target), volatileTurn(1, 1, 1), mustRandom(t, 547))
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
			if !found || member.MajorStatus != "" || member.BadPoisonCounter != 0 || member.ItemID != 0 || member.HeldItemCuresPoison {
				t.Fatalf("中毒净化后的成员快照 = %+v", member)
			}
			assertPoisonCureEventOrder(t, result.Events, status, testID("poison-cure-item"))
		})
	}
}

// poisonApplicationSkill 构造必定令单名对手进入指定中毒状态的变化技能。
func poisonApplicationSkill(status battleengine.MajorStatus) battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("poison-application-" + string(status)), Name: "中毒施加", ElementID: testID("poison-element"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10,
		StatusApplications: []battleengine.MajorStatusApplication{{
			Status: status, Target: battleengine.EffectTargetSelected, ChancePercent: 100,
		}},
	}
}

// assertPoisonCureEventOrder 校验普通中毒或剧毒先被写入，再由持有道具立即解除。
func assertPoisonCureEventOrder(t *testing.T, events []battleengine.Event, status battleengine.MajorStatus, itemID Identifier) {
	t.Helper()
	applicationIndex, cureIndex := -1, -1
	for index, event := range events {
		switch value := event.(type) {
		case battleengine.MajorStatusAppliedEvent:
			if value.Status == status {
				applicationIndex = index
			}
		case battleengine.HeldItemPoisonCuredEvent:
			if value.ItemID == itemID && value.Status == status {
				cureIndex = index
			}
		}
	}
	if applicationIndex < 0 || cureIndex != applicationIndex+1 {
		t.Fatalf("中毒净化事件顺序 = %+v", events)
	}
}
