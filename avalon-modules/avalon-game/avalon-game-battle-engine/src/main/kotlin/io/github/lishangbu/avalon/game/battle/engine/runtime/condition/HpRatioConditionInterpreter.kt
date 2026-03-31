package io.github.lishangbu.avalon.game.battle.engine.runtime.condition

import io.github.lishangbu.avalon.game.battle.engine.dsl.ConditionNode
import io.github.lishangbu.avalon.game.battle.engine.dsl.condition.HpRatioConditionNode
import io.github.lishangbu.avalon.game.battle.engine.event.EventContext
import io.github.lishangbu.avalon.game.battle.engine.runtime.ConditionInterpreter
import io.github.lishangbu.avalon.game.battle.engine.runtime.support.ComparisonSupport
import io.github.lishangbu.avalon.game.battle.engine.runtime.support.EventContextActorReader
import io.github.lishangbu.avalon.game.battle.engine.type.ConditionTypeId
import io.github.lishangbu.avalon.game.battle.engine.type.StandardConditionTypeIds

/**
 * `hp_ratio` 条件解释器。
 */
class HpRatioConditionInterpreter : ConditionInterpreter {
    override val type: ConditionTypeId = StandardConditionTypeIds.HP_RATIO

    override fun evaluate(
        condition: ConditionNode,
        context: EventContext,
    ): Boolean {
        require(condition is HpRatioConditionNode) { "Condition must be HpRatioConditionNode." }
        val unit =
            requireNotNull(EventContextActorReader.readUnit(condition.actor, context)) {
                "Actor '${condition.actor.value}' is not available for hp ratio evaluation."
            }
        val currentRatio = unit.currentHp.toDouble() / unit.maxHp.toDouble()
        return ComparisonSupport.compare(currentRatio, condition.operator, condition.value)
    }
}
