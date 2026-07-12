package io.github.lishangbu.match.game

/** 持久投影中的一方，只保留成员的公开战斗状态。 */
data class MatchBattleViewSide(var participants: List<MatchBattleViewParticipant> = emptyList())
