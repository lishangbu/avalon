package io.github.lishangbu.avalon.game.controller

import io.github.lishangbu.avalon.game.service.battle.GameBattleService
import io.github.lishangbu.avalon.game.service.battle.SmartCaptureChoiceRequest
import io.github.lishangbu.avalon.game.service.battle.SmartItemChoiceRequest
import io.github.lishangbu.avalon.game.service.battle.SmartMoveChoiceRequest
import io.github.lishangbu.avalon.game.service.battle.SubmitReplacementChoiceRequest
import io.github.lishangbu.avalon.game.service.battle.SubmitRunChoiceRequest
import io.github.lishangbu.avalon.game.service.battle.SubmitSwitchChoiceRequest
import io.github.lishangbu.avalon.game.service.battle.view.CreateImportedBattleSessionApiRequest
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleSessionView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleSettlementView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleTargetQueryView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleTurnResultView
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 游戏对战业务控制器。 */
@RestController
@RequestMapping("/game/battle")
class GameBattleController(
    private val gameBattleService: GameBattleService,
) {
    /** 使用真实数据直接创建并导入战斗会话。 */
    @PostMapping("/sessions/imported")
    fun createImportedSession(
        @RequestBody request: CreateImportedBattleSessionApiRequest,
    ): GameBattleSessionView = gameBattleService.createImportedSession(request)

    /** 启动已创建的战斗会话。 */
    @PostMapping("/sessions/{sessionId}/start")
    fun startSession(
        @PathVariable sessionId: String,
    ): GameBattleSessionView = gameBattleService.startSession(sessionId)

    /** 查询当前战斗会话。 */
    @GetMapping("/sessions/{sessionId}")
    fun querySession(
        @PathVariable sessionId: String,
    ): GameBattleSessionView = gameBattleService.querySession(sessionId)

    /** 查询当前 effect 在会话中的可选目标。 */
    @GetMapping("/sessions/{sessionId}/targets")
    fun queryTargets(
        @PathVariable sessionId: String,
        @RequestParam effectId: String,
        @RequestParam actorUnitId: String,
    ): GameBattleTargetQueryView = gameBattleService.queryTargets(sessionId, effectId, actorUnitId)

    /** 提交智能出招。 */
    @PostMapping("/sessions/{sessionId}/choices/move")
    fun submitMoveChoice(
        @PathVariable sessionId: String,
        @RequestBody request: SmartMoveChoiceRequest,
    ): GameBattleSessionView = gameBattleService.submitMoveChoice(sessionId, request)

    /** 提交智能用道具。 */
    @PostMapping("/sessions/{sessionId}/choices/item")
    fun submitItemChoice(
        @PathVariable sessionId: String,
        @RequestBody request: SmartItemChoiceRequest,
    ): GameBattleSessionView = gameBattleService.submitItemChoice(sessionId, request)

    /** 提交捕捉动作。 */
    @PostMapping("/sessions/{sessionId}/choices/capture")
    fun submitCaptureChoice(
        @PathVariable sessionId: String,
        @RequestBody request: SmartCaptureChoiceRequest,
    ): GameBattleSessionView = gameBattleService.submitCaptureChoice(sessionId, request)

    /** 提交换人。 */
    @PostMapping("/sessions/{sessionId}/choices/switch")
    fun submitSwitchChoice(
        @PathVariable sessionId: String,
        @RequestBody request: SubmitSwitchChoiceRequest,
    ): GameBattleSessionView = gameBattleService.submitSwitchChoice(sessionId, request)

    /** 提交逃跑。 */
    @PostMapping("/sessions/{sessionId}/choices/run")
    fun submitRunChoice(
        @PathVariable sessionId: String,
        @RequestBody request: SubmitRunChoiceRequest,
    ): GameBattleSessionView = gameBattleService.submitRunChoice(sessionId, request)

    /** 提交替补上场。 */
    @PostMapping("/sessions/{sessionId}/replacement")
    fun submitReplacementChoice(
        @PathVariable sessionId: String,
        @RequestBody request: SubmitReplacementChoiceRequest,
    ): GameBattleSessionView =
        gameBattleService.submitReplacementChoice(
            sessionId = sessionId,
            sideId = request.sideId,
            incomingUnitId = request.incomingUnitId,
        )

    /** 结算当前回合。 */
    @PostMapping("/sessions/{sessionId}/resolve-turn")
    fun resolveTurn(
        @PathVariable sessionId: String,
    ): GameBattleTurnResultView = gameBattleService.resolveTurn(sessionId)

    /** 对已结束 battle 执行统一结算。 */
    @PostMapping("/sessions/{sessionId}/settle")
    fun settleSession(
        @PathVariable sessionId: String,
    ): GameBattleSettlementView = gameBattleService.settleSession(sessionId)
}
