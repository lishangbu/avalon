package io.github.lishangbu.match.game

/** 持久化的精简战斗投影；只保存 Match View 必需状态，不保存事件流或随机轨迹。 */
data class MatchBattleViewState(
	var sides: List<MatchBattleViewSide> = emptyList(),
	var requirements: List<MatchBattleViewRequirement> = emptyList(),
	var events: List<MatchBattleEvent> = emptyList(),
)
