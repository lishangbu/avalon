package io.github.lishangbu.match.game

/** 单方提交只确认己方已锁定；双方齐备并结算后才返回推进后的 View。 */
data class MatchTurnResponse(val locked: Boolean, val match: MatchViewResponse? = null)
