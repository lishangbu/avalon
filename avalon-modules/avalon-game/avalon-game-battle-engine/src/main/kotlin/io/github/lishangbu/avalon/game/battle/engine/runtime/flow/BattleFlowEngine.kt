package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

/**
 * battle 主流程入口。
 *
 * 当前阶段先只接入：
 * - 命中结算
 * - residual 结算
 */
interface BattleFlowEngine {
    fun resolveMoveAction(
        snapshot: BattleRuntimeSnapshot,
        moveId: String,
        attackerId: String,
        targetId: String,
        accuracy: Int? = null,
        evasion: Int? = null,
        basePower: Int,
        damage: Int,
        attributes: Map<String, Any?> = emptyMap(),
    ): MoveResolutionResult

    fun resolveMoveHit(
        snapshot: BattleRuntimeSnapshot,
        moveId: String,
        attackerId: String,
        targetId: String,
        attributes: Map<String, Any?> = emptyMap(),
    ): BattleRuntimeSnapshot

    fun resolveResidualPhase(
        snapshot: BattleRuntimeSnapshot,
    ): BattleRuntimeSnapshot
}
