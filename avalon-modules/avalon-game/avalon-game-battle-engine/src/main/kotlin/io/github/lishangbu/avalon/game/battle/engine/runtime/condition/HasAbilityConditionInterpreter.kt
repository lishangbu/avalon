package io.github.lishangbu.avalon.game.battle.engine.runtime.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HasAbilityConditionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.ConditionInterpreter
import io.github.lishangbu.avalon.game.battle.engine.runtime.support.EventContextActorReader
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * `has_ability` 条件解释器。
 */
class HasAbilityConditionInterpreter : ConditionInterpreter {
    override val type: ConditionTypeId = StandardConditionTypeIds.HAS_ABILITY

    override fun evaluate(
        condition: ConditionNode,
        context: EventContext,
    ): Boolean {
        require(condition is HasAbilityConditionNode) { "Condition must be HasAbilityConditionNode." }
        val unit =
            requireNotNull(EventContextActorReader.readUnit(condition.actor, context)) {
                "Actor '${condition.actor.value}' is not available for ability evaluation."
            }
        return unit.abilityId == condition.value
    }
}
