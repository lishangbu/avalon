package io.github.lishangbu.battleengine.model

/**
 * 战斗结果。
 *
 * 结果只表达引擎已经能够确认的终局事实。`winningSideId` 为空表示没有胜方，例如达到格式回合上限后的平局裁定。
 * 玩家主动认输、复杂计分和多人队伍裁定会继续通过稳定 `reason` 扩展。
 */
data class BattleResult(
	val winningSideId: String?,
	val reason: String,
) {
	init {
		require(winningSideId == null || winningSideId.isNotBlank()) { "winningSideId must not be blank when present" }
		require(reason.isNotBlank()) { "reason must not be blank" }
	}
}
