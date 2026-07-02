package io.github.lishangbu.battleengine.model

/**
 * 战斗开始时冻结的规则快照。
 *
 * 规则快照是 `battle-rules` 管理资料和纯引擎之间的边界对象。引擎不关心这些数据来自数据库、
 * 测试用例还是未来的缓存层，只要求快照在一次战斗生命周期内保持不可变。
 *
 * 快照包含属性克制表、天气/状态规则需要识别的属性 ID，以及准备阶段可执行的队伍限制。
 * 属性 ID 按 `game_element.code` 冻结在 [elementIds] 中，避免每新增一种需要识别的属性就继续扩展一批
 * `xxxElementId` 字段；禁用资料 ID 仍由上层从资料库组装后传入，引擎本身不硬编码具体资料编号。
 */
data class BattleRuleSnapshot(
	val elementChart: ElementEffectivenessChart = ElementEffectivenessChart.neutral(),
	val elementIds: Map<String, Long> = emptyMap(),
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
		require(elementIds.keys.all { it.isNotBlank() }) { "elementIds must contain only non-blank codes" }
		require(elementIds.values.all { it > 0 }) { "elementIds must contain only positive ids" }
		require(grassyTerrainHealDenominator > 0) { "grassyTerrainHealDenominator must be positive" }
		require(maxParticipantLevel == null || maxParticipantLevel in 1..100) {
			"maxParticipantLevel must be between 1 and 100 when present"
		}
		require(bannedCreatureIds.all { it > 0 }) { "bannedCreatureIds must contain only positive ids" }
		require(bannedSkillIds.all { it > 0 }) { "bannedSkillIds must contain only positive ids" }
		require(bannedAbilityIds.all { it > 0 }) { "bannedAbilityIds must contain only positive ids" }
		require(bannedItemIds.all { it > 0 }) { "bannedItemIds must contain only positive ids" }
	}

	/**
	 * 按资料侧稳定 code 读取属性 ID。
	 *
	 * 返回 `null` 表示当前规则快照没有携带该属性；调用方必须把缺失视为对应规则不可触发，而不是猜测资料 ID。
	 */
	fun elementId(code: String): Long? = elementIds[code]
}
