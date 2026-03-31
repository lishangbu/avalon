package io.github.lishangbu.avalon.game.battle.engine.capture

data class CaptureContext(
    val alreadyCaught: Boolean = false,
    val isFishingEncounter: Boolean = false,
    val isSurfEncounter: Boolean = false,
    val isNight: Boolean = false,
    val isCave: Boolean = false,
    val isUltraBeast: Boolean = false,
    val targetLevel: Int? = null,
    val targetWeight: Int? = null,
    val targetTypes: Set<String> = emptySet(),
)

data class CaptureFormulaInput(
    val currentHp: Int,
    val maxHp: Int,
    val captureRate: Int,
    val statusId: String?,
    val ballItemInternalName: String,
    val turn: Int,
    val battleContext: CaptureContext,
)

data class CaptureFormulaResult(
    val success: Boolean,
    val shakes: Int,
    val captureValue: Double,
    val finalRate: Double,
    val ballRate: Double,
    val statusRate: Double,
    val reason: String,
)
