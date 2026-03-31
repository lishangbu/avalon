package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * 第一版标准 actor 集合。
 *
 * 设计意图：
 * - 提供文档中约定的默认 actor 常量。
 * - 避免在 DSL 节点构造时重复手写字符串。
 */
object StandardActorIds {
    val SELF: ActorId = ActorId("self")
    val TARGET: ActorId = ActorId("target")
    val SOURCE: ActorId = ActorId("source")
    val MOVE: ActorId = ActorId("move")
    val FIELD: ActorId = ActorId("field")
    val SIDE: ActorId = ActorId("side")
    val FOE_SIDE: ActorId = ActorId("foe_side")
}
