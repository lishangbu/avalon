package io.github.lishangbu.avalon.game.battle.engine.mutation

/**
 * 设置天气变更。
 *
 * @property weatherId 目标天气标识。
 */
data class SetWeatherMutation(
    val weatherId: String,
) : BattleMutation
