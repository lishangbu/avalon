package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import kotlin.math.roundToInt

/**
 * 默认 battle 命中判定策略。
 */
class DefaultBattleHitResolutionPolicy : BattleHitResolutionPolicy {
    /**
     * 计算当前出招是否命中。
     */
    override fun determine(
        accuracy: Int?,
        evasion: Int?,
        attributes: Map<String, Any?>,
    ): Boolean {
        val explicitResult = attributes["hitSuccessful"] as? Boolean
        if (explicitResult != null) {
            return explicitResult
        }
        val accuracyRoll = (attributes["accuracyRoll"] as? Number)?.toInt() ?: return true
        val effectiveAccuracy = accuracy ?: 100
        val effectiveEvasion = if (evasion == null || evasion <= 0) 100 else evasion
        val hitChance =
            ((effectiveAccuracy.toDouble() / effectiveEvasion.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
        return accuracyRoll <= hitChance
    }
}
