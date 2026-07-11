package io.github.lishangbu.battlesession

import io.github.lishangbu.battlesession.model.BattleSessionStatus

/** 表示命令试图继续推进已经进入终态的会话。 */
class BattleSessionNotActiveException(
	val sessionId: String,
	val status: BattleSessionStatus,
) : IllegalStateException("battle session is not active: $sessionId ($status)")
