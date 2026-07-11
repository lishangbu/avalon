package io.github.lishangbu.battlesession

/** 表示 Session Identifier 不存在或对应 Recent Session 已被淘汰。 */
class BattleSessionNotFoundException(
	val sessionId: String,
) : NoSuchElementException("battle session not found: $sessionId")
