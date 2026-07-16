package io.github.lishangbu.match.game

/** 不含 Runtime actorId 的持久化行动，所有定位均限制在公开 position。 */
data class MatchTurnAction(
	var actorPosition: Int = 0,
	var type: String = "",
	var skillId: Long? = null,
	var targetPosition: Int = 0,
	var targetYou: Boolean = false,
	var terastallize: Boolean = false,
)
