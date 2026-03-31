package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * 第一版标准 effect kind 集合。
 *
 * 设计意图：
 * - 为 EffectDefinition 提供约定俗成的默认 kind。
 * - 让数据层和注册层共享统一命名。
 */
object StandardEffectKindIds {
    val MOVE: EffectKindId = EffectKindId("move")
    val ABILITY: EffectKindId = EffectKindId("ability")
    val ITEM: EffectKindId = EffectKindId("item")
    val STATUS: EffectKindId = EffectKindId("status")
    val VOLATILE: EffectKindId = EffectKindId("volatile")
    val SIDE_CONDITION: EffectKindId = EffectKindId("side_condition")
    val PSEUDO_WEATHER: EffectKindId = EffectKindId("pseudo_weather")
    val WEATHER: EffectKindId = EffectKindId("weather")
    val TERRAIN: EffectKindId = EffectKindId("terrain")
    val FORMAT_RULE: EffectKindId = EffectKindId("format_rule")
}
