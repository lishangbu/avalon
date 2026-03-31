package io.github.lishangbu.avalon.game.battle.engine.service

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSession
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureResult

/**
 * 默认捕捉解析器。
 *
 * 未被业务模块覆盖时，明确拒绝 capture action。
 */
class UnsupportedCaptureActionResolver : CaptureActionResolver {
    override fun resolve(
        session: BattleSession,
        action: BattleSessionCaptureAction,
    ): BattleSessionCaptureResult = error("Capture action is not supported for session '${session.currentSnapshot.battle.id}'.")
}
