package io.github.lishangbu.avalon.game.service.battle.view

import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionActionKind

/**
 * 面向前端的战斗动作视图。
 *
 * @property kind 当前动作种类。
 * @property priority 当前动作优先级。
 * @property speed 当前动作排序速度。
 * @property submittingUnitId 提交当前动作的单位标识。
 * @property sideId 当前动作所属 side 标识。
 * @property effectId 当前动作对应的 effect 标识。
 * @property targetUnitId 当前动作目标单位标识。
 * @property playerId 发起捕捉的玩家标识。
 * @property ballItemId 使用的球类道具标识。
 * @property outgoingUnitId 当前下场单位标识。
 * @property incomingUnitId 即将上场单位标识。
 */
data class GameBattleActionView(
    val kind: BattleSessionActionKind,
    val priority: Int,
    val speed: Int,
    val submittingUnitId: String? = null,
    val sideId: String? = null,
    val effectId: String? = null,
    val targetUnitId: String? = null,
    val playerId: String? = null,
    val ballItemId: String? = null,
    val outgoingUnitId: String? = null,
    val incomingUnitId: String? = null,
)

/**
 * 面向前端的 move 结算视图。
 *
 * @property cancelled 本次出招是否在前置阶段被取消。
 * @property hitSuccessful 本次出招是否成功命中目标。
 * @property criticalHit 本次出招是否击中要害。
 * @property accuracy 经过修正后的命中值。
 * @property evasion 经过修正后的回避值。
 * @property basePower 经过修正后的威力。
 * @property damageRoll 本次伤害浮动使用的随机倍率；未参与伤害浮动时为空。
 * @property damage 经过修正后的伤害。
 */
data class GameBattleMoveResolutionView(
    val cancelled: Boolean,
    val hitSuccessful: Boolean,
    val criticalHit: Boolean,
    val accuracy: Int?,
    val evasion: Int?,
    val basePower: Int,
    val damageRoll: Int? = null,
    val damage: Int,
)

/**
 * 面向前端的捕捉动作执行结果视图。
 *
 * @property success 本次捕捉是否成功。
 * @property playerId 发起捕捉的玩家标识。
 * @property ballItemId 使用的球类道具标识。
 * @property sourceUnitId 扔球单位标识。
 * @property targetId 捕捉目标单位标识。
 * @property shakes 本次捕捉的摇晃次数。
 * @property reason 本次捕捉结果原因。
 * @property finalRate 本次捕捉的最终概率。
 */
data class GameBattleCaptureExecutionView(
    val success: Boolean,
    val playerId: String,
    val ballItemId: String,
    val sourceUnitId: String,
    val targetId: String,
    val shakes: Int,
    val reason: String,
    val finalRate: Double,
)

/**
 * 面向前端的单个动作执行结果视图。
 *
 * @property action 已执行动作视图。
 * @property snapshot 执行该动作后的快照视图。
 * @property moveResult 如果该动作是 move，则附带其结算视图。
 * @property captureResult 如果该动作是 capture，则附带其结算视图。
 */
data class GameBattleActionExecutionResultView(
    val action: GameBattleActionView,
    val snapshot: GameBattleSnapshotView,
    val moveResult: GameBattleMoveResolutionView? = null,
    val captureResult: GameBattleCaptureExecutionView? = null,
)

/**
 * 面向前端的整回合推进结果视图。
 *
 * @property actionResults 本回合动作执行结果视图列表。
 * @property snapshot 执行动作并完成回合结束后的最新快照视图。
 */
data class GameBattleTurnResultView(
    val actionResults: List<GameBattleActionExecutionResultView>,
    val snapshot: GameBattleSnapshotView,
)
