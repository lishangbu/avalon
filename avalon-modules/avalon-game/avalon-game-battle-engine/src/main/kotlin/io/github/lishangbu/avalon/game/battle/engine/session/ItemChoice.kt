package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 物品使用输入 DTO。
 *
 * @property itemId 物品效果定义标识。
 * @property actorUnitId 使用者单位标识。
 * @property targetId 目标单位标识。
 * @property priority 行动优先级。
 * @property speed 排序所需速度值。
 * @property attributes 透传给 battle flow 的扩展属性。
 */
data class ItemChoice(
    val itemId: String,
    val actorUnitId: String,
    val targetId: String,
    override val priority: Int = 0,
    override val speed: Int = 0,
    val attributes: Map<String, Any?> = emptyMap(),
) : BattleSessionEffectChoice {
    /**
     * 当前 choice 的命令种类。
     */
    override val kind: BattleSessionChoiceKind = BattleSessionChoiceKind.ITEM

    /**
     * 提交当前 choice 的单位标识。
     */
    override val submittingUnitId: String
        get() = actorUnitId

    /**
     * 当前 choice 选择的目标单位标识。
     */
    override val targetUnitId: String
        get() = targetId

    /**
     * 当前 choice 对应的 effect 标识。
     */
    override val effectId: String
        get() = itemId
}
