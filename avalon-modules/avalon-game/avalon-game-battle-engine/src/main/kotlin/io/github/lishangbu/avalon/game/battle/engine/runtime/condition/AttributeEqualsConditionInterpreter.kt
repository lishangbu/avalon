package io.github.lishangbu.avalon.game.battle.engine.runtime.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.AttributeEqualsConditionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.ConditionInterpreter
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * `attribute_equals` 条件解释器。
 */
class AttributeEqualsConditionInterpreter : ConditionInterpreter {
    override val type: ConditionTypeId = StandardConditionTypeIds.ATTRIBUTE_EQUALS

    override fun evaluate(
        condition: ConditionNode,
        context: EventContext,
    ): Boolean {
        require(condition is AttributeEqualsConditionNode) { "Condition must be AttributeEqualsConditionNode." }
        return context.attributes[condition.key]?.toString() == condition.value
    }
}
