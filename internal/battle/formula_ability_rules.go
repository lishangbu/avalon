package battle

import "github.com/lishangbu/avalon/internal/battleengine"

// cloneFormulaRule 复制不含引用字段的可选规则；含集合字段的规则由调用方继续复制集合。
func cloneFormulaRule[T any](value *T) *T {
	if value == nil {
		return nil
	}
	cloned := *value
	return &cloned
}

// cloneFormulaDamageClassReduction 深复制伤害分类防守倍率及其分类集合。
func cloneFormulaDamageClassReduction(value *battleengine.DamageClassDamageReduction) *battleengine.DamageClassDamageReduction {
	cloned := cloneFormulaRule(value)
	if cloned != nil {
		cloned.DamageClasses = append([]battleengine.DamageClass(nil), value.DamageClasses...)
	}
	return cloned
}

// cloneFormulaElementReduction 深复制有效属性防守倍率及其稳定属性身份集合。
func cloneFormulaElementReduction(value *battleengine.ElementSkillDamageReduction) *battleengine.ElementSkillDamageReduction {
	cloned := cloneFormulaRule(value)
	if cloned != nil {
		cloned.ElementIDs = append([]battleengine.Identifier(nil), value.ElementIDs...)
	}
	return cloned
}

// cloneFormulaAttackingStat 深复制条件攻击能力倍率及其主要异常集合。
func cloneFormulaAttackingStat(value *battleengine.AttackingStatMultiplier) *battleengine.AttackingStatMultiplier {
	cloned := cloneFormulaRule(value)
	if cloned != nil {
		cloned.RequiredMajorStatuses = append([]battleengine.MajorStatus(nil), value.RequiredMajorStatuses...)
	}
	return cloned
}

// cloneFormulaAllySkillBoost 深复制伙伴分类增伤倍率及其分类集合。
func cloneFormulaAllySkillBoost(value *battleengine.AllySkillDamageBoost) *battleengine.AllySkillDamageBoost {
	cloned := cloneFormulaRule(value)
	if cloned != nil {
		cloned.DamageClasses = append([]battleengine.DamageClass(nil), value.DamageClasses...)
	}
	return cloned
}
