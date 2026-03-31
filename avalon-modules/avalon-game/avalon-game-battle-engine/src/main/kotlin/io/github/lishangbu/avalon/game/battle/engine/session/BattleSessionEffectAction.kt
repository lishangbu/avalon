package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 基于 effect 定义执行的 action。
 */
interface BattleSessionEffectAction : BattleSessionTargetedAction {
    /**
     * 当前 action 对应的 effect 标识。
     */
    val effectId: String
}
