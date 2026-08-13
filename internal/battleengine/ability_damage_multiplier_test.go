package battleengine_test

import (
	"errors"
	"reflect"
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestNewStateRejectsInvalidAbilityDamageMultiplierRules 验证纯战斗引擎入口独立拒绝十类不完整倍率，
// 即使调用方绕过资料与 Battle 编译边界，也不能把零分母、未知天气或重复身份带入权威状态。
func TestNewStateRejectsInvalidAbilityDamageMultiplierRules(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name      string
		configure func(*battleengine.MemberSnapshot)
	}{
		{name: "基础威力上限缺失", configure: func(member *battleengine.MemberSnapshot) {
			member.BasePowerAtMostDamageBoost = &battleengine.BasePowerAtMostDamageBoost{Numerator: 3, Denominator: 2}
		}},
		{name: "反作用倍率分母为零", configure: func(member *battleengine.MemberSnapshot) {
			member.RecoilSkillDamageBoost = &battleengine.RecoilSkillDamageBoost{Numerator: 6}
		}},
		{name: "低生命属性身份为空", configure: func(member *battleengine.MemberSnapshot) {
			member.LowHPElementDamageBoost = &battleengine.LowHPElementDamageBoost{HPThresholdNumerator: 1, HPThresholdDenominator: 3, DamageNumerator: 3, DamageDenominator: 2}
		}},
		{name: "天气不属于封闭集合", configure: func(member *battleengine.MemberSnapshot) {
			member.WeatherElementDamageBoost = &battleengine.WeatherElementDamageBoost{Weather: battleengine.WeatherKind("unknown"), ElementIDs: testIDs("fire"), Numerator: 3, Denominator: 2}
		}},
		{name: "属性集合包含重复值", configure: func(member *battleengine.MemberSnapshot) {
			member.ElementSkillDamageBoost = &battleengine.ElementSkillDamageBoost{ElementIDs: testIDs("fire", "fire"), Numerator: 3, Denominator: 2}
		}},
		{name: "属性一致覆盖分母为零", configure: func(member *battleengine.MemberSnapshot) {
			member.SameElementBonusOverride = &battleengine.SameElementBonusOverride{Numerator: 2}
		}},
		{name: "接触技能倍率分子为零", configure: func(member *battleengine.MemberSnapshot) {
			member.ContactBasedSkillDamageBoost = &battleengine.ContactBasedSkillDamageBoost{Denominator: 10}
		}},
		{name: "要害倍率分母为零", configure: func(member *battleengine.MemberSnapshot) {
			member.CriticalHitDamageBoost = &battleengine.CriticalHitDamageBoost{Numerator: 3}
		}},
		{name: "克制倍率分子为零", configure: func(member *battleengine.MemberSnapshot) {
			member.SuperEffectiveDamageBoost = &battleengine.SuperEffectiveDamageBoost{Denominator: 4}
		}},
		{name: "抗性倍率分母为零", configure: func(member *battleengine.MemberSnapshot) {
			member.NotVeryEffectiveDamageBoost = &battleengine.NotVeryEffectiveDamageBoost{Numerator: 2}
		}},
	}
	for _, test := range tests {
		test := test
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			actor := newMember(1, "invalid-ability-damage-attacker", 100, 100)
			target := newMember(1, "invalid-ability-damage-target", 100, 100)
			test.configure(&actor)
			_, err := battleengine.NewState(battleengine.InitialState{
				Format: battleengine.FormatSnapshot{Code: "invalid-ability-damage", ActiveSlotsPerSide: 1, TeamSize: 1},
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

// TestResolveTurnAbilityDamageMultipliers 验证十条攻击型特性规则都在普通直接伤害的最终倍率链生效。
// 每个子测试同时固定匹配路径、关键不匹配边界、结构化伤害事件、随机轨迹和最终成员快照。
func TestResolveTurnAbilityDamageMultipliers(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name          string
		configure     func(*battleengine.MemberSnapshot, *battleengine.MemberSnapshot, *battleengine.SkillSnapshot, *battleengine.RuleSnapshot, *battleengine.EnvironmentSnapshot)
		configureMiss func(*battleengine.MemberSnapshot, *battleengine.MemberSnapshot, *battleengine.SkillSnapshot, *battleengine.RuleSnapshot, *battleengine.EnvironmentSnapshot)
		wantPlain     uint32
		wantBoosted   uint32
		wantMiss      uint32
	}{
		{
			name: "基础威力不超过上限时强化",
			configure: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.Power = 60
				actor.BasePowerAtMostDamageBoost = &battleengine.BasePowerAtMostDamageBoost{MaximumPower: 60, Numerator: 3, Denominator: 2}
			},
			configureMiss: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.Power = 61
				actor.BasePowerAtMostDamageBoost = &battleengine.BasePowerAtMostDamageBoost{MaximumPower: 60, Numerator: 3, Denominator: 2}
			},
			wantPlain: 28, wantBoosted: 42, wantMiss: 28,
		},
		{
			name: "按实际伤害反作用技能强化",
			configure: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.DrainPercent = -25
				actor.RecoilSkillDamageBoost = &battleengine.RecoilSkillDamageBoost{Numerator: 6, Denominator: 5}
			},
			configureMiss: func(actor, _ *battleengine.MemberSnapshot, _ *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.RecoilSkillDamageBoost = &battleengine.RecoilSkillDamageBoost{Numerator: 6, Denominator: 5}
			},
			wantPlain: 37, wantBoosted: 44, wantMiss: 37,
		},
		{
			name: "低体力匹配属性强化",
			configure: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.CurrentHP = 33
				skill.ElementID = testID("bug")
				actor.LowHPElementDamageBoost = &battleengine.LowHPElementDamageBoost{
					ElementID: testID("bug"), HPThresholdNumerator: 1, HPThresholdDenominator: 3, DamageNumerator: 3, DamageDenominator: 2,
				}
			},
			configureMiss: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.CurrentHP = 34
				skill.ElementID = testID("bug")
				actor.LowHPElementDamageBoost = &battleengine.LowHPElementDamageBoost{
					ElementID: testID("bug"), HPThresholdNumerator: 1, HPThresholdDenominator: 3, DamageNumerator: 3, DamageDenominator: 2,
				}
			},
			wantPlain: 37, wantBoosted: 55, wantMiss: 37,
		},
		{
			name: "匹配天气与有效属性强化",
			configure: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, environment *battleengine.EnvironmentSnapshot) {
				skill.ElementID = testID("rock")
				environment.Weather = &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 3}
				actor.WeatherElementDamageBoost = &battleengine.WeatherElementDamageBoost{
					Weather: battleengine.WeatherKindSandstorm, ElementIDs: testIDs("ground", "rock", "steel"), Numerator: 13, Denominator: 10,
				}
			},
			configureMiss: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, environment *battleengine.EnvironmentSnapshot) {
				skill.ElementID = testID("water")
				environment.Weather = &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 3}
				actor.WeatherElementDamageBoost = &battleengine.WeatherElementDamageBoost{
					Weather: battleengine.WeatherKindSandstorm, ElementIDs: testIDs("ground", "rock", "steel"), Numerator: 13, Denominator: 10,
				}
			},
			wantPlain: 37, wantBoosted: 48, wantMiss: 37,
		},
		{
			name: "指定有效属性强化",
			configure: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.ElementID = testID("electric")
				actor.ElementSkillDamageBoost = &battleengine.ElementSkillDamageBoost{ElementIDs: testIDs("electric"), Numerator: 3, Denominator: 2}
			},
			configureMiss: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.ElementID = testID("water")
				actor.ElementSkillDamageBoost = &battleengine.ElementSkillDamageBoost{ElementIDs: testIDs("electric"), Numerator: 3, Denominator: 2}
			},
			wantPlain: 37, wantBoosted: 55, wantMiss: 37,
		},
		{
			name: "属性一致加成倍率覆盖",
			configure: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.ElementIDs = testIDs("fire")
				skill.ElementID = testID("fire")
				actor.SameElementBonusOverride = &battleengine.SameElementBonusOverride{Numerator: 2, Denominator: 1}
			},
			configureMiss: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.ElementIDs = testIDs("water")
				skill.ElementID = testID("fire")
				actor.SameElementBonusOverride = &battleengine.SameElementBonusOverride{Numerator: 2, Denominator: 1}
			},
			wantPlain: 55, wantBoosted: 74, wantMiss: 37,
		},
		{
			name: "有效接触技能强化",
			configure: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.MakesContact = true
				actor.ContactBasedSkillDamageBoost = &battleengine.ContactBasedSkillDamageBoost{Numerator: 13, Denominator: 10}
			},
			configureMiss: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.MakesContact = true
				actor.ContactSuppression = true
				actor.ContactBasedSkillDamageBoost = &battleengine.ContactBasedSkillDamageBoost{Numerator: 13, Denominator: 10}
			},
			wantPlain: 37, wantBoosted: 48, wantMiss: 37,
		},
		{
			name: "击中要害额外强化",
			configure: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.CriticalHitStage = 3
				actor.CriticalHitDamageBoost = &battleengine.CriticalHitDamageBoost{Numerator: 3, Denominator: 2}
			},
			configureMiss: func(actor, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.CriticalHitStage = 3
				target.CriticalHitImmunity = true
				actor.CriticalHitDamageBoost = &battleengine.CriticalHitDamageBoost{Numerator: 3, Denominator: 2}
			},
			wantPlain: 55, wantBoosted: 83, wantMiss: 37,
		},
		{
			name: "严格克制伤害强化",
			configure: func(actor, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, rules *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.ElementID = testID("fire")
				target.ElementIDs = testIDs("grass")
				rules.ElementEffectiveness = []battleengine.ElementEffectiveness{{AttackElementID: testID("fire"), DefenseElementID: testID("grass"), Numerator: 2, Denominator: 1}}
				actor.SuperEffectiveDamageBoost = &battleengine.SuperEffectiveDamageBoost{Numerator: 5, Denominator: 4}
			},
			configureMiss: func(actor, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, rules *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.ElementID = testID("fire")
				target.ElementIDs = testIDs("water")
				rules.ElementEffectiveness = []battleengine.ElementEffectiveness{{AttackElementID: testID("fire"), DefenseElementID: testID("water"), Numerator: 1, Denominator: 2}}
				actor.SuperEffectiveDamageBoost = &battleengine.SuperEffectiveDamageBoost{Numerator: 5, Denominator: 4}
			},
			wantPlain: 74, wantBoosted: 92, wantMiss: 18,
		},
		{
			name: "非零抗性伤害补偿",
			configure: func(actor, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, rules *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.ElementID = testID("fire")
				target.ElementIDs = testIDs("water")
				rules.ElementEffectiveness = []battleengine.ElementEffectiveness{{AttackElementID: testID("fire"), DefenseElementID: testID("water"), Numerator: 1, Denominator: 2}}
				actor.NotVeryEffectiveDamageBoost = &battleengine.NotVeryEffectiveDamageBoost{Numerator: 2, Denominator: 1}
			},
			configureMiss: func(actor, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, rules *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.ElementID = testID("fire")
				target.ElementIDs = testIDs("ghost")
				rules.ElementEffectiveness = []battleengine.ElementEffectiveness{{AttackElementID: testID("fire"), DefenseElementID: testID("ghost"), Numerator: 0, Denominator: 1}}
				actor.NotVeryEffectiveDamageBoost = &battleengine.NotVeryEffectiveDamageBoost{Numerator: 2, Denominator: 1}
			},
			wantPlain: 18, wantBoosted: 37, wantMiss: 0,
		},
	}

	for _, test := range tests {
		test := test
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			plain := resolveAbilityDamageMultiplierTurn(t, nil)
			boosted := resolveAbilityDamageMultiplierTurn(t, test.configure)
			miss := resolveAbilityDamageMultiplierTurn(t, test.configureMiss)
			if test.name == "基础威力不超过上限时强化" {
				plain = resolveAbilityDamageMultiplierTurn(t, func(_, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
					skill.Power = 60
				})
			}
			if test.name == "属性一致加成倍率覆盖" {
				plain = resolveAbilityDamageMultiplierTurn(t, func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
					actor.ElementIDs = testIDs("fire")
					skill.ElementID = testID("fire")
				})
			}
			if test.name == "击中要害额外强化" {
				plain = resolveAbilityDamageMultiplierTurn(t, func(_actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
					skill.CriticalHitStage = 3
				})
			}
			if test.name == "严格克制伤害强化" {
				plain = resolveAbilityDamageMultiplierTurn(t, func(_actor, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, rules *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
					skill.ElementID = testID("fire")
					target.ElementIDs = testIDs("grass")
					rules.ElementEffectiveness = []battleengine.ElementEffectiveness{{AttackElementID: testID("fire"), DefenseElementID: testID("grass"), Numerator: 2, Denominator: 1}}
				})
			}
			if test.name == "非零抗性伤害补偿" {
				plain = resolveAbilityDamageMultiplierTurn(t, func(_actor, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, rules *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
					skill.ElementID = testID("fire")
					target.ElementIDs = testIDs("water")
					rules.ElementEffectiveness = []battleengine.ElementEffectiveness{{AttackElementID: testID("fire"), DefenseElementID: testID("water"), Numerator: 1, Denominator: 2}}
				})
			}
			if plain.damage != test.wantPlain || boosted.damage != test.wantBoosted || miss.damage != test.wantMiss {
				t.Fatalf("伤害 = plain:%d boosted:%d miss:%d，期望 %d/%d/%d", plain.damage, boosted.damage, miss.damage, test.wantPlain, test.wantBoosted, test.wantMiss)
			}
			if !reflect.DeepEqual(boosted.trace, boosted.script) || !eventOccursBefore(boosted.events, battleengine.EventKindDamageApplied, battleengine.EventKindTurnEnded) {
				t.Fatalf("事件或随机轨迹错误: events=%v trace=%+v script=%+v", eventKinds(boosted.events), boosted.trace, boosted.script)
			}
		})
	}
}

// TestResolveTurnSameElementBonusOverridePreservesTerastallizationSemantics 验证太晶化后仍按冻结的自然属性保留
// 属性一致加成，并在自然属性与太晶属性同时匹配时使用现代双重一致倍率；特性覆盖必须替换对应倍率位置。
func TestResolveTurnSameElementBonusOverridePreservesTerastallizationSemantics(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name        string
		natural     string
		tera        string
		withAbility bool
		wantDamage  uint32
	}{
		{name: "自然与太晶属性同时匹配使用默认双重加成", natural: "fire", tera: "fire", wantDamage: 74},
		{name: "覆盖特性把双重加成替换为九比四", natural: "fire", tera: "fire", withAbility: true, wantDamage: 83},
		{name: "太晶属性改变后仍保留自然属性覆盖加成", natural: "fire", tera: "water", withAbility: true, wantDamage: 74},
	}
	for _, test := range tests {
		test := test
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			result := resolveAbilityDamageMultiplierTurn(t, func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				actor.NaturalElementIDs = testIDs(test.natural)
				actor.ElementIDs = testIDs(test.tera)
				actor.TeraElementID = testID(test.tera)
				actor.Terastallized = true
				skill.ElementID = testID(test.natural)
				if test.withAbility {
					actor.SameElementBonusOverride = &battleengine.SameElementBonusOverride{Numerator: 2, Denominator: 1}
				}
			})
			if result.damage != test.wantDamage || !reflect.DeepEqual(result.trace, result.script) {
				t.Fatalf("太晶属性一致伤害 = %d, trace = %+v，期望 damage=%d, trace=%+v", result.damage, result.trace, test.wantDamage, result.script)
			}
		})
	}
}

// TestResolveTurnAbilityDamageMultipliersUseEffectiveFacts 验证攻击型倍率读取本次结算形成的有效属性、有效天气和
// 强风修正后的最终相性，同时确认零原始威力的动态威力技能不能误触发低基础威力规则。
func TestResolveTurnAbilityDamageMultipliersUseEffectiveFacts(t *testing.T) {
	t.Parallel()
	tests := []struct {
		name       string
		configure  func(*battleengine.MemberSnapshot, *battleengine.MemberSnapshot, *battleengine.SkillSnapshot, *battleengine.RuleSnapshot, *battleengine.EnvironmentSnapshot)
		wantDamage uint32
	}{
		{
			name: "动态威力不冒充原始基础威力",
			configure: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, _ *battleengine.EnvironmentSnapshot) {
				skill.Power = 0
				skill.DynamicPower = battleengine.DynamicPowerRule{
					Kind: battleengine.DynamicPowerKindPositiveStatStageSum, Source: battleengine.EffectTargetUser,
					BasePower: 60, PowerPerPositiveStage: 10,
				}
				actor.BasePowerAtMostDamageBoost = &battleengine.BasePowerAtMostDamageBoost{MaximumPower: 60, Numerator: 3, Denominator: 2}
			},
			wantDamage: 28,
		},
		{
			name: "属性覆盖后的有效属性触发倍率",
			configure: func(actor, _ *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, environment *battleengine.EnvironmentSnapshot) {
				environment.Weather = &battleengine.WeatherEffect{Kind: battleengine.WeatherKindRain, TurnsRemaining: 3}
				skill.ElementID = testID("fire")
				skill.WeatherElementOverrides = []battleengine.WeatherElementOverride{{Weather: battleengine.WeatherKindRain, ElementID: testID("water")}}
				actor.ElementSkillDamageBoost = &battleengine.ElementSkillDamageBoost{ElementIDs: testIDs("water"), Numerator: 3, Denominator: 2}
			},
			wantDamage: 55,
		},
		{
			name: "天气封锁使天气倍率保持中性",
			configure: func(actor, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, _ *battleengine.RuleSnapshot, environment *battleengine.EnvironmentSnapshot) {
				environment.Weather = &battleengine.WeatherEffect{Kind: battleengine.WeatherKindSandstorm, TurnsRemaining: 3}
				target.WeatherEffectsSuppressed = true
				skill.ElementID = testID("rock")
				actor.WeatherElementDamageBoost = &battleengine.WeatherElementDamageBoost{
					Weather: battleengine.WeatherKindSandstorm, ElementIDs: testIDs("rock"), Numerator: 13, Denominator: 10,
				}
			},
			wantDamage: 37,
		},
		{
			name: "强风中和飞行弱点后不再触发克制倍率",
			configure: func(actor, target *battleengine.MemberSnapshot, skill *battleengine.SkillSnapshot, rules *battleengine.RuleSnapshot, environment *battleengine.EnvironmentSnapshot) {
				skill.ElementID = testID("electric")
				target.ElementIDs = testIDs("flying")
				target.SwitchInStrongWeather = battleengine.StrongWeatherKindStrongWinds
				environment.StrongWeather = &battleengine.StrongWeatherState{
					Kind: battleengine.StrongWeatherKindStrongWinds, Source: battleengine.MemberRef{Side: battleengine.SideTwo, Position: 1},
				}
				rules.ElementIDs = map[string]battleengine.Identifier{"electric": testID("electric"), "flying": testID("flying")}
				rules.ElementEffectiveness = []battleengine.ElementEffectiveness{{
					AttackElementID: testID("electric"), DefenseElementID: testID("flying"), Numerator: 2, Denominator: 1,
				}}
				actor.SuperEffectiveDamageBoost = &battleengine.SuperEffectiveDamageBoost{Numerator: 5, Denominator: 4}
			},
			wantDamage: 37,
		},
	}
	for _, test := range tests {
		test := test
		t.Run(test.name, func(t *testing.T) {
			t.Parallel()
			result := resolveAbilityDamageMultiplierTurn(t, test.configure)
			if result.damage != test.wantDamage || !reflect.DeepEqual(result.trace, result.script) {
				t.Fatalf("有效战斗事实伤害 = %d, trace = %+v，期望 damage=%d, trace=%+v", result.damage, result.trace, test.wantDamage, result.script)
			}
		})
	}
}

// abilityDamageMultiplierResult 保存一次公开回合结算中用于比较的伤害、事件和精确随机轨迹。
type abilityDamageMultiplierResult struct {
	damage uint32
	events []battleengine.Event
	trace  []battleengine.RandomTraceEntry
	script []battleengine.RandomTraceEntry
}

// resolveAbilityDamageMultiplierTurn 通过 ResolveTurn 执行一条攻击型特性规则，并返回第一方造成的本体伤害。
func resolveAbilityDamageMultiplierTurn(
	t *testing.T,
	configure func(*battleengine.MemberSnapshot, *battleengine.MemberSnapshot, *battleengine.SkillSnapshot, *battleengine.RuleSnapshot, *battleengine.EnvironmentSnapshot),
) abilityDamageMultiplierResult {
	t.Helper()
	actor := newMember(1, "ability-damage-attacker", 100, 100)
	actor.Stats.Speed = 200
	actor.ElementIDs = testIDs("neutral")
	actor.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("ability-damage-skill"), Name: "攻击型特性伤害", ElementID: testID("fire"),
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: 80, Accuracy: 100, RemainingPP: 5, MaxPP: 5,
	}
	target := sleepingStrongWeatherSource("ability-damage-target", "")
	target.MaxHP, target.CurrentHP = 500, 500
	target.ElementIDs = testIDs("neutral-target")
	rules := battleengine.RuleSnapshot{SchemaVersion: 1}
	environment := battleengine.EnvironmentSnapshot{}
	if configure != nil {
		configure(&actor, &target, &actor.Skills[0], &rules, &environment)
	}
	script := []battleengine.RandomTraceEntry{{
		Sequence: 1, Bound: 24, Reason: "critical hit for " + actor.Skills[0].SkillID.String(), Value: 1,
	}, {
		Sequence: 2, Bound: 16, Reason: "damage random for " + actor.Skills[0].SkillID.String(), Value: 15,
	}}
	if actor.Skills[0].CriticalHitStage >= 3 {
		script = script[1:]
		script[0].Sequence = 1
	}
	random, err := battleengine.NewTracedRandom(script)
	if err != nil {
		t.Fatalf("NewTracedRandom() error = %v", err)
	}
	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "ability-damage-multiplier", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules:  rules, Environment: environment,
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
	damage := uint32(0)
	for _, event := range result.Events {
		if applied, ok := event.(battleengine.DamageAppliedEvent); ok && applied.Actor.Side == battleengine.SideOne {
			damage = applied.Amount
			break
		}
	}
	return abilityDamageMultiplierResult{damage: damage, events: result.Events, trace: result.RandomTrace, script: script}
}
