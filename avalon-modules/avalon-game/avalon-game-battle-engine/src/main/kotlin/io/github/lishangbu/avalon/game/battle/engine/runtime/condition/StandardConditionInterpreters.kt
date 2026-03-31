package io.github.lishangbu.avalon.game.battle.engine.runtime.condition

import io.github.lishangbu.avalon.game.battle.engine.runtime.ConditionInterpreter

/**
 * 第一版标准条件解释器集合。
 */
object StandardConditionInterpreters {
    fun all(): List<ConditionInterpreter> =
        listOf(
            ChanceConditionInterpreter(),
            HpRatioConditionInterpreter(),
            HasStatusConditionInterpreter(),
            HasVolatileConditionInterpreter(),
            HasTypeConditionInterpreter(),
            HasItemConditionInterpreter(),
            HasAbilityConditionInterpreter(),
            WeatherIsConditionInterpreter(),
            TerrainIsConditionInterpreter(),
            BoostCompareConditionInterpreter(),
            StatCompareConditionInterpreter(),
            MoveHasTagConditionInterpreter(),
            TargetRelationConditionInterpreter(),
            TurnCompareConditionInterpreter(),
            AttributeEqualsConditionInterpreter(),
        )
}
