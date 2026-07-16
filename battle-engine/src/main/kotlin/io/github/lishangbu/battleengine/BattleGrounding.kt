package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleRuleSnapshot

/** 返回叠加当前携带道具后的接地事实。 */
fun BattleParticipant.isEffectivelyGrounded(): Boolean = when {
	itemEffects.any { it is BattleItemEffect.GroundingOverride } -> true
	itemEffects.any { it is BattleItemEffect.AirborneUntilDamaged } -> false
	else -> grounded
}

/** 计算伤害技能在接地和免疫失效道具修正后的属性相性。 */
internal fun effectiveTypeEffectiveness(
	rules: BattleRuleSnapshot,
	attackingElementId: Long,
	actor: BattleParticipant,
	target: BattleParticipant,
): Double {
	val airborneByItem = target.itemEffects.any { it is BattleItemEffect.AirborneUntilDamaged }
	val forcedGrounded = target.itemEffects.any { it is BattleItemEffect.GroundingOverride }
	if (attackingElementId == rules.elementId("ground") && airborneByItem && !forcedGrounded) {
		return 0.0
	}
	val raw = rules.elementChart.multiplier(attackingElementId, target.elementIds)
	if (raw != 0.0) {
		return raw
	}
	val ignoresTypeImmunity = target.itemEffects.any { it is BattleItemEffect.TypeImmunitySuppression } ||
		actor.abilityEffects.filterIsInstance<BattleAbilityEffect.ElementTypeImmunityBypass>()
			.any { attackingElementId in it.elementIds }
	val forcedGroundedAgainstGroundSkill = attackingElementId == rules.elementId("ground") &&
		target.itemEffects.any { it is BattleItemEffect.GroundingOverride }
	return if (ignoresTypeImmunity || forcedGroundedAgainstGroundSkill) {
		rules.elementChart.multiplierIgnoringImmunity(attackingElementId, target.elementIds)
	} else {
		0.0
	}
}
