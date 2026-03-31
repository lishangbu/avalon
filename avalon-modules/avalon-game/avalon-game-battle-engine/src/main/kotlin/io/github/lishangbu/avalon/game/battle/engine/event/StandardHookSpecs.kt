package io.github.lishangbu.avalon.game.battle.engine.event

import io.github.lishangbu.avalon.game.battle.engine.type.HookName
import io.github.lishangbu.avalon.game.battle.engine.type.StandardRelayTypeIds

/**
 * 第一版标准 HookSpec 集合。
 *
 * 设计意图：
 * - 把标准 Hook 的 relay 语义与取消能力集中定义。
 * - 为默认 HookSpecRegistry 提供即插即用的数据来源。
 */
object StandardHookSpecs {
    fun all(): Map<HookName, HookSpec> =
        listOf(
            HookSpec(StandardHookNames.ON_SWITCH_IN, StandardRelayTypeIds.NONE, false, false),
            HookSpec(StandardHookNames.ON_SWITCH_OUT, StandardRelayTypeIds.NONE, false, false),
            HookSpec(StandardHookNames.ON_BEFORE_TURN, StandardRelayTypeIds.NONE, false, false),
            HookSpec(StandardHookNames.ON_BEFORE_MOVE, StandardRelayTypeIds.BOOLEAN, false, true),
            HookSpec(StandardHookNames.ON_TRY_MOVE, StandardRelayTypeIds.BOOLEAN, false, true),
            HookSpec(StandardHookNames.ON_PREPARE_HIT, StandardRelayTypeIds.BOOLEAN, false, true),
            HookSpec(StandardHookNames.ON_TRY_HIT, StandardRelayTypeIds.BOOLEAN, false, true),
            HookSpec(StandardHookNames.ON_MODIFY_ACCURACY, StandardRelayTypeIds.DECIMAL, true, false),
            HookSpec(StandardHookNames.ON_MODIFY_EVASION, StandardRelayTypeIds.DECIMAL, true, false),
            HookSpec(StandardHookNames.ON_MODIFY_BASE_POWER, StandardRelayTypeIds.INTEGER, true, false),
            HookSpec(StandardHookNames.ON_MODIFY_ATTACK, StandardRelayTypeIds.INTEGER, true, false),
            HookSpec(StandardHookNames.ON_MODIFY_DEFENSE, StandardRelayTypeIds.INTEGER, true, false),
            HookSpec(StandardHookNames.ON_MODIFY_CRIT_RATIO, StandardRelayTypeIds.INTEGER, true, false),
            HookSpec(StandardHookNames.ON_MODIFY_STAB, StandardRelayTypeIds.DECIMAL, true, false),
            HookSpec(StandardHookNames.ON_MODIFY_DAMAGE, StandardRelayTypeIds.INTEGER, true, false),
            HookSpec(StandardHookNames.ON_DAMAGE, StandardRelayTypeIds.INTEGER, true, false),
            HookSpec(StandardHookNames.ON_HEAL, StandardRelayTypeIds.INTEGER, true, false),
            HookSpec(StandardHookNames.ON_HIT, StandardRelayTypeIds.NONE, false, false),
            HookSpec(StandardHookNames.ON_AFTER_HIT, StandardRelayTypeIds.NONE, false, false),
            HookSpec(StandardHookNames.ON_AFTER_MOVE, StandardRelayTypeIds.NONE, false, false),
            HookSpec(StandardHookNames.ON_SET_STATUS, StandardRelayTypeIds.BOOLEAN, false, true),
            HookSpec(StandardHookNames.ON_TRY_ADD_VOLATILE, StandardRelayTypeIds.BOOLEAN, false, true),
            HookSpec(StandardHookNames.ON_BOOST, StandardRelayTypeIds.OBJECT, true, false),
            HookSpec(StandardHookNames.ON_RESIDUAL, StandardRelayTypeIds.NONE, false, false),
            HookSpec(StandardHookNames.ON_WEATHER_CHANGE, StandardRelayTypeIds.NONE, false, false),
            HookSpec(StandardHookNames.ON_TERRAIN_CHANGE, StandardRelayTypeIds.NONE, false, false),
            HookSpec(StandardHookNames.ON_FAINT, StandardRelayTypeIds.NONE, false, false),
        ).associateBy(HookSpec::name)
}
