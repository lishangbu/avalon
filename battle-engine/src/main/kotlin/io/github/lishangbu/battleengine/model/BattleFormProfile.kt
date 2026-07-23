package io.github.lishangbu.battleengine.model

/**
 * 成员可在战斗中切换到的一份完整形态画像。
 *
 * 画像只包含形态变化会替换的稳定战斗事实；技能、特性、道具、状态和能力阶级继续属于成员运行态，不随形态切换重置。
 */
data class BattleFormProfile(
	val creatureId: Long,
	val maxHp: Int,
	val attack: Int,
	val defense: Int,
	val specialAttack: Int,
	val specialDefense: Int,
	val speed: Int,
	val weight: Int,
	val elementIds: Set<Long>,
) {
	init {
		require(creatureId > 0) { "creatureId must be positive" }
		require(maxHp > 0) { "maxHp must be positive" }
		require(attack > 0 && defense > 0 && specialAttack > 0 && specialDefense > 0 && speed > 0) {
			"battle stats must be positive"
		}
		require(weight > 0) { "weight must be positive" }
		require(elementIds.isNotEmpty() && elementIds.all { it > 0 }) { "elementIds must contain positive ids" }
	}
}
