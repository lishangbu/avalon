package abilitydetail

import (
	"strings"

	"github.com/lishangbu/avalon/internal/battleengine"
)

// TargetGenderDamageMultiplier 是按攻击方与目标性别关系选择最终伤害倍率的独立资料。
type TargetGenderDamageMultiplier = battleengine.TargetGenderDamageMultiplier

// PunchBasedSkillDamageBoost 是拳击类普通公式伤害的独立最终倍率资料。
type PunchBasedSkillDamageBoost = battleengine.PunchBasedSkillDamageBoost

// SlicingBasedSkillDamageBoost 是切割类普通公式伤害的独立最终倍率资料。
type SlicingBasedSkillDamageBoost = battleengine.SlicingBasedSkillDamageBoost

// SoundBasedSkillDamageBoost 是声音类普通公式伤害的独立攻击方倍率资料。
type SoundBasedSkillDamageBoost = battleengine.SoundBasedSkillDamageBoost

// PulseBasedSkillDamageBoost 是波动类普通伤害及目标回复共享的独立倍率资料。
type PulseBasedSkillDamageBoost = battleengine.PulseBasedSkillDamageBoost

// BiteBasedSkillDamageBoost 是啃咬类普通公式伤害的独立最终倍率资料。
type BiteBasedSkillDamageBoost = battleengine.BiteBasedSkillDamageBoost

// SecondaryEffectsSuppressedDamageBoost 是抑制追加效果后提供最终伤害倍率的独立资料。
type SecondaryEffectsSuppressedDamageBoost = battleengine.SecondaryEffectsSuppressedDamageBoost

// SoundBasedSkillDamageReduction 是承受声音类普通公式伤害时使用的独立防守倍率资料。
type SoundBasedSkillDamageReduction = battleengine.SoundBasedSkillDamageReduction

// SuperEffectiveDamageReduction 是承受严格克制普通公式伤害时使用的独立防守倍率资料。
type SuperEffectiveDamageReduction = battleengine.SuperEffectiveDamageReduction

// FullHPDamageReduction 是每段伤害开始时满生命触发的独立防守倍率资料。
type FullHPDamageReduction = battleengine.FullHPDamageReduction

// DamageClassDamageReduction 是按物理或特殊分类匹配的独立防守倍率资料。
type DamageClassDamageReduction = battleengine.DamageClassDamageReduction

// ElementSkillDamageReduction 是按技能当前有效属性集合匹配的独立防守倍率资料。
type ElementSkillDamageReduction = battleengine.ElementSkillDamageReduction

// ContactBasedSkillDamageReduction 是按本次有效接触事实匹配的独立防守倍率资料。
type ContactBasedSkillDamageReduction = battleengine.ContactBasedSkillDamageReduction

// AttackingStatMultiplier 是持有成员攻击侧公式能力的独立条件倍率资料。
type AttackingStatMultiplier = battleengine.AttackingStatMultiplier

// OpponentAttackingStatMultiplier 是防守方特性修正攻击者公式能力的独立倍率资料。
type OpponentAttackingStatMultiplier = battleengine.OpponentAttackingStatMultiplier

// DefendingStatMultiplier 是持有成员防守侧公式能力的独立条件倍率资料。
type DefendingStatMultiplier = battleengine.DefendingStatMultiplier

// OpponentDefendingStatMultiplier 是攻击方特性修正目标公式防守能力的独立倍率资料。
type OpponentDefendingStatMultiplier = battleengine.OpponentDefendingStatMultiplier

// AllySkillDamageBoost 是持有者为其它存活上场伙伴提供的分类伤害倍率资料。
type AllySkillDamageBoost = battleengine.AllySkillDamageBoost

// AllyReceivedDamageReduction 是持有者为其它存活上场伙伴提供的承伤倍率资料。
type AllyReceivedDamageReduction = battleengine.AllyReceivedDamageReduction

// AllyAbilityPresenceAttackingStatMultiplier 是匹配互助组伙伴在场时启用的攻击能力倍率资料。
type AllyAbilityPresenceAttackingStatMultiplier = battleengine.AllyAbilityPresenceAttackingStatMultiplier

// cloneFormulaMultiplierValues 深复制规则 112—131 的全部独立资料字段和内部集合。
func cloneFormulaMultiplierValues(values OptionalValues) OptionalValues {
	values.TargetGenderDamageMultiplier = cloneRule(values.TargetGenderDamageMultiplier)
	values.PunchBasedSkillDamageBoost = cloneRule(values.PunchBasedSkillDamageBoost)
	values.SlicingBasedSkillDamageBoost = cloneRule(values.SlicingBasedSkillDamageBoost)
	values.SoundBasedSkillDamageBoost = cloneRule(values.SoundBasedSkillDamageBoost)
	values.PulseBasedSkillDamageBoost = cloneRule(values.PulseBasedSkillDamageBoost)
	values.BiteBasedSkillDamageBoost = cloneRule(values.BiteBasedSkillDamageBoost)
	values.SecondaryEffectsSuppressedDamageBoost = cloneRule(values.SecondaryEffectsSuppressedDamageBoost)
	values.SoundBasedSkillDamageReduction = cloneRule(values.SoundBasedSkillDamageReduction)
	values.SuperEffectiveDamageReduction = cloneRule(values.SuperEffectiveDamageReduction)
	values.FullHPDamageReduction = cloneRule(values.FullHPDamageReduction)
	values.DamageClassDamageReduction = cloneRule(values.DamageClassDamageReduction)
	if values.DamageClassDamageReduction != nil {
		values.DamageClassDamageReduction.DamageClasses = append([]battleengine.DamageClass(nil), values.DamageClassDamageReduction.DamageClasses...)
	}
	values.ElementSkillDamageReduction = cloneRule(values.ElementSkillDamageReduction)
	if values.ElementSkillDamageReduction != nil {
		values.ElementSkillDamageReduction.ElementIDs = append([]battleengine.Identifier(nil), values.ElementSkillDamageReduction.ElementIDs...)
	}
	values.ContactBasedSkillDamageReduction = cloneRule(values.ContactBasedSkillDamageReduction)
	values.AttackingStatMultiplier = cloneRule(values.AttackingStatMultiplier)
	if values.AttackingStatMultiplier != nil {
		values.AttackingStatMultiplier.RequiredMajorStatuses = append([]battleengine.MajorStatus(nil), values.AttackingStatMultiplier.RequiredMajorStatuses...)
	}
	values.OpponentAttackingStatMultiplier = cloneRule(values.OpponentAttackingStatMultiplier)
	values.DefendingStatMultiplier = cloneRule(values.DefendingStatMultiplier)
	values.OpponentDefendingStatMultiplier = cloneRule(values.OpponentDefendingStatMultiplier)
	values.AllySkillDamageBoost = cloneRule(values.AllySkillDamageBoost)
	if values.AllySkillDamageBoost != nil {
		values.AllySkillDamageBoost.DamageClasses = append([]battleengine.DamageClass(nil), values.AllySkillDamageBoost.DamageClasses...)
	}
	values.AllyReceivedDamageReduction = cloneRule(values.AllyReceivedDamageReduction)
	values.AllyAbilityGroupCode = strings.TrimSpace(values.AllyAbilityGroupCode)
	values.AllyAbilityPresenceAttackingStatMultiplier = cloneRule(values.AllyAbilityPresenceAttackingStatMultiplier)
	if values.AllyAbilityPresenceAttackingStatMultiplier != nil {
		values.AllyAbilityPresenceAttackingStatMultiplier.GroupCode = strings.TrimSpace(values.AllyAbilityPresenceAttackingStatMultiplier.GroupCode)
	}
	return values
}

// cloneRule 复制不含切片、映射或指针字段的可选规则；含集合的规则由调用方继续复制集合。
func cloneRule[T any](value *T) *T {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// validFormulaMultiplierValues 校验规则 112—131 的正分数、集合、能力项、环境和互助组约束。
func validFormulaMultiplierValues(values OptionalValues) bool {
	return validGenderMultiplier(values.TargetGenderDamageMultiplier) &&
		validSimpleMultiplier(values.PunchBasedSkillDamageBoost) && validSimpleMultiplier(values.SlicingBasedSkillDamageBoost) &&
		validSimpleMultiplier(values.SoundBasedSkillDamageBoost) && validSimpleMultiplier(values.PulseBasedSkillDamageBoost) &&
		validSimpleMultiplier(values.BiteBasedSkillDamageBoost) && validSimpleMultiplier(values.SecondaryEffectsSuppressedDamageBoost) &&
		validSimpleMultiplier(values.SoundBasedSkillDamageReduction) && validSimpleMultiplier(values.SuperEffectiveDamageReduction) &&
		validSimpleMultiplier(values.FullHPDamageReduction) && validDamageClasses(values.DamageClassDamageReduction) &&
		validElementReduction(values.ElementSkillDamageReduction) && validSimpleMultiplier(values.ContactBasedSkillDamageReduction) &&
		validAttackingStatMultiplier(values.AttackingStatMultiplier) && validOpponentAttackingStatMultiplier(values.OpponentAttackingStatMultiplier) &&
		validDefendingStatMultiplier(values.DefendingStatMultiplier) && validOpponentDefendingStatMultiplier(values.OpponentDefendingStatMultiplier) &&
		validDamageClasses(values.AllySkillDamageBoost) && validSimpleMultiplier(values.AllyReceivedDamageReduction) &&
		validAbilityGroupCode(values.AllyAbilityGroupCode, true) && validAllyPresenceMultiplier(values.AllyAbilityPresenceAttackingStatMultiplier)
}

type simpleMultiplier interface {
	*battleengine.PunchBasedSkillDamageBoost | *battleengine.SlicingBasedSkillDamageBoost |
		*battleengine.SoundBasedSkillDamageBoost | *battleengine.PulseBasedSkillDamageBoost |
		*battleengine.BiteBasedSkillDamageBoost | *battleengine.SecondaryEffectsSuppressedDamageBoost |
		*battleengine.SoundBasedSkillDamageReduction | *battleengine.SuperEffectiveDamageReduction |
		*battleengine.FullHPDamageReduction | *battleengine.ContactBasedSkillDamageReduction |
		*battleengine.AllyReceivedDamageReduction
}

func validSimpleMultiplier[T simpleMultiplier](value T) bool {
	if value == nil {
		return true
	}
	switch rule := any(value).(type) {
	case *battleengine.PunchBasedSkillDamageBoost:
		return validPositiveFraction(rule.Numerator, rule.Denominator)
	case *battleengine.SlicingBasedSkillDamageBoost:
		return validPositiveFraction(rule.Numerator, rule.Denominator)
	case *battleengine.SoundBasedSkillDamageBoost:
		return validPositiveFraction(rule.Numerator, rule.Denominator)
	case *battleengine.PulseBasedSkillDamageBoost:
		return validPositiveFraction(rule.Numerator, rule.Denominator)
	case *battleengine.BiteBasedSkillDamageBoost:
		return validPositiveFraction(rule.Numerator, rule.Denominator)
	case *battleengine.SecondaryEffectsSuppressedDamageBoost:
		return validPositiveFraction(rule.Numerator, rule.Denominator)
	case *battleengine.SoundBasedSkillDamageReduction:
		return validPositiveFraction(rule.Numerator, rule.Denominator)
	case *battleengine.SuperEffectiveDamageReduction:
		return validPositiveFraction(rule.Numerator, rule.Denominator)
	case *battleengine.FullHPDamageReduction:
		return validPositiveFraction(rule.Numerator, rule.Denominator)
	case *battleengine.ContactBasedSkillDamageReduction:
		return validPositiveFraction(rule.Numerator, rule.Denominator)
	case *battleengine.AllyReceivedDamageReduction:
		return validPositiveFraction(rule.Numerator, rule.Denominator)
	default:
		return false
	}
}

func validGenderMultiplier(value *TargetGenderDamageMultiplier) bool {
	return value == nil || validPositiveFraction(value.SameGenderNumerator, value.SameGenderDenominator) &&
		validPositiveFraction(value.OppositeGenderNumerator, value.OppositeGenderDenominator)
}

type damageClassMultiplier interface {
	*battleengine.DamageClassDamageReduction | *battleengine.AllySkillDamageBoost
}

func validDamageClasses[T damageClassMultiplier](value T) bool {
	if value == nil {
		return true
	}
	var classes []battleengine.DamageClass
	var numerator, denominator uint16
	switch rule := any(value).(type) {
	case *battleengine.DamageClassDamageReduction:
		classes, numerator, denominator = rule.DamageClasses, rule.Numerator, rule.Denominator
	case *battleengine.AllySkillDamageBoost:
		classes, numerator, denominator = rule.DamageClasses, rule.Numerator, rule.Denominator
	}
	if len(classes) == 0 || !validPositiveFraction(numerator, denominator) {
		return false
	}
	seen := map[battleengine.DamageClass]struct{}{}
	for _, class := range classes {
		if class != battleengine.DamageClassPhysical && class != battleengine.DamageClassSpecial {
			return false
		}
		if _, duplicate := seen[class]; duplicate {
			return false
		}
		seen[class] = struct{}{}
	}
	return true
}

func validElementReduction(value *ElementSkillDamageReduction) bool {
	if value == nil {
		return true
	}
	if len(value.ElementIDs) == 0 || !validPositiveFraction(value.Numerator, value.Denominator) {
		return false
	}
	seen := map[battleengine.Identifier]struct{}{}
	for _, id := range value.ElementIDs {
		if !id.IsValid() {
			return false
		}
		if _, duplicate := seen[id]; duplicate {
			return false
		}
		seen[id] = struct{}{}
	}
	return true
}

func validAttackingStatMultiplier(value *AttackingStatMultiplier) bool {
	if value == nil {
		return true
	}
	if !validAttackStat(value.Stat) || !validPositiveFraction(value.Numerator, value.Denominator) ||
		!validFormulaWeather(value.RequiredWeather) || !validFormulaTerrain(value.RequiredTerrain) ||
		(value.MaximumHPNumerator == 0) != (value.MaximumHPDenominator == 0) ||
		value.MaximumHPDenominator != 0 && value.MaximumHPNumerator > value.MaximumHPDenominator ||
		value.IgnoreBurnAttackReduction && value.Stat != battleengine.StatAttack {
		return false
	}
	seen := map[battleengine.MajorStatus]struct{}{}
	for _, status := range value.RequiredMajorStatuses {
		if !status.Valid() {
			return false
		}
		if _, duplicate := seen[status]; duplicate {
			return false
		}
		seen[status] = struct{}{}
	}
	return true
}

func validOpponentAttackingStatMultiplier(value *OpponentAttackingStatMultiplier) bool {
	return value == nil || validAttackStat(value.Stat) && validPositiveFraction(value.Numerator, value.Denominator)
}

func validDefendingStatMultiplier(value *DefendingStatMultiplier) bool {
	return value == nil || validDefenseStat(value.Stat) && validPositiveFraction(value.Numerator, value.Denominator) && validFormulaTerrain(value.RequiredTerrain)
}

func validOpponentDefendingStatMultiplier(value *OpponentDefendingStatMultiplier) bool {
	return value == nil || validDefenseStat(value.Stat) && validPositiveFraction(value.Numerator, value.Denominator)
}

func validAllyPresenceMultiplier(value *AllyAbilityPresenceAttackingStatMultiplier) bool {
	return value == nil || validAbilityGroupCode(value.GroupCode, false) && validAttackStat(value.Stat) &&
		validPositiveFraction(value.Numerator, value.Denominator)
}

func validPositiveFraction(numerator, denominator uint16) bool {
	return numerator != 0 && denominator != 0
}

func validAttackStat(value battleengine.Stat) bool {
	return value == battleengine.StatAttack || value == battleengine.StatSpecialAttack
}

func validDefenseStat(value battleengine.Stat) bool {
	return value == battleengine.StatDefense || value == battleengine.StatSpecialDefense
}

func validFormulaWeather(value battleengine.WeatherKind) bool {
	return value == "" || value == battleengine.WeatherKindSun || value == battleengine.WeatherKindRain ||
		value == battleengine.WeatherKindSandstorm || value == battleengine.WeatherKindSnow
}

func validFormulaTerrain(value battleengine.TerrainKind) bool {
	return value == "" || value == battleengine.TerrainKindElectric || value == battleengine.TerrainKindGrassy ||
		value == battleengine.TerrainKindMisty || value == battleengine.TerrainKindPsychic
}

func validAbilityGroupCode(value string, emptyAllowed bool) bool {
	if value == "" {
		return emptyAllowed
	}
	if len(value) > 64 || strings.TrimSpace(value) != value || value[0] < 'a' || value[0] > 'z' {
		return false
	}
	for index := 1; index < len(value); index++ {
		character := value[index]
		if character != '-' && (character < 'a' || character > 'z') && (character < '0' || character > '9') {
			return false
		}
	}
	return true
}
