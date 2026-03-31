package io.github.lishangbu.avalon.game.battle.engine.service.memory

import io.github.lishangbu.avalon.game.battle.engine.service.BattleSessionStore
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession
import java.util.concurrent.ConcurrentHashMap

/**
 * BattleSession 的内存存储实现。
 */
class InMemoryBattleSessionStore : BattleSessionStore {
    private val sessions: MutableMap<String, BattleSession> = ConcurrentHashMap()

    override fun save(
        sessionId: String,
        session: BattleSession,
    ) {
        sessions[sessionId] = session
    }

    override fun find(sessionId: String): BattleSession? = sessions[sessionId]

    override fun remove(sessionId: String) {
        sessions.remove(sessionId)
    }
}
