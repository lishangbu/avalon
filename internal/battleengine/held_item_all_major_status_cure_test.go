package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestHeldItemCuresAllMajorStatuses 验证全主要异常净化道具对六种资料声明的主要异常分别立即消耗。
// 每个子用例同时断言异常状态快照、睡眠与剧毒附属计数、道具投影和相邻事件顺序，避免泛化净化遗漏状态残留。
func TestHeldItemCuresAllMajorStatuses(t *testing.T) {
	t.Parallel()
	statuses := []battleengine.MajorStatus{
		battleengine.MajorStatusBurn,
		battleengine.MajorStatusParalysis,
		battleengine.MajorStatusPoison,
		battleengine.MajorStatusBadPoison,
		battleengine.MajorStatusSleep,
		battleengine.MajorStatusFreeze,
	}
	for index, status := range statuses {
		t.Run(string(status), func(t *testing.T) {
			actor := newMember(1, "all-major-status-cure-actor-"+string(status), 500, 500)
			actor.Stats.Speed = 200
			actor.Skills[0] = allMajorStatusApplicationSkill(status)
			target := newMember(1, "all-major-status-cure-target-"+string(status), 500, 500)
			target.ItemID = testID("all-major-status-cure-item")
			target.HeldItemCuresAllMajorStatuses = true
			target.Skills[0] = fieldSpeedOrderSkill(1, "全主要异常净化后行动", 0)

			result, err := battleengine.ResolveTurn(newWeatherState(t, battleengine.EnvironmentSnapshot{}, battleengine.RuleSnapshot{SchemaVersion: 1}, actor, target), volatileTurn(1, 1, 1), mustRandom(t, 552+uint64(index)))
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			member, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
			if !found || member.MajorStatus != "" || member.SleepTurnsRemaining != 0 || member.BadPoisonCounter != 0 || member.ItemID != 0 || member.HeldItemCuresAllMajorStatuses {
				t.Fatalf("全主要异常净化后的成员快照 = %+v", member)
			}
			assertAllMajorStatusCureEventOrder(t, result.Events, status, testID("all-major-status-cure-item"))
		})
	}
}

// allMajorStatusApplicationSkill 构造必定令单名对手获得指定主要异常的变化技能，用于验证全范围净化道具。
func allMajorStatusApplicationSkill(status battleengine.MajorStatus) battleengine.SkillSnapshot {
	return battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("all-major-status-application-" + string(status)), Name: "全主要异常施加", ElementID: testID("all-major-status-element"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10,
		StatusApplications: []battleengine.MajorStatusApplication{{
			Status: status, Target: battleengine.EffectTargetSelected, ChancePercent: 100,
		}},
	}
}

// assertAllMajorStatusCureEventOrder 校验任一种可治疗主要异常写入后都紧接着发生全范围道具消耗。
func assertAllMajorStatusCureEventOrder(t *testing.T, events []battleengine.Event, status battleengine.MajorStatus, itemID Identifier) {
	t.Helper()
	applicationIndex, cureIndex := -1, -1
	for index, event := range events {
		switch value := event.(type) {
		case battleengine.MajorStatusAppliedEvent:
			if value.Status == status {
				applicationIndex = index
			}
		case battleengine.HeldItemAllMajorStatusCuredEvent:
			if value.ItemID == itemID && value.Status == status {
				cureIndex = index
			}
		}
	}
	if applicationIndex < 0 || cureIndex != applicationIndex+1 {
		t.Fatalf("全主要异常净化事件顺序 = %+v", events)
	}
}
