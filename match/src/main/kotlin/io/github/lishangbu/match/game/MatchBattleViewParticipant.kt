package io.github.lishangbu.match.game

/** 持久投影中的成员状态，不包含任何隐藏配招或内部 actor 标识。 */
data class MatchBattleViewParticipant(
	var creatureId: Long = 0,
	var active: Boolean = false,
	var currentHp: Int = 0,
	var maxHp: Int = 0,
	var teraElementId: Long? = null,
)
