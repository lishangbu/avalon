package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.mutation.DamageMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.MutationApplicationContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.MoveResolutionResult
import io.github.lishangbu.avalon.game.battle.engine.type.StandardTargetSelectorIds

/**
 * session action 执行辅助组件。
 *
 * 设计意图：
 * - 承载 move/item 目标展开、直接伤害写回、switch/run 状态更新等共用执行逻辑。
 * - 让具体 action handler 保持轻量，并可作为无状态 Spring Bean 复用。
 */
class BattleSessionActionExecutionSupport {
    /**
     * 执行一个已经具备目标信息的 effect。
     *
     * @param session 当前 battle session。
     * @param effectId 当前执行的 effect 标识。
     * @param actorUnitId 行动发起者单位标识。
     * @param targetUnitId 调用方提供的目标单位标识。
     * @param accuracy 命中值输入。
     * @param evasion 回避值输入。
     * @param basePower 基础威力输入。
     * @param damage 基础伤害输入。
     * @param attributes 透传给 battle flow 的扩展属性。
     * @return effect 结算完成后的聚合结果。
     */
    fun executeResolvedEffect(
        session: BattleSession,
        effectId: String,
        actorUnitId: String,
        targetUnitId: String,
        accuracy: Int?,
        evasion: Int?,
        basePower: Int,
        damage: Int,
        attributes: Map<String, Any?>,
    ): MoveResolutionResult {
        val resolvedAttributes = completeRandomAttributes(session, accuracy, attributes)
        val targetQuery = session.queryTargets(effectId, actorUnitId)
        val resolvedTargetIds =
            when {
                targetQuery.requiresExplicitTarget -> listOf(targetUnitId)
                targetQuery.availableTargetUnitIds.isEmpty() -> listOf(targetUnitId)
                else -> targetQuery.availableTargetUnitIds
            }

        var aggregatedResult: MoveResolutionResult? = null

        resolvedTargetIds.forEach { resolvedTargetId ->
            val result =
                session.battleFlowEngine.resolveMoveAction(
                    snapshot = session.currentSnapshot,
                    moveId = effectId,
                    attackerId = actorUnitId,
                    targetId = resolvedTargetId,
                    accuracy = accuracy,
                    evasion = evasion,
                    basePower = basePower,
                    damage = damage,
                    attributes = resolvedAttributes,
                )
            session.currentSnapshot = result.snapshot
            if (result.hitSuccessful && result.damage > 0) {
                session.currentSnapshot = applyDirectDamage(session, resolvedTargetId, result.damage)
            }
            session.currentSnapshot = session.resolveFaintAndReplacement()
            val finalResult = result.copy(snapshot = session.currentSnapshot)
            recordMoveExecution(session, effectId, actorUnitId, resolvedTargetId, finalResult)
            val currentAggregated = aggregatedResult
            aggregatedResult =
                if (currentAggregated == null) {
                    finalResult
                } else {
                    currentAggregated.copy(
                        snapshot = session.currentSnapshot,
                        hitSuccessful = currentAggregated.hitSuccessful || finalResult.hitSuccessful,
                        accuracy = finalResult.accuracy,
                        evasion = finalResult.evasion,
                        basePower = finalResult.basePower,
                        damage = finalResult.damage,
                    )
                }
            if (session.currentSnapshot.battle.ended) {
                return requireNotNull(aggregatedResult)
            }
        }
        return requireNotNull(aggregatedResult) {
            "No targets were resolved for effect '$effectId'."
        }
    }

    private fun completeRandomAttributes(
        session: BattleSession,
        accuracy: Int?,
        attributes: Map<String, Any?>,
    ): Map<String, Any?> {
        var resolvedAttributes = attributes
        if (accuracy != null && resolvedAttributes["accuracyRoll"] == null) {
            resolvedAttributes += "accuracyRoll" to session.nextPercentageRoll()
        }
        if (resolvedAttributes["chanceRoll"] == null) {
            resolvedAttributes += "chanceRoll" to session.nextPercentageRoll()
        }
        return resolvedAttributes
    }

    /**
     * 将直接伤害 mutation 应用到当前快照。
     */
    fun applyDirectDamage(
        session: BattleSession,
        targetId: String,
        damage: Int,
    ): BattleRuntimeSnapshot {
        val applyResult =
            session.mutationApplier.apply(
                mutations =
                    listOf(
                        DamageMutation(
                            target = StandardTargetSelectorIds.TARGET,
                            mode = null,
                            value = damage.toDouble(),
                        ),
                    ),
                context =
                    MutationApplicationContext(
                        battle = session.currentSnapshot.battle,
                        field = session.currentSnapshot.field,
                        units = session.currentSnapshot.units,
                        targetId = targetId,
                    ),
            )
        return session.currentSnapshot.copy(
            battle = applyResult.battle,
            field = applyResult.field,
            units = applyResult.units,
        )
    }

    /**
     * 记录一次 move 或 item 的最终结算结果。
     */
    fun recordMoveExecution(
        session: BattleSession,
        moveId: String,
        attackerId: String,
        targetId: String,
        result: MoveResolutionResult,
    ) {
        session.recordLog(
            "Executed move $moveId from $attackerId to $targetId. " +
                "hit=${result.hitSuccessful}, basePower=${result.basePower}, damage=${result.damage}.",
        )
        session.recordEvent(
            BattleSessionMoveExecutedPayload(
                moveId = moveId,
                attackerId = attackerId,
                targetId = targetId,
                hitSuccessful = result.hitSuccessful,
                basePower = result.basePower,
                damage = result.damage,
            ),
        )
    }

    /**
     * 执行 switch 动作并更新当前 active 列表。
     */
    fun applySwitchAction(
        session: BattleSession,
        action: BattleSessionSwitchAction,
    ): BattleRuntimeSnapshot {
        val side = requireNotNull(session.currentSnapshot.sides[action.sideId]) { "Side '${action.sideId}' was not found." }
        require(action.outgoingUnitId in side.activeUnitIds) {
            "Outgoing unit '${action.outgoingUnitId}' is not active on side '${action.sideId}'."
        }
        require(action.incomingUnitId in side.unitIds) {
            "Incoming unit '${action.incomingUnitId}' is not registered on side '${action.sideId}'."
        }
        require((session.currentSnapshot.units[action.incomingUnitId]?.currentHp ?: 0) > 0) {
            "Incoming unit '${action.incomingUnitId}' is not able to battle."
        }
        val nextActiveIds =
            side.activeUnitIds.map { unitId ->
                if (unitId == action.outgoingUnitId) action.incomingUnitId else unitId
            }
        session.currentSnapshot =
            session.currentSnapshot.copy(
                sides = session.currentSnapshot.sides + (action.sideId to side.copy(activeUnitIds = nextActiveIds)),
            )
        session.recordLog("Executed switch for side ${action.sideId}: ${action.outgoingUnitId} -> ${action.incomingUnitId}.")
        session.recordEvent(
            BattleSessionSwitchExecutedPayload(
                sideId = action.sideId,
                outgoingUnitId = action.outgoingUnitId,
                incomingUnitId = action.incomingUnitId,
            ),
        )
        return session.currentSnapshot
    }

    /**
     * 执行逃跑动作并立即结束 battle。
     */
    fun applyRunAction(
        session: BattleSession,
        action: BattleSessionRunAction,
    ): BattleRuntimeSnapshot {
        val survivingOpponent =
            session.currentSnapshot.sides.keys
                .firstOrNull { sideId -> sideId != action.sideId }
        session.currentSnapshot =
            session.currentSnapshot.copy(
                battle =
                    session.currentSnapshot.battle.copy(
                        ended = true,
                        winner = survivingOpponent,
                    ),
            )
        session.recordLog("Executed run action for side ${action.sideId}. Winner: $survivingOpponent.")
        session.recordEvent(
            BattleSessionBattleEndedPayload(
                winner = survivingOpponent,
                actionType = BattleSessionActionEventKind.RUN,
                runner = action.sideId,
            ),
        )
        return session.currentSnapshot
    }
}
