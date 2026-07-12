package io.github.lishangbu.match.game

/** 玩家 Match 视图中的参与方公开身份；内部 side 与 Snapshot 均不得出现在公开契约中。 */
data class MatchParticipantResponse(val displayName: String, val you: Boolean)
