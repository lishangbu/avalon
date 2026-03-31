package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames
import io.github.lishangbu.avalon.game.battle.engine.repository.EffectDefinitionRepository

/**
 * 默认 battle 主流程实现。
 *
 * 设计意图：
 * - 在 battle 层真正消费 effect 定义，而不只停留在规则处理器单测。
 * - 通过 move resolution pipeline、mutation interceptor chain 和 hit policy 拆解 battle flow 主流程。
 *
 * @property effectRepository effect 定义查询入口。
 * @property phaseProcessor battle hook phase 处理器。
 * @property moveResolutionPipeline 单次出招主流程 pipeline。
 */
class DefaultBattleFlowEngine(
    private val effectRepository: EffectDefinitionRepository,
    private val phaseProcessor: BattleFlowPhaseProcessor,
    private val moveResolutionPipeline: BattleMoveResolutionPipeline,
) : BattleFlowEngine {
    /**
     * 推进一次完整的 move resolution。
     */
    override fun resolveMoveAction(
        snapshot: BattleRuntimeSnapshot,
        moveId: String,
        attackerId: String,
        targetId: String,
        accuracy: Int?,
        evasion: Int?,
        basePower: Int,
        damage: Int,
        attributes: Map<String, Any?>,
    ): MoveResolutionResult {
        val moveEffect = effectRepository.get(moveId)
        val context =
            BattleMoveResolutionContext(
                snapshot = snapshot,
                moveEffect = moveEffect,
                attackerId = attackerId,
                targetId = targetId,
                sourceId = attackerId,
                attributes = attributes.withDefaultBattleAttributes(targetRelation = "foe"),
                accuracy = accuracy,
                evasion = evasion,
                basePower = basePower,
                damage = damage,
            )
        return moveResolutionPipeline.resolve(context)
    }

    /**
     * 以只关心命中与 hook 的简化参数推进一次 move resolution。
     */
    override fun resolveMoveHit(
        snapshot: BattleRuntimeSnapshot,
        moveId: String,
        attackerId: String,
        targetId: String,
        attributes: Map<String, Any?>,
    ): BattleRuntimeSnapshot =
        resolveMoveAction(
            snapshot = snapshot,
            moveId = moveId,
            attackerId = attackerId,
            targetId = targetId,
            accuracy = null,
            evasion = null,
            basePower = 0,
            damage = 0,
            attributes = attributes,
        ).snapshot

    /**
     * 推进一次 residual phase。
     */
    override fun resolveResidualPhase(snapshot: BattleRuntimeSnapshot): BattleRuntimeSnapshot {
        var currentSnapshot = snapshot
        val unitIds = currentSnapshot.units.keys.toList()
        unitIds.forEach { unitId ->
            currentSnapshot =
                phaseProcessor
                    .processAttachedEffects(
                        snapshot = currentSnapshot,
                        unitId = unitId,
                        hookName = StandardHookNames.ON_RESIDUAL.value,
                        targetId = null,
                        sourceId = null,
                        relay = null,
                        attributes = emptyMap(),
                    ).snapshot
        }
        return currentSnapshot
    }

    /**
     * 为 battle attributes 补上默认的 targetRelation。
     */
    private fun Map<String, Any?>.withDefaultBattleAttributes(targetRelation: String): Map<String, Any?> = if (containsKey("targetRelation")) this else this + ("targetRelation" to targetRelation)
}
