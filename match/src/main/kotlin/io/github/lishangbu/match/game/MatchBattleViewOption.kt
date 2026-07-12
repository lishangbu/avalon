package io.github.lishangbu.match.game

/** Runtime 行动选项的精简持久形式，side 仅在服务端转换为“己方/对方”。 */
data class MatchBattleViewOption(
	var type: String = "",
	var skillId: Long? = null,
	var targetSide: Int = 0,
	var targetPosition: Int = 0,
)
