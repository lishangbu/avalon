package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * BattleSession 中的等待行动。
 */
data class BattleSessionWaitAction(
    val unitId: String,
    override val kind: BattleSessionActionKind = BattleSessionActionKind.WAIT,
    override val priority: Int = 0,
    override val speed: Int = 0,
) : BattleSessionSubmittingAction {
    /**
     * 提交当前 action 的单位标识。
     */
    override val submittingUnitId: String
        get() = unitId
}
