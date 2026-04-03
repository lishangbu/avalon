package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureFormulaResult
import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.battle.engine.service.BattleAiChoiceProvider
import io.github.lishangbu.avalon.game.battle.engine.service.BattleSessionService
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureResourceUsage
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionResourceUsage
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionTurnResult
import io.github.lishangbu.avalon.game.battle.engine.session.CaptureChoice
import io.github.lishangbu.avalon.game.battle.engine.session.RunChoice
import io.github.lishangbu.avalon.game.battle.engine.session.SwitchChoice
import io.github.lishangbu.avalon.game.battle.engine.session.WaitChoice
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQuery
import io.github.lishangbu.avalon.game.service.battle.view.CreateImportedBattleSessionApiRequest
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleRequestAssembler
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleSessionView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleSettlementView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleTargetQueryView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleTurnResultView
import io.github.lishangbu.avalon.game.service.battle.view.GameBattleViewAssembler
import io.github.lishangbu.avalon.game.service.capture.CaptureBattleResult
import io.github.lishangbu.avalon.game.service.capture.CaptureCommand
import io.github.lishangbu.avalon.game.service.capture.CapturedCreatureSummary
import io.github.lishangbu.avalon.game.service.capture.DefaultBattleCaptureRecordService
import io.github.lishangbu.avalon.game.service.capture.DefaultCapturePreparationService
import io.github.lishangbu.avalon.game.service.capture.DefaultOwnedCreatureService
import io.github.lishangbu.avalon.game.service.capture.DefaultPlayerInventoryService
import io.github.lishangbu.avalon.game.service.unit.BattleUnitImporter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 游戏对战编排层。
 *
 * 在智能导入与 battle engine 之间提供一个更贴近业务的入口。
 */
@Service
class DefaultGameBattleService internal constructor(
    private val battleSessionService: BattleSessionService,
    private val battleUnitImporter: BattleUnitImporter,
    private val battleChoiceFactory: BattleChoiceFactory,
    private val battleAiChoiceProviders: List<BattleAiChoiceProvider>,
    private val battleTypePolicies: List<BattleTypePolicy>,
    private val capturePreparationService: DefaultCapturePreparationService,
    private val playerInventoryService: DefaultPlayerInventoryService,
    private val ownedCreatureService: DefaultOwnedCreatureService,
    private val battleCaptureRecordService: DefaultBattleCaptureRecordService,
    private val gameBattleRequestAssembler: GameBattleRequestAssembler,
    private val gameBattleViewAssembler: GameBattleViewAssembler,
) : GameBattleService {
    override fun createImportedSession(request: CreateImportedBattleSessionApiRequest): GameBattleSessionView {
        val internalRequest = gameBattleRequestAssembler.toCreateImportedSessionRequest(request)
        require(internalRequest.sides.isNotEmpty()) { "At least one side must be provided." }
        battleSessionService.createSession(internalRequest.sessionId, internalRequest.formatId)
        battleSessionService.configureSession(
            sessionId = internalRequest.sessionId,
            battleKind = internalRequest.battleKind,
            capturableSideId = internalRequest.capturableSideId,
        )

        internalRequest.sides.forEach { side ->
            battleSessionService.registerSide(internalRequest.sessionId, side.sideId)
            val normalizedActiveIds = normalizeActiveUnitIds(side)
            side.units.forEach { unitRequest ->
                val imported = battleUnitImporter.importUnit(unitRequest)
                battleSessionService.registerUnit(
                    sessionId = internalRequest.sessionId,
                    sideId = side.sideId,
                    unit = imported.unit,
                    active = imported.unit.id in normalizedActiveIds,
                )
            }
        }

        if (internalRequest.autoStart) {
            return gameBattleViewAssembler.toSessionView(battleSessionService.startSession(internalRequest.sessionId))
        }
        return gameBattleViewAssembler.toSessionView(battleSessionService.querySession(internalRequest.sessionId))
    }

    override fun startSession(sessionId: String): GameBattleSessionView = gameBattleViewAssembler.toSessionView(battleSessionService.startSession(sessionId))

    override fun querySession(sessionId: String): GameBattleSessionView = gameBattleViewAssembler.toSessionView(battleSessionService.querySession(sessionId))

    override fun queryTargets(
        sessionId: String,
        effectId: String,
        actorUnitId: String,
    ): GameBattleTargetQueryView =
        gameBattleViewAssembler.toTargetQueryView(
            battleChoiceFactory.queryTargets(
                session = battleSessionService.querySession(sessionId),
                effectId = effectId,
                actorUnitId = actorUnitId,
            ),
        )

    override fun submitMoveChoice(
        sessionId: String,
        request: SmartMoveChoiceRequest,
    ): GameBattleSessionView {
        val session = battleSessionService.querySession(sessionId)
        val choice = battleChoiceFactory.createMoveChoice(session, request)
        return gameBattleViewAssembler.toSessionView(battleSessionService.submitChoice(sessionId, choice))
    }

    override fun submitItemChoice(
        sessionId: String,
        request: SmartItemChoiceRequest,
    ): GameBattleSessionView {
        val session = battleSessionService.querySession(sessionId)
        val choice = battleChoiceFactory.createItemChoice(session, request)
        return gameBattleViewAssembler.toSessionView(battleSessionService.submitChoice(sessionId, choice))
    }

    override fun submitCaptureChoice(
        sessionId: String,
        request: SmartCaptureChoiceRequest,
    ): GameBattleSessionView {
        val session = battleSessionService.querySession(sessionId)
        val resolvedSourceUnitId = resolveCaptureSourceUnitId(session, request.sourceUnitId, request.targetId)
        val speed =
            request.speed ?: (
                session.snapshot.units[resolvedSourceUnitId]
                    ?.stats
                    ?.get("speed") ?: 0
            )
        return gameBattleViewAssembler.toSessionView(
            battleSessionService.submitChoice(
                sessionId,
                CaptureChoice(
                    playerId = request.playerId,
                    ballItemId = request.ballItemId,
                    sourceUnitId = resolvedSourceUnitId,
                    targetId = request.targetId,
                    priority = request.priority ?: 0,
                    speed = speed,
                ),
            ),
        )
    }

    override fun submitSwitchChoice(
        sessionId: String,
        request: SubmitSwitchChoiceRequest,
    ): GameBattleSessionView =
        gameBattleViewAssembler.toSessionView(
            battleSessionService.submitChoice(
                sessionId,
                SwitchChoice(
                    sideId = request.sideId,
                    outgoingUnitId = request.outgoingUnitId,
                    incomingUnitId = request.incomingUnitId,
                    priority = request.priority ?: 0,
                    speed = request.speed ?: 0,
                ),
            ),
        )

    override fun submitRunChoice(
        sessionId: String,
        request: SubmitRunChoiceRequest,
    ): GameBattleSessionView =
        gameBattleViewAssembler.toSessionView(
            battleSessionService.submitChoice(
                sessionId,
                RunChoice(
                    sideId = request.sideId,
                    priority = request.priority ?: 0,
                    speed = request.speed ?: resolveRunSpeed(battleSessionService.querySession(sessionId), request.sideId),
                ),
            ),
        )

    override fun submitReplacementChoice(
        sessionId: String,
        sideId: String,
        incomingUnitId: String,
    ): GameBattleSessionView =
        gameBattleViewAssembler.toSessionView(
            battleSessionService.submitReplacementChoice(sessionId, sideId, incomingUnitId),
        )

    @Transactional(rollbackFor = [Exception::class])
    override fun settleSession(sessionId: String): GameBattleSettlementView = gameBattleViewAssembler.toSettlementView(settleSessionInternal(sessionId))

    @Transactional(rollbackFor = [Exception::class])
    private fun settleSessionInternal(sessionId: String): BattleSettlementResult {
        val session = battleSessionService.querySession(sessionId)
        require(session.snapshot.battle.ended) { "Battle '$sessionId' has not ended yet." }
        if (session.snapshot.battle.settled) {
            return BattleSettlementResult(
                sessionId = sessionId,
                endedReason = session.snapshot.battle.endedReason,
                settled = true,
                session = session,
                captureResult = null,
            )
        }
        val policy = resolvePolicy(session.snapshot.battle.battleKind)
        return policy.settlementHandler().settle(
            sessionId = sessionId,
            session = session,
            support =
                object : BattleSettlementSupport {
                    override fun settleCaptureLedger(
                        sessionId: String,
                        session: BattleSessionQuery,
                    ): CaptureBattleResult? = settleCaptureLedgerInternal(sessionId, session)

                    override fun markSessionSettled(sessionId: String): BattleSessionQuery = battleSessionService.markSessionSettled(sessionId)
                },
        )
    }

    override fun resolveTurn(sessionId: String): GameBattleTurnResultView {
        val session = battleSessionService.querySession(sessionId)
        resolveAiChoiceProvider(session.snapshot.battle.battleKind)
            .provide(session)
            .forEach { choice ->
                battleSessionService.submitChoice(sessionId = sessionId, choice = choice)
            }
        return gameBattleViewAssembler.toTurnResultView(battleSessionService.resolveTurn(sessionId))
    }

    private fun normalizeActiveUnitIds(side: ImportedBattleSideRequest): Set<String> {
        require(side.units.isNotEmpty()) { "Side '${side.sideId}' must contain at least one unit." }
        val allUnitIds = side.units.map { unit -> unit.unitId }.toSet()
        val normalized = side.activeUnitIds.ifEmpty { setOf(side.units.first().unitId) }
        require(normalized.all { unitId -> unitId in allUnitIds }) {
            "Side '${side.sideId}' has activeUnitIds that are not present in its unit list."
        }
        return normalized
    }

    private fun resolveCaptureSourceUnitId(
        session: BattleSessionQuery,
        requestedSourceUnitId: String,
        targetId: String,
    ): String {
        if (requestedSourceUnitId.isNotBlank()) {
            return requestedSourceUnitId
        }
        val capturableSideId = session.snapshot.battle.capturableSideId
        return session.snapshot.sides.values
            .firstOrNull { side -> side.id != capturableSideId && side.activeUnitIds.isNotEmpty() }
            ?.activeUnitIds
            ?.firstOrNull()
            ?: error("No active source unit is available for capture.")
    }

    private fun resolveRunSpeed(
        session: BattleSessionQuery,
        sideId: String,
    ): Int =
        session.snapshot.sides[sideId]
            ?.activeUnitIds
            ?.mapNotNull(session.snapshot.units::get)
            ?.maxOfOrNull { unit -> unit.stats["speed"] ?: unit.stats["spe"] ?: 0 }
            ?: 0

    private fun preparedCaptureContextFromUsage(
        sessionId: String,
        session: BattleSessionQuery,
        usage: BattleSessionCaptureResourceUsage,
    ) = capturePreparationService.prepare(
        sessionId = sessionId,
        snapshot = session.snapshot,
        command =
            CaptureCommand(
                playerId = usage.playerId,
                ballItemId = usage.ballItemId,
                targetUnitId = usage.targetUnitId,
                sourceUnitId = usage.sourceUnitId,
            ),
    )

    private fun captureFormulaResultFromUsage(usage: BattleSessionCaptureResourceUsage): CaptureFormulaResult =
        CaptureFormulaResult(
            success = usage.success,
            shakes = usage.shakes,
            captureValue = 0.0,
            finalRate = usage.finalRate,
            ballRate = 0.0,
            statusRate = 0.0,
            reason = usage.reason,
        )

    private fun settleCaptureLedgerInternal(
        sessionId: String,
        session: BattleSessionQuery,
    ): CaptureBattleResult? {
        val captureUsages = session.captureResourceLedger
        var captureSummary: CapturedCreatureSummary? = null
        val successfulUsage =
            captureUsages.lastOrNull { usage ->
                usage.success && usage.targetUnitId == session.snapshot.battle.capturedUnitId
            }

        captureUsages.forEach { usage ->
            val prepared = preparedCaptureContextFromUsage(sessionId, session, usage)
            playerInventoryService.ensureAvailable(prepared.playerId, prepared.ballItemId)
            playerInventoryService.consume(prepared.playerId, prepared.ballItemId)
            val formulaResult = captureFormulaResultFromUsage(usage)
            val currentCaptureSummary =
                if (usage == successfulUsage) {
                    ownedCreatureService.capture(prepared).also { summary -> captureSummary = summary }
                } else {
                    null
                }
            battleCaptureRecordService.record(prepared, formulaResult, currentCaptureSummary)
        }

        return successfulUsage?.let { usage ->
            CaptureBattleResult(
                success = true,
                sessionId = sessionId,
                targetUnitId = usage.targetUnitId,
                ballItemId = usage.ballItemId,
                shakes = usage.shakes,
                reason = usage.reason,
                battleEnded = true,
                finalRate = usage.finalRate,
                session = session,
                capturedCreature = captureSummary,
            )
        }
    }

    private fun resolvePolicy(battleKind: BattleType): BattleTypePolicy =
        battleTypePolicies.firstOrNull { policy -> policy.supports(battleKind) }
            ?: error("No BattleTypePolicy registered for battleKind '$battleKind'.")

    private fun resolveAiChoiceProvider(battleKind: BattleType): BattleAiChoiceProvider =
        battleAiChoiceProviders.firstOrNull { provider -> provider.supports(battleKind) }
            ?: error("No BattleAiChoiceProvider registered for battleKind '$battleKind'.")
}
