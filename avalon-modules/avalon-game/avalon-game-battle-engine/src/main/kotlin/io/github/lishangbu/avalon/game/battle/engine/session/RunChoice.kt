package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 逃跑输入 DTO。
 *
 * @property sideId 发起逃跑的 side 标识。
 * @property priority 行动优先级。
 * @property speed 排序所需速度值。
 */
data class RunChoice(
    override val sideId: String,
    override val priority: Int = 0,
    override val speed: Int = 0,
) : BattleSessionSideChoice {
    /**
     * 当前 choice 的命令种类。
     */
    override val kind: BattleSessionChoiceKind = BattleSessionChoiceKind.RUN
}
