package io.github.lishangbu.avalon.game.service.battle.view

import io.github.lishangbu.avalon.game.battle.engine.session.target.BattleSessionTargetMode

/**
 * 面向前端的目标查询视图。
 *
 * @property effectId 被查询的 effect 标识。
 * @property actorUnitId 当前出手单位标识。
 * @property mode 当前目标模式。
 * @property availableTargetUnitIds 当前可选目标单位标识列表。
 * @property requiresExplicitTarget 当前是否必须显式选目标。
 */
data class GameBattleTargetQueryView(
    val effectId: String,
    val actorUnitId: String,
    val mode: BattleSessionTargetMode,
    val availableTargetUnitIds: List<String>,
    val requiresExplicitTarget: Boolean,
)

/**
 * 面向前端的已捕捉生物摘要视图。
 *
 * @property ownedCreatureId 已捕捉实例标识。
 * @property creatureId 生物标识。
 * @property creatureSpeciesId 生物物种标识。
 * @property creatureInternalName 生物内部名称。
 * @property creatureName 生物展示名称。
 */
data class GameBattleCapturedCreatureView(
    val ownedCreatureId: String,
    val creatureId: String,
    val creatureSpeciesId: String,
    val creatureInternalName: String,
    val creatureName: String,
)

/**
 * 面向前端的捕捉结算视图。
 *
 * @property success 本次捕捉是否成功。
 * @property sessionId 战斗会话标识。
 * @property targetUnitId 目标单位标识。
 * @property ballItemId 使用的球类道具标识。
 * @property shakes 本次捕捉摇晃次数。
 * @property reason 本次捕捉结果原因。
 * @property battleEnded 本次捕捉是否导致战斗结束。
 * @property finalRate 本次捕捉最终概率。
 * @property capturedCreature 如果捕捉成功并已落库，则附带被捕捉生物摘要。
 */
data class GameBattleCaptureResultView(
    val success: Boolean,
    val sessionId: String,
    val targetUnitId: String,
    val ballItemId: String,
    val shakes: Int,
    val reason: String,
    val battleEnded: Boolean,
    val finalRate: Double,
    val capturedCreature: GameBattleCapturedCreatureView? = null,
)

/**
 * 面向前端的战斗结算视图。
 *
 * @property sessionId 战斗会话标识。
 * @property endedReason 战斗结束原因。
 * @property settled 当前战斗是否已经完成结算。
 * @property session 当前会话视图。
 * @property captureResult 如果是捕捉战斗，则附带捕捉结算视图。
 */
data class GameBattleSettlementView(
    val sessionId: String,
    val endedReason: String?,
    val settled: Boolean,
    val session: GameBattleSessionView,
    val captureResult: GameBattleCaptureResultView? = null,
)
