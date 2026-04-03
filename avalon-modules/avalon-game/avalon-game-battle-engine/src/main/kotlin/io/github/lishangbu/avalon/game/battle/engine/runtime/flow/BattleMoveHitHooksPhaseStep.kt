package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames

/**
 * 命中后 hook phase step。
 *
 * @property phaseProcessor battle hook phase 处理器。
 */
class BattleMoveHitHooksPhaseStep(
    private val phaseProcessor: BattleFlowPhaseProcessor,
) : BattleMoveResolutionStep {
    /**
     * 当前步骤在 pipeline 中的执行顺序。
     */
    override val order: Int = 400

    /**
     * 执行命中后的 `on_hit` 与 `on_after_hit` 阶段。
     */
    override fun execute(context: BattleMoveResolutionContext) {
        if (!context.hitSuccessful) {
            return
        }
        val hitResult =
            phaseProcessor.processPhase(
                snapshot = context.snapshot,
                hookName = StandardHookNames.ON_HIT.value,
                moveEffect = context.moveEffect,
                selfId = context.attackerId,
                targetId = context.targetId,
                sourceId = context.sourceId,
                relay = null,
                attributes = context.attributes + mapOf("criticalHit" to context.criticalHit),
            )
        context.snapshot = hitResult.snapshot

        val afterHitResult =
            phaseProcessor.processPhase(
                snapshot = context.snapshot,
                hookName = StandardHookNames.ON_AFTER_HIT.value,
                moveEffect = context.moveEffect,
                selfId = context.attackerId,
                targetId = context.targetId,
                sourceId = context.sourceId,
                relay = null,
                attributes = context.attributes + mapOf("criticalHit" to context.criticalHit),
            )
        context.snapshot = afterHitResult.snapshot
    }
}
