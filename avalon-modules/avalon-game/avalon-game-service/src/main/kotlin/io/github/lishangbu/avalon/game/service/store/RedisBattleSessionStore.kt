package io.github.lishangbu.avalon.game.service.store

import io.github.lishangbu.avalon.game.battle.engine.service.BattleSessionFactory
import io.github.lishangbu.avalon.game.battle.engine.service.BattleSessionStore
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionState
import org.springframework.data.redis.core.StringRedisTemplate
import tools.jackson.databind.ObjectMapper

/**
 * BattleSession 的 Redis 存储实现。
 */
class RedisBattleSessionStore(
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val battleSessionFactory: BattleSessionFactory,
) : BattleSessionStore {
    override fun save(
        sessionId: String,
        session: BattleSession,
    ) {
        val stateJson = objectMapper.writeValueAsString(session.exportState())
        stringRedisTemplate.opsForValue().set(key(sessionId), stateJson)
    }

    override fun find(sessionId: String): BattleSession? =
        stringRedisTemplate
            .opsForValue()
            .get(key(sessionId))
            ?.let { payload -> objectMapper.readValue(payload, BattleSessionState::class.java) }
            ?.let(battleSessionFactory::restore)

    override fun remove(sessionId: String) {
        stringRedisTemplate.delete(key(sessionId))
    }

    private fun key(sessionId: String): String = "$KEY_PREFIX$sessionId"

    private companion object {
        const val KEY_PREFIX: String = "avalon:battle:session:"
    }
}
