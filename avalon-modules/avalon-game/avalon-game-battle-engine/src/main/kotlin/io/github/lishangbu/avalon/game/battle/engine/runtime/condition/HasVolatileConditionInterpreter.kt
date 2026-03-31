package io.github.lishangbu.avalon.game.battle.engine.runtime.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HasVolatileConditionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.ConditionInterpreter
import io.github.lishangbu.avalon.game.battle.engine.runtime.support.EventContextActorReader
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * `has_volatile` 条件解释器。
 */
class HasVolatileConditionInterpreter : ConditionInterpreter {
    override val type: ConditionTypeId = StandardConditionTypeIds.HAS_VOLATILE

    override fun evaluate(
        condition: ConditionNode,
        context: EventContext,
    ): Boolean {
        require(condition is HasVolatileConditionNode) { "Condition must be HasVolatileConditionNode." }
        val unit =
            requireNotNull(EventContextActorReader.readUnit(condition.actor, context)) {
                "Actor '${condition.actor.value}' is not available for volatile evaluation."
            }
        return condition.value in unit.volatileIds
    }
}
