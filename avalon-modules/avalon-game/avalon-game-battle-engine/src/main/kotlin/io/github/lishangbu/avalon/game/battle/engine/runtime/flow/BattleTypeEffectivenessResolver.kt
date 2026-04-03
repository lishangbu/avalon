package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

import io.github.lishangbu.avalon.game.battle.engine.model.UnitState

/**
 * battle engine 内部使用的属性克制解析器。
 *
 * 设计意图：
 * - 让伤害主公式可以在 engine 内部统一消费属性克制倍率。
 * - 通过接口隔离具体数据来源，默认实现可保持为无修正。
 */
fun interface BattleTypeEffectivenessResolver {
    /**
     * 解析当前招式类型对目标的最终属性倍率。
     */
    fun resolve(
        moveType: String?,
        attacker: UnitState?,
        target: UnitState?,
    ): Double
}

/**
 * 默认的无属性克制修正实现。
 */
object NoopBattleTypeEffectivenessResolver : BattleTypeEffectivenessResolver {
    override fun resolve(
        moveType: String?,
        attacker: UnitState?,
        target: UnitState?,
    ): Double = 1.0
}
