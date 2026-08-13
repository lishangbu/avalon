package battleengine_test

import (
	"errors"
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestNewStateRejectsInvalidFormulaAndAllyAbilityMultipliers 验证规则 122—131 的分类集合、能力项、分数和
// 互助组代码都由纯引擎入口独立校验，损坏冻结事实不能进入权威状态。
func TestNewStateRejectsInvalidFormulaAndAllyAbilityMultipliers(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name      string
		configure func(*battleengine.MemberSnapshot)
	}{
		{name: "分类减伤包含变化技能", configure: func(member *battleengine.MemberSnapshot) {
			member.DamageClassDamageReduction = &battleengine.DamageClassDamageReduction{DamageClasses: []battleengine.DamageClass{battleengine.DamageClassStatus}, Numerator: 1, Denominator: 2}
		}},
		{name: "属性减伤集合重复", configure: func(member *battleengine.MemberSnapshot) {
			member.ElementSkillDamageReduction = &battleengine.ElementSkillDamageReduction{ElementIDs: testIDs("fire", "fire"), Numerator: 1, Denominator: 2}
		}},
		{name: "接触减伤缺失分母", configure: func(member *battleengine.MemberSnapshot) {
			member.ContactBasedSkillDamageReduction = &battleengine.ContactBasedSkillDamageReduction{Numerator: 1}
		}},
		{name: "攻击能力倍率使用防御", configure: func(member *battleengine.MemberSnapshot) {
			member.AttackingStatMultiplier = &battleengine.AttackingStatMultiplier{Stat: battleengine.StatDefense, Numerator: 2, Denominator: 1}
		}},
		{name: "目标特性攻击倍率使用特防", configure: func(member *battleengine.MemberSnapshot) {
			member.OpponentAttackingStatMultiplier = &battleengine.OpponentAttackingStatMultiplier{Stat: battleengine.StatSpecialDefense, Numerator: 3, Denominator: 4}
		}},
		{name: "防守能力倍率使用攻击", configure: func(member *battleengine.MemberSnapshot) {
			member.DefendingStatMultiplier = &battleengine.DefendingStatMultiplier{Stat: battleengine.StatAttack, Numerator: 2, Denominator: 1}
		}},
		{name: "对手防守倍率缺失分子", configure: func(member *battleengine.MemberSnapshot) {
			member.OpponentDefendingStatMultiplier = &battleengine.OpponentDefendingStatMultiplier{Stat: battleengine.StatDefense, Denominator: 4}
		}},
		{name: "伙伴增伤分类为空", configure: func(member *battleengine.MemberSnapshot) {
			member.AllySkillDamageBoost = &battleengine.AllySkillDamageBoost{Numerator: 13, Denominator: 10}
		}},
		{name: "伙伴减伤分母为零", configure: func(member *battleengine.MemberSnapshot) {
			member.AllyReceivedDamageReduction = &battleengine.AllyReceivedDamageReduction{Numerator: 3}
		}},
		{name: "互助组攻击倍率代码为空", configure: func(member *battleengine.MemberSnapshot) {
			member.AllyAbilityPresenceAttackingStatMultiplier = &battleengine.AllyAbilityPresenceAttackingStatMultiplier{Stat: battleengine.StatSpecialAttack, Numerator: 3, Denominator: 2}
		}},
	}
	for _, test := range tests {
		test := test
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			actor := newMember(1, "invalid-formula-ability", 100, 100)
			target := newMember(1, "invalid-formula-target", 100, 100)
			test.configure(&actor)
			_, err := battleengine.NewState(battleengine.InitialState{
				Format: battleengine.FormatSnapshot{Code: "invalid-formula-ability", ActiveSlotsPerSide: 1, TeamSize: 1},
				Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
				Sides: []battleengine.SideSnapshot{
					{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{actor}},
					{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
				},
			})
			if !errors.Is(err, battleengine.ErrInvalidInitialState) {
				t.Fatalf("NewState() error = %v，期望 ErrInvalidInitialState", err)
			}
		})
	}
}

// TestResolveTurnDefensiveAbilityDamageMultipliers 验证规则 122—124 使用技能分类、有效属性和有效接触，
// 并在攻击方无视目标特性时统一保持中性。
func TestResolveTurnDefensiveAbilityDamageMultipliers(t *testing.T) {
	t.Parallel()
	type configure func(*battleengine.MemberSnapshot, *battleengine.MemberSnapshot, *battleengine.SkillSnapshot, *battleengine.RuleSnapshot, *battleengine.EnvironmentSnapshot)
	tests := []struct {
		name      string
		match     configure
		miss      configure
		wantMatch uint32
	}{
		{
			name: "122 物理伤害分类减伤",
			match: func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				target.DamageClassDamageReduction = &battleengine.DamageClassDamageReduction{DamageClasses: []battleengine.DamageClass{battleengine.DamageClassPhysical}, Numerator: 1, Denominator: 2}
			},
			miss: func(actor *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.IgnoreTargetAbilityEffects = true
				target.DamageClassDamageReduction = &battleengine.DamageClassDamageReduction{DamageClasses: []battleengine.DamageClass{battleengine.DamageClassPhysical}, Numerator: 1, Denominator: 2}
			},
			wantMatch: 18,
		},
		{
			name: "123 指定有效属性技能减伤",
			match: func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.ElementID = testID("fire")
				target.ElementSkillDamageReduction = &battleengine.ElementSkillDamageReduction{ElementIDs: testIDs("fire"), Numerator: 1, Denominator: 2}
			},
			miss: func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.ElementID = testID("water")
				target.ElementSkillDamageReduction = &battleengine.ElementSkillDamageReduction{ElementIDs: testIDs("fire"), Numerator: 1, Denominator: 2}
			},
			wantMatch: 18,
		},
		{
			name: "124 有效接触技能减伤",
			match: func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.MakesContact = true
				target.ContactBasedSkillDamageReduction = &battleengine.ContactBasedSkillDamageReduction{Numerator: 1, Denominator: 2}
			},
			miss: func(actor *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.MakesContact = true
				actor.ContactSuppression = true
				target.ContactBasedSkillDamageReduction = &battleengine.ContactBasedSkillDamageReduction{Numerator: 1, Denominator: 2}
			},
			wantMatch: 18,
		},
	}
	for _, test := range tests {
		test := test
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			plain := resolveAbilityDamageMultiplierTurn(t, nil)
			matched := resolveAbilityDamageMultiplierTurn(t, test.match)
			missed := resolveAbilityDamageMultiplierTurn(t, test.miss)
			if plain.damage != 37 || matched.damage != test.wantMatch || missed.damage != 37 || !reflect.DeepEqual(matched.trace, matched.script) {
				t.Fatalf("伤害 = plain:%d matched:%d missed:%d trace=%+v", plain.damage, matched.damage, missed.damage, matched.trace)
			}
		})
	}
}

// TestResolveTurnAbilityStatMultipliers 验证规则 125—128 在能力阶级后的正确公式位置生效，并覆盖环境条件、
// 防守特性穿透和物理灼伤绕过边界。
func TestResolveTurnAbilityStatMultipliers(t *testing.T) {
	t.Parallel()
	plain := resolveAbilityDamageMultiplierTurn(t, nil)
	attacking := resolveAbilityDamageMultiplierTurn(t, func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
		actor.AttackingStatMultiplier = &battleengine.AttackingStatMultiplier{Stat: battleengine.StatAttack, Numerator: 2, Denominator: 1}
	})
	attackingMiss := resolveAbilityDamageMultiplierTurn(t, func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
		actor.AttackingStatMultiplier = &battleengine.AttackingStatMultiplier{Stat: battleengine.StatAttack, Numerator: 2, Denominator: 1, RequiredWeather: battleengine.WeatherKindSun}
	})
	opponentAttacking := resolveAbilityDamageMultiplierTurn(t, func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
		target.OpponentAttackingStatMultiplier = &battleengine.OpponentAttackingStatMultiplier{Stat: battleengine.StatAttack, Numerator: 3, Denominator: 4}
	})
	opponentAttackingIgnored := resolveAbilityDamageMultiplierTurn(t, func(actor, target *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
		actor.IgnoreTargetAbilityEffects = true
		target.OpponentAttackingStatMultiplier = &battleengine.OpponentAttackingStatMultiplier{Stat: battleengine.StatAttack, Numerator: 3, Denominator: 4}
	})
	defending := resolveAbilityDamageMultiplierTurn(t, func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
		target.DefendingStatMultiplier = &battleengine.DefendingStatMultiplier{Stat: battleengine.StatDefense, Numerator: 2, Denominator: 1}
	})
	opponentDefending := resolveAbilityDamageMultiplierTurn(t, func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
		actor.OpponentDefendingStatMultiplier = &battleengine.OpponentDefendingStatMultiplier{Stat: battleengine.StatDefense, Numerator: 3, Denominator: 4}
	})
	if attacking.damage <= plain.damage || attackingMiss.damage != plain.damage || opponentAttacking.damage >= plain.damage ||
		opponentAttackingIgnored.damage != plain.damage || defending.damage >= plain.damage || opponentDefending.damage <= plain.damage {
		t.Fatalf("能力倍率伤害 plain=%d ownAttack=%d ownMiss=%d targetAttack=%d ignored=%d ownDefense=%d targetDefense=%d",
			plain.damage, attacking.damage, attackingMiss.damage, opponentAttacking.damage, opponentAttackingIgnored.damage, defending.damage, opponentDefending.damage)
	}
	for _, result := range []abilityDamageMultiplierResult{attacking, opponentAttacking, defending, opponentDefending} {
		if !reflect.DeepEqual(result.trace, result.script) || !eventOccursBefore(result.events, battleengine.EventKindDamageApplied, battleengine.EventKindTurnEnded) {
			t.Fatalf("能力倍率事件或轨迹错误: events=%v trace=%+v", eventKinds(result.events), result.trace)
		}
	}
}

// TestResolveTurnAttackingStatMultiplierCanIgnoreBurnReduction 验证灼伤绕过只在同一条匹配攻击倍率规则激活时生效。
func TestResolveTurnAttackingStatMultiplierCanIgnoreBurnReduction(t *testing.T) {
	t.Parallel()
	withoutOverride := resolveAbilityDamageMultiplierTurn(t, func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
		actor.MajorStatus = battleengine.MajorStatusBurn
		actor.AttackingStatMultiplier = &battleengine.AttackingStatMultiplier{Stat: battleengine.StatAttack, Numerator: 2, Denominator: 1}
	})
	withOverride := resolveAbilityDamageMultiplierTurn(t, func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
		actor.MajorStatus = battleengine.MajorStatusBurn
		actor.AttackingStatMultiplier = &battleengine.AttackingStatMultiplier{Stat: battleengine.StatAttack, Numerator: 2, Denominator: 1, IgnoreBurnAttackReduction: true}
	})
	if withOverride.damage <= withoutOverride.damage || !reflect.DeepEqual(withOverride.trace, withOverride.script) {
		t.Fatalf("灼伤能力倍率 damage=%d override=%d trace=%+v", withoutOverride.damage, withOverride.damage, withOverride.trace)
	}
}

// TestResolveTurnAllyAbilityMultipliers 验证规则 129—131 只读取同侧其它存活上场成员，按伤害分类和互助组
// 精确匹配，并在伤害事件之前完成公式修正而不改变随机轨迹。
func TestResolveTurnAllyAbilityMultipliers(t *testing.T) {
	t.Parallel()
	neutral := resolveAllyAbilityMultiplierTurn(t, nil)
	allyDamageBoost := resolveAllyAbilityMultiplierTurn(t, func(_actor, ally, _target, _targetAlly *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot) {
		ally.AllySkillDamageBoost = &battleengine.AllySkillDamageBoost{DamageClasses: []battleengine.DamageClass{battleengine.DamageClassPhysical}, Numerator: 13, Denominator: 10}
	})
	allyDamageBoostMiss := resolveAllyAbilityMultiplierTurn(t, func(_actor, ally, _target, _targetAlly *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot) {
		ally.AllySkillDamageBoost = &battleengine.AllySkillDamageBoost{DamageClasses: []battleengine.DamageClass{battleengine.DamageClassSpecial}, Numerator: 13, Denominator: 10}
	})
	allyReduction := resolveAllyAbilityMultiplierTurn(t, func(_actor, _ally, _target, targetAlly *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot) {
		targetAlly.AllyReceivedDamageReduction = &battleengine.AllyReceivedDamageReduction{Numerator: 3, Denominator: 4}
	})
	groupBoost := resolveAllyAbilityMultiplierTurn(t, func(actor, ally, _target, _targetAlly *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot) {
		actor.AllyAbilityPresenceAttackingStatMultiplier = &battleengine.AllyAbilityPresenceAttackingStatMultiplier{GroupCode: "plus-minus", Stat: battleengine.StatAttack, Numerator: 3, Denominator: 2}
		ally.AllyAbilityGroupCode = "plus-minus"
	})
	groupMiss := resolveAllyAbilityMultiplierTurn(t, func(actor, ally, _target, _targetAlly *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot) {
		actor.AllyAbilityPresenceAttackingStatMultiplier = &battleengine.AllyAbilityPresenceAttackingStatMultiplier{GroupCode: "plus-minus", Stat: battleengine.StatAttack, Numerator: 3, Denominator: 2}
		ally.AllyAbilityGroupCode = "other-group"
	})
	if allyDamageBoost.damage <= neutral.damage || allyDamageBoostMiss.damage != neutral.damage || allyReduction.damage >= neutral.damage ||
		groupBoost.damage <= neutral.damage || groupMiss.damage != neutral.damage {
		t.Fatalf("伙伴倍率 damage neutral=%d boost=%d classMiss=%d reduction=%d group=%d groupMiss=%d",
			neutral.damage, allyDamageBoost.damage, allyDamageBoostMiss.damage, allyReduction.damage, groupBoost.damage, groupMiss.damage)
	}
	for _, result := range []allyAbilityMultiplierResult{allyDamageBoost, allyReduction, groupBoost} {
		if !reflect.DeepEqual(result.trace, result.script) || !eventOccursBefore(result.events, battleengine.EventKindDamageApplied, battleengine.EventKindTurnEnded) {
			t.Fatalf("伙伴倍率事件或轨迹错误: events=%v trace=%+v", eventKinds(result.events), result.trace)
		}
	}
}

// allyAbilityMultiplierResult 保存一次双打伙伴规则的伤害、事件与完整随机轨迹。
type allyAbilityMultiplierResult struct {
	damage uint32
	events []battleengine.Event
	trace  []battleengine.RandomTraceEntry
	script []battleengine.RandomTraceEntry
}

// resolveAllyAbilityMultiplierTurn 构造四名成员均需提交命令的双打回合，并返回第一槽成员对目标第一槽的伤害。
func resolveAllyAbilityMultiplierTurn(
	t *testing.T,
	configure func(*battleengine.MemberSnapshot, *battleengine.MemberSnapshot, *battleengine.MemberSnapshot, *battleengine.MemberSnapshot, *battleengine.SkillSnapshot),
) allyAbilityMultiplierResult {
	t.Helper()
	actor := newMember(1, "ally-aura-actor", 500, 500)
	actor.Stats.Speed = 200
	actor.ElementIDs = testIDs("neutral")
	actor.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("ally-aura-skill"), Name: "伙伴光环攻击", ElementID: testID("neutral-skill"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
	}
	actorAlly := sleepingStrongWeatherSource("ally-aura-actor-ally", "")
	actorAlly.Position = 2
	actorAlly.Stats.Speed = 150
	target := sleepingStrongWeatherSource("ally-aura-target", "")
	target.Position = 1
	target.Stats.Speed = 100
	target.MaxHP, target.CurrentHP = 1_000, 1_000
	target.ElementIDs = testIDs("neutral-target")
	targetAlly := sleepingStrongWeatherSource("ally-aura-target-ally", "")
	targetAlly.Position = 2
	targetAlly.Stats.Speed = 50
	if configure != nil {
		configure(&actor, &actorAlly, &target, &targetAlly, &actor.Skills[0])
	}
	script := []battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("ally-aura-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("ally-aura-skill").String(), Value: 15},
	}
	random, err := battleengine.NewTracedRandom(script)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "ally-aura-double", ActiveSlotsPerSide: 2, TeamSize: 2},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{actor, actorAlly}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1, 2}, Members: []battleengine.MemberSnapshot{target, targetAlly}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	command := battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: []battleengine.Action{
		strongWeatherUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		strongWeatherUseSkillAction(battleengine.SideOne, 2, battleengine.SideTwo, 1),
		strongWeatherUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
		strongWeatherUseSkillAction(battleengine.SideTwo, 2, battleengine.SideOne, 1),
	}}
	result, err := battleengine.ResolveTurn(state, command, random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	damage := uint32(0)
	for _, event := range result.Events {
		if applied, ok := event.(battleengine.DamageAppliedEvent); ok && applied.Actor == (battleengine.MemberRef{Side: battleengine.SideOne, Position: 1}) {
			damage = applied.Amount
			break
		}
	}
	return allyAbilityMultiplierResult{damage: damage, events: result.Events, trace: result.RandomTrace, script: script}
}
