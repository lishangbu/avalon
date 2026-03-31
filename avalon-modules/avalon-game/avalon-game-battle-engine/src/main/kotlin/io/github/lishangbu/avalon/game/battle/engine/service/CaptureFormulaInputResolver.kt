package io.github.lishangbu.avalon.game.battle.engine.service

import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureFormulaInput
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureAction

/**
 * 由业务层提供捕捉公式输入。
 *
 * battle-engine 只负责执行捕捉公式，不负责查询业务数据集或玩家资产。
 */
interface CaptureFormulaInputResolver {
    fun resolve(
        sessionId: String,
        snapshot: BattleRuntimeSnapshot,
        action: BattleSessionCaptureAction,
    ): CaptureFormulaInput
}
