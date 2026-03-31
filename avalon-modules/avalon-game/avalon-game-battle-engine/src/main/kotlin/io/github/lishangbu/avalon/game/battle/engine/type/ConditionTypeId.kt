package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * Condition 节点类型标识。
 *
 * 设计意图：
 * - 作为条件 DSL 节点与解释器之间的桥接键。
 * - 用类型化 ID 替代到处散写的字符串。
 *
 * @property value 条件类型名称，例如 `all`、`hp_ratio`。
 */
@JvmInline
value class ConditionTypeId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Condition type id must not be blank." }
    }
}
