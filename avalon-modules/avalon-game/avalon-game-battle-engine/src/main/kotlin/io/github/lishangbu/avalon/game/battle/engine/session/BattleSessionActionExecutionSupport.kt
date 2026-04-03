package io.github.lishangbu.avalon.game.battle.engine.session

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.battle.engine.model.SideState
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.mutation.DamageMutation
import io.github.lishangbu.avalon.game.battle.engine.runtime.apply.MutationApplicationContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.MoveResolutionResult
import io.github.lishangbu.avalon.game.battle.engine.type.StandardTargetSelectorIds
import kotlin.math.floor
import kotlin.math.roundToInt

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
                        criticalHit = currentAggregated.criticalHit || finalResult.criticalHit,
                        accuracy = finalResult.accuracy,
                        evasion = finalResult.evasion,
                        basePower = finalResult.basePower,
                        damageRoll = finalResult.damageRoll,
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
                "hit=${result.hitSuccessful}, critical=${result.criticalHit}, " +
                "basePower=${result.basePower}, " +
                (result.damageRoll?.let { "damageRoll=$it, " } ?: "") +
                "damage=${result.damage}.",
        )
        session.recordEvent(
            BattleSessionMoveExecutedPayload(
                moveId = moveId,
                attackerId = attackerId,
                targetId = targetId,
                hitSuccessful = result.hitSuccessful,
                criticalHit = result.criticalHit,
                basePower = result.basePower,
                damageRoll = result.damageRoll,
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
     * 执行逃跑动作。
     */
    fun applyRunAction(
        session: BattleSession,
        action: BattleSessionRunAction,
    ): BattleRuntimeSnapshot {
        val resolution = resolveRunAttempt(session, action)
        if (!resolution.success) {
            val failedAttempts = resolution.failedAttempts ?: session.currentSnapshot.battle.failedRunAttempts[action.sideId] ?: 0
            session.currentSnapshot =
                session.currentSnapshot.copy(
                    battle =
                        session.currentSnapshot.battle.copy(
                            failedRunAttempts = session.currentSnapshot.battle.failedRunAttempts + (action.sideId to failedAttempts),
                        ),
                )
            session.recordLog(
                "Run failed for side ${action.sideId}. " +
                    "reason=${resolution.reason} runner=${resolution.runnerUnitId} " +
                    "failedAttempts=$failedAttempts" +
                    (resolution.escapeValue?.let { " escapeValue=$it" } ?: "") +
                    (resolution.roll?.let { " roll=$it" } ?: "") +
                    ".",
            )
            session.recordEvent(
                BattleSessionRunFailedPayload(
                    sideId = action.sideId,
                    runnerUnitId = resolution.runnerUnitId,
                    reason = resolution.reason,
                    failedAttempts = failedAttempts,
                    escapeValue = resolution.escapeValue,
                    roll = resolution.roll,
                ),
            )
            return session.currentSnapshot
        }

        val survivingOpponent =
            session.currentSnapshot.sides.keys
                .firstOrNull { sideId -> sideId != action.sideId }
        session.currentSnapshot =
            session.currentSnapshot.copy(
                battle =
                    session.currentSnapshot.battle.copy(
                        ended = true,
                        winner = survivingOpponent,
                        endedReason = "run",
                    ),
            )
        session.recordLog(
            "Executed run action for side ${action.sideId}. " +
                "runner=${resolution.runnerUnitId} reason=${resolution.reason}. Winner: $survivingOpponent.",
        )
        session.recordEvent(
            BattleSessionBattleEndedPayload(
                winner = survivingOpponent,
                actionType = BattleSessionActionEventKind.RUN,
                runner = action.sideId,
            ),
        )
        return session.currentSnapshot
    }

    private fun resolveRunAttempt(
        session: BattleSession,
        action: BattleSessionRunAction,
    ): RunResolution {
        val battle = session.currentSnapshot.battle
        if (battle.battleKind != BattleType.WILD) {
            return RunResolution(success = false, reason = "run-not-allowed")
        }
        val side =
            session.currentSnapshot.sides[action.sideId]
                ?: return RunResolution(success = false, reason = "side-not-found")
        val opponents =
            session.currentSnapshot.sides.values
                .filter { other -> other.id != action.sideId }
        val runner = resolveRunnerUnit(session, side)
        if (runner == null) {
            return RunResolution(success = false, reason = "runner-not-found")
        }
        if (guaranteedRunSuccess(runner)) {
            return RunResolution(
                success = true,
                runnerUnitId = runner.id,
                reason = "guaranteed",
            )
        }
        blockedRunReason(runner, opponents, session)
            ?.let { reason ->
                return RunResolution(
                    success = false,
                    runnerUnitId = runner.id,
                    reason = reason,
                    failedAttempts = (battle.failedRunAttempts[action.sideId] ?: 0) + 1,
                )
            }

        val failedAttempts = (battle.failedRunAttempts[action.sideId] ?: 0) + 1
        val runnerSpeed = effectiveSpeed(runner).coerceAtLeast(1)
        val opponentSpeed =
            opponents
                .flatMap(SideState::activeUnitIds)
                .mapNotNull(session.currentSnapshot.units::get)
                .maxOfOrNull(::effectiveSpeed)
                ?.coerceAtLeast(1)
                ?: 1
        // The wiki formula scales the foe speed by 1/4 and keeps the denominator in 8-bit range.
        val scaledOpponentSpeed = ((opponentSpeed / 4) % 256).coerceAtLeast(1)
        val escapeValue = floor((runnerSpeed * 32.0) / scaledOpponentSpeed.toDouble()).toInt() + (30 * failedAttempts)
        if (escapeValue > 255) {
            return RunResolution(
                success = true,
                runnerUnitId = runner.id,
                reason = "formula",
                failedAttempts = failedAttempts,
                escapeValue = escapeValue,
            )
        }
        val roll = session.nextRandomInt(256)
        return if (roll < escapeValue) {
            RunResolution(
                success = true,
                runnerUnitId = runner.id,
                reason = "formula",
                failedAttempts = failedAttempts,
                escapeValue = escapeValue,
                roll = roll,
            )
        } else {
            RunResolution(
                success = false,
                runnerUnitId = runner.id,
                reason = "formula",
                failedAttempts = failedAttempts,
                escapeValue = escapeValue,
                roll = roll,
            )
        }
    }

    private fun resolveRunnerUnit(
        session: BattleSession,
        side: SideState,
    ): UnitState? =
        side.activeUnitIds
            .mapNotNull(session.currentSnapshot.units::get)
            .maxByOrNull(::effectiveSpeed)

    private fun guaranteedRunSuccess(unit: UnitState): Boolean =
        "ghost" in unit.typeIds ||
            unit.abilityId == "run-away" ||
            unit.itemId == "smoke-ball"

    private fun blockedRunReason(
        runner: UnitState,
        opponents: List<SideState>,
        session: BattleSession,
    ): String? {
        if (runner.conditionIds.any { it in DIRECT_TRAP_CONDITIONS } || runner.volatileIds.any { it in DIRECT_TRAP_CONDITIONS }) {
            return "direct-trap"
        }
        val opponentUnits =
            opponents
                .flatMap(SideState::activeUnitIds)
                .mapNotNull(session.currentSnapshot.units::get)
        val shadowTagged =
            opponentUnits.any { unit ->
                unit.abilityId == "shadow-tag" &&
                    runner.abilityId != "shadow-tag"
            }
        if (shadowTagged) {
            return "shadow-tag"
        }
        val arenaTrapped =
            opponentUnits.any { unit -> unit.abilityId == "arena-trap" } &&
                isGrounded(runner)
        if (arenaTrapped) {
            return "arena-trap"
        }
        return null
    }

    private fun isGrounded(unit: UnitState): Boolean {
        if ("flying" in unit.typeIds) {
            return false
        }
        if (unit.abilityId == "levitate") {
            return false
        }
        if (unit.itemId == "air-balloon") {
            return false
        }
        if ("magnet-rise" in unit.volatileIds || "magnet-rise" in unit.conditionIds) {
            return false
        }
        return true
    }

    private fun effectiveSpeed(unit: UnitState): Int {
        val baseSpeed = unit.stats["speed"] ?: unit.stats["spe"] ?: 0
        val stage = unit.boosts["speed"] ?: unit.boosts["spe"] ?: 0
        return (baseSpeed * stageMultiplier(stage)).roundToInt()
    }

    private fun stageMultiplier(stage: Int): Double {
        val normalized = stage.coerceIn(-6, 6)
        return if (normalized >= 0) {
            (2.0 + normalized) / 2.0
        } else {
            2.0 / (2.0 - normalized)
        }
    }

    private data class RunResolution(
        val success: Boolean,
        val reason: String,
        val runnerUnitId: String? = null,
        val failedAttempts: Int? = null,
        val escapeValue: Int? = null,
        val roll: Int? = null,
    )

    private companion object {
        val DIRECT_TRAP_CONDITIONS: Set<String> =
            setOf(
                "trapped",
                "partially-trapped",
                "cannot-escape",
            )
    }
}
