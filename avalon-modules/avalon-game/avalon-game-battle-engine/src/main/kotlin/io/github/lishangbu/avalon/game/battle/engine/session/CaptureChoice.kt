package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 捕捉输入 DTO。
 *
 * @property playerId 发起捕捉的玩家标识。
 * @property ballItemId 使用的球类道具内部标识。
 * @property sourceUnitId 扔球的己方单位标识。
 * @property targetId 目标野生单位标识。
 * @property priority 行动优先级。
 * @property speed 排序所需速度值。
 */
data class CaptureChoice(
    val playerId: String,
    val ballItemId: String,
    val sourceUnitId: String,
    val targetId: String,
    override val priority: Int = 0,
    override val speed: Int = 0,
) : BattleSessionTargetedChoice {
    /**
     * 当前 choice 的命令种类。
     */
    override val kind: BattleSessionChoiceKind = BattleSessionChoiceKind.CAPTURE

    /**
     * 提交当前 choice 的单位标识。
     */
    override val submittingUnitId: String
        get() = sourceUnitId

    /**
     * 当前 choice 选择的目标单位标识。
     */
    override val targetUnitId: String
        get() = targetId
}
