package io.github.lishangbu.avalon.game.battle.engine.type

/**
 * Hook 的稳定标识。
 *
 * 设计意图：
 * - 用显式类型替代裸字符串，避免把 Hook 名称散落在运行时逻辑中。
 * - 作为 [io.github.lishangbu.avalon.game.battle.engine.event.HookSpec] 与 registry 的统一键。
 *
 * 该类型只负责标识，不负责校验其是否已经注册。
 *
 * @property value Hook 的字符串名称，例如 `on_hit`。
 */
@JvmInline
value class HookName(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "Hook name must not be blank." }
    }
}
