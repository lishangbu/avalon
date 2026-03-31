package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * 第一版标准 action type 集合。
 *
 * 设计意图：
 * - 与设计文档中的动作白名单保持一致。
 * - 供动作节点与执行器 registry 共享。
 */
object StandardActionTypeIds {
    val DAMAGE: ActionTypeId = ActionTypeId("damage")
    val HEAL: ActionTypeId = ActionTypeId("heal")
    val ADD_STATUS: ActionTypeId = ActionTypeId("add_status")
    val REMOVE_STATUS: ActionTypeId = ActionTypeId("remove_status")
    val ADD_VOLATILE: ActionTypeId = ActionTypeId("add_volatile")
    val REMOVE_VOLATILE: ActionTypeId = ActionTypeId("remove_volatile")
    val BOOST: ActionTypeId = ActionTypeId("boost")
    val CLEAR_BOOSTS: ActionTypeId = ActionTypeId("clear_boosts")
    val SET_WEATHER: ActionTypeId = ActionTypeId("set_weather")
    val CLEAR_WEATHER: ActionTypeId = ActionTypeId("clear_weather")
    val SET_TERRAIN: ActionTypeId = ActionTypeId("set_terrain")
    val CLEAR_TERRAIN: ActionTypeId = ActionTypeId("clear_terrain")
    val CONSUME_ITEM: ActionTypeId = ActionTypeId("consume_item")
    val RESTORE_PP: ActionTypeId = ActionTypeId("restore_pp")
    val CHANGE_TYPE: ActionTypeId = ActionTypeId("change_type")
    val FORCE_SWITCH: ActionTypeId = ActionTypeId("force_switch")
    val FAIL_MOVE: ActionTypeId = ActionTypeId("fail_move")
    val TRIGGER_EVENT: ActionTypeId = ActionTypeId("trigger_event")
    val APPLY_CONDITION: ActionTypeId = ActionTypeId("apply_condition")
    val REMOVE_CONDITION: ActionTypeId = ActionTypeId("remove_condition")
    val MODIFY_MULTIPLIER: ActionTypeId = ActionTypeId("modify_multiplier")
    val SET_FLAG: ActionTypeId = ActionTypeId("set_flag")
    val CLEAR_FLAG: ActionTypeId = ActionTypeId("clear_flag")
}
