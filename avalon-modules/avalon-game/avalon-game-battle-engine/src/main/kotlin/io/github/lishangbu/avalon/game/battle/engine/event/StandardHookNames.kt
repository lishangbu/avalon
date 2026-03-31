package io.github.lishangbu.avalon.game.battle.engine.event

import io.github.lishangbu.avalon.game.battle.engine.type.HookName

/**
 * 第一版标准 Hook 名称集合。
 *
 * 设计意图：
 * - 让标准 Hook 命名集中管理。
 * - 为默认 HookSpec、DSL 数据、测试用例提供统一常量来源。
 */
object StandardHookNames {
    val ON_SWITCH_IN: HookName = HookName("on_switch_in")
    val ON_SWITCH_OUT: HookName = HookName("on_switch_out")
    val ON_BEFORE_TURN: HookName = HookName("on_before_turn")
    val ON_BEFORE_MOVE: HookName = HookName("on_before_move")
    val ON_TRY_MOVE: HookName = HookName("on_try_move")
    val ON_PREPARE_HIT: HookName = HookName("on_prepare_hit")
    val ON_TRY_HIT: HookName = HookName("on_try_hit")
    val ON_MODIFY_ACCURACY: HookName = HookName("on_modify_accuracy")
    val ON_MODIFY_EVASION: HookName = HookName("on_modify_evasion")
    val ON_MODIFY_BASE_POWER: HookName = HookName("on_modify_base_power")
    val ON_MODIFY_ATTACK: HookName = HookName("on_modify_attack")
    val ON_MODIFY_DEFENSE: HookName = HookName("on_modify_defense")
    val ON_MODIFY_CRIT_RATIO: HookName = HookName("on_modify_crit_ratio")
    val ON_MODIFY_STAB: HookName = HookName("on_modify_stab")
    val ON_MODIFY_DAMAGE: HookName = HookName("on_modify_damage")
    val ON_DAMAGE: HookName = HookName("on_damage")
    val ON_HEAL: HookName = HookName("on_heal")
    val ON_HIT: HookName = HookName("on_hit")
    val ON_AFTER_HIT: HookName = HookName("on_after_hit")
    val ON_AFTER_MOVE: HookName = HookName("on_after_move")
    val ON_SET_STATUS: HookName = HookName("on_set_status")
    val ON_TRY_ADD_VOLATILE: HookName = HookName("on_try_add_volatile")
    val ON_BOOST: HookName = HookName("on_boost")
    val ON_RESIDUAL: HookName = HookName("on_residual")
    val ON_WEATHER_CHANGE: HookName = HookName("on_weather_change")
    val ON_TERRAIN_CHANGE: HookName = HookName("on_terrain_change")
    val ON_FAINT: HookName = HookName("on_faint")
}
