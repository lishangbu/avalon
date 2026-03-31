package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * Relay 语义类型标识。
 *
 * 设计意图：
 * - 描述某个 Hook 在事件链中传递的 relay 值是什么语义。
 * - 避免把 relay 语义写死成 enum，保留后续扩展空间。
 *
 * 该类型只负责标识，不负责定义具体运算规则。
 *
 * @property value relay 类型名称，例如 `boolean` 或 `integer`。
 */
@JvmInline
value class RelayTypeId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Relay type id must not be blank." }
    }
}
