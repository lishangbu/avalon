package io.github.lishangbu.match.game

/** 客户端按稳定 code 本地化并播放动画的结构化战斗事实。 */
data class MatchBattleEvent(
	var code: String = "",
	var parameters: Map<String, Any?> = emptyMap(),
)
