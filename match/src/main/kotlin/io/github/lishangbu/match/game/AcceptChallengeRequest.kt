package io.github.lishangbu.match.game

/** 接收方以 Challenge revision 与当前 Team Lead 执行原子接受。 */
data class AcceptChallengeRequest(var expectedRevision: Long = -1, var teamId: String = "")
