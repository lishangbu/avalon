package io.github.lishangbu.battleengine.model

/**
 * 战斗结果。
 *
 * 结果只表达引擎已经能够确认的终局事实。第一阶段只支持胜负和未结束状态；
 * 平局、回合上限裁定、玩家放弃等结果会在格式规则扩展时加入。
 */
data class BattleResult(
	val winningSideId: String,
	val reason: String,
) {
	init {
		require(winningSideId.isNotBlank()) { "winningSideId must not be blank" }
		require(reason.isNotBlank()) { "reason must not be blank" }
	}
}
