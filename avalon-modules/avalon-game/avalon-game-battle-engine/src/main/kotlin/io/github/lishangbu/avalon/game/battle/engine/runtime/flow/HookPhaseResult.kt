package io.github.lishangbu.avalon.game.battle.engine.runtime.flow

/**
 * 单个 hook phase 在 battle flow 中的处理结果。
 *
 * @property snapshot 处理后的快照。
 * @property cancelled 当前 phase 是否请求中断。
 * @property relay phase 结束后的 relay 值。
 */
data class HookPhaseResult(
    val snapshot: BattleRuntimeSnapshot,
    val cancelled: Boolean,
    val relay: Any? = null,
)
