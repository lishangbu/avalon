package io.github.lishangbu.avalon.game.service.battle.view

import io.github.lishangbu.avalon.game.battle.engine.model.BattleState
import io.github.lishangbu.avalon.game.battle.engine.model.FieldState
import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.MoveResolutionResult
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionActionExecutionResult
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureResourceUsage
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionCaptureResult
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionChoiceStatus
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionEffectAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionEvent
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionReplacementRequest
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionResourceUsage
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionSideAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionSubmittingAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionSwitchingAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionTargetedAction
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionTurnResult
import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetQuery
import io.github.lishangbu.avalon.game.service.battle.BattleSettlementResult
import io.github.lishangbu.avalon.game.service.capture.CaptureBattleResult
import io.github.lishangbu.avalon.game.service.capture.CapturedCreatureSummary
import org.springframework.stereotype.Component

/**
 * battle API 视图组装器。
 *
 * 设计意图：
 * - 把 engine DTO 映射为面向前端的稳定 API view。
 * - 让 controller 与 `GameBattleService` 不再直接暴露 engine 层的数据结构。
 */
@Component
class GameBattleViewAssembler {
    /**
     * 把 battle session 查询结果映射为前端视图。
     */
    fun toSessionView(query: BattleSessionQuery): GameBattleSessionView =
        GameBattleSessionView(
            snapshot = toSnapshotView(query.snapshot),
            pendingActions = query.pendingActions.map(::toActionView),
            choiceStatuses = query.choiceStatuses.map(::toChoiceStatusView),
            replacementRequests = query.replacementRequests.map(::toReplacementRequestView),
            resourceLedger = query.resourceLedger.map(::toResourceUsageView),
            captureResourceLedger = query.captureResourceLedger.map(::toCaptureResourceUsageView),
            battleLogs = query.battleLogs,
            events = query.eventLogs.map(::toEventView),
        )

    /**
     * 把目标查询结果映射为前端视图。
     */
    fun toTargetQueryView(query: BattleSessionTargetQuery): GameBattleTargetQueryView =
        GameBattleTargetQueryView(
            effectId = query.effectId,
            actorUnitId = query.actorUnitId,
            mode = query.mode,
            availableTargetUnitIds = query.availableTargetUnitIds,
            requiresExplicitTarget = query.requiresExplicitTarget,
        )

    /**
     * 把整回合推进结果映射为前端视图。
     */
    fun toTurnResultView(result: BattleSessionTurnResult): GameBattleTurnResultView =
        GameBattleTurnResultView(
            actionResults = result.actionResults.map(::toActionExecutionResultView),
            snapshot = toSnapshotView(result.snapshot),
        )

    /**
     * 把内部结算结果映射为前端视图。
     */
    fun toSettlementView(result: BattleSettlementResult): GameBattleSettlementView =
        GameBattleSettlementView(
            sessionId = result.sessionId,
            endedReason = result.endedReason,
            settled = result.settled,
            session = toSessionView(result.session),
            captureResult = result.captureResult?.let(::toCaptureResultView),
        )

    /**
     * 把内部捕捉结算结果映射为前端视图。
     */
    fun toCaptureResultView(result: CaptureBattleResult): GameBattleCaptureResultView =
        GameBattleCaptureResultView(
            success = result.success,
            sessionId = result.sessionId,
            targetUnitId = result.targetUnitId,
            ballItemId = result.ballItemId,
            shakes = result.shakes,
            reason = result.reason,
            battleEnded = result.battleEnded,
            finalRate = result.finalRate,
            capturedCreature = result.capturedCreature?.let(::toCapturedCreatureView),
        )

    /**
     * 把快照映射为前端视图。
     */
    private fun toSnapshotView(snapshot: BattleRuntimeSnapshot): GameBattleSnapshotView =
        GameBattleSnapshotView(
            battle = toBattleStateView(snapshot.battle),
            field = toFieldView(snapshot.field),
            units =
                snapshot.units.values
                    .sortedBy(UnitState::id)
                    .map(::toUnitView),
            sides =
                snapshot.sides.values
                    .sortedBy(SideState::id)
                    .map(::toSideView),
        )

    /**
     * 把 battle state 映射为前端视图。
     */
    private fun toBattleStateView(state: BattleState): GameBattleStateView =
        GameBattleStateView(
            id = state.id,
            formatId = state.formatId,
            battleKind = state.battleKind,
            started = state.started,
            turn = state.turn,
            ended = state.ended,
            settled = state.settled,
            winner = state.winner,
            endedReason = state.endedReason,
            capturableSideId = state.capturableSideId,
            capturedUnitId = state.capturedUnitId,
        )

    /**
     * 把 field state 映射为前端视图。
     */
    private fun toFieldView(field: FieldState): GameBattleFieldView =
        GameBattleFieldView(
            weatherId = field.weatherId,
            terrainId = field.terrainId,
        )

    /**
     * 把 unit state 映射为前端视图。
     */
    private fun toUnitView(unit: UnitState): GameBattleUnitView =
        GameBattleUnitView(
            id = unit.id,
            currentHp = unit.currentHp,
            maxHp = unit.maxHp,
            statusId = unit.statusId,
            abilityId = unit.abilityId,
            itemId = unit.itemId,
            typeIds = unit.typeIds.toList().sorted(),
            volatileIds = unit.volatileIds.toList().sorted(),
            conditionIds = unit.conditionIds.toList().sorted(),
            boosts = unit.boosts,
            stats = unit.stats,
            movePp = unit.movePp,
            flags = unit.flags,
            forceSwitchRequested = unit.forceSwitchRequested,
        )

    /**
     * 把 side state 映射为前端视图。
     */
    private fun toSideView(side: SideState): GameBattleSideView =
        GameBattleSideView(
            id = side.id,
            unitIds = side.unitIds,
            activeUnitIds = side.activeUnitIds,
        )

    /**
     * 把当前待执行动作映射为前端视图。
     */
    private fun toActionView(action: BattleSessionAction): GameBattleActionView =
        GameBattleActionView(
            kind = action.kind,
            priority = action.priority,
            speed = action.speed,
            submittingUnitId = (action as? BattleSessionSubmittingAction)?.submittingUnitId,
            sideId = (action as? BattleSessionSideAction)?.sideId,
            effectId = (action as? BattleSessionEffectAction)?.effectId,
            targetUnitId = (action as? BattleSessionTargetedAction)?.targetUnitId,
            playerId = (action as? BattleSessionCaptureAction)?.playerId,
            ballItemId = (action as? BattleSessionCaptureAction)?.ballItemId,
            outgoingUnitId = (action as? BattleSessionSwitchingAction)?.outgoingUnitId,
            incomingUnitId = (action as? BattleSessionSwitchingAction)?.incomingUnitId,
        )

    /**
     * 把单个动作执行结果映射为前端视图。
     */
    private fun toActionExecutionResultView(result: BattleSessionActionExecutionResult): GameBattleActionExecutionResultView =
        GameBattleActionExecutionResultView(
            action = toActionView(result.action),
            snapshot = toSnapshotView(result.snapshot),
            moveResult = result.moveResult?.let(::toMoveResolutionView),
            captureResult = result.captureResult?.let(::toCaptureExecutionView),
        )

    /**
     * 把 move 结算结果映射为前端视图。
     */
    private fun toMoveResolutionView(result: MoveResolutionResult): GameBattleMoveResolutionView =
        GameBattleMoveResolutionView(
            cancelled = result.cancelled,
            hitSuccessful = result.hitSuccessful,
            criticalHit = result.criticalHit,
            accuracy = result.accuracy,
            evasion = result.evasion,
            basePower = result.basePower,
            damageRoll = result.damageRoll,
            damage = result.damage,
        )

    /**
     * 把 capture action 执行结果映射为前端视图。
     */
    private fun toCaptureExecutionView(result: BattleSessionCaptureResult): GameBattleCaptureExecutionView =
        GameBattleCaptureExecutionView(
            success = result.success,
            playerId = result.playerId,
            ballItemId = result.ballItemId,
            sourceUnitId = result.sourceUnitId,
            targetId = result.targetId,
            shakes = result.shakes,
            reason = result.reason,
            finalRate = result.finalRate,
        )

    /**
     * 把 choice status 映射为前端视图。
     */
    private fun toChoiceStatusView(status: BattleSessionChoiceStatus): GameBattleChoiceStatusView =
        GameBattleChoiceStatusView(
            sideId = status.sideId,
            activeUnitIds = status.activeUnitIds,
            submittedUnitIds = status.submittedUnitIds,
            missingUnitIds = status.missingUnitIds,
            requiredActionCount = status.requiredActionCount,
            submittedActionCount = status.submittedActionCount,
            ready = status.ready,
        )

    /**
     * 把替补请求映射为前端视图。
     */
    private fun toReplacementRequestView(request: BattleSessionReplacementRequest): GameBattleReplacementRequestView =
        GameBattleReplacementRequestView(
            sideId = request.sideId,
            outgoingUnitIds = request.outgoingUnitIds,
            candidateUnitIds = request.candidateUnitIds,
        )

    /**
     * 把 battle event 映射为前端视图。
     */
    private fun toEventView(event: BattleSessionEvent): GameBattleEventView =
        GameBattleEventView(
            type = event.type,
            turn = event.turn,
            payloadType = event.payload::class.simpleName ?: event.payload::class.qualifiedName.orEmpty(),
            attributes = event.attributes,
        )

    /**
     * 把资源账本条目映射为前端视图。
     */
    private fun toResourceUsageView(usage: BattleSessionResourceUsage): GameBattleResourceUsageView =
        GameBattleResourceUsageView(
            kind = usage.kind,
            payloadType = usage::class.simpleName ?: usage::class.qualifiedName.orEmpty(),
            attributes =
                when (usage) {
                    is BattleSessionCaptureResourceUsage -> {
                        mapOf(
                            "playerId" to usage.playerId,
                            "sourceUnitId" to usage.sourceUnitId,
                            "ballItemId" to usage.ballItemId,
                            "targetUnitId" to usage.targetUnitId,
                            "quantity" to usage.quantity,
                            "success" to usage.success,
                            "shakes" to usage.shakes,
                            "reason" to usage.reason,
                            "finalRate" to usage.finalRate,
                        )
                    }

                    else -> {
                        emptyMap()
                    }
                },
        )

    /**
     * 把捕捉账本条目映射为前端视图。
     */
    private fun toCaptureResourceUsageView(usage: BattleSessionCaptureResourceUsage): GameBattleCaptureResourceUsageView =
        GameBattleCaptureResourceUsageView(
            playerId = usage.playerId,
            sourceUnitId = usage.sourceUnitId,
            ballItemId = usage.ballItemId,
            targetUnitId = usage.targetUnitId,
            quantity = usage.quantity,
            success = usage.success,
            shakes = usage.shakes,
            reason = usage.reason,
            finalRate = usage.finalRate,
        )

    /**
     * 把已捕捉生物摘要映射为前端视图。
     */
    private fun toCapturedCreatureView(summary: CapturedCreatureSummary): GameBattleCapturedCreatureView =
        GameBattleCapturedCreatureView(
            ownedCreatureId = summary.ownedCreatureId,
            creatureId = summary.creatureId,
            creatureSpeciesId = summary.creatureSpeciesId,
            creatureInternalName = summary.creatureInternalName,
            creatureName = summary.creatureName,
        )
}
