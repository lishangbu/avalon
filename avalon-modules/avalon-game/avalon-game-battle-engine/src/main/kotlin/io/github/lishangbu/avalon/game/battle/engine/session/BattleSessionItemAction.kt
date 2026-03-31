package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * BattleSession 中最小可用的物品使用行动。
 *
 * @property itemId 物品效果定义标识。
 * @property actorUnitId 使用者单位标识。
 * @property targetId 目标单位标识。
 * @property priority 行动优先级。
 * @property speed 排序所需速度值。
 * @property attributes 传递给 battle flow 的扩展属性。
 */
data class BattleSessionItemAction(
    val itemId: String,
    val actorUnitId: String,
    val targetId: String,
    override val kind: BattleSessionActionKind = BattleSessionActionKind.ITEM,
    override val priority: Int = 0,
    override val speed: Int = 0,
    val attributes: Map<String, Any?> = emptyMap(),
) : BattleSessionEffectAction {
    /**
     * 提交当前 action 的单位标识。
     */
    override val submittingUnitId: String
        get() = actorUnitId

    /**
     * 当前 action 的目标单位标识。
     */
    override val targetUnitId: String
        get() = targetId

    /**
     * 当前 action 对应的 effect 标识。
     */
    override val effectId: String
        get() = itemId
}
