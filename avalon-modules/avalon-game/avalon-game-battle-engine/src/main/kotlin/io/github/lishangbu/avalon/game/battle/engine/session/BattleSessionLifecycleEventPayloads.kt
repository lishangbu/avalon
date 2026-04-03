package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * battle session 启动事件 payload。
 */
data object BattleSessionStartedPayload : BattleSessionEventPayload {
    /**
     * 当前 payload 对应的结构化事件类型。
     */
    override val type: BattleSessionEventType = BattleSessionEventType.SESSION_STARTED
}

/**
 * side 注册事件 payload。
 *
 * @property sideId 本次注册的 side 标识。
 */
data class BattleSessionSideRegisteredPayload(
    val sideId: String,
) : BattleSessionEventPayload {
    /**
     * 当前 payload 对应的结构化事件类型。
     */
    override val type: BattleSessionEventType = BattleSessionEventType.SIDE_REGISTERED

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> = mapOf("sideId" to sideId)
}

/**
 * 单位注册事件 payload。
 *
 * @property sideId 单位所属 side 标识。
 * @property unitId 本次注册的单位标识。
 * @property active 单位是否直接加入 active 列表。
 */
data class BattleSessionUnitRegisteredPayload(
    val sideId: String,
    val unitId: String,
    val active: Boolean,
) : BattleSessionEventPayload {
    /**
     * 当前 payload 对应的结构化事件类型。
     */
    override val type: BattleSessionEventType = BattleSessionEventType.UNIT_REGISTERED

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> =
        mapOf(
            "sideId" to sideId,
            "unitId" to unitId,
            "active" to active,
        )
}

/**
 * 捕捉失败事件 payload。
 *
 * @property ballItemId 使用的球类道具标识。
 * @property targetUnitId 捕捉目标单位标识。
 * @property shakes 本次捕捉的摇晃次数。
 * @property reason 捕捉失败原因。
 * @property finalRate 本次捕捉的最终概率。
 */
data class BattleSessionCaptureFailedPayload(
    val ballItemId: String,
    val targetUnitId: String,
    val shakes: Int,
    val reason: String,
    val finalRate: Double,
) : BattleSessionEventPayload {
    /**
     * 当前 payload 对应的结构化事件类型。
     */
    override val type: BattleSessionEventType = BattleSessionEventType.CAPTURE_FAILED

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> =
        mapOf(
            "ballItemId" to ballItemId,
            "targetUnitId" to targetUnitId,
            "shakes" to shakes,
            "reason" to reason,
            "finalRate" to finalRate,
        )
}

/**
 * 逃跑失败事件 payload。
 *
 * @property sideId 发起逃跑的 side 标识。
 * @property runnerUnitId 代表本次逃跑尝试的单位标识。
 * @property reason 本次逃跑失败原因。
 * @property failedAttempts 当前 side 已累计的失败逃跑次数。
 * @property escapeValue 本次公式计算得到的逃走值；若未进入公式则为空。
 * @property roll 本次随机判定值；若未进入随机判定则为空。
 */
data class BattleSessionRunFailedPayload(
    val sideId: String,
    val runnerUnitId: String?,
    val reason: String,
    val failedAttempts: Int,
    val escapeValue: Int? = null,
    val roll: Int? = null,
) : BattleSessionEventPayload {
    /**
     * 当前 payload 对应的结构化事件类型。
     */
    override val type: BattleSessionEventType = BattleSessionEventType.RUN_FAILED

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> =
        buildMap {
            put("sideId", sideId)
            put("reason", reason)
            put("failedAttempts", failedAttempts)
            runnerUnitId?.let { put("runnerUnitId", it) }
            escapeValue?.let { put("escapeValue", it) }
            roll?.let { put("roll", it) }
        }
}

/**
 * 捕捉成功事件 payload。
 *
 * @property targetUnitId 被成功捕捉的目标单位标识。
 */
data class BattleSessionCaptureSucceededPayload(
    val targetUnitId: String,
) : BattleSessionEventPayload {
    /**
     * 当前 payload 对应的结构化事件类型。
     */
    override val type: BattleSessionEventType = BattleSessionEventType.CAPTURE_SUCCEEDED

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> = mapOf("targetUnitId" to targetUnitId)
}

/**
 * 回合结算完成事件 payload。
 *
 * @property actionCount 本回合已执行的 action 数量。
 */
data class BattleSessionTurnResolvedPayload(
    val actionCount: Int,
) : BattleSessionEventPayload {
    /**
     * 当前 payload 对应的结构化事件类型。
     */
    override val type: BattleSessionEventType = BattleSessionEventType.TURN_RESOLVED

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> = mapOf("actionCount" to actionCount)
}

/**
 * 回合结束事件 payload。
 */
data object BattleSessionTurnEndedPayload : BattleSessionEventPayload {
    /**
     * 当前 payload 对应的结构化事件类型。
     */
    override val type: BattleSessionEventType = BattleSessionEventType.TURN_ENDED
}

/**
 * 替补完成事件 payload。
 *
 * @property sideId 发生替补的 side 标识。
 * @property before 替补前的 active 单位列表；手动替补时可为空。
 * @property after 替补后的 active 单位列表。
 * @property manual 当前替补是否来自手动提交；自动替补时可为空。
 */
data class BattleSessionAutoReplacedPayload(
    val sideId: String,
    val before: List<String>? = null,
    val after: List<String>,
    val manual: Boolean? = null,
) : BattleSessionEventPayload {
    /**
     * 当前 payload 对应的结构化事件类型。
     */
    override val type: BattleSessionEventType = BattleSessionEventType.AUTO_REPLACED

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> =
        buildMap {
            put("sideId", sideId)
            before?.let { put("before", it) }
            put("after", after)
            manual?.let { put("manual", it) }
        }
}

/**
 * 战斗结束事件 payload。
 *
 * @property winner 当前战斗胜者标识；平局时为空。
 * @property actionType 如果战斗由某类 action 直接结束，则记录该动作种类。
 * @property runner 如果战斗由逃跑直接结束，则记录逃跑方 side 标识。
 */
data class BattleSessionBattleEndedPayload(
    val winner: String?,
    val actionType: BattleSessionActionEventKind? = null,
    val runner: String? = null,
) : BattleSessionEventPayload {
    /**
     * 当前 payload 对应的结构化事件类型。
     */
    override val type: BattleSessionEventType = BattleSessionEventType.BATTLE_ENDED

    /**
     * 把当前 payload 映射为兼容现有读模型的 attributes。
     */
    override fun toAttributes(): Map<String, Any?> =
        buildMap {
            put("winner", winner)
            actionType?.let { put("actionType", it.wireValue) }
            runner?.let { put("runner", it) }
        }
}
