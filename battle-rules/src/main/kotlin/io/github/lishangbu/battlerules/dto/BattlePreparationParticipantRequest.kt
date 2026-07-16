package io.github.lishangbu.battlerules.dto

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 战斗准备阶段校验的成员快照。
 */
@Schema(description = "战斗准备阶段校验的成员快照。")
data class BattlePreparationParticipantRequest(
	@field:Schema(description = "战斗内成员 ID。", example = "side-a-1")
	var actorId: String = "",
	@field:Schema(description = "成员种类资料 ID。", example = "1")
	var creatureId: Long = 0,
	@field:Schema(description = "成员等级。", example = "50")
	var level: Int = 50,
	@field:Schema(description = "技能资料 ID 列表。")
	var skillIds: List<Long> = emptyList(),
	@field:Schema(description = "特性资料 ID。", nullable = true)
	var abilityId: Long? = null,
	@field:Schema(description = "道具资料 ID。", nullable = true)
	var itemId: Long? = null,
	@field:Schema(
		description = "个体值配置。key 使用 hp、attack、defense、special-attack、special-defense、speed；未传能力按 31 处理。",
		example = """{"hp":31,"attack":31,"defense":31,"special-attack":31,"special-defense":31,"speed":31}""",
	)
	var individualValues: Map<String, Int> = emptyMap(),
	@field:Schema(
		description = "努力值配置。key 使用 hp、attack、defense、special-attack、special-defense、speed；未传能力按 0 处理，总和不能超过 510。",
		example = """{"hp":0,"attack":0,"defense":0,"special-attack":0,"special-defense":0,"speed":252}""",
	)
	var effortValues: Map<String, Int> = emptyMap(),
	@field:Schema(
		description = "性格提升的能力 code。中性性格留空；非空时必须和 natureDecreasedStat 同时填写，且不能使用 hp。",
		nullable = true,
		allowableValues = ["attack", "defense", "special-attack", "special-defense", "speed"],
	)
	var natureIncreasedStat: String? = null,
	@field:Schema(
		description = "性格降低的能力 code。中性性格留空；非空时必须和 natureIncreasedStat 同时填写，且不能使用 hp。",
		nullable = true,
		allowableValues = ["attack", "defense", "special-attack", "special-defense", "speed"],
	)
	var natureDecreasedStat: String? = null,
	@field:Schema(description = "预先配置的太晶属性 ID。", nullable = true)
	var teraElementId: Long? = null,
)
