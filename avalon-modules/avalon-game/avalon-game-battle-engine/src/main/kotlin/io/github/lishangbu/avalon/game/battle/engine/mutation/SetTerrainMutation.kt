package io.github.lishangbu.avalon.game.battle.engine.mutation

/**
 * 设置地形变更。
 *
 * @property terrainId 目标地形标识。
 */
data class SetTerrainMutation(
    val terrainId: String,
) : BattleMutation
