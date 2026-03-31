package io.github.lishangbu.avalon.game.battle.engine.model

/**
 * 场地级运行时状态骨架。
 *
 * 设计意图：
 * - 表示天气、地形等战场公共状态。
 * - 避免把 battle 共享状态和 side / unit 状态混在一起。
 *
 * @property weatherId 当前天气标识。
 * @property terrainId 当前地形标识。
 */
data class FieldState(
    val weatherId: String? = null,
    val terrainId: String? = null,
)
