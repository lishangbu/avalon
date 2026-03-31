package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 绑定到某个 side 的 action。
 */
interface BattleSessionSideAction : BattleSessionAction {
    /**
     * 当前 action 所属的 side 标识。
     */
    val sideId: String
}
