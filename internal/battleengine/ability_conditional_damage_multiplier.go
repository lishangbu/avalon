package battleengine

import "strings"

// TargetGenderDamageMultiplier 描述按攻击方与目标性别关系修正最终伤害的规则。
type TargetGenderDamageMultiplier struct {
	// SameGenderNumerator 是双方均有性别且性别相同时使用的正分子。
	SameGenderNumerator uint16 `json:"sameGenderNumerator"`
	// SameGenderDenominator 是双方均有性别且性别相同时使用的正分母。
	SameGenderDenominator uint16 `json:"sameGenderDenominator"`
	// OppositeGenderNumerator 是双方均有性别且性别不同时使用的正分子。
	OppositeGenderNumerator uint16 `json:"oppositeGenderNumerator"`
	// OppositeGenderDenominator 是双方均有性别且性别不同时使用的正分母。
	OppositeGenderDenominator uint16 `json:"oppositeGenderDenominator"`
}

// PunchBasedSkillDamageBoost 描述拳击类普通直接伤害技能的最终伤害倍率。
type PunchBasedSkillDamageBoost struct {
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// SlicingBasedSkillDamageBoost 描述切割类普通直接伤害技能的最终伤害倍率。
type SlicingBasedSkillDamageBoost struct {
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// SoundBasedSkillDamageBoost 描述声音类普通直接伤害技能的最终伤害倍率。
type SoundBasedSkillDamageBoost struct {
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// PulseBasedSkillDamageBoost 描述波动类普通直接伤害技能的最终伤害倍率。
type PulseBasedSkillDamageBoost struct {
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// BiteBasedSkillDamageBoost 描述啃咬类普通直接伤害技能的最终伤害倍率。
type BiteBasedSkillDamageBoost struct {
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// SecondaryEffectsSuppressedDamageBoost 描述以移除伤害技能附加异常和能力变化换取的最终伤害倍率。
type SecondaryEffectsSuppressedDamageBoost struct {
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// SoundBasedSkillDamageReduction 描述防守方承受声音类普通直接伤害时的最终伤害倍率。
type SoundBasedSkillDamageReduction struct {
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// SuperEffectiveDamageReduction 描述防守方承受严格克制直接伤害时的最终伤害倍率。
type SuperEffectiveDamageReduction struct {
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// FullHPDamageReduction 描述防守方每段伤害开始时仍处于满生命状态时的最终伤害倍率。
type FullHPDamageReduction struct {
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// DamageClassDamageReduction 描述防守方承受指定伤害分类技能时的最终伤害倍率。
type DamageClassDamageReduction struct {
	// DamageClasses 是允许触发的非空物理或特殊伤害分类集合。
	DamageClasses []DamageClass `json:"damageClasses"`
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// ElementSkillDamageReduction 描述防守方承受指定有效属性技能时的最终伤害倍率。
type ElementSkillDamageReduction struct {
	// ElementIDs 是允许触发的非空、无重复稳定属性 Identifier 集合。
	ElementIDs []Identifier `json:"elementIds"`
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// ContactBasedSkillDamageReduction 描述防守方承受本次仍构成有效接触的技能时的最终伤害倍率。
type ContactBasedSkillDamageReduction struct {
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// AllySkillDamageBoost 描述持有者为当前上场伙伴提供的伤害分类最终倍率。
type AllySkillDamageBoost struct {
	// DamageClasses 是允许触发的非空物理或特殊伤害分类集合。
	DamageClasses []DamageClass `json:"damageClasses"`
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// AllyReceivedDamageReduction 描述持有者为当前上场伙伴提供的公式伤害最终倍率。
type AllyReceivedDamageReduction struct {
	// Numerator 是精确倍率的正整数分子。
	Numerator uint16 `json:"numerator"`
	// Denominator 是精确倍率的正整数分母。
	Denominator uint16 `json:"denominator"`
}

// abilityDamageFraction 保存伙伴光环在本次单段公式中已经匹配的精确正分数。
type abilityDamageFraction struct {
	// numerator 是倍率分子。
	numerator uint16
	// denominator 是倍率分母。
	denominator uint16
}

// abilityAllyDamageModifiers 保存一次伤害命中从双方当前上场伙伴收集的全部光环事实。
type abilityAllyDamageModifiers struct {
	// skillDamageBoosts 是攻击方伙伴按伤害分类匹配的全部最终增伤分数。
	skillDamageBoosts []abilityDamageFraction
	// receivedDamageReductions 是防守方伙伴提供的全部公式承伤分数。
	receivedDamageReductions []abilityDamageFraction
	// attackingStatMultiplier 是攻击方自身因匹配互助组伙伴在场而激活的能力倍率；nil 表示未激活。
	attackingStatMultiplier *AllyAbilityPresenceAttackingStatMultiplier
}

// activeAllyDamageModifiers 从权威场上成员快照收集伙伴增伤、伙伴减伤和互助组能力倍率。
func activeAllyDamageModifiers(state State, actorSlot, targetSlot SlotRef, skill SkillSnapshot) abilityAllyDamageModifiers {
	result := abilityAllyDamageModifiers{}
	actor, actorFound := state.ActiveMember(actorSlot)
	if actorFound {
		for _, ally := range activeLivingAllies(state, actorSlot) {
			if value := ally.AllySkillDamageBoost; value != nil && containsDamageClass(value.DamageClasses, skill.DamageClass) {
				result.skillDamageBoosts = append(result.skillDamageBoosts, abilityDamageFraction{value.Numerator, value.Denominator})
			}
		}
		if value := actor.AllyAbilityPresenceAttackingStatMultiplier; value != nil {
			for _, ally := range activeLivingAllies(state, actorSlot) {
				if ally.AllyAbilityGroupCode == value.GroupCode {
					result.attackingStatMultiplier = cloneAllyAbilityPresenceAttackingStatMultiplier(value)
					break
				}
			}
		}
	}
	for _, ally := range activeLivingAllies(state, targetSlot) {
		if value := ally.AllyReceivedDamageReduction; value != nil {
			result.receivedDamageReductions = append(result.receivedDamageReductions, abilityDamageFraction{value.Numerator, value.Denominator})
		}
	}
	return result
}

// activeLivingAllies 返回指定场上槽位同侧的其它存活上场成员，并保持阵营冻结槽位顺序。
func activeLivingAllies(state State, holderSlot SlotRef) []MemberSnapshot {
	holder, holderFound := state.ActiveMember(holderSlot)
	if !holderFound {
		return nil
	}
	for _, side := range state.sides {
		if side.Side != holderSlot.Side {
			continue
		}
		result := make([]MemberSnapshot, 0, len(side.ActiveMembers)-1)
		for _, position := range side.ActiveMembers {
			if position == holder.Position {
				continue
			}
			member, found := state.member(side.Side, position)
			if found && member.CurrentHP > 0 {
				result = append(result, member)
			}
		}
		return result
	}
	return nil
}

// containsDamageClass 报告封闭伤害分类集合是否包含目标值。
func containsDamageClass(values []DamageClass, target DamageClass) bool {
	for _, value := range values {
		if value == target {
			return true
		}
	}
	return false
}

// validAbilityConditionalDamageMultipliers 校验条件增伤、防守减伤和伙伴光环规则。
func validAbilityConditionalDamageMultipliers(member MemberSnapshot) bool {
	return validTargetGenderDamageMultiplier(member.TargetGenderDamageMultiplier) &&
		validConditionalDamageFraction(member.PunchBasedSkillDamageBoost) &&
		validConditionalDamageFraction(member.SlicingBasedSkillDamageBoost) &&
		validConditionalDamageFraction(member.SoundBasedSkillDamageBoost) &&
		validConditionalDamageFraction(member.PulseBasedSkillDamageBoost) &&
		validConditionalDamageFraction(member.BiteBasedSkillDamageBoost) &&
		validConditionalDamageFraction(member.SecondaryEffectsSuppressedDamageBoost) &&
		validConditionalDamageFraction(member.SoundBasedSkillDamageReduction) &&
		validConditionalDamageFraction(member.SuperEffectiveDamageReduction) &&
		validConditionalDamageFraction(member.FullHPDamageReduction) &&
		validDamageClassDamageReduction(member.DamageClassDamageReduction) &&
		validElementSkillDamageReduction(member.ElementSkillDamageReduction) &&
		validConditionalDamageFraction(member.ContactBasedSkillDamageReduction) &&
		validAllySkillDamageBoost(member.AllySkillDamageBoost) &&
		validConditionalDamageFraction(member.AllyReceivedDamageReduction) &&
		validAllyAbilityGroupCode(member.AllyAbilityGroupCode)
}

// conditionalDamageFraction 是只供校验独立无条件分数规则使用的封闭类型集合。
type conditionalDamageFraction interface {
	PunchBasedSkillDamageBoost | SlicingBasedSkillDamageBoost | SoundBasedSkillDamageBoost |
		PulseBasedSkillDamageBoost | BiteBasedSkillDamageBoost | SecondaryEffectsSuppressedDamageBoost |
		SoundBasedSkillDamageReduction | SuperEffectiveDamageReduction | FullHPDamageReduction |
		ContactBasedSkillDamageReduction | AllyReceivedDamageReduction
}

// validConditionalDamageFraction 校验可选规则是否包含完整正分数。
func validConditionalDamageFraction[T conditionalDamageFraction](value *T) bool {
	if value == nil {
		return true
	}
	return conditionalDamageFractionValues(*value)
}

// conditionalDamageFractionValues 从封闭独立类型中读取统一的数值不变量。
func conditionalDamageFractionValues[T conditionalDamageFraction](value T) bool {
	switch fraction := any(value).(type) {
	case PunchBasedSkillDamageBoost:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case SlicingBasedSkillDamageBoost:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case SoundBasedSkillDamageBoost:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case PulseBasedSkillDamageBoost:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case BiteBasedSkillDamageBoost:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case SecondaryEffectsSuppressedDamageBoost:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case SoundBasedSkillDamageReduction:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case SuperEffectiveDamageReduction:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case FullHPDamageReduction:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case ContactBasedSkillDamageReduction:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	case AllyReceivedDamageReduction:
		return fraction.Numerator != 0 && fraction.Denominator != 0
	default:
		return false
	}
}

// validTargetGenderDamageMultiplier 校验同性与异性两组独立正分数。
func validTargetGenderDamageMultiplier(value *TargetGenderDamageMultiplier) bool {
	return value == nil || value.SameGenderNumerator != 0 && value.SameGenderDenominator != 0 &&
		value.OppositeGenderNumerator != 0 && value.OppositeGenderDenominator != 0
}

// validDamageClassDamageReduction 校验防守减伤只引用物理或特殊分类且不重复。
func validDamageClassDamageReduction(value *DamageClassDamageReduction) bool {
	return value == nil || validDamagingClasses(value.DamageClasses) && value.Numerator != 0 && value.Denominator != 0
}

// validElementSkillDamageReduction 校验防守属性集合及完整正分数。
func validElementSkillDamageReduction(value *ElementSkillDamageReduction) bool {
	return value == nil || validDamageBoostElementIDs(value.ElementIDs) && value.Numerator != 0 && value.Denominator != 0
}

// validAllySkillDamageBoost 校验伙伴增伤的伤害分类和正分数。
func validAllySkillDamageBoost(value *AllySkillDamageBoost) bool {
	return value == nil || validDamagingClasses(value.DamageClasses) && value.Numerator != 0 && value.Denominator != 0
}

// validDamagingClasses 校验集合非空、只含物理或特殊且不重复。
func validDamagingClasses(values []DamageClass) bool {
	if len(values) == 0 || len(values) > 2 {
		return false
	}
	seen := make(map[DamageClass]struct{}, len(values))
	for _, value := range values {
		if value != DamageClassPhysical && value != DamageClassSpecial {
			return false
		}
		if _, duplicate := seen[value]; duplicate {
			return false
		}
		seen[value] = struct{}{}
	}
	return true
}

// validAllyAbilityGroupCode 校验互助组代码为空或为有界、已去除首尾空白的稳定标识。
func validAllyAbilityGroupCode(value string) bool {
	return value == "" || len(value) <= 64 && strings.TrimSpace(value) == value
}

// skillHasSecondaryStatusOrStatEffects 报告伤害技能是否携带可被强行类特性移除的附加效果。
func skillHasSecondaryStatusOrStatEffects(skill SkillSnapshot) bool {
	return skill.DamageClass != DamageClassStatus &&
		(len(skill.StatusApplications) != 0 || len(skill.StatStageEffects) != 0 || len(skill.VolatileStatusApplications) != 0)
}

// cloneTargetGenderDamageMultiplier 深复制可选性别关系伤害倍率。
func cloneTargetGenderDamageMultiplier(value *TargetGenderDamageMultiplier) *TargetGenderDamageMultiplier {
	return cloneValue(value)
}

// clonePunchBasedSkillDamageBoost 深复制可选拳击类伤害强化。
func clonePunchBasedSkillDamageBoost(value *PunchBasedSkillDamageBoost) *PunchBasedSkillDamageBoost {
	return cloneValue(value)
}

// cloneSlicingBasedSkillDamageBoost 深复制可选切割类伤害强化。
func cloneSlicingBasedSkillDamageBoost(value *SlicingBasedSkillDamageBoost) *SlicingBasedSkillDamageBoost {
	return cloneValue(value)
}

// cloneSoundBasedSkillDamageBoost 深复制可选声音类伤害强化。
func cloneSoundBasedSkillDamageBoost(value *SoundBasedSkillDamageBoost) *SoundBasedSkillDamageBoost {
	return cloneValue(value)
}

// clonePulseBasedSkillDamageBoost 深复制可选波动类伤害强化。
func clonePulseBasedSkillDamageBoost(value *PulseBasedSkillDamageBoost) *PulseBasedSkillDamageBoost {
	return cloneValue(value)
}

// cloneBiteBasedSkillDamageBoost 深复制可选啃咬类伤害强化。
func cloneBiteBasedSkillDamageBoost(value *BiteBasedSkillDamageBoost) *BiteBasedSkillDamageBoost {
	return cloneValue(value)
}

// cloneSecondaryEffectsSuppressedDamageBoost 深复制可选附加效果抑制伤害强化。
func cloneSecondaryEffectsSuppressedDamageBoost(value *SecondaryEffectsSuppressedDamageBoost) *SecondaryEffectsSuppressedDamageBoost {
	return cloneValue(value)
}

// cloneSoundBasedSkillDamageReduction 深复制可选声音类伤害减免。
func cloneSoundBasedSkillDamageReduction(value *SoundBasedSkillDamageReduction) *SoundBasedSkillDamageReduction {
	return cloneValue(value)
}

// cloneSuperEffectiveDamageReduction 深复制可选克制伤害减免。
func cloneSuperEffectiveDamageReduction(value *SuperEffectiveDamageReduction) *SuperEffectiveDamageReduction {
	return cloneValue(value)
}

// cloneFullHPDamageReduction 深复制可选满生命伤害减免。
func cloneFullHPDamageReduction(value *FullHPDamageReduction) *FullHPDamageReduction {
	return cloneValue(value)
}

// cloneDamageClassDamageReduction 深复制可选伤害分类减免及其分类集合。
func cloneDamageClassDamageReduction(value *DamageClassDamageReduction) *DamageClassDamageReduction {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.DamageClasses = append([]DamageClass(nil), value.DamageClasses...)
	return &cloned
}

// cloneElementSkillDamageReduction 深复制可选属性技能减免及其属性集合。
func cloneElementSkillDamageReduction(value *ElementSkillDamageReduction) *ElementSkillDamageReduction {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.ElementIDs = append([]Identifier(nil), value.ElementIDs...)
	return &cloned
}

// cloneContactBasedSkillDamageReduction 深复制可选接触技能伤害减免。
func cloneContactBasedSkillDamageReduction(value *ContactBasedSkillDamageReduction) *ContactBasedSkillDamageReduction {
	return cloneValue(value)
}

// cloneAllySkillDamageBoost 深复制可选伙伴伤害强化及其分类集合。
func cloneAllySkillDamageBoost(value *AllySkillDamageBoost) *AllySkillDamageBoost {
	if value == nil {
		return nil
	}
	cloned := *value
	cloned.DamageClasses = append([]DamageClass(nil), value.DamageClasses...)
	return &cloned
}

// cloneAllyReceivedDamageReduction 深复制可选伙伴承伤减免。
func cloneAllyReceivedDamageReduction(value *AllyReceivedDamageReduction) *AllyReceivedDamageReduction {
	return cloneValue(value)
}
