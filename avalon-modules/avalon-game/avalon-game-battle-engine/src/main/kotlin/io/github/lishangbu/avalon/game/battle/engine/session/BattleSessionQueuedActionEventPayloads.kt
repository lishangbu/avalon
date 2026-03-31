package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * battle session action 入队事件 payload 基类。
 *
 * @property actionType 当前入队动作的种类。
 * @property priority 当前动作优先级。
 * @property speed 当前动作排序速度。
 */
sealed interface BattleSessionQueuedActionPayload : BattleSessionEventPayload {
    val actionType: BattleSessionActionEventKind
    val priority: Int
    val speed: Int

    override val type: BattleSessionEventType
        get() = BattleSessionEventType.MOVE_QUEUED
}

/**
 * move action 入队事件 payload。
 *
 * @property moveId 招式标识。
 * @property attackerId 出手单位标识。
 * @property targetId 目标单位标识。
 * @property priority 当前动作优先级。
 * @property speed 当前动作排序速度。
 */
data class BattleSessionMoveQueuedPayload(
    val moveId: String,
    val attackerId: String,
    val targetId: String,
    override val priority: Int,
    override val speed: Int,
) : BattleSessionQueuedActionPayload {
    /**
     * 当前入队动作的种类。
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
            "priority" to priority,
            "speed" to speed,
        )
}

/**
 * switch action 入队事件 payload。
 *
 * @property sideId 发起换人的 side 标识。
 * @property outgoingUnitId 当前下场单位标识。
 * @property incomingUnitId 即将上场单位标识。
 * @property priority 当前动作优先级。
 * @property speed 当前动作排序速度。
 */
data class BattleSessionSwitchQueuedPayload(
    val sideId: String,
    val outgoingUnitId: String,
    val incomingUnitId: String,
    override val priority: Int,
    override val speed: Int,
) : BattleSessionQueuedActionPayload {
    /**
     * 当前入队动作的种类。
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
            "priority" to priority,
            "speed" to speed,
        )
}

/**
 * item action 入队事件 payload。
 *
 * @property itemId 物品标识。
 * @property actorUnitId 使用者单位标识。
 * @property targetId 目标单位标识。
 * @property priority 当前动作优先级。
 * @property speed 当前动作排序速度。
 */
data class BattleSessionItemQueuedPayload(
    val itemId: String,
    val actorUnitId: String,
    val targetId: String,
    override val priority: Int,
    override val speed: Int,
) : BattleSessionQueuedActionPayload {
    /**
     * 当前入队动作的种类。
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
            "priority" to priority,
            "speed" to speed,
        )
}

/**
 * capture action 入队事件 payload。
 *
 * @property playerId 发起捕捉的玩家标识。
 * @property ballItemId 使用的球类道具标识。
 * @property sourceUnitId 扔球单位标识。
 * @property targetId 目标野生单位标识。
 * @property priority 当前动作优先级。
 * @property speed 当前动作排序速度。
 */
data class BattleSessionCaptureQueuedPayload(
    val playerId: String,
    val ballItemId: String,
    val sourceUnitId: String,
    val targetId: String,
    override val priority: Int,
    override val speed: Int,
) : BattleSessionQueuedActionPayload {
    /**
     * 当前入队动作的种类。
     */
    override val actionType: BattleSessionActionEventKind = BattleSessionActionEventKind.CAPTURE

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> =
        mapOf(
            "actionType" to actionType.wireValue,
            "playerId" to playerId,
            "ballItemId" to ballItemId,
            "sourceUnitId" to sourceUnitId,
            "targetId" to targetId,
            "priority" to priority,
            "speed" to speed,
        )
}

/**
 * run action 入队事件 payload。
 *
 * @property sideId 发起逃跑的 side 标识。
 * @property priority 当前动作优先级。
 * @property speed 当前动作排序速度。
 */
data class BattleSessionRunQueuedPayload(
    val sideId: String,
    override val priority: Int,
    override val speed: Int,
) : BattleSessionQueuedActionPayload {
    /**
     * 当前入队动作的种类。
     */
    override val actionType: BattleSessionActionEventKind = BattleSessionActionEventKind.RUN

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> =
        mapOf(
            "actionType" to actionType.wireValue,
            "sideId" to sideId,
            "priority" to priority,
            "speed" to speed,
        )
}

/**
 * wait action 入队事件 payload。
 *
 * @property unitId 发起等待的单位标识。
 * @property priority 当前动作优先级。
 * @property speed 当前动作排序速度。
 */
data class BattleSessionWaitQueuedPayload(
    val unitId: String,
    override val priority: Int,
    override val speed: Int,
) : BattleSessionQueuedActionPayload {
    /**
     * 当前入队动作的种类。
     */
    override val actionType: BattleSessionActionEventKind = BattleSessionActionEventKind.WAIT

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> =
        mapOf(
            "actionType" to actionType.wireValue,
            "unitId" to unitId,
            "priority" to priority,
            "speed" to speed,
        )
}
