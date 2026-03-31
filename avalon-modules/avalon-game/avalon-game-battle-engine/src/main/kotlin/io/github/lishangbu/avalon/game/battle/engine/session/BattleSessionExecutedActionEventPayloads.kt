package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * battle session action 执行完成事件 payload 基类。
 *
 * @property actionType 当前已执行动作的种类。
 */
sealed interface BattleSessionExecutedActionPayload : BattleSessionEventPayload {
    val actionType: BattleSessionActionEventKind

    override val type: BattleSessionEventType
        get() = BattleSessionEventType.MOVE_EXECUTED
}

/**
 * move action 执行完成事件 payload。
 *
 * @property moveId 招式标识。
 * @property attackerId 出手单位标识。
 * @property targetId 目标单位标识。
 * @property hitSuccessful 当前招式是否命中。
 * @property basePower 当前招式最终威力。
 * @property damage 当前招式最终伤害。
 */
data class BattleSessionMoveExecutedPayload(
    val moveId: String,
    val attackerId: String,
    val targetId: String,
    val hitSuccessful: Boolean,
    val basePower: Int,
    val damage: Int,
) : BattleSessionExecutedActionPayload {
    /**
     * 当前已执行动作的种类。
     */
    override val actionType: BattleSessionActionEventKind = BattleSessionActionEventKind.MOVE

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> =
        mapOf(
            "actionType" to actionType.wireValue,
            "moveId" to moveId,
            "attackerId" to attackerId,
            "targetId" to targetId,
            "hitSuccessful" to hitSuccessful,
            "basePower" to basePower,
            "damage" to damage,
        )
}

/**
 * switch action 执行完成事件 payload。
 *
 * @property sideId 发起换人的 side 标识。
 * @property outgoingUnitId 当前下场单位标识。
 * @property incomingUnitId 即将上场单位标识。
 */
data class BattleSessionSwitchExecutedPayload(
    val sideId: String,
    val outgoingUnitId: String,
    val incomingUnitId: String,
) : BattleSessionExecutedActionPayload {
    /**
     * 当前已执行动作的种类。
     */
    override val actionType: BattleSessionActionEventKind = BattleSessionActionEventKind.SWITCH

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> =
        mapOf(
            "actionType" to actionType.wireValue,
            "sideId" to sideId,
            "outgoingUnitId" to outgoingUnitId,
            "incomingUnitId" to incomingUnitId,
        )
}

/**
 * item action 执行完成事件 payload。
 *
 * @property itemId 物品标识。
 * @property actorUnitId 使用者单位标识。
 * @property targetId 目标单位标识。
 */
data class BattleSessionItemExecutedPayload(
    val itemId: String,
    val actorUnitId: String,
    val targetId: String,
) : BattleSessionExecutedActionPayload {
    /**
     * 当前已执行动作的种类。
     */
    override val actionType: BattleSessionActionEventKind = BattleSessionActionEventKind.ITEM

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> =
        mapOf(
            "actionType" to actionType.wireValue,
            "itemId" to itemId,
            "actorUnitId" to actorUnitId,
            "targetId" to targetId,
        )
}

/**
 * wait action 执行完成事件 payload。
 *
 * @property unitId 发起等待的单位标识。
 */
data class BattleSessionWaitExecutedPayload(
    val unitId: String,
) : BattleSessionExecutedActionPayload {
    /**
     * 当前已执行动作的种类。
     */
    override val actionType: BattleSessionActionEventKind = BattleSessionActionEventKind.WAIT

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> =
        mapOf(
            "actionType" to actionType.wireValue,
            "unitId" to unitId,
        )
}
