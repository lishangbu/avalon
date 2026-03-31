package io.github.lishangbu.avalon.game.service.capture

import io.github.lishangbu.avalon.game.battle.engine.capture.CaptureContext
import io.github.lishangbu.avalon.game.battle.engine.model.UnitState
import io.github.lishangbu.avalon.game.battle.engine.runtime.flow.BattleRuntimeSnapshot
import io.github.lishangbu.avalon.game.battle.engine.session.BattleSessionQuery

data class CaptureCommand(
    val playerId: String,
    val ballItemId: String,
    val targetUnitId: String,
    val sourceUnitId: String? = null,
)

data class CapturedCreatureSummary(
    val ownedCreatureId: String,
    val creatureId: String,
    val creatureSpeciesId: String,
    val creatureInternalName: String,
    val creatureName: String,
)

data class CaptureBattleResult(
    val success: Boolean,
    val sessionId: String,
    val targetUnitId: String,
    val ballItemId: String,
    val shakes: Int,
    val reason: String,
    val battleEnded: Boolean,
    val finalRate: Double,
    val session: BattleSessionQuery,
    val capturedCreature: CapturedCreatureSummary? = null,
)

data class BattleUnitMetadata(
    val creatureId: Long,
    val creatureSpeciesId: Long,
    val creatureInternalName: String,
    val creatureName: String,
    val level: Int,
    val requiredExperience: Int,
    val natureId: Long?,
    val captureRate: Int?,
    val ivs: Map<String, Int>,
    val evs: Map<String, Int>,
    val calculatedStats: Map<String, Int>,
)

data class PreparedCaptureContext(
    val sessionId: String,
    val playerId: Long,
    val ballItemId: Long,
    val ballItemInternalName: String,
    val targetUnitId: String,
    val sourceUnitId: String?,
    val snapshot: BattleRuntimeSnapshot,
    val targetUnit: UnitState,
    val sourceUnit: UnitState?,
    val targetMetadata: BattleUnitMetadata,
    val battleContext: CaptureContext,
)
