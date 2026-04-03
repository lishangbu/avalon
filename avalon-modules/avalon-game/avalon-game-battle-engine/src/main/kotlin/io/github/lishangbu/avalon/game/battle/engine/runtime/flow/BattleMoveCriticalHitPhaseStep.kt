package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.event.StandardHookNames

/**
 * 要害判定与倍率应用 phase step。
 *
 * @property phaseProcessor battle hook phase 处理器。
 */
class BattleMoveCriticalHitPhaseStep(
    private val phaseProcessor: BattleFlowPhaseProcessor,
) : BattleMoveResolutionStep {
    /**
     * 当前步骤在 pipeline 中的执行顺序。
     */
    override val order: Int = 250

    /**
     * 执行要害等级修正、随机判定与伤害倍率应用。
     */
    override fun execute(context: BattleMoveResolutionContext) {
        if (!context.hitSuccessful) {
            context.criticalHit = false
            return
        }
        val damageClass = context.moveEffect.data["damageClass"]?.toString()
        if (damageClass == null || damageClass == "status" || context.basePower <= 0) {
            context.criticalHit = false
            return
        }
        val explicitCritical = context.attributes["criticalHit"] as? Boolean
        if (explicitCritical != null) {
            context.criticalHit = explicitCritical
            return
        }

        val baseCritRatio = resolveBaseCritRatio(context)
        val critRatioResult =
            phaseProcessor.processPhase(
                snapshot = context.snapshot,
                hookName = StandardHookNames.ON_MODIFY_CRIT_RATIO.value,
                moveEffect = context.moveEffect,
                selfId = context.attackerId,
                targetId = context.targetId,
                sourceId = context.sourceId,
                relay = baseCritRatio,
                attributes =
                    context.attributes +
                        mapOf(
                            "baseCritRatio" to baseCritRatio,
                            "damageClass" to damageClass,
                        ),
            )
        context.snapshot = critRatioResult.snapshot
        val critRatio = ((critRatioResult.relay as? Number)?.toInt() ?: baseCritRatio).coerceIn(0, MAX_CRIT_RATIO)
        context.criticalHit = determineCriticalHit(context, critRatio)
    }

    private fun resolveBaseCritRatio(context: BattleMoveResolutionContext): Int {
        if (context.attributes["alwaysCriticalHit"] == true) {
            return GUARANTEED_CRIT_RATIO
        }
        val attributeCritRatioBonus = (context.attributes["critRatio"] as? Number)?.toInt()
        val dataCritRatioBonus = (context.moveEffect.data["critRatio"] as? Number)?.toInt()
        val moveAlwaysCritical = context.moveEffect.data["alwaysCriticalHit"] == true
        return when {
            moveAlwaysCritical -> GUARANTEED_CRIT_RATIO
            else -> (DEFAULT_CRIT_RATIO + (attributeCritRatioBonus ?: dataCritRatioBonus ?: 0)).coerceAtLeast(0)
        }
    }

    private fun determineCriticalHit(
        context: BattleMoveResolutionContext,
        critRatio: Int,
    ): Boolean {
        if (critRatio >= GUARANTEED_CRIT_RATIO) {
            return true
        }
        if (critRatio <= 0) {
            return false
        }
        val denominator = CRIT_DENOMINATORS[critRatio] ?: return false
        val explicitRoll = (context.attributes["criticalRoll"] as? Number)?.toInt()
        val roll = explicitRoll ?: nextRandomInt(context, denominator)
        return roll == 0
    }

    private fun nextRandomInt(
        context: BattleMoveResolutionContext,
        bound: Int,
    ): Int {
        val randomResult =
            context.snapshot.battle.randomState
                .nextInt(bound)
        context.snapshot =
            context.snapshot.copy(
                battle =
                    context.snapshot.battle.copy(
                        randomState = randomResult.nextState,
                    ),
            )
        return randomResult.value
    }

    private companion object {
        private const val MAX_CRIT_RATIO: Int = 4
        private const val DEFAULT_CRIT_RATIO: Int = 1
        private const val GUARANTEED_CRIT_RATIO: Int = 4
        private val CRIT_DENOMINATORS: Map<Int, Int> =
            mapOf(
                1 to 24,
                2 to 8,
                3 to 2,
            )
    }
}
