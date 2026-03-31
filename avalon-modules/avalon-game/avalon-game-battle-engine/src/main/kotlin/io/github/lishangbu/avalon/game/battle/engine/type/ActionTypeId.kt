package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * Action 节点类型标识。
 *
 * 设计意图：
 * - 作为动作 DSL 节点与执行器之间的桥接键。
 * - 保持动作系统可注册扩展。
 *
 * @property value 动作类型名称，例如 `damage`、`boost`。
 */
@JvmInline
value class ActionTypeId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Action type id must not be blank." }
    }
}
