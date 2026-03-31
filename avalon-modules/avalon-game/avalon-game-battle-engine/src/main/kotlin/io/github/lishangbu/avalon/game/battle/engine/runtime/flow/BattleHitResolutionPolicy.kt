package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

/**
 * battle 命中判定策略。
 *
 * 设计意图：
 * - 把 accuracy / evasion / 显式命中结果的决策逻辑从 flow 主体中拆出。
 * - 让不同规则集可以替换命中判定策略。
 */
interface BattleHitResolutionPolicy {
    /**
     * 计算当前出招是否命中。
     *
     * @param accuracy 当前阶段的命中值。
     * @param evasion 当前阶段的回避值。
     * @param attributes 透传的 battle 附加属性。
     * @return 当前出招是否成功命中目标。
     */
    fun determine(
        accuracy: Int?,
        evasion: Int?,
        attributes: Map<String, Any?>,
    ): Boolean
}
