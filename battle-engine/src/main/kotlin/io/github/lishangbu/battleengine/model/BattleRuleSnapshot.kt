package io.github.lishangbu.battleengine.model

/**
 * 战斗开始时冻结的规则快照。
 *
 * 规则快照是 `battle-rules` 管理资料和纯引擎之间的边界对象。引擎不关心这些数据来自数据库、
 * 测试 fixture 还是未来的缓存层，只要求快照在一次战斗生命周期内保持不可变。
 *
 * 第一阶段快照包含属性克制表、天气/状态规则需要识别的元素 ID，以及准备阶段可执行的队伍限制。
 * 元素 ID 和禁用资料 ID 都由上层从资料库组装后传入，引擎本身不硬编码具体资料编号。
 * 后续状态、特性和道具 hook 会继续挂到这里，但仍会保持结构化字段，不引入自由脚本或 raw JSON。
 */
data class BattleRuleSnapshot(
	val elementChart: ElementEffectivenessChart = ElementEffectivenessChart.neutral(),
	val electricElementId: Long? = null,
	val fireElementId: Long? = null,
	val iceElementId: Long? = null,
	val poisonElementId: Long? = null,
	val steelElementId: Long? = null,
	val waterElementId: Long? = null,
	val grassyTerrainHealDenominator: Int = 16,
	val maxParticipantLevel: Int? = null,
	val bannedCreatureIds: Set<Long> = emptySet(),
	val bannedSkillIds: Set<Long> = emptySet(),
	val bannedAbilityIds: Set<Long> = emptySet(),
	val bannedItemIds: Set<Long> = emptySet(),
	val uniqueCreatureRequired: Boolean = false,
	val uniqueItemRequired: Boolean = false,
) {
	init {
		require(electricElementId == null || electricElementId > 0) { "electricElementId must be positive when present" }
		require(fireElementId == null || fireElementId > 0) { "fireElementId must be positive when present" }
		require(iceElementId == null || iceElementId > 0) { "iceElementId must be positive when present" }
		require(poisonElementId == null || poisonElementId > 0) { "poisonElementId must be positive when present" }
		require(steelElementId == null || steelElementId > 0) { "steelElementId must be positive when present" }
		require(waterElementId == null || waterElementId > 0) { "waterElementId must be positive when present" }
		require(grassyTerrainHealDenominator > 0) { "grassyTerrainHealDenominator must be positive" }
		require(maxParticipantLevel == null || maxParticipantLevel in 1..100) {
			"maxParticipantLevel must be between 1 and 100 when present"
		}
		require(bannedCreatureIds.all { it > 0 }) { "bannedCreatureIds must contain only positive ids" }
		require(bannedSkillIds.all { it > 0 }) { "bannedSkillIds must contain only positive ids" }
		require(bannedAbilityIds.all { it > 0 }) { "bannedAbilityIds must contain only positive ids" }
		require(bannedItemIds.all { it > 0 }) { "bannedItemIds must contain only positive ids" }
	}
}
