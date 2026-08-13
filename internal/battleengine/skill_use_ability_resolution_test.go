package battleengine_test

import (
	"testing"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TestResolveTurnMegaSolTreatsUserSkillsAsSunlight 验证超级日光只为持有者本次使用的招式提供晴天语义。
//
// 它既要强化火属性伤害，也要让明确声明“晴天下跳过蓄力”的技能直接完成；环境快照本身仍然没有天气，
// 避免把该特性错误实现为入场建立全场天气。
func TestResolveTurnMegaSolTreatsUserSkillsAsSunlight(t *testing.T) {
	t.Parallel()
	plainDamage, plain := resolveSkillUseAbilityAttack(t, skillUseAbilityAttackOptions{elementID: testID("fire"), power: 80})
	sunDamage, sun := resolveSkillUseAbilityAttack(t, skillUseAbilityAttackOptions{
		elementID: testID("fire"), power: 80, skillWeatherOverride: battleengine.WeatherKindSun,
	})
	if sunDamage <= plainDamage || plain.Snapshot().Environment.Weather != nil || sun.Snapshot().Environment.Weather != nil {
		t.Fatalf("超级日光伤害/环境 = plain:%d sun:%d plainWeather:%+v sunWeather:%+v", plainDamage, sunDamage, plain.Snapshot().Environment.Weather, sun.Snapshot().Environment.Weather)
	}

	_, charged := resolveSkillUseAbilityAttack(t, skillUseAbilityAttackOptions{
		elementID: testID("grass"), power: 120, chargeSkippedWeather: battleengine.WeatherKindSun,
	})
	_, skipped := resolveSkillUseAbilityAttack(t, skillUseAbilityAttackOptions{
		elementID: testID("grass"), power: 120, chargeSkippedWeather: battleengine.WeatherKindSun,
		skillWeatherOverride: battleengine.WeatherKindSun,
	})
	chargedMember, _ := charged.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	skippedMember, _ := skipped.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if chargedMember.ChargingSkillPosition != 1 || skippedMember.ChargingSkillPosition != 0 {
		t.Fatalf("超级日光蓄力结果 = plain:%+v megaSol:%+v", chargedMember, skippedMember)
	}
}

// TestResolveTurnPiercingDrillDealsQuarterDamageThroughProtection 验证贯穿钻以接触技能穿透个人保护时只造成原伤害的四分之一。
// 非保护目标仍承受完整伤害，确保倍率只属于成功穿透保护的结算分支。
func TestResolveTurnPiercingDrillDealsQuarterDamageThroughProtection(t *testing.T) {
	t.Parallel()
	fullDamage, _ := resolveSkillUseAbilityAttack(t, skillUseAbilityAttackOptions{elementID: testID("ground"), power: 100, makesContact: true})
	bypassedDamage, _ := resolveSkillUseAbilityAttack(t, skillUseAbilityAttackOptions{
		elementID: testID("ground"), power: 100, makesContact: true, targetProtected: true,
		protectionBypass: true, protectionBypassMultiplier: &battleengine.DamageFraction{Numerator: 1, Denominator: 4},
	})
	if bypassedDamage == 0 || bypassedDamage != fullDamage/4 {
		t.Fatalf("贯穿钻保护穿透伤害 = %d，完整伤害 = %d", bypassedDamage, fullDamage)
	}
}

// TestResolveTurnSpicySprayBurnsDamagingAttacker 验证辣椒喷发在持有者承受技能本体伤害后灼伤攻击者。
// 规则不要求接触；状态通过引擎既有属性免疫入口写入，不能直接绕过主要异常校验。
func TestResolveTurnSpicySprayBurnsDamagingAttacker(t *testing.T) {
	t.Parallel()
	_, resolved := resolveSkillUseAbilityAttack(t, skillUseAbilityAttackOptions{
		elementID: testID("water"), power: 60,
		receivedDamageAttackerStatus: battleengine.MajorStatusBurn,
	})
	attacker, _ := resolved.ActiveMember(battleengine.SlotRef{Side: battleengine.SideOne, Position: 1})
	if attacker.MajorStatus != battleengine.MajorStatusBurn {
		t.Fatalf("辣椒喷发后的攻击者异常 = %q，期望 burn", attacker.MajorStatus)
	}
}

// TestResolveTurnDragonizeConvertsAndBoostsNormalSkill 验证龙皮肤把一般属性技能转换为龙属性并仅强化被转换的技能。
// 原生龙属性技能不能获得转换倍率，避免把该特性退化成无条件的龙属性伤害强化。
func TestResolveTurnDragonizeConvertsAndBoostsNormalSkill(t *testing.T) {
	t.Parallel()
	normalDamage, _ := resolveSkillUseAbilityAttack(t, skillUseAbilityAttackOptions{elementID: testID("normal"), power: 80})
	convertedDamage, _ := resolveSkillUseAbilityAttack(t, skillUseAbilityAttackOptions{
		elementID: testID("normal"), power: 80,
		elementConversion: &battleengine.SkillElementConversion{
			SourceElementID: testID("normal"), TargetElementID: testID("dragon"), DamageNumerator: 6, DamageDenominator: 5,
		},
	})
	nativeDragonDamage, _ := resolveSkillUseAbilityAttack(t, skillUseAbilityAttackOptions{
		elementID: testID("dragon"), power: 80,
		elementConversion: &battleengine.SkillElementConversion{
			SourceElementID: testID("normal"), TargetElementID: testID("dragon"), DamageNumerator: 6, DamageDenominator: 5,
		},
	})
	if convertedDamage <= normalDamage || nativeDragonDamage != normalDamage {
		t.Fatalf("龙皮肤伤害 = normal:%d converted:%d nativeDragon:%d", normalDamage, convertedDamage, nativeDragonDamage)
	}
}

// skillUseAbilityAttackOptions 描述技能使用型特性公开回合测试所需的最小资料事实。
type skillUseAbilityAttackOptions struct {
	// elementID 是被测技能的基础属性稳定标识。
	elementID Identifier
	// power 是被测技能的基础威力。
	power uint16
	// makesContact 表示技能静态声明为接触类技能。
	makesContact bool
	// targetProtected 表示目标先于攻击者建立个人保护。
	targetProtected bool
	// protectionBypass 表示攻击者能够用有效接触技能穿透个人保护。
	protectionBypass bool
	// protectionBypassMultiplier 是成功穿透保护时应用的独立伤害倍率。
	protectionBypassMultiplier *battleengine.DamageFraction
	// skillWeatherOverride 是攻击者特性为其技能提供的天气语义；空值表示没有覆盖。
	skillWeatherOverride battleengine.WeatherKind
	// chargeSkippedWeather 是技能允许直接跳过蓄力的天气；空值表示技能不蓄力。
	chargeSkippedWeather battleengine.WeatherKind
	// receivedDamageAttackerStatus 是目标受伤后通过特性施加给攻击者的主要异常。
	receivedDamageAttackerStatus battleengine.MajorStatus
	// elementConversion 是攻击者特性提供的技能属性转换与转换专属倍率。
	elementConversion *battleengine.SkillElementConversion
}

// resolveSkillUseAbilityAttack 通过公开状态与回合结算入口执行一回合，并返回攻击造成的第一段本体伤害。
func resolveSkillUseAbilityAttack(t *testing.T, options skillUseAbilityAttackOptions) (uint32, battleengine.State) {
	t.Helper()
	attacker := newMember(1, "skill-use-ability-attacker", 1_000, 1_000)
	attacker.Stats.Speed = 100
	attacker.ElementIDs = testIDs("neutral")
	attacker.SkillWeatherOverride = options.skillWeatherOverride
	attacker.SkillElementConversion = options.elementConversion
	attacker.ContactSkillProtectionBypass = options.protectionBypass
	attacker.ContactSkillProtectionBypassDamageMultiplier = options.protectionBypassMultiplier
	attacker.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("skill-use-ability-attack"), Name: "技能使用型特性攻击", ElementID: options.elementID,
		DamageClass: battleengine.DamageClassPhysical, TargetScope: battleengine.SkillTargetScopeSelectedTarget,
		Power: options.power, Accuracy: 100, RemainingPP: 10, MaxPP: 10, MakesContact: options.makesContact,
	}
	if options.chargeSkippedWeather != "" {
		attacker.Skills[0].ChargeSkippedWeathers = []battleengine.WeatherKind{options.chargeSkippedWeather}
		attacker.Skills[0].VolatileStatusApplications = []battleengine.VolatileStatusApplication{{
			Status: battleengine.VolatileStatusCharging, Target: battleengine.EffectTargetUser,
			ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
		}}
	}

	target := newMember(1, "skill-use-ability-target", 1_000, 1_000)
	target.Stats.Speed = 200
	if options.receivedDamageAttackerStatus != "" {
		target.ReactiveAbilityRules = &battleengine.ReactiveAbilityRules{
			ReceivedDamageAttackerMajorStatus: options.receivedDamageAttackerStatus,
		}
	}
	if options.targetProtected {
		target.Skills[0] = battleengine.SkillSnapshot{DamageMode: battleengine.SkillDamageModeFormula, MinHits: 1, MaxHits: 1, Position: 1, SkillID: testID("skill-use-ability-protection"), Name: "保护", ElementID: testID("neutral"),
			DamageClass: battleengine.DamageClassStatus, TargetScope: battleengine.SkillTargetScopeSelf,
			Accuracy: 100, RemainingPP: 10, MaxPP: 10,
			VolatileStatusApplications: []battleengine.VolatileStatusApplication{{
				Status: battleengine.VolatileStatusProtection, Target: battleengine.EffectTargetUser,
				ChancePercent: 100, MinTurns: 1, MaxTurns: 1,
			}},
		}
	} else {
		target.Stats.Speed = 50
	}

	state, err := battleengine.NewState(battleengine.InitialState{
		Format: battleengine.FormatSnapshot{Code: "skill-use-ability", ActiveSlotsPerSide: 1, TeamSize: 1},
		Rules: battleengine.RuleSnapshot{SchemaVersion: 1, ElementIDs: map[string]Identifier{
			"fire": testID("fire"), "water": testID("water"), "grass": testID("grass"), "ground": testID("ground"), "dragon": testID("dragon"), "normal": testID("normal"),
		}, ElementEffectiveness: []battleengine.ElementEffectiveness{
			{AttackElementID: testID("normal"), DefenseElementID: testID("neutral"), Numerator: 1, Denominator: 1},
			{AttackElementID: testID("dragon"), DefenseElementID: testID("neutral"), Numerator: 1, Denominator: 1},
			{AttackElementID: testID("fire"), DefenseElementID: testID("neutral"), Numerator: 1, Denominator: 1},
			{AttackElementID: testID("water"), DefenseElementID: testID("neutral"), Numerator: 1, Denominator: 1},
			{AttackElementID: testID("grass"), DefenseElementID: testID("neutral"), Numerator: 1, Denominator: 1},
			{AttackElementID: testID("ground"), DefenseElementID: testID("neutral"), Numerator: 1, Denominator: 1},
		}},
		Sides: []battleengine.SideSnapshot{
			{Side: battleengine.SideOne, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{attacker}},
			{Side: battleengine.SideTwo, ActiveMembers: []battleengine.MemberPosition{1}, Members: []battleengine.MemberSnapshot{target}},
		},
	})
	if err != nil {
		t.Fatalf("NewState() error = %v", err)
	}
	actions := []battleengine.Action{{
		Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1},
		UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}},
	}}
	if options.targetProtected {
		actions = append(actions, battleengine.Action{
			Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
			UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1}},
		})
	} else {
		actions = append(actions, battleengine.Action{
			Kind: battleengine.ActionKindUseSkill, Actor: battleengine.SlotRef{Side: battleengine.SideTwo, Position: 1},
			UseSkill: &battleengine.UseSkillAction{SkillPosition: 1, Target: battleengine.SlotRef{Side: battleengine.SideOne, Position: 1}},
		})
	}
	resolved, err := battleengine.ResolveTurn(state, battleengine.TurnCommand{SchemaVersion: 1, TurnNumber: 1, Actions: actions}, mustRandom(t, 8_021))
	if err != nil {
		t.Fatalf("ResolveTurn() error = %v", err)
	}
	var damage uint32
	for _, event := range resolved.Events {
		if applied, ok := event.(battleengine.DamageAppliedEvent); ok && applied.SkillID == testID("skill-use-ability-attack") {
			damage += applied.Amount
		}
	}
	return damage, resolved.State
}
