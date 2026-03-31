package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * Condition DSL 中 actor 的标识。
 *
 * 设计意图：
 * - 表达条件是针对 self、target、source 还是 field 等上下文对象求值。
 * - 让 actor 取值进入受控类型系统。
 *
 * @property value actor 名称。
 */
@JvmInline
value class ActorId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Actor id must not be blank." }
    }
}
