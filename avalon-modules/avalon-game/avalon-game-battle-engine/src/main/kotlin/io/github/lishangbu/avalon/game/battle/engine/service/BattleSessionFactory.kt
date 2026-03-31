package io.github.lishangbu.avalon.game.battle.engine.service

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionState

/**
 * BattleSession 工厂。
 *
 * 设计意图：
 * - 把 session 的装配逻辑从 service 主体中拆出。
 * - 允许 service 层只关注会话生命周期，不关心 runtime 依赖如何拼装。
 */
interface BattleSessionFactory {
    fun create(
        sessionId: String,
        formatId: String,
    ): BattleSession

    fun restore(state: BattleSessionState): BattleSession
}
