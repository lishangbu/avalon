package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 带显式目标的 choice。
 */
interface BattleSessionTargetedChoice : BattleSessionSubmittingChoice {
    /**
     * 当前 choice 选择的目标单位标识。
     */
    val targetUnitId: String
}
