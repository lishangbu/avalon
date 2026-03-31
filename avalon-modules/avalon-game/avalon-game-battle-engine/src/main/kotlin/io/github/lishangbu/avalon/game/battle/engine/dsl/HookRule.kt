package io.github.lishangbu.avalon.game.battle.engine.dsl

/**
 * 单条 Hook 规则定义。
 *
 * 设计意图：
 * - 把一个 Hook 下的“条件 + 动作”组合表达成稳定的数据结构。
 * - 让 Hook 中多条规则可以独立排序、独立标记。
 *
 * @property priority 规则优先级，数值越高越先结算。
 * @property subOrder 同 priority 下的更细粒度排序值。
 * @property condition 规则的触发条件，空值表示无条件执行。
 * @property thenActions 条件满足时执行的动作列表。
 * @property elseActions 条件不满足时执行的动作列表。
 * @property tags 用于调试、分组或作者标记的标签集合。
 */
data class HookRule(
    val priority: Int = 0,
    val subOrder: Int = 0,
    val condition: ConditionNode? = null,
    val thenActions: List<ActionNode> = emptyList(),
    val elseActions: List<ActionNode> = emptyList(),
    val tags: Set<String> = emptySet(),
)
