package io.github.lishangbu.avalon.game.battle.engine.runtime

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.HookRule
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.AllConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.AnyConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.NotConditionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.mutation.BattleMutation
import io.github.lishangbu.avalon.game.battle.engine.registry.ActionExecutorRegistry
import io.github.lishangbu.avalon.game.battle.engine.registry.ConditionInterpreterRegistry

/**
 * 默认 HookRule 处理器。
 *
 * 设计意图：
 * - 串起条件求值与动作执行。
 * - 以结构化 mutation 和 relay 变化作为处理输出。
 */
class DefaultHookRuleProcessor(
    private val conditionInterpreterRegistry: ConditionInterpreterRegistry,
    private val actionExecutorRegistry: ActionExecutorRegistry,
) : HookRuleProcessor {
    override fun process(
        rule: HookRule,
        context: EventContext,
    ): HookRuleResult {
        val matched: Boolean = rule.condition?.let { evaluateCondition(it, context) } ?: true
        val selectedActions = if (matched) rule.thenActions else rule.elseActions

        var currentRelay: Any? = context.relay
        var cancelled = false
        val mutations = mutableListOf<BattleMutation>()

        for (action in selectedActions) {
            val executor = actionExecutorRegistry.get(action.type)
            val actionContext = context.copy(relay = currentRelay)
            val actionResult = executor.execute(action, actionContext)
            if (actionResult.relay != null) {
                currentRelay = actionResult.relay
            }
            mutations += actionResult.mutations
            if (actionResult.cancelled) {
                cancelled = true
                break
            }
        }

        return HookRuleResult(
            matched = matched,
            cancelled = cancelled,
            relay = currentRelay,
            mutations = mutations,
        )
    }

    private fun evaluateCondition(
        condition: ConditionNode,
        context: EventContext,
    ): Boolean =
        when (condition) {
            is AllConditionNode -> condition.conditions.all { evaluateCondition(it, context) }
            is AnyConditionNode -> condition.conditions.any { evaluateCondition(it, context) }
            is NotConditionNode -> !evaluateCondition(condition.condition, context)
            else -> conditionInterpreterRegistry.get(condition.type).evaluate(condition, context)
        }
}
