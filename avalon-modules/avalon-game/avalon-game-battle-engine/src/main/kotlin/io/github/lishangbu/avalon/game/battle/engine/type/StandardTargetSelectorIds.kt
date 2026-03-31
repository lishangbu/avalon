package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * 第一版标准 target selector 集合。
 *
 * 设计意图：
 * - 与文档中的 selector 白名单保持一致。
 * - 作为动作节点的默认目标选择器来源。
 */
object StandardTargetSelectorIds {
    val SELF: TargetSelectorId = TargetSelectorId("self")
    val TARGET: TargetSelectorId = TargetSelectorId("target")
    val SOURCE: TargetSelectorId = TargetSelectorId("source")
    val ALLY: TargetSelectorId = TargetSelectorId("ally")
    val ALL_ALLIES: TargetSelectorId = TargetSelectorId("all_allies")
    val FOE: TargetSelectorId = TargetSelectorId("foe")
    val ALL_FOES: TargetSelectorId = TargetSelectorId("all_foes")
    val SIDE: TargetSelectorId = TargetSelectorId("side")
    val FOE_SIDE: TargetSelectorId = TargetSelectorId("foe_side")
    val FIELD: TargetSelectorId = TargetSelectorId("field")
    val ALL: TargetSelectorId = TargetSelectorId("all")
}
