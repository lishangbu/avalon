package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * Session Roster 中由当前资料标识描述的参战成员。
 */
@Schema(description = "Battle Session 的参战成员配置。")
data class BattleSessionRosterParticipantRequest(
	@field:Schema(description = "成员种类资料 ID。", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	var creatureId: Long = 0,
	@field:Schema(description = "成员等级。", example = "50", requiredMode = Schema.RequiredMode.REQUIRED)
	var level: Int = 0,
	@field:Schema(description = "技能资料 ID 列表。", requiredMode = Schema.RequiredMode.REQUIRED)
	var skillIds: List<Long> = emptyList(),
	@field:Schema(description = "特性资料 ID。", nullable = true)
	var abilityId: Long? = null,
	@field:Schema(description = "道具资料 ID。", nullable = true)
	var itemId: Long? = null,
	@field:Schema(description = "个体值配置；未传能力按 31 处理。")
	var individualValues: Map<String, Int> = emptyMap(),
	@field:Schema(description = "努力值配置；未传能力按 0 处理。")
	var effortValues: Map<String, Int> = emptyMap(),
	@field:Schema(description = "性格提升的能力 code；中性性格留空。", nullable = true)
	var natureIncreasedStat: String? = null,
	@field:Schema(description = "性格降低的能力 code；中性性格留空。", nullable = true)
	var natureDecreasedStat: String? = null,
)
