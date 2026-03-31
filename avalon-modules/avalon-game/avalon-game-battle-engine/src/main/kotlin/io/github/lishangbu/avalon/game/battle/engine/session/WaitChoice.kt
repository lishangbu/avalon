package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 占位等待输入。
 *
 * 用于 AI 或脚本在当前回合不执行具体技能/道具/换人时补齐动作槽位。
 */
data class WaitChoice(
    val unitId: String,
    override val priority: Int = 0,
    override val speed: Int = 0,
) : BattleSessionSubmittingChoice {
    /**
     * 当前 choice 的命令种类。
     */
    override val kind: BattleSessionChoiceKind = BattleSessionChoiceKind.WAIT

    /**
     * 提交当前 choice 的单位标识。
     */
    override val submittingUnitId: String
        get() = unitId
}
