package io.github.lishangbu.battleengine.model

/** 保存变身前必须在离场时恢复的战斗资料。 */
data class BattleTransformSnapshot(
	val creatureId: Long,
	val attack: Int,
	val defense: Int,
	val specialAttack: Int,
	val specialDefense: Int,
	val speed: Int,
	val weight: Int,
	val elementIds: Set<Long>,
	val skillSlots: List<BattleSkillSlot>,
	val abilityId: Long?,
	val abilityEffects: List<BattleAbilityEffect>,
)
