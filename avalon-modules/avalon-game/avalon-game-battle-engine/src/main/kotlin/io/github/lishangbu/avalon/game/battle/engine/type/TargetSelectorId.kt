package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * Action DSL 中 target selector 的标识。
 *
 * 设计意图：
 * - 把动作目标从裸字符串提升为显式类型。
 * - 交由 [io.github.lishangbu.avalon.game.battle.engine.runtime.TargetResolver] 统一解析。
 *
 * @property value selector 名称。
 */
@JvmInline
value class TargetSelectorId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Target selector id must not be blank." }
    }
}
