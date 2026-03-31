package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 绑定到某个 side 的 choice。
 */
interface BattleSessionSideChoice : BattleSessionChoice {
    /**
     * 当前 choice 所属的 side 标识。
     */
    val sideId: String
}
