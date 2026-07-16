package io.github.lishangbu.battleengine.model

/**
 * 判断技能在“本次由指定使用者发动”时是否仍然构成接触。
 *
 * [BattleSkillSlot.makesContact] 是资料层的静态标签，表示这个技能在没有动态规则介入时属于接触类技能。现代规则还
 * 存在少量会在发动瞬间改写接触事实的来源，例如携带拳击手套使用拳击类技能时，该次攻击不再被视为接触。把动态
 * 判断集中在这里，可以让保护绕过、接触反制特性、接触类伤害倍率和后续可能加入的接触减伤规则读取同一口径。
 *
 * 这个函数不处理“接触后副作用是否被免疫”。部位护具这类道具并不移除接触事实，只阻止攻击方因为接触目标而承受
 * 的反制效果；因此它应在接触副作用阶段单独判断，而不能让这里返回 false。否则会错误影响无形拳式保护绕过和接触
 * 类伤害倍率。
 */
fun BattleSkillSlot.makesEffectiveContact(user: BattleParticipant): Boolean {
	if (!makesContact) {
		return false
	}
	if (user.abilityEffects.any { it is BattleAbilityEffect.ContactSuppression }) {
		return false
	}
	return user.itemEffects.none { effect ->
		effect is BattleItemEffect.PunchBasedContactSuppression && punchBased
	}
}
