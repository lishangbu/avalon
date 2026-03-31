package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 替换输入 DTO。
 *
 * @property sideId 发起替换的 side 标识。
 * @property outgoingUnitId 当前下场单位标识。
 * @property incomingUnitId 当前上场单位标识。
 * @property priority 行动优先级。
 * @property speed 排序所需速度值。
 */
data class SwitchChoice(
    override val sideId: String,
    override val outgoingUnitId: String,
    override val incomingUnitId: String,
    override val priority: Int = 0,
    override val speed: Int = 0,
) : BattleSessionSwitchingChoice {
    /**
     * 当前 choice 的命令种类。
     */
    override val kind: BattleSessionChoiceKind = BattleSessionChoiceKind.SWITCH
}
