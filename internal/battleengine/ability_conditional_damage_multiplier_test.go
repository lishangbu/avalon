package battleengine_test

import (
	"errors"
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestNewStateRejectsInvalidConditionalAbilityDamageMultipliers 验证规则 112—121 绕过资料层后仍会被纯引擎拒绝。
func TestNewStateRejectsInvalidConditionalAbilityDamageMultipliers(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name      string
		configure func(*battleengine.MemberSnapshot)
	}{
		{name: "性别关系倍率缺失异性分母", configure: func(member *battleengine.MemberSnapshot) {
			member.TargetGenderDamageMultiplier = &battleengine.TargetGenderDamageMultiplier{SameGenderNumerator: 5, SameGenderDenominator: 4, OppositeGenderNumerator: 3}
		}},
		{name: "拳击倍率分母为零", configure: func(member *battleengine.MemberSnapshot) {
			member.PunchBasedSkillDamageBoost = &battleengine.PunchBasedSkillDamageBoost{Numerator: 6}
		}},
		{name: "切割倍率分子为零", configure: func(member *battleengine.MemberSnapshot) {
			member.SlicingBasedSkillDamageBoost = &battleengine.SlicingBasedSkillDamageBoost{Denominator: 2}
		}},
		{name: "声音增伤分母为零", configure: func(member *battleengine.MemberSnapshot) {
			member.SoundBasedSkillDamageBoost = &battleengine.SoundBasedSkillDamageBoost{Numerator: 13}
		}},
		{name: "波动倍率分子为零", configure: func(member *battleengine.MemberSnapshot) {
			member.PulseBasedSkillDamageBoost = &battleengine.PulseBasedSkillDamageBoost{Denominator: 2}
		}},
		{name: "啃咬倍率分母为零", configure: func(member *battleengine.MemberSnapshot) {
			member.BiteBasedSkillDamageBoost = &battleengine.BiteBasedSkillDamageBoost{Numerator: 3}
		}},
		{name: "附加效果抑制倍率分子为零", configure: func(member *battleengine.MemberSnapshot) {
			member.SecondaryEffectsSuppressedDamageBoost = &battleengine.SecondaryEffectsSuppressedDamageBoost{Denominator: 10}
		}},
		{name: "声音减伤分母为零", configure: func(member *battleengine.MemberSnapshot) {
			member.SoundBasedSkillDamageReduction = &battleengine.SoundBasedSkillDamageReduction{Numerator: 1}
		}},
		{name: "克制减伤分子为零", configure: func(member *battleengine.MemberSnapshot) {
			member.SuperEffectiveDamageReduction = &battleengine.SuperEffectiveDamageReduction{Denominator: 4}
		}},
		{name: "满生命减伤分母为零", configure: func(member *battleengine.MemberSnapshot) {
			member.FullHPDamageReduction = &battleengine.FullHPDamageReduction{Numerator: 1}
		}},
	}
	for _, test := range tests {
		test := test
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			actor := newMember(1, "invalid-conditional-ability", 100, 100)
			target := newMember(1, "invalid-conditional-target", 100, 100)
			test.configure(&actor)
			_, err := battleengine.NewState(battleengine.InitialState{
				Format: battleengine.FormatSnapshot{Code: "invalid-conditional-ability", ActiveSlotsPerSide: 1, TeamSize: 1},
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

// TestResolveTurnConditionalAbilityDamageMultipliers 验证规则 112—121 在公开回合入口按匹配条件生效，
// 同时覆盖不匹配边界、目标特性穿透、结构化伤害事件、固定随机轨迹和最终状态推进。
func TestResolveTurnConditionalAbilityDamageMultipliers(t *testing.T) {
	t.Parallel()
	type configure func(*battleengine.MemberSnapshot, *battleengine.MemberSnapshot, *battleengine.SkillSnapshot, *battleengine.RuleSnapshot, *battleengine.EnvironmentSnapshot)
	tests := []struct {
		name           string
		plainConfigure configure
		match          configure
		miss           configure
		wantPlain      uint32
		wantMatch      uint32
		wantMiss       uint32
	}{
		{
			name: "112 同性目标伤害倍率",
			match: func(actor, target *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.GenderCode, target.GenderCode = "male", "male"
				actor.TargetGenderDamageMultiplier = &battleengine.TargetGenderDamageMultiplier{SameGenderNumerator: 5, SameGenderDenominator: 4, OppositeGenderNumerator: 3, OppositeGenderDenominator: 4}
			},
			miss: func(actor, target *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.GenderCode, target.GenderCode = "male", ""
				actor.TargetGenderDamageMultiplier = &battleengine.TargetGenderDamageMultiplier{SameGenderNumerator: 5, SameGenderDenominator: 4, OppositeGenderNumerator: 3, OppositeGenderDenominator: 4}
			},
			wantPlain: 37, wantMatch: 46, wantMiss: 37,
		},
		{
			name: "113 拳击类技能增伤",
			match: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.PunchBased = true
				actor.PunchBasedSkillDamageBoost = &battleengine.PunchBasedSkillDamageBoost{Numerator: 6, Denominator: 5}
			},
			miss: func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.PunchBasedSkillDamageBoost = &battleengine.PunchBasedSkillDamageBoost{Numerator: 6, Denominator: 5}
			},
			wantPlain: 37, wantMatch: 44, wantMiss: 37,
		},
		{
			name: "114 切割类技能增伤",
			match: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.SlicingBased = true
				actor.SlicingBasedSkillDamageBoost = &battleengine.SlicingBasedSkillDamageBoost{Numerator: 3, Denominator: 2}
			},
			miss: func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.SlicingBasedSkillDamageBoost = &battleengine.SlicingBasedSkillDamageBoost{Numerator: 3, Denominator: 2}
			},
			wantPlain: 37, wantMatch: 55, wantMiss: 37,
		},
		{
			name: "115 声音类技能增伤",
			match: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.SoundBased = true
				actor.SoundBasedSkillDamageBoost = &battleengine.SoundBasedSkillDamageBoost{Numerator: 13, Denominator: 10}
			},
			miss: func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.SoundBasedSkillDamageBoost = &battleengine.SoundBasedSkillDamageBoost{Numerator: 13, Denominator: 10}
			},
			wantPlain: 37, wantMatch: 48, wantMiss: 37,
		},
		{
			name: "116 波动类技能增伤",
			match: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.PulseBased = true
				actor.PulseBasedSkillDamageBoost = &battleengine.PulseBasedSkillDamageBoost{Numerator: 3, Denominator: 2}
			},
			miss: func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.PulseBasedSkillDamageBoost = &battleengine.PulseBasedSkillDamageBoost{Numerator: 3, Denominator: 2}
			},
			wantPlain: 37, wantMatch: 55, wantMiss: 37,
		},
		{
			name: "117 啃咬类技能增伤",
			match: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.BiteBased = true
				actor.BiteBasedSkillDamageBoost = &battleengine.BiteBasedSkillDamageBoost{Numerator: 3, Denominator: 2}
			},
			miss: func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.BiteBasedSkillDamageBoost = &battleengine.BiteBasedSkillDamageBoost{Numerator: 3, Denominator: 2}
			},
			wantPlain: 37, wantMatch: 55, wantMiss: 37,
		},
		{
			name: "118 抑制附加效果后增伤",
			match: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.StatStageEffects = []battleengine.StatStageEffect{{Stat: battleengine.StatDefense, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 100}}
				actor.SecondaryEffectsSuppressedDamageBoost = &battleengine.SecondaryEffectsSuppressedDamageBoost{Numerator: 13, Denominator: 10}
			},
			miss: func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.SecondaryEffectsSuppressedDamageBoost = &battleengine.SecondaryEffectsSuppressedDamageBoost{Numerator: 13, Denominator: 10}
			},
			wantPlain: 37, wantMatch: 48, wantMiss: 37,
		},
		{
			name: "119 声音类技能减伤",
			match: func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.SoundBased = true
				target.SoundBasedSkillDamageReduction = &battleengine.SoundBasedSkillDamageReduction{Numerator: 1, Denominator: 2}
			},
			miss: func(actor, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.SoundBased = true
				actor.IgnoreTargetAbilityEffects = true
				target.SoundBasedSkillDamageReduction = &battleengine.SoundBasedSkillDamageReduction{Numerator: 1, Denominator: 2}
			},
			wantPlain: 37, wantMatch: 18, wantMiss: 37,
		},
		{
			name: "120 严格克制技能减伤",
			plainConfigure: func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, rules *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.ElementID, target.ElementIDs = testID("fire"), testIDs("grass")
				rules.ElementEffectiveness = []battleengine.ElementEffectiveness{{AttackElementID: testID("fire"), DefenseElementID: testID("grass"), Numerator: 2, Denominator: 1}}
			},
			match: func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, rules *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.ElementID, target.ElementIDs = testID("fire"), testIDs("grass")
				rules.ElementEffectiveness = []battleengine.ElementEffectiveness{{AttackElementID: testID("fire"), DefenseElementID: testID("grass"), Numerator: 2, Denominator: 1}}
				target.SuperEffectiveDamageReduction = &battleengine.SuperEffectiveDamageReduction{Numerator: 3, Denominator: 4}
			},
			miss: func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				target.SuperEffectiveDamageReduction = &battleengine.SuperEffectiveDamageReduction{Numerator: 3, Denominator: 4}
			},
			wantPlain: 74, wantMatch: 55, wantMiss: 37,
		},
		{
			name: "121 满生命单段减伤",
			match: func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				target.FullHPDamageReduction = &battleengine.FullHPDamageReduction{Numerator: 1, Denominator: 2}
			},
			miss: func(_ *battleengine.MemberSnapshot, target *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				target.CurrentHP = target.MaxHP - 1
				target.FullHPDamageReduction = &battleengine.FullHPDamageReduction{Numerator: 1, Denominator: 2}
			},
			wantPlain: 37, wantMatch: 18, wantMiss: 37,
		},
	}

	for _, test := range tests {
		test := test
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			plain := resolveAbilityDamageMultiplierTurn(t, test.plainConfigure)
			matched := resolveAbilityDamageMultiplierTurn(t, test.match)
			missed := resolveAbilityDamageMultiplierTurn(t, test.miss)
			if plain.damage != test.wantPlain || matched.damage != test.wantMatch || missed.damage != test.wantMiss {
				t.Fatalf("伤害 = plain:%d matched:%d missed:%d，期望 %d/%d/%d", plain.damage, matched.damage, missed.damage, test.wantPlain, test.wantMatch, test.wantMiss)
			}
			if !reflect.DeepEqual(matched.trace, matched.script) || !eventOccursBefore(matched.events, battleengine.EventKindDamageApplied, battleengine.EventKindTurnEnded) {
				t.Fatalf("事件或随机轨迹错误: events=%v trace=%+v script=%+v", eventKinds(matched.events), matched.trace, matched.script)
			}
		})
	}
}

// TestResolveTurnTargetGenderDamageMultiplierUsesOppositeGenderFraction 验证异性分数独立于同性分数，
// 防止资料编译或伤害公式把两条性别关系压缩为单个布尔增伤开关。
func TestResolveTurnTargetGenderDamageMultiplierUsesOppositeGenderFraction(t *testing.T) {
	t.Parallel()
	result := resolveAbilityDamageMultiplierTurn(t, func(actor, target *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
		actor.GenderCode, target.GenderCode = "male", "female"
		actor.TargetGenderDamageMultiplier = &battleengine.TargetGenderDamageMultiplier{SameGenderNumerator: 5, SameGenderDenominator: 4, OppositeGenderNumerator: 3, OppositeGenderDenominator: 4}
	})
	if result.damage != 27 || !reflect.DeepEqual(result.trace, result.script) {
		t.Fatalf("异性伤害 = %d, trace=%+v，期望 27 和固定轨迹", result.damage, result.trace)
	}
}

// TestResolveTurnSecondaryEffectsSuppressedDamageBoostRemovesStatChanges 验证规则 118 不仅提高伤害，
// 还会移除技能声明的目标能力变化且不消费对应概率随机数。
func TestResolveTurnSecondaryEffectsSuppressedDamageBoostRemovesStatChanges(t *testing.T) {
	t.Parallel()
	actor := newMember(1, "sheer-force-actor", 100, 100)
	actor.Stats.Speed = 200
	actor.SecondaryEffectsSuppressedDamageBoost = &battleengine.SecondaryEffectsSuppressedDamageBoost{Numerator: 13, Denominator: 10}
	actor.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("sheer-force-skill"), Name: "强行测试", ElementID: testID("neutral"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
		StatStageEffects: []battleengine.StatStageEffect{{Stat: battleengine.StatDefense, Target: battleengine.EffectTargetSelected, StageDelta: -1, ChancePercent: 50}},
	}
	target := sleepingStrongWeatherSource("sheer-force-target", "")
	target.MaxHP, target.CurrentHP = 500, 500
	script := []battleengine.RandomTraceEntry{
		{Sequence: 1, Bound: 24, Reason: "critical hit for " + testID("sheer-force-skill").String(), Value: 1},
		{Sequence: 2, Bound: 16, Reason: "damage random for " + testID("sheer-force-skill").String(), Value: 15},
	}
	random, err := battleengine.NewTracedRandom(script)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "sheer-force", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  battleengine.RuleSnapshot{SchemaVersion: 1},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{actor}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	result, err := battleengine.ResolveTurn(state, fieldSpeedOrderTurn(1,
		strongWeatherUseSkillAction(battleengine.SideOne, 1, battleengine.SideTwo, 1),
		strongWeatherUseSkillAction(battleengine.SideTwo, 1, battleengine.SideOne, 1),
	), random)
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	updatedTarget, found := result.State.ActiveMember(battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1})
	if !found || updatedTarget.StatStages[battleengine.StatDefense] != 0 || !reflect.DeepEqual(result.RandomTrace, script) {
		t.Fatalf("抑制后目标=%+v trace=%+v，期望防御阶级不变且仅消费要害与伤害随机", updatedTarget, result.RandomTrace)
	}
	for _, event := range result.Events {
		if _, changed := event.(battleengine.StatStageChangedEvent); changed {
			t.Fatalf("抑制附加效果后不应产生能力变化事件: %v", eventKinds(result.Events))
		}
	}
}
