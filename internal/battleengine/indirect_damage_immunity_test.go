package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnIndirectDamageImmunityBlocksEndTurnSources 验证间接伤害免疫会同时阻止已有的天气、主要异常、束缚和寄生种子扣血。
// 状态本身仍会被写入并保持，避免将“免疫伤害”误实现为“免疫异常或清除持续效果”。
func TestResolveTurnIndirectDamageImmunityBlocksEndTurnSources(t *testing.T) {
	t.Parallel()

	source := newMember(1, "indirect-immune-source", 160, 160)
	source.Stats.Speed = 200
	source.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("indirect-immune-applicator"), Name: "持续效果", ElementID: testID("normal"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Accuracy: 100, RemainingPP: 10, MaxPP: 10,
		StatusApplications: []battleengine.MajorStatusApplication{{
			Status: battleengine.MajorStatusBurn, Target: battleengine.EffectTargetSelected, ChancePercent: 100,
		}},
		VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
			Status: battleengine.VolatileStatusBinding, Target: battleengine.EffectTargetSelected,
			ChancePercent: 100, MinTurns: 2, MaxTurns: 2,
		}},
		LeechSeedApplication: &battleengine.LeechSeedApplication{ChancePercent: 100},
	}
	immune := newMember(1, "indirect-immune-target", 160, 160)
	immune.Stats.Speed = 10
	immune.IndirectDamageImmunity = true
	immune.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("indirect-immune-wait"), Name: "等待", ElementID: testID("normal"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		// 回复百分比使等待动作属于可执行的自身状态技能；满血时不会额外改变断言关注的状态。
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, HealingPercent: 1,
	}
	state := newWeatherState(t,
		battleengine.EnvironmentSnapshot{Weather: &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 2}},
		battleengine.RuleSnapshot{SchemaVersion: 1}, source, immune,
	)

	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			volatileSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
			volatileSkillAction(battleengine.SideTwo, 1, battleengine.SideTwo, 1),
		},
	}, mustRandom(t, 611))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}

	target, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || target.CurrentHP != target.MaxHP || target.MajorStatus != battleengine.MajorStatusBurn ||
		target.BindingTurnsRemaining != 2 || target.LeechSeedSourceSlot == nil {
		t.Fatalf("间接伤害免疫后的目标状态 = %+v, found=%t", target, found)
	}
	if indirectDamageEventTargetsMember(result.Events, battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) {
		t.Fatalf("间接伤害免疫目标不应产生间接伤害事件: %+v", result.Events)
	}
}

// TestResolveTurnIndirectDamageImmunityBlocksEntryHazards 验证间接伤害免疫仅跳过隐形岩和撒菱的生命扣除，
// 不阻止成员实际换入，也不影响危害层继续留在己方场地。
func TestResolveTurnIndirectDamageImmunityBlocksEntryHazards(t *testing.T) {
	t.Parallel()

	attacker := newMember(1, "entry-hazard-observer", 160, 160)
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("entry-hazard-wait"), Name: "等待", ElementID: testID("normal"),
		DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
		// 使用无副作用的最小回复保证该回合行动可被命令层接受。
		Accuracy: 100, RemainingPP: 10, MaxPP: 10, HealingPercent: 1,
	}
	active := newMember(1, "entry-hazard-active", 160, 160)
	reserve := newMember(2, "entry-hazard-immune", 160, 160)
	reserve.IndirectDamageImmunity = true
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "indirect-entry-hazard", ActiveSlotsPerSide: 1, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{"rock": testID("rock")}},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{attacker}},
			{
				Side:          battleengine.SideTwo,
				ActiveMembers: []battleengine.MemberPosition{1},
				Members:       []battleengine.MemberSnapshot{active, reserve},
				Conditions: battleengine.SideConditionSnapshot{
					StealthRock:  true,
					SpikesLayers: 1,
				},
			},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}

	result, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{
		SchemaVersion: 1,
		TurnNumber:    1,
		Actions: []battleengine.Action{
			volatileSkillAction(battleengine.SideOne, 1, battleengine.SideOne, 1),
			{Kind: battleengine.ActionKindSwitch, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}, Switch: &battleengine.SwitchAction{MemberPosition: 2}},
		},
	}, mustRandom(t, 612))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}

	entered, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || entered.Position != 2 || entered.CurrentHP != entered.MaxHP {
		t.Fatalf("免疫成员换入后的状态 = %+v, found=%t", entered, found)
	}
	if indirectDamageEventTargetsMember(result.Events, battleengine.MemberRef{Side: battleengine.SideTwo, Position: 2}) {
		t.Fatalf("入场危害不应伤害具备间接伤害免疫的成员: %+v", result.Events)
	}
}

// TestResolveTurnIndirectDamageImmunityKeepsConfusionPrevention 验证混乱的行动阻止和随机数消费保持不变，
// 仅取消原本会紧随其后的混乱自伤。
func TestResolveTurnIndirectDamageImmunityKeepsConfusionPrevention(t *testing.T) {
	t.Parallel()

	inflictor := newMember(1, "indirect-confusion-inflictor", 400, 400)
	inflictor.Stats.Speed = 200
	inflictor.Skills[0].DamageClass = battleengine.DamageClassStatus
	inflictor.Skills[0].Power = 0
	inflictor.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
		Status: battleengine.VolatileStatusConfusion, Target: battleengine.EffectTargetSelected,
		ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
	}}
	immune := newMember(1, "indirect-confusion-immune", 400, 400)
	immune.Stats.Speed = 10
	immune.IndirectDamageImmunity = true
	immune.Skills[0].DamageClass = battleengine.DamageClassStatus
	immune.Skills[0].Power = 0
	// 免疫成员仍需提交一个可执行动作；满血时该回复不会影响混乱自伤断言。
	immune.Skills[0].HealingPercent = 1
	random, err := battleengine.NewTracedRandom([]battleengine.RandomTraceEntry{{
		Sequence: 1, Bound: 3, Reason: "confusion chance for side 2 member 1", Value: 0,
	}})
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}

	result, err := battleengine.ResolveTurn(volatileState(t, inflictor, immune), volatileTurn(1, 1, 1), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}

	target, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || target.CurrentHP != target.MaxHP || target.ConfusionTurnsRemaining != 0 || target.Skills[0].RemainingPP != target.Skills[0].MaxPP {
		t.Fatalf("间接伤害免疫下的混乱目标状态 = %+v, found=%t", target, found)
	}
	if !volatilePreventionExists(result.Events, battleengine.SideTwo, battleengine.SkillPreventionReasonConfusion) ||
		indirectDamageEventTargetsMember(result.Events, battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1}) {
		t.Fatalf("混乱免疫事件边界不正确: %+v", result.Events)
	}
}

// indirectDamageEventTargetsMember 报告事件序列中是否含有目标成员的现有间接伤害写入。
// 它刻意只枚举本规则已接入的伤害事件，避免把技能本体伤害或技能反作用误归类为间接伤害。
func indirectDamageEventTargetsMember(events []battleengine.Event, target battleengine.MemberRef) bool {
	for _, event := range events {
		switch value := event.(type) {
		case battleengine.StealthRockDamageAppliedEvent:
			if value.Target == target {
				return true
			}
		case battleengine.SpikesDamageAppliedEvent:
			if value.Target == target {
				return true
			}
		case battleengine.MajorStatusDamageAppliedEvent:
			if value.Target == target {
				return true
			}
		case battleengine.VolatileStatusDamageAppliedEvent:
			if value.Target == target {
				return true
			}
		case battleengine.LeechSeedDamageAppliedEvent:
			if value.Target == target {
				return true
			}
		case battleengine.WeatherDamageAppliedEvent:
			if value.Target == target {
				return true
			}
		}
	}
	return false
}
