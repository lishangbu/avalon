package io.github.lishangbu.match.game

/** 对手成员已经通过公开行动或触发事件暴露的最小事实集合，不保存事件流和随机轨迹。 */
data class MatchDisclosure(
	var skillIds: Set<Long> = emptySet(),
	var abilityId: Long? = null,
	var itemId: Long? = null,
)
