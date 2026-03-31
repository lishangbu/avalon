package io.github.lishangbu.avalon.game.battle.engine.service

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession

/**
 * BattleSession 运行态存储抽象。
 *
 * 设计意图：
 * - 让 session service 不再绑定具体存储介质。
 * - 为内存、Redis 等不同存储实现提供统一契约。
 */
interface BattleSessionStore {
    fun save(
        sessionId: String,
        session: BattleSession,
    )

    fun find(sessionId: String): BattleSession?

    fun remove(sessionId: String)
}
