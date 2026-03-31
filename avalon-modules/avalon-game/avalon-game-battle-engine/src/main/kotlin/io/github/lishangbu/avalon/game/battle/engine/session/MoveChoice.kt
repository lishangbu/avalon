package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 出招输入 DTO。
 *
 * @property moveId 招式标识。
 * @property attackerId 行动发起者单位标识。
 * @property targetId 目标单位标识。
 * @property priority 行动优先级。
 * @property speed 排序所需速度值。
 * @property accuracy 命中值输入。
 * @property evasion 回避值输入。
 * @property basePower 招式基础威力输入。
 * @property damage 招式基础伤害输入。
 * @property attributes 透传给 battle flow 的扩展属性。
 */
data class MoveChoice(
    val moveId: String,
    val attackerId: String,
    val targetId: String,
    override val priority: Int = 0,
    override val speed: Int = 0,
    val accuracy: Int? = null,
    val evasion: Int? = null,
    val basePower: Int,
    val damage: Int,
    val attributes: Map<String, Any?> = emptyMap(),
) : BattleSessionEffectChoice {
    /**
     * 当前 choice 的命令种类。
     */
    override val kind: BattleSessionChoiceKind = BattleSessionChoiceKind.MOVE

    /**
     * 提交当前 choice 的单位标识。
     */
    override val submittingUnitId: String
        get() = attackerId

    /**
     * 当前 choice 选择的目标单位标识。
     */
    override val targetUnitId: String
        get() = targetId

    /**
     * 当前 choice 对应的 effect 标识。
     */
    override val effectId: String
        get() = moveId
}
