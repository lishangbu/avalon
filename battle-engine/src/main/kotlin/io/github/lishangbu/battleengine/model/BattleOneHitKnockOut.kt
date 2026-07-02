package io.github.lishangbu.battleengine.model

/**
 * 一击必杀类技能的现代规则参数。
 *
 * 这类技能命中后不进入普通伤害公式，也不读取技能威力、攻防能力值、属性一致加成、击中要害、天气或场地伤害倍率；
 * 它们在通过属性免疫和命中判定后，直接造成等于目标当前 HP 的伤害。现代主系列规则还为命中判定增加了几条
 * 专用条件，因此这里把命中口径与伤害口径放在同一个模型里：
 * - 目标等级高于使用者时，技能直接失败，不消费命中随机数。
 * - 基础命中率为 [baseAccuracyPercent]，再加上“使用者等级 - 目标等级”的差值。
 * - 命中率不受命中/闪避能力阶级影响；当前引擎尚未实现锁定类必中状态，因此这里不声明相关字段。
 * - 个别同属性敏感技能可声明 [sameElementUserBaseAccuracyPercent] 和 [blocksSameElementTarget]，用来表达
 *   “同属性使用者基础命中率较高、同属性目标天然无效”的现代例外，而不把具体技能 ID 写进纯引擎。
 */
data class BattleOneHitKnockOut(
	val baseAccuracyPercent: Int = 30,
	val sameElementUserBaseAccuracyPercent: Int? = null,
	val blocksSameElementTarget: Boolean = false,
) {
	init {
		require(baseAccuracyPercent in 1..100) { "baseAccuracyPercent must be between 1 and 100" }
		require(sameElementUserBaseAccuracyPercent == null || sameElementUserBaseAccuracyPercent in 1..100) {
			"sameElementUserBaseAccuracyPercent must be between 1 and 100 when present"
		}
	}

	/**
	 * 计算本次一击必杀命中判定使用的基础命中率。
	 *
	 * `sameElementUserBaseAccuracyPercent` 只在使用者拥有技能当前属性时生效；其它情况下回到普通基础值。属性覆盖
	 * 已经由 [BattleSkillSlot.effectiveElementId] 统一处理，所以天气改变技能属性时，这里也自然使用本次实际属性。
	 */
	fun baseAccuracyFor(
		skillElementId: Long,
		actorElementIds: Set<Long>,
	): Int =
		if (sameElementUserBaseAccuracyPercent != null && skillElementId in actorElementIds) {
			sameElementUserBaseAccuracyPercent
		} else {
			baseAccuracyPercent
		}
}
