package io.github.lishangbu.battlerules.coverage

import io.github.lishangbu.battleengine.model.BattleSkillSlot

/** 根据已装配技能槽中的强类型效果定位共享运行时机制的纯引擎行为测试证据。 */
class BattleSkillBehaviorEvidence(
	private val testSourceIndex: BattleBehaviorTestSourceIndex,
) {
	/**
	 * 返回覆盖当前技能所用运行时机制的测试类集合。
	 *
	 * 证据粒度是效果类型或非默认分支，并不表示测试以当前技能编码和全部资料参数执行过；使用相同结构化机制的技能
	 * 会复用同一批引擎测试。
	 */
	fun classesFor(skillCode: String, effectPolicy: String, slot: BattleSkillSlot): Set<String> {
		val tokens = linkedSetOf<String>()
		addTypedEffect(tokens, slot.fixedDamage)
		addTypedEffect(tokens, slot.proportionalDamage)
		addTypedEffect(tokens, slot.hpDerivedDamage)
		addTypedEffect(tokens, slot.receivedDamage)
		addTypedEffect(tokens, slot.oneHitKnockOut)
		addTypedEffects(tokens, slot.conditionalPowerMultipliers)
		addTypedEffect(tokens, slot.dynamicPower)
		addTypedEffects(tokens, slot.statusApplications)
		addTypedEffects(tokens, slot.volatileStatusApplications)
		addTypedEffects(tokens, slot.statStageEffects)
		addTypedEffects(tokens, slot.statStageOperations)
		addTypedEffects(tokens, slot.sideConditionApplications)
		addTypedEffects(tokens, slot.sideSpeedModifierApplications)
		addTypedEffects(tokens, slot.sideEntryHazardApplications)
		addTypedEffects(tokens, slot.sideProtectionApplications)
		addTypedEffects(tokens, slot.fieldSpeedOrderApplications)
		addTypedEffects(tokens, slot.hpEffects)
		addTypedEffects(tokens, slot.postDamageStatusCures)
		addTypedEffects(tokens, slot.weightEffects)
		addTypedEffects(tokens, slot.environmentEffects)
		addNonDefaultFieldTokens(tokens, slot)
		if (effectPolicy == STANDARD_DAMAGE_POLICY) {
			tokens += STANDARD_DAMAGE_TOKEN
		}
		return testSourceIndex.classesContainingAny(tokens) +
			EXPLICIT_POLICY_EVIDENCE[effectPolicy].orEmpty() +
			EXPLICIT_SKILL_EVIDENCE[skillCode].orEmpty()
	}

	private fun addTypedEffects(tokens: MutableSet<String>, effects: Collection<Any>) {
		effects.forEach { effect -> addTypedEffect(tokens, effect) }
	}

	private fun addTypedEffect(tokens: MutableSet<String>, effect: Any?) {
		effect ?: return
		val effectClass = effect.javaClass
		val ownerName = effectClass.enclosingClass?.simpleName
		val constructorName = listOfNotNull(ownerName, effectClass.simpleName).joinToString(".")
		tokens += "$constructorName("
	}

	private fun addNonDefaultFieldTokens(tokens: MutableSet<String>, slot: BattleSkillSlot) {
		if (slot.minHits != 1 || slot.maxHits != 1) tokens += "minHits ="
		if (slot.protectsUser) tokens += "protectsUser = true"
		if (slot.enduresFatalDamage) tokens += "enduresFatalDamage = true"
		if (slot.chargesBeforeUse) tokens += "chargesBeforeUse = true"
		if (slot.rechargesAfterUse) tokens += "rechargesAfterUse = true"
		if (slot.forceTargetSwitch) tokens += "forceTargetSwitch = true"
		if (slot.locksAccuracyOnTarget) tokens += "locksAccuracyOnTarget = true"
		if (slot.targetLastSkillPpReduction > 0) tokens += "targetLastSkillPpReduction ="
		if (slot.plantsLeechSeed) tokens += "plantsLeechSeed = true"
		if (slot.clearsUserSideHazardsAndTraps) tokens += "clearsUserSideHazardsAndTraps = true"
		if (slot.clearsFieldHazardsAndSubstitutes) tokens += "clearsFieldHazardsAndSubstitutes = true"
		if (slot.clearsTargetSideBarriersAndFieldHazards) tokens += "clearsTargetSideBarriersAndFieldHazards = true"
		if (slot.breaksProtection) tokens += "breaksProtection = true"
		if (slot.leavesTargetAtOneHp) tokens += "leavesTargetAtOneHp = true"
		if (slot.breaksTargetSideDamageReductions) tokens += "breaksTargetSideDamageReductions = true"
		if (slot.usableOnlyFirstSkillActionSinceEntering) tokens += "usableOnlyFirstSkillActionSinceEntering = true"
		if (slot.requiresTargetPendingDamagingSkill) tokens += "requiresTargetPendingDamagingSkill = true"
		if (slot.requiresTargetPendingPriorityDamagingSkill) tokens += "requiresTargetPendingPriorityDamagingSkill = true"
		if (slot.protectsUserSideFromMultiTargetSkills) tokens += "protectsUserSideFromMultiTargetSkills = true"
		if (slot.protectsUserSideFromPrioritySkills) tokens += "protectsUserSideFromPrioritySkills = true"
		if (slot.criticalHitStageBoost > 0) tokens += "criticalHitStageBoost ="
		if (slot.defendingStatOverride != null) tokens += "defendingStatOverride ="
		if (slot.returnsUserToDefensiveForm) tokens += "returnsUserToDefensiveForm = true"
		if (slot.restoresUserBySleeping) tokens += "restoresUserBySleeping = true"
		if (slot.curesUserMajorStatus) tokens += "curesUserMajorStatus = true"
		if (slot.curesUserSideMajorStatuses) tokens += "curesUserSideMajorStatuses = true"
		if (slot.curesUserSideActiveMajorStatuses) tokens += "curesUserSideActiveMajorStatuses = true"
		if (slot.removesUserElementAfterDamage) tokens += "removesUserElementAfterDamage = true"
		if (slot.chargeSkippedByWeathers.isNotEmpty()) tokens += "chargeSkippedByWeathers ="
		if (slot.accuracyOverridesByWeather.isNotEmpty()) tokens += "accuracyOverridesByWeather ="
		if (slot.powerMultipliersByWeather.isNotEmpty()) tokens += "powerMultipliersByWeather ="
		if (slot.groundedPowerMultipliersByTerrain.isNotEmpty()) tokens += "groundedPowerMultipliersByTerrain ="
		if (slot.elementOverridesByWeather.isNotEmpty()) tokens += "elementOverridesByWeather ="
		if (slot.elementOverridesByTerrain.isNotEmpty()) tokens += "elementOverridesByTerrain ="
		if (slot.groundedTerrainPriorityBoosts.isNotEmpty()) tokens += "groundedTerrainPriorityBoosts ="
	}

	private companion object {
		private const val STANDARD_DAMAGE_POLICY = "standard-damage"
		private const val STANDARD_DAMAGE_TOKEN = "BattleDamageRequest("
		private val EXPLICIT_POLICY_EVIDENCE = mapOf(
			"protect-self" to setOf("BattleActionFlowBoundaryTests"),
			"endure-fatal-damage" to setOf("BattleEndurePublicReferenceTests"),
		)
		private val EXPLICIT_SKILL_EVIDENCE = mapOf(
			"celebrate" to setOf("BattleNoEffectSkillTests"),
			"happy-hour" to setOf("BattleNoEffectSkillTests"),
			"splash" to setOf("BattleNoEffectSkillTests"),
		)
	}
}
