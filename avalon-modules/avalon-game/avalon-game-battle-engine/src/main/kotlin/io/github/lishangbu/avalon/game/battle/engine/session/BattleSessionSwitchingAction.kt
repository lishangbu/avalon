package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 换人 action。
 */
interface BattleSessionSwitchingAction :
    BattleSessionSideAction,
    BattleSessionSubmittingAction {
    /**
     * 当前下场单位标识。
     */
    val outgoingUnitId: String

    /**
     * 即将上场单位标识。
     */
    val incomingUnitId: String

    /**
     * 提交当前 action 的单位标识。
     */
    override val submittingUnitId: String
        get() = outgoingUnitId
}
