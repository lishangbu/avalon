package io.github.lishangbu.avalon.game.battle.engine.runtime.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HasTypeConditionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.ConditionInterpreter
import io.github.lishangbu.avalon.game.battle.engine.runtime.support.EventContextActorReader
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * `has_type` 条件解释器。
 */
class HasTypeConditionInterpreter : ConditionInterpreter {
    override val type: ConditionTypeId = StandardConditionTypeIds.HAS_TYPE

    override fun evaluate(
        condition: ConditionNode,
        context: EventContext,
    ): Boolean {
        require(condition is HasTypeConditionNode) { "Condition must be HasTypeConditionNode." }
        val unit =
            requireNotNull(EventContextActorReader.readUnit(condition.actor, context)) {
                "Actor '${condition.actor.value}' is not available for type evaluation."
            }
        return condition.value in unit.typeIds
    }
}
