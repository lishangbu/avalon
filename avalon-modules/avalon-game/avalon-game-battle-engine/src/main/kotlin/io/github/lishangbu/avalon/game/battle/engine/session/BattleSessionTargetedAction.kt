package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 带显式目标的 action。
 */
interface BattleSessionTargetedAction : BattleSessionSubmittingAction {
    /**
     * 当前 action 的目标单位标识。
     */
    val targetUnitId: String
}
