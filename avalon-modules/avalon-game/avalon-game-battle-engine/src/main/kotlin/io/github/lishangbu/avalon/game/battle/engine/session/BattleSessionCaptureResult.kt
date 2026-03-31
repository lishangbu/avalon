package io.github.lishangbu.avalon.game.battle.engine.session

/**
 * 捕捉行动在 battle 内部的执行结果。
 */
data class BattleSessionCaptureResult(
    val success: Boolean,
    val playerId: String,
    val ballItemId: String,
    val sourceUnitId: String,
    val targetId: String,
    val shakes: Int,
    val reason: String,
    val finalRate: Double,
)
