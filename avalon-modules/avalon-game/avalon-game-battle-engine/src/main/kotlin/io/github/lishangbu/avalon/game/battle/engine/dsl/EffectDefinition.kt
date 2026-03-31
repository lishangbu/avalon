package io.github.lishangbu.avalon.game.battle.engine.dsl

import io.github.lishangbu.avalon.game.battle.engine.type.EffectKindId
import io.github.lishangbu.avalon.game.battle.engine.type.HookName
import io.github.lishangbu.avalon.game.battle.engine.type.SpecialHandlerId

/**
 * Effect 的统一数据定义。
 *
 * 设计意图：
 * - 用统一结构承载招式、特性、状态、天气等 effect 数据。
 * - 作为 JSON 数据加载后的核心内存表示。
 *
 * @property id effect 唯一标识。
 * @property kind effect 所属类别，例如 move、ability。
 * @property name effect 展示名称。
 * @property tags 用于筛选或条件判断的标签集合。
 * @property data 不属于 Hook 的静态元数据。
 * @property hooks 该 effect 挂载的 hook 规则集合。
 * @property specialHandler 可选的特例处理器标识。
 */
data class EffectDefinition(
    val id: String,
    val kind: EffectKindId,
    val name: String,
    val tags: Set<String> = emptySet(),
    val data: Map<String, Any?> = emptyMap(),
    val hooks: Map<HookName, List<HookRule>> = emptyMap(),
    val specialHandler: SpecialHandlerId? = null,
)
