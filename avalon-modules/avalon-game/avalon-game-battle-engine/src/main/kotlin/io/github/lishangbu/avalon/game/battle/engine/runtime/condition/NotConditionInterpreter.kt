package io.github.lishangbu.avalon.game.battle.engine.runtime.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.NotConditionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.ConditionInterpreter
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * `not` 条件解释器。
 */
class NotConditionInterpreter : ConditionInterpreter {
    override val type: ConditionTypeId = StandardConditionTypeIds.NOT

    override fun evaluate(
        condition: ConditionNode,
        context: EventContext,
    ): Boolean {
        require(condition is NotConditionNode) { "Condition must be NotConditionNode." }
        error("NotConditionInterpreter requires HookRuleProcessor-level recursion support.")
    }
}
