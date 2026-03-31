package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 基于 effect 定义执行的 choice。
 */
interface BattleSessionEffectChoice : BattleSessionTargetedChoice {
    /**
     * 当前 choice 对应的 effect 标识。
     */
    val effectId: String
}
