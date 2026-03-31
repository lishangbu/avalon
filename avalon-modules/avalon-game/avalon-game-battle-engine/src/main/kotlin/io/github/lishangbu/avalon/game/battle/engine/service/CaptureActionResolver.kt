package io.github.lishangbu.avalon.game.battle.engine.service

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureResult

/**
 * 捕捉行动解析器。
 *
 * 设计意图：
 * - 让 battle-engine 在不依赖具体业务模块的情况下执行 capture action。
 * - 由上层提供 wild battle 的真实数据计算实现。
 */
interface CaptureActionResolver {
    fun resolve(
        session: BattleSession,
        action: BattleSessionCaptureAction,
    ): BattleSessionCaptureResult
}
