package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 由某个具体单位提交的 choice。
 */
interface BattleSessionSubmittingChoice : BattleSessionChoice {
    /**
     * 提交当前 choice 的单位标识。
     */
    val submittingUnitId: String
}
