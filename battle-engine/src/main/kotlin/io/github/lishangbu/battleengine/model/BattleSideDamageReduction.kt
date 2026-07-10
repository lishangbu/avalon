package io.github.lishangbu.battleengine.model

/**
 * 一方场上承受标准伤害时的减免屏障。
 *
 * 该模型只表达“防守方一侧当前存在何种伤害减免”，不绑定具体技能名称或资料表 code。现代主系列里，
 * 物理屏障只影响物理伤害，特殊屏障只影响特殊伤害，全伤害屏障同时覆盖两类；三者在一次伤害中不叠加。
 * 持续回合由状态机在回合末推进，空值表示测试用例或外部系统暂不关心持续时间。
 */
data class BattleSideDamageReduction(
	val kind: BattleSideDamageReductionKind,
	val turnsRemaining: Int? = null,
) {
	init {
		require(turnsRemaining == null || turnsRemaining > 0) { "turnsRemaining must be positive when present" }
	}

	/**
	 * 判断该屏障是否影响当前技能伤害分类。
	 */
	fun appliesTo(damageClass: BattleDamageClass): Boolean =
		when (kind) {
			BattleSideDamageReductionKind.PHYSICAL -> damageClass == BattleDamageClass.PHYSICAL
			BattleSideDamageReductionKind.SPECIAL -> damageClass == BattleDamageClass.SPECIAL
			BattleSideDamageReductionKind.ALL_STANDARD_DAMAGE -> damageClass != BattleDamageClass.STATUS
		}

	/**
	 * 推进一个完整回合后的剩余状态。
	 */
	fun advanceTurn(): BattleSideDamageReduction? =
		when (turnsRemaining) {
			null -> this
			1 -> null
			else -> copy(turnsRemaining = turnsRemaining - 1)
		}
}
