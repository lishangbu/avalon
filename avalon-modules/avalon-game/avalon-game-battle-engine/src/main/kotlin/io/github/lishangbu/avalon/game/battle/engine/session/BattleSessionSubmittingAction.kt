package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 由某个具体单位提交的 action。
 */
interface BattleSessionSubmittingAction : BattleSessionAction {
    /**
     * 提交当前 action 的单位标识。
     */
    val submittingUnitId: String
}
