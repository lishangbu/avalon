package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.game.service.battle.view.CreateImportedBattleSessionApiRequest
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleSessionView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleSettlementView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleTargetQueryView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleTurnResultView

/** 真实数据驱动的战斗应用服务。 */
interface GameBattleService {
    fun createImportedSession(request: CreateImportedBattleSessionApiRequest): GameBattleSessionView

    fun startSession(sessionId: String): GameBattleSessionView

    fun querySession(sessionId: String): GameBattleSessionView

    fun queryTargets(
        sessionId: String,
        effectId: String,
        actorUnitId: String,
    ): GameBattleTargetQueryView

    fun submitMoveChoice(
        sessionId: String,
        request: SmartMoveChoiceRequest,
    ): GameBattleSessionView

    fun submitItemChoice(
        sessionId: String,
        request: SmartItemChoiceRequest,
    ): GameBattleSessionView

    fun submitCaptureChoice(
        sessionId: String,
        request: SmartCaptureChoiceRequest,
    ): GameBattleSessionView

    fun submitSwitchChoice(
        sessionId: String,
        request: SubmitSwitchChoiceRequest,
    ): GameBattleSessionView

    fun submitRunChoice(
        sessionId: String,
        request: SubmitRunChoiceRequest,
    ): GameBattleSessionView

    fun submitReplacementChoice(
        sessionId: String,
        sideId: String,
        incomingUnitId: String,
    ): GameBattleSessionView

    fun settleSession(sessionId: String): GameBattleSettlementView

    fun resolveTurn(sessionId: String): GameBattleTurnResultView
}
