package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames

/**
 * 命中与回避修正 phase step。
 *
 * @property phaseProcessor battle hook phase 处理器。
 */
class BattleMoveAccuracyEvasionPhaseStep(
    private val phaseProcessor: BattleFlowPhaseProcessor,
) : BattleMoveResolutionStep {
    /**
     * 当前步骤在 pipeline 中的执行顺序。
     */
    override val order: Int = 100

    /**
     * 执行命中与回避修正阶段。
     */
    override fun execute(context: BattleMoveResolutionContext) {
        val accuracyResult =
            phaseProcessor.processPhase(
                snapshot = context.snapshot,
                hookName = StandardHookNames.ON_MODIFY_ACCURACY.value,
                moveEffect = context.moveEffect,
                selfId = context.attackerId,
                targetId = context.targetId,
                sourceId = context.sourceId,
                relay = context.accuracy?.toDouble(),
                attributes = context.attributes,
            )
        context.snapshot = accuracyResult.snapshot
        context.accuracy = (accuracyResult.relay as? Number)?.toInt() ?: context.accuracy

        val evasionResult =
            phaseProcessor.processPhase(
                snapshot = context.snapshot,
                hookName = StandardHookNames.ON_MODIFY_EVASION.value,
                moveEffect = context.moveEffect,
                selfId = context.targetId,
                targetId = context.targetId,
                sourceId = context.sourceId,
                relay = context.evasion?.toDouble(),
                attributes = context.attributes,
            )
        context.snapshot = evasionResult.snapshot
        context.evasion = (evasionResult.relay as? Number)?.toInt() ?: context.evasion
    }
}
