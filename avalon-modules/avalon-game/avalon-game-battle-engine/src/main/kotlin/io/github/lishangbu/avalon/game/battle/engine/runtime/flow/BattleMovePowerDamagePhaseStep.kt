package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames

/**
 * 威力与伤害修正 phase step。
 *
 * @property phaseProcessor battle hook phase 处理器。
 */
class BattleMovePowerDamagePhaseStep(
    private val phaseProcessor: BattleFlowPhaseProcessor,
) : BattleMoveResolutionStep {
    /**
     * 当前步骤在 pipeline 中的执行顺序。
     */
    override val order: Int = 300

    /**
     * 执行威力与伤害修正阶段。
     */
    override fun execute(context: BattleMoveResolutionContext) {
        val basePowerResult =
            phaseProcessor.processPhase(
                snapshot = context.snapshot,
                hookName = StandardHookNames.ON_MODIFY_BASE_POWER.value,
                moveEffect = context.moveEffect,
                selfId = context.attackerId,
                targetId = context.targetId,
                sourceId = context.sourceId,
                relay = context.basePower.toDouble(),
                attributes = context.attributes,
            )
        context.snapshot = basePowerResult.snapshot
        context.basePower = (basePowerResult.relay as? Number)?.toInt() ?: context.basePower

        val damageResult =
            phaseProcessor.processPhase(
                snapshot = context.snapshot,
                hookName = StandardHookNames.ON_MODIFY_DAMAGE.value,
                moveEffect = context.moveEffect,
                selfId = context.attackerId,
                targetId = context.targetId,
                sourceId = context.sourceId,
                relay = context.damage.toDouble(),
                attributes = context.attributes,
            )
        context.snapshot = damageResult.snapshot
        context.damage = (damageResult.relay as? Number)?.toInt() ?: context.damage
    }
}
