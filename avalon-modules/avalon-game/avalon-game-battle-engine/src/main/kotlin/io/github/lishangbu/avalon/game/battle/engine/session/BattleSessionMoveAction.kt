package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * BattleSession 中最小可用的出招动作请求。
 *
 * 设计意图：
 * - 作为一回合内待执行动作的稳定数据结构。
 * - 当前阶段只覆盖 move action，不扩展到换人、用道具等其它行动。
 *
 * @property moveId 招式标识。
 * @property attackerId 行动发起者单位标识。
 * @property targetId 目标单位标识。
 * @property priority 显式行动优先级，值越大越先结算。
 * @property speed 行动发起者的速度值，优先级相同时用于排序。
 * @property accuracy 命中值输入。
 * @property evasion 回避值输入。
 * @property basePower 招式基础威力输入。
 * @property damage 招式基础伤害输入。
 * @property attributes 传递给 battle flow 的扩展属性。
 */
data class BattleSessionMoveAction(
    val moveId: String,
    val attackerId: String,
    val targetId: String,
    override val kind: BattleSessionActionKind = BattleSessionActionKind.MOVE,
    override val priority: Int = 0,
    override val speed: Int = 0,
    val accuracy: Int? = null,
    val evasion: Int? = null,
    val basePower: Int,
    val damage: Int,
    val attributes: Map<String, Any?> = emptyMap(),
) : BattleSessionEffectAction {
    /**
     * 提交当前 action 的单位标识。
     */
    override val submittingUnitId: String
        get() = attackerId

    /**
     * 当前 action 的目标单位标识。
     */
    override val targetUnitId: String
        get() = targetId

    /**
     * 当前 action 对应的 effect 标识。
     */
    override val effectId: String
        get() = moveId
}
