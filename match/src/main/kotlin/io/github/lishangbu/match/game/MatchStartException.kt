package io.github.lishangbu.match.game

/** Runtime 未能承接已持久化 Match；matchId 用于客户端稳定关联失败记录。 */
class MatchStartException(val matchId: Long) : RuntimeException("match.start-failed")
