package io.github.lishangbu.avalon.game.service.battle

import io.github.lishangbu.avalon.game.battle.engine.model.BattleType
import io.github.lishangbu.avalon.game.service.capture.CaptureBattleResult
import io.github.lishangbu.avalon.game.service.unit.BattleUnitImportRequest

/** 一方阵营的导入建局请求。 */
data class ImportedBattleSideRequest(
    val sideId: String,
    val units: List<BattleUnitImportRequest>,
    val activeUnitIds: Set<String> = emptySet(),
)

/** 使用真实数据快速创建战斗会话。 */
data class CreateImportedBattleSessionRequest(
    val sessionId: String,
    val formatId: String,
    val sides: List<ImportedBattleSideRequest>,
    val battleKind: BattleType = BattleType.TRAINER,
    val capturableSideId: String? = null,
    val autoStart: Boolean = true,
)

/** 智能出招请求。 */
data class SmartMoveChoiceRequest(
    val attackerId: String,
    val moveId: String,
    val targetId: String? = null,
    val priority: Int? = null,
    val speed: Int? = null,
    val accuracy: Int? = null,
    val evasion: Int? = null,
    val basePower: Int? = null,
    val damage: Int? = null,
    val accuracyRoll: Int? = null,
    val chanceRoll: Int? = null,
    val attributes: Map<String, Any?> = emptyMap(),
)

/** 智能用道具请求。 */
data class SmartItemChoiceRequest(
    val actorUnitId: String,
    val itemId: String,
    val targetId: String? = null,
    val priority: Int? = null,
    val speed: Int? = null,
    val chanceRoll: Int? = null,
    val attributes: Map<String, Any?> = emptyMap(),
)

/** 智能捕捉请求。 */
data class SmartCaptureChoiceRequest(
    val playerId: String,
    val ballItemId: String,
    val sourceUnitId: String,
    val targetId: String,
    val priority: Int? = null,
    val speed: Int? = null,
)

/** 面向前端的换人请求。 */
data class SubmitSwitchChoiceRequest(
    val sideId: String,
    val outgoingUnitId: String,
    val incomingUnitId: String,
    val priority: Int? = null,
    val speed: Int? = null,
)

/** 面向前端的逃跑请求。 */
data class SubmitRunChoiceRequest(
    val sideId: String,
    val priority: Int? = null,
    val speed: Int? = null,
)

/** 面向前端的替补上场请求。 */
data class SubmitReplacementChoiceRequest(
    val sideId: String,
    val incomingUnitId: String,
)

/** 统一战斗结算结果。 */
data class BattleSettlementResult(
    val sessionId: String,
    val endedReason: String?,
    val settled: Boolean,
    val session: io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery,
    val captureResult: CaptureBattleResult? = null,
)
