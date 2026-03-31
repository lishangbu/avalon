package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames

/**
 * 出招收尾 hook phase step。
 *
 * @property phaseProcessor battle hook phase 处理器。
 */
class BattleMoveAfterMovePhaseStep(
    private val phaseProcessor: BattleFlowPhaseProcessor,
) : BattleMoveResolutionStep {
    /**
     * 当前步骤在 pipeline 中的执行顺序。
     */
    override val order: Int = 500

    /**
     * 执行 `on_after_move` 收尾阶段。
     */
    override fun execute(context: BattleMoveResolutionContext) {
        val result =
            phaseProcessor.processPhase(
                snapshot = context.snapshot,
                hookName = StandardHookNames.ON_AFTER_MOVE.value,
                moveEffect = context.moveEffect,
                selfId = context.attackerId,
                targetId = context.targetId,
                sourceId = context.sourceId,
                relay = null,
                attributes = context.attributes,
            )
        context.snapshot = result.snapshot
    }
}
