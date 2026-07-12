package io.github.lishangbu.match.game

/** 持久投影中的待选行动；仅 ACTIVE Match 会将其投影给所属查看方。 */
data class MatchBattleViewRequirement(
	var actorSide: Int = 0,
	var actorPosition: Int = 0,
	var options: List<MatchBattleViewOption> = emptyList(),
)
