package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * Special handler 标识。
 *
 * 设计意图：
 * - 显式标记无法由通用 DSL 表达的特殊机制实现。
 * - 作为 special handler registry 的统一键。
 *
 * @property value 特例处理器名称。
 */
@JvmInline
value class SpecialHandlerId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Special handler id must not be blank." }
    }
}
