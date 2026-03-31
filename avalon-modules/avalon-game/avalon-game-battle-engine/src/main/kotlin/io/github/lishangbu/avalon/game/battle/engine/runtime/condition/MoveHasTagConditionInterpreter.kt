package io.github.lishangbu.avalon.game.battle.engine.runtime.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.MoveHasTagConditionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.ConditionInterpreter
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * `move_has_tag` 条件解释器。
 */
class MoveHasTagConditionInterpreter : ConditionInterpreter {
    override val type: ConditionTypeId = StandardConditionTypeIds.MOVE_HAS_TAG

    override fun evaluate(
        condition: ConditionNode,
        context: EventContext,
    ): Boolean {
        require(condition is MoveHasTagConditionNode) { "Condition must be MoveHasTagConditionNode." }
        return condition.value in (context.effect?.tags ?: emptySet())
    }
}
