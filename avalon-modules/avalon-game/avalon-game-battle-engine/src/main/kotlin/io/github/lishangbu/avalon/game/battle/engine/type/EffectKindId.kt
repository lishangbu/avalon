package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * Effect kind 的稳定标识。
 *
 * 设计意图：
 * - 统一表示招式、特性、状态、天气等 effect 类别。
 * - 保持 kind 系统可扩展，不把 DSL-facing 类型固定成 enum。
 *
 * @property value effect kind 名称，例如 `move`、`ability`。
 */
@JvmInline
value class EffectKindId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Effect kind id must not be blank." }
    }
}
