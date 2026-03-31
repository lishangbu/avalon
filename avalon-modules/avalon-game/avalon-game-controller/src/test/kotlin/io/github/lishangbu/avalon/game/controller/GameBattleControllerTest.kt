package io.github.lishangbu.avalon.game.controller

import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionChoiceStatus
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionEvent
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionReplacementRequest
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionResourceUsage
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetMode
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQuery
import io.github.lishangbu.avalon.game.service.battle.GameBattleService
import io.github.lishangbu.avalon.game.service.battle.SmartCaptureChoiceRequest
import io.github.lishangbu.avalon.game.service.battle.SmartItemChoiceRequest
import io.github.lishangbu.avalon.game.service.battle.SmartMoveChoiceRequest
import io.github.lishangbu.avalon.game.service.battle.SubmitReplacementChoiceRequest
import io.github.lishangbu.avalon.game.service.battle.SubmitRunChoiceRequest
import io.github.lishangbu.avalon.game.service.battle.SubmitSwitchChoiceRequest
import io.github.lishangbu.avalon.game.service.battle.view.CreateImportedBattleSessionApiRequest
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleCaptureResultView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleImportedSideApiRequest
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleImportedUnitApiRequest
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleSessionView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleSettlementView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleTargetQueryView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleTurnResultView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleViewAssembler
import io.github.lishangbu.avalon.game.service.capture.CaptureBattleResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class GameBattleControllerTest {
    @Test
    fun createImportedSession_delegatesToService() {
        val service = FakeGameBattleService()
        val controller = GameBattleController(service)
        val request =
            CreateImportedBattleSessionApiRequest(
                sessionId = "session-1",
                formatId = "single",
                sides =
                    listOf(
                        GameBattleImportedSideApiRequest(
                            sideId = "side-a",
                            units = listOf(GameBattleImportedUnitApiRequest(unitId = "unit-a", level = 50, creatureInternalName = "pikachu")),
                        ),
                    ),
            )

        val result = controller.createImportedSession(request)

        assertSame(service.queryResult, result)
        assertSame(request, service.createdRequest)
    }

    @Test
    fun startAndQuerySession_delegateToService() {
        val service = FakeGameBattleService()
        val controller = GameBattleController(service)

        val started = controller.startSession("session-2")
        val queried = controller.querySession("session-2")

        assertSame(service.queryResult, started)
        assertSame(service.queryResult, queried)
        assertEquals("session-2", service.startedSessionId)
        assertEquals("session-2", service.queriedSessionId)
    }

    @Test
    fun queryTargets_delegatesToService() {
        val service = FakeGameBattleService()
        val controller = GameBattleController(service)

        val result = controller.queryTargets("session-3", "thunderbolt", "unit-a")

        assertSame(service.targetQueryResult, result)
        assertEquals("session-3", service.targetSessionId)
        assertEquals("thunderbolt", service.targetEffectId)
        assertEquals("unit-a", service.targetActorUnitId)
    }

    @Test
    fun submitMoveAndItemChoices_delegateToService() {
        val service = FakeGameBattleService()
        val controller = GameBattleController(service)
        val moveRequest = SmartMoveChoiceRequest(attackerId = "unit-a", moveId = "thunderbolt")
        val itemRequest = SmartItemChoiceRequest(actorUnitId = "unit-a", itemId = "sitrus-berry")

        val moveResult = controller.submitMoveChoice("session-4", moveRequest)
        val itemResult = controller.submitItemChoice("session-4", itemRequest)

        assertSame(service.queryResult, moveResult)
        assertSame(service.queryResult, itemResult)
        assertEquals("session-4", service.moveSessionId)
        assertSame(moveRequest, service.moveRequest)
        assertEquals("session-4", service.itemSessionId)
        assertSame(itemRequest, service.itemRequest)
    }

    @Test
    fun submitCaptureChoice_delegatesToService() {
        val service = FakeGameBattleService()
        val controller = GameBattleController(service)
        val request = SmartCaptureChoiceRequest(playerId = "1", ballItemId = "great-ball", sourceUnitId = "unit-a", targetId = "unit-b")

        val result = controller.submitCaptureChoice("session-capture-choice", request)

        assertSame(service.queryResult, result)
        assertEquals("session-capture-choice", service.captureChoiceSessionId)
        assertSame(request, service.captureChoiceRequest)
    }

    @Test
    fun submitSwitchRunAndReplacement_delegateToService() {
        val service = FakeGameBattleService()
        val controller = GameBattleController(service)
        val switchChoice = SubmitSwitchChoiceRequest(sideId = "side-a", outgoingUnitId = "unit-a", incomingUnitId = "unit-b")
        val runChoice = SubmitRunChoiceRequest(sideId = "side-b")
        val replacementCommand = SubmitReplacementChoiceRequest(sideId = "side-a", incomingUnitId = "unit-c")

        val switchResult = controller.submitSwitchChoice("session-5", switchChoice)
        val runResult = controller.submitRunChoice("session-5", runChoice)
        val replacementResult = controller.submitReplacementChoice("session-5", replacementCommand)

        assertSame(service.queryResult, switchResult)
        assertSame(service.queryResult, runResult)
        assertSame(service.queryResult, replacementResult)
        assertEquals("session-5", service.switchSessionId)
        assertSame(switchChoice, service.switchChoice)
        assertEquals("session-5", service.runSessionId)
        assertSame(runChoice, service.runChoice)
        assertEquals("session-5", service.replacementSessionId)
        assertEquals("side-a", service.replacementSideId)
        assertEquals("unit-c", service.replacementIncomingUnitId)
    }

    @Test
    fun resolveTurn_delegatesToService() {
        val service = FakeGameBattleService()
        val controller = GameBattleController(service)

        val result = controller.resolveTurn("session-6")

        assertSame(service.turnResult, result)
        assertEquals("session-6", service.resolvedSessionId)
    }

    @Test
    fun settleSession_delegatesToService() {
        val service = FakeGameBattleService()
        val controller = GameBattleController(service)

        val result = controller.settleSession("session-7")

        assertSame(service.settlementResult, result)
        assertEquals("session-7", service.settleSessionId)
    }

    private class FakeGameBattleService : GameBattleService {
        var createdRequest: CreateImportedBattleSessionApiRequest? = null
        var startedSessionId: String? = null
        var queriedSessionId: String? = null
        var targetSessionId: String? = null
        var targetEffectId: String? = null
        var targetActorUnitId: String? = null
        var moveSessionId: String? = null
        var moveRequest: SmartMoveChoiceRequest? = null
        var itemSessionId: String? = null
        var itemRequest: SmartItemChoiceRequest? = null
        var switchSessionId: String? = null
        var switchChoice: SubmitSwitchChoiceRequest? = null
        var runSessionId: String? = null
        var runChoice: SubmitRunChoiceRequest? = null
        var replacementSessionId: String? = null
        var replacementSideId: String? = null
        var replacementIncomingUnitId: String? = null
        var captureChoiceSessionId: String? = null
        var captureChoiceRequest: SmartCaptureChoiceRequest? = null
        var settleSessionId: String? = null
        var resolvedSessionId: String? = null

        private val viewAssembler = GameBattleViewAssembler()

        val queryResult: GameBattleSessionView = viewAssembler.toSessionView(sampleQuery())
        val captureResult =
            GameBattleCaptureResultView(
                success = true,
                sessionId = "session-capture",
                targetUnitId = "unit-b",
                ballItemId = "great-ball",
                shakes = 4,
                reason = "all-shakes-passed",
                battleEnded = true,
                finalRate = 100.0,
                capturedCreature = null,
            )
        val settlementResult =
            GameBattleSettlementView(
                sessionId = "session-7",
                endedReason = "capture",
                settled = true,
                session = queryResult,
                captureResult = captureResult,
            )
        val targetQueryResult: GameBattleTargetQueryView =
            viewAssembler.toTargetQueryView(
                BattleSessionTargetQuery(
                    effectId = "thunderbolt",
                    actorUnitId = "unit-a",
                    mode = BattleSessionTargetMode.FOE,
                    availableTargetUnitIds = listOf("unit-b"),
                    requiresExplicitTarget = true,
                ),
            )
        val turnResult: GameBattleTurnResultView =
            viewAssembler.toTurnResultView(
                io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionTurnResult(
                    actionResults = emptyList(),
                    snapshot = sampleQuery().snapshot,
                ),
            )

        override fun createImportedSession(request: CreateImportedBattleSessionApiRequest): GameBattleSessionView {
            createdRequest = request
            return queryResult
        }

        override fun startSession(sessionId: String): GameBattleSessionView {
            startedSessionId = sessionId
            return queryResult
        }

        override fun querySession(sessionId: String): GameBattleSessionView {
            queriedSessionId = sessionId
            return queryResult
        }

        override fun queryTargets(
            sessionId: String,
            effectId: String,
            actorUnitId: String,
        ): GameBattleTargetQueryView {
            targetSessionId = sessionId
            targetEffectId = effectId
            targetActorUnitId = actorUnitId
            return targetQueryResult
        }

        override fun submitMoveChoice(
            sessionId: String,
            request: SmartMoveChoiceRequest,
        ): GameBattleSessionView {
            moveSessionId = sessionId
            moveRequest = request
            return queryResult
        }

        override fun submitItemChoice(
            sessionId: String,
            request: SmartItemChoiceRequest,
        ): GameBattleSessionView {
            itemSessionId = sessionId
            itemRequest = request
            return queryResult
        }

        override fun submitCaptureChoice(
            sessionId: String,
            request: SmartCaptureChoiceRequest,
        ): GameBattleSessionView {
            captureChoiceSessionId = sessionId
            captureChoiceRequest = request
            return queryResult
        }

        override fun submitSwitchChoice(
            sessionId: String,
            request: SubmitSwitchChoiceRequest,
        ): GameBattleSessionView {
            switchSessionId = sessionId
            switchChoice = request
            return queryResult
        }

        override fun submitRunChoice(
            sessionId: String,
            request: SubmitRunChoiceRequest,
        ): GameBattleSessionView {
            runSessionId = sessionId
            runChoice = request
            return queryResult
        }

        override fun submitReplacementChoice(
            sessionId: String,
            sideId: String,
            incomingUnitId: String,
        ): GameBattleSessionView {
            replacementSessionId = sessionId
            replacementSideId = sideId
            replacementIncomingUnitId = incomingUnitId
            return queryResult
        }

        override fun settleSession(sessionId: String): GameBattleSettlementView {
            settleSessionId = sessionId
            return settlementResult
        }

        override fun resolveTurn(sessionId: String): GameBattleTurnResultView {
            resolvedSessionId = sessionId
            return turnResult
        }
    }
}

private fun sampleQuery(): BattleSessionQuery =
    BattleSessionQuery(
        snapshot =
            BattleRuntimeSnapshot(
                battle = BattleState(id = "session-sample", formatId = "single", started = true, turn = 1),
                field = FieldState(),
                units =
                    mapOf(
                        "unit-a" to UnitState(id = "unit-a", currentHp = 100, maxHp = 100),
                        "unit-b" to UnitState(id = "unit-b", currentHp = 100, maxHp = 100),
                    ),
                sides =
                    mapOf(
                        "side-a" to SideState(id = "side-a", unitIds = listOf("unit-a"), activeUnitIds = listOf("unit-a")),
                        "side-b" to SideState(id = "side-b", unitIds = listOf("unit-b"), activeUnitIds = listOf("unit-b")),
                    ),
            ),
        pendingActions = emptyList<BattleSessionAction>(),
        choiceStatuses = emptyList<BattleSessionChoiceStatus>(),
        replacementRequests = emptyList<BattleSessionReplacementRequest>(),
        resourceLedger = emptyList<BattleSessionResourceUsage>(),
        battleLogs = emptyList(),
        eventLogs = emptyList<BattleSessionEvent>(),
    )
