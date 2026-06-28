package io.github.lishangbu.battleengine.model

/**
 * 属性克制倍率表。
 *
 * 该类型是战斗规则快照的一部分，用纯 Kotlin Map 表达攻击属性到防御属性的倍率。
 * 缺失的组合按现代规则中的中性倍率 1.0 处理；多属性防御方通过连续相乘得到最终倍率。
 * 这里不保存文本名称，也不保存历史版本差异，只保存本次战斗快照需要的数值关系。
 */
class ElementEffectivenessChart(
	private val multiplierByAttackingAndDefendingElement: Map<Long, Map<Long, Double>> = emptyMap(),
) {
	/**
	 * 计算攻击属性打到防御方属性集合时的总倍率。
	 *
	 * @param attackingElementId 技能属性 ID。
	 * @param defendingElementIds 防御方当前属性 ID 集合，空集合会被视为中性倍率。
	 * @return 连乘后的倍率。免疫组合可以通过 0.0 表达，并会使最终伤害降为 0。
	 */
	fun multiplier(attackingElementId: Long, defendingElementIds: Set<Long>): Double =
		defendingElementIds.fold(1.0) { current, defendingElementId ->
			current * (multiplierByAttackingAndDefendingElement[attackingElementId]?.get(defendingElementId) ?: 1.0)
		}

	companion object {
		/**
		 * 构造一个全中性属性倍率表，适合公式单元测试或尚未接入完整属性资料的最小战斗。
		 */
		fun neutral(): ElementEffectivenessChart = ElementEffectivenessChart()
	}
}
