package io.github.lishangbu.avalon.game.battle.engine.service

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionChoice
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionTurnResult

/**
 * BattleSession 对外服务接口。
 *
 * 设计意图：
 * - 为上层调用方提供稳定的会话 API。
 * - 屏蔽 BattleSession 内部装配、存储和查询细节。
 */
interface BattleSessionService {
    fun createSession(
        sessionId: String,
        formatId: String,
    ): BattleSessionQuery

    fun configureSession(
        sessionId: String,
        battleKind: BattleType,
        capturableSideId: String? = null,
    ): BattleSessionQuery

    fun startSession(sessionId: String): BattleSessionQuery

    fun registerSide(
        sessionId: String,
        sideId: String,
    ): BattleSessionQuery

    fun registerUnit(
        sessionId: String,
        sideId: String,
        unit: UnitState,
        active: Boolean = true,
    ): BattleSessionQuery

    fun submitChoice(
        sessionId: String,
        choice: BattleSessionChoice,
    ): BattleSessionQuery

    fun submitChoices(
        sessionId: String,
        choices: List<BattleSessionChoice>,
    ): BattleSessionQuery

    fun submitReplacementChoice(
        sessionId: String,
        sideId: String,
        incomingUnitId: String,
    ): BattleSessionQuery

    fun recordCaptureFailure(
        sessionId: String,
        ballItemId: String,
        targetUnitId: String,
        shakes: Int,
        reason: String,
        finalRate: Double,
    ): BattleSessionQuery

    fun finishSessionByCapture(
        sessionId: String,
        targetUnitId: String,
    ): BattleSessionQuery

    fun markSessionSettled(sessionId: String): BattleSessionQuery

    fun resolveTurn(sessionId: String): BattleSessionTurnResult

    fun querySession(sessionId: String): BattleSessionQuery
}
