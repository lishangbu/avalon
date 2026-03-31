package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames

/**
 * 出招前置 phase step。
 *
 * 设计意图：
 * - 串行执行 `on_before_move`、`on_try_move`、`on_prepare_hit`、`on_try_hit` 四个前置阶段。
 * - 一旦任意前置阶段取消出招，就立刻终止后续 move resolution。
 *
 * @property phaseProcessor battle hook phase 处理器。
 */
class BattleMovePreHitPhaseStep(
    private val phaseProcessor: BattleFlowPhaseProcessor,
) : BattleMoveResolutionStep {
    /**
     * 当前步骤在 pipeline 中的执行顺序。
     */
    override val order: Int = 0

    /**
     * 执行出招前置阶段。
     */
    override fun execute(context: BattleMoveResolutionContext) {
        val phaseSpecs =
            listOf(
                StandardHookNames.ON_BEFORE_MOVE.value to null,
                StandardHookNames.ON_TRY_MOVE.value to true,
                StandardHookNames.ON_PREPARE_HIT.value to true,
                StandardHookNames.ON_TRY_HIT.value to true,
            )

        phaseSpecs.forEach { (hookName, relay) ->
            val result =
                phaseProcessor.processPhase(
                    snapshot = context.snapshot,
                    hookName = hookName,
                    moveEffect = context.moveEffect,
                    selfId = context.attackerId,
                    targetId = context.targetId,
                    sourceId = context.sourceId,
                    relay = relay,
                    attributes = context.attributes,
                )
            context.snapshot = result.snapshot
            if (result.cancelled) {
                context.markCancelled(result.snapshot)
                return
            }
        }
    }
}
