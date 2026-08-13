package battleengine

// contactDamageAbilityModifier 返回目标特性对有效接触伤害施加的最终倍率。
//
// 这个入口只处理目标侧的承受接触伤害减半规则：接触事实必须由 skillMakesEffectiveContact 统一给出，
// 因而接触抑制特性会同时影响保护穿透与此处的减伤。攻击方无视目标特性时必须跳过该防守规则；静态
// 非接触技能、变化技能和无伤害结算不会调用普通伤害公式，故不会误触发。
func contactDamageAbilityModifier(attacker, defender MemberSnapshot, skill SkillSnapshot) (uint64, uint64) {
	if defender.ReceivedContactDamageHalved && !ignoresTargetAbilityEffects(attacker, skill) && skillMakesEffectiveContact(attacker, skill) {
		return 1, 2
	}
	return 1, 1
}

// fireDamageAbilityModifier 返回目标特性对当前有效火属性伤害施加的最终倍率。
//
// 属性比较只使用 RuleSnapshot 中冻结的 fire Identifier 和已经完成天气等改写的有效技能属性，绝不写死数据库主键或
// 显示名称。该规则同样是目标侧防守特性，所以攻击方无视目标特性时必须跳过。
func fireDamageAbilityModifier(
	rules RuleSnapshot,
	attacker, defender MemberSnapshot,
	skill SkillSnapshot,
	skillElementID Identifier,
) (uint64, uint64) {
	fireElementID := rules.ElementIDs["fire"]
	if defender.ReceivedFireDamageDoubled && fireElementID != 0 && skillElementID == fireElementID && !ignoresTargetAbilityEffects(attacker, skill) {
		return 2, 1
	}
	return 1, 1
}
