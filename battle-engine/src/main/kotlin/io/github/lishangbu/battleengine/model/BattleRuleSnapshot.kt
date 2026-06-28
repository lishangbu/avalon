package io.github.lishangbu.battleengine.model

/**
 * 战斗开始时冻结的规则快照。
 *
 * 规则快照是 `battle-rules` 管理资料和纯引擎之间的边界对象。引擎不关心这些数据来自数据库、
 * 测试 fixture 还是未来的缓存层，只要求快照在一次战斗生命周期内保持不可变。
 *
 * 第一阶段快照包含属性克制表，以及天气伤害倍率需要识别的元素 ID。元素 ID 由上层从资料库组装后传入，
 * 引擎本身不硬编码具体资料编号。后续状态、特性和道具 hook 会继续挂到这里，但仍会保持结构化字段，
 * 不引入自由脚本或 raw JSON。
 */
data class BattleRuleSnapshot(
	val elementChart: ElementEffectivenessChart = ElementEffectivenessChart.neutral(),
	val fireElementId: Long? = null,
	val waterElementId: Long? = null,
	val grassyTerrainHealDenominator: Int = 16,
) {
	init {
		require(fireElementId == null || fireElementId > 0) { "fireElementId must be positive when present" }
		require(waterElementId == null || waterElementId > 0) { "waterElementId must be positive when present" }
		require(grassyTerrainHealDenominator > 0) { "grassyTerrainHealDenominator must be positive" }
	}
}
