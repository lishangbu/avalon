package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * BattleSession 中的捕捉行动。
 */
data class BattleSessionCaptureAction(
    val playerId: String,
    val ballItemId: String,
    val sourceUnitId: String,
    val targetId: String,
    override val kind: BattleSessionActionKind = BattleSessionActionKind.CAPTURE,
    override val priority: Int = 0,
    override val speed: Int = 0,
) : BattleSessionTargetedAction {
    /**
     * 提交当前 action 的单位标识。
     */
    override val submittingUnitId: String
        get() = sourceUnitId

    /**
     * 当前 action 的目标单位标识。
     */
    override val targetUnitId: String
        get() = targetId
}
