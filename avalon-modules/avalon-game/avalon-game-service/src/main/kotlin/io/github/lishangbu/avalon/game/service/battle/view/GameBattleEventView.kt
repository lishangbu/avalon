package io.github.lishangbu.avalon.game.service.battle.view

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionEventType
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionResourceUsageKind

/**
 * 面向前端的 battle 事件视图。
 *
 * @property type 当前事件类型。
 * @property turn 当前事件发生时的回合数。
 * @property payloadType 当前事件 payload 的强类型名称。
 * @property attributes 与旧事件模型兼容的属性映射。
 */
data class GameBattleEventView(
    val type: BattleSessionEventType,
    val turn: Int,
    val payloadType: String,
    val attributes: Map<String, Any?>,
)

/**
 * 面向前端的通用资源账本视图。
 *
 * @property kind 当前账本条目种类。
 * @property payloadType 当前账本条目的强类型名称。
 * @property attributes 与前端兼容的账本属性映射。
 */
data class GameBattleResourceUsageView(
    val kind: BattleSessionResourceUsageKind,
    val payloadType: String,
    val attributes: Map<String, Any?>,
)

/**
 * 面向前端的捕捉资源账本视图。
 *
 * @property playerId 发起捕捉的玩家标识。
 * @property sourceUnitId 扔球单位标识。
 * @property ballItemId 使用的球类道具标识。
 * @property targetUnitId 目标单位标识。
 * @property quantity 本次消耗数量。
 * @property success 本次捕捉是否成功。
 * @property shakes 本次捕捉的摇晃次数。
 * @property reason 本次捕捉结果原因。
 * @property finalRate 本次捕捉的最终概率。
 */
data class GameBattleCaptureResourceUsageView(
    val playerId: String,
    val sourceUnitId: String,
    val ballItemId: String,
    val targetUnitId: String,
    val quantity: Int,
    val success: Boolean,
    val shakes: Int,
    val reason: String,
    val finalRate: Double,
)
