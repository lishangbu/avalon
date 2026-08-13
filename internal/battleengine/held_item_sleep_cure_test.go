package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemCuresSleep 验证睡眠净化道具只在成员成功进入睡眠后消耗，并清除睡眠附属回合计数。
// 它固定异常写入、睡眠随机时长和道具净化事件的顺序，防止睡眠已经解除后仍留下休眠行动限制。
func TestHeldItemCuresSleep(t *testing.T) {
	t.Parallel()
	actor := newMember(1, "sleep-cure-actor", 500, 500)
	actor.Stats.Speed = 200
	actor.Skills[0] = sleepApplicationSkill()
	target := newMember(1, "sleep-cure-target", 500, 500)
	target.ItemID = testID("sleep-cure-item")
	target.HeldItemCuresSleep = true
	target.Skills[0] = fieldSpeedOrderSkill(1, "睡眠净化后行动", 0)

	result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, target), volatileTurn(1, 1, 1), mustRandom(t, 546))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || member.MajorStatus != "" || member.SleepTurnsRemaining != 0 || member.ItemID != 0 || member.HeldItemCuresSleep {
		t.Fatalf("睡眠净化后的成员快照 = %+v", member)
	}
	assertSleepCureEventOrder(t, result.Events, testID("sleep-cure-item"))
	if len(result.RandomTrace) == 0 || result.RandomTrace[0].Reason != "sleep duration for "+testID("sleep-application-skill").String() {
		t.Fatalf("睡眠时长随机轨迹 = %+v", result.RandomTrace)
	}
}

// sleepApplicationSkill 构造必定令单名对手睡眠的变化技能，用于验证一次性睡眠净化道具的触发时机。
func sleepApplicationSkill() battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("sleep-application-skill"), Name: "睡眠施加", ElementID: testID("sleep-element"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10,
		StatusApplications: []battleengine.MajorStatusApplication{{
			Status: battleengine.MajorStatusSleep, Target: battleengine.EffectTargetSelected, ChancePercent: 100,
		}},
	}
}

// assertSleepCureEventOrder 校验睡眠的实际写入后紧接着发生道具消耗和净化。
func assertSleepCureEventOrder(t *testing.T, events []battleengine.Event, itemID Identifier) {
	t.Helper()
	applicationIndex, cureIndex := -1, -1
	for index, event := range events {
		switch value := event.(type) {
		case battleengine.MajorStatusAppliedEvent:
			if value.Status == battleengine.MajorStatusSleep {
				applicationIndex = index
			}
		case battleengine.HeldItemSleepCuredEvent:
			if value.ItemID == itemID && value.Status == battleengine.MajorStatusSleep {
				cureIndex = index
			}
		}
	}
	if applicationIndex < 0 || cureIndex != applicationIndex+1 {
		t.Fatalf("睡眠净化事件顺序 = %+v", events)
	}
}
