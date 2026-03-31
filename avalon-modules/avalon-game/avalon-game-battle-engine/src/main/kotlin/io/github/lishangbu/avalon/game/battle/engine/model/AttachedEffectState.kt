package io.github.lishangbu.avalon.game.battle.engine.model

/**
 * 挂载在运行时对象上的 effect state 骨架。
 *
 * 设计意图：
 * - 保存 effect 的局内状态，而不是把这些信息散落在 battle / side / unit 字段中。
 * - 为持续回合、来源、创建顺序等信息提供统一容器。
 *
 * @property effectId effect 定义标识。
 * @property sourceId effect 来源对象标识。
 * @property duration 剩余持续时间，空值表示无固定持续回合。
 * @property effectOrder effect 挂载顺序，用于稳定 tie-break。
 * @property flags effect 自身维护的轻量字符串标记集合。
 */
data class AttachedEffectState(
    val effectId: String,
    val sourceId: String? = null,
    val duration: Int? = null,
    val effectOrder: Int = 0,
    val flags: Map<String, String> = emptyMap(),
)
