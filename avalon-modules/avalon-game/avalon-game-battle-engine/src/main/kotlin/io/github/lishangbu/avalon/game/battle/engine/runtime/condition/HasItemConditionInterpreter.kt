package io.github.lishangbu.avalon.game.battle.engine.runtime.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HasItemConditionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.ConditionInterpreter
import io.github.lishangbu.avalon.game.battle.engine.runtime.support.EventContextActorReader
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * `has_item` 条件解释器。
 */
class HasItemConditionInterpreter : ConditionInterpreter {
    override val type: ConditionTypeId = StandardConditionTypeIds.HAS_ITEM

    override fun evaluate(
        condition: ConditionNode,
        context: EventContext,
    ): Boolean {
        require(condition is HasItemConditionNode) { "Condition must be HasItemConditionNode." }
        val unit =
            requireNotNull(EventContextActorReader.readUnit(condition.actor, context)) {
                "Actor '${condition.actor.value}' is not available for item evaluation."
            }
        return unit.itemId == condition.value
    }
}
