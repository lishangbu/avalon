package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * 第一版标准 condition type 集合。
 *
 * 设计意图：
 * - 与设计文档中的条件白名单保持一致。
 * - 供具体 DSL 节点与解释器 registry 共享。
 */
object StandardConditionTypeIds {
    val ALL: ConditionTypeId = ConditionTypeId("all")
    val ANY: ConditionTypeId = ConditionTypeId("any")
    val NOT: ConditionTypeId = ConditionTypeId("not")
    val CHANCE: ConditionTypeId = ConditionTypeId("chance")
    val HP_RATIO: ConditionTypeId = ConditionTypeId("hp_ratio")
    val HAS_STATUS: ConditionTypeId = ConditionTypeId("has_status")
    val HAS_VOLATILE: ConditionTypeId = ConditionTypeId("has_volatile")
    val HAS_TYPE: ConditionTypeId = ConditionTypeId("has_type")
    val HAS_ITEM: ConditionTypeId = ConditionTypeId("has_item")
    val HAS_ABILITY: ConditionTypeId = ConditionTypeId("has_ability")
    val WEATHER_IS: ConditionTypeId = ConditionTypeId("weather_is")
    val TERRAIN_IS: ConditionTypeId = ConditionTypeId("terrain_is")
    val BOOST_COMPARE: ConditionTypeId = ConditionTypeId("boost_compare")
    val STAT_COMPARE: ConditionTypeId = ConditionTypeId("stat_compare")
    val MOVE_HAS_TAG: ConditionTypeId = ConditionTypeId("move_has_tag")
    val TARGET_RELATION: ConditionTypeId = ConditionTypeId("target_relation")
    val TURN_COMPARE: ConditionTypeId = ConditionTypeId("turn_compare")
    val ATTRIBUTE_EQUALS: ConditionTypeId = ConditionTypeId("attribute_equals")
}
