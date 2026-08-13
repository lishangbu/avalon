package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnHeldItemExtendsSideDamageReductionDuration 验证持有道具只会延长自己成功建立的匹配减伤屏障。
// 延长值是在屏障写入时与技能声明持续回合取较大值；已存在屏障不会被刷新，且回合末仍按统一生命周期递减。
func TestResolveTurnHeldItemExtendsSideDamageReductionDuration(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name           string
		baseTurns      uint8
		configureItem  func(*battleengine.MemberSnapshot)
		configureSkill func(*battleengine.SkillSnapshot, uint8)
		duration       func(battleengine.SideConditionSnapshot) uint8
		eventDuration  func([]battleengine.Event) (uint8, bool)
		wantAfterTurn  uint8
		wantEvent      uint8
	}{
		{
			name: "反射壁延长至道具声明回合", baseTurns: 5,
			configureItem: func(member *battleengine.MemberSnapshot) { member.HeldItemReflectTurnsRemaining = 8 },
			configureSkill: func(skill *battleengine.SkillSnapshot, turns uint8) {
				skill.ReflectApplication = &battleengine.ReflectApplication{Effect: battleengine.ReflectEffect{TurnsRemaining: turns}, ChancePercent: 100}
			},
			duration: func(conditions battleengine.SideConditionSnapshot) uint8 {
				if conditions.Reflect == nil {
					return 0
				}
				return conditions.Reflect.TurnsRemaining
			},
			eventDuration: reflectStartedDuration,
			wantAfterTurn: 7, wantEvent: 8,
		},
		{
			name: "光墙延长至道具声明回合", baseTurns: 5,
			configureItem: func(member *battleengine.MemberSnapshot) { member.HeldItemLightScreenTurnsRemaining = 8 },
			configureSkill: func(skill *battleengine.SkillSnapshot, turns uint8) {
				skill.LightScreenApplication = &battleengine.LightScreenApplication{Effect: battleengine.LightScreenEffect{TurnsRemaining: turns}, ChancePercent: 100}
			},
			duration: func(conditions battleengine.SideConditionSnapshot) uint8 {
				if conditions.LightScreen == nil {
					return 0
				}
				return conditions.LightScreen.TurnsRemaining
			},
			eventDuration: lightScreenStartedDuration,
			wantAfterTurn: 7, wantEvent: 8,
		},
		{
			name: "极光幕延长至道具声明回合", baseTurns: 5,
			configureItem: func(member *battleengine.MemberSnapshot) { member.HeldItemAuroraVeilTurnsRemaining = 8 },
			configureSkill: func(skill *battleengine.SkillSnapshot, turns uint8) {
				skill.AuroraVeilApplication = &battleengine.AuroraVeilApplication{Effect: battleengine.AuroraVeilEffect{TurnsRemaining: turns}, ChancePercent: 100}
			},
			duration: func(conditions battleengine.SideConditionSnapshot) uint8 {
				if conditions.AuroraVeil == nil {
					return 0
				}
				return conditions.AuroraVeil.TurnsRemaining
			},
			eventDuration: auroraVeilStartedDuration,
			wantAfterTurn: 7, wantEvent: 8,
		},
		{
			name: "较长的技能持续回合不被道具缩短", baseTurns: 9,
			configureItem: func(member *battleengine.MemberSnapshot) { member.HeldItemReflectTurnsRemaining = 8 },
			configureSkill: func(skill *battleengine.SkillSnapshot, turns uint8) {
				skill.ReflectApplication = &battleengine.ReflectApplication{Effect: battleengine.ReflectEffect{TurnsRemaining: turns}, ChancePercent: 100}
			},
			duration: func(conditions battleengine.SideConditionSnapshot) uint8 {
				if conditions.Reflect == nil {
					return 0
				}
				return conditions.Reflect.TurnsRemaining
			},
			eventDuration: reflectStartedDuration,
			wantAfterTurn: 8, wantEvent: 9,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			caster := newMember(1, "screen-duration-holder", 500, 500)
			caster.Stats.Speed = 200
			caster.ItemID = testID("screen-duration-item")
			test.configureItem(&caster)
			caster.Skills[0].DamageClass = battleengine.DamageClassStatus
			caster.Skills[0].Power = 0
			caster.Skills[0].TargetScope = battleengine.SkillTargetScopeSelf
			test.configureSkill(&caster.Skills[0], test.baseTurns)
			target := newMember(1, "screen-duration-observer", 500, 500)
			target.Stats.Speed = 10

			resolved, err := battleengine.ResolveTurn(volatileState(t, caster, target), volatileTurn(1, 1, 1), mustRandom(t, 501))
			if err != nil {
				t.Fatalf("ResolveTurn() error = %v", err)
			}
			conditions := resolved.State.Snapshot().Sides[0].Conditions
			if actual := test.duration(conditions); actual != test.wantAfterTurn {
				t.Fatalf("屏障回合数 = %d，期望 %d，状态 = %+v", actual, test.wantAfterTurn, conditions)
			}
			if duration, found := test.eventDuration(resolved.Events); !found || duration != test.wantEvent {
				t.Fatalf("屏障开始事件回合数 = %d，存在 = %t，期望 %d，事件 = %+v", duration, found, test.wantEvent, resolved.Events)
			}
		})
	}
}

// reflectStartedDuration 从事件流读取本回合反射壁开始事件的初始持续回合。
func reflectStartedDuration(events []battleengine.Event) (uint8, bool) {
	for _, event := range events {
		if value, ok := event.(battleengine.ReflectStartedEvent); ok {
			return value.TurnsRemaining, true
		}
	}
	return 0, false
}

// lightScreenStartedDuration 从事件流读取本回合光墙开始事件的初始持续回合。
func lightScreenStartedDuration(events []battleengine.Event) (uint8, bool) {
	for _, event := range events {
		if value, ok := event.(battleengine.LightScreenStartedEvent); ok {
			return value.TurnsRemaining, true
		}
	}
	return 0, false
}

// auroraVeilStartedDuration 从事件流读取本回合极光幕开始事件的初始持续回合。
func auroraVeilStartedDuration(events []battleengine.Event) (uint8, bool) {
	for _, event := range events {
		if value, ok := event.(battleengine.AuroraVeilStartedEvent); ok {
			return value.TurnsRemaining, true
		}
	}
	return 0, false
}
